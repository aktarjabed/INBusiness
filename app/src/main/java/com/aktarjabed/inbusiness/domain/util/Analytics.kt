package com.aktarjabed.inbusiness.domain.util

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Analytics @Inject constructor() {

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    fun initialize() {
        firebaseAnalytics = Firebase.analytics
        Timber.d("Analytics initialized")
    }

    fun logEvent(eventName: String, params: Map<String, Any>? = null) {
        try {
            firebaseAnalytics.logEvent(eventName) {
                params?.forEach { (key, value) ->
                    when (value) {
                        is String -> param(key, value)
                        is Long -> param(key, value)
                        is Double -> param(key, value)
                        is Int -> param(key, value.toLong())
                        else -> param(key, value.toString())
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to log analytics event: $eventName")
        }
    }

    fun setUserId(userId: String) {
        try {
            firebaseAnalytics.setUserId(userId)
        } catch (e: Exception) {
            Timber.e(e, "Failed to set user ID")
        }
    }

    fun setUserProperty(name: String, value: String) {
        try {
            firebaseAnalytics.setUserProperty(name, value)
        } catch (e: Exception) {
            Timber.e(e, "Failed to set user property: $name")
        }
    }

    // Predefined events
    fun logLogin(method: String) {
        logEvent("login", mapOf("method" to method))
    }

    fun logSignup(method: String) {
        logEvent("sign_up", mapOf("method" to method))
    }

    fun logPaymentAttempt(plan: String, amount: Int) {
        logEvent("payment_attempt", mapOf(
            "plan" to plan,
            "amount" to amount
        ))
    }

    fun logPaymentSuccess(plan: String, paymentId: String) {
        logEvent("payment_success", mapOf(
            "plan" to plan,
            "payment_id" to paymentId
        ))
    }

    fun logInvoiceCreated(invoiceId: String, amount: Double) {
        logEvent("invoice_created", mapOf(
            "invoice_id" to invoiceId,
            "amount" to amount
        ))
    }
}