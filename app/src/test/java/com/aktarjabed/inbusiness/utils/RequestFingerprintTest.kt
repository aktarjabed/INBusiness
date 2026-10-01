package com.aktarjabed.inbusiness.utils

import com.aktarjabed.inbusiness.data.entities.InvoiceItem
import com.aktarjabed.inbusiness.domain.invoice.SupplyType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * The request fingerprint is the duplicate-invoice guard: two requests may only be
 * treated as the same submission (idempotent replay) when every business-relevant
 * field matches. These tests protect both directions:
 *
 *  - cosmetic differences (case, surrounding whitespace, numeric formatting) must NOT
 *    produce a different fingerprint, otherwise retries create duplicate invoices;
 *  - any substantive difference must produce a different fingerprint, otherwise a
 *    modified invoice would silently replay the old one.
 */
class RequestFingerprintTest {

    private fun item(
        description: String = "Urea 50kg",
        quantity: Double = 2.0,
        pricePerUnit: Double = 300.0,
        gstPercentage: Double = 5.0,
        unitType: String = "Bag",
        productId: Long? = 1L
    ) = InvoiceItem(
        id = "",
        invoiceId = "",
        description = description,
        quantity = quantity,
        pricePerUnit = pricePerUnit,
        unitType = unitType,
        subTotal = quantity * pricePerUnit,
        gstPercentage = gstPercentage,
        taxAmount = 0.0,
        totalAmount = quantity * pricePerUnit,
        productId = productId
    )

    private fun fingerprint(
        customerName: String = "Ramesh",
        customerGSTIN: String? = "29ABCDE1234F1Z5",
        buyerAddress: String = "Cachar",
        supplyType: SupplyType = SupplyType.INTRA_STATE,
        items: List<InvoiceItem> = listOf(item()),
        amountPaid: Double = 0.0,
        paymentMethod: String = "NONE"
    ) = RequestFingerprint.generate(
        businessId = "biz-1",
        sellerName = "J.A. Agro",
        sellerAddress = "Dhanehari",
        sellerGSTIN = "18ABCDE1234F1Z5",
        customerName = customerName,
        customerGSTIN = customerGSTIN,
        buyerAddress = buyerAddress,
        supplyType = supplyType,
        subtotal = 600.0,
        totalAmount = 630.0,
        taxAmount = 30.0,
        items = items,
        amountPaid = amountPaid,
        paymentMethod = paymentMethod
    )

    @Test
    fun sameRequestProducesSameFingerprint() {
        assertEquals(fingerprint(), fingerprint())
    }

    @Test
    fun cosmeticDifferencesAreIgnored() {
        assertEquals(
            fingerprint(customerName = "  RAMESH ", customerGSTIN = "29abcde1234f1z5"),
            fingerprint(customerName = "Ramesh", customerGSTIN = "29ABCDE1234F1Z5")
        )
        assertEquals(
            fingerprint(items = listOf(item(quantity = 2.0, pricePerUnit = 300.00))),
            fingerprint(items = listOf(item(quantity = 2.000, pricePerUnit = 300.0)))
        )
    }

    @Test
    fun changesToAnyBusinessFieldChangeTheFingerprint() {
        val baseline = fingerprint()

        assertNotEquals(baseline, fingerprint(customerName = "Suresh"))
        assertNotEquals(baseline, fingerprint(customerGSTIN = "29ABCDE1234F1Z6"))
        assertNotEquals(baseline, fingerprint(customerGSTIN = null))
        assertNotEquals(baseline, fingerprint(buyerAddress = "Silchar"))
        assertNotEquals(baseline, fingerprint(supplyType = SupplyType.INTER_STATE))
        assertNotEquals(baseline, fingerprint(amountPaid = 100.0))
        assertNotEquals(baseline, fingerprint(paymentMethod = "CASH"))
        assertNotEquals(baseline, fingerprint(items = listOf(item(quantity = 3.0))))
        assertNotEquals(baseline, fingerprint(items = listOf(item(pricePerUnit = 301.0))))
        assertNotEquals(baseline, fingerprint(items = listOf(item(gstPercentage = 12.0))))
        assertNotEquals(baseline, fingerprint(items = listOf(item(unitType = "Kg"))))
        assertNotEquals(baseline, fingerprint(items = listOf(item(description = "DAP 50kg"))))
        assertNotEquals(baseline, fingerprint(items = listOf(item(productId = null))))
        assertNotEquals(baseline, fingerprint(items = listOf(item(), item())))
    }

    @Test
    fun fieldOrderingIsUnambiguous() {
        // "ab"+"c" must not hash the same as "a"+"bc": the length prefix prevents
        // boundary shifting between adjacent fields.
        assertNotEquals(
            fingerprint(customerName = "ab", buyerAddress = "c"),
            fingerprint(customerName = "a", buyerAddress = "bc")
        )
    }

    @Test
    fun fingerprintIsASha256HexDigest() {
        val value = fingerprint()
        assertEquals(64, value.length)
        assertEquals(true, value.matches(Regex("^[0-9a-f]{64}$")))
    }
}
