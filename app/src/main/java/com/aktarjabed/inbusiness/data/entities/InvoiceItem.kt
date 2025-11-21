package com.aktarjabed.inbusiness.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "invoice_items")
data class InvoiceItem(
    @PrimaryKey val id: String,
    val invoiceId: String,
    val description: String,
    val quantity: Double,
    val unitPrice: Double,
    val totalAmount: Double
)