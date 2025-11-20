package com.aktarjabed.inbusiness.presentation.screens.invoice

import android.content.Context
import android.content.Intent
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aktarjabed.inbusiness.data.entities.Invoice
import com.aktarjabed.inbusiness.data.entities.InvoiceStatus
import com.aktarjabed.inbusiness.data.repository.InvoiceRepository
import com.aktarjabed.inbusiness.domain.services.PdfGenerator
import com.aktarjabed.inbusiness.utils.PdfDocumentAdapter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class PdfPreviewViewModel @Inject constructor(
    private val repo: InvoiceRepository,
    private val pdfGen: PdfGenerator
) : ViewModel() {

    private val _uiState = MutableStateFlow(PdfPreviewUiState())
    val uiState: StateFlow<PdfPreviewUiState> = _uiState.asStateFlow()

    fun load(invoiceId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            repo.getInvoiceById(invoiceId)?.let { inv ->
                _uiState.value = _uiState.value.copy(invoice = inv)
                val items = repo.getInvoiceItems(invoiceId)
                val pdf = pdfGen.generateInvoicePdf(inv, items)
                _uiState.value = _uiState.value.copy(pdfFile = pdf, isLoading = false)
            } ?: run {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Invoice not found"
                )
            }
        }
    }

    fun sharePdf(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Invoice ${uiState.value.invoice?.invoiceNumber}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Invoice"))
    }

    fun printPdf(context: Context, file: File) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val jobName = "Invoice ${uiState.value.invoice?.invoiceNumber}"
        printManager.print(jobName, PdfDocumentAdapter(file), null)
    }

    fun markAsPaid() {
        viewModelScope.launch {
            uiState.value.invoice?.let { inv ->
                val updated = inv.copy(status = InvoiceStatus.PAID, paidAmount = inv.totalAmount, balanceAmount = 0.0)
                repo.saveInvoice(updated, emptyList())
                _uiState.value = _uiState.value.copy(invoice = updated, message = "Marked as paid")
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}

data class PdfPreviewUiState(
    val isLoading: Boolean = true,
    val invoice: Invoice? = null,
    val pdfFile: File? = null,
    val message: String? = null
)