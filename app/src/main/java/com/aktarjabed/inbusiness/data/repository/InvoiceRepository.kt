package com.aktarjabed.inbusiness.data.repository

import com.aktarjabed.inbusiness.data.dao.InvoiceDao
import com.aktarjabed.inbusiness.data.entities.Invoice
import com.aktarjabed.inbusiness.data.entities.InvoiceItem
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InvoiceRepository @Inject constructor(private val invoiceDao: InvoiceDao) {

    fun getAllInvoices(): Flow<List<Invoice>> = invoiceDao.getAllInvoices()

    suspend fun getInvoiceById(id: String): Invoice? = invoiceDao.getInvoiceById(id)

    suspend fun getInvoiceItems(invoiceId: String): List<InvoiceItem> = invoiceDao.getInvoiceItems(invoiceId)

    suspend fun saveInvoice(invoice: Invoice, items: List<InvoiceItem>) {
        invoiceDao.saveInvoiceWithItems(invoice, items.map { it.copy(invoiceId = invoice.id) })
    }

    suspend fun deleteInvoice(invoice: Invoice) {
        invoiceDao.deleteInvoice(invoice)
    }
}