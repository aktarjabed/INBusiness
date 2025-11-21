package com.aktarjabed.inbusiness.data.local.dao

import androidx.room.*
import com.aktarjabed.inbusiness.data.local.entities.Invoice
import com.aktarjabed.inbusiness.data.local.entities.InvoiceItem
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface InvoiceDao {
    // Invoice operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: Invoice): Long

    @Update
    suspend fun updateInvoice(invoice: Invoice)

    @Query("SELECT * FROM invoices WHERE id = :invoiceId")
    suspend fun getInvoiceById(invoiceId: String): Invoice?

    @Query("SELECT * FROM invoices WHERE id = :invoiceId")
    fun getInvoiceByIdFlow(invoiceId: String): Flow<Invoice?>

    @Query("SELECT * FROM invoices WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getInvoicesByUser(userId: String): List<Invoice>

    @Query("SELECT * FROM invoices WHERE userId = :userId ORDER BY createdAt DESC")
    fun getInvoicesByUserFlow(userId: String): Flow<List<Invoice>>

    @Query("SELECT * FROM invoices WHERE userId = :userId AND status = :status ORDER BY createdAt DESC")
    suspend fun getInvoicesByStatus(userId: String, status: String): List<Invoice>

    @Query("SELECT * FROM invoices WHERE userId = :userId AND invoiceNumber = :invoiceNumber LIMIT 1")
    suspend fun getInvoiceByNumber(userId: String, invoiceNumber: String): Invoice?

    @Query("UPDATE invoices SET status = :status WHERE id = :invoiceId")
    suspend fun updateInvoiceStatus(invoiceId: String, status: String)

    @Query("DELETE FROM invoices WHERE id = :invoiceId")
    suspend fun deleteInvoice(invoiceId: String)

    @Query("SELECT COUNT(*) FROM invoices WHERE userId = :userId")
    suspend fun getInvoiceCountForUser(userId: String): Int

    @Query("SELECT SUM(totalAmount) FROM invoices WHERE userId = :userId AND status = 'PAID'")
    suspend fun getTotalRevenue(userId: String): Double?

    @Query("SELECT * FROM invoices WHERE dueDate < :currentDate AND status IN ('DRAFT', 'SENT')")
    suspend fun getOverdueInvoices(currentDate: LocalDateTime): List<Invoice>

    // Invoice Item operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoiceItem(item: InvoiceItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoiceItems(items: List<InvoiceItem>)

    @Update
    suspend fun updateInvoiceItem(item: InvoiceItem)

    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId ORDER BY createdAt")
    suspend fun getInvoiceItems(invoiceId: String): List<InvoiceItem>

    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId ORDER BY createdAt")
    fun getInvoiceItemsFlow(invoiceId: String): Flow<List<InvoiceItem>>

    @Query("DELETE FROM invoice_items WHERE invoiceId = :invoiceId")
    suspend fun deleteInvoiceItems(invoiceId: String)

    @Query("DELETE FROM invoice_items WHERE id = :itemId")
    suspend fun deleteInvoiceItem(itemId: String)

    @Transaction
    suspend fun insertInvoiceWithItems(invoice: Invoice, items: List<InvoiceItem>) {
        insertInvoice(invoice)
        insertInvoiceItems(items)
    }

    @Transaction
    suspend fun updateInvoiceWithItems(invoice: Invoice, items: List<InvoiceItem>) {
        updateInvoice(invoice)
        deleteInvoiceItems(invoice.id)
        insertInvoiceItems(items)
    }

    @Transaction
    suspend fun deleteInvoiceWithItems(invoiceId: String) {
        deleteInvoiceItems(invoiceId)
        deleteInvoice(invoiceId)
    }
}