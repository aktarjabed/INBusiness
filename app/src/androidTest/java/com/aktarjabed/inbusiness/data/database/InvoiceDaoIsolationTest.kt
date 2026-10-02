package com.aktarjabed.inbusiness.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aktarjabed.inbusiness.data.dao.InvoiceDao
import com.aktarjabed.inbusiness.data.entities.Invoice
import com.aktarjabed.inbusiness.data.entities.InvoiceItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class InvoiceDaoIsolationTest {
    private lateinit var db: AppDatabase
    private lateinit var invoiceDao: InvoiceDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        invoiceDao = db.invoiceDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testTenantIsolation() = runBlocking {
        val biz1 = "business-1"
        val biz2 = "business-2"

        val invoice1 = Invoice(
            id = "inv-1",
            businessId = biz1,
            invoiceNumber = "INV-1",
            customerId = "cust-1",
            customerName = "Cust 1",
            totalAmount = 100.0,
            taxAmount = 10.0,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        val invoice2 = Invoice(
            id = "inv-2",
            businessId = biz2,
            invoiceNumber = "INV-2",
            customerId = "cust-2",
            customerName = "Cust 2",
            totalAmount = 200.0,
            taxAmount = 20.0,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        invoiceDao.insertInvoice(invoice1)
        invoiceDao.insertInvoice(invoice2)

        val items1 = InvoiceItem(id = "item-1", invoiceId = "inv-1", description = "Item 1", quantity = 1.0, pricePerUnit = 100.0)
        val items2 = InvoiceItem(id = "item-2", invoiceId = "inv-2", description = "Item 2", quantity = 2.0, pricePerUnit = 100.0)

        invoiceDao.insertItem(items1)
        invoiceDao.insertItem(items2)

        // Read isolation
        val invoicesBiz1 = invoiceDao.getAllInvoicesOnce(biz1)
        assertEquals(1, invoicesBiz1.size)
        assertEquals("inv-1", invoicesBiz1[0].id)

        val invoiceBiz2 = invoiceDao.getInvoiceById("inv-2", biz1)
        assertNull(invoiceBiz2)

        val itemsBiz1 = invoiceDao.getInvoiceItems("inv-1", biz1)
        assertEquals(1, itemsBiz1.size)

        val itemsBiz2FromBiz1 = invoiceDao.getInvoiceItems("inv-2", biz1)
        assertEquals(0, itemsBiz2FromBiz1.size)


        val invoicesBiz2 = invoiceDao.getAllInvoicesOnce(biz2)
        assertEquals(1, invoicesBiz2.size)
    }

    @Test
    fun historicalSuggestionQueryProjectsOnlySuggestionFieldsAndIsBusinessScoped() = runBlocking {
        val invoice = Invoice(
            id = "suggestion-invoice",
            businessId = "suggestion-business",
            invoiceNumber = "INV-1",
            customerName = "Buyer",
            createdAt = Instant.ofEpochMilli(10),
            updatedAt = Instant.ofEpochMilli(10)
        )
        invoiceDao.insertInvoice(invoice)
        invoiceDao.insertItem(
            InvoiceItem(
                id = "suggestion-item",
                invoiceId = invoice.id,
                description = "Urea",
                quantity = 4.0,
                pricePerUnit = 25.0,
                unitType = "BAG",
                subTotal = 100.0,
                gstPercentage = 5.0,
                taxAmount = 5.0,
                totalAmount = 105.0,
                productId = 42L
            )
        )

        val suggestions = invoiceDao.getHistoricalInvoiceItems("suggestion-business").first()
        assertEquals(1, suggestions.size)
        assertEquals("Urea", suggestions.single().description)
        assertEquals(25.0, suggestions.single().pricePerUnit, 0.0)
        assertEquals("BAG", suggestions.single().unitType)
        assertEquals(42L, suggestions.single().productId)
        assertEquals(5.0, suggestions.single().gstPercentage, 0.0)
        assertTrue(invoiceDao.getHistoricalInvoiceItems("another-business").first().isEmpty())
    }
}
