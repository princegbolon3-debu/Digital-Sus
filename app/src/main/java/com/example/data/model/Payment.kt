package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "payments",
    indices = [
        Index(value = ["transactionReference"], unique = true)
    ]
)
data class Payment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val contributionId: Long,
    val groupId: Long,
    val cycleId: Long,
    val memberUserId: Long,
    val amount: Double,
    val paymentMethod: String, // "Lonestar MTN Mobile Money", "Orange Money Liberia", "Cash Deposit", "Direct Bank Transfer"
    val transactionReference: String,
    val status: String = "SUBMITTED", // "SUBMITTED", "VERIFIED", "REJECTED"
    val note: String? = null,
    val rejectionReason: String? = null,
    val submittedAt: Long = System.currentTimeMillis(),
    val verifiedAt: Long? = null,
    val verifiedByUserId: Long? = null
)
