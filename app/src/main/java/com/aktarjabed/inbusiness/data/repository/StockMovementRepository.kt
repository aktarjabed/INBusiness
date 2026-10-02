package com.aktarjabed.inbusiness.data.repository

import com.aktarjabed.inbusiness.data.dao.StockMovementDao
import com.aktarjabed.inbusiness.data.entities.StockMovement
import com.aktarjabed.inbusiness.domain.context.BusinessContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StockMovementRepository @Inject constructor(
    private val stockMovementDao: StockMovementDao,
    private val businessContext: BusinessContext
) {
    fun getMovementsForProduct(productId: Long): Flow<List<StockMovement>> = businessContext.activeBusinessId.flatMapLatest { businessId ->
        stockMovementDao.getMovementsForProduct(businessId, productId)
    }

    suspend fun getMovementsByReference(referenceType: String, referenceId: String): List<StockMovement> {
        val businessId = businessContext.activeBusinessId.first()
        return stockMovementDao.getMovementsByReference(businessId, referenceType, referenceId)
    }

    /**
     * Records a manual ledger entry.
     *
     * Only type-independent invariants are enforced here. The *sign* of `quantity` is
     * deliberately not: it is type-dependent, and two of the types in use are legitimately
     * signed either way (`STOCK_ADJUSTMENT` goes up or down, `OPENING_STOCK` is positive,
     * `SALE`/`STOCK_DEDUCTION` are negative). Encoding a fixed sign-per-type table here would
     * reject valid adjustments; the call sites that know the type (invoice creation, manual
     * deduction, product edit) set it.
     */
    suspend fun addMovement(movement: StockMovement): Long {
        require(movement.businessId == businessContext.activeBusinessId.first()) { "Movement must belong to active business" }
        require(movement.quantity.isFinite() && movement.quantity != 0.0) { "Movement quantity must be finite and non-zero" }
        require(movement.movementType.isNotBlank()) { "Movement type cannot be blank" }
        require(movement.referenceId.isNotBlank()) { "Movement reference id cannot be blank" }
        return stockMovementDao.insertMovement(movement)
    }
}
