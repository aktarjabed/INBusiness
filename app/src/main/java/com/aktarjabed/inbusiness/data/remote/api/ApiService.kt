package com.aktarjabed.inbusiness.data.remote.api

import com.aktarjabed.inbusiness.data.remote.api.models.VerifyPaymentRequest
import com.aktarjabed.inbusiness.data.remote.api.models.VerifyPaymentResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    @POST("payment/verify")
    suspend fun verifyPayment(
        @Body request: VerifyPaymentRequest
    ): Response<VerifyPaymentResponse>
}