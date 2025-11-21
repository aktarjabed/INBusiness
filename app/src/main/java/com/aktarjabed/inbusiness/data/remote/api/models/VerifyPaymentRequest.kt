package com.aktarjabed.inbusiness.data.remote.api.models

import com.google.gson.annotations.SerializedName

data class VerifyPaymentRequest(
    @SerializedName("orderId")
    val orderId: String,

    @SerializedName("paymentId")
    val paymentId: String,

    @SerializedName("signature")
    val signature: String,

    @SerializedName("userId")
    val userId: String
)