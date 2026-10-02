package com.aktarjabed.inbusiness.domain.invoice

import com.aktarjabed.inbusiness.data.dao.InvoiceItemSuggestion
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Guards the invariant that replaced the hot-path SQL subquery: history rows arrive
 * newest-first and the first line per product/description key is the one that wins.
 */
class InvoiceHistoryKeyTest {

    private fun item(
        productId: Long?,
        description: String = "Item",
        price: Double = 100.0
    ) = InvoiceItemSuggestion(
        description = description,
        pricePerUnit = price,
        unitType = "BAG",
        productId = productId,
        gstPercentage = 5.0
    )

    @Test
    fun `last line per linked product wins, regardless of description casing`() {
        val history = listOf(
            item(productId = 7L, description = "Urea", price = 300.0),
            item(productId = 7L, description = "urea 50kg", price = 250.0)
        )
        val latest = history.collapseHistoryToLatest()
        assertEquals(listOf(300.0), latest.map { it.pricePerUnit })
    }

    @Test
    fun `ad-hoc lines group by trimmed, case-insensitive description`() {
        val history = listOf(
            item(productId = null, description = "  Sprayer  ", price = 900.0),
            item(productId = null, description = "sprayer", price = 850.0)
        )
        val latest = history.collapseHistoryToLatest()
        assertEquals(listOf(900.0), latest.map { it.pricePerUnit })
    }

    @Test
    fun `linked and ad-hoc lines never share a key`() {
        val linked = item(productId = 5L, description = "5")
        val adHoc = item(productId = null, description = "5")
        val latest = listOf(linked, adHoc).collapseHistoryToLatest()
        assertEquals(2, latest.size)
        assertEquals(listOf(5L, null), latest.map { it.productId })
    }

    @Test
    fun `distinct products all survive the collapse`() {
        val history = listOf(
            item(productId = 1L),
            item(productId = 2L),
            item(productId = null, description = "custom")
        )
        assertEquals(3, history.collapseHistoryToLatest().size)
    }
}
