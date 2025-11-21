package com.aktarjabed.inbusiness.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
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
)