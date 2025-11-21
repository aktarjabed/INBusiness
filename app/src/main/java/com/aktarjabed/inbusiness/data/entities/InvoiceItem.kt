package com.aktarjabed.inbusiness.data.entities

import androidx.room.Entity
<<<<<<< HEAD
import androidx.room.PrimaryKey

@Entity(tableName = "invoice_items")
data class InvoiceItem(
    @PrimaryKey val id: String,
    val invoiceId: String,
    val description: String,
    val quantity: Double,
    val unitPrice: Double,
    val totalAmount: Double
=======
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "invoice_items",
    foreignKeys = [
        ForeignKey(
            entity = Invoice::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("invoiceId")]
)
data class InvoiceItem(
    @PrimaryKey val id: String = "",
    val invoiceId: String = "",
    val description: String = "",
    val quantity: Double = 0.0,
    val unitPrice: Double = 0.0,
    val taxRate: Double = 0.0,
    val amount: Double = 0.0
>>>>>>> ad4bb8454e6b04046f0ba290c1f04d22f25fbfa5
)