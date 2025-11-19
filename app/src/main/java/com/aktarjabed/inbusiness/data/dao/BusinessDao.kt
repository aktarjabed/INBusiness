package com.aktarjabed.inbusiness.data.dao

import androidx.room.*
import com.aktarjabed.inbusiness.data.entities.BusinessData
import com.aktarjabed.inbusiness.data.entities.CalculationResult
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessDao {
    @Query("SELECT * FROM business_data ORDER BY createdAt DESC")
    fun getAllBusinessData(): Flow<List<BusinessData>>

    @Query("SELECT * FROM business_data WHERE id = :id")
    suspend fun getBusinessDataById(id: String): BusinessData?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBusinessData(data: BusinessData)

    @Delete
    suspend fun deleteBusinessData(data: BusinessData)

    @Query("SELECT * FROM calculation_results WHERE businessDataId = :businessDataId")
    fun getCalculationResults(businessDataId: String): Flow<List<CalculationResult>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalculationResult(result: CalculationResult)

    @Query("DELETE FROM calculation_results WHERE businessDataId = :businessDataId")
    suspend fun deleteCalculationResults(businessDataId: String)
}