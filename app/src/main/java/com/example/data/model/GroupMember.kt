package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "group_members",
    indices = [
        Index(value = ["groupId", "payoutPosition"], unique = false),
        Index(value = ["groupId", "userId"], unique = true)
    ]
)
data class GroupMember(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val groupId: Long,
    val userId: Long,
    val payoutPosition: Int, // 1, 2, 3...
    val status: String = "ACTIVE", // "ACTIVE", "PENDING_APPROVAL", "REMOVED"
    val joinedAt: Long = System.currentTimeMillis()
)
