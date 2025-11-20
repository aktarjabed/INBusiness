package com.aktarjabed.inbusiness.data.remote

import android.util.Base64
import android.util.Log
import com.aktarjabed.inbusiness.data.entities.Invoice
import com.aktarjabed.inbusiness.data.entities.InvoiceItem
import com.aktarjabed.inbusiness.data.remote.models.*
import com.aktarjabed.inbusiness.di.IoDispatcher
import com.aktarjabed.inbusiness.util.CircuitBreaker
import com.aktarjabed.inbusiness.util.retryWithBackoff
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

@Singleton
class NicClient @Inject constructor(
    private val nicApi: NicApi,
    private val authManager: NicAuthManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private val circuitBreaker = CircuitBreaker(
        failureThreshold = 3,
        resetTimeout = 60.seconds,
        halfOpenMaxAttempts = 2
    )

    suspend fun generateIRN(
        invoice: Invoice,
        items: List<InvoiceItem>
    ): Result<IRNData> = withContext(ioDispatcher) {
        circuitBreaker.execute {
            retryWithBackoff(
                maxAttempts = 3,
                initialDelay = 1.seconds,
                maxDelay = 10.seconds
            ) {
                Log.d(TAG, "Generating IRN for invoice: ${invoice.invoiceNumber}")

                // Step 1: Get authentication data
                val authData = authManager.getAuthData().getOrThrow()
                val sek = authManager.getSEK()
                    ?: throw NicException("SEK not available")

                // Step 2: Build NIC invoice JSON
                val invoiceJson = buildNicInvoiceJson(invoice, items)
                Log.d(TAG, "Invoice JSON built: ${invoiceJson.length} chars")

                // Step 3: Encrypt invoice data with SEK
                val encryptedData = encryptWithSEK(invoiceJson, sek)
                val encryptedDataBase64 = Base64.encodeToString(encryptedData, Base64.NO_WRAP)

                // Step 4: Call IRN generation API
                val request = IRNGenerateRequest(data = encryptedDataBase64)
                val response = nicApi.generateIRN(
                    token = "Bearer ${authData.authToken}",
                    request = request
                )

                // Step 5: Validate response
                if (response.status != "1") {
                    val errorMsg = response.errorDetails?.firstOrNull()?.errorMessage
                        ?: "IRN generation failed"
                    throw NicException(errorMsg)
                }

                val irnData = response.data
                    ?: throw NicException("No IRN data received")

                Log.i(TAG, "IRN generated successfully: ${irnData.irn}")
                irnData
            }
        }
    }

    suspend fun getInvoiceByIRN(irn: String): Result<IRNData> = withContext(ioDispatcher) {
        runCatching {
            val authData = authManager.getAuthData().getOrThrow()

            val response = nicApi.getInvoiceByIRN(
                token = "Bearer ${authData.authToken}",
                irn = irn
            )

            if (response.status != "1") {
                val errorMsg = response.errorDetails?.firstOrNull()?.errorMessage
                    ?: "Failed to fetch invoice"
                throw NicException(errorMsg)
            }

            response.data ?: throw NicException("No data received")
        }
    }

    suspend fun cancelIRN(
        irn: String,
        cancelReason: String,
        cancelRemarks: String
    ): Result<IRNCancelData> = withContext(ioDispatcher) {
        runCatching {
            val authData = authManager.getAuthData().getOrThrow()
            val sek = authManager.getSEK()
                ?: throw NicException("SEK not available")

            // Build cancellation JSON
            val cancelJson = JSONObject().apply {
                put("Irn", irn)
                put("CnlRsn", cancelReason)
                put("CnlRem", cancelRemarks)
            }.toString()

            // Encrypt with SEK
            val encryptedData = encryptWithSEK(cancelJson, sek)
            val encryptedDataBase64 = Base64.encodeToString(encryptedData, Base64.NO_WRAP)

            val request = IRNCancelRequest(data = encryptedDataBase64)
            val response = nicApi.cancelIRN(
                token = "Bearer ${authData.authToken}",
                request = request
            )

            if (response.status != "1") {
                val errorMsg = response.errorDetails?.firstOrNull()?.errorMessage
                    ?: "Cancellation failed"
                throw NicException(errorMsg)
            }

            response.data ?: throw NicException("No cancellation data received")
        }
    }

    private fun buildNicInvoiceJson(invoice: Invoice, items: List<InvoiceItem>): String {
        return JSONObject().apply {
            put("Version", "1.1")

            // Transaction Details
            put("TranDtls", JSONObject().apply {
                put("TaxSch", "GST")
                put("SupTyp", if (invoice.customerGstin.isBlank()) "B2C" else "B2B")
                put("RegRev", "N")
                put("IgstOnIntra", "N")
            })

            // Document Details
            put("DocDtls", JSONObject().apply {
                put("Typ", "INV")
                put("No", invoice.invoiceNumber)
                put("Dt", invoice.invoiceDate.toNicDate())
            })

            // Seller Details
            put("SellerDtls", JSONObject().apply {
                put("Gstin", invoice.supplierGstin)
                put("LglNm", invoice.supplierName)
                put("TrdNm", invoice.supplierName)
                put("Addr1", invoice.supplierAddress.take(100))
                put("Loc", extractCity(invoice.supplierAddress))
                put("Pin", extractPincode(invoice.supplierAddress))
                put("Stcd", invoice.supplierGstin.take(2))
            })

            // Buyer Details
            put("BuyerDtls", JSONObject().apply {
                if (invoice.customerGstin.isNotBlank()) {
                    put("Gstin", invoice.customerGstin)
                }
                put("LglNm", invoice.customerName)
                put("Pos", if (invoice.customerGstin.isNotBlank())
                    invoice.customerGstin.take(2) else "99")
                put("Addr1", invoice.customerAddress.take(100))
                put("Loc", extractCity(invoice.customerAddress))
                put("Pin", extractPincode(invoice.customerAddress))
                put("Stcd", if (invoice.customerGstin.isNotBlank())
                    invoice.customerGstin.take(2) else "99")
            })

            // Item List
            put("ItemList", JSONArray().apply {
                items.forEachIndexed { index, item ->
                    put(JSONObject().apply {
                        put("SlNo", (index + 1).toString())
                        put("PrdDesc", item.itemName)
                        put("IsServc", "N")
                        put("HsnCd", item.hsnCode.ifBlank { "0" })
                        put("Qty", item.quantity)
                        put("Unit", "NOS")
                        put("UnitPrice", item.rate)
                        put("TotAmt", item.totalAmount)
                        put("Discount", 0.0)
                        put("AssAmt", item.totalAmount)
                        put("GstRt", 18.0)
                        put("IgstAmt", if (invoice.igstAmount > 0) item.totalAmount * 0.18 else 0.0)
                        put("CgstAmt", if (invoice.cgstAmount > 0) item.totalAmount * 0.09 else 0.0)
                        put("SgstAmt", if (invoice.sgstAmount > 0) item.totalAmount * 0.09 else 0.0)
                        put("CesRt", 0.0)
                        put("CesAmt", 0.0)
                        put("CesNonAdvlAmt", 0.0)
                        put("StateCesRt", 0.0)
                        put("StateCesAmt", 0.0)
                        put("StateCesNonAdvlAmt", 0.0)
                        put("OthChrg", 0.0)
                        put("TotItemVal", item.totalAmount)
                    })
                }
            })

            // Value Details
            put("ValDtls", JSONObject().apply {
                put("AssVal", invoice.subtotal)
                put("CgstVal", invoice.cgstAmount)
                put("SgstVal", invoice.sgstAmount)
                put("IgstVal", invoice.igstAmount)
                put("CesVal", 0.0)
                put("StCesVal", 0.0)
                put("Discount", 0.0)
                put("OthChrg", 0.0)
                put("RndOffAmt", invoice.roundOff)
                put("TotInvVal", invoice.totalAmount)
            })
        }.toString()
    }

    private fun encryptWithSEK(data: String, sek: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/ECB/PKCS7Padding", "BC")
        val secretKey = SecretKeySpec(sek, "AES")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        return cipher.doFinal(data.toByteArray(Charsets.UTF_8))
    }

    private fun Instant.toNicDate(): String {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            .withZone(ZoneId.systemDefault())
        return formatter.format(this)
    }

    private fun extractCity(address: String): String {
        return address.split(",").getOrNull(1)?.trim()?.take(50) ?: "City"
    }

    private fun extractPincode(address: String): String {
        val pincodeRegex = """\b\d{6}\b""".toRegex()
        return pincodeRegex.find(address)?.value ?: "000000"
    }

    companion object {
        private const val TAG = "NicClient"
    }
}