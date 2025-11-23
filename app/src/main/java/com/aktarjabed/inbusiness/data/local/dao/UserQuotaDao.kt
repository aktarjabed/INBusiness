package com.aktarjabed.inbusiness.data.local.dao

import androidx.room.*
import com.aktarjabed.inbusiness.data.local.entities.UserQuotaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserQuotaDao {
    @Query("SELECT * FROM user_quotas WHERE userId = :userId")
    suspend fun getQuota(userId: String): UserQuotaEntity?

    @Query("SELECT * FROM user_quotas WHERE userId = :userId")
    fun getQuotaFlow(userId: String): Flow<UserQuotaEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(quota: UserQuotaEntity)

    @Update
    suspend fun updateQuota(quota: UserQuotaEntity)

    @Query("""
        UPDATE user_quotas
        SET tier = :tier,
            watermark = :watermark,
            retentionDays = :retentionDays
        WHERE userId = :userId
    """)
    suspend fun upgradeTier(
        userId: String,
        tier: String,
        watermark: Boolean,
        retentionDays: Int
    )

    @Query("""
        UPDATE user_quotas
        SET tier = :tier,
            watermark = :watermark,
            retentionDays = :retentionDays
        WHERE userId = :userId
    """)
    suspend fun downgradeTier(
        userId: String,
        tier: String,
        watermark: Boolean,
        retentionDays: Int
    )

    @Query("SELECT * FROM user_quotas")
    suspend fun getAllQuotas(): List<UserQuotaEntity>

    @Query("SELECT * FROM user_quotas WHERE tier != 'FREE'")
    suspend fun getAllPaidUsers(): List<UserQuotaEntity>

    @Query("""
        UPDATE user_quotas
        SET dailyUsed = 0,
            lastResetEpochDay = :epochDay
        WHERE userId = :userId
    """)
    suspend fun resetDailyQuota(userId: String, epochDay: Long)

    @Query("""
        UPDATE user_quotas
        SET monthlyUsed = 0,
            lastMonthlyResetEpochDay = :epochDay
        WHERE userId = :userId
    """)
    suspend fun resetMonthlyQuota(userId: String, epochDay: Long)

    @Query("UPDATE user_quotas SET dailyUsed = dailyUsed + 1 WHERE userId = :userId")
    suspend fun incrementDailyCount(userId: String)

    @Query("UPDATE user_quotas SET monthlyUsed = monthlyUsed + 1 WHERE userId = :userId")
    suspend fun incrementMonthlyCount(userId: String)

    @Query("""
        UPDATE user_quotas
        SET dailyUsed = dailyUsed + 1,
            monthlyUsed = monthlyUsed + 1
        WHERE userId = :userId
    """)
    suspend fun incrementDailyAndMonthly(userId: String)

    @Query("UPDATE user_quotas SET freeExpiryEpochDay = :expiryDay WHERE userId = :userId")
    suspend fun updateFreeExpiry(userId: String, expiryDay: Long)

    @Query("DELETE FROM user_quotas WHERE userId = :userId")
    suspend fun deleteQuota(userId: String)
}