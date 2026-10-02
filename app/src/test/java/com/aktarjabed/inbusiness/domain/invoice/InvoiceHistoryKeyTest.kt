package com.aktarjabed.inbusiness.domain.invoice

import com.aktarjabed.inbusiness.data.entities.InvoiceItem
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Guards the invariant that replaced the hot-path SQL subquery: history rows arrive
 * newest-first and the first line per product/description key is the one that wins.
 */
class InvoiceHistoryKeyTest {

    private fun item(
        id: String,
        productId: Long?,
        description: String = "Item",
        price: Double = 100.0
    ) = InvoiceItem(
        id = id,
        invoiceId = "inv-$id",
        description = description,
        quantity = 1.0,
        pricePerUnit = price,
        productId = productId
    )

    @Test
    fun `last line per linked product wins, regardless of description casing`() {
        val history = listOf(
            item("a", productId = 7L, description = "Urea", price = 300.0),
            item("b", productId = 7L, description = "urea 50kg", price = 250.0)
        )
        val latest = history.collapseHistoryToLatest()
        assertEquals(listOf("a"), latest.map { it.id })
    }

    @Test
    fun `ad-hoc lines group by trimmed, case-insensitive description`() {
        val history = listOf(
            item("a", productId = null, description = "  Sprayer  ", price = 900.0),
            item("b", productId = null, description = "sprayer", price = 850.0)
        )
        val latest = history.collapseHistoryToLatest()
        assertEquals(listOf("a"), latest.map { it.id })
    }

    @Test
    fun `linked and ad-hoc lines never share a key`() {
        val linked = item("a", productId = 5L, description = "5")
        val adHoc = item("b", productId = null, description = "5")
        assertEquals(listOf("a", "b"), listOf(linked, adHoc).collapseHistoryToLatest().map { it.id })
    }

    @Test
    fun `distinct products all survive the collapse`() {
        val history = listOf(
            item("a", productId = 1L),
            item("b", productId = 2L),
            item("c", productId = null, description = "custom")
        )
        assertEquals(3, history.collapseHistoryToLatest().size)
    }
}
