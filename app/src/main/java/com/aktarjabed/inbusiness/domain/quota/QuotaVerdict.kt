package com.aktarjabed.inbusiness.domain.quota

import com.aktarjabed.inbusiness.utils.AppDateUtils
import java.time.Duration
import java.time.LocalDateTime

sealed class QuotaVerdict {
    data class Allowed(val remaining: Int) : QuotaVerdict()

    /**
     * Daily limit reached.
     *
     * [resetTime] is the next business-day boundary in the authoritative business
     * timezone (Asia/Kolkata), so the "resets at" countdown matches the moment the
     * SQL quota windows actually roll over, regardless of the device timezone.
     */
    data class DailyCap(
        val limit: Int,
        val resetTime: LocalDateTime = LocalDateTime.now(AppDateUtils.businessZoneId)
            .plusDays(1)
            .withHour(0).withMinute(0).withSecond(0).withNano(0)
    ) : QuotaVerdict() {
        val resetIn: Duration
            get() = Duration.between(
                LocalDateTime.now(AppDateUtils.businessZoneId),
                resetTime
            ).let { if (it.isNegative) Duration.ZERO else it }
    }

    object MonthlyCap : QuotaVerdict()
    object FreeExpired : QuotaVerdict()
}
