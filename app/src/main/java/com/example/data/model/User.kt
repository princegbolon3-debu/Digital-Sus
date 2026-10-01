package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fullName: String,
    val phone: String,
    val email: String,
    val role: String, // "MEMBER", "ORGANIZER", "ADMIN"
    val password: String = "123456",
    val profileImageUrl: String? = null,
    val status: String = "ACTIVE", // "ACTIVE", "PENDING_APPROVAL", "REJECTED", "SUSPENDED"
    val createdAt: Long = System.currentTimeMillis()
)
