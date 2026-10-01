package com.aktarjabed.inbusiness.data.repository

import androidx.room.withTransaction
import com.aktarjabed.inbusiness.data.dao.InvoiceDao
import com.aktarjabed.inbusiness.data.dao.PaymentDao
import com.aktarjabed.inbusiness.data.database.AppDatabase
import com.aktarjabed.inbusiness.data.entities.Payment
import com.aktarjabed.inbusiness.domain.context.BusinessContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentRepository @Inject constructor(
    private val paymentDao: PaymentDao,
    private val invoiceDao: InvoiceDao,
    private val database: AppDatabase,
    private val businessContext: BusinessContext
) {
    private val allowedPaymentModes = setOf("CASH", "CARD", "UPI", "BANK_TRANSFER")

    fun getPaymentsForInvoice(invoiceId: String): Flow<List<Payment>> = businessContext.activeBusinessId.flatMapLatest { businessId ->
        paymentDao.getPaymentsForInvoice(businessId, invoiceId)
    }

    suspend fun getTotalPaidForInvoice(invoiceId: String): Double {
        val businessId = businessContext.activeBusinessId.first()
        return paymentDao.getTotalPaidForInvoice(businessId, invoiceId) ?: 0.0
    }

    /**
     * Records a successful payment and keeps the invoice summary in sync atomically.
     * Failed/refunded payment records need dedicated workflows and are not accepted here.
     */
    suspend fun addPayment(payment: Payment): Long {
        val businessId = businessContext.activeBusinessId.first()
        require(payment.businessId == businessId) { "Payment must belong to active business" }
        require(payment.invoiceId.isNotBlank()) { "Invoice id cannot be blank" }
        require(payment.amount.isFinite()) { "Payment amount must be finite" }
        require(payment.status == "SUCCESS") { "Only successful payments can be recorded here" }
        require(payment.paymentMode in allowedPaymentModes) { "Select a valid payment method" }

        val amount = BigDecimal.valueOf(payment.amount).setScale(2, RoundingMode.HALF_UP)
        require(amount.signum() > 0) { "Payment amount must be greater than zero" }

        return database.withTransaction {
            val invoice = invoiceDao.getInvoiceById(payment.invoiceId, businessId)
                ?: throw IllegalArgumentException("Invoice not found for active business")
            require(invoice.status == "COMPLETED") { "Payments cannot be added to a cancelled invoice" }

            val paidFromLedger = BigDecimal.valueOf(
                paymentDao.getTotalPaidForInvoice(businessId, invoice.id) ?: 0.0
            ).setScale(2, RoundingMode.HALF_UP)
            val invoicePaid = BigDecimal.valueOf(invoice.amountPaid).setScale(2, RoundingMode.HALF_UP)
            require(paidFromLedger.compareTo(invoicePaid) == 0) {
                "Payment ledger does not reconcile with the invoice; reconcile it before adding another payment"
            }

            val invoiceTotal = BigDecimal.valueOf(invoice.totalAmount).setScale(2, RoundingMode.HALF_UP)
            val newPaidTotal = paidFromLedger.add(amount).setScale(2, RoundingMode.HALF_UP)
            require(newPaidTotal <= invoiceTotal) { "Payment cannot exceed the invoice balance" }

            val paymentId = paymentDao.insertPayment(
                payment.copy(
                    businessId = businessId,
                    amount = amount.toDouble(),
                    paymentDate = System.currentTimeMillis(),
                    status = "SUCCESS"
                )
            )

            val newBalanceDue = invoiceTotal.subtract(newPaidTotal).setScale(2, RoundingMode.HALF_UP)
            val summaryPaymentMethod = when {
                paidFromLedger.signum() == 0 -> payment.paymentMode
                invoice.paymentMethod == payment.paymentMode -> invoice.paymentMethod
                else -> "MULTIPLE"
            }
            invoiceDao.updateInvoice(
                invoice.copy(
                    amountPaid = newPaidTotal.toDouble(),
                    balanceDue = newBalanceDue.toDouble(),
                    paymentMethod = summaryPaymentMethod,
                    updatedAt = Instant.now()
                )
            )
            paymentId
        }
    }
}
