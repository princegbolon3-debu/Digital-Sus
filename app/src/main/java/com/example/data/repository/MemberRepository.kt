package com.example.data.repository

import com.example.data.local.MemberDao
import com.example.data.model.Member
import kotlinx.coroutines.flow.Flow

/**
 * Repository layer for managing Susu association group members.
 * Implements clean architecture, encapsulating Room DAO operations
 * and enforcing business logic and validation for CRUD operations.
 */
class MemberRepository(private val memberDao: MemberDao) {

    val allMembers: Flow<List<Member>> = memberDao.getAllMembers()

    fun getMembersForGroup(groupId: Long): Flow<List<Member>> {
        return memberDao.getMembersByGroupId(groupId)
    }

    suspend fun getMembersForGroupDirect(groupId: Long): List<Member> {
        return memberDao.getMembersByGroupIdDirect(groupId)
    }

    fun getMemberById(id: Long): Flow<Member?> {
        return memberDao.getMemberById(id)
    }

    suspend fun getMemberByIdDirect(id: Long): Member? {
        return memberDao.getMemberByIdDirect(id)
    }

    fun searchMembers(groupId: Long, query: String): Flow<List<Member>> {
        val trimmed = query.trim()
        return if (trimmed.isEmpty()) {
            memberDao.getMembersByGroupId(groupId)
        } else {
            memberDao.searchMembersInGroup(groupId, trimmed)
        }
    }

    fun getMemberCount(groupId: Long): Flow<Int> {
        return memberDao.getMemberCount(groupId)
    }

    /**
     * Create (Add) a new member to the Susu association.
     */
    suspend fun addMember(member: Member): OperationResult<Long> {
        val cleanName = member.fullName.trim()
        if (cleanName.isBlank()) {
            return OperationResult.Error("Member full name cannot be blank.")
        }
        val cleanPhone = member.phone.trim()
        if (cleanPhone.isBlank()) {
            return OperationResult.Error("Mobile phone number is required.")
        }

        // Auto-assign payout position if not set or invalid
        val maxPos = memberDao.getMaxPayoutPosition(member.groupId) ?: 0
        val assignedPosition = if (member.payoutPosition <= 0) {
            maxPos + 1
        } else {
            member.payoutPosition
        }

        val sanitizedMember = member.copy(
            fullName = cleanName,
            phone = cleanPhone,
            email = member.email.trim(),
            payoutPosition = assignedPosition,
            joinedAt = if (member.joinedAt <= 0) System.currentTimeMillis() else member.joinedAt
        )

        return try {
            val insertedId = memberDao.insertMember(sanitizedMember)
            OperationResult.Success(insertedId, "$cleanName added to group at position #$assignedPosition.")
        } catch (e: Exception) {
            OperationResult.Error("Failed to add member: ${e.localizedMessage ?: "Unknown database error"}")
        }
    }

    /**
     * Update existing member details (Name, Phone, Email, Position, Status, Notes).
     */
    suspend fun updateMember(member: Member): OperationResult<Unit> {
        if (member.id <= 0) {
            return OperationResult.Error("Cannot update member: Invalid member ID.")
        }
        val cleanName = member.fullName.trim()
        if (cleanName.isBlank()) {
            return OperationResult.Error("Member full name cannot be blank.")
        }
        val cleanPhone = member.phone.trim()
        if (cleanPhone.isBlank()) {
            return OperationResult.Error("Mobile phone number cannot be blank.")
        }

        val existing = memberDao.getMemberByIdDirect(member.id)
            ?: return OperationResult.Error("Member record not found in database.")

        // If payout position was changed, check for conflict and swap
        if (existing.payoutPosition != member.payoutPosition && member.payoutPosition > 0) {
            val conflictingMember = memberDao.getMemberByPosition(member.groupId, member.payoutPosition)
            if (conflictingMember != null && conflictingMember.id != member.id) {
                // Swap position with the conflicting member to preserve ordering
                memberDao.updateMember(conflictingMember.copy(payoutPosition = existing.payoutPosition))
            }
        }

        val sanitized = member.copy(
            fullName = cleanName,
            phone = cleanPhone,
            email = member.email.trim()
        )

        return try {
            val rows = memberDao.updateMember(sanitized)
            if (rows > 0) {
                OperationResult.Success(Unit, "Member $cleanName updated successfully.")
            } else {
                OperationResult.Error("No member row was updated.")
            }
        } catch (e: Exception) {
            OperationResult.Error("Failed to update member: ${e.localizedMessage ?: "Unknown database error"}")
        }
    }

    /**
     * Delete / Remove a member from the Susu association.
     */
    suspend fun deleteMember(memberId: Long): OperationResult<Unit> {
        val existing = memberDao.getMemberByIdDirect(memberId)
            ?: return OperationResult.Error("Member not found or already deleted.")

        return try {
            val rows = memberDao.deleteMemberById(memberId)
            if (rows > 0) {
                OperationResult.Success(Unit, "${existing.fullName} removed from group.")
            } else {
                OperationResult.Error("Failed to remove member from database.")
            }
        } catch (e: Exception) {
            OperationResult.Error("Failed to delete member: ${e.localizedMessage ?: "Database error"}")
        }
    }

    /**
     * Update rotation order / payout position.
     */
    suspend fun updateMemberPosition(groupId: Long, memberId: Long, newPosition: Int): OperationResult<Unit> {
        if (newPosition <= 0) {
            return OperationResult.Error("Payout position must be 1 or greater.")
        }
        val target = memberDao.getMemberByIdDirect(memberId)
            ?: return OperationResult.Error("Member not found.")

        val existingAtPos = memberDao.getMemberByPosition(groupId, newPosition)
        if (existingAtPos != null && existingAtPos.id != memberId) {
            memberDao.updateMember(existingAtPos.copy(payoutPosition = target.payoutPosition))
        }

        memberDao.updateMember(target.copy(payoutPosition = newPosition))
        return OperationResult.Success(Unit, "Updated ${target.fullName}'s turn to #$newPosition.")
    }
}
