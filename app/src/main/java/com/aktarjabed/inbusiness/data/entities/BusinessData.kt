package com.aktarjabed.inbusiness.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

enum class GstType { SGST_CGST, IGST }

@Entity(tableName = "business_data")
data class BusinessData(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val scenarioName: String = "Scenario ${System.currentTimeMillis()}",
    val rawMaterialsCost: Double = 0.0,
    val supplierCosts: Double = 0.0,
    val inputGst: Double = 0.0,
    val unitPrice: Double = 0.0,
    val quantity: Int = 0,
    val outputGst: Double = 0.0,
    val gstType: GstType = GstType.SGST_CGST,
    val incomeTaxSlab: Double = 0.0,
    val tdsAmount: Double = 0.0,
    val corporateTaxRate: Double = 0.0,
    val gstRate: Double = 0.0,
    val hsnCode: String = "",
    val monthlyRent: Double = 0.0,
    val transportCosts: Double = 0.0,
    val labourCosts: Double = 0.0,
    val utilityCosts: Double = 0.0,
    val marketingCosts: Double = 0.0,
    val insuranceCosts: Double = 0.0,
    val depreciation: Double = 0.0,
    val interestCosts: Double = 0.0,
    val inventoryLevel: Int = 0,
    val otherIncome: Double = 0.0,
    val createdAt: Instant = Instant.now()
)

@Entity(tableName = "calculation_results")
data class CalculationResult(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val businessDataId: String = "",
    val grossProfit: Double = 0.0,
    val ebitda: Double = 0.0,
    val netProfit: Double = 0.0,
    val gstPayable: Double = 0.0,
    val breakEvenPoint: Double = 0.0,
    val cashFlow: Double = 0.0,
    val grossMargin: Double = 0.0,
    val netMargin: Double = 0.0,
    val operatingMargin: Double = 0.0,
    val roi: Double = 0.0,
    val calculatedAt: Instant = Instant.now()
)