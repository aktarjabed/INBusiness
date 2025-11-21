package com.aktarjabed.inbusiness.presentation.screens.invoice

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aktarjabed.inbusiness.data.dao.InvoiceDao
import com.aktarjabed.inbusiness.data.entities.Invoice
import com.aktarjabed.inbusiness.data.entities.InvoiceItem
import com.aktarjabed.inbusiness.domain.quota.QuotaGate
import com.aktarjabed.inbusiness.domain.quota.QuotaVerdict
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class InvoiceViewModel @Inject constructor(
    private val quotaGate: QuotaGate,
    private val invoiceDao: InvoiceDao
) : ViewModel() {

    private val _uiState = MutableStateFlow<InvoiceUiState>(InvoiceUiState.Initial)
    val uiState: StateFlow<InvoiceUiState> = _uiState.asStateFlow()

    private val currentUserId = "default_user" // TODO: Replace with actual auth

    fun checkQuotaAndPrepare() {
        viewModelScope.launch {
            _uiState.value = InvoiceUiState.Loading
<<<<<<< HEAD

            try {
                val verdict = quotaGate.assertQuota(currentUserId)

=======

            try {
                val verdict = quotaGate.assertQuota(currentUserId)

>>>>>>> ad4bb8454e6b04046f0ba290c1f04d22f25fbfa5
                when (verdict) {
                    is QuotaVerdict.Allowed -> {
                        val nextInvoiceNumber = generateInvoiceNumber()
                        _uiState.value = InvoiceUiState.CreateAllowed(
                            remainingToday = verdict.remaining,
                            invoiceNumber = nextInvoiceNumber
                        )
                        Log.d(TAG, "Quota check passed. Remaining: ${verdict.remaining}")
                    }
<<<<<<< HEAD

=======

>>>>>>> ad4bb8454e6b04046f0ba290c1f04d22f25fbfa5
                    is QuotaVerdict.DailyCap -> {
                        _uiState.value = InvoiceUiState.QuotaBlocked(verdict)
                        Log.w(TAG, "Daily quota exceeded. Limit: ${verdict.limit}")
                    }
<<<<<<< HEAD

=======

>>>>>>> ad4bb8454e6b04046f0ba290c1f04d22f25fbfa5
                    is QuotaVerdict.MonthlyCap -> {
                        _uiState.value = InvoiceUiState.QuotaBlocked(verdict)
                        Log.w(TAG, "Monthly quota exceeded")
                    }
<<<<<<< HEAD

=======

>>>>>>> ad4bb8454e6b04046f0ba290c1f04d22f25fbfa5
                    is QuotaVerdict.FreeExpired -> {
                        _uiState.value = InvoiceUiState.QuotaBlocked(verdict)
                        Log.w(TAG, "Free tier expired")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking quota", e)
                _uiState.value = InvoiceUiState.Error("Failed to check quota: ${e.message}")
            }
        }
    }

    fun createInvoice(
        customerName: String,
        totalAmount: Double,
        items: List<InvoiceItem> = emptyList()
    ) {
        viewModelScope.launch {
            _uiState.value = InvoiceUiState.Loading
<<<<<<< HEAD

=======

>>>>>>> ad4bb8454e6b04046f0ba290c1f04d22f25fbfa5
            try {
                // Double-check quota before creating
                val verdict = quotaGate.assertQuota(currentUserId)
                if (verdict !is QuotaVerdict.Allowed) {
                    _uiState.value = InvoiceUiState.QuotaBlocked(verdict)
                    return@launch
                }
<<<<<<< HEAD

=======

>>>>>>> ad4bb8454e6b04046f0ba290c1f04d22f25fbfa5
                // Create invoice
                val invoiceId = UUID.randomUUID().toString()
                val invoice = Invoice(
                    id = invoiceId,
                    businessId = "default_business", // TODO: Replace with actual business
<<<<<<< HEAD
                    invoiceNumber = (uiState.value as? InvoiceUiState.CreateAllowed)?.invoiceNumber
=======
                    invoiceNumber = (uiState.value as? InvoiceUiState.CreateAllowed)?.invoiceNumber
>>>>>>> ad4bb8454e6b04046f0ba290c1f04d22f25fbfa5
                        ?: generateInvoiceNumber(),
                    customerId = "",
                    customerName = customerName,
                    totalAmount = totalAmount,
                    taxAmount = totalAmount * 0.18, // 18% GST
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                )
<<<<<<< HEAD

                invoiceDao.insertInvoiceWithItems(invoice, items)

=======

                invoiceDao.insertInvoiceWithItems(invoice, items)

>>>>>>> ad4bb8454e6b04046f0ba290c1f04d22f25fbfa5
                _uiState.value = InvoiceUiState.Success(
                    invoiceId = invoiceId,
                    message = "Invoice created successfully"
                )
<<<<<<< HEAD

=======

>>>>>>> ad4bb8454e6b04046f0ba290c1f04d22f25fbfa5
                Log.i(TAG, "Invoice created: $invoiceId. Remaining quota: ${verdict.remaining}")
            } catch (e: Exception) {
                Log.e(TAG, "Error creating invoice", e)
                _uiState.value = InvoiceUiState.Error("Failed to create invoice: ${e.message}")
            }
        }
    }

    private suspend fun generateInvoiceNumber(): String {
        val lastInvoice = invoiceDao.getRecentInvoices(1).firstOrNull()
        val lastNumber = lastInvoice?.invoiceNumber?.substringAfter("INV-")?.toIntOrNull() ?: 0
        return "INV-${String.format("%05d", lastNumber + 1)}"
    }

    fun resetState() {
        _uiState.value = InvoiceUiState.Initial
    }

    companion object {
        private const val TAG = "InvoiceViewModel"
    }
}