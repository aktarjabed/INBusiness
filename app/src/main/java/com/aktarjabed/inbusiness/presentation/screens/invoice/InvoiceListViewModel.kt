package com.aktarjabed.inbusiness.presentation.screens.invoice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aktarjabed.inbusiness.data.entities.Invoice
import com.aktarjabed.inbusiness.data.entities.InvoiceStatus
import com.aktarjabed.inbusiness.data.repository.InvoiceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InvoiceListViewModel @Inject constructor(
    private val repo: InvoiceRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedStatus = MutableStateFlow<InvoiceStatus?>(null)
    private val _isSearchVisible = MutableStateFlow(false)

    private val _message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<InvoiceListUiState> = combine(
        repo.getAllInvoices(),
        _searchQuery,
        _selectedStatus,
        _isSearchVisible,
        _message
    ) { invoices, query, status, searchVisible, message ->
        val filtered = invoices
            .filter { inv ->
                query.isBlank() ||
                        inv.invoiceNumber.contains(query, ignoreCase = true) ||
                        inv.customerName.contains(query, ignoreCase = true)
            }
            .filter { inv ->
                status == null || inv.status == status
            }

        InvoiceListUiState(
            invoices = filtered,
            searchQuery = query,
            selectedStatus = status,
            isSearchVisible = searchVisible,
            message = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = InvoiceListUiState()
    )

    fun onSearch(query: String) { _searchQuery.value = query }
    fun onStatusFilter(status: InvoiceStatus) {
        if (_selectedStatus.value == status) {
            _selectedStatus.value = null
        } else {
            _selectedStatus.value = status
        }
    }
    fun toggleSearch() { _isSearchVisible.value = !_isSearchVisible.value }
    fun clearMessage() { _message.value = null }

    fun deleteInvoice(invoice: Invoice) {
        viewModelScope.launch {
            repo.deleteInvoice(invoice)
            _message.value = "Invoice ${invoice.invoiceNumber} deleted."
        }
    }
}

data class InvoiceListUiState(
    val invoices: List<Invoice> = emptyList(),
    val searchQuery: String = "",
    val selectedStatus: InvoiceStatus? = null,
    val isSearchVisible: Boolean = false,
    val message: String? = null
)