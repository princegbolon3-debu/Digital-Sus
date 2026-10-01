package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contribution_cycles")
data class ContributionCycle(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val groupId: Long,
    val cycleNumber: Int,
    val startDate: Long,
    val dueDate: Long,
    val expectedAmount: Double,
    val status: String = "ACTIVE", // "ACTIVE", "PENDING_PAYOUT", "PAID_OUT", "COMPLETED"
    val createdAt: Long = System.currentTimeMillis()
)
