package com.aktarjabed.inbusiness.presentation.screens.invoice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The invoice draft codec is the only thing standing between process death and a lost invoice,
 * so the interesting cases are the hostile ones: user text containing the codec's own separators,
 * unicode, and truncated/foreign payloads.
 */
class InvoiceDraftCodecTest {

    private fun item(
        description: String = "Urea 50kg",
        quantity: Double = 2.0,
        pricePerUnit: Double = 267.5,
        gstPercentage: Double = 5.0,
        unitType: String = "Bag",
        productId: Long? = 7L
    ) = InvoiceItemInput(
        description = description,
        quantity = quantity,
        pricePerUnit = pricePerUnit,
        gstPercentage = gstPercentage,
        unitType = unitType,
        productId = productId
    )

    @Test
    fun `round-trips every field of every item`() {
        val items = listOf(
            item(description = "Urea 50kg", productId = 7L),
            item(description = "DAP", quantity = 1.5, pricePerUnit = 1350.0, gstPercentage = 12.0, productId = null)
        )
        assertEquals(items, InvoiceDraftCodec.decode(InvoiceDraftCodec.encode(items)))
    }

    @Test
    fun `round-trips text containing the codec separators`() {
        // These would corrupt a delimiter-joined encoding.
        val nasty = "Semi;colon: 10;20;30  new\nline  quote\"s  back\\slash"
        val items = listOf(item(description = nasty, unitType = "1;2:3"))
        val restored = InvoiceDraftCodec.decode(InvoiceDraftCodec.encode(items))
        assertEquals(nasty, restored.single().description)
        assertEquals("1;2:3", restored.single().unitType)
    }

    @Test
    fun `round-trips unicode and emoji in descriptions`() {
        // Descriptions may be in Devanagari/Bengali, and ₹/emoji are 2 UTF-16 units; the length
        // prefix counts the same units on both sides, so these must survive byte-for-byte.
        val items = listOf(
            item(description = "यूरिया ₹1,200 🌾"),
            item(description = "ইউরিয়া ব্যাগ ✅")
        )
        assertEquals(items, InvoiceDraftCodec.decode(InvoiceDraftCodec.encode(items)))
    }

    @Test
    fun `round-trips an empty description and empty unit type`() {
        val items = listOf(item(description = "", unitType = ""))
        assertEquals(items, InvoiceDraftCodec.decode(InvoiceDraftCodec.encode(items)))
    }

    @Test
    fun `round-trips an empty item list`() {
        assertEquals(emptyList<InvoiceItemInput>(), InvoiceDraftCodec.decode(InvoiceDraftCodec.encode(emptyList())))
    }

    @Test
    fun `preserves item order`() {
        val items = (1..5).map { item(description = "item-$it") }
        assertEquals(
            listOf("item-1", "item-2", "item-3", "item-4", "item-5"),
            InvoiceDraftCodec.decode(InvoiceDraftCodec.encode(items)).map { it.description }
        )
    }

    @Test
    fun `preserves doubles that do not have a short decimal form`() {
        val items = listOf(
            item(quantity = 1.0 / 3.0, pricePerUnit = 0.1 + 0.2, gstPercentage = 5.5),
            item(quantity = 0.000_001, pricePerUnit = 123_456.789, gstPercentage = 0.0)
        )
        assertEquals(items, InvoiceDraftCodec.decode(InvoiceDraftCodec.encode(items)))
    }

    @Test
    fun `null productId stays null and a set one survives`() {
        val restored = InvoiceDraftCodec.decode(
            InvoiceDraftCodec.encode(listOf(item(productId = null), item(productId = 42L)))
        )
        assertEquals(listOf(null, 42L), restored.map { it.productId })
    }

    @Test
    fun `no draft decodes to an empty list`() {
        assertEquals(emptyList<InvoiceItemInput>(), InvoiceDraftCodec.decode(null))
        assertEquals(emptyList<InvoiceItemInput>(), InvoiceDraftCodec.decode(""))
    }

    @Test
    fun `corrupt or foreign payloads decode to an empty list instead of throwing`() {
        val hostile = listOf(
            "garbage",
            "1",
            "1;",
            "1;2;",                                     // claims 2 items, has none
            "1;1;5:abc",                                // truncated string
            "1;1;-1:x",                                 // negative length
            "1;1;9999:abc",                             // length beyond the payload
            "1;abc;",                                   // non-numeric count
            "1;100000;",                                // implausible count
            "2;0;",                                     // unknown version
            "1;1;3:abc2:u;NaNx;1;2;-",                  // malformed middle field
            "1;1;3:abc2:u;1;2;3;nope;"                  // malformed product id
        )
        for (payload in hostile) {
            assertTrue(
                "expected empty list for: '$payload'",
                InvoiceDraftCodec.decode(payload).isEmpty()
            )
        }
    }

    @Test
    fun `a draft written by a future version is ignored, not misread`() {
        val newer = "2;0;"
        assertTrue(InvoiceDraftCodec.decode(newer).isEmpty())
    }
}
