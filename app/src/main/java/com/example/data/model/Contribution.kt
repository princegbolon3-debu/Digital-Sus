package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "contributions",
    indices = [
        Index(value = ["cycleId", "memberUserId"], unique = true)
    ]
)
data class Contribution(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cycleId: Long,
    val groupId: Long,
    val memberUserId: Long,
    val expectedAmount: Double,
    val paidAmount: Double = 0.0,
    val status: String = "UNPAID", // "UNPAID", "SUBMITTED", "VERIFIED", "LATE"
    val dueDate: Long,
    val createdAt: Long = System.currentTimeMillis()
)
