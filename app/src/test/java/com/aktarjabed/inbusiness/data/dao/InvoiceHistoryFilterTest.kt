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

    private fun query(
        search: String? = null,
        startDate: Long? = null,
        endDate: Long? = null,
        status: String? = null,
        paymentStatus: String? = null,
        documentType: String? = null,
        limit: Int = 50,
        offset: Int = 0
    ) = InvoiceHistoryFilter(
        businessId = "biz-1",
        search = search,
        startDate = startDate,
        endDate = endDate,
        status = status,
        paymentStatus = paymentStatus,
        documentType = documentType,
        limit = limit,
        offset = offset
    ).toSQLiteQuery()

    private fun placeholderCount(sql: String): Int = sql.count { it == '?' }

    @Test
    fun alwaysScopesToTheActiveBusiness() {
        val q = query()
        assertTrue(q.sql.startsWith("SELECT * FROM invoices WHERE businessId = ?"))
        assertEquals(listOf<Any>("biz-1"), q.arguments.toList())
        assertTrue(q.sql.endsWith("ORDER BY createdAt DESC LIMIT ? OFFSET ?"))
    }

    @Test
    fun paginationIsAlwaysBound() {
        val q = query(limit = 25, offset = 100)
        val args = q.arguments.toList()
        assertEquals(25, args[args.size - 2])
        assertEquals(100, args[args.size - 1])
    }

    @Test
    fun maliciousSearchInputIsNeverInterpolated() {
        val payload = "'; DROP TABLE invoices; --"
        val q = query(search = payload)

        assertFalse(q.sql.contains("DROP TABLE"))
        assertTrue(q.sql.contains("invoiceNumber LIKE '%' || ? || '%'"))
        assertTrue(q.arguments.toList().contains(payload))
        assertEquals(placeholderCount(q.sql), q.arguments.size)
    }

    @Test
    fun filterValuesAreBoundInDeclarationOrder() {
        val q = query(
            search = "Ramesh",
            startDate = 1_000L,
            endDate = 2_000L,
            status = "COMPLETED",
            documentType = "TAX_INVOICE",
            limit = 10,
            offset = 5
        )
        assertEquals(
            listOf<Any>("biz-1", "Ramesh", "Ramesh", 1_000L, 2_000L, "COMPLETED", "TAX_INVOICE", 10, 5),
            q.arguments.toList()
        )
    }

    @Test
    fun paymentStatusFiltersAreClosedSet() {
        assertTrue(query(paymentStatus = "PAID").sql.contains("balanceDue <= 0 AND status = 'COMPLETED'"))
        assertTrue(query(paymentStatus = "UNPAID").sql.contains("balanceDue > 0 AND status = 'COMPLETED'"))
    }

    @Test
    fun blankSearchIsIgnored() {
        assertEquals(query().sql, query(search = "   ").sql)
    }

    @Test
    fun boundArgumentCountMatchesPlaceholdersForEveryCombination() {
        val variants = listOf(
            query(),
            query(search = "x"),
            query(startDate = 1L),
            query(endDate = 2L),
            query(status = "CANCELLED"),
            query(paymentStatus = "PAID"),
            query(documentType = "BILL_OF_SUPPLY"),
            query("x", 1L, 2L, "COMPLETED", "UNPAID", "TAX_INVOICE")
        )

        variants.forEach { q ->
            assertEquals(
                "placeholder/bind mismatch for: ${q.sql}",
                placeholderCount(q.sql),
                q.arguments.size
            )
        }
    }

    @Test
    fun rawQueryIsConstructedAsSimpleQuery() {
        assertTrue(query(search = "x") is SimpleSQLiteQuery)
    }
}
