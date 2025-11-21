package com.aktarjabed.inbusiness.data.local.entities

import androidx.room.Entity
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
    @PrimaryKey val id: String = generateId(),
    val invoiceId: String,
    val description: String,
    val quantity: Double,
    val unitPrice: Double,
    val discount: Double = 0.0,
    val taxRate: Double = 0.0,
    val totalAmount: Double,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun generateId(): String = "item_${System.currentTimeMillis()}_${(0..9999).random()}"
    }
}