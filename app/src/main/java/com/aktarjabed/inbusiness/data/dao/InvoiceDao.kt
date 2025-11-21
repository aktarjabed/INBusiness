package com.aktarjabed.inbusiness.data.dao

import androidx.room.*
import com.aktarjabed.inbusiness.data.entities.Invoice
import com.aktarjabed.inbusiness.data.entities.InvoiceItem
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {

    @Query("SELECT * FROM invoices ORDER BY createdAt DESC")
    fun getAllInvoices(): Flow<List<Invoice>>

    @Query("SELECT * FROM invoices ORDER BY createdAt DESC")
    suspend fun getAllInvoicesOnce(): List<Invoice>

    @Query("SELECT * FROM invoices WHERE id = :id LIMIT 1")
    suspend fun getInvoiceById(id: String): Invoice?

    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId")
    suspend fun getInvoiceItems(invoiceId: String): List<InvoiceItem>

    @Transaction
    suspend fun insertInvoiceWithItems(invoice: Invoice, items: List<InvoiceItem>) {
        insertInvoice(invoice)
        items.forEach { insertItem(it) }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: Invoice)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: InvoiceItem)

    @Transaction
    suspend fun updateInvoiceWithItems(invoice: Invoice, items: List<InvoiceItem>) {
        updateInvoice(invoice)
        deleteItemsForInvoice(invoice.id)
        items.forEach { insertItem(it) }
    }

    @Update
    suspend fun updateInvoice(invoice: Invoice)

    @Query("DELETE FROM invoice_items WHERE invoiceId = :invoiceId")
    suspend fun deleteItemsForInvoice(invoiceId: String)

    @Delete
    suspend fun deleteInvoice(invoice: Invoice)

    @Query("SELECT * FROM invoices WHERE invoiceNumber LIKE '%' || :query || '%' OR customerName LIKE '%' || :query || '%'")
    fun searchInvoices(query: String): Flow<List<Invoice>>

    @Query("SELECT * FROM invoices ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentInvoices(limit: Int): List<Invoice>
}