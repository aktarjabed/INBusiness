package com.aktarjabed.inbusiness.domain.quota

import java.time.Duration
import java.time.LocalDateTime

sealed class QuotaVerdict {
    data class Allowed(val remaining: Int) : QuotaVerdict()
<<<<<<< HEAD

=======

>>>>>>> ad4bb8454e6b04046f0ba290c1f04d22f25fbfa5
    data class DailyCap(
        val limit: Int,
        val resetTime: LocalDateTime = LocalDateTime.now().plusDays(1)
            .withHour(0).withMinute(0).withSecond(0)
    ) : QuotaVerdict() {
        val resetIn: Duration
            get() = Duration.between(LocalDateTime.now(), resetTime)
    }
<<<<<<< HEAD

=======

>>>>>>> ad4bb8454e6b04046f0ba290c1f04d22f25fbfa5
    object MonthlyCap : QuotaVerdict()
    object FreeExpired : QuotaVerdict()
}