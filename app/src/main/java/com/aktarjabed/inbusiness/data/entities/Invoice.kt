package com.aktarjabed.inbusiness.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
<<<<<<< HEAD
import java.time.LocalDateTime

@Entity(tableName = "invoices")
data class Invoice(
    @PrimaryKey val id: String,
    val userId: String,
    val invoiceNumber: String,
    val clientName: String,
    val totalAmount: Double,
    val status: String,
    val createdAt: LocalDateTime
=======
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
>>>>>>> ad4bb8454e6b04046f0ba290c1f04d22f25fbfa5
)