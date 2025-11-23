package com.aktarjabed.inbusiness.domain.quotas

import android.content.Context
import com.aktarjabed.inbusiness.data.db.dao.UserQuotaDao
import com.aktarjabed.inbusiness.data.db.entities.UserQuotaEntity
import com.aktarjabed.inbusiness.data.remote.RemoteConfigRepository
import com.aktarjabed.inbusiness.domain.analytics.QuotaAnalytics
import com.aktarjabed.inbusiness.domain.device.DeviceClassifier
import com.aktarjabed.inbusiness.domain.device.DeviceTier
import com.aktarjabed.inbusiness.domain.experiments.Experiments
import com.aktarjabed.inbusiness.util.SystemClock
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuotaGate @Inject constructor(
    private val dao: UserQuotaDao,
    private val remote: RemoteConfigRepository,
    private val experiments: Experiments,
    private val analytics: QuotaAnalytics,
    private val deviceClassifier: DeviceClassifier,
    private val clock: SystemClock,
    @ApplicationContext private val context: Context
) {

    suspend fun assertQuota(userId: String): QuotaVerdict = withContext(Dispatchers.IO) {
        // Kill switch (can disable free tier remotely in 30 seconds)
        if (remote.getBoolean("kill_free_tier")) {
            return@withContext QuotaVerdict.Killed
        }

        val today = clock.todayEpochDay()
        val entity = dao.getQuota(userId) ?: createFirstQuota(userId, today)

        // Roll daily counter if new day
        if (entity.lastResetEpochDay != today) {
            dao.resetDaily(userId, today)
            return@withContext assertQuota(userId) // Retry after reset
        }

        // Roll monthly counter if new month
        val monthStart = clock.monthStartEpochDay()
        if (entity.lastMonthlyResetEpochDay != monthStart) {
            dao.resetMonthly(userId, monthStart)
            return@withContext assertQuota(userId) // Retry after reset
        }

        // Check if free tier expired (after 1 year)
        if (entity.freeExpiryEpochDay != null && today > entity.freeExpiryEpochDay) {
            analytics.trackQuotaHit(userId, "expired")
            return@withContext QuotaVerdict.FreeExpired
        }

        // Get daily limit (base + launch bonus)
        val dailyCap = experiments.dailyLimit() + getLaunchBonus(entity.tier)

        // Check quotas
        when {
            entity.dailyUsed >= dailyCap -> {
                analytics.trackQuotaHit(userId, "daily")
                QuotaVerdict.DailyCap(dailyCap)
            }

            entity.tier == "FREE" && entity.monthlyUsed >= 60 -> {
                analytics.trackQuotaHit(userId, "monthly")
                QuotaVerdict.MonthlyCap
            }

            else -> {
                // Allowed - increment usage
                dao.incrementUsage(userId)
                QuotaVerdict.Allowed(dailyCap - entity.dailyUsed - 1)
            }
        }
    }

    private suspend fun createFirstQuota(userId: String, today: Long): UserQuotaEntity {
        // Auto-detect device tier
        val deviceTier = deviceClassifier.getDeviceTier(context)

        // Check for remote override
        val tierOverride = remote.getString("device_tier_override")
        val finalTier = if (tierOverride.isNotBlank()) {
            tierOverride
        } else {
            mapDeviceTierToSubscription(deviceTier)
        }

        // Get launch end date
        val launchEndDate = remote.getString("launch_ends").takeIf { it.isNotBlank() }
            ?.let { LocalDate.parse(it).toEpochDay() }
            ?: (today + 365)

        // Determine retention days
        val isLaunchPeriod = today <= launchEndDate
        val retentionDays = if (isLaunchPeriod) 60 else 30

        val entity = UserQuotaEntity(
            userId = userId,
            tier = finalTier,
            dailyUsed = 0,
            lastResetEpochDay = today,
            monthlyUsed = 0,
            lastMonthlyResetEpochDay = clock.monthStartEpochDay(),
            watermark = (finalTier == "FREE"),
            retentionDays = retentionDays,
            freeExpiryEpochDay = if (finalTier == "FREE") today + 365 else null,
            lastUpgradePrompt = 0,
            upgradePromptCount = 0,
            referredBy = null
        )

        dao.insertOrReplace(entity)

        // Log device tier assignment
        val deviceInfo = deviceClassifier.getDeviceInfo(context)
        analytics.logEvent("device_tier_assigned") {
            param("user_id", userId)
            param("tier", finalTier)
            param("device_tier", deviceTier.name)
            param("manufacturer", deviceInfo.manufacturer)
            param("model", deviceInfo.model)
            param("ram_mb", deviceInfo.ramMB)
            param("cpu_cores", deviceInfo.cpuCores.toLong())
            param("sdk_int", deviceInfo.sdkInt.toLong())
        }

        return entity
    }

    private fun mapDeviceTierToSubscription(deviceTier: DeviceTier): String {
        return when (deviceTier) {
            DeviceTier.LOW_END -> "FREE"
            DeviceTier.MID_RANGE -> "FREE"  // Still free, but better UX
            DeviceTier.HIGH_END -> "FREE"   // Future: Could offer premium trial
        }
    }

    private fun getLaunchBonus(tier: String): Int {
        if (tier != "FREE") return 0

        val launchEndDate = remote.getString("launch_ends").takeIf { it.isNotBlank() }
            ?.let { LocalDate.parse(it) }
            ?: LocalDate.now().plusYears(1)

        return if (clock.today() <= launchEndDate) 1 else 0  // +1 during launch year
    }
}
