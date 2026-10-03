package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun OrganizerDashboardScreen(
    currentUser: User,
    group: SusuGroup?,
    cycles: List<ContributionCycle>,
    selectedCycleNumber: Int,
    members: List<MemberWithPosition>,
    summary: CycleFinancialSummary?,
    pendingPayments: List<Payment>,
    onSelectCycle: (Int) -> Unit,
    onVerifyPayment: (paymentId: Long, isApproved: Boolean, reason: String?) -> Unit,
    onCompletePayout: (reference: String, notes: String?) -> Unit,
    onRecordMemberPayment: (memberUserId: Long) -> Unit,
    onChangeMemberPosition: (memberId: Long, newPos: Int) -> Unit,
    onCreateGroup: () -> Unit,
    onCreateCycle: ((cycleNumber: Int, daysDue: Int, status: String) -> Unit)? = null,
    pendingUsers: List<User> = emptyList(),
    onApproveUser: (userId: Long) -> Unit = {},
    onRejectUser: (userId: Long, reason: String?) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var selectedPaymentForVerification by remember { mutableStateOf<Payment?>(null) }
    var showPayoutDialog by remember { mutableStateOf(false) }
    var showCreateCycleDialog by remember { mutableStateOf(false) }
    var memberToRecordPaymentFor by remember { mutableStateOf<MemberWithPosition?>(null) }
    var memberToChangePosition by remember { mutableStateOf<MemberWithPosition?>(null) }

    val currentCycle = cycles.find { it.cycleNumber == selectedCycleNumber }
    val isCycleActive = currentCycle?.status == "ACTIVE"
    val isCycleCompleted = currentCycle?.status == "COMPLETED"

    val recipient = summary?.currentRecipient
    val isFullyVerified = (summary?.totalOutstanding ?: 0.0) == 0.0

    // Dialog for Payment Verification
    selectedPaymentForVerification?.let { payment ->
        val memberUser = members.find { it.user.id == payment.memberUserId }?.user
        VerifyPaymentDialog(
            payment = payment,
            member = memberUser,
            onDismiss = { selectedPaymentForVerification = null },
            onConfirm = { isApproved, reason ->
                onVerifyPayment(payment.id, isApproved, reason)
                selectedPaymentForVerification = null
            }
        )
    }

    // Dialog for Release Payout
    if (showPayoutDialog && group != null) {
        CompletePayoutDialog(
            recipient = recipient,
            cycleNumber = selectedCycleNumber,
            payoutAmount = summary?.totalExpected ?: (group.contributionAmount * group.numberOfCycles),
            isFullyVerified = isFullyVerified,
            onDismiss = { showPayoutDialog = false },
            onConfirm = { ref, notes ->
                onCompletePayout(ref, notes)
                showPayoutDialog = false
            }
        )
    }

    // Dialog for Creating New Cycle
    if (showCreateCycleDialog && group != null) {
        val nextCycleNum = (cycles.maxOfOrNull { it.cycleNumber } ?: 0) + 1
        CreateCycleDialog(
            suggestedCycleNumber = nextCycleNum,
            groupName = group.name,
            onDismiss = { showCreateCycleDialog = false },
            onCreate = { cycleNum, daysDue, status ->
                showCreateCycleDialog = false
                onCreateCycle?.invoke(cycleNum, daysDue, status)
            }
        )
    }

    // Dialog for recording member cash payment
    memberToRecordPaymentFor?.let { item ->
        if (group != null) {
            SubmitPaymentDialog(
                group = group,
                cycleNumber = selectedCycleNumber,
                onDismiss = { memberToRecordPaymentFor = null },
                onSubmit = { amt, method, ref, note ->
                    onRecordMemberPayment(item.user.id)
                    memberToRecordPaymentFor = null
                }
            )
        }
    }

    // Dialog to change position in rotation order
    memberToChangePosition?.let { item ->
        var newPositionText by remember { mutableStateOf(item.membership.payoutPosition.toString()) }
        androidx.compose.ui.window.Dialog(onDismissRequest = { memberToChangePosition = null }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Change Payout Position", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Set turn # for ${item.user.fullName} (Currently #${item.membership.payoutPosition})", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = newPositionText,
                        onValueChange = { newPositionText = it },
                        label = { Text("New Position (1..${members.size})") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { memberToChangePosition = null }, modifier = Modifier.weight(1f)) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                val pos = newPositionText.toIntOrNull()
                                if (pos != null && pos in 1..members.size) {
                                    onChangeMemberPosition(item.membership.id, pos)
                                    memberToChangePosition = null
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = SusuEmeraldPrimary)
                        ) {
                            Text("Save Turn")
                        }
                    }
                }
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("organizer_dashboard_scroll"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (group == null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("organizer_no_group_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(SusuEmeraldContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AddBusiness, contentDescription = null, tint = SusuEmeraldPrimary, modifier = Modifier.size(32.dp))
                        }
                        Text("No Susu Circle Assigned Yet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            text = "You are approved as a Group Organizer. You can create a new Susu savings circle now to start enrolling members and tracking contributions, or wait for the Administrator to assign you to an existing circle.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onCreateGroup,
                            colors = ButtonDefaults.buttonColors(containerColor = SusuEmeraldPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("+ Create a Susu Circle", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // Group Header Info
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("organizer_group_header"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SusuEmeraldDark)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("ORGANIZER CONTROL PANEL", style = MaterialTheme.typography.labelSmall, color = SusuGold, fontWeight = FontWeight.Bold)
                                Text(group.name, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Surface(shape = RoundedCornerShape(8.dp), color = SusuGoldContainer) {
                                Text(
                                    text = "Code: ${group.groupCode}",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SusuOnGoldContainer
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            QuickMetric(label = "Members", value = "${members.size}/${group.numberOfCycles}", color = Color.White)
                            QuickMetric(label = "Contribution", value = "L$ ${group.contributionAmount.toInt()}", color = SusuGold)
                            QuickMetric(label = "Frequency", value = group.frequency, color = Color.White)
                        }
                    }
                }
            }
        }

        // Pending Member Registrations Awaiting Organizer Approval
        if (pendingUsers.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pending_members_queue_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SusuGoldContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = SusuGoldDark)
                                Text(
                                    text = "Pending Member Approvals",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SusuOnGoldContainer
                                )
                            }
                            Surface(shape = RoundedCornerShape(8.dp), color = SusuGold) {
                                Text(
                                    text = "${pendingUsers.size} NEW",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }

                        Text(
                            text = "Review and accept new member registrations before they can access the group and contribute.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SusuOnGoldContainer
                        )
                    }
                }
            }

            items(pendingUsers) { applicant ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("applicant_item_${applicant.id}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = applicant.fullName,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "${applicant.phone}${if (applicant.email.isNotBlank()) " • ${applicant.email}" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            StatusBadge(status = "PENDING_APPROVAL")
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onRejectUser(applicant.id, "Organizer declined application") },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRejectedRed),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Decline")
                            }

                            Button(
                                onClick = { onApproveUser(applicant.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = SusuEmeraldPrimary),
                                modifier = Modifier
                                    .weight(1.5f)
                                    .testTag("approve_member_button_${applicant.id}"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Approve Member ✓", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Pending Verifications Queue (Top Priority for zero stress)
        if (pendingPayments.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Pending Verifications",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(shape = RoundedCornerShape(8.dp), color = StatusPendingContainer) {
                            Text(
                                text = "${pendingPayments.size} NEED REVIEW",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SusuGoldDark
                            )
                        }
                    }
                }
            }

            items(pendingPayments) { payment ->
                val member = members.find { it.user.id == payment.memberUserId }?.user
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pending_payment_item_${payment.id}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(member?.fullName ?: "Member #${payment.memberUserId}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("${payment.paymentMethod} • Ref: ${payment.transactionReference}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            CurrencyText(amount = payment.amount, style = MaterialTheme.typography.titleLarge, color = SusuEmeraldPrimary)
                        }

                        if (!payment.note.isNullOrBlank()) {
                            Text("Note: ${payment.note}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { selectedPaymentForVerification = payment },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRejectedRed),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Reject")
                            }
                            Button(
                                onClick = { onVerifyPayment(payment.id, true, null) },
                                colors = ButtonDefaults.buttonColors(containerColor = SusuEmeraldPrimary),
                                modifier = Modifier.weight(1.5f).testTag("quick_verify_button_${payment.id}"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Approve & Receipt ✓", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SusuEmeraldContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF047857))
                        Column {
                            Text("All Caught Up!", fontWeight = FontWeight.Bold, color = Color(0xFF065F46))
                            Text("No member payments are currently waiting for verification.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF047857))
                        }
                    }
                }
            }
        }

        // Cycle Selector Tabs (FR-010)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Select Contribution Cycle", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    TextButton(
                        onClick = { showCreateCycleDialog = true },
                        modifier = Modifier.testTag("add_cycle_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ New Cycle", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("cycle_selector_row")
                ) {
                    items(cycles) { cycle ->
                        val isSelected = cycle.cycleNumber == selectedCycleNumber
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectCycle(cycle.cycleNumber) },
                            label = {
                                Text("Cycle ${cycle.cycleNumber} ${if (cycle.status == "COMPLETED") "✓" else ""}")
                            },
                            leadingIcon = {
                                if (cycle.status == "COMPLETED") {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusPaidGreen, modifier = Modifier.size(16.dp))
                                } else if (cycle.status == "ACTIVE") {
                                    Icon(Icons.Default.PlayCircle, contentDescription = null, tint = SusuEmeraldPrimary, modifier = Modifier.size(16.dp))
                                }
                            }
                        )
                    }
                }
            }
        }

        // Financial KPIs Card (FR-014, FR-016)
        item {
            summary?.let { s ->
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("financial_kpi_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "FINANCIAL SUMMARY • CYCLE ${s.cycleNumber}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Expected Total", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                CurrencyText(amount = s.totalExpected, style = MaterialTheme.typography.titleMedium)
                            }
                            Column {
                                Text("Verified Collected", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                CurrencyText(amount = s.totalVerifiedCollected, style = MaterialTheme.typography.titleMedium, color = SusuEmeraldPrimary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Outstanding", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                CurrencyText(amount = s.totalOutstanding, style = MaterialTheme.typography.titleMedium, color = if (s.totalOutstanding > 0) StatusRejectedRed else StatusPaidGreen)
                            }
                        }

                        // Progress
                        val progress = if (s.totalExpected > 0) (s.totalVerifiedCollected / s.totalExpected).toFloat() else 0f
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            LinearProgressIndicator(
                                progress = { progress.coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = SusuEmeraldPrimary,
                                trackColor = SusuEmeraldContainer
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${s.verifiedMemberCount} of ${s.totalMembers} verified", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${(progress * 100).toInt()}% collected", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = SusuEmeraldPrimary)
                            }
                        }
                    }
                }
            }
        }

        // Current Cycle Payout Recipient & Release Button (FR-017, FR-018, BR-006)
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("recipient_payout_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCycleCompleted) StatusPaidContainer else SusuGoldContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CYCLE ${selectedCycleNumber} PAYOUT RECIPIENT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = SusuOnGoldContainer
                        )
                        StatusBadge(status = if (isCycleCompleted) "COMPLETED" else "PENDING_PAYOUT")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = recipient?.fullName ?: "Member #${selectedCycleNumber}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Rotation Turn #${selectedCycleNumber} • ${recipient?.phone ?: ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = SusuOnGoldContainer.copy(alpha = 0.8f)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Payout Pool", style = MaterialTheme.typography.labelSmall, color = SusuOnGoldContainer)
                            CurrencyText(
                                amount = summary?.totalExpected ?: 0.0,
                                style = MaterialTheme.typography.titleLarge,
                                color = SusuEmeraldPrimary
                            )
                        }
                    }

                    if (!isCycleCompleted) {
                        Button(
                            onClick = { showPayoutDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("release_payout_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = SusuEmeraldPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Celebration, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Release Payout & Advance Cycle")
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF047857))
                            Text(
                                text = "Payout of L$${summary?.totalExpected?.toInt() ?: 0} has been completed to ${recipient?.fullName}.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF065F46),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Member Roster & Payment Status (FR-013)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Member Contributions Roster",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text("Cycle $selectedCycleNumber", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        items(members) { item ->
            val contribution = item.currentCycleContribution
            val status = contribution?.status ?: "UNPAID"

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { memberToChangePosition = item },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("#${item.membership.payoutPosition}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.user.fullName, fontWeight = FontWeight.Bold)
                        Text(item.user.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        StatusBadge(status = status)
                        if (status == "UNPAID") {
                            Text(
                                text = "Record Cash",
                                style = MaterialTheme.typography.labelSmall,
                                color = SusuEmeraldPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { memberToRecordPaymentFor = item }
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun QuickMetric(label: String, value: String, color: Color) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
    }
}
