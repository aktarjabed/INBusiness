package com.aktarjabed.inbusiness.data.repository

import androidx.room.withTransaction
import com.aktarjabed.inbusiness.data.dao.ProductDao
import com.aktarjabed.inbusiness.data.dao.StockMovementDao
import com.aktarjabed.inbusiness.data.database.AppDatabase
import com.aktarjabed.inbusiness.data.entities.Product
import com.aktarjabed.inbusiness.data.entities.StockMovement
import com.aktarjabed.inbusiness.domain.context.BusinessContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject
import javax.inject.Singleton

sealed class StockDeductionResult {
    object Success : StockDeductionResult()
    object InsufficientStock : StockDeductionResult()
    object NotFound : StockDeductionResult()
}

@Singleton
class ProductRepository @Inject constructor(
    private val productDao: ProductDao,
    private val stockMovementDao: StockMovementDao,
    private val database: AppDatabase,
    private val businessContext: BusinessContext
) {
    fun getAllProducts(): Flow<List<Product>> = businessContext.activeBusinessId.flatMapLatest { businessId ->
        productDao.getAllProducts(businessId)
    }

    fun searchProducts(query: String): Flow<List<Product>> = businessContext.activeBusinessId.flatMapLatest { businessId ->
        productDao.searchProducts(businessId, query)
    }

    fun searchProductsByCategory(query: String, category: String): Flow<List<Product>> = businessContext.activeBusinessId.flatMapLatest { businessId ->
        productDao.searchProductsByCategory(businessId, query, category)
    }

    fun getUniqueCategories(): Flow<List<String>> = businessContext.activeBusinessId.flatMapLatest { businessId ->
        productDao.getUniqueCategories(businessId)
    }

    fun getUniqueUnitTypes(): Flow<List<String>> = businessContext.activeBusinessId.flatMapLatest { businessId ->
        productDao.getUniqueUnitTypes(businessId)
    }

    suspend fun getProductById(id: Long): Product? {
        val businessId = businessContext.activeBusinessId.first()
        return productDao.getProductById(id, businessId)
    }

    suspend fun saveProduct(
        id: Long,
        name: String,
        brand: String,
        category: String,
        unitType: String,
        pricePerUnit: Double,
        availableStock: Double,
        batchNumber: String?,
        isWholesaleOnly: Boolean,
        gstPercentage: Double,
        reorderThreshold: Double = 0.0
    ): Long {
        val businessId = businessContext.activeBusinessId.first()
        require(name.isNotBlank()) { "Name cannot be blank" }
        require(brand.isNotBlank()) { "Brand cannot be blank" }
        require(category.isNotBlank()) { "Category cannot be blank" }
        require(unitType.isNotBlank()) { "Unit type cannot be blank" }
        require(pricePerUnit.isFinite() && pricePerUnit >= 0.0) { "Price must be finite and cannot be negative" }
        require(availableStock.isFinite() && availableStock >= 0.0) { "Stock must be finite and cannot be negative" }
        require(gstPercentage.isFinite() && gstPercentage >= 0.0) { "GST percentage must be finite and cannot be negative" }
        require(reorderThreshold.isFinite() && reorderThreshold >= 0.0) { "Reorder threshold must be finite and cannot be negative" }

        val trimmedName = name.trim()
        val trimmedBrand = brand.trim()
        val trimmedCategory = category.trim()
        val trimmedUnitType = unitType.trim()
        val trimmedBatchNumber = batchNumber?.trim()?.takeIf { it.isNotBlank() } ?: ""

        return database.withTransaction {
            if (id == 0L) {
                val productId = productDao.insertProduct(
                    Product(
                        id = 0L,
                        businessId = businessId,
                        name = trimmedName,
                        brand = trimmedBrand,
                        category = trimmedCategory,
                        unitType = trimmedUnitType,
                        pricePerUnit = pricePerUnit,
                        availableStock = availableStock,
                        batchNumber = trimmedBatchNumber,
                        isWholesaleOnly = isWholesaleOnly,
                        gstPercentage = gstPercentage,
                        reorderThreshold = reorderThreshold
                    )
                )
                if (availableStock > 0.0) {
                    stockMovementDao.insertMovement(
                        StockMovement(
                            businessId = businessId,
                            productId = productId,
                            movementType = "OPENING_STOCK",
                            quantity = availableStock,
                            stockBefore = 0.0,
                            stockAfter = availableStock,
                            referenceType = "PRODUCT",
                            referenceId = productId.toString(),
                            reason = "Opening stock"
                        )
                    )
                }
                productId
            } else {
                val existingProduct = productDao.getProductById(id, businessId)
                    ?: throw IllegalStateException("Product does not exist or belongs to another business.")

                val rowsAffected = productDao.updateProduct(
                    id = id,
                    businessId = businessId,
                    name = trimmedName,
                    brand = trimmedBrand,
                    category = trimmedCategory,
                    unitType = trimmedUnitType,
                    pricePerUnit = pricePerUnit,
                    availableStock = availableStock,
                    batchNumber = trimmedBatchNumber,
                    isWholesaleOnly = isWholesaleOnly,
                    gstPercentage = gstPercentage,
                    reorderThreshold = reorderThreshold
                )
                if (rowsAffected == 0) {
                    throw IllegalStateException("Failed to update product. It may not exist or belongs to another business.")
                }

                val stockDelta = availableStock - existingProduct.availableStock
                if (stockDelta != 0.0) {
                    stockMovementDao.insertMovement(
                        StockMovement(
                            businessId = businessId,
                            productId = id,
                            movementType = "STOCK_ADJUSTMENT",
                            quantity = stockDelta,
                            stockBefore = existingProduct.availableStock,
                            stockAfter = availableStock,
                            referenceType = "PRODUCT",
                            referenceId = id.toString(),
                            reason = "Stock adjusted through product edit"
                        )
                    )
                }
                id
            }
        }
    }

    suspend fun deductStock(productId: Long, quantity: Double): StockDeductionResult {
        val businessId = businessContext.activeBusinessId.first()
        require(quantity.isFinite() && quantity > 0.0) { "Deduction quantity must be finite and strictly positive" }
        return database.withTransaction {
            val product = productDao.getProductById(productId, businessId)
                ?: return@withTransaction StockDeductionResult.NotFound
            val affectedRows = productDao.deductStock(productId, businessId, quantity)
            if (affectedRows == 0) {
                StockDeductionResult.InsufficientStock
            } else {
                stockMovementDao.insertMovement(
                    StockMovement(
                        businessId = businessId,
                        productId = productId,
                        movementType = "STOCK_DEDUCTION",
                        quantity = -quantity,
                        stockBefore = product.availableStock,
                        stockAfter = product.availableStock - quantity,
                        referenceType = "MANUAL",
                        referenceId = java.util.UUID.randomUUID().toString(),
                        reason = "Manual stock deduction"
                    )
                )
                StockDeductionResult.Success
            }
        }
    }

    suspend fun deleteProduct(productId: Long) {
        val businessId = businessContext.activeBusinessId.first()
        val rowsAffected = productDao.deleteProduct(productId, businessId)
        if (rowsAffected == 0) {
            throw IllegalStateException("Failed to delete product. It may not exist or belongs to another business.")
        }
    }
}
