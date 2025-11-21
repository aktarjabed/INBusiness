package com.aktarjabed.inbusiness.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey val paymentId: String,
    val userId: String,
    val razorpayOrderId: String,
    val razorpayPaymentId: String,
    val signature: String,
    val amountPaise: Long,
    val plan: String,
    val currency: String,
    val status: String,
    val createdAt: Long
)