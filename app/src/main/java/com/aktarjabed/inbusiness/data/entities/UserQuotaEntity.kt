package com.aktarjabed.inbusiness.data.entities

import androidx.room.Entity
<<<<<<< HEAD
import androidx.room.PrimaryKey

@Entity(tableName = "user_quotas")
data class UserQuotaEntity(
    @PrimaryKey val userId: String,
    val tier: String,
=======
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_quota",
    indices = [Index(value = ["userId"], unique = true)]
)
data class UserQuotaEntity(
    @PrimaryKey val userId: String,
    val tier: String,                      // FREE, BASIC, PRO, ENTERPRISE
>>>>>>> ad4bb8454e6b04046f0ba290c1f04d22f25fbfa5
    val dailyUsed: Int,
    val lastResetEpochDay: Long,
    val monthlyUsed: Int,
    val lastMonthlyResetEpochDay: Long,
    val watermark: Boolean,
    val retentionDays: Int,
<<<<<<< HEAD
    val freeExpiryEpochDay: Long,
    val deviceTier: String
=======
    val freeExpiryEpochDay: Long?,
    val lastUpgradePrompt: Long = 0,
    val upgradePromptCount: Int = 0,
    val referredBy: String? = null,
    val bonusInvoices: Int = 0,
    val deviceTier: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
>>>>>>> ad4bb8454e6b04046f0ba290c1f04d22f25fbfa5
)