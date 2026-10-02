package com.aktarjabed.inbusiness.data.dao

/**
 * Lightweight projection used to build invoice-item autocomplete suggestions.
 *
 * Historical suggestion queries do not need persisted invoice totals, line quantities, or
 * invoice identifiers; projecting only these fields avoids materializing full InvoiceItem
 * entities for every historical line whenever Room invalidates invoice history.
 */
data class InvoiceItemSuggestion(
    val description: String,
    val pricePerUnit: Double,
    val unitType: String,
    val productId: Long?,
    val gstPercentage: Double
)
