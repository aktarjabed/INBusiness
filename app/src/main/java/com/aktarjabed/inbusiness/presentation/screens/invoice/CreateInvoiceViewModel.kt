package com.aktarjabed.inbusiness.presentation.screens.invoice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aktarjabed.inbusiness.data.entities.Invoice
import com.aktarjabed.inbusiness.data.entities.InvoiceItem
import com.aktarjabed.inbusiness.data.repository.InvoiceRepository
import com.aktarjabed.inbusiness.domain.services.PdfGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CreateInvoiceViewModel @Inject constructor(
    private val repo: InvoiceRepository,
    private val pdfGen: PdfGenerator
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateInvoiceUiState())
    val uiState: StateFlow<CreateInvoiceUiState> = _uiState.asStateFlow()

    fun loadInvoice(id: String) {
        viewModelScope.launch {
            repo.getInvoiceById(id)?.let { inv ->
                _uiState.value = _uiState.value.copy(
                    invoiceId = inv.id,
                    invoiceNumber = inv.invoiceNumber,
                    customerName = inv.customerName,
                    customerGstin = inv.customerGstin,
                    customerAddress = inv.customerAddress,
                    items = repo.getInvoiceItems(inv.id),
                    notes = inv.notes,
                    termsAndConditions = inv.termsAndConditions,
                    invoiceDate = inv.invoiceDate,
                    dueDate = inv.dueDate
                )
                recalculateTotals()
            }
        }
    }

    // Customer
    fun onCustomerNameChange(name: String) {
        _uiState.value = _uiState.value.copy(customerName = name)
        validate()
    }
    fun onCustomerGstinChange(gstin: String) {
        _uiState.value = _uiState.value.copy(customerGstin = gstin)
        recalculateTotals()
    }
    fun onCustomerAddressChange(address: String) {
        _uiState.value = _uiState.value.copy(customerAddress = address)
    }

    // Supplier
    fun onSupplierGstinChange(gstin: String) {
        _uiState.value = _uiState.value.copy(supplierGstin = gstin)
        recalculateTotals()
    }

    // Items
    fun addNewItem() {
        val newList = _uiState.value.items.toMutableList()
        newList.add(InvoiceItem(id = UUID.randomUUID().toString(), invoiceId = null, itemName = "", hsnCode = "", quantity = 1.0, rate = 0.0, totalAmount = 0.0))
        _uiState.value = _uiState.value.copy(items = newList)
    }
    fun updateItem(index: Int, item: InvoiceItem) {
        val newList = _uiState.value.items.toMutableList()
        newList[index] = item
        _uiState.value = _uiState.value.copy(items = newList)
        recalculateTotals()
    }
    fun removeItem(index: Int) {
        val newList = _uiState.value.items.toMutableList()
        newList.removeAt(index)
        _uiState.value = _uiState.value.copy(items = newList)
        recalculateTotals()
    }

    // Notes
    fun onNotesChange(notes: String) {
        _uiState.value = _uiState.value.copy(notes = notes)
    }
    fun onTermsChange(terms: String) {
        _uiState.value = _uiState.value.copy(termsAndConditions = terms)
    }

    // Tax Logic
    private fun recalculateTotals() {
        val items = _uiState.value.items
        val subtotal = items.sumOf { it.totalAmount }

        val supplierStateCode = _uiState.value.supplierGstin.take(2)
        val customerStateCode = _uiState.value.customerGstin.take(2)

        val isIntraState = supplierStateCode.isNotBlank() && customerStateCode.isNotBlank() && supplierStateCode == customerStateCode

        val (cgst, sgst, igst) = if (isIntraState) {
            val tax = subtotal * 0.09
            Triple(tax, tax, 0.0)
        } else {
            val tax = subtotal * 0.18
            Triple(0.0, 0.0, tax)
        }

        val total = subtotal + cgst + sgst + igst + _uiState.value.roundOff

        _uiState.value = _uiState.value.copy(
            subtotal = subtotal,
            cgstAmount = cgst,
            sgstAmount = sgst,
            igstAmount = igst,
            totalAmount = total
        )
        validate()
    }

    private fun validate() {
        val valid = _uiState.value.customerName.isNotBlank() &&
                   _uiState.value.items.isNotEmpty() &&
                   _uiState.value.totalAmount > 0
        _uiState.value = _uiState.value.copy(isValid = valid)
    }

    fun saveInvoice() {
        viewModelScope.launch {
            if (!_uiState.value.isValid) {
                _uiState.value = _uiState.value.copy(message = "Please fill all required fields.")
                return@launch
            }
            val state = _uiState.value
            val invoice = Invoice(
                id = state.invoiceId.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString(),
                invoiceNumber = state.invoiceNumber.takeIf { it.isNotBlank() } ?: "INV/${System.currentTimeMillis()}",
                customerName = state.customerName,
                customerGstin = state.customerGstin,
                customerAddress = state.customerAddress,
                subtotal = state.subtotal,
                cgstAmount = state.cgstAmount,
                sgstAmount = state.sgstAmount,
                igstAmount = state.igstAmount,
                totalAmount = state.totalAmount,
                balanceAmount = state.totalAmount,
                notes = state.notes,
                termsAndConditions = state.termsAndConditions,
                createdBy = "" // TODO: Replace with actual user ID from authentication service
            )

            repo.saveInvoice(invoice, state.items)
            generatePdf(invoice, state.items)
        }
    }

    private suspend fun generatePdf(invoice: Invoice, items: List<InvoiceItem>) {
        runCatching {
            pdfGen.generateInvoicePdf(invoice, items)
        }.onSuccess { file ->
            _uiState.value = _uiState.value.copy(
                message = "Invoice saved. PDF: ${file.name}",
                pdfFile = file
            )
        }.onFailure {
            _uiState.value = _uiState.value.copy(message = "PDF error: ${it.message}")
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}

data class CreateInvoiceUiState(
    val invoiceId: String = "",
    val invoiceNumber: String = "",
    val supplierGstin: String = "",
    val customerName: String = "",
    val customerGstin: String = "",
    val customerAddress: String = "",
    val items: List<InvoiceItem> = emptyList(),
    val notes: String = "",
    val termsAndConditions: String = "Payment due within 30 days.",
    val invoiceDate: Instant = Instant.now(),
    val dueDate: Instant = Instant.now().plusSeconds(30 * 24 * 60 * 60),
    val subtotal: Double = 0.0,
    val cgstAmount: Double = 0.0,
    val sgstAmount: Double = 0.0,
    val igstAmount: Double = 0.0,
    val roundOff: Double = 0.0,
    val totalAmount: Double = 0.0,
    val isValid: Boolean = false,
    val message: String? = null,
    val pdfFile: File? = null
)