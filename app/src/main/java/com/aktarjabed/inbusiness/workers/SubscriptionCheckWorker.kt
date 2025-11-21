package com.aktarjabed.inbusiness.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.aktarjabed.inbusiness.data.local.dao.PaymentDao
import com.aktarjabed.inbusiness.data.local.dao.UserQuotaDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.util.concurrent.TimeUnit

@HiltWorker
class SubscriptionCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val paymentDao: PaymentDao,
    private val quotaDao: UserQuotaDao
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Timber.d("Running subscription check worker")

            val quotas = quotaDao.getAllQuotas()

            quotas.forEach { quota ->
                if (quota.tier != "FREE") {
                    val lastPayment = paymentDao.getLatestPaidPayment(quota.userId)

                    lastPayment?.let { payment ->
                        val now = System.currentTimeMillis()
                        val expiryTime = payment.createdAt + (30 * 24 * 60 * 60 * 1000L)

                        if (now > expiryTime) {
                            quotaDao.downgradeTier(
                                userId = quota.userId,
                                tier = "FREE",
                                watermark = true,
                                retentionDays = 30
                            )
                            Timber.i("Downgraded user ${quota.userId} to FREE")
                        }
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Subscription check failed")
            Result.retry()
        }
    }

    companion object {
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<SubscriptionCheckWorker>(
                repeatInterval = 1,
                repeatIntervalTimeUnit = TimeUnit.DAYS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "subscription_check",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}