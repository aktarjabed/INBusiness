package com.aktarjabed.inbusiness.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aktarjabed.inbusiness.data.dao.InvoiceDao
import com.aktarjabed.inbusiness.data.dao.PaymentDao
import com.aktarjabed.inbusiness.data.database.AppDatabase
import com.aktarjabed.inbusiness.data.entities.Invoice
import com.aktarjabed.inbusiness.data.entities.Payment
import com.aktarjabed.inbusiness.domain.context.BusinessContext
import com.aktarjabed.inbusiness.domain.invoice.CalculateInvoiceTotalsUseCase
import com.aktarjabed.inbusiness.domain.invoice.InvoiceCreationResult
import com.aktarjabed.inbusiness.domain.quota.QuotaGate
import com.aktarjabed.inbusiness.domain.device.DeviceClassifier
import com.aktarjabed.inbusiness.util.SystemClock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PaymentRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var invoiceDao: InvoiceDao
    private lateinit var paymentDao: PaymentDao
    private lateinit var paymentRepository: PaymentRepository
    private lateinit var invoiceRepository: InvoiceRepository
    private lateinit var businessContext: BusinessContext

    @Before
    fun setUp() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        invoiceDao = database.invoiceDao()
        paymentDao = database.paymentDao()

        businessContext = BusinessContext(ApplicationProvider.getApplicationContext())
        businessContext.setActiveBusinessId(BUSINESS_ID)
        paymentRepository = PaymentRepository(paymentDao, invoiceDao, database, businessContext)
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val quotaGate = QuotaGate(
            database.userQuotaDao(),
            DeviceClassifier(),
            SystemClock(),
            context
        )
        invoiceRepository = InvoiceRepository(
            database,
            invoiceDao,
            paymentDao,
            database.stockMovementDao(),
            database.productDao(),
            database.businessDao(),
            CalculateInvoiceTotalsUseCase(),
            quotaGate,
            businessContext
        )

        invoiceDao.insertInvoice(
            Invoice(
                id = INVOICE_ID,
                businessId = BUSINESS_ID,
                invoiceNumber = "INV-00001",
                totalAmount = 100.0,
                amountPaid = 0.0,
                balanceDue = 100.0,
                paymentMethod = "NONE"
            )
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun recordsPaymentAtomicallyAndRejectsOverpayment() = runBlocking {
        paymentRepository.addPayment(
            Payment(
                businessId = BUSINESS_ID,
                invoiceId = INVOICE_ID,
                amount = 30.0,
                paymentMode = "CASH"
            )
        )

        var invoice = invoiceDao.getInvoiceById(INVOICE_ID, BUSINESS_ID)!!
        assertEquals(30.0, invoice.amountPaid, 0.001)
        assertEquals(70.0, invoice.balanceDue, 0.001)
        assertEquals("CASH", invoice.paymentMethod)
        assertEquals(30.0, paymentDao.getTotalPaidForInvoice(BUSINESS_ID, INVOICE_ID)!!, 0.001)

        assertIllegalArgument {
            paymentRepository.addPayment(
                Payment(
                    businessId = BUSINESS_ID,
                    invoiceId = INVOICE_ID,
                    amount = 70.01,
                    paymentMode = "UPI"
                )
            )
        }

        invoice = invoiceDao.getInvoiceById(INVOICE_ID, BUSINESS_ID)!!
        assertEquals(30.0, invoice.amountPaid, 0.001)
        assertEquals(70.0, invoice.balanceDue, 0.001)
        assertEquals(1, paymentDao.getPaymentsForInvoice(BUSINESS_ID, INVOICE_ID).first().size)

        paymentRepository.addPayment(
            Payment(
                businessId = BUSINESS_ID,
                invoiceId = INVOICE_ID,
                amount = 70.0,
                paymentMode = "UPI"
            )
        )

        invoice = invoiceDao.getInvoiceById(INVOICE_ID, BUSINESS_ID)!!
        assertEquals(100.0, invoice.amountPaid, 0.001)
        assertEquals(0.0, invoice.balanceDue, 0.001)
        assertEquals("MULTIPLE", invoice.paymentMethod)
        assertEquals(100.0, paymentDao.getTotalPaidForInvoice(BUSINESS_ID, INVOICE_ID)!!, 0.001)
    }

    @Test
    fun rejectsCrossBusinessAndUnreconciledPayments() = runBlocking {
        assertIllegalArgument {
            paymentRepository.addPayment(
                Payment(
                    businessId = "another-business",
                    invoiceId = INVOICE_ID,
                    amount = 10.0,
                    paymentMode = "CASH"
                )
            )
        }

        paymentDao.insertPayment(
            Payment(
                businessId = BUSINESS_ID,
                invoiceId = INVOICE_ID,
                amount = 10.0,
                paymentMode = "CASH"
            )
        )
        assertIllegalArgument {
            paymentRepository.addPayment(
                Payment(
                    businessId = BUSINESS_ID,
                    invoiceId = INVOICE_ID,
                    amount = 10.0,
                    paymentMode = "CASH"
                )
            )
        }
        assertEquals(0.0, invoiceDao.getInvoiceById(INVOICE_ID, BUSINESS_ID)!!.amountPaid, 0.001)
        assertEquals(10.0, paymentDao.getTotalPaidForInvoice(BUSINESS_ID, INVOICE_ID)!!, 0.001)
    }

    @Test
    fun roundsRecordedPaymentToTwoDecimalPlaces() = runBlocking {
        paymentRepository.addPayment(
            Payment(
                businessId = BUSINESS_ID,
                invoiceId = INVOICE_ID,
                amount = 0.005,
                paymentMode = "CASH"
            )
        )

        assertEquals(0.01, invoiceDao.getInvoiceById(INVOICE_ID, BUSINESS_ID)!!.amountPaid, 0.0001)
        assertEquals(99.99, invoiceDao.getInvoiceById(INVOICE_ID, BUSINESS_ID)!!.balanceDue, 0.0001)
        assertEquals(0.01, paymentDao.getTotalPaidForInvoice(BUSINESS_ID, INVOICE_ID)!!, 0.0001)
    }

    @Test
    fun rejectsPaymentForCancelledInvoice() = runBlocking {
        val invoice = invoiceDao.getInvoiceById(INVOICE_ID, BUSINESS_ID)!!
        invoiceDao.updateInvoice(invoice.copy(status = "CANCELLED"))

        assertIllegalArgument {
            paymentRepository.addPayment(
                Payment(
                    businessId = BUSINESS_ID,
                    invoiceId = INVOICE_ID,
                    amount = 10.0,
                    paymentMode = "CASH"
                )
            )
        }
        assertEquals(0, paymentDao.getPaymentsForInvoice(BUSINESS_ID, INVOICE_ID).first().size)
    }

    @Test
    fun cancellationIsBlockedWhenSuccessfulPaymentExists() = runBlocking {
        paymentRepository.addPayment(
            Payment(
                businessId = BUSINESS_ID,
                invoiceId = INVOICE_ID,
                amount = 10.0,
                paymentMode = "CASH"
            )
        )

        val result = invoiceRepository.cancelInvoice(INVOICE_ID)
        assertTrue(result is InvoiceCreationResult.InvalidRequest)
        assertEquals("COMPLETED", invoiceDao.getInvoiceById(INVOICE_ID, BUSINESS_ID)!!.status)
        assertEquals(90.0, invoiceDao.getInvoiceById(INVOICE_ID, BUSINESS_ID)!!.balanceDue, 0.001)
    }

    private suspend fun assertIllegalArgument(action: suspend () -> Unit) {
        try {
            action()
            throw AssertionError("Expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }
    }

    private companion object {
        const val BUSINESS_ID = "test-business"
        const val INVOICE_ID = "test-invoice"
    }
}
