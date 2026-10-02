package com.aktarjabed.inbusiness.domain.invoice

import java.math.BigDecimal
import java.math.RoundingMode

enum class SupplyType {
    INTRA_STATE,
    INTER_STATE,
    UNKNOWN
}

object GstCalculator {
    /**
     * Regex for validating Indian GSTIN format.
     */
    private val GSTIN_REGEX = Regex("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$")

    /** Base-36 code point alphabet used by the GSTIN check digit (Luhn mod 36). */
    private const val GSTIN_CODE_POINTS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"

    /**
     * Validates a GSTIN's format **and** its check digit.
     *
     * The 15th character is a Luhn mod 36 check digit over the first 14. Format-only validation
     * lets a mistyped-but-well-formed GSTIN through, and because the first two digits are the
     * state code, a single mistyped state digit silently flips the invoice between CGST+SGST and
     * IGST — a tax-compliance error, not a cosmetic one. A typo anywhere else in the PAN is only
     * caught by the check digit.
     *
     * The check digit is a hard gate in a few places: [determineSupplyType] returns UNKNOWN for a
     * failing GSTIN, and the setup/invoice screens refuse to save one, so an unregistered buyer
     * must leave the field blank (the intended flow) rather than entering a placeholder. The
     * invoice screen's manual Intra/Inter selector is the escape hatch when a real counterparty's
     * GSTIN still fails.
     */
    fun isValidGstin(gstin: String?): Boolean {
        if (gstin.isNullOrBlank()) return false
        val normalized = gstin.trim().uppercase()
        if (!GSTIN_REGEX.matches(normalized)) return false
        return normalized[14] == gstinCheckDigit(normalized.substring(0, 14))
    }

    /**
     * Luhn mod 36 check digit for the first 14 characters of a GSTIN.
     * The specification's worked example is `27AAPFU0939F1ZV` -> `V`.
     */
    private fun gstinCheckDigit(first14: String): Char {
        var sum = 0
        for ((index, char) in first14.withIndex()) {
            val product = GSTIN_CODE_POINTS.indexOf(char) * if (index % 2 == 0) 1 else 2
            sum += product / 36 + product % 36
        }
        return GSTIN_CODE_POINTS[(36 - sum % 36) % 36]
    }

    /**
     * Determines SupplyType by comparing the first two digits (state code) of GSTINs.
     * Fallbacks to UNKNOWN if either GSTIN is missing, invalid, or manual override is desired.
     */
    fun determineSupplyType(sellerGstin: String?, buyerGstin: String?): SupplyType {
        if (sellerGstin.isNullOrBlank() || buyerGstin.isNullOrBlank()) {
            return SupplyType.UNKNOWN
        }

        if (!isValidGstin(sellerGstin) || !isValidGstin(buyerGstin)) {
            return SupplyType.UNKNOWN
        }

        // Trim only; case is comparison-irrelevant (digits vs digits) and the values themselves
        // are normalized to uppercase before they are persisted by the callers.
        val sellerState = sellerGstin.trim().substring(0, 2)
        val buyerState = buyerGstin.trim().substring(0, 2)

        return if (sellerState == buyerState) {
            SupplyType.INTRA_STATE
        } else {
            SupplyType.INTER_STATE
        }
    }

    /**
     * Calculates tax amounts for a single item based on its subtotal (qty * price) and GST percentage.
     * Rounding to 2 decimal places is commonly expected for currency.
     */
    fun calculateItemTaxes(
        quantity: Double,
        unitPrice: Double,
        gstPercentage: Double,
        supplyType: SupplyType
    ): ItemTaxResult {
        val qty = BigDecimal.valueOf(quantity)
        val price = BigDecimal.valueOf(unitPrice)
        val gstPct = BigDecimal.valueOf(gstPercentage)

        // subTotal = round(quantity * rate, 2)
        val rawSubtotal = qty.multiply(price)
        val subtotal = rawSubtotal.setScale(2, RoundingMode.HALF_UP)

        // taxAmount = round(unroundedLineSubtotal * gst% / 100, 2)
        val rawTaxAmount = rawSubtotal.multiply(gstPct).divide(BigDecimal.valueOf(100.0))
        val taxAmount = rawTaxAmount.setScale(2, RoundingMode.HALF_UP)

        val totalAmount = subtotal.add(taxAmount)

        val (cgst, sgst, igst) = when (supplyType) {
            SupplyType.INTRA_STATE -> {
                // cgst = round(taxAmount / 2, 2)
                val cgstRaw = taxAmount.divide(BigDecimal.valueOf(2.0))
                val cgstRounded = cgstRaw.setScale(2, RoundingMode.HALF_UP)
                // sgst = taxAmount - cgst
                val sgstRounded = taxAmount.subtract(cgstRounded)
                Triple(cgstRounded.toDouble(), sgstRounded.toDouble(), 0.0)
            }
            SupplyType.INTER_STATE -> Triple(0.0, 0.0, taxAmount.toDouble())
            SupplyType.UNKNOWN -> throw IllegalArgumentException("SupplyType cannot be UNKNOWN during tax calculation")
        }

        return ItemTaxResult(
            subtotal = subtotal.toDouble(),
            taxAmount = taxAmount.toDouble(),
            totalAmount = totalAmount.toDouble(),
            cgstAmount = cgst,
            sgstAmount = sgst,
            igstAmount = igst
        )
    }

    data class ItemTaxResult(
        val subtotal: Double,
        val taxAmount: Double,
        val totalAmount: Double,
        val cgstAmount: Double,
        val sgstAmount: Double,
        val igstAmount: Double
    )
}
