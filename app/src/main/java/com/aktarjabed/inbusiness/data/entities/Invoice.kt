package com.aktarjabed.inbusiness.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "invoices")
data class Invoice(
    @PrimaryKey val id: String = "",
    val businessId: String = "",
    val invoiceNumber: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val customerGSTIN: String? = null,
    val totalAmount: Double = 0.0,
    val taxAmount: Double = 0.0,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val irn: String? = null,
    val ackNo: String? = null,
    val ackDate: Instant? = null,
    val qrCodeData: String? = null
)