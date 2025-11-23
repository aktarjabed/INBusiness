package com.aktarjabed.inbusiness.data.remote.api.models

import com.google.gson.annotations.SerializedName

data class VerifyPaymentResponse(
    @SerializedName("verified")
    val verified: Boolean,

    @SerializedName("message")
    val message: String,

    @SerializedName("paymentId")
    val paymentId: String?,

    @SerializedName("plan")
    val plan: String?
)