package com.example.data.remote.api

import com.example.data.remote.model.OrangeTokenResponse
import com.example.data.remote.model.OrangeWebPaymentPayload
import com.example.data.remote.model.OrangeWebPaymentResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Header
import retrofit2.http.POST

interface OrangeMoneyApiService {

    @FormUrlEncoded
    @POST("oauth/v3/token")
    suspend fun getOAuthToken(
        @Header("Authorization") basicAuth: String,
        @Field("grant_type") grantType: String = "client_credentials"
    ): Response<OrangeTokenResponse>

    @POST("orange-money-webpay/dev/v1/webpayment")
    suspend fun initiateWebPayment(
        @Header("Authorization") bearerToken: String,
        @Body payload: OrangeWebPaymentPayload
    ): Response<OrangeWebPaymentResponse>
}
