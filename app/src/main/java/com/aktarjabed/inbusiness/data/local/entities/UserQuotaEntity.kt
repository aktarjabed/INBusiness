package com.aktarjabed.inbusiness.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_quotas")
data class UserQuotaEntity(
    @PrimaryKey val userId: String,
    val tier: String,               // FREE, BASIC, PRO, ENTERPRISE
    val dailyUsed: Int,
    val lastResetEpochDay: Long,     // epochDay
    val monthlyUsed: Int,
    val lastMonthlyResetEpochDay: Long,
    val watermark: Boolean,
    val retentionDays: Int,
    val freeExpiryEpochDay: Long,
    val deviceTier: String
)