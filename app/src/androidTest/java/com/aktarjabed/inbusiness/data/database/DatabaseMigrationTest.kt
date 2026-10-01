package com.aktarjabed.inbusiness.data.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {
    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate13To18_preservesExistingRowsAndAddsExpectedColumns() {
        val dbName = "migration-test-13-18"
        var db = helper.createDatabase(dbName, 13)
        insertBusinessData(db)
        insertInvoice(db, "inv-1", "1", "INV-0001")
        insertProduct(db, "1", productId = null)
        db.close()

        db = helper.runMigrationsAndValidate(
            dbName,
            18,
            true,
            AppDatabase.MIGRATION_13_14,
            AppDatabase.MIGRATION_14_15,
            AppDatabase.MIGRATION_15_16,
            AppDatabase.MIGRATION_16_17,
            AppDatabase.MIGRATION_17_18
        )

        db.query("SELECT status, documentType FROM invoices WHERE id = 'inv-1'").use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals("COMPLETED", cursor.getString(0))
            assertEquals("TAX_INVOICE", cursor.getString(1))
        }
        db.query("SELECT isActive, hsnSac, uqc FROM products WHERE name = 'Product1'").use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals(1, cursor.getInt(0))
            assertEquals("", cursor.getString(1))
            assertEquals("", cursor.getString(2))
        }

        // Exercise the new v14-v16 tables using the schema that actually exists at v18.
        db.execSQL("INSERT INTO customers (businessId, name, address, gstin, phone, isActive) VALUES (1, 'Cust1', '', '', '', 1)")
        db.execSQL("INSERT INTO payments (businessId, invoiceId, amount, paymentMode, paymentDate, referenceNumber, status) VALUES (1, 'inv-1', 100.0, 'CASH', 100, '', 'SUCCESS')")
        db.execSQL("INSERT INTO stock_movements (businessId, productId, movementType, quantity, stockBefore, stockAfter, referenceType, referenceId, reason, createdAt) VALUES (1, 1, 'SALE', -5.0, 100.0, 95.0, 'INVOICE', 'inv-1', '', 100)")
        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate18To19_preservesRowsAndNormalizesBusinessIds() {
        val dbName = "migration-test-18-19"
        var db = helper.createDatabase(dbName, 18)
        insertBusinessData(db)
        insertInvoice(db, "inv-18", "1", "INV-0018")
        insertProduct(db, "1", productId = 1L)
        db.execSQL("INSERT INTO customers (id, businessId, name, address, gstin, phone, isActive) VALUES (1, 1, 'Cust18', '', '', '', 1)")
        db.execSQL("INSERT INTO payments (id, businessId, invoiceId, amount, paymentMode, paymentDate, referenceNumber, status) VALUES (1, 1, 'inv-18', 25.0, 'UPI', 100, 'ref-18', 'SUCCESS')")
        db.execSQL("INSERT INTO stock_movements (id, businessId, productId, movementType, quantity, stockBefore, stockAfter, referenceType, referenceId, reason, createdAt) VALUES (1, 1, 1, 'OPENING_STOCK', 100.0, 0.0, 100.0, 'PRODUCT', '1', '', 100)")
        db.close()

        // Let Room run the migration and validate the complete v19 schema snapshot.
        db = helper.runMigrationsAndValidate(
            dbName,
            19,
            true,
            AppDatabase.MIGRATION_18_19
        )

        assertTextBusinessId(db, "customers")
        assertTextBusinessId(db, "payments")
        assertTextBusinessId(db, "stock_movements")
        assertEquals("Cust18", readString(db, "SELECT name FROM customers WHERE id = 1"))
        assertEquals("ref-18", readString(db, "SELECT referenceNumber FROM payments WHERE id = 1"))
        assertEquals("OPENING_STOCK", readString(db, "SELECT movementType FROM stock_movements WHERE id = 1"))
        assertEquals("1", readString(db, "SELECT businessId FROM customers WHERE id = 1"))
        assertEquals("1", readString(db, "SELECT businessId FROM payments WHERE id = 1"))
        assertEquals("1", readString(db, "SELECT businessId FROM stock_movements WHERE id = 1"))

        db.query("PRAGMA foreign_key_check").use { cursor ->
            assertEquals("Foreign key violations after migration", 0, cursor.count)
        }
        db.close()
    }

    private fun insertBusinessData(db: SupportSQLiteDatabase) {
        db.execSQL(
            """INSERT INTO business_data (
                id, name, gstin, address, city, state, pincode, phoneNumber, email, scenarioName,
                unitPrice, quantity, rawMaterialsCost, supplierCosts, monthlyRent, transportCosts,
                labourCosts, utilityCosts, marketingCosts, insuranceCosts, interestCosts, depreciation,
                incomeTaxSlab, tdsAmount, otherIncome, outputGst, inputGst
            ) VALUES ('1', 'Test Business', '', '', '', '', '', '', '', '',
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)"""
        )
    }

    private fun insertInvoice(db: SupportSQLiteDatabase, id: String, businessId: String, number: String) {
        db.execSQL(
            """INSERT INTO invoices (
                id, businessId, invoiceNumber, sellerName, sellerAddress, customerId, customerName,
                buyerAddress, subtotal, totalAmount, taxAmount, totalCgst, totalSgst, totalIgst,
                supplyType, amountPaid, balanceDue, paymentMethod, createdAt, updatedAt
            ) VALUES ('$id', '$businessId', '$number', 'Seller', 'Address', '', 'Customer',
                'Buyer Address', 100.0, 118.0, 18.0, 9.0, 9.0, 0.0,
                'INTRA_STATE', 0.0, 118.0, 'NONE', 1000000, 1000000)"""
        )
    }

    private fun insertProduct(db: SupportSQLiteDatabase, businessId: String, productId: Long?) {
        val idColumn = if (productId == null) "" else "id, "
        val idValue = if (productId == null) "" else "$productId, "
        val v18Columns = if (productId == null) "" else ", isActive, reorderThreshold, hsnSac, uqc"
        val v18Values = if (productId == null) "" else ", 1, 0.0, '', ''"
        db.execSQL(
            """INSERT INTO products ($idColumn businessId, name, brand, category, unitType,
                pricePerUnit, availableStock, batchNumber, isWholesaleOnly, gstPercentage$v18Columns)
                VALUES ($idValue'$businessId', 'Product1', 'Brand1', 'Cat1', 'PCS',
                10.0, 100.0, 'BATCH-1', 0, 18.0$v18Values)"""
        )
    }

    private fun assertTextBusinessId(db: SupportSQLiteDatabase, table: String) {
        db.query("PRAGMA table_info(`$table`)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            val typeIndex = cursor.getColumnIndex("type")
            var foundTextBusinessId = false
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIndex) == "businessId") {
                    foundTextBusinessId = cursor.getString(typeIndex).equals("TEXT", ignoreCase = true)
                }
            }
            assertEquals("$table.businessId should be TEXT", true, foundTextBusinessId)
        }
    }

    private fun readString(db: SupportSQLiteDatabase, sql: String): String? {
        db.query(sql).use { cursor ->
            if (!cursor.moveToFirst()) {
                throw AssertionError("Expected a row for: $sql")
            }
            cursor.getString(0)
        }
    }
}
