package com.aktarjabed.inbusiness.domain.models

data class Anomaly(
    val type: Type,
    val severity: Severity,
    val score: Double, // 0.0 to 1.0
    val title: String,
    val message: String,
    val suggestion: String,
    val field: String? = null
) {
    enum class Type {
        AMOUNT_OUTLIER,
        TAX_ANOMALY,
        DUPLICATE,
        TIMING_ANOMALY,
        PATTERN_ANOMALY,
        DATA_QUALITY
    }

    enum class Severity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    fun getColor(): Long {
        return when (severity) {
            Severity.LOW -> 0xFFFFC107 // Amber
            Severity.MEDIUM -> 0xFFFF9800 // Orange
            Severity.HIGH -> 0xFFF44336 // Red
            Severity.CRITICAL -> 0xFFB71C1C // Dark Red
        }
    }
}