package com.example.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MtnPayer(
    @Json(name = "partyIdType") val partyIdType: String = "MSISDN",
    @Json(name = "partyId") val partyId: String
)

@JsonClass(generateAdapter = true)
data class MtnRequestToPayPayload(
    @Json(name = "amount") val amount: String,
    @Json(name = "currency") val currency: String = "LRD",
    @Json(name = "externalId") val externalId: String,
    @Json(name = "payer") val payer: MtnPayer,
    @Json(name = "payerMessage") val payerMessage: String = "Digital Susu Contribution",
    @Json(name = "payeeNote") val payeeNote: String = "Liberia Susu Pool"
)

@JsonClass(generateAdapter = true)
data class MtnTokenResponse(
    @Json(name = "access_token") val accessToken: String?,
    @Json(name = "token_type") val tokenType: String?,
    @Json(name = "expires_in") val expiresIn: Int?
)

@JsonClass(generateAdapter = true)
data class MtnTransactionStatus(
    @Json(name = "financialTransactionId") val financialTransactionId: String? = null,
    @Json(name = "externalId") val externalId: String? = null,
    @Json(name = "amount") val amount: String? = null,
    @Json(name = "currency") val currency: String? = null,
    @Json(name = "status") val status: String? = null, // "SUCCESSFUL", "PENDING", "FAILED"
    @Json(name = "reason") val reason: String? = null
)

@JsonClass(generateAdapter = true)
data class OrangeTokenResponse(
    @Json(name = "access_token") val accessToken: String?,
    @Json(name = "token_type") val tokenType: String?,
    @Json(name = "expires_in") val expiresIn: String?
)

@JsonClass(generateAdapter = true)
data class OrangeWebPaymentPayload(
    @Json(name = "merchant_key") val merchantKey: String,
    @Json(name = "currency") val currency: String = "LRD",
    @Json(name = "order_id") val orderId: String,
    @Json(name = "amount") val amount: Double,
    @Json(name = "return_url") val returnUrl: String = "https://digitalsusu.liberia.lr/success",
    @Json(name = "cancel_url") val cancelUrl: String = "https://digitalsusu.liberia.lr/cancel",
    @Json(name = "notif_url") val notifUrl: String = "https://digitalsusu.liberia.lr/notify",
    @Json(name = "lang") val lang: String = "en",
    @Json(name = "reference") val reference: String
)

@JsonClass(generateAdapter = true)
data class OrangeWebPaymentResponse(
    @Json(name = "status") val status: Int? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "payment_url") val paymentUrl: String? = null,
    @Json(name = "pay_token") val payToken: String? = null,
    @Json(name = "notif_token") val notifToken: String? = null
)

data class MobilePaymentResult(
    val isSuccess: Boolean,
    val transactionReference: String,
    val provider: String, // "MTN_MOMO" or "ORANGE_MONEY"
    val status: String, // "COMPLETED", "PENDING", "FAILED"
    val message: String,
    val externalId: String? = null
)
