package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payouts")
data class Payout(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val groupId: Long,
    val cycleId: Long,
    val cycleNumber: Int,
    val recipientUserId: Long,
    val amount: Double,
    val status: String = "PENDING", // "PENDING", "APPROVED", "COMPLETED", "FAILED"
    val paymentMethod: String = "Lonestar MTN Mobile Money",
    val transactionReference: String? = null,
    val approvedAt: Long? = null,
    val completedAt: Long? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
