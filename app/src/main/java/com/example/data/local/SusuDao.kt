package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AuditLog
import com.example.data.model.Contribution
import com.example.data.model.ContributionCycle
import com.example.data.model.GroupMember
import com.example.data.model.NotificationItem
import com.example.data.model.Payment
import com.example.data.model.Payout
import com.example.data.model.Receipt
import com.example.data.model.SusuGroup
import com.example.data.model.User
import kotlinx.coroutines.flow.Flow

@Dao
interface SusuDao {

    // --- Users ---
    @Query("SELECT * FROM users ORDER BY id ASC")
    fun getAllUsers(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE id = :id")
    fun getUserById(id: Long): Flow<User?>

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserByIdDirect(id: Long): User?

    @Query("SELECT * FROM users WHERE (LOWER(email) = LOWER(:identifier) OR phone = :identifier) LIMIT 1")
    suspend fun getUserByCredentials(identifier: String): User?

    @Query("SELECT * FROM users WHERE status = 'PENDING_APPROVAL' ORDER BY createdAt DESC")
    fun getPendingApprovalUsers(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE status = 'PENDING_APPROVAL' ORDER BY createdAt DESC")
    suspend fun getPendingApprovalUsersDirect(): List<User>

    @Query("UPDATE users SET status = :status WHERE id = :userId")
    suspend fun updateUserStatus(userId: Long, status: String)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUserById(userId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<User>): List<Long>

    @Update
    suspend fun updateUser(user: User)

    // --- Susu Groups ---
    @Query("SELECT * FROM susu_groups ORDER BY createdAt DESC")
    fun getAllGroups(): Flow<List<SusuGroup>>

    @Query("SELECT * FROM susu_groups WHERE id = :id")
    fun getGroupById(id: Long): Flow<SusuGroup?>

    @Query("SELECT * FROM susu_groups WHERE id = :id")
    suspend fun getGroupByIdDirect(id: Long): SusuGroup?

    @Query("SELECT * FROM susu_groups WHERE groupCode = :code LIMIT 1")
    suspend fun getGroupByCode(code: String): SusuGroup?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: SusuGroup): Long

    @Update
    suspend fun updateGroup(group: SusuGroup)

    // --- Group Members ---
    @Query("SELECT * FROM group_members WHERE groupId = :groupId ORDER BY payoutPosition ASC")
    fun getMembersForGroup(groupId: Long): Flow<List<GroupMember>>

    @Query("SELECT * FROM group_members WHERE groupId = :groupId ORDER BY payoutPosition ASC")
    suspend fun getMembersForGroupDirect(groupId: Long): List<GroupMember>

    @Query("SELECT * FROM group_members WHERE userId = :userId")
    fun getMembershipsForUser(userId: Long): Flow<List<GroupMember>>

    @Query("SELECT * FROM group_members WHERE groupId = :groupId AND userId = :userId LIMIT 1")
    suspend fun getMemberByGroupAndUser(groupId: Long, userId: Long): GroupMember?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: GroupMember): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<GroupMember>): List<Long>

    @Update
    suspend fun updateMember(member: GroupMember)

    @Delete
    suspend fun deleteMember(member: GroupMember)

    // --- Cycles ---
    @Query("SELECT * FROM contribution_cycles WHERE groupId = :groupId ORDER BY cycleNumber ASC")
    fun getCyclesForGroup(groupId: Long): Flow<List<ContributionCycle>>

    @Query("SELECT * FROM contribution_cycles WHERE groupId = :groupId AND cycleNumber = :cycleNumber LIMIT 1")
    suspend fun getCycleByNumber(groupId: Long, cycleNumber: Int): ContributionCycle?

    @Query("SELECT * FROM contribution_cycles WHERE groupId = :groupId AND status = 'ACTIVE' LIMIT 1")
    fun getActiveCycle(groupId: Long): Flow<ContributionCycle?>

    @Query("SELECT * FROM contribution_cycles WHERE id = :id")
    suspend fun getCycleByIdDirect(id: Long): ContributionCycle?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCycle(cycle: ContributionCycle): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCycles(cycles: List<ContributionCycle>): List<Long>

    @Update
    suspend fun updateCycle(cycle: ContributionCycle)

    // --- Contributions ---
    @Query("SELECT * FROM contributions WHERE cycleId = :cycleId")
    fun getContributionsForCycle(cycleId: Long): Flow<List<Contribution>>

    @Query("SELECT * FROM contributions WHERE cycleId = :cycleId")
    suspend fun getContributionsForCycleDirect(cycleId: Long): List<Contribution>

    @Query("SELECT * FROM contributions WHERE cycleId = :cycleId AND memberUserId = :memberUserId LIMIT 1")
    suspend fun getContributionByCycleAndMember(cycleId: Long, memberUserId: Long): Contribution?

    @Query("SELECT * FROM contributions WHERE id = :id")
    suspend fun getContributionById(id: Long): Contribution?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContribution(contribution: Contribution): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContributions(contributions: List<Contribution>): List<Long>

    @Update
    suspend fun updateContribution(contribution: Contribution)

    // --- Payments ---
    @Query("SELECT * FROM payments ORDER BY submittedAt DESC")
    fun getAllPayments(): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE groupId = :groupId ORDER BY submittedAt DESC")
    fun getPaymentsForGroup(groupId: Long): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE memberUserId = :userId ORDER BY submittedAt DESC")
    fun getPaymentsForMember(userId: Long): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE groupId = :groupId AND status = 'SUBMITTED' ORDER BY submittedAt ASC")
    fun getPendingPaymentsForGroup(groupId: Long): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE id = :id")
    suspend fun getPaymentById(id: Long): Payment?

    @Query("SELECT * FROM payments WHERE transactionReference = :ref LIMIT 1")
    suspend fun getPaymentByReference(ref: String): Payment?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: Payment): Long

    @Update
    suspend fun updatePayment(payment: Payment)

    // --- Payouts ---
    @Query("SELECT * FROM payouts WHERE groupId = :groupId ORDER BY cycleNumber ASC")
    fun getPayoutsForGroup(groupId: Long): Flow<List<Payout>>

    @Query("SELECT * FROM payouts WHERE groupId = :groupId AND cycleId = :cycleId LIMIT 1")
    suspend fun getPayoutForCycle(groupId: Long, cycleId: Long): Payout?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayout(payout: Payout): Long

    @Update
    suspend fun updatePayout(payout: Payout)

    // --- Receipts ---
    @Query("SELECT * FROM receipts ORDER BY issuedAt DESC")
    fun getAllReceipts(): Flow<List<Receipt>>

    @Query("SELECT * FROM receipts WHERE memberUserId = :userId ORDER BY issuedAt DESC")
    fun getReceiptsForMember(userId: Long): Flow<List<Receipt>>

    @Query("SELECT * FROM receipts WHERE receiptNumber = :receiptNumber LIMIT 1")
    suspend fun getReceiptByNumber(receiptNumber: String): Receipt?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceipt(receipt: Receipt): Long

    // --- Notifications ---
    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY createdAt DESC")
    fun getNotificationsForUser(userId: Long): Flow<List<NotificationItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationItem): Long

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markNotificationAsRead(id: Long)

    @Query("UPDATE notifications SET isRead = 1 WHERE userId = :userId")
    suspend fun markAllNotificationsAsRead(userId: Long)

    // --- Audit Logs ---
    @Query("SELECT * FROM audit_logs ORDER BY createdAt DESC")
    fun getAllAuditLogs(): Flow<List<AuditLog>>

    @Query("SELECT * FROM audit_logs WHERE entityType = 'GROUP' AND entityId = :groupId ORDER BY createdAt DESC")
    fun getAuditLogsForGroup(groupId: Long): Flow<List<AuditLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLog): Long
}
