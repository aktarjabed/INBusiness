package com.aktarjabed.inbusiness.data.repository

import android.app.Activity
import com.aktarjabed.inbusiness.data.local.dao.PaymentDao
import com.aktarjabed.inbusiness.data.local.dao.UserQuotaDao
import com.aktarjabed.inbusiness.data.local.entities.PaymentEntity
import com.aktarjabed.inbusiness.data.local.entities.UserEntity
import com.aktarjabed.inbusiness.data.remote.config.RemoteConfigRepository
import com.aktarjabed.inbusiness.domain.model.SubscriptionPlan
import com.aktarjabed.inbusiness.domain.util.Result
import com.razorpay.Checkout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentRepository @Inject constructor(
    private val paymentDao: PaymentDao,
    private val quotaDao: UserQuotaDao,
    private val remoteConfig: RemoteConfigRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var currentCallback: ((Result<String>) -> Unit)? = null
    private var currentUser: UserEntity? = null
    private var currentPlan: SubscriptionPlan? = null

    fun initiatePayment(
        activity: Activity,
        plan: SubscriptionPlan,
        user: UserEntity,
        onResult: (Result<String>) -> Unit
    ) {
        currentCallback = onResult
        currentUser = user
        currentPlan = plan

        try {
            val razorpayKeyId = remoteConfig.getRazorpayKeyId()
            if (razorpayKeyId.isBlank()) {
                onResult(Result.failure(Exception("Razorpay not configured")))
                return
            }

            val checkout = Checkout().apply {
                setKeyID(razorpayKeyId)
            }

            val options = JSONObject().apply {
                put("name", "INBusiness")
                put("description", "${plan.displayName} Subscription")
                put("image", "https://your-logo-url.com/logo.png")
                put("currency", "INR")
                put("amount", plan.priceInPaise)

                // Prefill user details
                put("prefill", JSONObject().apply {
                    put("email", user.email ?: "")
                    put("contact", user.phone ?: "")
                    put("name", user.displayName ?: "")
                })

                // All Indian payment methods
                put("method", JSONObject().apply {
                    put("upi", true)           // GPay, PhonePe, Paytm, BHIM
                    put("card", true)          // Visa, Master, RuPay
                    put("netbanking", true)    // All banks
                    put("wallet", true)        // Paytm, Amazon Pay, Mobikwik
                    put("emi", true)           // Card EMI
                    put("paylater", true)      // LazyPay, Simpl
                })

                // Theme
                put("theme", JSONObject().apply {
                    put("color", "#6200EE")
                    put("hide_topbar", false)
                })

                // Retry config
                put("retry", JSONObject().apply {
                    put("enabled", true)
                    put("max_count", 3)
                })

                put("send_sms_hash", true)
                put("remember_customer", false)
                put("timeout", 300) // 5 minutes
            }

            checkout.open(activity, options)
            Timber.d("Payment initiated for plan: ${plan.displayName}")

        } catch (e: Exception) {
            Timber.e(e, "Payment initiation failed")
            onResult(Result.failure(e))
            cleanup()
        }
    }

    fun onPaymentSuccess(razorpayPaymentId: String) {
        scope.launch {
            try {
                val user = currentUser ?: return@launch
                val plan = currentPlan ?: return@launch

                // Create payment record
                val payment = PaymentEntity(
                    paymentId = UUID.randomUUID().toString(),
                    userId = user.uid,
                    razorpayOrderId = "order_${System.currentTimeMillis()}",
                    razorpayPaymentId = razorpayPaymentId,
                    signature = "", // Verify on server
                    amountPaise = plan.priceInPaise.toLong(),
                    plan = plan.name,
                    currency = "INR",
                    status = "PAID",
                    createdAt = System.currentTimeMillis()
                )

                paymentDao.insertPayment(payment)

                // Upgrade user quota
                quotaDao.upgradeTier(
                    userId = user.uid,
                    tier = plan.name,
                    watermark = false,
                    retentionDays = Int.MAX_VALUE
                )

                Timber.i("Payment successful: $razorpayPaymentId, Plan: ${plan.name}")
                currentCallback?.invoke(Result.success(razorpayPaymentId))

            } catch (e: Exception) {
                Timber.e(e, "Error processing payment success")
                currentCallback?.invoke(Result.failure(e))
            } finally {
                cleanup()
            }
        }
    }

    fun onPaymentError(code: Int, response: String?) {
        scope.launch {
            try {
                val user = currentUser ?: return@launch
                val plan = currentPlan ?: return@launch

                // Log failed payment
                val payment = PaymentEntity(
                    paymentId = UUID.randomUUID().toString(),
                    userId = user.uid,
                    razorpayOrderId = "",
                    razorpayPaymentId = "",
                    signature = "",
                    amountPaise = plan.priceInPaise.toLong(),
                    plan = plan.name,
                    currency = "INR",
                    status = "FAILED",
                    createdAt = System.currentTimeMillis()
                )

                paymentDao.insertPayment(payment)

                Timber.e("Payment failed: Code=$code, Response=$response")
                currentCallback?.invoke(
                    Result.failure(Exception("Payment failed: $response"))
                )

            } catch (e: Exception) {
                Timber.e(e, "Error processing payment failure")
            } finally {
                cleanup()
            }
        }
    }

    suspend fun getPaymentHistory(userId: String): List<PaymentEntity> {
        return try {
            paymentDao.getUserPayments(userId)
        } catch (e: Exception) {
            Timber.e(e, "Error fetching payment history")
            emptyList()
        }
    }

    suspend fun handleSubscriptionRenewal(userId: String): Result<SubscriptionPlan> {
        return try {
            val lastPayment = paymentDao.getLatestPaidPayment(userId)
            val plan = lastPayment?.let { SubscriptionPlan.valueOf(it.plan) }
                ?: return Result.failure(Exception("No payment found"))

            // Check if subscription is still valid (30 days)
            val now = System.currentTimeMillis()
            val expiryTime = lastPayment.createdAt + (30 * 24 * 60 * 60 * 1000L)

            if (now > expiryTime) {
                // Subscription expired, downgrade to free
                quotaDao.downgradeTier(userId, "FREE", true, 30)
                Timber.i("Subscription expired for user: $userId")
                Result.failure(Exception("Subscription expired"))
            } else {
                Result.success(plan)
            }
        } catch (e: Exception) {
            Timber.e(e, "Subscription renewal check failed")
            Result.failure(e)
        }
    }

    private fun cleanup() {
        currentCallback = null
        currentUser = null
        currentPlan = null
    }
}