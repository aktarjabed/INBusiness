package com.aktarjabed.inbusiness.domain.model

data class BusinessInput(
    // === BASIC METRICS (Always Required) ===
    val totalRevenue: Double = 0.0,
    val cogs: Double = 0.0,
    val operatingExpenses: Double = 0.0,

    // === ADVANCED OPTIONAL METRICS ===
    val depreciation: Double = 0.0,
    val interestExpense: Double = 0.0,
    val otherIncome: Double = 0.0,
    val extraordinaryItems: Double = 0.0,

    // === CAPITAL EXPENDITURE ===
    val capEx: Double = 0.0,
    val assetSale: Double = 0.0,

    // === WORKING CAPITAL ===
    val accountsReceivable: Double = 0.0,
    val inventory: Double = 0.0,
    val accountsPayable: Double = 0.0,
    val prepaidExpenses: Double = 0.0,

    // === FINANCING ACTIVITIES ===
    val equityRaised: Double = 0.0,
    val loanReceived: Double = 0.0,
    val loanRepayment: Double = 0.0,
    val dividendsPaid: Double = 0.0,

    // === TAX & COMPLIANCE ===
    val advanceTaxPaid: Double = 0.0,
    val tdsReceived: Double = 0.0,
    val tdsPaid: Double = 0.0,

    // === BREAK-EVEN ANALYSIS ===
    val unitsSold: Int = 0,
    val fixedCostsDetailed: Double = 0.0,
    val variableCostPerUnitDetailed: Double = 0.0,
    val expectedUnits: Int = 0,
    val sellingPricePerUnitDetailed: Double = 0.0,

    // === BUSINESS METADATA ===
    val businessType: String = "INDIVIDUAL",
    val taxRegime: String = "NEW_REGIME",
    val gstRegistrationType: String = "REGULAR",
    val industryType: String = "RETAIL",
    val stateOfOperation: String = "INTRA_STATE",
    val businessAgeMonths: Int = 12,
    val numberOfEmployees: Int = 1,

    // === GST DETAILED ===
    val gstRate: Double = 18.0,
    val gstCGST: Double = 0.0,
    val gstSGST: Double = 0.0,
    val gstIGST: Double = 0.0,
    val gstInputCredit: Double = 0.0,
    val gstInputCreditDetailed: Double = 0.0,

    // === TARGETS & BENCHMARKS ===
    val targetROE: Double = 15.0,
    val targetCurrentRatio: Double = 2.0,
    val industryBenchmark: String = "AVERAGE"
)

data class CalculationResults(
    // === PROFITABILITY ===
    val grossProfit: Double,
    val grossProfitMargin: Double,
    val ebitda: Double,
    val ebit: Double,
    val ebt: Double,
    val taxCalculation: TaxCalculation,
    val netProfit: Double,
    val netProfitMargin: Double,

    // === CASH FLOW ===
    val operatingCashFlow: Double,
    val freeCashFlow: Double,
    val investingCashFlow: Double,
    val financingCashFlow: Double,
    val netCashFlow: Double,

    // === BREAK-EVEN ===
    val breakEvenUnits: Int,
    val breakEvenRevenue: Double,
    val contributionMargin: Double,
    val marginOfSafety: Double,

    // === WORKING CAPITAL ===
    val workingCapital: Double,
    val workingCapitalRatio: Double,

    // === FINANCIAL RATIOS ===
    val currentRatio: Double,
    val debtToEquity: Double,
    val roe: Double,
    val roa: Double,

    // === GST ===
    val gstBreakdown: GSTBreakdown,

    // === ADVANCED METRICS ===
    val businessHealthScore: Double,
    val riskLevel: String,
    val industryComparison: IndustryComparison
)

data class TaxCalculation(
    val taxableIncome: Double,
    val taxSlabs: List<TaxSlab>,
    val cess: Double = 0.0,
    val totalTax: Double,
    val effectiveRate: Double
)

data class TaxSlab(
    val from: Double,
    val upTo: Double,
    val rate: Double,
    val taxAmount: Double = 0.0
)

data class GSTBreakdown(
    val outputGST: Double,
    val inputCredit: Double,
    val cgst: Double,
    val sgst: Double,
    val igst: Double,
    val gstPayable: Double
)

data class IndustryComparison(
    val industryType: String,
    val yourGrossMargin: Double,
    val industryGrossMargin: Double,
    val yourNetMargin: Double,
    val industryNetMargin: Double,
    val yourCurrentRatio: Double,
    val industryCurrentRatio: Double,
    val performanceScore: Double
)