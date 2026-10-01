package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.SusuViewModel
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    private val viewModel: SusuViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val snackbarHostState = remember { SnackbarHostState() }

                // State from ViewModel
                val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
                val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
                val allGroups by viewModel.allGroups.collectAsStateWithLifecycle()
                val selectedGroup by viewModel.selectedGroup.collectAsStateWithLifecycle()
                val selectedCycleNumber by viewModel.selectedCycleNumber.collectAsStateWithLifecycle()
                val cycles by viewModel.groupCycles.collectAsStateWithLifecycle()
                val membersWithDetails by viewModel.membersWithDetails.collectAsStateWithLifecycle()
                val cycleSummary by viewModel.cycleSummary.collectAsStateWithLifecycle()
                val pendingPayments by viewModel.pendingPayments.collectAsStateWithLifecycle()
                val pendingUsers by viewModel.pendingUsers.collectAsStateWithLifecycle()
                val groupPayouts by viewModel.groupPayouts.collectAsStateWithLifecycle()
                val allReceipts by viewModel.allReceipts.collectAsStateWithLifecycle()
                val userReceipts by viewModel.userReceipts.collectAsStateWithLifecycle()
                val userNotifications by viewModel.userNotifications.collectAsStateWithLifecycle()
                val auditLogs by viewModel.allAuditLogs.collectAsStateWithLifecycle()
                val allPayments by viewModel.allPayments.collectAsStateWithLifecycle()
                val roomMembers by viewModel.membersList.collectAsStateWithLifecycle()

                // Dialogs State
                var showEditProfileDialog by remember { mutableStateOf(false) }
                var showRegisterDialog by remember { mutableStateOf(false) }
                var showCreateGroupDialog by remember { mutableStateOf(false) }
                var showJoinGroupDialog by remember { mutableStateOf(false) }
                var showGroupDropdown by remember { mutableStateOf(false) }
                var showHowItWorksDialog by remember { mutableStateOf(false) }

                // Navigation State per active role
                var currentNavigationTab by remember { mutableIntStateOf(0) }

                // Reset navigation tab on role switch
                LaunchedEffect(currentUser?.role) {
                    currentNavigationTab = 0
                }

                // Listen to UI feedback events
                LaunchedEffect(Unit) {
                    viewModel.uiFeedback.collectLatest { feedback ->
                        snackbarHostState.showSnackbar(
                            message = feedback.message,
                            duration = SnackbarDuration.Short
                        )
                    }
                }

                // Global Dialogs
                if (showHowItWorksDialog) {
                    HowItWorksDialog(onDismiss = { showHowItWorksDialog = false })
                }

                if (showEditProfileDialog && currentUser != null) {
                    EditProfileDialog(
                        currentUser = currentUser!!,
                        onDismiss = { showEditProfileDialog = false },
                        onSaveProfile = { name, phone, email, newPassword ->
                            viewModel.updateProfile(name, phone, email, newPassword)
                        },
                        onLogout = {
                            showEditProfileDialog = false
                            viewModel.logout()
                        }
                    )
                }

                if (showRegisterDialog) {
                    RegisterUserDialog(
                        onDismiss = { showRegisterDialog = false },
                        onRegister = { name, phone, email, role ->
                            viewModel.registerUser(name, phone, email, role) {
                                showRegisterDialog = false
                            }
                        }
                    )
                }

                if (showCreateGroupDialog) {
                    CreateGroupDialog(
                        onDismiss = { showCreateGroupDialog = false },
                        onCreate = { name, code, desc, amt, freq, cyc ->
                            viewModel.createGroup(name, code, desc, amt, freq, cyc) {
                                showCreateGroupDialog = false
                            }
                        }
                    )
                }

                if (showJoinGroupDialog) {
                    JoinGroupDialog(
                        onDismiss = { showJoinGroupDialog = false },
                        onJoin = { code ->
                            viewModel.joinGroup(code) {
                                showJoinGroupDialog = false
                            }
                        }
                    )
                }

                // --- FIRST INTERFACE: AUTHENTICATION GATEWAY (LOGIN / REGISTER) ---
                if (currentUser == null) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        snackbarHost = { SnackbarHost(snackbarHostState) }
                    ) { authPadding ->
                        AuthScreen(
                            onLogin = { identifier, password ->
                                viewModel.login(identifier, password)
                            },
                            onRegister = { fullName, phone, email, role, password ->
                                viewModel.registerUser(fullName, phone, email, role, password)
                            },
                            modifier = Modifier.padding(authPadding)
                        )
                    }
                } else {
                    val activeUser = currentUser!!
                    val unreadNotifs = userNotifications.count { !it.isRead }

                    when (activeUser.role) {
                        // =========================================================================
                        // 1. SYSTEM ADMINISTRATOR PORTAL APP
                        // =========================================================================
                        "ADMIN" -> {
                            Scaffold(
                                modifier = Modifier.fillMaxSize(),
                                snackbarHost = { SnackbarHost(snackbarHostState) },
                                topBar = {
                                    TopAppBar(
                                        title = {
                                            Column {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = "🇱🇷 Susu Admin",
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.titleMedium
                                                    )
                                                    Surface(
                                                        shape = RoundedCornerShape(12.dp),
                                                        color = SusuGoldContainer
                                                    ) {
                                                        Text(
                                                            text = "ADMIN",
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = SusuOnGoldContainer
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = "Central Registry & System Audit",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        actions = {
                                            // Register user as admin
                                            IconButton(
                                                onClick = { showRegisterDialog = true },
                                                modifier = Modifier.testTag("admin_add_user_btn")
                                            ) {
                                                Icon(Icons.Default.PersonAdd, contentDescription = "Add User")
                                            }

                                            // Create Susu group as admin
                                            IconButton(
                                                onClick = { showCreateGroupDialog = true },
                                                modifier = Modifier.testTag("admin_add_group_btn")
                                            ) {
                                                Icon(Icons.Default.AddBusiness, contentDescription = "New Group")
                                            }

                                            // Admin Profile
                                            IconButton(
                                                onClick = { showEditProfileDialog = true },
                                                modifier = Modifier.testTag("admin_profile_btn")
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .clip(CircleShape)
                                                        .background(SusuGold),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = activeUser.fullName.take(1),
                                                        color = Color.Black,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }

                                            // Sign Out
                                            IconButton(
                                                onClick = { viewModel.logout() },
                                                modifier = Modifier.testTag("admin_logout_btn")
                                            ) {
                                                Icon(
                                                    Icons.Default.Logout,
                                                    contentDescription = "Sign Out",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        },
                                        colors = TopAppBarDefaults.topAppBarColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                        )
                                    )
                                },
                                bottomBar = {
                                    NavigationBar(
                                        modifier = Modifier
                                            .testTag("admin_bottom_nav")
                                            .windowInsetsPadding(WindowInsets.navigationBars),
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ) {
                                        NavigationBarItem(
                                            selected = currentNavigationTab == 0,
                                            onClick = { currentNavigationTab = 0 },
                                            icon = {
                                                BadgedBox(
                                                    badge = {
                                                        if (pendingUsers.isNotEmpty()) {
                                                            Badge { Text(pendingUsers.size.toString()) }
                                                        }
                                                    }
                                                ) {
                                                    Icon(
                                                        if (currentNavigationTab == 0) Icons.Filled.AdminPanelSettings else Icons.Outlined.AdminPanelSettings,
                                                        contentDescription = "Audit & Users"
                                                    )
                                                }
                                            },
                                            label = { Text("Audit & Users") },
                                            modifier = Modifier.testTag("nav_admin_audit")
                                        )

                                        NavigationBarItem(
                                            selected = currentNavigationTab == 1,
                                            onClick = { currentNavigationTab = 1 },
                                            icon = {
                                                Icon(
                                                    if (currentNavigationTab == 1) Icons.Filled.Assessment else Icons.Outlined.Assessment,
                                                    contentDescription = "Reports"
                                                )
                                            },
                                            label = { Text("Reports") },
                                            modifier = Modifier.testTag("nav_admin_reports")
                                        )

                                        NavigationBarItem(
                                            selected = currentNavigationTab == 2,
                                            onClick = { currentNavigationTab = 2 },
                                            icon = {
                                                Icon(
                                                    if (currentNavigationTab == 2) Icons.Filled.ReceiptLong else Icons.Outlined.ReceiptLong,
                                                    contentDescription = "Receipts"
                                                )
                                            },
                                            label = { Text("Receipts") },
                                            modifier = Modifier.testTag("nav_admin_receipts")
                                        )
                                    }
                                }
                            ) { innerPadding ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding)
                                ) {
                                    when (currentNavigationTab) {
                                        0 -> AdminDashboardScreen(
                                            users = allUsers,
                                            groups = allGroups,
                                            payments = allPayments,
                                            auditLogs = auditLogs,
                                            onRegisterUser = { showRegisterDialog = true },
                                            onApproveUser = { id -> viewModel.approveUser(id) },
                                            onSuspendUser = { id -> viewModel.suspendUser(id) },
                                            onDeleteUser = { id -> viewModel.deleteUser(id) },
                                            onCreateGroup = { showCreateGroupDialog = true },
                                            onLogout = { viewModel.logout() }
                                        )

                                        1 -> ReportsScreen(
                                            group = selectedGroup,
                                            members = membersWithDetails,
                                            summary = cycleSummary,
                                            payouts = groupPayouts,
                                            payments = allPayments,
                                            allUsers = allUsers
                                        )

                                        2 -> ReceiptsScreen(
                                            receipts = allReceipts
                                        )
                                    }
                                }
                            }
                        }

                        // =========================================================================
                        // 2. SUSU ORGANIZER APP
                        // =========================================================================
                        "ORGANIZER" -> {
                            Scaffold(
                                modifier = Modifier.fillMaxSize(),
                                snackbarHost = { SnackbarHost(snackbarHostState) },
                                topBar = {
                                    TopAppBar(
                                        navigationIcon = {
                                            if (currentNavigationTab == 5) {
                                                IconButton(onClick = { currentNavigationTab = 0 }) {
                                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                                                }
                                            }
                                        },
                                        title = {
                                            if (currentNavigationTab == 5) {
                                                Text("Organizer Notifications", fontWeight = FontWeight.Bold)
                                            } else {
                                                Column {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text(
                                                            text = "🇱🇷 Organizer App",
                                                            fontWeight = FontWeight.Bold,
                                                            style = MaterialTheme.typography.titleMedium
                                                        )
                                                        Surface(
                                                            shape = RoundedCornerShape(12.dp),
                                                            color = SusuEmeraldContainer
                                                        ) {
                                                            Text(
                                                                text = "ORGANIZER",
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = SusuOnEmeraldContainer
                                                            )
                                                        }
                                                    }

                                                    // Circle selector dropdown
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.clickable { showGroupDropdown = true }
                                                    ) {
                                                        Text(
                                                            text = selectedGroup?.name ?: "Select Circle",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        Icon(
                                                            imageVector = Icons.Default.ArrowDropDown,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.size(16.dp)
                                                        )

                                                        DropdownMenu(
                                                            expanded = showGroupDropdown,
                                                            onDismissRequest = { showGroupDropdown = false }
                                                        ) {
                                                            allGroups.forEach { g ->
                                                                DropdownMenuItem(
                                                                    text = { Text("${g.name} (${g.groupCode})") },
                                                                    onClick = {
                                                                        viewModel.selectGroup(g)
                                                                        showGroupDropdown = false
                                                                    }
                                                                )
                                                            }
                                                            HorizontalDivider()
                                                            DropdownMenuItem(
                                                                text = { Text("+ Create New Susu Group") },
                                                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                                                                onClick = {
                                                                    showGroupDropdown = false
                                                                    showCreateGroupDialog = true
                                                                }
                                                            )
                                                            DropdownMenuItem(
                                                                text = { Text("Join Group with Code") },
                                                                leadingIcon = { Icon(Icons.Default.GroupAdd, contentDescription = null) },
                                                                onClick = {
                                                                    showGroupDropdown = false
                                                                    showJoinGroupDialog = true
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        actions = {
                                            IconButton(onClick = { showHowItWorksDialog = true }) {
                                                Icon(
                                                    Icons.Default.HelpOutline,
                                                    contentDescription = "How it Works",
                                                    tint = SusuEmeraldPrimary
                                                )
                                            }

                                            IconButton(onClick = { currentNavigationTab = 5 }) {
                                                BadgedBox(
                                                    badge = {
                                                        if (unreadNotifs > 0) {
                                                            Badge { Text(unreadNotifs.toString()) }
                                                        }
                                                    }
                                                ) {
                                                    Icon(
                                                        if (unreadNotifs > 0) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                                                        contentDescription = "Notifications"
                                                    )
                                                }
                                            }

                                            IconButton(onClick = { showEditProfileDialog = true }) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .clip(CircleShape)
                                                        .background(SusuEmeraldPrimary),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = activeUser.fullName.take(1),
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }

                                            IconButton(onClick = { viewModel.logout() }) {
                                                Icon(
                                                    Icons.Default.Logout,
                                                    contentDescription = "Sign Out",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        },
                                        colors = TopAppBarDefaults.topAppBarColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                        )
                                    )
                                },
                                bottomBar = {
                                    if (currentNavigationTab != 5) {
                                        NavigationBar(
                                            modifier = Modifier
                                                .testTag("organizer_bottom_nav")
                                                .windowInsetsPadding(WindowInsets.navigationBars),
                                            containerColor = MaterialTheme.colorScheme.surface
                                        ) {
                                            // 0. Dashboard
                                            NavigationBarItem(
                                                selected = currentNavigationTab == 0,
                                                onClick = { currentNavigationTab = 0 },
                                                icon = {
                                                    val pendingBadgeCount = pendingPayments.size + pendingUsers.size
                                                    BadgedBox(
                                                        badge = {
                                                            if (pendingBadgeCount > 0) {
                                                                Badge { Text(pendingBadgeCount.toString()) }
                                                            }
                                                        }
                                                    ) {
                                                        Icon(
                                                            if (currentNavigationTab == 0) Icons.Filled.AccountBalanceWallet else Icons.Outlined.AccountBalanceWallet,
                                                            contentDescription = "Dashboard"
                                                        )
                                                    }
                                                },
                                                label = { Text("Dashboard") },
                                                modifier = Modifier.testTag("nav_organizer_dashboard")
                                            )

                                            // 1. Members
                                            NavigationBarItem(
                                                selected = currentNavigationTab == 1,
                                                onClick = { currentNavigationTab = 1 },
                                                icon = {
                                                    BadgedBox(
                                                        badge = {
                                                            if (roomMembers.isNotEmpty()) {
                                                                Badge { Text(roomMembers.size.toString()) }
                                                            }
                                                        }
                                                    ) {
                                                        Icon(
                                                            if (currentNavigationTab == 1) Icons.Filled.People else Icons.Outlined.People,
                                                            contentDescription = "Members"
                                                        )
                                                    }
                                                },
                                                label = { Text("Members") },
                                                modifier = Modifier.testTag("nav_organizer_members")
                                            )

                                            // 2. Turns
                                            NavigationBarItem(
                                                selected = currentNavigationTab == 2,
                                                onClick = { currentNavigationTab = 2 },
                                                icon = {
                                                    Icon(
                                                        if (currentNavigationTab == 2) Icons.Filled.Repeat else Icons.Outlined.Repeat,
                                                        contentDescription = "Turns"
                                                    )
                                                },
                                                label = { Text("Turns") },
                                                modifier = Modifier.testTag("nav_organizer_turns")
                                            )

                                            // 3. Receipts
                                            NavigationBarItem(
                                                selected = currentNavigationTab == 3,
                                                onClick = { currentNavigationTab = 3 },
                                                icon = {
                                                    Icon(
                                                        if (currentNavigationTab == 3) Icons.Filled.ReceiptLong else Icons.Outlined.ReceiptLong,
                                                        contentDescription = "Receipts"
                                                    )
                                                },
                                                label = { Text("Receipts") },
                                                modifier = Modifier.testTag("nav_organizer_receipts")
                                            )

                                            // 4. Reports
                                            NavigationBarItem(
                                                selected = currentNavigationTab == 4,
                                                onClick = { currentNavigationTab = 4 },
                                                icon = {
                                                    Icon(
                                                        if (currentNavigationTab == 4) Icons.Filled.Assessment else Icons.Outlined.Assessment,
                                                        contentDescription = "Reports"
                                                    )
                                                },
                                                label = { Text("Reports") },
                                                modifier = Modifier.testTag("nav_organizer_reports")
                                            )
                                        }
                                    }
                                }
                            ) { innerPadding ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding)
                                ) {
                                    when (currentNavigationTab) {
                                        0 -> OrganizerDashboardScreen(
                                            currentUser = activeUser,
                                            group = selectedGroup,
                                            cycles = cycles,
                                            selectedCycleNumber = selectedCycleNumber,
                                            members = membersWithDetails,
                                            summary = cycleSummary,
                                            pendingPayments = pendingPayments,
                                            onSelectCycle = { cycleNum -> viewModel.selectCycleNumber(cycleNum) },
                                            onVerifyPayment = { id, isAppr, reason -> viewModel.verifyPayment(id, isAppr, reason) },
                                            onCompletePayout = { ref, notes -> viewModel.completePayout(ref, notes) },
                                            onRecordMemberPayment = { memberUserId ->
                                                selectedGroup?.let { g ->
                                                    viewModel.submitPayment(
                                                        amount = g.contributionAmount,
                                                        paymentMethod = "Cash Deposit",
                                                        transactionRef = "CSH-WP-${(1000..9999).random()}",
                                                        note = "Cash collected in person by organizer"
                                                    )
                                                }
                                            },
                                            onChangeMemberPosition = { mId, pos -> viewModel.updateMemberPosition(mId, pos) },
                                            onCreateGroup = { showCreateGroupDialog = true },
                                            onCreateCycle = { cNum, days, st -> viewModel.createCycle(cNum, days, st) },
                                            pendingUsers = pendingUsers,
                                            onApproveUser = { id -> viewModel.approveUser(id) },
                                            onRejectUser = { id, rsn -> viewModel.rejectUser(id, rsn) }
                                        )

                                        1 -> GroupMembersScreen(
                                            group = selectedGroup,
                                            members = roomMembers,
                                            onAddMember = { name, phone, email, pos, st, rl, notes, em ->
                                                viewModel.addGroupMember(name, phone, email, pos, st, rl, em, notes)
                                            },
                                            onUpdateMember = { member -> viewModel.updateGroupMember(member) },
                                            onDeleteMember = { memberId -> viewModel.deleteGroupMember(memberId) }
                                        )

                                        2 -> TurnsScreen(
                                            group = selectedGroup,
                                            members = membersWithDetails,
                                            currentCycle = selectedGroup?.currentCycle ?: 1,
                                            currentUserId = activeUser.id
                                        )

                                        3 -> ReceiptsScreen(
                                            receipts = allReceipts
                                        )

                                        4 -> ReportsScreen(
                                            group = selectedGroup,
                                            members = membersWithDetails,
                                            summary = cycleSummary,
                                            payouts = groupPayouts,
                                            payments = allPayments,
                                            allUsers = allUsers
                                        )

                                        5 -> NotificationsScreen(
                                            notifications = userNotifications,
                                            onMarkAsRead = { id -> viewModel.markNotificationRead(id) },
                                            onMarkAllAsRead = { viewModel.markAllNotificationsRead() }
                                        )
                                    }
                                }
                            }
                        }

                        // =========================================================================
                        // 3. SUSU GROUP MEMBER APP
                        // =========================================================================
                        else -> {
                            Scaffold(
                                modifier = Modifier.fillMaxSize(),
                                snackbarHost = { SnackbarHost(snackbarHostState) },
                                topBar = {
                                    TopAppBar(
                                        navigationIcon = {
                                            if (currentNavigationTab == 3) {
                                                IconButton(onClick = { currentNavigationTab = 0 }) {
                                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                                                }
                                            }
                                        },
                                        title = {
                                            if (currentNavigationTab == 3) {
                                                Text("My Notifications", fontWeight = FontWeight.Bold)
                                            } else {
                                                Column {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text(
                                                            text = "🇱🇷 Member App",
                                                            fontWeight = FontWeight.Bold,
                                                            style = MaterialTheme.typography.titleMedium
                                                        )
                                                        Surface(
                                                            shape = RoundedCornerShape(12.dp),
                                                            color = MaterialTheme.colorScheme.surfaceVariant
                                                        ) {
                                                            Text(
                                                                text = "MEMBER",
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                        }
                                                    }

                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.clickable { showGroupDropdown = true }
                                                    ) {
                                                        Text(
                                                            text = selectedGroup?.name ?: "No Circle Joined",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        Icon(
                                                            imageVector = Icons.Default.ArrowDropDown,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.size(16.dp)
                                                        )

                                                        DropdownMenu(
                                                            expanded = showGroupDropdown,
                                                            onDismissRequest = { showGroupDropdown = false }
                                                        ) {
                                                            allGroups.forEach { g ->
                                                                DropdownMenuItem(
                                                                    text = { Text("${g.name} (${g.groupCode})") },
                                                                    onClick = {
                                                                        viewModel.selectGroup(g)
                                                                        showGroupDropdown = false
                                                                    }
                                                                )
                                                            }
                                                            HorizontalDivider()
                                                            DropdownMenuItem(
                                                                text = { Text("Join Group with Code") },
                                                                leadingIcon = { Icon(Icons.Default.GroupAdd, contentDescription = null) },
                                                                onClick = {
                                                                    showGroupDropdown = false
                                                                    showJoinGroupDialog = true
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                        actions = {
                                            IconButton(onClick = { showHowItWorksDialog = true }) {
                                                Icon(
                                                    Icons.Default.HelpOutline,
                                                    contentDescription = "How it Works",
                                                    tint = SusuEmeraldPrimary
                                                )
                                            }

                                            IconButton(onClick = { currentNavigationTab = 3 }) {
                                                BadgedBox(
                                                    badge = {
                                                        if (unreadNotifs > 0) {
                                                            Badge { Text(unreadNotifs.toString()) }
                                                        }
                                                    }
                                                ) {
                                                    Icon(
                                                        if (unreadNotifs > 0) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                                                        contentDescription = "Notifications"
                                                    )
                                                }
                                            }

                                            IconButton(onClick = { showEditProfileDialog = true }) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .clip(CircleShape)
                                                        .background(SusuEmeraldPrimary),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = activeUser.fullName.take(1),
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }

                                            IconButton(onClick = { viewModel.logout() }) {
                                                Icon(
                                                    Icons.Default.Logout,
                                                    contentDescription = "Sign Out",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        },
                                        colors = TopAppBarDefaults.topAppBarColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                        )
                                    )
                                },
                                bottomBar = {
                                    if (currentNavigationTab != 3) {
                                        NavigationBar(
                                            modifier = Modifier
                                                .testTag("member_bottom_nav")
                                                .windowInsetsPadding(WindowInsets.navigationBars),
                                            containerColor = MaterialTheme.colorScheme.surface
                                        ) {
                                            NavigationBarItem(
                                                selected = currentNavigationTab == 0,
                                                onClick = { currentNavigationTab = 0 },
                                                icon = {
                                                    Icon(
                                                        if (currentNavigationTab == 0) Icons.Filled.AccountBalanceWallet else Icons.Outlined.AccountBalanceWallet,
                                                        contentDescription = "Home"
                                                    )
                                                },
                                                label = { Text("Home") },
                                                modifier = Modifier.testTag("nav_member_home")
                                            )

                                            NavigationBarItem(
                                                selected = currentNavigationTab == 1,
                                                onClick = { currentNavigationTab = 1 },
                                                icon = {
                                                    Icon(
                                                        if (currentNavigationTab == 1) Icons.Filled.Repeat else Icons.Outlined.Repeat,
                                                        contentDescription = "Turns"
                                                    )
                                                },
                                                label = { Text("Turns") },
                                                modifier = Modifier.testTag("nav_member_turns")
                                            )

                                            NavigationBarItem(
                                                selected = currentNavigationTab == 2,
                                                onClick = { currentNavigationTab = 2 },
                                                icon = {
                                                    Icon(
                                                        if (currentNavigationTab == 2) Icons.Filled.ReceiptLong else Icons.Outlined.ReceiptLong,
                                                        contentDescription = "Receipts"
                                                    )
                                                },
                                                label = { Text("Receipts") },
                                                modifier = Modifier.testTag("nav_member_receipts")
                                            )

                                            NavigationBarItem(
                                                selected = currentNavigationTab == 3,
                                                onClick = { currentNavigationTab = 3 },
                                                icon = {
                                                    BadgedBox(
                                                        badge = {
                                                            if (unreadNotifs > 0) {
                                                                Badge { Text(unreadNotifs.toString()) }
                                                            }
                                                        }
                                                    ) {
                                                        Icon(
                                                            if (currentNavigationTab == 3) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                                                            contentDescription = "Alerts"
                                                        )
                                                    }
                                                },
                                                label = { Text("Alerts") },
                                                modifier = Modifier.testTag("nav_member_alerts")
                                            )
                                        }
                                    }
                                }
                            ) { innerPadding ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding)
                                ) {
                                    when (currentNavigationTab) {
                                        0 -> MemberDashboardScreen(
                                            currentUser = activeUser,
                                            group = selectedGroup,
                                            members = membersWithDetails,
                                            summary = cycleSummary,
                                            userReceipts = userReceipts,
                                            onSubmitPayment = { amt, method, ref, note ->
                                                viewModel.submitPayment(amt, method, ref, note)
                                            },
                                            onOpenReceipt = {},
                                            onJoinGroup = { showJoinGroupDialog = true },
                                            onEditProfile = { showEditProfileDialog = true },
                                            onViewTurns = { currentNavigationTab = 1 },
                                            onViewReceipts = { currentNavigationTab = 2 },
                                            onShowHowItWorks = { showHowItWorksDialog = true }
                                        )

                                        1 -> TurnsScreen(
                                            group = selectedGroup,
                                            members = membersWithDetails,
                                            currentCycle = selectedGroup?.currentCycle ?: 1,
                                            currentUserId = activeUser.id
                                        )

                                        2 -> ReceiptsScreen(
                                            receipts = userReceipts.ifEmpty { allReceipts }
                                        )

                                        3 -> NotificationsScreen(
                                            notifications = userNotifications,
                                            onMarkAsRead = { id -> viewModel.markNotificationRead(id) },
                                            onMarkAllAsRead = { viewModel.markAllNotificationsRead() }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
