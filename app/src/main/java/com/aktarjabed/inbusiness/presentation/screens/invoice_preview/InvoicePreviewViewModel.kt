package com.aktarjabed.inbusiness.presentation.screens.invoice_preview

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aktarjabed.inbusiness.data.entities.Invoice
import com.aktarjabed.inbusiness.data.entities.InvoiceItem
import com.aktarjabed.inbusiness.data.entities.Payment
import com.aktarjabed.inbusiness.data.repository.InvoiceRepository
import com.aktarjabed.inbusiness.data.repository.PaymentRepository
import com.aktarjabed.inbusiness.domain.invoice.InvoiceCreationResult
import com.aktarjabed.inbusiness.domain.usecase.GetInvoiceForPreviewUseCase
import com.aktarjabed.inbusiness.domain.usecase.PreviewResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class InvoicePreviewUiState {
    object Loading : InvoicePreviewUiState()
    data class Success(val invoice: Invoice, val items: List<InvoiceItem>) : InvoicePreviewUiState()
    data class Error(val message: String) : InvoicePreviewUiState()
}

sealed class RecordPaymentUiState {
    object Idle : RecordPaymentUiState()
    object Saving : RecordPaymentUiState()
    object Saved : RecordPaymentUiState()
    data class Error(val message: String) : RecordPaymentUiState()
}

sealed class CancelInvoiceUiState {
    object Idle : CancelInvoiceUiState()
    object Cancelling : CancelInvoiceUiState()
    object Cancelled : CancelInvoiceUiState()
    data class Error(val message: String) : CancelInvoiceUiState()
}

@HiltViewModel
class InvoicePreviewViewModel @Inject constructor(
    private val getInvoiceForPreviewUseCase: GetInvoiceForPreviewUseCase,
    private val paymentRepository: PaymentRepository,
    private val invoiceRepository: InvoiceRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val invoiceId: String = checkNotNull(savedStateHandle["invoiceId"])

    private val _uiState = MutableStateFlow<InvoicePreviewUiState>(InvoicePreviewUiState.Loading)
    val uiState: StateFlow<InvoicePreviewUiState> = _uiState.asStateFlow()

    private val _recordPaymentState = MutableStateFlow<RecordPaymentUiState>(RecordPaymentUiState.Idle)
    val recordPaymentState: StateFlow<RecordPaymentUiState> = _recordPaymentState.asStateFlow()

    private val _cancelInvoiceState = MutableStateFlow<CancelInvoiceUiState>(CancelInvoiceUiState.Idle)
    val cancelInvoiceState: StateFlow<CancelInvoiceUiState> = _cancelInvoiceState.asStateFlow()

    /**
     * Payments are not keyed by an idempotency token, so a double tap must not be able to
     * post the same amount twice. The dialog disables its button, but the state update is
     * asynchronous, hence this guard.
     */
    private var paymentInFlight = false

    /** Same reasoning as [paymentInFlight]: cancellation must run exactly once. */
    private var cancelInFlight = false

    init {
        viewModelScope.launch { loadInvoice() }
    }

    private suspend fun loadInvoice() {
        try {
            val result = getInvoiceForPreviewUseCase(invoiceId)
            when (result) {
                is PreviewResult.Success -> {
                    _uiState.value = InvoicePreviewUiState.Success(result.invoice, result.items)
                }
                is PreviewResult.Error -> {
                    _uiState.value = InvoicePreviewUiState.Error(result.message)
                }
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("InvoicePreviewViewModel", "Failed to load invoice", e)
            _uiState.value = InvoicePreviewUiState.Error(e.message ?: "Unknown error occurred")
        }
    }

    fun recordPayment(amount: Double, paymentMode: String) {
        if (paymentInFlight) return

        val currentInvoice = (_uiState.value as? InvoicePreviewUiState.Success)?.invoice
        if (currentInvoice == null) {
            _recordPaymentState.value = RecordPaymentUiState.Error("Invoice is not ready")
            return
        }

        paymentInFlight = true
        viewModelScope.launch {
            _recordPaymentState.value = RecordPaymentUiState.Saving
            try {
                paymentRepository.addPayment(
                    Payment(
                        businessId = currentInvoice.businessId,
                        invoiceId = currentInvoice.id,
                        amount = amount,
                        paymentMode = paymentMode
                    )
                )
                loadInvoice()
                _recordPaymentState.value = RecordPaymentUiState.Saved
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e("InvoicePreviewViewModel", "Failed to record payment", e)
                _recordPaymentState.value = RecordPaymentUiState.Error(
                    e.message ?: "Could not record payment"
                )
            } finally {
                paymentInFlight = false
            }
        }
    }

    fun resetRecordPaymentState() {
        _recordPaymentState.value = RecordPaymentUiState.Idle
    }

    /**
     * Cancels the invoice through [InvoiceRepository.cancelInvoice], which reverses the
     * stock ledger exactly once and refuses to run while payments exist. The repository
     * owns all the invariants; this method only translates the result into UI state.
     */
    fun cancelInvoice() {
        if (cancelInFlight) return
        cancelInFlight = true

        viewModelScope.launch {
            _cancelInvoiceState.value = CancelInvoiceUiState.Cancelling
            try {
                when (val result = invoiceRepository.cancelInvoice(invoiceId)) {
                    is InvoiceCreationResult.Success -> {
                        loadInvoice()
                        _cancelInvoiceState.value = CancelInvoiceUiState.Cancelled
                    }
                    is InvoiceCreationResult.InvalidRequest -> {
                        _cancelInvoiceState.value = CancelInvoiceUiState.Error(result.message)
                    }
                    else -> {
                        _cancelInvoiceState.value = CancelInvoiceUiState.Error(
                            "This invoice could not be cancelled. Reload the invoice and try again."
                        )
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e("InvoicePreviewViewModel", "Failed to cancel invoice", e)
                _cancelInvoiceState.value = CancelInvoiceUiState.Error(
                    e.message ?: "Could not cancel the invoice"
                )
            } finally {
                cancelInFlight = false
            }
        }
    }

    fun resetCancelInvoiceState() {
        _cancelInvoiceState.value = CancelInvoiceUiState.Idle
    }
}
