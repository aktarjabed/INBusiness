package com.aktarjabed.inbusiness.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aktarjabed.inbusiness.domain.model.BusinessInput
import com.aktarjabed.inbusiness.domain.model.CalculationResults
import com.aktarjabed.inbusiness.domain.model.GSTBreakdown
import com.aktarjabed.inbusiness.domain.model.IndustryComparison
import com.aktarjabed.inbusiness.domain.model.TaxCalculation
import com.aktarjabed.inbusiness.domain.model.TaxSlab
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject
import kotlin.math.ceil

@HiltViewModel
class CalculatorViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(CalculatorUiState())
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    // === BASIC INPUTS (Always Visible) ===
    fun updateRevenue(value: String) = updateInput { it.copy(totalRevenue = value.toDoubleOrNull() ?: 0.0) }
    fun updateCOGS(value: String) = updateInput { it.copy(cogs = value.toDoubleOrNull() ?: 0.0) }
    fun updateOperatingExpenses(value: String) = updateInput { it.copy(operatingExpenses = value.toDoubleOrNull() ?: 0.0) }

    // === ADVANCED OPTIONAL INPUTS ===
    fun updateDepreciation(value: String) = updateInput { it.copy(depreciation = value.toDoubleOrNull() ?: 0.0) }
    fun updateInterestExpense(value: String) = updateInput { it.copy(interestExpense = value.toDoubleOrNull() ?: 0.0) }
    fun updateOtherIncome(value: String) = updateInput { it.copy(otherIncome = value.toDoubleOrNull() ?: 0.0) }
    fun updateExtraordinaryItems(value: String) = updateInput { it.copy(extraordinaryItems = value.toDoubleOrNull() ?: 0.0) }

    // === CAPITAL EXPENDITURE ===
    fun updateCapEx(value: String) = updateInput { it.copy(capEx = value.toDoubleOrNull() ?: 0.0) }
    fun updateAssetSale(value: String) = updateInput { it.copy(assetSale = value.toDoubleOrNull() ?: 0.0) }

    // === WORKING CAPITAL ===
    fun updateAccountsReceivable(value: String) = updateInput { it.copy(accountsReceivable = value.toDoubleOrNull() ?: 0.0) }
    fun updateInventory(value: String) = updateInput { it.copy(inventory = value.toDoubleOrNull() ?: 0.0) }
    fun updateAccountsPayable(value: String) = updateInput { it.copy(accountsPayable = value.toDoubleOrNull() ?: 0.0) }
    fun updatePrepaidExpenses(value: String) = updateInput { it.copy(prepaidExpenses = value.toDoubleOrNull() ?: 0.0) }

    // === FINANCING ===
    fun updateEquityRaised(value: String) = updateInput { it.copy(equityRaised = value.toDoubleOrNull() ?: 0.0) }
    fun updateLoanReceived(value: String) = updateInput { it.copy(loanReceived = value.toDoubleOrNull() ?: 0.0) }
    fun updateLoanRepayment(value: String) = updateInput { it.copy(loanRepayment = value.toDoubleOrNull() ?: 0.0) }
    fun updateDividendsPaid(value: String) = updateInput { it.copy(dividendsPaid = value.toDoubleOrNull() ?: 0.0) }

    // === TAX & COMPLIANCE ===
    fun updateAdvanceTaxPaid(value: String) = updateInput { it.copy(advanceTaxPaid = value.toDoubleOrNull() ?: 0.0) }
    fun updateTDSReceived(value: String) = updateInput { it.copy(tdsReceived = value.toDoubleOrNull() ?: 0.0) }
    fun updateTDSPaid(value: String) = updateInput { it.copy(tdsPaid = value.toDoubleOrNull() ?: 0.0) }

    // === BREAK-EVEN DETAILED ===
    fun updateUnitsSold(value: String) = updateInput { it.copy(unitsSold = value.toIntOrNull() ?: 0) }
    fun updateFixedCostsDetailed(value: String) = updateInput { it.copy(fixedCostsDetailed = value.toDoubleOrNull() ?: 0.0) }
    fun updateVariableCostPerUnitDetailed(value: String) = updateInput { it.copy(variableCostPerUnitDetailed = value.toDoubleOrNull() ?: 0.0) }
    fun updateExpectedUnits(value: String) = updateInput { it.copy(expectedUnits = value.toIntOrNull() ?: 0) }
    fun updateSellingPricePerUnitDetailed(value: String) = updateInput { it.copy(sellingPricePerUnitDetailed = value.toDoubleOrNull() ?: 0.0) }

    // === RATIO ANALYSIS ===
    fun updateIndustryBenchmark(value: String) = updateInput { it.copy(industryBenchmark = value) }
    fun updateTargetROE(value: String) = updateInput { it.copy(targetROE = value.toDoubleOrNull() ?: 0.0) }
    fun updateTargetCurrentRatio(value: String) = updateInput { it.copy(targetCurrentRatio = value.toDoubleOrNull() ?: 0.0) }

    // === GST DETAILED ===
    fun updateGSTCGST(value: String) = updateInput { it.copy(gstCGST = value.toDoubleOrNull() ?: 0.0) }
    fun updateGSTSGST(value: String) = updateInput { it.copy(gstSGST = value.toDoubleOrNull() ?: 0.0) }
    fun updateGSTIGST(value: String) = updateInput { it.copy(gstIGST = value.toDoubleOrNull() ?: 0.0) }
    fun updateGSTInputCreditDetailed(value: String) = updateInput { it.copy(gstInputCreditDetailed = value.toDoubleOrNull() ?: 0.0) }

    // === BUSINESS METADATA ===
    fun updateBusinessAgeMonths(value: String) = updateInput { it.copy(businessAgeMonths = value.toIntOrNull() ?: 0) }
    fun updateNumberOfEmployees(value: String) = updateInput { it.copy(numberOfEmployees = value.toIntOrNull() ?: 0) }
    fun updateIndustryType(value: String) = updateInput { it.copy(industryType = value) }
    fun updateStateOfOperation(value: String) = updateInput { it.copy(stateOfOperation = value) }

    // === ADVANCED BUSINESS TYPE ===
    fun updateBusinessType(value: String) = updateInput { it.copy(businessType = value) }
    fun updateTaxRegime(value: String) = updateInput { it.copy(taxRegime = value) }
    fun updateGSTRegistrationType(value: String) = updateInput { it.copy(gstRegistrationType = value) }

    private fun updateInput(transform: (BusinessInput) -> BusinessInput) {
        _uiState.update { current ->
            current.copy(input = transform(current.input))
        }
        recalculate()
    }

    private fun recalculate() {
        viewModelScope.launch {
            try {
                val input = _uiState.value.input

                // === REVENUE & PROFITABILITY ===
                val grossProfit = input.totalRevenue - input.cogs
                val grossProfitMargin = if (input.totalRevenue > 0) (grossProfit / input.totalRevenue) * 100 else 0.0

                val ebitda = grossProfit - input.operatingExpenses
                val ebit = ebitda - input.depreciation
                val ebt = ebit - input.interestExpense + input.otherIncome - input.extraordinaryItems

                // === TAX CALCULATION ===
                val taxCalc = calculateIncomeTax(ebt, input.businessType, input.taxRegime)
                val netProfit = ebt - taxCalc.totalTax
                val netProfitMargin = if (input.totalRevenue > 0) (netProfit / input.totalRevenue) * 100 else 0.0

                // === CASH FLOW ANALYSIS ===
                val operatingCashFlow = netProfit + input.depreciation
                val investingCashFlow = -input.capEx + input.assetSale
                val financingCashFlow = input.equityRaised + input.loanReceived - input.loanRepayment - input.dividendsPaid
                val netCashFlow = operatingCashFlow + investingCashFlow + financingCashFlow

                // === WORKING CAPITAL ===
                val workingCapital = input.accountsReceivable + input.inventory + input.prepaidExpenses - input.accountsPayable
                val workingCapitalRatio = if (input.accountsPayable > 0) workingCapital / input.accountsPayable else 0.0

                // === BREAK-EVEN ANALYSIS ===
                val contributionMarginPerUnit = input.sellingPricePerUnitDetailed - input.variableCostPerUnitDetailed
                val breakEvenUnits = if (contributionMarginPerUnit > 0)
                    ceil(input.fixedCostsDetailed / contributionMarginPerUnit).toInt() else 0
                val breakEvenRevenueDetailed = breakEvenUnits * input.sellingPricePerUnitDetailed
                val marginOfSafety = if (input.expectedUnits > breakEvenUnits)
                    ((input.expectedUnits - breakEvenUnits).toDouble() / input.expectedUnits) * 100 else 0.0

                // === FINANCIAL RATIOS ===
                val currentRatio = if (input.accountsPayable > 0) workingCapital / input.accountsPayable else 0.0
                val debtToEquity = if (input.equityRaised > 0) input.loanReceived / input.equityRaised else 0.0
                val roe = if (input.equityRaised > 0) (netProfit / input.equityRaised) * 100 else 0.0
                val roa = if (input.capEx > 0) (netProfit / input.capEx) * 100 else 0.0

                // === GST DETAILED ===
                val gstBreakdown = calculateGSTDetailed(input)

                // === BUSINESS HEALTH METRICS ===
                val businessHealthScore = calculateBusinessHealthScore(input, netProfit, workingCapitalRatio, currentRatio, roe)
                val riskLevel = calculateRiskLevel(debtToEquity, currentRatio, netProfitMargin)

                // === INDUSTRY COMPARISON ===
                val industryComparison = compareToIndustry(input.industryType, grossProfitMargin, netProfitMargin, currentRatio)

                val results = CalculationResults(
                    // Basic metrics
                    grossProfit = grossProfit,
                    grossProfitMargin = grossProfitMargin,
                    ebitda = ebitda,
                    ebit = ebit,
                    ebt = ebt,
                    taxCalculation = taxCalc,
                    netProfit = netProfit,
                    netProfitMargin = netProfitMargin,

                    // Cash flow
                    operatingCashFlow = operatingCashFlow,
                    freeCashFlow = netCashFlow, // Using netCashFlow as freeCashFlow placeholder or vice versa, adjusting to match domain
                    investingCashFlow = investingCashFlow,
                    financingCashFlow = financingCashFlow,
                    netCashFlow = netCashFlow,

                    // Break-even
                    breakEvenUnits = breakEvenUnits,
                    breakEvenRevenue = breakEvenRevenueDetailed,
                    contributionMargin = contributionMarginPerUnit,
                    marginOfSafety = marginOfSafety,

                    // Working capital
                    workingCapital = workingCapital,
                    workingCapitalRatio = workingCapitalRatio,

                    // Ratios
                    currentRatio = currentRatio,
                    debtToEquity = debtToEquity,
                    roe = roe,
                    roa = roa,

                    // GST
                    gstBreakdown = gstBreakdown,

                    // Advanced metrics
                    businessHealthScore = businessHealthScore,
                    riskLevel = riskLevel,
                    industryComparison = industryComparison
                )

                _uiState.update { current ->
                    current.copy(
                        results = results,
                        isLoading = false,
                        error = null
                    )
                }

                Timber.d("Advanced calculations completed: NP=%.2f, FCF=%.2f, Health Score=%.1f",
                    netProfit, netCashFlow, businessHealthScore)

            } catch (e: Exception) {
                Timber.e(e, "Advanced calculation error")
                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        error = "Calculation failed: ${e.message}"
                    )
                }
            }
        }
    }

    private fun calculateIncomeTax(
        ebt: Double,
        businessType: String,
        taxRegime: String
    ): TaxCalculation {
        if (ebt <= 0) return TaxCalculation(taxableIncome = 0.0, taxSlabs = emptyList(), totalTax = 0.0, effectiveRate = 0.0)

        val slabs = when (businessType) {
            "INDIVIDUAL" -> when (taxRegime) {
                "NEW_REGIME" -> listOf(
                    TaxSlab(0.0, 300000.0, 0.0),
                    TaxSlab(300000.0, 600000.0, 5.0),
                    TaxSlab(600000.0, 900000.0, 10.0),
                    TaxSlab(900000.0, 1200000.0, 15.0),
                    TaxSlab(1200000.0, 1500000.0, 20.0),
                    TaxSlab(1500000.0, Double.MAX_VALUE, 30.0)
                )
                "OLD_REGIME" -> listOf(
                    TaxSlab(0.0, 250000.0, 0.0),
                    TaxSlab(250000.0, 500000.0, 5.0),
                    TaxSlab(500000.0, 1000000.0, 20.0),
                    TaxSlab(1000000.0, Double.MAX_VALUE, 30.0)
                )
                else -> listOf(TaxSlab(0.0, Double.MAX_VALUE, 30.0))
            }
            "COMPANY" -> listOf(
                TaxSlab(0.0, 10000000.0, 25.0),
                TaxSlab(10000000.0, Double.MAX_VALUE, 30.0)
            )
            else -> listOf(TaxSlab(0.0, Double.MAX_VALUE, 30.0))
        }

        var remainingIncome = ebt
        var totalTax = 0.0
        val appliedSlabs = mutableListOf<TaxSlab>()

        for (slab in slabs) {
            if (remainingIncome <= 0) break

            val taxableInSlab = when {
                ebt <= slab.upTo -> minOf(remainingIncome, ebt - slab.from)
                else -> minOf(remainingIncome, slab.upTo - slab.from)
            }

            if (taxableInSlab > 0) {
                val taxInSlab = taxableInSlab * (slab.rate / 100.0)
                totalTax += taxInSlab
                appliedSlabs.add(slab.copy(taxAmount = taxInSlab))
                remainingIncome -= taxableInSlab
            }
        }

        // Add cess (4%)
        val cess = totalTax * 0.04
        val totalWithCess = totalTax + cess

        val effectiveRate = if (ebt > 0) (totalWithCess / ebt) * 100 else 0.0

        return TaxCalculation(
            taxableIncome = ebt,
            taxSlabs = appliedSlabs,
            cess = cess,
            totalTax = totalWithCess,
            effectiveRate = effectiveRate
        )
    }

    private fun calculateGSTDetailed(input: BusinessInput): GSTBreakdown {
        val gstAmount = input.totalRevenue * (input.gstRate / 100.0)

        // Use detailed inputs if provided, otherwise calculate
        val cgst = if (input.gstCGST > 0) input.gstCGST else if (input.stateOfOperation != "INTER_STATE") gstAmount / 2 else 0.0
        val sgst = if (input.gstSGST > 0) input.gstSGST else if (input.stateOfOperation != "INTER_STATE") gstAmount / 2 else 0.0
        val igst = if (input.gstIGST > 0) input.gstIGST else if (input.stateOfOperation == "INTER_STATE") gstAmount else 0.0

        val inputCredit = if (input.gstInputCreditDetailed > 0) input.gstInputCreditDetailed else input.gstInputCredit
        val gstPayable = maxOf(0.0, (cgst + sgst + igst) - inputCredit)

        return GSTBreakdown(
            outputGST = cgst + sgst + igst,
            inputCredit = inputCredit,
            cgst = cgst,
            sgst = sgst,
            igst = igst,
            gstPayable = gstPayable
        )
    }

    private fun calculateBusinessHealthScore(
        input: BusinessInput,
        netProfit: Double,
        workingCapitalRatio: Double,
        currentRatio: Double,
        roe: Double
    ): Double {
        var score = 50.0 // Base score

        // Profitability (30 points)
        val profitMargin = if (input.totalRevenue > 0) (netProfit / input.totalRevenue) * 100 else 0.0
        score += when {
            profitMargin >= 20 -> 30.0
            profitMargin >= 15 -> 25.0
            profitMargin >= 10 -> 20.0
            profitMargin >= 5 -> 15.0
            profitMargin >= 0 -> 10.0
            else -> 0.0
        }

        // Liquidity (20 points)
        score += when {
            currentRatio >= 2.0 -> 20.0
            currentRatio >= 1.5 -> 15.0
            currentRatio >= 1.0 -> 10.0
            else -> 5.0
        }

        // Working Capital (15 points)
        score += when {
            workingCapitalRatio >= 1.2 -> 15.0
            workingCapitalRatio >= 1.0 -> 12.0
            workingCapitalRatio >= 0.8 -> 8.0
            else -> 4.0
        }

        // ROE (15 points)
        score += when {
            roe >= 20 -> 15.0
            roe >= 15 -> 12.0
            roe >= 10 -> 9.0
            roe >= 5 -> 6.0
            else -> 3.0
        }

        return minOf(score, 100.0)
    }

    private fun calculateRiskLevel(
        debtToEquity: Double,
        currentRatio: Double,
        netProfitMargin: Double
    ): String {
        var riskScore = 0

        if (debtToEquity > 2.0) riskScore += 3
        else if (debtToEquity > 1.0) riskScore += 2
        else if (debtToEquity > 0.5) riskScore += 1

        if (currentRatio < 1.0) riskScore += 3
        else if (currentRatio < 1.5) riskScore += 2
        else if (currentRatio < 2.0) riskScore += 1

        if (netProfitMargin < 0) riskScore += 3
        else if (netProfitMargin < 5) riskScore += 2
        else if (netProfitMargin < 10) riskScore += 1

        return when {
            riskScore >= 7 -> "HIGH"
            riskScore >= 4 -> "MEDIUM"
            else -> "LOW"
        }
    }

    private fun compareToIndustry(
        industryType: String,
        grossProfitMargin: Double,
        netProfitMargin: Double,
        currentRatio: Double
    ): IndustryComparison {
        // Industry benchmarks (simplified)
        val benchmarks = when (industryType) {
            "RETAIL" -> IndustryBenchmarks(grossMargin = 30.0, netMargin = 8.0, currentRatio = 2.0)
            "MANUFACTURING" -> IndustryBenchmarks(grossMargin = 35.0, netMargin = 10.0, currentRatio = 1.8)
            "SERVICES" -> IndustryBenchmarks(grossMargin = 50.0, netMargin = 15.0, currentRatio = 2.2)
            "IT_SERVICES" -> IndustryBenchmarks(grossMargin = 60.0, netMargin = 20.0, currentRatio = 2.5)
            "RESTAURANT" -> IndustryBenchmarks(grossMargin = 25.0, netMargin = 5.0, currentRatio = 1.5)
            else -> IndustryBenchmarks(grossMargin = 30.0, netMargin = 10.0, currentRatio = 2.0)
        }

        return IndustryComparison(
            industryType = industryType,
            yourGrossMargin = grossProfitMargin,
            industryGrossMargin = benchmarks.grossMargin,
            yourNetMargin = netProfitMargin,
            industryNetMargin = benchmarks.netMargin,
            yourCurrentRatio = currentRatio,
            industryCurrentRatio = benchmarks.currentRatio,
            performanceScore = calculatePerformanceScore(grossProfitMargin, netProfitMargin, currentRatio, benchmarks)
        )
    }

    private fun calculatePerformanceScore(
        yourGross: Double,
        yourNet: Double,
        yourCurrent: Double,
        benchmarks: IndustryBenchmarks
    ): Double {
        var score = 0.0

        // Gross margin comparison
        score += when {
            yourGross >= benchmarks.grossMargin -> 40.0
            yourGross >= benchmarks.grossMargin * 0.9 -> 30.0
            yourGross >= benchmarks.grossMargin * 0.8 -> 20.0
            else -> 10.0
        }

        // Net margin comparison
        score += when {
            yourNet >= benchmarks.netMargin -> 40.0
            yourNet >= benchmarks.netMargin * 0.9 -> 30.0
            yourNet >= benchmarks.netMargin * 0.8 -> 20.0
            else -> 10.0
        }

        // Current ratio comparison
        score += when {
            yourCurrent >= benchmarks.currentRatio -> 20.0
            yourCurrent >= benchmarks.currentRatio * 0.9 -> 15.0
            yourCurrent >= benchmarks.currentRatio * 0.8 -> 10.0
            else -> 5.0
        }

        return score
    }

    fun resetCalculator() {
        _uiState.value = CalculatorUiState()
    }

    fun toggleAdvancedInputs() {
        _uiState.update { current ->
            current.copy(showAdvancedInputs = !current.showAdvancedInputs)
        }
    }

    fun setQuickMode(mode: String) {
        _uiState.update { current ->
            current.copy(quickMode = mode)
        }
        applyQuickMode(mode)
    }

    private fun applyQuickMode(mode: String) {
        val basicInput = when (mode) {
            "RETAIL_SHOP" -> BusinessInput(
                totalRevenue = 5000000.0,
                cogs = 3500000.0,
                operatingExpenses = 800000.0,
                gstRate = 18.0,
                industryType = "RETAIL"
            )
            "CONSULTING" -> BusinessInput(
                totalRevenue = 2000000.0,
                cogs = 200000.0,
                operatingExpenses = 400000.0,
                gstRate = 18.0,
                industryType = "SERVICES"
            )
            "MANUFACTURING" -> BusinessInput(
                totalRevenue = 10000000.0,
                cogs = 6500000.0,
                operatingExpenses = 2000000.0,
                gstRate = 18.0,
                industryType = "MANUFACTURING"
            )
            else -> BusinessInput()
        }

        _uiState.update { current ->
            current.copy(input = basicInput)
        }
        recalculate()
    }
}

// === UI STATE ===
data class CalculatorUiState(
    val input: BusinessInput = BusinessInput(),
    val results: CalculationResults? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showAdvancedInputs: Boolean = false,
    val quickMode: String = "CUSTOM",
    val activeTab: Int = 0 // 0: Basic, 1: Advanced, 2: Ratios
)

// === SUPPORTING DATA CLASSES ===
data class IndustryBenchmarks(
    val grossMargin: Double,
    val netMargin: Double,
    val currentRatio: Double
)