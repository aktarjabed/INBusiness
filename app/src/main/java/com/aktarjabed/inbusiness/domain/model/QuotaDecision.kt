package com.aktarjabed.inbusiness.domain.model

import java.time.LocalDateTime

sealed class QuotaDecision {
    data class Allowed(val remainingToday: Int) : QuotaDecision()
    data class DailyExceeded(val resetsAt: LocalDateTime) : QuotaDecision()
    data class MonthlyExceeded(val resetsAt: LocalDateTime) : QuotaDecision()
    object NeedsReauthentication : QuotaDecision()
    object MaintenanceMode : QuotaDecision()
}