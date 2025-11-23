package com.aktarjabed.inbusiness.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aktarjabed.inbusiness.domain.model.BusinessInput
import com.aktarjabed.inbusiness.domain.model.CalculationResults
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class CalculatorViewModel @Inject constructor() : ViewModel() {

    private val _inputs = MutableStateFlow(BusinessInput())
    val inputs: StateFlow<BusinessInput> = _inputs.asStateFlow()

    private val _results = MutableStateFlow(CalculationResults())
    val results: StateFlow<CalculationResults> = _results.asStateFlow()

    init {
        calculate()
    }

    fun updateInput(update: (BusinessInput) -> BusinessInput) {
        _inputs.update(update)
        calculate()
    }

    private fun calculate() {
        viewModelScope.launch {
            val i = _inputs.value

            // Parsing
            val aov = i.averageOrderValue.toDoubleOrZero()
            val orders = i.monthlyOrders.toDoubleOrZero()

            // 1. Revenue
            val grossRevenue = aov * orders

            // 2. Variable Costs
            val unitVariableCost = i.materialCostPerUnit.toDoubleOrZero() +
                                  i.packagingCost.toDoubleOrZero() +
                                  i.shippingCost.toDoubleOrZero()
            val totalCOGS = unitVariableCost * orders
            val grossProfit = grossRevenue - totalCOGS
            val grossMargin = if (grossRevenue > 0) (grossProfit / grossRevenue) * 100 else 0.0

            // 3. Fixed Costs
            val fixedCosts = i.rent.toDoubleOrZero() +
                            i.salaries.toDoubleOrZero() +
                            i.marketingBudget.toDoubleOrZero() +
                            i.softwareSubscriptions.toDoubleOrZero() +
                            i.professionalTax.toDoubleOrZero()

            // 4. Operating Profit (EBITDA)
            val operatingProfit = grossProfit - fixedCosts

            // 5. Taxes (Simplified)
            val gstOut = grossRevenue * (i.gstRate.toDoubleOrZero() / 100)
            val gstIn = totalCOGS * (i.gstRate.toDoubleOrZero() / 100) // Input credit
            val gstPayable = (gstOut - gstIn).coerceAtLeast(0.0)

            val taxBase = operatingProfit.coerceAtLeast(0.0)
            val incomeTax = taxBase * (i.incomeTaxRate.toDoubleOrZero() / 100)

            val netProfit = operatingProfit - incomeTax
            val netMargin = if (grossRevenue > 0) (netProfit / grossRevenue) * 100 else 0.0

            // 6. Break-even
            val contributionMargin = aov - unitVariableCost
            val breakEvenUnits = if (contributionMargin > 0) (fixedCosts / contributionMargin).roundToInt() else 0
            val breakEvenRevenue = breakEvenUnits * aov

            // 7. ROI / Payback
            val capex = i.equipmentCost.toDoubleOrZero() +
                       i.licenseFees.toDoubleOrZero() +
                       i.initialInventory.toDoubleOrZero()

            val roiMonths = if (netProfit > 0) capex / netProfit else 0.0
            val annualRoi = if (capex > 0) ((netProfit * 12) / capex) * 100 else 0.0

            _results.value = CalculationResults(
                grossRevenue = grossRevenue,
                totalCOGS = totalCOGS,
                grossProfit = grossProfit,
                grossMarginPercent = grossMargin,
                totalFixedCosts = fixedCosts,
                operatingProfit = operatingProfit,
                netProfitBeforeTax = operatingProfit,
                estimatedTax = incomeTax,
                netProfitAfterTax = netProfit,
                netMarginPercent = netMargin,
                breakEvenUnits = breakEvenUnits,
                breakEvenRevenue = breakEvenRevenue,
                roiMonths = roiMonths,
                roiPercent = annualRoi
            )
        }
    }

    private fun String.toDoubleOrZero() = this.toDoubleOrNull() ?: 0.0
}
