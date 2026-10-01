package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.*
import com.example.ui.components.CurrencyText
import com.example.ui.components.DigitalReceiptDialog
import com.example.ui.components.HowItWorksDialog
import com.example.ui.components.StatusBadge
import com.example.ui.components.SubmitPaymentDialog
import com.example.ui.theme.*

/**
 * Member-Specific Dashboard Screen.
 * Exclusively displays personal savings contributions, personal turn position,
 * cycle progress, and payment actions. All administrative and organizer controls
 * are strictly hidden.
 */
@Composable
fun MemberDashboardScreen(
    currentUser: User,
    group: SusuGroup?,
    members: List<MemberWithPosition>,
    summary: CycleFinancialSummary?,
    userReceipts: List<Receipt>,
    onSubmitPayment: (amount: Double, method: String, ref: String, note: String?) -> Unit,
    onOpenReceipt: (Receipt) -> Unit = {},
    onJoinGroup: () -> Unit = {},
    onCreateGroup: () -> Unit = {},
    onEditProfile: () -> Unit = {},
    onSwitchUser: () -> Unit = {},
    onViewTurns: () -> Unit = {},
    onViewReceipts: () -> Unit = {},
    onShowHowItWorks: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showPaymentDialog by remember { mutableStateOf(false) }
    var selectedReceipt by remember { mutableStateOf<Receipt?>(null) }
    var showLocalHowItWorks by remember { mutableStateOf(false) }

    val myMembership = members.find { it.user.id == currentUser.id }
    val myContribution = myMembership?.currentCycleContribution
    val myStatus = myContribution?.status ?: "UNPAID"
    val myPosition = myMembership?.membership?.payoutPosition ?: 1

    val nextRecipient = summary?.currentRecipient
    val isMyTurnThisCycle = nextRecipient?.id == currentUser.id || (group != null && myPosition == group.currentCycle)

    // Calculate personal savings metrics
    val totalPersonalSaved = userReceipts.sumOf { it.amount }
    val expectedLumpSum = if (group != null) {
        summary?.totalExpected ?: (group.contributionAmount * group.numberOfCycles)
    } else 0.0

    // Cycle progress
    val currentCycle = group?.currentCycle ?: 1
    val totalCycles = group?.numberOfCycles ?: 10
    val cycleProgress = (currentCycle.toFloat() / totalCycles.toFloat()).coerceIn(0f, 1f)

    if (showPaymentDialog && group != null) {
        SubmitPaymentDialog(
            group = group,
            cycleNumber = group.currentCycle,
            memberPhone = currentUser.phone,
            onDismiss = { showPaymentDialog = false },
            onSubmit = { amt, method, ref, note ->
                onSubmitPayment(amt, method, ref, note)
                showPaymentDialog = false
            }
        )
    }

    if (showLocalHowItWorks) {
        HowItWorksDialog(onDismiss = { showLocalHowItWorks = false })
    }

    selectedReceipt?.let { receipt ->
        DigitalReceiptDialog(receipt = receipt, onDismiss = { selectedReceipt = null })
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("member_dashboard_scroll"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Personal Welcome & Hero Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("member_hero_banner"),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_susu_hero),
                        contentDescription = "Susu Community Savings",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentScale = ContentScale.Crop
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.35f),
                                        Color.Black.copy(alpha = 0.85f)
                                    )
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .padding(18.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White.copy(alpha = 0.2f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("🇱🇷", fontSize = 14.sp)
                                    Text(
                                        text = "MEMBER SAVINGS PORTAL",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SusuGold,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = SusuGold,
                                modifier = Modifier
                                    .clickable { onEditProfile() }
                                    .testTag("member_profile_pill")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "My Profile",
                                        tint = Color.Black,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "My Profile",
                                        color = Color.Black,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Welcome back, ${currentUser.fullName}",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            if (group != null) {
                                Text(
                                    text = "${group.name} • Group Code: ${group.groupCode}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            } else {
                                Text(
                                    text = "Transparent savings and rotating credit without stress.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (group != null) {
            // 2. Personal Savings Summary Grid (Only Personal Metrics - No Admin Totals)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Personal Total Saved
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("personal_total_saved_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SusuEmeraldContainer)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Savings,
                                    contentDescription = null,
                                    tint = SusuEmeraldDark,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "My Total Saved",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SusuOnEmeraldContainer
                                )
                            }
                            CurrencyText(
                                amount = totalPersonalSaved,
                                style = MaterialTheme.typography.titleLarge,
                                color = SusuEmeraldDark,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${userReceipts.size} verified contributions",
                                style = MaterialTheme.typography.bodySmall,
                                color = SusuOnEmeraldContainer.copy(alpha = 0.8f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    // My Turn & Expected Pot
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("personal_turn_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isMyTurnThisCycle) SusuGoldContainer else MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Stars,
                                    contentDescription = null,
                                    tint = if (isMyTurnThisCycle) SusuGoldDark else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (isMyTurnThisCycle) "Payout Turn!" else "My Payout Turn",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMyTurnThisCycle) SusuOnGoldContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "Turn #$myPosition",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isMyTurnThisCycle) Color(0xFFB45309) else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isMyTurnThisCycle) "🎉 Taking pot this cycle!" else "Pot: L$ ${expectedLumpSum.toInt()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isMyTurnThisCycle) Color(0xFF92400E) else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // 3. Cycle Progress Section (Visual Tracker of Cycle 1..N)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cycle_progress_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                            Column {
                                Text(
                                    text = "CYCLE PROGRESS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Cycle $currentCycle of $totalCycles",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SusuEmeraldContainer
                            ) {
                                Text(
                                    text = "${(cycleProgress * 100).toInt()}% COMPLETED",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SusuOnEmeraldContainer
                                )
                            }
                        }

                        // Progress Bar
                        LinearProgressIndicator(
                            progress = { cycleProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = SusuEmeraldPrimary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        // Cycle Details Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Repeat,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${group.frequency} Contribution",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.CardGiftcard,
                                    contentDescription = null,
                                    tint = SusuGoldDark,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (isMyTurnThisCycle) "You are taking the pot!" else "Recipient: ${nextRecipient?.fullName ?: "Member #$currentCycle"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isMyTurnThisCycle) Color(0xFFB45309) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // 4. Personal Contribution Payment Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("my_contribution_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (myStatus) {
                            "VERIFIED", "PAID" -> StatusPaidContainer.copy(alpha = 0.5f)
                            "SUBMITTED" -> StatusPendingContainer.copy(alpha = 0.5f)
                            else -> MaterialTheme.colorScheme.surface
                        }
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
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
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(SusuEmeraldPrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Payment,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "MY CONTRIBUTION (CYCLE $currentCycle)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    CurrencyText(
                                        amount = group.contributionAmount,
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = SusuEmeraldPrimary
                                    )
                                }
                            }
                            StatusBadge(status = myStatus)
                        }

                        // Explanatory status feedback
                        when (myStatus) {
                            "VERIFIED", "PAID" -> {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF047857))
                                        Text(
                                            text = "You're all set! Your contribution for Cycle $currentCycle is verified and settled.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF065F46),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            "SUBMITTED" -> {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.HourglassTop, contentDescription = null, tint = SusuGoldDark)
                                        Column {
                                            Text(
                                                text = "Payment submitted! Waiting for Organizer to verify.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = SusuGoldDark
                                            )
                                            Text(
                                                text = "No action needed from you right now. Sit back and relax!",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF78350F)
                                            )
                                        }
                                    }
                                }
                            }

                            else -> {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = "Please submit your ${group.frequency.lowercase()} contribution of L$ ${group.contributionAmount.toInt()} using Lonestar MTN, Orange Money, or Cash.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Button(
                                        onClick = { showPaymentDialog = true },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("member_submit_payment_button"),
                                        colors = ButtonDefaults.buttonColors(containerColor = SusuEmeraldPrimary),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Icon(Icons.Default.Payment, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Submit Payment (MoMo / Cash)", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Member Quick Actions Row (Only Personal Member Actions)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionItem(
                        title = "Pay Susu",
                        subtitle = "MoMo / Cash",
                        icon = Icons.Default.Payment,
                        tint = SusuEmeraldPrimary,
                        bgColor = SusuEmeraldContainer,
                        modifier = Modifier.weight(1f),
                        onClick = { showPaymentDialog = true }
                    )

                    QuickActionItem(
                        title = "Turns List",
                        subtitle = "My Schedule",
                        icon = Icons.Default.Repeat,
                        tint = SusuGoldDark,
                        bgColor = SusuGoldContainer,
                        modifier = Modifier.weight(1f),
                        onClick = onViewTurns
                    )

                    QuickActionItem(
                        title = "My Receipts",
                        subtitle = "${userReceipts.size} Issued",
                        icon = Icons.Default.ReceiptLong,
                        tint = Color(0xFF2563EB),
                        bgColor = Color(0xFFDBEAFE),
                        modifier = Modifier.weight(1f),
                        onClick = onViewReceipts
                    )

                    QuickActionItem(
                        title = "Guide",
                        subtitle = "How It Works",
                        icon = Icons.Default.HelpOutline,
                        tint = Color(0xFF7C3AED),
                        bgColor = Color(0xFFEDE9FE),
                        modifier = Modifier.weight(1f),
                        onClick = { showLocalHowItWorks = true }
                    )
                }
            }

            // 6. Personal Receipts & Payment History
            if (userReceipts.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "My Digital Receipts",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = onViewReceipts) {
                            Text("View All", color = SusuEmeraldPrimary)
                        }
                    }
                }

                items(userReceipts.take(3)) { receipt ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReceipt = receipt }
                            .testTag("receipt_item_${receipt.receiptNumber}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = SusuEmeraldPrimary)
                                Column {
                                    Text(receipt.receiptNumber, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = "Cycle ${receipt.cycleNumber} • ${receipt.paymentMethod}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            CurrencyText(
                                amount = receipt.amount,
                                style = MaterialTheme.typography.titleMedium,
                                color = SusuEmeraldPrimary
                            )
                        }
                    }
                }
            }

            // 7. Rotation Turns Schedule Preview (Read-Only)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Rotation Order Schedule",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onViewTurns) {
                        Text("See All Turns", color = SusuEmeraldPrimary)
                    }
                }
            }

            items(members) { item ->
                val isMe = item.user.id == currentUser.id
                val isCurrentTurn = item.membership.payoutPosition == currentCycle
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = if (isCurrentTurn) SusuEmeraldContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface,
                    border = if (isMe) androidx.compose.foundation.BorderStroke(1.5.dp, SusuEmeraldPrimary) else null
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isCurrentTurn) SusuEmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "#${item.membership.payoutPosition}",
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrentTurn) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = item.user.fullName,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (isMe) {
                                    Surface(shape = RoundedCornerShape(4.dp), color = SusuEmeraldPrimary) {
                                        Text(
                                            text = "YOU",
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                            Text(
                                text = if (item.membership.payoutPosition == currentCycle) "Receiving pot this cycle" else "Turn #${item.membership.payoutPosition}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        StatusBadge(status = item.currentCycleContribution?.status ?: "UNPAID")
                    }
                }
            }
        } else {
            // When not yet joined in any group: Clean member join invitation (NO admin controls)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(
                            Icons.Default.GroupAdd,
                            contentDescription = null,
                            tint = SusuEmeraldPrimary,
                            modifier = Modifier.size(52.dp)
                        )
                        Text(
                            text = "Join a Susu Group",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Enter the group invite code provided by your Susu group organizer to join and start contributing.",
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onJoinGroup,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("member_join_group_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = SusuEmeraldPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Enter Group Code", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    bgColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = tint, modifier = Modifier.size(20.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
    }
}
