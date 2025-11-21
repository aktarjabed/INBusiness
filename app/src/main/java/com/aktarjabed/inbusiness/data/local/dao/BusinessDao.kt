package com.aktarjabed.inbusiness.data.local.dao

import androidx.room.*
import com.aktarjabed.inbusiness.data.local.entities.BusinessData
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface BusinessDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBusiness(business: BusinessData): Long

    @Update
    suspend fun updateBusiness(business: BusinessData)

    @Query("SELECT * FROM business_data WHERE userId = :userId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getBusinessByUserId(userId: String): BusinessData?

    @Query("SELECT * FROM business_data WHERE userId = :userId ORDER BY createdAt DESC LIMIT 1")
    fun getBusinessByUserIdFlow(userId: String): Flow<BusinessData?>

    @Query("SELECT * FROM business_data WHERE id = :businessId")
    suspend fun getBusinessById(businessId: String): BusinessData?

    @Query("DELETE FROM business_data WHERE id = :businessId")
    suspend fun deleteBusiness(businessId: String)

    @Query("SELECT * FROM business_data WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getAllBusinessesForUser(userId: String): List<BusinessData>

    @Query("SELECT COUNT(*) FROM business_data WHERE userId = :userId")
    suspend fun getBusinessCountForUser(userId: String): Int

    @Query("UPDATE business_data SET updatedAt = :timestamp WHERE id = :businessId")
    suspend fun updateBusinessTimestamp(
        businessId: String,
        timestamp: LocalDateTime = LocalDateTime.now()
    )

    @Transaction
    suspend fun upsertBusiness(business: BusinessData) {
        val existing = getBusinessByUserId(business.userId)
        if (existing == null) {
            insertBusiness(business)
        } else {
            updateBusiness(business.copy(id = existing.id))
        }
    }
}