package com.aktarjabed.inbusiness.domain.usecase

import com.aktarjabed.inbusiness.data.entities.BusinessData
import com.aktarjabed.inbusiness.domain.models.FinancialMetrics
import javax.inject.Inject

class CalculateMetricsUseCase @Inject constructor() {
    operator fun invoke(data: BusinessData): FinancialMetrics {
        val revenue = data.unitPrice * data.quantity
        val cogs = data.rawMaterialsCost + data.supplierCosts
        val grossProfit = revenue - cogs

        val operatingExpenses = data.monthlyRent + data.transportCosts + data.labourCosts +
                               data.utilityCosts + data.marketingCosts + data.insuranceCosts +
                               data.interestCosts

        val ebitda = grossProfit - operatingExpenses + data.depreciation

        val taxAmount = (revenue * data.incomeTaxSlab / 100) + data.tdsAmount
        val netProfit = ebitda - taxAmount + data.otherIncome

        val gstPayable = data.outputGst - data.inputGst

        val variableCostPerUnit = (data.rawMaterialsCost + data.transportCosts) /
                                 if (data.quantity > 0) data.quantity else 1
        val breakEvenPoint = if (data.unitPrice - variableCostPerUnit > 0) {
            (data.monthlyRent + data.labourCosts) / (data.unitPrice - variableCostPerUnit)
        } else 0.0

        val cashFlow = netProfit + data.depreciation

        val grossMargin = if (revenue > 0) (grossProfit / revenue) * 100 else 0.0
        val netMargin = if (revenue > 0) (netProfit / revenue) * 100 else 0.0
        val operatingMargin = if (revenue > 0) (ebitda / revenue) * 100 else 0.0
        val roi = if (cogs + operatingExpenses > 0) {
            (netProfit / (cogs + operatingExpenses)) * 100
        } else 0.0

        val expenseBreakdown = mapOf(
            "Raw Materials" to data.rawMaterialsCost,
            "Supplier Costs" to data.supplierCosts,
            "Rent" to data.monthlyRent,
            "Transport" to data.transportCosts,
            "Labour" to data.labourCosts,
            "Utilities" to data.utilityCosts,
            "Marketing" to data.marketingCosts,
            "Insurance" to data.insuranceCosts,
            "Interest" to data.interestCosts
        )

        val profitProjection = List(6) { month ->
            val monthRevenue = revenue * (1 + month * 0.1)
            val monthExpenses = operatingExpenses * (1 + month * 0.05)
            monthRevenue - monthExpenses - taxAmount
        }

        return FinancialMetrics(
            revenue = revenue,
            cogs = cogs,
            grossProfit = grossProfit,
            operatingExpenses = operatingExpenses,
            ebitda = ebitda,
            netProfit = netProfit,
            gstPayable = gstPayable,
            breakEvenPoint = breakEvenPoint,
            cashFlow = cashFlow,
            grossMargin = grossMargin,
            netMargin = netMargin,
            operatingMargin = operatingMargin,
            roi = roi,
            expenseBreakdown = expenseBreakdown,
            profitProjection = profitProjection
        )
    }
}