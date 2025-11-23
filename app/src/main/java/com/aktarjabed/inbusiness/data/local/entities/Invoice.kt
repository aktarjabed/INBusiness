package com.aktarjabed.inbusiness.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.aktarjabed.inbusiness.data.local.database.Converters
import java.time.LocalDateTime

@Entity(tableName = "invoices")
@TypeConverters(Converters::class)
data class Invoice(
    @PrimaryKey val id: String = generateId(),
    val userId: String,
    val invoiceNumber: String,
    val irn: String?, // NIC Invoice Reference Number
    val qrcode: String?, // QR Code data
    val clientName: String,
    val clientEmail: String?,
    val clientPhone: String?,
    val clientAddress: String,
    val clientGstin: String?,
    val invoiceDate: LocalDateTime,
    val dueDate: LocalDateTime,
    val subtotal: Double,
    val taxAmount: Double,
    val totalAmount: Double,
    val status: InvoiceStatus = InvoiceStatus.DRAFT,
    val notes: String?,
    val terms: String?,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val isDeleted: Boolean = false
) {
    enum class InvoiceStatus {
        DRAFT,
        SENT,
        PAID,
        OVERDUE,
        CANCELLED
    }

    companion object {
        fun generateId(): String = "invoice_${System.currentTimeMillis()}_${(0..9999).random()}"
    }
}