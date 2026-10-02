package com.aktarjabed.inbusiness.data.repository

import android.util.Log
import androidx.room.withTransaction
import com.aktarjabed.inbusiness.data.dao.InvoiceDao
import com.aktarjabed.inbusiness.data.dao.InvoiceItemSuggestion
import com.aktarjabed.inbusiness.data.dao.ProductDao
import com.aktarjabed.inbusiness.data.database.AppDatabase
import com.aktarjabed.inbusiness.data.entities.Invoice
import com.aktarjabed.inbusiness.data.entities.InvoiceItem
import com.aktarjabed.inbusiness.data.dao.BusinessDao
import com.aktarjabed.inbusiness.data.entities.InvoiceSequence
import com.aktarjabed.inbusiness.domain.invoice.CalculateInvoiceTotalsUseCase
import com.aktarjabed.inbusiness.domain.invoice.InvoiceCreationResult
import com.aktarjabed.inbusiness.domain.invoice.SupplyType
import com.aktarjabed.inbusiness.domain.invoice.collapseHistoryToLatest
import com.aktarjabed.inbusiness.domain.context.BusinessContext
import com.aktarjabed.inbusiness.domain.quota.QuotaGate
import com.aktarjabed.inbusiness.domain.quota.QuotaVerdict
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.UUID

import javax.inject.Inject
import javax.inject.Singleton

private class TransactionAbortException(val result: com.aktarjabed.inbusiness.domain.invoice.InvoiceCreationResult) : Exception() {
    /**
     * Aborting a transaction is normal control flow here (quota exceeded, insufficient stock,
     * idempotent replay), and JVM stack-trace capture on every expected rejection is pure
     * overhead on a hot path. Domain failure details live in [result], not in a stack trace.
     */
    override fun fillInStackTrace(): Throwable = this
}

@Singleton
class InvoiceRepository @Inject constructor(
    private val database: AppDatabase,
    private val invoiceDao: InvoiceDao,
    private val paymentDao: com.aktarjabed.inbusiness.data.dao.PaymentDao,
    private val stockMovementDao: com.aktarjabed.inbusiness.data.dao.StockMovementDao,
    private val productDao: ProductDao,
    private val businessDao: BusinessDao,
    private val calculateInvoiceTotalsUseCase: CalculateInvoiceTotalsUseCase,
    private val quotaGate: QuotaGate,
    private val businessContext: BusinessContext
) {

    companion object {
        private const val TAG = "InvoiceRepository"
        private val VALID_PAYMENT_METHODS = setOf("CASH", "CARD", "UPI", "BANK_TRANSFER")
    }

    suspend fun getAllInvoicesOnce(): List<Invoice> {
        val businessId = businessContext.activeBusinessId.first()
        return invoiceDao.getAllInvoicesOnce(businessId)
    }

    suspend fun getInvoiceById(id: String): Invoice? {
        val businessId = businessContext.activeBusinessId.first()
        return invoiceDao.getInvoiceById(id, businessId)
    }

    suspend fun getInvoiceItems(invoiceId: String): List<InvoiceItem> {
        val businessId = businessContext.activeBusinessId.first()
        return invoiceDao.getInvoiceItems(invoiceId, businessId)
    }

    /**
     * The latest line per product (or per free-text description for ad-hoc lines), projected to
     * only the fields required by invoice-item autocomplete.
     *
     * The DAO returns raw rows newest-first and the collapse happens here, avoiding the correlated
     * "latest id per product" subquery that made the history query quadratic in its row count.
     */
    fun getHistoricalInvoiceItems(): Flow<List<InvoiceItemSuggestion>> {
        return businessContext.activeBusinessId.flatMapLatest { businessId ->
            invoiceDao.getHistoricalInvoiceItems(businessId)
                .map { items -> items.collapseHistoryToLatest() }
        }
    }




    suspend fun createInvoice(
        customerName: String,
        customerGSTIN: String?,
        buyerAddress: String,
        supplyType: SupplyType,
        items: List<InvoiceItem>,
        idempotencyKey: String? = null,
        amountPaid: Double = 0.0,
        paymentMethod: String = "NONE"
    ): InvoiceCreationResult = withContext(Dispatchers.IO) {

        if (items.isEmpty()) {
            return@withContext InvoiceCreationResult.InvalidRequest("Invoice must have at least one item")
        }
        if (amountPaid > 0.0 && paymentMethod !in VALID_PAYMENT_METHODS) {
            return@withContext InvoiceCreationResult.InvalidRequest("Select a valid payment method for the initial payment")
        }

        // A DataStore read failure (or a missing context value) has to come back as a typed
        // InvoiceCreationResult like any other failure: callers switch on the returned result
        // instead of catching exceptions, so letting one escape here would surface as a crash.
        // Both reads happen *before* withTransaction opens, so a database transaction is never
        // held open across unrelated DataStore I/O.
        val (businessId, userId) = try {
            businessContext.activeBusinessId.first() to businessContext.currentUserId.first()
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e(TAG, "Business context unavailable; cannot create an invoice", e)
            return@withContext InvoiceCreationResult.UnexpectedFailure(e)
        }

        var requestFingerprint: String? = null

        try {
            database.withTransaction {
                val businessData = businessDao.getBusinessDataById(businessId)
                    ?: throw TransactionAbortException(InvoiceCreationResult.InvalidRequest("Business data not found"))

                val sellerName = businessData.name
                val sellerAddress = businessData.address
                val sellerGSTIN = businessData.gstin

                val calcResult = try {
                    calculateInvoiceTotalsUseCase(items, supplyType, amountPaid)
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    throw TransactionAbortException(InvoiceCreationResult.InvalidRequest(e.message ?: "Invalid calculation"))
                }

                val effectivePaymentMethod = if (calcResult.amountPaid > 0.0) paymentMethod else "NONE"
                requestFingerprint = com.aktarjabed.inbusiness.utils.RequestFingerprint.generate(
                    businessId = businessId,
                    sellerName = sellerName,
                    sellerAddress = sellerAddress,
                    sellerGSTIN = sellerGSTIN,
                    customerName = customerName,
                    customerGSTIN = customerGSTIN,
                    buyerAddress = buyerAddress,
                    supplyType = supplyType,
                    subtotal = calcResult.subtotal,
                    totalAmount = calcResult.totalAmount,
                    taxAmount = calcResult.taxAmount,
                    items = calcResult.processedItems,
                    amountPaid = calcResult.amountPaid,
                    paymentMethod = effectivePaymentMethod
                )

                // 1. Idempotency Check (Fast path)
                if (idempotencyKey != null) {
                    val existingInvoice = invoiceDao.getInvoiceByIdempotencyKey(idempotencyKey, businessId)
                    if (existingInvoice != null) {
                        if (existingInvoice.requestFingerprint == requestFingerprint) {
                            throw TransactionAbortException(InvoiceCreationResult.IdempotentReplay(existingInvoice.id, existingInvoice.invoiceNumber))
                        } else {
                            throw TransactionAbortException(InvoiceCreationResult.InvalidRequest("Idempotency key reused for a different payload"))
                        }
                    }
                }

                // 2. Consume Quota within the transaction
                val verdict = quotaGate.assertQuota(userId, consume = true)
                if (verdict !is QuotaVerdict.Allowed) {
                    throw TransactionAbortException(InvoiceCreationResult.QuotaExceeded(
                        required = 1,
                        available = 0 // Approximate for now, could be enhanced
                    ))
                }

                val invoiceId = UUID.randomUUID().toString()

                // 3. Stock Deductions for linked products
                // Iterating the *processed* items guarantees that only lines that passed
                // validation can move stock, and that the numbers written to the ledger are
                // the same ones persisted on the invoice.
                for (item in calcResult.processedItems) {
                    if (item.productId != null) {
                        val product = productDao.getProductById(item.productId, businessId)
                            ?: throw TransactionAbortException(InvoiceCreationResult.ProductNotFound(item.productId))

                        val affectedRows = productDao.deductStock(item.productId, businessId, item.quantity)
                        if (affectedRows == 0) {
                            // Rollback and return InsufficientStock
                            throw TransactionAbortException(InvoiceCreationResult.InsufficientStock(
                                productId = item.productId,
                                productName = product.name,
                                requested = item.quantity,
                                available = product.availableStock
                            ))
                        }

                        stockMovementDao.insertMovement(
                            com.aktarjabed.inbusiness.data.entities.StockMovement(
                                businessId = businessId,
                                productId = item.productId,
                                movementType = "SALE",
                                quantity = -item.quantity,
                                stockBefore = product.availableStock,
                                stockAfter = product.availableStock - item.quantity,
                                referenceType = "INVOICE",
                                referenceId = invoiceId,
                                reason = "Invoice creation"
                            )
                        )
                    }
                }

                // 4. Atomic Sequence logic
                // Ensure the sequence row exists safely
                invoiceDao.insertSequence(InvoiceSequence(businessId, 0))

                // Atomically increment
                invoiceDao.incrementSequence(businessId)

                // Read the resulting value
                val currentSeq = invoiceDao.getInvoiceSequence(businessId)
                val nextSeqNumber = currentSeq?.lastSequenceNumber ?: 1

                val nextInvoiceNumber = "INV-${String.format(java.util.Locale.US, "%05d", nextSeqNumber)}"

                val updatedItems = calcResult.processedItems.map {
                    it.copy(
                        invoiceId = invoiceId,
                        id = UUID.randomUUID().toString()
                    )
                }

                // Capture the invoice timestamp after validation, quota consumption,
                // stock updates, sequence allocation, and line preparation, immediately before
                // persisting the header. Reuse it for the initial payment so those records align.
                val now = Instant.now()
                val invoice = Invoice(
                    id = invoiceId,
                    businessId = businessId,
                    idempotencyKey = idempotencyKey,
                    requestFingerprint = requestFingerprint,
                    invoiceNumber = nextInvoiceNumber,
                    sellerName = sellerName,
                    sellerAddress = sellerAddress,
                    sellerGSTIN = sellerGSTIN,
                    customerId = "", // Reserved for full customer management
                    customerName = customerName,
                    customerGSTIN = customerGSTIN,
                    buyerAddress = buyerAddress,
                    subtotal = calcResult.subtotal,
                    totalAmount = calcResult.totalAmount,
                    taxAmount = calcResult.taxAmount,
                    totalCgst = calcResult.totalCgst,
                    totalSgst = calcResult.totalSgst,
                    totalIgst = calcResult.totalIgst,
                    supplyType = supplyType.name,
                    amountPaid = calcResult.amountPaid,
                    balanceDue = calcResult.balanceDue,
                    paymentMethod = effectivePaymentMethod,
                    createdAt = now,
                    updatedAt = now
                )

                invoiceDao.insertInvoice(invoice)
                updatedItems.forEach { invoiceDao.insertItem(it) }

                // 5. Initial Payment Insertion
                if (calcResult.amountPaid > 0.0) {
                    paymentDao.insertPayment(
                        com.aktarjabed.inbusiness.data.entities.Payment(
                            businessId = businessId,
                            invoiceId = invoiceId,
                            amount = calcResult.amountPaid,
                            paymentMode = effectivePaymentMethod,
                            paymentDate = now.toEpochMilli()
                        )
                    )
                }

                return@withTransaction InvoiceCreationResult.Success(invoiceId, nextInvoiceNumber)
            }
        } catch (e: TransactionAbortException) {
            return@withContext e.result
        } catch (e: android.database.sqlite.SQLiteConstraintException) {
             // 5. Concurrency fallback for Idempotency (Slow path - unique constraint collision)
             // Transaction has rolled back by now
             Log.e(TAG, "Idempotency constraint conflict", e)
             val msg = e.message ?: ""
             if (!msg.contains("idempotencyKey", ignoreCase = true) && !msg.contains("index_invoices_businessId_idempotencyKey", ignoreCase = true)) {
                 return@withContext InvoiceCreationResult.UnexpectedFailure(e)
             }
             if (idempotencyKey != null) {
                 val existing = invoiceDao.getInvoiceByIdempotencyKey(idempotencyKey, businessId)
                 if (existing != null) {
                     if (existing.requestFingerprint == requestFingerprint) {
                         return@withContext InvoiceCreationResult.IdempotentReplay(existing.id, existing.invoiceNumber)
                     } else {
                         return@withContext InvoiceCreationResult.InvalidRequest("Idempotency key reused for a different payload")
                     }
                 }
             }
             // No matching invoice exists, propagate the original DB failure
             return@withContext InvoiceCreationResult.UnexpectedFailure(e)
        } catch (e: kotlinx.coroutines.CancellationException) {
             throw e // Explicitly rethrow CancellationException
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e(TAG, "Failed to create invoice", e)
            return@withContext InvoiceCreationResult.UnexpectedFailure(e)
        }
    }

    suspend fun cancelInvoice(invoiceId: String): com.aktarjabed.inbusiness.domain.invoice.InvoiceCreationResult = withContext(Dispatchers.IO) {
        try {
            // Inside the try so a DataStore/context failure is reported through the same
            // typed result as every other cancellation failure instead of escaping.
            val businessId = businessContext.activeBusinessId.first()
            val invoiceNumber = database.withTransaction {
                val invoice = invoiceDao.getInvoiceById(invoiceId, businessId)
                    ?: throw TransactionAbortException(InvoiceCreationResult.InvalidRequest("Invoice not found"))

                if (invoice.status == "CANCELLED") {
                    throw TransactionAbortException(InvoiceCreationResult.InvalidRequest("Invoice is already cancelled"))
                }

                val hasSuccessfulPayments = paymentDao.hasSuccessfulPaymentForInvoice(businessId, invoiceId)
                if (invoice.amountPaid > 0.0 || hasSuccessfulPayments) {
                    throw TransactionAbortException(
                        InvoiceCreationResult.InvalidRequest(
                            "This invoice has recorded payments. Refund/reconcile payments before cancellation."
                        )
                    )
                }

                val items = invoiceDao.getInvoiceItems(invoiceId, businessId)

                // Validate the whole reversal before mutating anything: skipping a missing
                // product would cancel the invoice while leaving its stock deducted, which
                // silently corrupts inventory. Aborting rolls the transaction back instead.
                val linkedProducts = items.mapNotNull { item ->
                    item.productId?.let { productId ->
                        val product = productDao.getProductById(productId, businessId)
                            ?: throw TransactionAbortException(
                                InvoiceCreationResult.InvalidRequest(
                                    "Cannot cancel: product $productId referenced by this invoice no longer exists"
                                )
                            )
                        item to product
                    }
                }

                invoiceDao.updateInvoice(invoice.copy(status = "CANCELLED", updatedAt = Instant.now()))

                for ((item, product) in linkedProducts) {
                    productDao.addStock(product.id, businessId, item.quantity)
                    stockMovementDao.insertMovement(
                        com.aktarjabed.inbusiness.data.entities.StockMovement(
                            businessId = businessId,
                            productId = product.id,
                            movementType = "SALE_REVERSAL",
                            quantity = item.quantity,
                            stockBefore = product.availableStock,
                            stockAfter = product.availableStock + item.quantity,
                            referenceType = "INVOICE",
                            referenceId = invoiceId,
                            reason = "Invoice cancelled"
                        )
                    )
                }
                invoice.invoiceNumber
            }
            InvoiceCreationResult.Success(invoiceId, invoiceNumber)
        } catch (e: TransactionAbortException) {
            e.result
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            InvoiceCreationResult.InvalidRequest("Cancellation failed: ${e.message}")
        }
    }
}