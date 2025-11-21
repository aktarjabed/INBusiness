package com.aktarjabed.inbusiness.presentation.screens.invoice

import com.aktarjabed.inbusiness.domain.quota.QuotaVerdict

sealed class InvoiceUiState {
    object Initial : InvoiceUiState()
    object Loading : InvoiceUiState()

    data class CreateAllowed(
        val remainingToday: Int,
        val invoiceNumber: String = ""
    ) : InvoiceUiState()

    data class QuotaBlocked(
        val verdict: QuotaVerdict
    ) : InvoiceUiState()

    data class Success(
        val invoiceId: String,
        val message: String = "Invoice created successfully"
    ) : InvoiceUiState()

    data class Error(
        val message: String
    ) : InvoiceUiState()
}