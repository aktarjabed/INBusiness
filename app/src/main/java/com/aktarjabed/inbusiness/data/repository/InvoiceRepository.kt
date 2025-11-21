package com.aktarjabed.inbusiness.data.repository

import com.aktarjabed.inbusiness.data.local.dao.InvoiceDao
import com.aktarjabed.inbusiness.data.local.entities.Invoice
import com.aktarjabed.inbusiness.data.local.entities.InvoiceItem
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import com.aktarjabed.inbusiness.domain.util.Result

@Singleton
class InvoiceRepository @Inject constructor(
    private val invoiceDao: InvoiceDao
) {

    fun getInvoicesByUser(userId: String): Flow<List<Invoice>> {
        return invoiceDao.getInvoicesByUserFlow(userId)
    }

    suspend fun getInvoiceById(invoiceId: String): Invoice? {
        return try {
            invoiceDao.getInvoiceById(invoiceId)
        } catch (e: Exception) {
            Timber.e(e, "Error fetching invoice")
            null
        }
    }

    suspend fun createInvoice(invoice: Invoice, items: List<InvoiceItem>): Result<String> {
        return try {
            invoiceDao.insertInvoiceWithItems(invoice, items)
            Timber.i("Invoice created: ${invoice.id}")
            Result.success(invoice.id)
        } catch (e: Exception) {
            Timber.e(e, "Error creating invoice")
            Result.failure(e)
        }
    }

    suspend fun updateInvoice(invoice: Invoice, items: List<InvoiceItem>): Result<Unit> {
        return try {
            invoiceDao.updateInvoiceWithItems(invoice, items)
            Timber.i("Invoice updated: ${invoice.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error updating invoice")
            Result.failure(e)
        }
    }

    suspend fun deleteInvoice(invoiceId: String): Result<Unit> {
        return try {
            invoiceDao.deleteInvoiceWithItems(invoiceId)
            Timber.i("Invoice deleted: $invoiceId")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error deleting invoice")
            Result.failure(e)
        }
    }

    suspend fun getInvoiceCount(userId: String): Int {
        return try {
            invoiceDao.getInvoiceCountForUser(userId)
        } catch (e: Exception) {
            Timber.e(e, "Error getting invoice count")
            0
        }
    }
}