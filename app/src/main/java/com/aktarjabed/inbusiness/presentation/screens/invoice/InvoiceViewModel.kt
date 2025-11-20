package com.aktarjabed.inbusiness.presentation.screens.invoice

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aktarjabed.inbusiness.BuildConfig
import com.aktarjabed.inbusiness.data.entities.Invoice
import com.aktarjabed.inbusiness.data.entities.InvoiceItem
import com.aktarjabed.inbusiness.data.repository.InvoiceRepository
import com.aktarjabed.inbusiness.domain.ai.AnomalyDetector
import com.aktarjabed.inbusiness.domain.models.Anomaly
import com.aktarjabed.inbusiness.domain.usecase.GenerateIRNUseCase
import com.aktarjabed.inbusiness.util.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class InvoiceViewModel @Inject constructor(
    private val invoiceRepository: InvoiceRepository,
    private val generateIRNUseCase: GenerateIRNUseCase,
    private val anomalyDetector: AnomalyDetector,
    private val networkMonitor: NetworkMonitor,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val invoiceId: String? = savedStateHandle["invoiceId"]

    private val _uiState = MutableStateFlow(InvoiceUiState())
    val uiState: StateFlow<InvoiceUiState> = _uiState.asStateFlow()

    // Network connectivity state
    val isConnected: StateFlow<Boolean> = networkMonitor.isConnected
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    init {
        invoiceId?.let { loadInvoice(it) }
        detectAnomalies()
    }

    /**
     * Load existing invoice for editing
     */
    private fun loadInvoice(id: String) {
        viewModelScope.launch {
            try {
                val invoice = invoiceRepository.getInvoiceById(id)
                val items = invoiceRepository.getInvoiceItems(id)

                if (invoice != null) {
                    _uiState.update { it.copy(
                        invoice = invoice,
                        items = items,
                        isLoading = false
                    ) }
                } else {
                    _uiState.update { it.copy(
                        error = "Invoice not found",
                        isLoading = false
                    ) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load invoice", e)
                _uiState.update { it.copy(
                    error = "Failed to load invoice: ${e.message}",
                    isLoading = false
                ) }
            }
        }
    }

    /**
     * Save invoice (create or update)
     */
    fun saveInvoice(invoice: Invoice, items: List<InvoiceItem>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }

            try {
                val savedInvoice = if (invoice.id.isEmpty()) {
                    // Create new invoice
                    val newInvoice = invoice.copy(
                        id = java.util.UUID.randomUUID().toString(),
                        createdAt = Instant.now(),
                        updatedAt = Instant.now()
                    )
                    invoiceRepository.createInvoice(newInvoice, items)
                    newInvoice
                } else {
                    // Update existing invoice
                    val updatedInvoice = invoice.copy(updatedAt = Instant.now())
                    invoiceRepository.updateInvoice(updatedInvoice, items)
                    updatedInvoice
                }

                _uiState.update { it.copy(
                    invoice = savedInvoice,
                    items = items,
                    isSaving = false,
                    saveSuccess = true
                ) }

                Log.i(TAG, "Invoice saved successfully: ${savedInvoice.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save invoice", e)
                _uiState.update { it.copy(
                    isSaving = false,
                    error = "Failed to save invoice: ${e.message}"
                ) }
            }
        }
    }

    /**
     * PHASE 2: Generate IRN using NIC API
     */
    fun generateIRN() {
        val invoice = _uiState.value.invoice
        val items = _uiState.value.items

        if (invoice.id.isEmpty()) {
            _uiState.update { it.copy(error = "Save invoice before generating IRN") }
            return
        }

        if (!BuildConfig.ENABLE_NIC) {
            _uiState.update { it.copy(error = "NIC e-invoicing is disabled") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(
                isGeneratingIRN = true,
                error = null
            ) }

            try {
                val result = generateIRNUseCase(invoice, items)

                result.onSuccess { irnData ->
                    Log.i(TAG, "IRN generated successfully: ${irnData.irn}")

                    // Update invoice with IRN data
                    val updatedInvoice = invoice.copy(
                        irn = irnData.irn,
                        ackNo = irnData.ackNo,
                        ackDate = irnData.ackDate,
                        qrCodeData = irnData.signedQRCode,
                        updatedAt = Instant.now()
                    )

                    invoiceRepository.updateInvoice(updatedInvoice, items)

                    _uiState.update { it.copy(
                        invoice = updatedInvoice,
                        isGeneratingIRN = false,
                        irnGenerated = true,
                        message = "IRN generated successfully"
                    ) }
                }.onFailure { error ->
                    Log.e(TAG, "IRN generation failed", error)
                    _uiState.update { it.copy(
                        isGeneratingIRN = false,
                        error = "IRN generation failed: ${error.message}"
                    ) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during IRN generation", e)
                _uiState.update { it.copy(
                    isGeneratingIRN = false,
                    error = "Unexpected error: ${e.message}"
                ) }
            }
        }
    }

    /**
     * PHASE 2: AI Anomaly Detection
     */
    private fun detectAnomalies() {
        viewModelScope.launch {
            try {
                val allInvoices = invoiceRepository.getAllInvoicesOnce()

                if (allInvoices.size < 10) {
                    // Need at least 10 invoices for statistical analysis
                    return@launch
                }

                val invoice = _uiState.value.invoice
                if (invoice.id.isNotEmpty()) {
                    val anomalies = anomalyDetector.detectAnomalies(invoice, allInvoices)

                    if (anomalies.isNotEmpty()) {
                        Log.w(TAG, "Anomalies detected: ${anomalies.size}")
                        _uiState.update { it.copy(anomalies = anomalies) }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Anomaly detection failed", e)
                // Don't block invoice creation if anomaly detection fails
            }
        }
    }

    /**
     * Clear messages after display
     */
    fun clearMessages() {
        _uiState.update { it.copy(
            message = null,
            error = null,
            saveSuccess = false,
            irnGenerated = false
        ) }
    }

    /**
     * Dismiss anomaly warning
     */
    fun dismissAnomaly(anomaly: Anomaly) {
        val currentAnomalies = _uiState.value.anomalies.toMutableList()
        currentAnomalies.remove(anomaly)
        _uiState.update { it.copy(anomalies = currentAnomalies) }
    }

    companion object {
        private const val TAG = "InvoiceViewModel"
    }
}

/**
 * UI State for Invoice screen
 */
data class InvoiceUiState(
    val invoice: Invoice = Invoice(),
    val items: List<InvoiceItem> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isGeneratingIRN: Boolean = false,
    val saveSuccess: Boolean = false,
    val irnGenerated: Boolean = false,
    val anomalies: List<Anomaly> = emptyList(),
    val message: String? = null,
    val error: String? = null
)