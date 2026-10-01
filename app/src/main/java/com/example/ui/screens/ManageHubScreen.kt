package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.*
import com.example.ui.theme.SusuEmeraldPrimary

@Composable
fun ManageHubScreen(
    currentUser: User,
    group: SusuGroup?,
    cycles: List<ContributionCycle>,
    selectedCycleNumber: Int,
    members: List<MemberWithPosition>,
    summary: CycleFinancialSummary?,
    pendingPayments: List<Payment>,
    allUsers: List<User>,
    allGroups: List<SusuGroup>,
    allPayments: List<Payment>,
    auditLogs: List<AuditLog>,
    groupPayouts: List<Payout>,
    roomMembers: List<Member> = emptyList(),
    onSelectCycle: (Int) -> Unit,
    onVerifyPayment: (paymentId: Long, isApproved: Boolean, reason: String?) -> Unit,
    onCompletePayout: (reference: String, notes: String?) -> Unit,
    onRecordMemberPayment: (memberUserId: Long) -> Unit,
    onChangeMemberPosition: (memberId: Long, newPos: Int) -> Unit,
    onCreateGroup: () -> Unit,
    onRegisterUser: () -> Unit,
    onCreateCycle: ((cycleNumber: Int, daysDue: Int, status: String) -> Unit)? = null,
    pendingUsers: List<User> = emptyList(),
    onApproveUser: (userId: Long) -> Unit = {},
    onRejectUser: (userId: Long, reason: String?) -> Unit = { _, _ -> },
    onAddMember: ((name: String, phone: String, email: String, position: Int, status: String, role: String, notes: String?, emergencyContact: String?) -> Unit)? = null,
    onUpdateMember: ((Member) -> Unit)? = null,
    onDeleteMember: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("manage_hub_screen")
    ) {
        // Tab selector bar
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = SusuEmeraldPrimary,
            modifier = Modifier.fillMaxWidth().testTag("manage_hub_tab_row")
        ) {
            // Tab 0: Organizer Dashboard
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Organizer") },
                icon = {
                    val totalActionCount = pendingPayments.size + pendingUsers.size
                    BadgedBox(
                        badge = {
                            if (totalActionCount > 0) {
                                Badge { Text(totalActionCount.toString()) }
                            }
                        }
                    ) {
                        Icon(Icons.Default.SupervisedUserCircle, contentDescription = null)
                    }
                },
                modifier = Modifier.testTag("hub_tab_organizer")
            )

            // Tab 1: Group Members (Room DB CRUD)
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Members") },
                icon = {
                    BadgedBox(
                        badge = {
                            if (roomMembers.isNotEmpty()) {
                                Badge { Text(roomMembers.size.toString()) }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Groups, contentDescription = null)
                    }
                },
                modifier = Modifier.testTag("hub_tab_members")
            )

            // Tab 2: Financial Reports
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Reports") },
                icon = { Icon(Icons.Default.Assessment, contentDescription = null) },
                modifier = Modifier.testTag("hub_tab_reports")
            )

            // Tab 3: System Audit Trail
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = { Text("Audit Trail") },
                icon = { Icon(Icons.Default.HistoryEdu, contentDescription = null) },
                modifier = Modifier.testTag("hub_tab_audit")
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> OrganizerDashboardScreen(
                    currentUser = currentUser,
                    group = group,
                    cycles = cycles,
                    selectedCycleNumber = selectedCycleNumber,
                    members = members,
                    summary = summary,
                    pendingPayments = pendingPayments,
                    onSelectCycle = onSelectCycle,
                    onVerifyPayment = onVerifyPayment,
                    onCompletePayout = onCompletePayout,
                    onRecordMemberPayment = onRecordMemberPayment,
                    onChangeMemberPosition = onChangeMemberPosition,
                    onCreateGroup = onCreateGroup,
                    onCreateCycle = onCreateCycle,
                    pendingUsers = pendingUsers,
                    onApproveUser = onApproveUser,
                    onRejectUser = onRejectUser
                )

                1 -> GroupMembersScreen(
                    group = group,
                    members = roomMembers,
                    onAddMember = { name, phone, email, pos, st, rl, notes, em ->
                        onAddMember?.invoke(name, phone, email, pos, st, rl, notes, em)
                    },
                    onUpdateMember = { m ->
                        onUpdateMember?.invoke(m)
                    },
                    onDeleteMember = { id ->
                        onDeleteMember?.invoke(id)
                    }
                )

                2 -> ReportsScreen(
                    group = group,
                    members = members,
                    summary = summary,
                    payouts = groupPayouts,
                    payments = allPayments,
                    allUsers = allUsers
                )

                3 -> AdminDashboardScreen(
                    users = allUsers,
                    groups = allGroups,
                    payments = allPayments,
                    auditLogs = auditLogs,
                    onRegisterUser = onRegisterUser
                )
            }
        }
    }
}
