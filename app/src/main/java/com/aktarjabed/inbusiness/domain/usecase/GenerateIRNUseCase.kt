package com.aktarjabed.inbusiness.domain.usecase

import android.util.Log
import com.aktarjabed.inbusiness.data.cache.IRNCache
import com.aktarjabed.inbusiness.data.entities.Invoice
import com.aktarjabed.inbusiness.data.entities.InvoiceItem
import com.aktarjabed.inbusiness.data.remote.NicClient
import com.aktarjabed.inbusiness.data.remote.models.IRNData
import com.aktarjabed.inbusiness.data.repository.InvoiceRepository
import com.aktarjabed.inbusiness.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject

class GenerateIRNUseCase @Inject constructor(
    private val nicClient: NicClient,
    private val invoiceRepository: InvoiceRepository,
    private val irnCache: IRNCache,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend operator fun invoke(
        invoice: Invoice,
        items: List<InvoiceItem>
    ): Result<IRNData> = withContext(ioDispatcher) {
        Log.d(TAG, "Starting IRN generation for invoice: ${invoice.invoiceNumber}")

        // Validate invoice before sending to NIC
        validateInvoice(invoice, items).onFailure {
            return@withContext Result.failure(it)
        }

        // Generate IRN from NIC
        val irnResult = nicClient.generateIRN(invoice, items)

        irnResult.onSuccess { irnData ->
            // Update invoice with IRN data
            val updatedInvoice = invoice.copy(
                irn = irnData.irn,
                ackNo = irnData.ackNo,
                ackDate = parseNicDate(irnData.ackDt),
                qrCodeData = irnData.signedQRCode,
                eInvoiceStatus = "GENERATED",
                updatedAt = Instant.now()
            )

            // Save to database
            invoiceRepository.saveInvoice(updatedInvoice, items)

            // Cache IRN data
            irnCache.cacheIRN(irnData.irn, irnData)

            Log.i(TAG, "IRN generated and saved successfully: ${irnData.irn}")
        }

        irnResult
    }

    private fun validateInvoice(
        invoice: Invoice,
        items: List<InvoiceItem>
    ): Result<Unit> {
        return runCatching {
            require(invoice.supplierGstin.length == 15) {
                "Invalid supplier GSTIN: ${invoice.supplierGstin}"
            }

            require(invoice.invoiceNumber.isNotBlank()) {
                "Invoice number is required"
            }

            require(items.isNotEmpty()) {
                "At least one item is required"
            }

            require(invoice.totalAmount > 0) {
                "Invoice amount must be greater than zero"
            }

            items.forEach { item ->
                require(item.itemName.isNotBlank()) {
                    "Item name is required"
                }
                require(item.quantity > 0) {
                    "Item quantity must be greater than zero"
                }
                require(item.rate >= 0) {
                    "Item rate cannot be negative"
                }
            }
        }
    }

    private fun parseNicDate(dateStr: String): Instant {
        return try {
            // NIC format: "dd-MM-yyyy HH:mm:ss"
            val formatter = java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")
            java.time.LocalDateTime.parse(dateStr, formatter)
                .atZone(java.time.ZoneId.systemDefault())
                .toInstant()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse NIC date: $dateStr", e)
            Instant.now()
        }
    }

    companion object {
        private const val TAG = "GenerateIRNUseCase"
    }
}