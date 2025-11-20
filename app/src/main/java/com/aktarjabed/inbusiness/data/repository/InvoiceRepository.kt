package com.aktarjabed.inbusiness.data.repository

import com.aktarjabed.inbusiness.data.dao.InvoiceDao
import com.aktarjabed.inbusiness.data.entities.Invoice
import com.aktarjabed.inbusiness.data.entities.InvoiceItem
import com.aktarjabed.inbusiness.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InvoiceRepository @Inject constructor(
    private val invoiceDao: InvoiceDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    fun getAllInvoices(): Flow<List<Invoice>> = invoiceDao.getAllInvoices()

    suspend fun getAllInvoicesOnce(): List<Invoice> = withContext(ioDispatcher) {
        invoiceDao.getAllInvoicesOnce()
    }

    suspend fun getInvoiceById(id: String): Invoice? = withContext(ioDispatcher) {
        invoiceDao.getInvoiceById(id)
    }

    suspend fun getInvoiceItems(invoiceId: String): List<InvoiceItem> = withContext(ioDispatcher) {
        invoiceDao.getInvoiceItems(invoiceId)
    }

    suspend fun createInvoice(invoice: Invoice, items: List<InvoiceItem>) = withContext(ioDispatcher) {
        invoiceDao.insertInvoiceWithItems(invoice, items)
    }

    suspend fun updateInvoice(invoice: Invoice, items: List<InvoiceItem>) = withContext(ioDispatcher) {
        invoiceDao.updateInvoiceWithItems(invoice, items)
    }

    suspend fun deleteInvoice(invoice: Invoice) = withContext(ioDispatcher) {
        invoiceDao.deleteInvoice(invoice)
    }

    fun searchInvoices(query: String): Flow<List<Invoice>> =
        invoiceDao.searchInvoices(query)

    suspend fun getRecentInvoices(limit: Int = 10): List<Invoice> = withContext(ioDispatcher) {
        invoiceDao.getRecentInvoices(limit)
    }
}