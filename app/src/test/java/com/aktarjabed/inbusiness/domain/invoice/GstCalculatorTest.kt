package com.aktarjabed.inbusiness.domain.invoice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GstCalculatorTest {

    // --- Intra-State Tests ---

    @Test
    fun testIntraStateRounding() {
        val result = GstCalculator.calculateItemTaxes(
            quantity = 1.0,
            unitPrice = 100.5,
            gstPercentage = 10.0,
            supplyType = SupplyType.INTRA_STATE
        )
        assertEquals(100.5, result.subtotal, 0.0001)
        assertEquals(10.05, result.taxAmount, 0.0001)
        assertEquals(5.03, result.cgstAmount, 0.0001)
        assertEquals(5.02, result.sgstAmount, 0.0001)
        assertEquals(0.0, result.igstAmount, 0.0001)
        assertEquals(110.55, result.totalAmount, 0.0001)
    }

    @Test
    fun testZeroGst() {
        val result = GstCalculator.calculateItemTaxes(
            quantity = 10.0,
            unitPrice = 15.0,
            gstPercentage = 0.0,
            supplyType = SupplyType.INTRA_STATE
        )
        assertEquals(150.0, result.subtotal, 0.0)
        assertEquals(0.0, result.taxAmount, 0.0)
        assertEquals(0.0, result.cgstAmount, 0.0)
        assertEquals(0.0, result.sgstAmount, 0.0)
        assertEquals(0.0, result.igstAmount, 0.0)
        assertEquals(150.0, result.totalAmount, 0.0)
    }

    @Test
    fun testDecimalGst() {
        val result = GstCalculator.calculateItemTaxes(
            quantity = 1.0,
            unitPrice = 100.0,
            gstPercentage = 5.5,
            supplyType = SupplyType.INTRA_STATE
        )
        assertEquals(100.0, result.subtotal, 0.0)
        assertEquals(5.5, result.taxAmount, 0.0)
        assertEquals(2.75, result.cgstAmount, 0.0)
        assertEquals(2.75, result.sgstAmount, 0.0)
        assertEquals(105.5, result.totalAmount, 0.0)
    }

    @Test
    fun testRounding() {
        val result = GstCalculator.calculateItemTaxes(
            quantity = 3.0,
            unitPrice = 1.33,
            gstPercentage = 18.0,
            supplyType = SupplyType.INTRA_STATE
        )
        assertEquals(3.99, result.subtotal, 0.0)
        assertEquals(0.72, result.taxAmount, 0.0)
        assertEquals(0.36, result.cgstAmount, 0.0)
        assertEquals(0.36, result.sgstAmount, 0.0)
        assertEquals(4.71, result.totalAmount, 0.0)
    }

    // --- Inter-State Tests ---

    @Test
    fun testInterStateRounding() {
        val result = GstCalculator.calculateItemTaxes(
            quantity = 1.0,
            unitPrice = 100.5,
            gstPercentage = 10.0,
            supplyType = SupplyType.INTER_STATE
        )
        assertEquals(100.5, result.subtotal, 0.0001)
        assertEquals(10.05, result.taxAmount, 0.0001)
        assertEquals(0.0, result.cgstAmount, 0.0001)
        assertEquals(0.0, result.sgstAmount, 0.0001)
        assertEquals(10.05, result.igstAmount, 0.0001)
        assertEquals(110.55, result.totalAmount, 0.0001)
    }

    @Test
    fun testInterStateZeroGst() {
        val result = GstCalculator.calculateItemTaxes(
            quantity = 5.0,
            unitPrice = 200.0,
            gstPercentage = 0.0,
            supplyType = SupplyType.INTER_STATE
        )
        assertEquals(1000.0, result.subtotal, 0.0)
        assertEquals(0.0, result.taxAmount, 0.0)
        assertEquals(0.0, result.igstAmount, 0.0)
        assertEquals(1000.0, result.totalAmount, 0.0)
    }

    // --- GSTIN Supply Type Resolution ---

    @Test
    fun testGstinValidationAndSupplyType() {
        assertEquals(SupplyType.INTRA_STATE, GstCalculator.determineSupplyType("29ABCDE1234F1Z5", "29XYZAB5678C1Z9"))
        assertEquals(SupplyType.INTER_STATE, GstCalculator.determineSupplyType("29ABCDE1234F1Z5", "27XYZAB5678C1Z9"))
        assertEquals(SupplyType.UNKNOWN, GstCalculator.determineSupplyType("INVALID", "27XYZAB5678C1Z9"))
        assertEquals(SupplyType.UNKNOWN, GstCalculator.determineSupplyType(null, null))
    }

    // --- Hardening: GSTIN normalization and invalid input ---

    @Test
    fun gstinValidationIsCaseInsensitiveAndRejectsMalformedValues() {
        assertTrue(GstCalculator.isValidGstin("29ABCDE1234F1Z5"))
        assertTrue("Valid GSTINs are case-insensitive", GstCalculator.isValidGstin("29abcde1234f1z5"))

        assertFalse("Blank is not a GSTIN", GstCalculator.isValidGstin(""))
        assertFalse("Blank is not a GSTIN", GstCalculator.isValidGstin("   "))
        assertFalse("null is not a GSTIN", GstCalculator.isValidGstin(null))
        assertFalse("14 characters is too short", GstCalculator.isValidGstin("29ABCDE1234F1Z"))
        assertFalse("16 characters is too long", GstCalculator.isValidGstin("29ABCDE1234F1Z55"))
        assertFalse("Missing the trailing entity code", GstCalculator.isValidGstin("29ABCDE1234F1Z"))
        assertFalse("Lowercase state code letters are not digits", GstCalculator.isValidGstin("XXABCDE1234F1Z5"))
        assertFalse("SQL-ish input is not a GSTIN", GstCalculator.isValidGstin("'; DROP TABLE invoices; --"))
    }

    @Test
    fun supplyTypeRequiresTwoValidGstins() {
        assertEquals(SupplyType.UNKNOWN, GstCalculator.determineSupplyType(null, "29ABCDE1234F1Z5"))
        assertEquals(SupplyType.UNKNOWN, GstCalculator.determineSupplyType("29ABCDE1234F1Z5", null))
        assertEquals(SupplyType.UNKNOWN, GstCalculator.determineSupplyType("29ABCDE1234F1Z5", "not-a-gstin"))
        // Same state code prefix => intra-state.
        assertEquals(SupplyType.INTRA_STATE, GstCalculator.determineSupplyType("29ABCDE1234F1Z5", "29XYZAB5678C1Z9"))
        // Different state code prefix => inter-state.
        assertEquals(SupplyType.INTER_STATE, GstCalculator.determineSupplyType("29ABCDE1234F1Z5", "18XYZAB5678C1Z9"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun unknownSupplyTypeIsRejectedAtCalculationTime() {
        GstCalculator.calculateItemTaxes(
            quantity = 1.0,
            unitPrice = 100.0,
            gstPercentage = 18.0,
            supplyType = SupplyType.UNKNOWN
        )
    }

    @Test
    fun zeroQuantityProducesZeroTotals() {
        val result = GstCalculator.calculateItemTaxes(
            quantity = 0.0,
            unitPrice = 500.0,
            gstPercentage = 18.0,
            supplyType = SupplyType.INTRA_STATE
        )
        assertEquals(0.0, result.subtotal, 0.0)
        assertEquals(0.0, result.taxAmount, 0.0)
        assertEquals(0.0, result.totalAmount, 0.0)
    }

    @Test
    fun cgstAndSgstAlwaysAddUpToTheTotalTax() {
        // Odd tax amounts must not lose or invent a paisa when splitting 50/50.
        listOf(0.01, 0.03, 1.11, 12.35, 99.99).forEach { gst ->
            val result = GstCalculator.calculateItemTaxes(
                quantity = 3.0,
                unitPrice = 7.77,
                gstPercentage = gst,
                supplyType = SupplyType.INTRA_STATE
            )
            assertEquals(
                "CGST + SGST must equal total tax for GST%=$gst",
                result.taxAmount,
                result.cgstAmount + result.sgstAmount,
                0.000_001
            )
            assertEquals(0.0, result.igstAmount, 0.0)
        }
    }

    @Test
    fun totalsAreRoundedToCurrencyPrecision() {
        val result = GstCalculator.calculateItemTaxes(
            quantity = 3.0,
            unitPrice = 3.333,
            gstPercentage = 18.0,
            supplyType = SupplyType.INTER_STATE
        )
        // 3 * 3.333 = 9.999 -> 10.00 subtotal, 18% => 1.80 tax, 11.80 total
        assertEquals(10.00, result.subtotal, 0.0)
        assertEquals(1.80, result.taxAmount, 0.0)
        assertEquals(11.80, result.totalAmount, 0.0)
    }
}
