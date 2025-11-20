package com.aktarjabed.inbusiness.data.remote.models

import com.google.gson.annotations.SerializedName

// Authentication
data class AuthRequest(
    @SerializedName("Data") val data: String
)

data class AuthResponse(
    @SerializedName("Status") val status: String,
    @SerializedName("Data") val data: AuthData?,
    @SerializedName("ErrorDetails") val errorDetails: List<ErrorDetail>?
)

data class AuthData(
    @SerializedName("AuthToken") val authToken: String,
    @SerializedName("Sek") val sek: String,
    @SerializedName("ClientId") val clientId: String,
    @SerializedName("TokenExpiry") val tokenExpiry: String
)

// IRN Generation
data class IRNGenerateRequest(
    @SerializedName("Data") val data: String
)

data class IRNResponse(
    @SerializedName("Status") val status: String,
    @SerializedName("Data") val data: IRNData?,
    @SerializedName("ErrorDetails") val errorDetails: List<ErrorDetail>?
)

data class IRNData(
    @SerializedName("Irn") val irn: String,
    @SerializedName("AckNo") val ackNo: String,
    @SerializedName("AckDt") val ackDt: String,
    @SerializedName("SignedInvoice") val signedInvoice: String,
    @SerializedName("SignedQRCode") val signedQRCode: String,
    @SerializedName("EwbNo") val ewbNo: String?,
    @SerializedName("EwbDt") val ewbDt: String?,
    @SerializedName("EwbValidTill") val ewbValidTill: String?
)

// IRN Cancellation
data class IRNCancelRequest(
    @SerializedName("Data") val data: String
)

data class IRNCancelResponse(
    @SerializedName("Status") val status: String,
    @SerializedName("Data") val data: IRNCancelData?,
    @SerializedName("ErrorDetails") val errorDetails: List<ErrorDetail>?
)

data class IRNCancelData(
    @SerializedName("Irn") val irn: String,
    @SerializedName("CancelDate") val cancelDate: String
)

// Common
data class ErrorDetail(
    @SerializedName("ErrorCode") val errorCode: String,
    @SerializedName("ErrorMessage") val errorMessage: String
)