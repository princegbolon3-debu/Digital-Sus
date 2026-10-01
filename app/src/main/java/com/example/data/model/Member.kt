package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Entity representing a group member in the Susu association.
 * Supports full local persistence CRUD operations (add, view, update, delete).
 */
@Entity(
    tableName = "members",
    indices = [
        Index(value = ["groupId", "payoutPosition"], unique = false),
        Index(value = ["groupId", "phone"], unique = false)
    ]
)
data class Member(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val groupId: Long = 1,
    val fullName: String,
    val phone: String,
    val email: String = "",
    val payoutPosition: Int = 1, // Rotation turn position (1, 2, 3...)
    val status: String = "ACTIVE", // "ACTIVE", "PAUSED", "INACTIVE", "REMOVED"
    val role: String = "MEMBER", // "MEMBER", "ORGANIZER", "SECRETARY"
    val joinedAt: Long = System.currentTimeMillis(),
    val emergencyContact: String? = null,
    val notes: String? = null
)
