package com.aktarjabed.inbusiness.domain.quotas

import java.time.Duration
import java.time.LocalDateTime

sealed class QuotaVerdict {
    data class Allowed(val remaining: Int) : QuotaVerdict()

    data class DailyCap(
        val limit: Int,
        val resetTime: LocalDateTime = LocalDateTime.now().plusDays(1).withHour(0).withMinute(0)
    ) : QuotaVerdict() {
        val resetIn: Duration
            get() = Duration.between(LocalDateTime.now(), resetTime)
    }

    object MonthlyCap : QuotaVerdict()

    object FreeExpired : QuotaVerdict()

    object Killed : QuotaVerdict()
}
