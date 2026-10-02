package com.aktarjabed.inbusiness.data.dao

import androidx.room.*
import com.aktarjabed.inbusiness.data.entities.Invoice
import com.aktarjabed.inbusiness.data.entities.InvoiceItem
import com.aktarjabed.inbusiness.data.entities.InvoiceSequence
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {

    @Query("SELECT * FROM invoices WHERE businessId = :businessId ORDER BY createdAt DESC")
    fun getAllInvoices(businessId: String): Flow<List<Invoice>>

    @Query("SELECT * FROM invoices WHERE businessId = :businessId ORDER BY createdAt DESC")
    suspend fun getAllInvoicesOnce(businessId: String): List<Invoice>

    @Query("SELECT * FROM invoices WHERE id = :id AND businessId = :businessId LIMIT 1")
    suspend fun getInvoiceById(id: String, businessId: String): Invoice?

    @Query("SELECT * FROM invoices WHERE idempotencyKey = :idempotencyKey AND businessId = :businessId LIMIT 1")
    suspend fun getInvoiceByIdempotencyKey(idempotencyKey: String, businessId: String): Invoice?

    @Query("SELECT * FROM invoice_items WHERE invoiceId = :invoiceId AND invoiceId IN (SELECT id FROM invoices WHERE businessId = :businessId)")
    suspend fun getInvoiceItems(invoiceId: String, businessId: String): List<InvoiceItem>

    /**
     * Every invoice line for [businessId], newest invoice first, with the same total order
     * ("invoice createdAt DESC, item id DESC") the previous implementation used.
     *
     * This deliberately returns *all* lines instead of filtering to one row per product in SQL.
     * The previous query used a correlated scalar subquery in the WHERE clause, which SQLite
     * re-evaluates once per candidate row: on a real engine 1.5k lines took ~0.6 s and 20k lines
     * took >100 s, so the invoice screen (product autocomplete) would jank or ANR on real data.
     *
     * Callers that need one line per product must collapse the list with
     * `collapseHistoryToLatest()`; because the order above matches the old subquery exactly,
     * "first row per key" selects the identical line the old query returned.
     */
    @Query("""
        SELECT i.*
        FROM invoice_items i
        INNER JOIN invoices inv ON i.invoiceId = inv.id
        WHERE inv.businessId = :businessId
        ORDER BY inv.createdAt DESC, i.id DESC
    """)
    fun getHistoricalInvoiceItems(businessId: String): Flow<List<InvoiceItem>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertInvoice(invoice: Invoice)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertItem(item: InvoiceItem)

    @Update
    suspend fun updateInvoice(invoice: Invoice)

    @RawQuery
    suspend fun getInvoicesByQuery(query: androidx.sqlite.db.SupportSQLiteQuery): List<Invoice>

    @Query("SELECT * FROM invoice_sequence WHERE businessId = :businessId LIMIT 1")
    suspend fun getInvoiceSequence(businessId: String): InvoiceSequence?

    @Query("UPDATE invoice_sequence SET lastSequenceNumber = lastSequenceNumber + 1 WHERE businessId = :businessId")
    suspend fun incrementSequence(businessId: String): Int

    /**
     * Creates the per-business counter row if it is missing. IGNORE (never REPLACE) is
     * essential: blindly upserting the sequence would let invoice numbers be reused.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSequence(sequence: InvoiceSequence): Long
}
