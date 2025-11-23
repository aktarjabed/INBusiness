package com.aktarjabed.inbusiness.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.aktarjabed.inbusiness.data.local.database.Converters
import java.time.LocalDateTime

@Entity(tableName = "business_data")
@TypeConverters(Converters::class)
data class BusinessData(
    @PrimaryKey val id: String = generateId(),
    val userId: String,
    val businessName: String,
    val businessType: String, // GSTIN, PAN, etc.
    val gstin: String?,
    val pan: String?,
    val address: String,
    val city: String,
    val state: String,
    val pincode: String,
    val email: String?,
    val phone: String?,
    val website: String?,
    val bankName: String?,
    val bankAccount: String?,
    val bankIfsc: String?,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val isDeleted: Boolean = false
) {
    companion object {
        fun generateId(): String = "business_${System.currentTimeMillis()}_${(0..9999).random()}"
    }
}