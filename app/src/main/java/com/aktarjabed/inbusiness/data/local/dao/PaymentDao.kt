package com.aktarjabed.inbusiness.data.local.dao

import androidx.room.*
import com.aktarjabed.inbusiness.data.local.entities.PaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity)

    @Update
    suspend fun updatePayment(payment: PaymentEntity)

    @Query("SELECT * FROM payments WHERE userId = :userId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestPayment(userId: String): PaymentEntity?

    @Query("SELECT * FROM payments WHERE userId = :userId AND status = 'PAID' ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestPaidPayment(userId: String): PaymentEntity?

    @Query("SELECT * FROM payments WHERE paymentId = :paymentId")
    suspend fun getPaymentById(paymentId: String): PaymentEntity?

    @Query("SELECT * FROM payments WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getUserPayments(userId: String): List<PaymentEntity>

    @Query("SELECT * FROM payments WHERE userId = :userId ORDER BY createdAt DESC")
    fun getUserPaymentsFlow(userId: String): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE status = 'PENDING' AND createdAt < :timestamp")
    suspend fun getPendingPaymentsOlderThan(timestamp: Long): List<PaymentEntity>

    @Query("UPDATE payments SET status = 'FAILED' WHERE paymentId = :paymentId")
    suspend fun markPaymentAsFailed(paymentId: String)

    @Query("UPDATE payments SET status = :status WHERE paymentId = :paymentId")
    suspend fun updatePaymentStatus(paymentId: String, status: String)

    @Query("SELECT SUM(amountPaise) FROM payments WHERE userId = :userId AND status = 'PAID'")
    suspend fun getTotalPaidAmount(userId: String): Long?

    @Query("SELECT COUNT(*) FROM payments WHERE userId = :userId AND status = 'PAID'")
    suspend fun getSuccessfulPaymentCount(userId: String): Int

    @Query("DELETE FROM payments WHERE paymentId = :paymentId")
    suspend fun deletePayment(paymentId: String)

    @Query("SELECT * FROM payments WHERE status = :status ORDER BY createdAt DESC")
    suspend fun getPaymentsByStatus(status: String): List<PaymentEntity>
}