package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val title: String,
    val message: String,
    val type: String, // "REMINDER", "CONFIRMATION", "PAYOUT", "ANNOUNCEMENT", "ALERT"
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
