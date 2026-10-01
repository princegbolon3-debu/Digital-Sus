package com.example.data.model

data class MemberWithPosition(
    val user: User,
    val membership: GroupMember,
    val currentCycleContribution: Contribution?,
    val latestPayment: Payment?
)

data class CycleFinancialSummary(
    val cycleNumber: Int,
    val totalMembers: Int,
    val contributionPerMember: Double,
    val totalExpected: Double,
    val totalVerifiedCollected: Double,
    val totalOutstanding: Double,
    val verifiedMemberCount: Int,
    val pendingMemberCount: Int,
    val unpaidMemberCount: Int,
    val currentRecipient: User?,
    val payoutStatus: String?
)

data class GroupReportSummary(
    val group: SusuGroup,
    val totalMembers: Int,
    val activeCycleNumber: Int,
    val totalExpectedOverall: Double,
    val totalCollectedOverall: Double,
    val totalPayoutsCompleted: Double,
    val completionPercentage: Float
)
