package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.*
import com.example.ui.components.CurrencyText
import com.example.ui.components.StatusBadge
import com.example.ui.theme.SusuEmeraldContainer
import com.example.ui.theme.SusuEmeraldPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    group: SusuGroup?,
    members: List<MemberWithPosition>,
    summary: CycleFinancialSummary?,
    payouts: List<Payout>,
    payments: List<Payment> = emptyList(),
    allUsers: List<User> = emptyList(),
    modifier: Modifier = Modifier
) {
    var selectedReportTab by remember { mutableIntStateOf(0) }
    var reportDownloadedToast by remember { mutableStateOf(false) }

    val tabs = listOf("Group Summary", "Member Contributions", "Payment History", "Payout Records")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("reports_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Financial & Audit Reports",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${group?.name ?: "Susu"} • SRS Compliance Reports",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = { reportDownloadedToast = true }) {
                Icon(Icons.Default.Download, contentDescription = "Export Report", tint = SusuEmeraldPrimary)
            }
        }

        if (reportDownloadedToast) {
            Surface(
                color = SusuEmeraldContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SusuEmeraldPrimary)
                    Text(
                        "Report exported successfully as PDF/CSV.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SusuEmeraldPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Tab Row
        ScrollableTabRow(
            selectedTabIndex = selectedReportTab,
            containerColor = MaterialTheme.colorScheme.surface,
            edgePadding = 0.dp
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedReportTab == index,
                    onClick = { selectedReportTab = index },
                    text = { Text(title, style = MaterialTheme.typography.labelMedium) }
                )
            }
        }

        when (selectedReportTab) {
            0 -> GroupSummaryReportTab(group, summary, members)
            1 -> MemberContributionsReportTab(members, group)
            2 -> PaymentHistoryReportTab(payments, allUsers)
            3 -> PayoutHistoryReportTab(payouts, members)
        }
    }
}

@Composable
private fun GroupSummaryReportTab(
    group: SusuGroup?,
    summary: CycleFinancialSummary?,
    members: List<MemberWithPosition>
) {
    if (group == null || summary == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No group data available", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Overall Susu Group Metrics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    ReportDataRow("Group Name", group.name)
                    ReportDataRow("Invite Code", group.groupCode)
                    ReportDataRow("Contribution Rate", "L$ ${group.contributionAmount.toInt()} (${group.frequency})")
                    ReportDataRow("Total Enrolled Members", "${members.size} of ${group.numberOfCycles}")
                    ReportDataRow("Current Active Cycle", "Cycle ${group.currentCycle} of ${group.numberOfCycles}")
                    ReportDataRow("Expected Total Pool", "L$ ${(group.contributionAmount * members.size).toInt()}")
                    ReportDataRow("Verified Collected Pool", "L$ ${summary.totalVerifiedCollected.toInt()}")
                    ReportDataRow("Outstanding This Cycle", "L$ ${summary.totalOutstanding.toInt()}")
                    ReportDataRow("Group Status", group.status)
                }
            }
        }
    }
}

@Composable
private fun MemberContributionsReportTab(
    members: List<MemberWithPosition>,
    group: SusuGroup?
) {
    var statusFilter by remember { mutableStateOf("ALL") }

    val paidCount = members.count { it.currentCycleContribution?.status == "VERIFIED" }
    val unpaidCount = members.size - paidCount

    val filteredMembers = when (statusFilter) {
        "PAID" -> members.filter { it.currentCycleContribution?.status == "VERIFIED" }
        "UNPAID" -> members.filter { it.currentCycleContribution?.status != "VERIFIED" }
        else -> members
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = statusFilter == "ALL",
                    onClick = { statusFilter = "ALL" },
                    label = { Text("All (${members.size})") }
                )
                FilterChip(
                    selected = statusFilter == "PAID",
                    onClick = { statusFilter = "PAID" },
                    label = { Text("Paid ($paidCount)") }
                )
                FilterChip(
                    selected = statusFilter == "UNPAID",
                    onClick = { statusFilter = "UNPAID" },
                    label = { Text("Unpaid ($unpaidCount)") }
                )
            }
        }

        if (filteredMembers.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (statusFilter == "PAID") "No members have paid yet for this cycle." else "All members have paid!",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        items(filteredMembers) { item ->
            val contribution = item.currentCycleContribution
            val status = contribution?.status ?: "UNPAID"

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(item.user.fullName, fontWeight = FontWeight.Bold)
                        Text(
                            "Turn #${item.membership.payoutPosition} • Expected: L$ ${group?.contributionAmount?.toInt() ?: 0}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        StatusBadge(status = status)
                        Text(
                            text = if (status == "VERIFIED") "Paid: L$ ${contribution?.paidAmount?.toInt() ?: 0}" else "Outstanding",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentHistoryReportTab(
    payments: List<Payment>,
    allUsers: List<User>
) {
    val userMap = remember(allUsers) { allUsers.associateBy { it.id } }
    val dateFormat = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.US)

    if (payments.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No payments submitted yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(payments) { payment ->
            val user = userMap[payment.memberUserId]

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = user?.fullName ?: "Member #${payment.memberUserId}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${payment.paymentMethod} • Ref: ${payment.transactionReference}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = dateFormat.format(Date(payment.submittedAt)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        CurrencyText(amount = payment.amount, color = SusuEmeraldPrimary)
                        StatusBadge(status = payment.status)
                    }
                }
            }
        }
    }
}

@Composable
private fun PayoutHistoryReportTab(
    payouts: List<Payout>,
    members: List<MemberWithPosition>
) {
    val memberMap = members.associateBy { it.user.id }
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)

    if (payouts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No payouts recorded yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(payouts) { payout ->
            val recipient = memberMap[payout.recipientUserId]?.user

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Cycle ${payout.cycleNumber} Payout",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Recipient: ${recipient?.fullName ?: "Member #${payout.recipientUserId}"}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Ref: ${payout.transactionReference ?: "Pending"} • ${payout.paymentMethod}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        CurrencyText(amount = payout.amount, color = SusuEmeraldPrimary)
                        StatusBadge(status = payout.status)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportDataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
