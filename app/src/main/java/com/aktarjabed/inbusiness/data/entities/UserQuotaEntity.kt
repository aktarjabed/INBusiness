package com.aktarjabed.inbusiness.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_quota",
    indices = [Index(value = ["userId"], unique = true)]
)
data class UserQuotaEntity(
    @PrimaryKey val userId: String,
    val tier: String,                      // FREE, BASIC, PRO, ENTERPRISE
    val dailyUsed: Int,
    val lastResetEpochDay: Long,
    val monthlyUsed: Int,
    val lastMonthlyResetEpochDay: Long,
    val watermark: Boolean,
    val retentionDays: Int,
    val freeExpiryEpochDay: Long?,
    val lastUpgradePrompt: Long = 0,
    val upgradePromptCount: Int = 0,
    val referredBy: String? = null,
    val bonusInvoices: Int = 0,
    val deviceTier: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)