package com.aktarjabed.inbusiness.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aktarjabed.inbusiness.data.database.AppDatabase
import com.aktarjabed.inbusiness.domain.context.BusinessContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProductRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: ProductRepository
    private lateinit var businessId: String

    @Before
    fun setUp() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        businessId = "product-repository-test"
        val businessContext = BusinessContext(ApplicationProvider.getApplicationContext())
        businessContext.setActiveBusinessId(businessId)
        repository = ProductRepository(
            database.productDao(),
            database.stockMovementDao(),
            database,
            businessContext
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun recordsOpeningStockAdjustmentsAndManualDeductions() = runBlocking {
        val productId = repository.saveProduct(
            id = 0,
            name = "Seed",
            brand = "J.A.",
            category = "Fertilizer",
            unitType = "KG",
            pricePerUnit = 100.0,
            availableStock = 10.0,
            batchNumber = "B-1",
            isWholesaleOnly = false,
            gstPercentage = 5.0,
            reorderThreshold = 3.0
        )

        assertEquals(10.0, repository.getProductById(productId)!!.availableStock, 0.001)
        assertEquals(3.0, repository.getProductById(productId)!!.reorderThreshold, 0.001)

        repository.saveProduct(
            id = productId,
            name = "Seed",
            brand = "J.A.",
            category = "Fertilizer",
            unitType = "KG",
            pricePerUnit = 100.0,
            availableStock = 8.0,
            batchNumber = "B-1",
            isWholesaleOnly = false,
            gstPercentage = 5.0,
            reorderThreshold = 2.0
        )
        assertEquals(2.0, repository.getProductById(productId)!!.reorderThreshold, 0.001)
        assertEquals(StockDeductionResult.Success, repository.deductStock(productId, 1.0))
        assertEquals(7.0, repository.getProductById(productId)!!.availableStock, 0.001)

        val movements = database.stockMovementDao().getMovementsByReference(
            businessId,
            "PRODUCT",
            productId.toString()
        )
        assertEquals(2, movements.size)
        assertTrue(movements.any { it.movementType == "OPENING_STOCK" && it.quantity == 10.0 })
        assertTrue(movements.any { it.movementType == "STOCK_ADJUSTMENT" && it.quantity == -2.0 })

        // Manual deductions use unique references; query by product to verify their ledger entry.
        val allMovements = database.stockMovementDao().getMovementsForProduct(businessId, productId).first()
        assertEquals(3, allMovements.size)
        assertTrue(allMovements.any { it.movementType == "STOCK_DEDUCTION" && it.quantity == -1.0 })
    }
}
