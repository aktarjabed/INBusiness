package com.aktarjabed.inbusiness.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "business_data")
data class BusinessData(
    @PrimaryKey val id: String,
    val userId: String,
    val businessName: String,
    val gstin: String?,
    val address: String,
    val phone: String?,
    val email: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)