package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Member
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for Susu association Group Members.
 * Provides reactive queries and suspend CRUD operations.
 */
@Dao
interface MemberDao {

    @Query("SELECT * FROM members ORDER BY payoutPosition ASC")
    fun getAllMembers(): Flow<List<Member>>

    @Query("SELECT * FROM members WHERE groupId = :groupId ORDER BY payoutPosition ASC")
    fun getMembersByGroupId(groupId: Long): Flow<List<Member>>

    @Query("SELECT * FROM members WHERE groupId = :groupId ORDER BY payoutPosition ASC")
    suspend fun getMembersByGroupIdDirect(groupId: Long): List<Member>

    @Query("SELECT * FROM members WHERE id = :id LIMIT 1")
    fun getMemberById(id: Long): Flow<Member?>

    @Query("SELECT * FROM members WHERE id = :id LIMIT 1")
    suspend fun getMemberByIdDirect(id: Long): Member?

    @Query("SELECT * FROM members WHERE groupId = :groupId AND payoutPosition = :position LIMIT 1")
    suspend fun getMemberByPosition(groupId: Long, position: Int): Member?

    @Query("SELECT * FROM members WHERE groupId = :groupId AND (fullName LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%') ORDER BY payoutPosition ASC")
    fun searchMembersInGroup(groupId: Long, query: String): Flow<List<Member>>

    @Query("SELECT COUNT(*) FROM members WHERE groupId = :groupId")
    fun getMemberCount(groupId: Long): Flow<Int>

    @Query("SELECT MAX(payoutPosition) FROM members WHERE groupId = :groupId")
    suspend fun getMaxPayoutPosition(groupId: Long): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: Member): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<Member>): List<Long>

    @Update
    suspend fun updateMember(member: Member): Int

    @Delete
    suspend fun deleteMember(member: Member): Int

    @Query("DELETE FROM members WHERE id = :id")
    suspend fun deleteMemberById(id: Long): Int

    @Query("DELETE FROM members WHERE groupId = :groupId")
    suspend fun deleteAllMembersForGroup(groupId: Long): Int
}
