package com.aktarjabed.inbusiness.presentation.screens.invoice

import com.aktarjabed.inbusiness.domain.quota.QuotaVerdict

sealed class InvoiceUiState {
    object Initial : InvoiceUiState()
    object Loading : InvoiceUiState()
<<<<<<< HEAD

=======

>>>>>>> ad4bb8454e6b04046f0ba290c1f04d22f25fbfa5
    data class CreateAllowed(
        val remainingToday: Int,
        val invoiceNumber: String = ""
    ) : InvoiceUiState()
<<<<<<< HEAD

    data class QuotaBlocked(
        val verdict: QuotaVerdict
    ) : InvoiceUiState()

=======

    data class QuotaBlocked(
        val verdict: QuotaVerdict
    ) : InvoiceUiState()

>>>>>>> ad4bb8454e6b04046f0ba290c1f04d22f25fbfa5
    data class Success(
        val invoiceId: String,
        val message: String = "Invoice created successfully"
    ) : InvoiceUiState()
<<<<<<< HEAD

=======

>>>>>>> ad4bb8454e6b04046f0ba290c1f04d22f25fbfa5
    data class Error(
        val message: String
    ) : InvoiceUiState()
}