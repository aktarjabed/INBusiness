package com.aktarjabed.inbusiness.data.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DashboardDao {

    @Query("""
        SELECT createdAt, totalAmount
        FROM invoices
        WHERE businessId = :businessId
          AND status = 'COMPLETED'
          AND createdAt >= :startTime
    """)
    suspend fun getRevenueForChartRaw(businessId: String, startTime: Long): List<InvoiceRevenueRaw>

    @Query("SELECT COALESCE(SUM(totalAmount), 0.0) FROM invoices WHERE businessId = :businessId AND status = 'COMPLETED'")
    fun observeTotalRevenue(businessId: String): Flow<Double>

    @Query("SELECT COALESCE(SUM(totalAmount), 0.0) FROM invoices WHERE businessId = :businessId AND status = 'COMPLETED' AND createdAt >= :startOfDay AND createdAt < :endOfDay")
    fun observeTodayRevenue(businessId: String, startOfDay: Long, endOfDay: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(balanceDue), 0.0) FROM invoices WHERE businessId = :businessId AND status = 'COMPLETED' AND balanceDue > 0")
    fun observePendingDues(businessId: String): Flow<Double>

    @Query("SELECT COUNT(*) FROM invoices WHERE businessId = :businessId AND status = 'COMPLETED' AND createdAt >= :startOfDay AND createdAt < :endOfDay")
    fun getInvoicesTodayCount(businessId: String, startOfDay: Long, endOfDay: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM products WHERE businessId = :businessId AND isActive = 1")
    fun observeActiveProductsCount(businessId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM products WHERE businessId = :businessId AND isActive = 1 AND availableStock <= MAX(reorderThreshold, 0)")
    fun observeLowStockProductsCount(businessId: String): Flow<Int>
}
