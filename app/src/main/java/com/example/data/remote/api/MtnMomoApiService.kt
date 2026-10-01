package com.example.data.remote.api

import com.example.data.remote.model.MtnRequestToPayPayload
import com.example.data.remote.model.MtnTokenResponse
import com.example.data.remote.model.MtnTransactionStatus
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface MtnMomoApiService {

    @POST("collection/token/")
    suspend fun getAccessToken(
        @Header("Authorization") basicAuth: String,
        @Header("Ocp-Apim-Subscription-Key") subscriptionKey: String
    ): Response<MtnTokenResponse>

    @POST("collection/v1_0/requesttopay")
    suspend fun requestToPay(
        @Header("Authorization") bearerToken: String,
        @Header("X-Reference-Id") referenceId: String,
        @Header("X-Target-Environment") targetEnvironment: String,
        @Header("Ocp-Apim-Subscription-Key") subscriptionKey: String,
        @Body payload: MtnRequestToPayPayload
    ): Response<ResponseBody>

    @GET("collection/v1_0/requesttopay/{referenceId}")
    suspend fun getRequestToPayStatus(
        @Header("Authorization") bearerToken: String,
        @Header("X-Target-Environment") targetEnvironment: String,
        @Header("Ocp-Apim-Subscription-Key") subscriptionKey: String,
        @Path("referenceId") referenceId: String
    ): Response<MtnTransactionStatus>
}
