package com.aktarjabed.inbusiness.data.remote

import com.aktarjabed.inbusiness.data.remote.models.*
import retrofit2.http.*

interface NicApi {

    @POST("eivital/v1.04/auth")
    suspend fun authenticate(@Body request: AuthRequest): AuthResponse

    @POST("eivital/v1.04/Invoice")
    suspend fun generateIRN(
        @Header("Authorization") token: String,
        @Body request: IRNGenerateRequest
    ): IRNResponse

    @GET("eivital/v1.04/Invoice/irn/{irn}")
    suspend fun getInvoiceByIRN(
        @Header("Authorization") token: String,
        @Path("irn") irn: String
    ): IRNResponse

    @POST("eivital/v1.04/Invoice/Cancel")
    suspend fun cancelIRN(
        @Header("Authorization") token: String,
        @Body request: IRNCancelRequest
    ): IRNCancelResponse
}