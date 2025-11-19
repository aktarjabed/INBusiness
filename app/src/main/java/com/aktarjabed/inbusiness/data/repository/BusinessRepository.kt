package com.aktarjabed.inbusiness.data.repository

import android.util.Log
import com.aktarjabed.inbusiness.data.dao.BusinessDao
import com.aktarjabed.inbusiness.data.entities.BusinessData
import com.aktarjabed.inbusiness.data.entities.CalculationResult
import com.aktarjabed.inbusiness.domain.models.FinancialMetrics
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BusinessRepository @Inject constructor(private val dao: BusinessDao) {

    companion object {
        private const val TAG = "BusinessRepo"
    }

    fun getAllBusinessData(): Flow<List<BusinessData>> = dao.getAllBusinessData()

    suspend fun getBusinessDataById(id: String): BusinessData? =
        runCatching { dao.getBusinessDataById(id) }
            .onFailure { Log.e(TAG, "getBusinessDataById: ${it.message}", it) }
            .getOrNull()

    suspend fun saveBusinessData(data: BusinessData) =
        runCatching { dao.insertBusinessData(data) }
            .onFailure { Log.e(TAG, "saveBusinessData: ${it.message}", it) }

    suspend fun deleteBusinessData(data: BusinessData) =
        runCatching { dao.deleteBusinessData(data) }
            .onFailure { Log.e(TAG, "deleteBusinessData: ${it.message}", it) }

    fun getCalculationResults(businessDataId: String): Flow<List<CalculationResult>> =
        dao.getCalculationResults(businessDataId)

    suspend fun saveCalculationResult(result: CalculationResult) =
        runCatching { dao.insertCalculationResult(result) }
            .onFailure { Log.e(TAG, "saveCalculationResult: ${it.message}", it) }

    /* ============== Business AI-safe calculator ============== */
    fun calculateFinancialMetrics(data: BusinessData): FinancialMetrics {
        return try {
            val revenue = data.unitPrice * data.quantity
            val cogs = data.rawMaterialsCost + data.supplierCosts
            val grossProfit = (revenue - cogs).coerceAtLeast(0.0)

            val operatingExpenses = listOf(
                data.monthlyRent, data.transportCosts, data.labourCosts,
                data.utilityCosts, data.marketingCosts, data.insuranceCosts,
                data.interestCosts
            ).sum()

            val ebitda = (grossProfit - operatingExpenses + data.depreciation).coerceAtLeast(0.0)

            val taxAmount = (revenue * data.incomeTaxSlab / 100.0) + data.tdsAmount
            val netProfit = (ebitda - taxAmount + data.otherIncome).coerceAtLeast(0.0)

            val gstPayable = (data.outputGst - data.inputGst).coerceAtLeast(0.0)

            val contributionMargin = if (data.quantity > 0) {
                data.unitPrice - (data.rawMaterialsCost + data.transportCosts) / data.quantity
            } else 0.0

            val breakEvenPoint = if (contributionMargin > 0) {
                (data.monthlyRent + data.labourCosts) / contributionMargin
            } else 0.0

            val cashFlow = netProfit + data.depreciation

            val grossMargin = if (revenue > 0) (grossProfit / revenue) * 100 else 0.0
            val netMargin = if (revenue > 0) (netProfit / revenue) * 100 else 0.0
            val operatingMargin = if (revenue > 0) (ebitda / revenue) * 100 else 0.0
            val roi = if (cogs + operatingExpenses > 0) (netProfit / (cogs + operatingExpenses)) * 100 else 0.0

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
                val rev = revenue * (1 + month * 0.10)
                val exp = operatingExpenses * (1 + month * 0.05)
                (rev - exp - taxAmount).coerceAtLeast(0.0)
            }

            FinancialMetrics(
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
        } catch (e: Exception) {
            Log.e(TAG, "Calculation error: ${e.message}", e)
            FinancialMetrics() // safe fallback
        }
    }
}