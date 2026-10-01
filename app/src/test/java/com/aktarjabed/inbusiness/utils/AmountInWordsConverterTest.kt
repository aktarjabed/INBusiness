package com.aktarjabed.inbusiness.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AmountInWordsConverterTest {

    @Test
    fun testZero() {
        assertEquals("Rupees Zero Only", AmountInWordsConverter.convertAmountToWords(0.0))
    }

    @Test
    fun testSimple() {
        assertEquals("Rupees One Hundred Five Only", AmountInWordsConverter.convertAmountToWords(105.0))
    }

    @Test
    fun testPaise() {
        assertEquals("Rupees Zero and Fifty Paise Only", AmountInWordsConverter.convertAmountToWords(0.50))
        assertEquals("Rupees One Hundred Five and Fifty Paise Only", AmountInWordsConverter.convertAmountToWords(105.50))
    }

    @Test
    fun testThousands() {
        assertEquals("Rupees Eight Thousand Eight Hundred Six Only", AmountInWordsConverter.convertAmountToWords(8806.0))
    }

    @Test
    fun testLakhsAndCrores() {
        assertEquals("Rupees One Lakh Twenty Three Thousand Four Hundred Fifty Six Only", AmountInWordsConverter.convertAmountToWords(123456.0))
        assertEquals("Rupees One Crore Twenty Three Lakh Forty Five Thousand Six Hundred Seventy Eight Only", AmountInWordsConverter.convertAmountToWords(12345678.0))
    }

    @Test
    fun testRoundingToNearestPaisa() {
        // Currency values are rounded to two decimals, not truncated.
        assertEquals("Rupees One and One Paise Only", AmountInWordsConverter.convertAmountToWords(1.005))
        assertEquals("Rupees One and Nine Paise Only", AmountInWordsConverter.convertAmountToWords(1.09))
        assertEquals("Rupees One and Ten Paise Only", AmountInWordsConverter.convertAmountToWords(1.10))
        assertEquals("Rupees One and One Paise Only", AmountInWordsConverter.convertAmountToWords(1.014))
    }

    @Test
    fun testLargeAmounts() {
        assertEquals(
            "Rupees One Thousand Only",
            AmountInWordsConverter.convertAmountToWords(1000.0)
        )
        assertEquals(
            "Rupees Ten Crore Only",
            AmountInWordsConverter.convertAmountToWords(100000000.0)
        )
    }

    @Test
    fun testInvalidAmountsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            AmountInWordsConverter.convertAmountToWords(-1.0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            AmountInWordsConverter.convertAmountToWords(Double.NaN)
        }
        assertThrows(IllegalArgumentException::class.java) {
            AmountInWordsConverter.convertAmountToWords(Double.POSITIVE_INFINITY)
        }
    }
}
