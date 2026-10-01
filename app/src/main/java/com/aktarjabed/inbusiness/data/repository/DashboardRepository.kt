package com.aktarjabed.inbusiness.data.repository

import com.aktarjabed.inbusiness.data.dao.DashboardDao
import com.aktarjabed.inbusiness.domain.context.BusinessContext
import com.aktarjabed.inbusiness.utils.AppDateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

data class ChartPoint(
    val date: LocalDate,
    val revenue: Double
)

@Singleton
class DashboardRepository @Inject constructor(
    private val dashboardDao: DashboardDao,
    private val businessContext: BusinessContext
) {
    private data class DayWindow(val start: Long, val end: Long)

    private fun todayWindowFlow(): Flow<DayWindow> = flow {
        while (true) {
            val start = AppDateUtils.getTodayStart()
            val end = AppDateUtils.getTomorrowStart()
            emit(DayWindow(start, end))
            delay((end - System.currentTimeMillis()).coerceAtLeast(1L))
        }
    }.distinctUntilChanged()

    fun observeTotalRevenue(): Flow<Double> = businessContext.activeBusinessId.flatMapLatest { businessId ->
        dashboardDao.observeTotalRevenue(businessId)
    }

    fun observeTodayRevenue(): Flow<Double> = businessContext.activeBusinessId.flatMapLatest { businessId ->
        todayWindowFlow().flatMapLatest { window ->
            dashboardDao.observeTodayRevenue(businessId, window.start, window.end)
        }
    }

    fun observePendingDues(): Flow<Double> = businessContext.activeBusinessId.flatMapLatest { businessId ->
        dashboardDao.observePendingDues(businessId)
    }

    fun observeInvoicesTodayCount(): Flow<Int> = businessContext.activeBusinessId.flatMapLatest { businessId ->
        todayWindowFlow().flatMapLatest { window ->
            dashboardDao.getInvoicesTodayCount(businessId, window.start, window.end)
        }
    }

    fun observeActiveProductsCount(): Flow<Int> = businessContext.activeBusinessId.flatMapLatest { businessId ->
        dashboardDao.observeActiveProductsCount(businessId)
    }

    fun observeLowStockProductsCount(): Flow<Int> = businessContext.activeBusinessId.flatMapLatest { businessId ->
        dashboardDao.observeLowStockProductsCount(businessId)
    }

    suspend fun getSevenDayChartData(): List<ChartPoint> = withContext(Dispatchers.IO) {
        val businessId = businessContext.activeBusinessId.first()
        val pastSevenDays = AppDateUtils.getPastSevenDays()
        val startTime = AppDateUtils.getStartOfDay(pastSevenDays.first())
        val rawData = dashboardDao.getRevenueForChartRaw(businessId, startTime)

        val aggregatedMap = rawData.groupBy { raw ->
            Instant.ofEpochMilli(raw.createdAt).atZone(AppDateUtils.businessZoneId).toLocalDate()
        }.mapValues { entry ->
            entry.value.sumOf { it.totalAmount }
        }

        pastSevenDays.map { date ->
            ChartPoint(
                date = date,
                revenue = aggregatedMap[date] ?: 0.0
            )
        }
    }
}
