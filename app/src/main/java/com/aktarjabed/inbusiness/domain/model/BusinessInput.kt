package com.aktarjabed.inbusiness.domain.model

data class BusinessInput(
    // 1. Revenue
    val averageOrderValue: String = "1500",
    val monthlyOrders: String = "300",
    val growthRate: String = "5.0",

    // 2. Variable Costs (COGS)
    val materialCostPerUnit: String = "400",
    val packagingCost: String = "20",
    val shippingCost: String = "80",

    // 3. Fixed Costs (OpEx)
    val rent: String = "25000",
    val salaries: String = "150000",
    val marketingBudget: String = "50000",
    val softwareSubscriptions: String = "5000",

    // 4. One-time (CapEx)
    val equipmentCost: String = "200000",
    val licenseFees: String = "15000",
    val initialInventory: String = "100000",

    // 5. Taxation (GST/Income)
    val gstRate: String = "18.0",
    val incomeTaxRate: String = "25.0",
    val professionalTax: String = "200"
)

data class CalculationResults(
    val grossRevenue: Double = 0.0,
    val totalCOGS: Double = 0.0,
    val grossProfit: Double = 0.0,
    val grossMarginPercent: Double = 0.0,

    val totalFixedCosts: Double = 0.0,
    val operatingProfit: Double = 0.0, // EBITDA

    val netProfitBeforeTax: Double = 0.0,
    val estimatedTax: Double = 0.0,
    val netProfitAfterTax: Double = 0.0,
    val netMarginPercent: Double = 0.0,

    val breakEvenUnits: Int = 0,
    val breakEvenRevenue: Double = 0.0,

    val roiMonths: Double = 0.0, // Payback period
    val roiPercent: Double = 0.0 // Annualized ROI
)

data class TaxSlab(
    val minIncome: Double,
    val rate: Double
)

data class IndustryComparison(
    val industry: String,
    val avgMargin: Double,
    val yourMargin: Double
)
