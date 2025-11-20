package com.aktarjabed.inbusiness.data.repository

import com.aktarjabed.inbusiness.data.dao.BusinessDao
import com.aktarjabed.inbusiness.data.entities.BusinessData
import com.aktarjabed.inbusiness.data.entities.CalculationResult
import com.aktarjabed.inbusiness.domain.models.FinancialMetrics
import com.aktarjabed.inbusiness.domain.usecase.CalculateMetricsUseCase
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BusinessRepository @Inject constructor(
    private val businessDao: BusinessDao,
    private val calculateMetricsUseCase: CalculateMetricsUseCase
) {

    fun getAllBusinessData(): Flow<List<BusinessData>> = businessDao.getAllBusinessData()

    suspend fun getBusinessDataById(id: String): BusinessData? = businessDao.getBusinessDataById(id)

    suspend fun saveBusinessData(data: BusinessData) {
        businessDao.insertBusinessData(data)
    }

    suspend fun deleteBusinessData(data: BusinessData) {
        businessDao.deleteBusinessData(data)
    }

    fun getCalculationResults(businessDataId: String): Flow<List<CalculationResult>> =
        businessDao.getCalculationResults(businessDataId)

    suspend fun saveCalculationResult(result: CalculationResult) {
        businessDao.insertCalculationResult(result)
    }

    fun calculateFinancialMetrics(data: BusinessData): FinancialMetrics {
        return calculateMetricsUseCase(data)
    }
}