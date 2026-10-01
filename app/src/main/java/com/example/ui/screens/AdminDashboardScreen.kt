package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AuditLog
import com.example.data.model.Payment
import com.example.data.model.SusuGroup
import com.example.data.model.User
import com.example.data.remote.MobileMoneyGateway
import com.example.ui.components.CurrencyText
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminDashboardScreen(
    users: List<User>,
    groups: List<SusuGroup>,
    payments: List<Payment>,
    auditLogs: List<AuditLog>,
    onRegisterUser: () -> Unit,
    onApproveUser: (userId: Long) -> Unit = {},
    onSuspendUser: (userId: Long) -> Unit = {},
    onDeleteUser: (userId: Long) -> Unit = {},
    onCreateGroup: () -> Unit = {},
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var adminViewTab by remember { mutableIntStateOf(0) } // 0: Overview & Audit, 1: Users Management, 2: Groups Oversight, 3: MoMo APIs
    val coroutineScope = rememberCoroutineScope()

    val totalVolume = payments.filter { it.status == "VERIFIED" }.sumOf { it.amount }
    val pendingVerifications = payments.count { it.status == "SUBMITTED" }
    val pendingUserApprovals = users.count { it.status == "PENDING_APPROVAL" }

    val filteredLogs = auditLogs.filter { log ->
        val matchesFilter = when (selectedFilter) {
            "PAYMENTS" -> log.entityType == "PAYMENT"
            "PAYOUTS" -> log.entityType == "PAYOUT"
            "GROUPS" -> log.entityType == "GROUP"
            "USERS" -> log.entityType == "USER"
            else -> true
        }
        val matchesSearch = searchQuery.isBlank() ||
                log.description.contains(searchQuery, ignoreCase = true) ||
                log.userName.contains(searchQuery, ignoreCase = true)
        matchesFilter && matchesSearch
    }

    val dateFormat = SimpleDateFormat("MMM dd, hh:mm a", Locale.US)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("admin_dashboard_scroll"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Admin Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("admin_header_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SusuSurfaceDark)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(shape = RoundedCornerShape(6.dp), color = SusuGold) {
                            Text(
                                text = "ADMINISTRATOR PORTAL",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }

                        IconButton(
                            onClick = onLogout,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.PowerSettingsNew, contentDescription = "Log Out", tint = Color.White)
                        }
                    }

                    Text("Digital Susu System Monitor", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                    Text("Central administrative oversight of users, groups, approvals, and immutable audit logs.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                }
            }
        }

        // Metrics Grid (SRS FR-021)
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AdminMetricCard(
                    title = "Total Users",
                    value = users.size.toString(),
                    icon = Icons.Default.People,
                    modifier = Modifier.weight(1f)
                )
                AdminMetricCard(
                    title = "Susu Groups",
                    value = groups.size.toString(),
                    icon = Icons.Default.Groups,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AdminMetricCard(
                    title = "Platform Volume",
                    value = "L$ ${totalVolume.toInt()}",
                    icon = Icons.Default.AccountBalanceWallet,
                    modifier = Modifier.weight(1f),
                    color = SusuEmeraldPrimary
                )
                AdminMetricCard(
                    title = "Pending Approvals",
                    value = "$pendingUserApprovals Users",
                    icon = Icons.Default.PendingActions,
                    modifier = Modifier.weight(1f),
                    color = if (pendingUserApprovals > 0) SusuGoldDark else Color.Gray
                )
            }
        }

        // Section Tabs
        item {
            TabRow(
                selectedTabIndex = adminViewTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = SusuEmeraldPrimary,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = adminViewTab == 0,
                    onClick = { adminViewTab = 0 },
                    text = { Text("Audit Trail") }
                )
                Tab(
                    selected = adminViewTab == 1,
                    onClick = { adminViewTab = 1 },
                    text = {
                        BadgedBox(
                            badge = {
                                if (pendingUserApprovals > 0) {
                                    Badge { Text(pendingUserApprovals.toString()) }
                                }
                            }
                        ) {
                            Text("Users (${users.size})")
                        }
                    }
                )
                Tab(
                    selected = adminViewTab == 2,
                    onClick = { adminViewTab = 2 },
                    text = { Text("Groups (${groups.size})") }
                )
                Tab(
                    selected = adminViewTab == 3,
                    onClick = { adminViewTab = 3 },
                    text = { Text("MoMo APIs") }
                )
            }
        }

        when (adminViewTab) {
            0 -> {
                // Audit Logs Section (SRS FR-024)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("System Audit Trail", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("${filteredLogs.size} Events", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        // Filter chips
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(listOf("ALL", "USERS", "PAYMENTS", "PAYOUTS", "GROUPS")) { filter ->
                                FilterChip(
                                    selected = selectedFilter == filter,
                                    onClick = { selectedFilter = filter },
                                    label = { Text(filter) }
                                )
                            }
                        }
                    }
                }

                if (filteredLogs.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No audit events recorded for this category", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                items(filteredLogs) { log ->
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("audit_item_${log.id}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (log.action) {
                                            "PAYMENT_VERIFIED", "PAYOUT_COMPLETED", "MEMBER_APPROVED" -> StatusPaidContainer
                                            "PAYMENT_REJECTED", "MEMBER_REJECTED" -> StatusRejectedContainer
                                            "PAYMENT_SUBMITTED" -> StatusPendingContainer
                                            else -> SusuEmeraldContainer
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (log.action) {
                                        "PAYMENT_VERIFIED", "PAYOUT_COMPLETED", "MEMBER_APPROVED" -> Icons.Default.Check
                                        "PAYMENT_REJECTED", "MEMBER_REJECTED" -> Icons.Default.Close
                                        "PAYMENT_SUBMITTED" -> Icons.Default.Payment
                                        else -> Icons.Default.History
                                    },
                                    contentDescription = null,
                                    tint = when (log.action) {
                                        "PAYMENT_VERIFIED", "PAYOUT_COMPLETED", "MEMBER_APPROVED" -> Color(0xFF047857)
                                        "PAYMENT_REJECTED", "MEMBER_REJECTED" -> StatusRejectedRed
                                        "PAYMENT_SUBMITTED" -> SusuGoldDark
                                        else -> SusuEmeraldPrimary
                                    },
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(
                                        text = log.action.replace("_", " "),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = SusuEmeraldPrimary
                                    )
                                    Text(
                                        text = dateFormat.format(Date(log.createdAt)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Text(
                                    text = log.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Text(
                                    text = "Actor: ${log.userName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            1 -> {
                // Users Section
                val pendingList = users.filter { it.status == "PENDING_APPROVAL" }
                if (pendingList.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("pending_approvals_banner"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SusuGoldContainer)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Default.HourglassTop, contentDescription = null, tint = SusuGoldDark)
                                        Text(
                                            text = "Pending Account Approvals (${pendingList.size})",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = SusuOnGoldContainer
                                        )
                                    }
                                    Surface(shape = RoundedCornerShape(12.dp), color = SusuGoldDark) {
                                        Text(
                                            text = "ACTION REQUIRED",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }

                                Text(
                                    text = "New members and organizers who registered must be accepted by Prince Gbolon before they can log in.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SusuOnGoldContainer
                                )
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("System User Directory", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Button(
                            onClick = onRegisterUser,
                            colors = ButtonDefaults.buttonColors(containerColor = SusuEmeraldPrimary)
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Create User")
                        }
                    }
                }

                val sortedUsers = users.sortedByDescending { it.status == "PENDING_APPROVAL" }
                items(sortedUsers) { user ->
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("user_item_${user.id}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (user.role) {
                                                "ADMIN" -> SusuGoldContainer
                                                "ORGANIZER" -> SusuEmeraldContainer
                                                else -> MaterialTheme.colorScheme.surfaceVariant
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = user.fullName.take(1),
                                        fontWeight = FontWeight.Bold,
                                        color = when (user.role) {
                                            "ADMIN" -> SusuOnGoldContainer
                                            "ORGANIZER" -> SusuOnEmeraldContainer
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(user.fullName, fontWeight = FontWeight.Bold)
                                    Text("${user.phone}${if (user.email.isNotBlank()) " • ${user.email}" else ""}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = when (user.role) {
                                            "ADMIN" -> SusuGoldContainer
                                            "ORGANIZER" -> SusuEmeraldContainer
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    ) {
                                        Text(
                                            text = user.role,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = when (user.role) {
                                                "ADMIN" -> SusuOnGoldContainer
                                                "ORGANIZER" -> SusuOnEmeraldContainer
                                                else -> MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    }

                                    StatusBadge(status = user.status)
                                }
                            }

                            // User Management Actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (user.status == "PENDING_APPROVAL") {
                                    Button(
                                        onClick = { onApproveUser(user.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = SusuEmeraldPrimary),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Approve Account ✓")
                                    }
                                }

                                if (user.role != "ADMIN") {
                                    OutlinedButton(
                                        onClick = { onSuspendUser(user.id) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(if (user.status == "SUSPENDED") "Activate" else "Suspend")
                                    }

                                    IconButton(onClick = { onDeleteUser(user.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete User", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // Groups Section
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Susu Groups Oversight", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Button(
                            onClick = onCreateGroup,
                            colors = ButtonDefaults.buttonColors(containerColor = SusuEmeraldPrimary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Group")
                        }
                    }
                }

                if (groups.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No Susu groups created yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                items(groups) { group ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(group.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Surface(shape = RoundedCornerShape(8.dp), color = SusuGoldContainer) {
                                    Text(
                                        text = group.groupCode,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = SusuOnGoldContainer
                                    )
                                }
                            }

                            Text(
                                text = group.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            HorizontalDivider()

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Contribution Rate:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("L$ ${group.contributionAmount.toInt()} (${group.frequency})", fontWeight = FontWeight.SemiBold)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Current Active Cycle:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Cycle ${group.currentCycle} of ${group.numberOfCycles}", fontWeight = FontWeight.SemiBold)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Group Status:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                StatusBadge(status = group.status)
                            }
                        }
                    }
                }
            }
            3 -> {
                // Mobile Money API Integrations (MTN & Orange)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Mobile Money API Gateways",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SusuEmeraldContainer
                            ) {
                                Text(
                                    "Liberia Open APIs",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SusuEmeraldPrimary
                                )
                            }
                        }

                        // MTN MoMo Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDE7)),
                            border = BorderStroke(1.dp, Color(0xFFFBC02D))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("🟡", fontSize = 20.sp)
                                        Column {
                                            Text("Lonestar Cell MTN MoMo API", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                            Text("momodeveloper.mtn.com", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (MobileMoneyGateway.isMtnConfigured()) SusuEmeraldContainer else Color(0xFFE0E0E0)
                                    ) {
                                        Text(
                                            if (MobileMoneyGateway.isMtnConfigured()) "PRODUCTION ACTIVE" else "SANDBOX READY",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (MobileMoneyGateway.isMtnConfigured()) SusuEmeraldPrimary else Color.DarkGray
                                        )
                                    }
                                }

                                Text(
                                    "Supports Collections (RequestToPay USSD Push prompt to member phones) and Disbursements (instant cashout to member wallets).",
                                    style = MaterialTheme.typography.bodySmall
                                )

                                HorizontalDivider()

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Protocol:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    Text("OAuth 2.0 REST / JSON", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Target Currency:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    Text("LRD (Liberian Dollars) / USD", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                                }

                                var mtnTestStatus by remember { mutableStateOf<String?>(null) }
                                var isTestingMtn by remember { mutableStateOf(false) }

                                if (mtnTestStatus != null) {
                                    Surface(shape = RoundedCornerShape(8.dp), color = Color.White) {
                                        Text(
                                            text = mtnTestStatus ?: "",
                                            modifier = Modifier.padding(8.dp),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SusuEmeraldPrimary
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        isTestingMtn = true
                                        mtnTestStatus = "Connecting to MTN Sandbox Open API..."
                                        coroutineScope.launch {
                                            val res = MobileMoneyGateway.processMtnMoMoPayment("0880123456", 500.0)
                                            isTestingMtn = false
                                            mtnTestStatus = "MTN MoMo Success: ${res.transactionReference} (L$500 USSD prompt authorized)"
                                        }
                                    },
                                    enabled = !isTestingMtn,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFBC02D), contentColor = Color.Black),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    if (isTestingMtn) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Testing MoMo API...")
                                    } else {
                                        Text("⚡ Test MTN USSD Push (L$ 500 Simulation)", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Orange Money Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                            border = BorderStroke(1.dp, Color(0xFFFF9800))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("🟠", fontSize = 20.sp)
                                        Column {
                                            Text("Orange Money Liberia API", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                            Text("developer.orange.com", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (MobileMoneyGateway.isOrangeConfigured()) SusuEmeraldContainer else Color(0xFFE0E0E0)
                                    ) {
                                        Text(
                                            if (MobileMoneyGateway.isOrangeConfigured()) "PRODUCTION ACTIVE" else "SANDBOX READY",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (MobileMoneyGateway.isOrangeConfigured()) SusuEmeraldPrimary else Color.DarkGray
                                        )
                                    }
                                }

                                Text(
                                    "Supports Orange Money Web Payment and M-Payment in Liberia with automated OTP validation and instant confirmation.",
                                    style = MaterialTheme.typography.bodySmall
                                )

                                HorizontalDivider()

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Country Code / Market:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    Text("LR (Liberia)", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Currency:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    Text("LRD / USD", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                                }

                                var orangeTestStatus by remember { mutableStateOf<String?>(null) }
                                var isTestingOrange by remember { mutableStateOf(false) }

                                if (orangeTestStatus != null) {
                                    Surface(shape = RoundedCornerShape(8.dp), color = Color.White) {
                                        Text(
                                            text = orangeTestStatus ?: "",
                                            modifier = Modifier.padding(8.dp),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SusuEmeraldPrimary
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        isTestingOrange = true
                                        orangeTestStatus = "Connecting to Orange Money Developer API..."
                                        coroutineScope.launch {
                                            val res = MobileMoneyGateway.processOrangeMoneyPayment("0770123456", 500.0)
                                            isTestingOrange = false
                                            orangeTestStatus = "Orange Success: ${res.transactionReference} (L$500 Mobile Payment validated)"
                                        }
                                    },
                                    enabled = !isTestingOrange,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57C00), contentColor = Color.White),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    if (isTestingOrange) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Testing Orange API...")
                                    } else {
                                        Text("⚡ Test Orange Money Prompt (L$ 500 Simulation)", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Production Activation Instructions
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("🚀 How to Go Live in Liberia", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                Text("1. Register a merchant agreement with Lonestar Cell MTN & Orange Liberia.", style = MaterialTheme.typography.bodySmall)
                                Text("2. Create developer accounts at momodeveloper.mtn.com and developer.orange.com.", style = MaterialTheme.typography.bodySmall)
                                Text("3. Enter your Production Primary Key and Client Secrets in the AI Studio Secrets panel.", style = MaterialTheme.typography.bodySmall)
                                Text("4. The app automatically detects your live credentials and switches from Sandbox to Live processing!", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = SusuEmeraldPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminMetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        }
    }
}
