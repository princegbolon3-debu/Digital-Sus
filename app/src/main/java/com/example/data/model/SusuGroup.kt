package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "susu_groups")
data class SusuGroup(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val groupCode: String,
    val description: String,
    val organizerId: Long,
    val contributionAmount: Double, // in LRD (L$)
    val frequency: String, // "DAILY", "WEEKLY", "BIWEEKLY", "MONTHLY"
    val startDate: Long,
    val numberOfCycles: Int,
    val currentCycle: Int = 1,
    val payoutMethod: String = "ROTATION_ORDER", // "ROTATION_ORDER"
    val status: String = "ACTIVE", // "ACTIVE", "COMPLETED", "CLOSED"
    val createdAt: Long = System.currentTimeMillis()
)
