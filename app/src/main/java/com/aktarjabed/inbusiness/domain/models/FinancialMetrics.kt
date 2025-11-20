package com.aktarjabed.inbusiness.domain.models

data class FinancialMetrics(
    val revenue: Double = 0.0,
    val cogs: Double = 0.0,
    val grossProfit: Double = 0.0,
    val operatingExpenses: Double = 0.0,
    val ebitda: Double = 0.0,
    val netProfit: Double = 0.0,
    val gstPayable: Double = 0.0,
    val breakEvenPoint: Double = 0.0,
    val cashFlow: Double = 0.0,
    val grossMargin: Double = 0.0,
    val netMargin: Double = 0.0,
    val operatingMargin: Double = 0.0,
    val roi: Double = 0.0,
    val expenseBreakdown: Map<String, Double> = emptyMap(),
    val profitProjection: List<Double> = emptyList()
)