package com.aktarjabed.inbusiness.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aktarjabed.inbusiness.data.entities.StockMovement
import kotlinx.coroutines.flow.Flow

@Dao
interface StockMovementDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMovement(movement: StockMovement): Long

    /**
     * Ledger for one product, newest first.
     *
     * `createdAt` alone is not a total order: `System.currentTimeMillis()` has millisecond
     * resolution, so a sale and its reversal (or several quick edits) can share a timestamp and
     * come back in arbitrary — possibly non-insertion — order. `id DESC` breaks ties in insertion
     * order, which is what an append-only ledger must show.
     */
    @Query("SELECT * FROM stock_movements WHERE productId = :productId AND businessId = :businessId ORDER BY createdAt DESC, id DESC")
    fun getMovementsForProduct(businessId: String, productId: Long): Flow<List<StockMovement>>

    @Query("SELECT * FROM stock_movements WHERE referenceType = :referenceType AND referenceId = :referenceId AND businessId = :businessId")
    suspend fun getMovementsByReference(businessId: String, referenceType: String, referenceId: String): List<StockMovement>
}
