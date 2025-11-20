package com.aktarjabed.inbusiness.domain.ai

import com.aktarjabed.inbusiness.data.entities.Invoice
import com.aktarjabed.inbusiness.domain.models.Anomaly
import org.apache.commons.math3.stat.StatUtils
import org.apache.commons.math3.util.FastMath
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class AnomalyDetector @Inject constructor() {

    fun detectAnomalies(
        invoice: Invoice,
        historicalInvoices: List<Invoice>
    ): List<Anomaly> {
        if (historicalInvoices.isEmpty()) {
            return emptyList()
        }

        val anomalies = mutableListOf<Anomaly>()

        // Check 1: Amount outlier detection
        checkAmountAnomaly(invoice, historicalInvoices)?.let { anomalies.add(it) }

        // Check 2: GST ratio validation
        checkGSTRatio(invoice)?.let { anomalies.add(it) }

        // Check 3: Duplicate detection
        checkDuplicates(invoice, historicalInvoices)?.let { anomalies.add(it) }

        // Check 4: Unusual timing
        checkTimingAnomaly(invoice, historicalInvoices)?.let { anomalies.add(it) }

        // Check 5: Round number detection
        checkRoundNumbers(invoice)?.let { anomalies.add(it) }

        return anomalies
    }

    private fun checkAmountAnomaly(
        invoice: Invoice,
        historical: List<Invoice>
    ): Anomaly? {
        val amounts = historical.map { it.totalAmount }.toDoubleArray()

        if (amounts.isEmpty()) return null

        val mean = StatUtils.mean(amounts)
        val stdDev = FastMath.sqrt(StatUtils.variance(amounts))

        if (stdDev < 0.01) return null // Avoid division by near-zero

        val zScore = abs((invoice.totalAmount - mean) / stdDev)

        return if (zScore > Z_SCORE_THRESHOLD) {
            Anomaly(
                type = Anomaly.Type.AMOUNT_OUTLIER,
                severity = when {
                    zScore > 4.0 -> Anomaly.Severity.HIGH
                    zScore > 3.0 -> Anomaly.Severity.MEDIUM
                    else -> Anomaly.Severity.LOW
                },
                score = (zScore / 10.0).coerceIn(0.0, 1.0),
                title = "Unusual Invoice Amount",
                message = "Amount ₹${"%.2f".format(invoice.totalAmount)} is " +
                        "${"%.1f".format(zScore)}σ from your average of ₹${"%.2f".format(mean)}",
                suggestion = "Verify quantities, rates, and tax calculations",
                field = "totalAmount"
            )
        } else null
    }

    private fun checkGSTRatio(invoice: Invoice): Anomaly? {
        if (invoice.totalAmount == 0.0) return null

        val gstTotal = invoice.cgstAmount + invoice.sgstAmount + invoice.igstAmount
        val gstRatio = gstTotal / invoice.totalAmount

        // Expected GST ratio should be between 12% and 20% for most cases
        return if (gstRatio !in GST_RATIO_MIN..GST_RATIO_MAX) {
            Anomaly(
                type = Anomaly.Type.TAX_ANOMALY,
                severity = Anomaly.Severity.MEDIUM,
                score = 0.7,
                title = "Unusual GST Ratio",
                message = "GST is ${"%.1f".format(gstRatio * 100)}% of total amount. " +
                        "Expected range: ${"%.0f".format(GST_RATIO_MIN * 100)}-${"%.0f".format(GST_RATIO_MAX * 100)}%",
                suggestion = "Verify tax rates (5%, 12%, 18%, or 28%)",
                field = "gstAmount"
            )
        } else null
    }

    private fun checkDuplicates(
        invoice: Invoice,
        historical: List<Invoice>
    ): Anomaly? {
        val duplicate = historical.find { it.invoiceNumber == invoice.invoiceNumber }

        return if (duplicate != null) {
            Anomaly(
                type = Anomaly.Type.DUPLICATE,
                severity = Anomaly.Severity.HIGH,
                score = 1.0,
                title = "Duplicate Invoice Number",
                message = "Invoice #${invoice.invoiceNumber} already exists",
                suggestion = "Use a unique invoice number",
                field = "invoiceNumber"
            )
        } else null
    }

    private fun checkTimingAnomaly(
        invoice: Invoice,
        historical: List<Invoice>
    ): Anomaly? {
        if (historical.isEmpty()) return null

        // Check if invoice date is in the future
        val now = java.time.Instant.now()
        if (invoice.invoiceDate.isAfter(now.plusSeconds(86400))) { // 1 day buffer
            return Anomaly(
                type = Anomaly.Type.TIMING_ANOMALY,
                severity = Anomaly.Severity.LOW,
                score = 0.5,
                title = "Future-Dated Invoice",
                message = "Invoice date is in the future",
                suggestion = "Verify the invoice date",
                field = "invoiceDate"
            )
        }

        // Check for rapid consecutive invoices (potential fraud)
        val recentInvoices = historical
            .filter { it.invoiceDate.isAfter(now.minusSeconds(300)) } // Last 5 minutes

        return if (recentInvoices.size > 10) {
            Anomaly(
                type = Anomaly.Type.TIMING_ANOMALY,
                severity = Anomaly.Severity.MEDIUM,
                score = 0.6,
                title = "Rapid Invoice Creation",
                message = "${recentInvoices.size} invoices created in last 5 minutes",
                suggestion = "Verify this is not automated/fraudulent activity",
                field = "timestamp"
            )
        } else null
    }

    private fun checkRoundNumbers(invoice: Invoice): Anomaly? {
        // Round number bias detection (common in fraudulent invoices)
        val isRound = invoice.totalAmount % 100 == 0.0 ||
                     invoice.totalAmount % 1000 == 0.0

        return if (isRound && invoice.totalAmount > 10000) {
            Anomaly(
                type = Anomaly.Type.PATTERN_ANOMALY,
                severity = Anomaly.Severity.LOW,
                score = 0.3,
                title = "Round Number Amount",
                message = "Amount is exactly ₹${invoice.totalAmount.toInt()}",
                suggestion = "Verify this is accurate (round numbers are unusual)",
                field = "totalAmount"
            )
        } else null
    }

    companion object {
        private const val Z_SCORE_THRESHOLD = 2.5
        private const val GST_RATIO_MIN = 0.12 // 12%
        private const val GST_RATIO_MAX = 0.22 // 22%
    }
}