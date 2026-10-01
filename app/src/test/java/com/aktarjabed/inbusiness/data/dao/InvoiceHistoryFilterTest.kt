package com.aktarjabed.inbusiness.data.dao

import androidx.sqlite.db.SimpleSQLiteQuery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Invoice history search is built with a raw SQL string, which makes it the most
 * security-sensitive query in the app. These tests pin two guarantees:
 *
 * 1. every user supplied value is passed as a bind argument, never interpolated;
 * 2. the placeholder count always matches the number of bind arguments, so no filter
 *    combination can produce a runtime "bind argument mismatch" crash.
 *
 * A regression here would be either an SQL injection vector or a crash on a specific
 * filter combination.
 */
class InvoiceHistoryFilterTest {

    private fun builtQuery(
        search: String? = null,
        startDate: Long? = null,
        endDate: Long? = null,
        status: String? = null,
        paymentStatus: String? = null,
        documentType: String? = null,
        limit: Int = 50,
        offset: Int = 0
    ): InvoiceHistoryFilter.BuiltQuery = InvoiceHistoryFilter(
        businessId = "biz-1",
        search = search,
        startDate = startDate,
        endDate = endDate,
        status = status,
        paymentStatus = paymentStatus,
        documentType = documentType,
        limit = limit,
        offset = offset
    ).buildQuery()

    private fun placeholderCount(sql: String): Int = sql.count { it == '?' }

    @Test
    fun alwaysScopesToTheActiveBusiness() {
        val q = builtQuery()
        assertTrue(q.sql.startsWith("SELECT * FROM invoices WHERE businessId = ?"))
        // The business id is always the first bind argument; pagination binds follow it.
        assertEquals("biz-1", q.args.first())
        assertEquals(listOf<Any?>("biz-1", 50, 0), q.args)
        assertTrue(q.sql.endsWith("ORDER BY createdAt DESC LIMIT ? OFFSET ?"))
    }

    @Test
    fun paginationIsAlwaysBound() {
        val q = builtQuery(limit = 25, offset = 100)
        assertEquals(25, q.args[q.args.size - 2])
        assertEquals(100, q.args[q.args.size - 1])
    }

    @Test
    fun maliciousSearchInputIsNeverInterpolated() {
        val payload = "'; DROP TABLE invoices; --"
        val q = builtQuery(search = payload)

        assertFalse(q.sql.contains("DROP TABLE"))
        assertTrue(q.sql.contains("invoiceNumber LIKE '%' || ? || '%'"))
        assertTrue(q.args.contains(payload))
        assertEquals(placeholderCount(q.sql), q.args.size)
    }

    @Test
    fun filterValuesAreBoundInDeclarationOrder() {
        val q = builtQuery(
            search = "Ramesh",
            startDate = 1_000L,
            endDate = 2_000L,
            status = "COMPLETED",
            documentType = "TAX_INVOICE",
            limit = 10,
            offset = 5
        )
        assertEquals(
            listOf<Any?>("biz-1", "Ramesh", "Ramesh", 1_000L, 2_000L, "COMPLETED", "TAX_INVOICE", 10, 5),
            q.args
        )
    }

    @Test
    fun paymentStatusFiltersAreClosedSet() {
        assertTrue(builtQuery(paymentStatus = "PAID").sql.contains("balanceDue <= 0 AND status = 'COMPLETED'"))
        assertTrue(builtQuery(paymentStatus = "UNPAID").sql.contains("balanceDue > 0 AND status = 'COMPLETED'"))
    }

    @Test
    fun blankSearchIsIgnored() {
        assertEquals(builtQuery().sql, builtQuery(search = "   ").sql)
        assertEquals(builtQuery().args, builtQuery(search = "   ").args)
    }

    @Test
    fun boundArgumentCountMatchesPlaceholdersForEveryCombination() {
        val variants = listOf(
            builtQuery(),
            builtQuery(search = "x"),
            builtQuery(startDate = 1L),
            builtQuery(endDate = 2L),
            builtQuery(status = "CANCELLED"),
            builtQuery(paymentStatus = "PAID"),
            builtQuery(documentType = "BILL_OF_SUPPLY"),
            builtQuery("x", 1L, 2L, "COMPLETED", "UNPAID", "TAX_INVOICE")
        )

        variants.forEach { q ->
            assertEquals(
                "placeholder/bind mismatch for: ${q.sql}",
                placeholderCount(q.sql),
                q.args.size
            )
        }
    }

    @Test
    fun rawQueryIsConstructedAsSimpleQuery() {
        val filter = InvoiceHistoryFilter(businessId = "biz-1", search = "x")

        val rawQuery = filter.toSQLiteQuery()
        assertTrue(rawQuery is SimpleSQLiteQuery)
        // The DAO receives exactly the SQL the builder produced.
        assertEquals(filter.buildQuery().sql, rawQuery.sql)
    }
}
