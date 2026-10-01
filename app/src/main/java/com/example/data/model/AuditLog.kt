package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val userName: String,
    val action: String, // "PAYMENT_SUBMITTED", "PAYMENT_VERIFIED", "PAYMENT_REJECTED", "PAYOUT_APPROVED", "PAYOUT_COMPLETED", "MEMBER_ADDED", "CYCLE_ADVANCED", "GROUP_CREATED", "ORDER_UPDATED"
    val entityType: String, // "PAYMENT", "PAYOUT", "GROUP", "MEMBER", "CYCLE"
    val entityId: Long,
    val description: String,
    val createdAt: Long = System.currentTimeMillis()
)
