package com.aktarjabed.inbusiness.data.remote.api.models

sealed class ApiResponse<out T> {
    data class Success<out T>(val data: T) : ApiResponse<T>()
    data class Error(val message: String, val code: Int? = null) : ApiResponse<Nothing>()
    object Loading : ApiResponse<Nothing>()
}

data class TokenResponse(
    val token: String,
    val expiresAt: Long
)

data class IRNResponse(
    val irn: String,
    val qrcode: String,
    val status: String,
    val message: String
)

data class InvoiceRequest(
    val invoiceId: String,
    val invoiceNumber: String,
    val clientGstin: String?,
    val totalAmount: Double,
    val invoiceDate: String
)