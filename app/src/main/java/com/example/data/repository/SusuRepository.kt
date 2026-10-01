package com.example.data.repository

import com.example.data.local.SusuDao
import com.example.data.model.AuditLog
import com.example.data.model.Contribution
import com.example.data.model.ContributionCycle
import com.example.data.model.CycleFinancialSummary
import com.example.data.model.GroupMember
import com.example.data.model.MemberWithPosition
import com.example.data.model.NotificationItem
import com.example.data.model.Payment
import com.example.data.model.Payout
import com.example.data.model.Receipt
import com.example.data.model.SusuGroup
import com.example.data.model.User
import com.example.data.remote.FirebaseSyncManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.UUID

sealed class OperationResult<out T> {
    data class Success<T>(val data: T, val message: String? = null) : OperationResult<T>()
    data class Error(val message: String) : OperationResult<Nothing>()
}

class SusuRepository(private val dao: SusuDao) {

    val allUsers: Flow<List<User>> = dao.getAllUsers()
    val pendingUsers: Flow<List<User>> = dao.getPendingApprovalUsers()
    val allGroups: Flow<List<SusuGroup>> = dao.getAllGroups()
    val allPayments: Flow<List<Payment>> = dao.getAllPayments()
    val allReceipts: Flow<List<Receipt>> = dao.getAllReceipts()
    val allAuditLogs: Flow<List<AuditLog>> = dao.getAllAuditLogs()

    fun getUser(userId: Long): Flow<User?> = dao.getUserById(userId)
    fun getGroup(groupId: Long): Flow<SusuGroup?> = dao.getGroupById(groupId)
    fun getNotificationsForUser(userId: Long): Flow<List<NotificationItem>> = dao.getNotificationsForUser(userId)
    fun getReceiptsForUser(userId: Long): Flow<List<Receipt>> = dao.getReceiptsForMember(userId)
    fun getPaymentsForUser(userId: Long): Flow<List<Payment>> = dao.getPaymentsForMember(userId)
    fun getPendingPayments(groupId: Long): Flow<List<Payment>> = dao.getPendingPaymentsForGroup(groupId)
    fun getCyclesForGroup(groupId: Long): Flow<List<ContributionCycle>> = dao.getCyclesForGroup(groupId)
    fun getPayoutsForGroup(groupId: Long): Flow<List<Payout>> = dao.getPayoutsForGroup(groupId)

    // Combine group members with their user info, current cycle contribution, and payment status
    fun getMembersWithDetails(groupId: Long, cycleNumber: Int): Flow<List<MemberWithPosition>> {
        return combine(
            dao.getMembersForGroup(groupId),
            dao.getAllUsers(),
            dao.getCyclesForGroup(groupId)
        ) { members, users, cycles ->
            val userMap = users.associateBy { it.id }
            val cycle = cycles.find { it.cycleNumber == cycleNumber }

            members.mapNotNull { member ->
                val user = userMap[member.userId] ?: return@mapNotNull null
                val contribution = if (cycle != null) {
                    dao.getContributionByCycleAndMember(cycle.id, member.userId)
                } else null

                val payment = if (contribution != null) {
                    dao.getPaymentById(contribution.id)
                } else null

                MemberWithPosition(
                    user = user,
                    membership = member,
                    currentCycleContribution = contribution,
                    latestPayment = payment
                )
            }
        }
    }

    // Get live financial summary for a cycle
    suspend fun getCycleSummary(groupId: Long, cycleNumber: Int): CycleFinancialSummary? {
        val group = dao.getGroupByIdDirect(groupId) ?: return null
        val members = dao.getMembersForGroupDirect(groupId)
        val cycle = dao.getCycleByNumber(groupId, cycleNumber) ?: return null
        val contributions = dao.getContributionsForCycleDirect(cycle.id)
        val payouts = dao.getPayoutForCycle(groupId, cycle.id)

        val totalExpected = members.size * group.contributionAmount
        val verifiedContributions = contributions.filter { it.status == "VERIFIED" }
        val verifiedTotal = verifiedContributions.sumOf { it.paidAmount }
        val pendingCount = contributions.count { it.status == "SUBMITTED" }
        val unpaidCount = members.size - verifiedContributions.size - pendingCount

        // Recipient is the member whose payoutPosition matches the cycleNumber
        val recipientMember = members.find { it.payoutPosition == cycleNumber }
        val recipientUser = recipientMember?.let { dao.getUserByIdDirect(it.userId) }

        return CycleFinancialSummary(
            cycleNumber = cycleNumber,
            totalMembers = members.size,
            contributionPerMember = group.contributionAmount,
            totalExpected = totalExpected,
            totalVerifiedCollected = verifiedTotal,
            totalOutstanding = (totalExpected - verifiedTotal).coerceAtLeast(0.0),
            verifiedMemberCount = verifiedContributions.size,
            pendingMemberCount = pendingCount,
            unpaidMemberCount = unpaidCount.coerceAtLeast(0),
            currentRecipient = recipientUser,
            payoutStatus = payouts?.status ?: "PENDING"
        )
    }

    // FR-011, FR-015: Payment Recording with Duplicate Reference Prevention
    suspend fun submitPayment(
        memberUserId: Long,
        groupId: Long,
        cycleId: Long,
        amount: Double,
        paymentMethod: String,
        reference: String,
        note: String?
    ): OperationResult<Long> {
        val trimmedRef = reference.trim()
        if (trimmedRef.isEmpty()) {
            return OperationResult.Error("Transaction reference number is required.")
        }

        // FR-015: Check duplicate reference
        val existing = dao.getPaymentByReference(trimmedRef)
        if (existing != null) {
            return OperationResult.Error("Duplicate Transaction: Reference '$trimmedRef' has already been recorded.")
        }

        val user = dao.getUserByIdDirect(memberUserId) ?: return OperationResult.Error("User not found.")
        val group = dao.getGroupByIdDirect(groupId) ?: return OperationResult.Error("Group not found.")
        val cycle = dao.getCycleByIdDirect(cycleId) ?: return OperationResult.Error("Cycle not found.")

        // Find or create contribution entry
        var contribution = dao.getContributionByCycleAndMember(cycleId, memberUserId)
        val contributionId = if (contribution == null) {
            val newC = Contribution(
                cycleId = cycleId,
                groupId = groupId,
                memberUserId = memberUserId,
                expectedAmount = group.contributionAmount,
                paidAmount = amount,
                status = "SUBMITTED",
                dueDate = cycle.dueDate
            )
            dao.insertContribution(newC)
        } else {
            dao.updateContribution(contribution.copy(status = "SUBMITTED", paidAmount = amount))
            contribution.id
        }

        val payment = Payment(
            contributionId = contributionId,
            groupId = groupId,
            cycleId = cycleId,
            memberUserId = memberUserId,
            amount = amount,
            paymentMethod = paymentMethod,
            transactionReference = trimmedRef,
            status = "SUBMITTED",
            note = note,
            submittedAt = System.currentTimeMillis()
        )
        val paymentId = dao.insertPayment(payment)

        // Audit Log
        dao.insertAuditLog(
            AuditLog(
                userId = memberUserId,
                userName = user.fullName,
                action = "PAYMENT_SUBMITTED",
                entityType = "PAYMENT",
                entityId = paymentId,
                description = "Submitted payment of L$$amount ($paymentMethod, Ref: $trimmedRef) for Cycle ${cycle.cycleNumber}."
            )
        )

        // Notify group organizer
        dao.insertNotification(
            NotificationItem(
                userId = group.organizerId,
                title = "Payment Submitted",
                message = "${user.fullName} submitted L$$amount via $paymentMethod (Ref: $trimmedRef) for Cycle ${cycle.cycleNumber}. Awaiting verification.",
                type = "ALERT"
            )
        )

        return OperationResult.Success(paymentId, "Payment submitted successfully! Status: PENDING VERIFICATION.")
    }

    // FR-012, FR-020: Verify or Reject Payment
    suspend fun verifyPayment(
        paymentId: Long,
        organizerUserId: Long,
        isApproved: Boolean,
        rejectionReason: String? = null
    ): OperationResult<Boolean> {
        val payment = dao.getPaymentById(paymentId) ?: return OperationResult.Error("Payment record not found.")
        val organizer = dao.getUserByIdDirect(organizerUserId) ?: return OperationResult.Error("Organizer not found.")
        val member = dao.getUserByIdDirect(payment.memberUserId) ?: return OperationResult.Error("Member not found.")
        val group = dao.getGroupByIdDirect(payment.groupId) ?: return OperationResult.Error("Group not found.")
        val cycle = dao.getCycleByIdDirect(payment.cycleId) ?: return OperationResult.Error("Cycle not found.")

        val now = System.currentTimeMillis()

        if (isApproved) {
            // Update payment to VERIFIED
            val updatedPayment = payment.copy(
                status = "VERIFIED",
                verifiedAt = now,
                verifiedByUserId = organizerUserId
            )
            dao.updatePayment(updatedPayment)

            // Update contribution
            val contribution = dao.getContributionById(payment.contributionId)
            if (contribution != null) {
                dao.updateContribution(contribution.copy(status = "VERIFIED", paidAmount = payment.amount))
            }

            // FR-020: Generate Receipt
            val receiptNumber = "SUSU-${java.text.SimpleDateFormat("yyyy", java.util.Locale.US).format(java.util.Date())}-%06d".format((1000..999999).random())
            val receipt = Receipt(
                receiptNumber = receiptNumber,
                paymentId = payment.id,
                groupId = group.id,
                groupName = group.name,
                memberUserId = member.id,
                memberName = member.fullName,
                amount = payment.amount,
                cycleNumber = cycle.cycleNumber,
                paymentMethod = payment.paymentMethod,
                transactionReference = payment.transactionReference,
                status = "VERIFIED",
                issuedAt = now
            )
            dao.insertReceipt(receipt)

            // Audit Log
            dao.insertAuditLog(
                AuditLog(
                    userId = organizerUserId,
                    userName = organizer.fullName,
                    action = "PAYMENT_VERIFIED",
                    entityType = "PAYMENT",
                    entityId = payment.id,
                    description = "Verified L$${payment.amount} contribution from ${member.fullName}. Generated receipt $receiptNumber."
                )
            )

            // Member Notification
            dao.insertNotification(
                NotificationItem(
                    userId = member.id,
                    title = "Contribution Verified! ✓",
                    message = "Your L$${payment.amount} contribution for ${group.name} Cycle ${cycle.cycleNumber} has been verified. Receipt: $receiptNumber.",
                    type = "CONFIRMATION"
                )
            )

            return OperationResult.Success(true, "Payment verified! Receipt $receiptNumber generated.")
        } else {
            // Rejected
            val updatedPayment = payment.copy(
                status = "REJECTED",
                rejectionReason = rejectionReason ?: "Invalid details or unconfirmed receipt by organizer."
            )
            dao.updatePayment(updatedPayment)

            val contribution = dao.getContributionById(payment.contributionId)
            if (contribution != null) {
                dao.updateContribution(contribution.copy(status = "UNPAID", paidAmount = 0.0))
            }

            dao.insertAuditLog(
                AuditLog(
                    userId = organizerUserId,
                    userName = organizer.fullName,
                    action = "PAYMENT_REJECTED",
                    entityType = "PAYMENT",
                    entityId = payment.id,
                    description = "Rejected payment Ref ${payment.transactionReference} from ${member.fullName}. Reason: $rejectionReason."
                )
            )

            dao.insertNotification(
                NotificationItem(
                    userId = member.id,
                    title = "Payment Rejected",
                    message = "Your contribution submission (Ref: ${payment.transactionReference}) was not verified. Reason: ${rejectionReason ?: "Please contact organizer"}.",
                    type = "ALERT"
                )
            )

            return OperationResult.Success(true, "Payment rejected and member notified.")
        }
    }

    // FR-018, FR-019: Complete Payout and Advance Rotation Cycle
    suspend fun completePayoutAndAdvanceCycle(
        groupId: Long,
        cycleId: Long,
        organizerUserId: Long,
        transactionRef: String,
        notes: String?
    ): OperationResult<Boolean> {
        val group = dao.getGroupByIdDirect(groupId) ?: return OperationResult.Error("Group not found.")
        val cycle = dao.getCycleByIdDirect(cycleId) ?: return OperationResult.Error("Cycle not found.")
        val organizer = dao.getUserByIdDirect(organizerUserId) ?: return OperationResult.Error("Organizer not found.")
        val members = dao.getMembersForGroupDirect(groupId)

        val recipientMember = members.find { it.payoutPosition == cycle.cycleNumber }
            ?: return OperationResult.Error("No recipient configured for cycle position ${cycle.cycleNumber}.")
        val recipientUser = dao.getUserByIdDirect(recipientMember.userId)
            ?: return OperationResult.Error("Recipient user not found.")

        val now = System.currentTimeMillis()
        val dayMs = 24L * 60L * 60L * 1000L

        // 1. Update or create Payout record
        var existingPayout = dao.getPayoutForCycle(groupId, cycleId)
        val payoutAmount = members.size * group.contributionAmount

        if (existingPayout != null) {
            dao.updatePayout(
                existingPayout.copy(
                    status = "COMPLETED",
                    transactionReference = transactionRef,
                    completedAt = now,
                    notes = notes
                )
            )
        } else {
            dao.insertPayout(
                Payout(
                    groupId = groupId,
                    cycleId = cycleId,
                    cycleNumber = cycle.cycleNumber,
                    recipientUserId = recipientUser.id,
                    amount = payoutAmount,
                    status = "COMPLETED",
                    paymentMethod = "Lonestar MTN Mobile Money",
                    transactionReference = transactionRef,
                    approvedAt = now,
                    completedAt = now,
                    notes = notes
                )
            )
        }

        // 2. Mark current cycle completed
        dao.updateCycle(cycle.copy(status = "COMPLETED"))

        // Audit Log
        dao.insertAuditLog(
            AuditLog(
                userId = organizerUserId,
                userName = organizer.fullName,
                action = "PAYOUT_COMPLETED",
                entityType = "PAYOUT",
                entityId = cycle.id,
                description = "Cycle ${cycle.cycleNumber} payout of L$$payoutAmount completed to ${recipientUser.fullName} (Ref: $transactionRef)."
            )
        )

        // Notification to recipient
        dao.insertNotification(
            NotificationItem(
                userId = recipientUser.id,
                title = "Susu Payout Received! 🎉",
                message = "Your Susu payout of L$$payoutAmount for ${group.name} Cycle ${cycle.cycleNumber} has been completed (Ref: $transactionRef).",
                type = "PAYOUT"
            )
        )

        // 3. Advance to next cycle or complete group!
        val nextCycleNum = cycle.cycleNumber + 1
        if (nextCycleNum <= group.numberOfCycles) {
            // Next cycle duration
            val cycleDurationMs = when (group.frequency) {
                "DAILY" -> 1 * dayMs
                "WEEKLY" -> 7 * dayMs
                "BIWEEKLY" -> 14 * dayMs
                "MONTHLY" -> 30 * dayMs
                else -> 7 * dayMs
            }

            val nextCycle = ContributionCycle(
                groupId = groupId,
                cycleNumber = nextCycleNum,
                startDate = now,
                dueDate = now + cycleDurationMs,
                expectedAmount = payoutAmount,
                status = "ACTIVE"
            )
            val nextCycleId = dao.insertCycle(nextCycle)
            dao.updateGroup(group.copy(currentCycle = nextCycleNum))

            // Pre-create contribution obligations for all members for next cycle
            members.forEach { m ->
                dao.insertContribution(
                    Contribution(
                        cycleId = nextCycleId,
                        groupId = groupId,
                        memberUserId = m.userId,
                        expectedAmount = group.contributionAmount,
                        paidAmount = 0.0,
                        status = "UNPAID",
                        dueDate = now + cycleDurationMs
                    )
                )
            }

            // Next recipient
            val nextRecipientMember = members.find { it.payoutPosition == nextCycleNum }
            if (nextRecipientMember != null) {
                dao.insertPayout(
                    Payout(
                        groupId = groupId,
                        cycleId = nextCycleId,
                        cycleNumber = nextCycleNum,
                        recipientUserId = nextRecipientMember.userId,
                        amount = payoutAmount,
                        status = "PENDING"
                    )
                )
            }

            // Announcements to all group members
            members.forEach { m ->
                dao.insertNotification(
                    NotificationItem(
                        userId = m.userId,
                        title = "Cycle $nextCycleNum Started",
                        message = "Cycle $nextCycleNum for ${group.name} is now ACTIVE. Contribution of L$${group.contributionAmount} is due in ${group.frequency.lowercase()}.",
                        type = "ANNOUNCEMENT"
                    )
                )
            }

            dao.insertAuditLog(
                AuditLog(
                    userId = organizerUserId,
                    userName = organizer.fullName,
                    action = "CYCLE_ADVANCED",
                    entityType = "CYCLE",
                    entityId = nextCycleId,
                    description = "Advanced group ${group.name} to Cycle $nextCycleNum."
                )
            )

            return OperationResult.Success(true, "Payout completed! Advanced to Cycle $nextCycleNum.")
        } else {
            // All cycles completed!
            dao.updateGroup(group.copy(status = "COMPLETED"))
            dao.insertAuditLog(
                AuditLog(
                    userId = organizerUserId,
                    userName = organizer.fullName,
                    action = "GROUP_COMPLETED",
                    entityType = "GROUP",
                    entityId = group.id,
                    description = "All ${group.numberOfCycles} cycles completed for ${group.name}! Full Susu rotation concluded."
                )
            )
            return OperationResult.Success(true, "Final payout completed! Susu group rotation successfully concluded.")
        }
    }

    // FR-005: Create Susu Group
    suspend fun createGroup(
        name: String,
        code: String,
        description: String,
        organizerId: Long,
        contributionAmount: Double,
        frequency: String,
        numberOfCycles: Int
    ): OperationResult<Long> {
        val cleanCode = code.trim().uppercase()
        val existing = dao.getGroupByCode(cleanCode)
        if (existing != null) {
            return OperationResult.Error("Group code '$cleanCode' is already taken. Please choose another.")
        }

        val organizer = dao.getUserByIdDirect(organizerId) ?: return OperationResult.Error("Organizer not found.")
        val now = System.currentTimeMillis()
        val dayMs = 24L * 60L * 60L * 1000L

        val group = SusuGroup(
            name = name.trim(),
            groupCode = cleanCode,
            description = description.trim(),
            organizerId = organizerId,
            contributionAmount = contributionAmount,
            frequency = frequency,
            startDate = now,
            numberOfCycles = numberOfCycles,
            currentCycle = 1,
            status = "ACTIVE"
        )
        val groupId = dao.insertGroup(group)

        // Add organizer as position 1
        dao.insertMember(
            GroupMember(
                groupId = groupId,
                userId = organizerId,
                payoutPosition = 1,
                status = "ACTIVE",
                joinedAt = now
            )
        )

        // Create Cycle 1
        val cycleDurationMs = when (frequency) {
            "DAILY" -> 1 * dayMs
            "WEEKLY" -> 7 * dayMs
            "BIWEEKLY" -> 14 * dayMs
            "MONTHLY" -> 30 * dayMs
            else -> 7 * dayMs
        }

        val cycleId = dao.insertCycle(
            ContributionCycle(
                groupId = groupId,
                cycleNumber = 1,
                startDate = now,
                dueDate = now + cycleDurationMs,
                expectedAmount = contributionAmount * numberOfCycles,
                status = "ACTIVE"
            )
        )

        // Organizer contribution obligation
        dao.insertContribution(
            Contribution(
                cycleId = cycleId,
                groupId = groupId,
                memberUserId = organizerId,
                expectedAmount = contributionAmount,
                paidAmount = 0.0,
                status = "UNPAID",
                dueDate = now + cycleDurationMs
            )
        )

        // Pending Payout for Cycle 1
        dao.insertPayout(
            Payout(
                groupId = groupId,
                cycleId = cycleId,
                cycleNumber = 1,
                recipientUserId = organizerId,
                amount = contributionAmount * numberOfCycles,
                status = "PENDING"
            )
        )

        dao.insertAuditLog(
            AuditLog(
                userId = organizerId,
                userName = organizer.fullName,
                action = "GROUP_CREATED",
                entityType = "GROUP",
                entityId = groupId,
                description = "Created Susu group '$name' ($cleanCode) with L$$contributionAmount $frequency."
            )
        )

        return OperationResult.Success(groupId, "Susu group '$name' created successfully!")
    }

    // FR-008, FR-010: Create Contribution Cycle
    suspend fun createCycle(
        groupId: Long,
        cycleNumber: Int,
        startDate: Long,
        dueDate: Long,
        status: String = "ACTIVE",
        organizerUserId: Long
    ): OperationResult<Long> {
        val group = dao.getGroupByIdDirect(groupId) ?: return OperationResult.Error("Group not found.")
        val organizer = dao.getUserByIdDirect(organizerUserId) ?: return OperationResult.Error("Organizer not found.")
        val existingCycle = dao.getCycleByNumber(groupId, cycleNumber)
        if (existingCycle != null) {
            return OperationResult.Error("Cycle $cycleNumber already exists for ${group.name}.")
        }

        val members = dao.getMembersForGroupDirect(groupId)
        val expectedTotal = members.size * group.contributionAmount

        val cycleId = dao.insertCycle(
            ContributionCycle(
                groupId = groupId,
                cycleNumber = cycleNumber,
                startDate = startDate,
                dueDate = dueDate,
                expectedAmount = expectedTotal,
                status = status
            )
        )

        // Pre-create contribution records for all members for this cycle
        members.forEach { m ->
            dao.insertContribution(
                Contribution(
                    cycleId = cycleId,
                    groupId = groupId,
                    memberUserId = m.userId,
                    expectedAmount = group.contributionAmount,
                    paidAmount = 0.0,
                    status = "UNPAID",
                    dueDate = dueDate
                )
            )
        }

        // Add payout placeholder if recipient exists
        val recipientMember = members.find { it.payoutPosition == cycleNumber }
        if (recipientMember != null) {
            dao.insertPayout(
                Payout(
                    groupId = groupId,
                    cycleId = cycleId,
                    cycleNumber = cycleNumber,
                    recipientUserId = recipientMember.userId,
                    amount = expectedTotal,
                    status = "PENDING"
                )
            )
        }

        if (cycleNumber > group.numberOfCycles) {
            dao.updateGroup(group.copy(numberOfCycles = cycleNumber))
        }

        dao.insertAuditLog(
            AuditLog(
                userId = organizerUserId,
                userName = organizer.fullName,
                action = "CYCLE_CREATED",
                entityType = "CYCLE",
                entityId = cycleId,
                description = "Created Cycle $cycleNumber for ${group.name} with expected total L$$expectedTotal."
            )
        )

        members.forEach { m ->
            dao.insertNotification(
                NotificationItem(
                    userId = m.userId,
                    title = "New Cycle $cycleNumber Opened",
                    message = "Cycle $cycleNumber for ${group.name} is now open. Contribution amount is L$${group.contributionAmount}.",
                    type = "ANNOUNCEMENT"
                )
            )
        }

        return OperationResult.Success(cycleId, "Cycle $cycleNumber created successfully!")
    }

    // FR-006: Join Susu Group via Code
    suspend fun joinGroup(groupCode: String, userId: Long): OperationResult<Boolean> {
        val group = dao.getGroupByCode(groupCode.trim().uppercase())
            ?: return OperationResult.Error("No Susu group found with code '${groupCode.trim().uppercase()}'.")

        if (group.status != "ACTIVE") {
            return OperationResult.Error("This Susu group is no longer active.")
        }

        val existingMember = dao.getMemberByGroupAndUser(group.id, userId)
        if (existingMember != null) {
            return OperationResult.Error("You are already a member of ${group.name}.")
        }

        val user = dao.getUserByIdDirect(userId) ?: return OperationResult.Error("User not found.")
        val members = dao.getMembersForGroupDirect(group.id)
        if (members.size >= group.numberOfCycles) {
            return OperationResult.Error("This group has reached maximum member capacity (${group.numberOfCycles}).")
        }

        val nextPosition = members.size + 1
        val now = System.currentTimeMillis()

        dao.insertMember(
            GroupMember(
                groupId = group.id,
                userId = userId,
                payoutPosition = nextPosition,
                status = "ACTIVE",
                joinedAt = now
            )
        )

        // Add contribution for current active cycle
        val activeCycles = dao.getCyclesForGroup(group.id)
        // Check current cycle
        val cycle = dao.getCycleByNumber(group.id, group.currentCycle)
        if (cycle != null) {
            dao.insertContribution(
                Contribution(
                    cycleId = cycle.id,
                    groupId = group.id,
                    memberUserId = userId,
                    expectedAmount = group.contributionAmount,
                    paidAmount = 0.0,
                    status = "UNPAID",
                    dueDate = cycle.dueDate
                )
            )
        }

        dao.insertAuditLog(
            AuditLog(
                userId = userId,
                userName = user.fullName,
                action = "MEMBER_JOINED",
                entityType = "GROUP",
                entityId = group.id,
                description = "${user.fullName} joined ${group.name} as position #$nextPosition."
            )
        )

        dao.insertNotification(
            NotificationItem(
                userId = group.organizerId,
                title = "New Member Joined",
                message = "${user.fullName} has joined ${group.name} as payout position #$nextPosition.",
                type = "ANNOUNCEMENT"
            )
        )

        return OperationResult.Success(true, "Successfully joined ${group.name}!")
    }

    // FR-009, BR-003: Update Payout Order with No Duplicates
    suspend fun updateMemberPosition(
        groupId: Long,
        memberId: Long,
        newPosition: Int,
        organizerUserId: Long
    ): OperationResult<Boolean> {
        val members = dao.getMembersForGroupDirect(groupId)
        val targetMember = members.find { it.id == memberId } ?: return OperationResult.Error("Member not found.")

        // Check duplicate
        val existingAtPos = members.find { it.payoutPosition == newPosition && it.id != memberId }
        if (existingAtPos != null) {
            // Swap positions for clean reordering
            dao.updateMember(existingAtPos.copy(payoutPosition = targetMember.payoutPosition))
            dao.updateMember(targetMember.copy(payoutPosition = newPosition))
        } else {
            dao.updateMember(targetMember.copy(payoutPosition = newPosition))
        }

        val organizer = dao.getUserByIdDirect(organizerUserId)
        val targetUser = dao.getUserByIdDirect(targetMember.userId)

        dao.insertAuditLog(
            AuditLog(
                userId = organizerUserId,
                userName = organizer?.fullName ?: "Organizer",
                action = "ORDER_UPDATED",
                entityType = "GROUP",
                entityId = groupId,
                description = "Updated payout rotation position for ${targetUser?.fullName} to #$newPosition."
            )
        )

        return OperationResult.Success(true, "Rotation order updated successfully.")
    }

    // User Login
    suspend fun login(identifier: String, password: String): OperationResult<User> {
        val clean = identifier.trim()
        if (clean.isBlank()) {
            return OperationResult.Error("Please enter your phone number or email address.")
        }
        var user = dao.getUserByCredentials(clean)

        // Cross-device cloud check: If user not in local database or pending, check Cloud Firestore
        if (user == null || user.status == "PENDING_APPROVAL") {
            try {
                val cloudUser = FirebaseSyncManager.checkCloudUserStatus(clean)
                if (cloudUser != null) {
                    if (user == null) {
                        val newId = dao.insertUser(cloudUser)
                        user = cloudUser.copy(id = newId)
                    } else if (cloudUser.status != user.status) {
                        user = user.copy(status = cloudUser.status)
                        dao.updateUser(user)
                    }
                }
            } catch (_: Exception) {}
        }

        if (user == null) {
            return OperationResult.Error("No account found with identifier '$clean'. Please check or create an account.")
        }

        if (user.password != password) {
            return OperationResult.Error("Incorrect password. Please try again.")
        }

        if (user.status == "PENDING_APPROVAL") {
            return OperationResult.Error("Account Pending Approval: The System Administrator (Prince Gbolon) must accept your account before you can log in.")
        }

        if (user.status == "SUSPENDED") {
            return OperationResult.Error("Account Suspended: Please contact the system administrator.")
        }

        if (user.status == "REJECTED") {
            return OperationResult.Error("Account Declined: Your registration application was not accepted by the administrator.")
        }

        return OperationResult.Success(user, "Welcome back, ${user.fullName}!")
    }

    // User Registration with Role-based Approval
    suspend fun registerUser(
        fullName: String,
        phone: String,
        email: String,
        role: String,
        password: String = "123456"
    ): OperationResult<Long> {
        if (fullName.isBlank() || phone.isBlank()) {
            return OperationResult.Error("Full name and phone number are required.")
        }

        val cleanPhone = phone.trim()
        val cleanEmail = email.trim()
        val existing = dao.getUserByCredentials(cleanPhone)
            ?: if (cleanEmail.isNotBlank()) dao.getUserByCredentials(cleanEmail) else null

        if (existing != null) {
            return OperationResult.Error("An account with phone $cleanPhone or email $cleanEmail already exists.")
        }

        // Both MEMBER and ORGANIZER accounts are PENDING_APPROVAL by the administrator
        val initialStatus = if (role.uppercase() == "ADMIN") "ACTIVE" else "PENDING_APPROVAL"

        val user = User(
            fullName = fullName.trim(),
            phone = cleanPhone,
            email = cleanEmail,
            role = role.uppercase(),
            password = if (password.isNotBlank()) password else "123456",
            status = initialStatus
        )
        val id = dao.insertUser(user)
        val savedUser = user.copy(id = id)

        // Sync registration to Cloud Firestore so Admin on Phone B receives it
        try {
            FirebaseSyncManager.syncUserToCloud(savedUser)
        } catch (_: Exception) {}

        dao.insertAuditLog(
            AuditLog(
                userId = id,
                userName = fullName,
                action = "USER_REGISTERED",
                entityType = "USER",
                entityId = id,
                description = if (initialStatus == "PENDING_APPROVAL")
                    "$role account registered by $fullName ($cleanPhone) and awaiting Administrator (Prince Gbolon) approval."
                else "New account registered as $role."
            )
        )

        if (initialStatus == "PENDING_APPROVAL") {
            // Notify System Administrator (Prince Gbolon id 1)
            dao.insertNotification(
                NotificationItem(
                    userId = 1,
                    title = "New $role Awaiting Approval",
                    message = "$fullName ($cleanPhone) has registered as a $role and is awaiting your administrative approval.",
                    type = "ALERT"
                )
            )
            return OperationResult.Success(id, "Account registered! Awaiting Administrator (Prince Gbolon) approval before activation.")
        } else {
            return OperationResult.Success(id, "Account registered successfully! You may now sign in.")
        }
    }

    // Approve Pending Member Account (Organizer or Admin action)
    suspend fun approveUser(userId: Long, approverId: Long): OperationResult<Boolean> {
        val user = dao.getUserByIdDirect(userId) ?: return OperationResult.Error("User not found.")
        val approver = dao.getUserByIdDirect(approverId)

        dao.updateUserStatus(userId, "ACTIVE")

        // Sync approval to Cloud Firestore so Phone A unlocks immediately
        try {
            FirebaseSyncManager.updateCloudUserStatus(user.phone, "ACTIVE")
            if (user.email.isNotBlank()) {
                FirebaseSyncManager.updateCloudUserStatus(user.email, "ACTIVE")
            }
        } catch (_: Exception) {}

        dao.insertAuditLog(
            AuditLog(
                userId = approverId,
                userName = approver?.fullName ?: "Administrator",
                action = "MEMBER_APPROVED",
                entityType = "USER",
                entityId = userId,
                description = "Approved ${user.role} account for ${user.fullName} (${user.phone})."
            )
        )

        dao.insertNotification(
            NotificationItem(
                userId = userId,
                title = "Account Approved! 🎉",
                message = "Your ${user.role.lowercase()} registration has been approved by the Administrator. You can now log in and join Susu groups.",
                type = "CONFIRMATION"
            )
        )

        return OperationResult.Success(true, "Approved ${user.fullName} successfully.")
    }

    // Reject Member Account (Organizer or Admin action)
    suspend fun rejectUser(userId: Long, approverId: Long, reason: String? = null): OperationResult<Boolean> {
        val user = dao.getUserByIdDirect(userId) ?: return OperationResult.Error("User not found.")
        val approver = dao.getUserByIdDirect(approverId)

        dao.updateUserStatus(userId, "REJECTED")

        dao.insertAuditLog(
            AuditLog(
                userId = approverId,
                userName = approver?.fullName ?: "Organizer",
                action = "MEMBER_REJECTED",
                entityType = "USER",
                entityId = userId,
                description = "Rejected member registration for ${user.fullName}. Reason: ${reason ?: "Not specified"}."
            )
        )

        dao.insertNotification(
            NotificationItem(
                userId = userId,
                title = "Registration Application Rejected",
                message = "Your registration was declined. Reason: ${reason ?: "Please contact group organizer."}",
                type = "ALERT"
            )
        )

        return OperationResult.Success(true, "Application rejected.")
    }

    // Admin action: Suspend or Activate User
    suspend fun suspendUser(userId: Long, adminId: Long): OperationResult<Boolean> {
        val user = dao.getUserByIdDirect(userId) ?: return OperationResult.Error("User not found.")
        val admin = dao.getUserByIdDirect(adminId)
        val newStatus = if (user.status == "SUSPENDED") "ACTIVE" else "SUSPENDED"
        dao.updateUserStatus(userId, newStatus)

        dao.insertAuditLog(
            AuditLog(
                userId = adminId,
                userName = admin?.fullName ?: "Admin",
                action = if (newStatus == "SUSPENDED") "USER_SUSPENDED" else "USER_ACTIVATED",
                entityType = "USER",
                entityId = userId,
                description = "Changed status of ${user.fullName} to $newStatus."
            )
        )
        return OperationResult.Success(true, "User status updated to $newStatus.")
    }

    // Admin action: Delete User
    suspend fun deleteUser(userId: Long, adminId: Long): OperationResult<Boolean> {
        val user = dao.getUserByIdDirect(userId) ?: return OperationResult.Error("User not found.")
        val admin = dao.getUserByIdDirect(adminId)
        dao.deleteUserById(userId)

        dao.insertAuditLog(
            AuditLog(
                userId = adminId,
                userName = admin?.fullName ?: "Admin",
                action = "USER_DELETED",
                entityType = "USER",
                entityId = userId,
                description = "Deleted user account ${user.fullName} (${user.email})."
            )
        )
        return OperationResult.Success(true, "User deleted successfully.")
    }

    suspend fun markNotificationRead(id: Long) {
        dao.markNotificationAsRead(id)
    }

    suspend fun markAllNotificationsRead(userId: Long) {
        dao.markAllNotificationsAsRead(userId)
    }

    // Update user personal profile
    suspend fun updateProfile(
        userId: Long,
        fullName: String,
        phone: String,
        email: String,
        newPassword: String? = null
    ): OperationResult<User> {
        val user = dao.getUserByIdDirect(userId) ?: return OperationResult.Error("User not found.")
        if (fullName.isBlank() || phone.isBlank()) {
            return OperationResult.Error("Full name and phone number cannot be blank.")
        }

        val cleanPhone = phone.trim()
        val cleanEmail = email.trim()

        // Check if phone or email already belongs to another user
        val existingWithPhone = dao.getUserByCredentials(cleanPhone)
        if (existingWithPhone != null && existingWithPhone.id != userId) {
            return OperationResult.Error("Another user is already registered with phone number $cleanPhone.")
        }

        val updatedUser = user.copy(
            fullName = fullName.trim(),
            phone = cleanPhone,
            email = cleanEmail,
            password = if (!newPassword.isNullOrBlank()) newPassword.trim() else user.password
        )
        dao.updateUser(updatedUser)

        dao.insertAuditLog(
            AuditLog(
                userId = userId,
                userName = updatedUser.fullName,
                action = "PROFILE_UPDATED",
                entityType = "USER",
                entityId = userId,
                description = "${updatedUser.fullName} updated their profile information."
            )
        )

        return OperationResult.Success(updatedUser, "Profile updated successfully!")
    }
}
