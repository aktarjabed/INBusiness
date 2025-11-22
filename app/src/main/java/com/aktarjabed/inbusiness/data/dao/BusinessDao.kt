package com.aktarjabed.inbusiness.data.dao

import androidx.room.*
import com.aktarjabed.inbusiness.data.entities.BusinessData
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessDao {

    @Query("SELECT * FROM business_data LIMIT 1")
    suspend fun getBusinessData(): BusinessData?

    @Query("SELECT * FROM business_data LIMIT 1")
    fun getBusinessDataFlow(): Flow<BusinessData?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(businessData: BusinessData)

    @Update
    suspend fun update(businessData: BusinessData)

    @Delete
    suspend fun delete(businessData: BusinessData)
}