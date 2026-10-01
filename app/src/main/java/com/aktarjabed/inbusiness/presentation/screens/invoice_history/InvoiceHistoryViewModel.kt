package com.aktarjabed.inbusiness.presentation.screens.invoice_history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aktarjabed.inbusiness.data.entities.Invoice
import com.aktarjabed.inbusiness.data.repository.InvoiceHistoryRepository
import com.aktarjabed.inbusiness.utils.AppDateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class InvoiceHistoryUiState(
    val invoices: List<Invoice> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val status: String? = "COMPLETED",
    val paymentStatus: String? = null,
    val documentType: String? = null,
    val isEndOfList: Boolean = false
)

@HiltViewModel
class InvoiceHistoryViewModel @Inject constructor(
    private val repository: InvoiceHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InvoiceHistoryUiState())
    val uiState: StateFlow<InvoiceHistoryUiState> = _uiState.asStateFlow()

    private var currentOffset = 0
    private val limit = 50
    private var activeLoad: Job? = null
    private var pendingSearch: Job? = null

    init {
        loadInvoices(reset = true)
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        activeLoad?.cancel()
        _uiState.update { it.copy(isLoading = false) }
        pendingSearch?.cancel()
        pendingSearch = viewModelScope.launch {
            delay(300)
            loadInvoices(reset = true)
        }
    }

    fun onDateRangeSelected(start: LocalDate?, end: LocalDate?) {
        _uiState.update { it.copy(startDate = start, endDate = end) }
        reloadForFilterChange()
    }

    fun onStatusChanged(status: String?) {
        _uiState.update { it.copy(status = status) }
        reloadForFilterChange()
    }

    fun onPaymentStatusChanged(paymentStatus: String?) {
        _uiState.update { it.copy(paymentStatus = paymentStatus) }
        reloadForFilterChange()
    }

    fun onDocumentTypeChanged(documentType: String?) {
        _uiState.update { it.copy(documentType = documentType) }
        reloadForFilterChange()
    }

    private fun reloadForFilterChange() {
        pendingSearch?.cancel()
        pendingSearch = null
        loadInvoices(reset = true)
    }

    fun loadMore() {
        if (pendingSearch?.isActive == true) return
        if (!_uiState.value.isLoading && !_uiState.value.isEndOfList) {
            loadInvoices(reset = false)
        }
    }

    fun loadInvoices(reset: Boolean = false) {
        if (reset) {
            activeLoad?.cancel()
            currentOffset = 0
            _uiState.update { it.copy(invoices = emptyList(), isEndOfList = false) }
        } else if (activeLoad?.isActive == true) {
            return
        }

        val requestOffset = currentOffset
        val requestState = _uiState.value
        activeLoad = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val startMillis = requestState.startDate?.let { AppDateUtils.getStartOfDay(it) }
                // The UI end date is inclusive, so query until the next business-local midnight.
                val endMillis = requestState.endDate?.let { AppDateUtils.getStartOfNextDay(it) }
                val newInvoices = repository.getInvoices(
                    search = requestState.searchQuery,
                    startDate = startMillis,
                    endDate = endMillis,
                    status = requestState.status,
                    paymentStatus = requestState.paymentStatus,
                    documentType = requestState.documentType,
                    limit = limit,
                    offset = requestOffset
                )

                currentCoroutineContext().ensureActive()
                _uiState.update {
                    it.copy(
                        invoices = if (reset) newInvoices else it.invoices + newInvoices,
                        isEndOfList = newInvoices.size < limit,
                        isLoading = false
                    )
                }
                currentOffset = requestOffset + newInvoices.size
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }
}
