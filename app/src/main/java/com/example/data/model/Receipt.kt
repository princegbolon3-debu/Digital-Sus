package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "receipts",
    indices = [
        Index(value = ["receiptNumber"], unique = true)
    ]
)
data class Receipt(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val receiptNumber: String, // e.g. SUSU-2026-000145
    val paymentId: Long,
    val groupId: Long,
    val groupName: String,
    val memberUserId: Long,
    val memberName: String,
    val amount: Double,
    val cycleNumber: Int,
    val paymentMethod: String,
    val transactionReference: String,
    val status: String = "VERIFIED",
    val issuedAt: Long = System.currentTimeMillis()
)
