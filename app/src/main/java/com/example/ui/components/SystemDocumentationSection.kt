package com.example.ui.components

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.SusuEmeraldContainer
import com.example.ui.theme.SusuEmeraldPrimary
import com.example.ui.theme.SusuGold
import com.example.ui.theme.SusuSurfaceDark

@Composable
fun SystemDocumentationSection() {
    val context = LocalContext.current
    var selectedDocTab by remember { mutableIntStateOf(0) } // 0: SRS, 1: PRD, 2: SDD, 3: SSD
    var fullScreenDocTitle by remember { mutableStateOf<String?>(null) }
    var fullScreenDocBody by remember { mutableStateOf<String?>(null) }
    var showDownloadDialog by remember { mutableStateOf(false) }

    if (showDownloadDialog) {
        DownloadAppDialog(onDismiss = { showDownloadDialog = false })
    }

    val docs = listOf(
        Triple("SRS", "Software Requirements Specification (SRS v4.0)", SRS_DOCUMENT_TEXT),
        Triple("PRD", "Product Requirements Document (PRD v4.0)", PRD_DOCUMENT_TEXT),
        Triple("SDD", "Software Design Document (SDD v4.0)", SDD_DOCUMENT_TEXT),
        Triple("SSD", "System Sequence Diagrams (SSD v4.0)", SSD_DOCUMENT_TEXT)
    )

    val currentDoc = docs[selectedDocTab]

    if (fullScreenDocTitle != null && fullScreenDocBody != null) {
        Dialog(
            onDismissRequest = {
                fullScreenDocTitle = null
                fullScreenDocBody = null
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SusuSurfaceDark)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = fullScreenDocTitle ?: "",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Author & Root Admin: Prince Gbolon (princegbolon3@gmail.com)",
                                style = MaterialTheme.typography.labelSmall,
                                color = SusuGold
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, fullScreenDocTitle)
                                        putExtra(Intent.EXTRA_TEXT, fullScreenDocBody)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Export ${fullScreenDocTitle}"))
                                }
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Share Document", tint = SusuGold)
                            }
                            IconButton(
                                onClick = {
                                    fullScreenDocTitle = null
                                    fullScreenDocBody = null
                                }
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        Text(
                            text = fullScreenDocBody ?: "",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, SusuEmeraldPrimary.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    Icon(Icons.Default.Description, contentDescription = null, tint = SusuEmeraldPrimary)
                    Column {
                        Text(
                            text = "Engineering & Product Documentation",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Official SRS, PRD, SDD & SSD Specifications",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        val allDocsText = buildString {
                            docs.forEach { (_, title, content) ->
                                appendLine("=================================================================")
                                appendLine(title)
                                appendLine("=================================================================")
                                appendLine(content)
                                appendLine("\n\n")
                            }
                        }
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Digital Susu — Complete SRS, PRD, SDD & SSD Package")
                            putExtra(Intent.EXTRA_TEXT, allDocsText)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Export Complete Documentation Suite"))
                    },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export All", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Download / Export App APK Card for Distribution
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SusuEmeraldContainer.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, SusuEmeraldPrimary.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.InstallMobile,
                        contentDescription = null,
                        tint = SusuEmeraldPrimary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Download App to Phone (DigitalSusu.apk)",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = SusuEmeraldPrimary
                        )
                        Text(
                            text = "Save APK directly to device Downloads or distribute to Susu members",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { showDownloadDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SusuEmeraldPrimary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("Download APK", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }

            TabRow(
                selectedTabIndex = selectedDocTab,
                containerColor = SusuEmeraldContainer.copy(alpha = 0.35f),
                contentColor = SusuEmeraldPrimary,
                modifier = Modifier.fillMaxWidth()
            ) {
                docs.forEachIndexed { index, (shortCode, _, _) ->
                    Tab(
                        selected = selectedDocTab == index,
                        onClick = { selectedDocTab = index },
                        text = { Text(shortCode, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = currentDoc.second,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = SusuEmeraldPrimary
                    )
                    Text(
                        text = currentDoc.third.take(900) + "\n\n... [Tap 'Read Full Document' or 'Export / Share' below for complete specification]",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        fullScreenDocTitle = currentDoc.second
                        fullScreenDocBody = currentDoc.third
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SusuEmeraldPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Read Full ${currentDoc.first}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Digital Susu - ${currentDoc.second}")
                            putExtra(Intent.EXTRA_TEXT, currentDoc.third)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share ${currentDoc.first} Document"))
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export / Share ${currentDoc.first}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

private const val SRS_DOCUMENT_TEXT = """# SOFTWARE REQUIREMENTS SPECIFICATION (SRS)
## Digital Susu — Rotating Savings & Credit Association (ROSCA) Management Platform
Version: 4.0 | Author & System Administrator: Prince Gbolon (princegbolon3@gmail.com)
Platform: Android (Kotlin, Jetpack Compose, Room SQLite v4)

1. INTRODUCTION & SCOPE
Digital Susu digitizes traditional Liberian Susu savings circles, replacing fragile paper tally cards with a role-governed, audit-logged Android application supporting Liberian Dollars (L$) and multi-channel Mobile Money verification.

2. USER CLASSES & ROLES
- SYSTEM ADMINISTRATOR (ADMIN): Root authority (princegbolon3@gmail.com). Approves/rejects pending user registrations, assigns users to specific Susu circles as ORGANIZER or MEMBER, creates groups, reassigns group organizers, and monitors the immutable system audit log.
- GROUP ORGANIZER (ORGANIZER): Manages assigned Susu circles, shares group invite codes, reviews member applications, verifies/rejects Lonestar MTN MoMo, Orange Money, Cash, and Bank payments, issues receipts, and releases cycle lump-sum payouts.
- GROUP MEMBER (MEMBER): Joins Susu circles, tracks personal savings and assigned rotation turn (#1..#N), submits cycle contributions with transaction references, and downloads/shares serialized digital receipts.

3. CORE FUNCTIONAL REQUIREMENTS
- FR-001 (Self-Registration & Pending Gate): New Member/Organizer registrations enter PENDING_APPROVAL state and are blocked from portal access until approved by the Administrator.
- FR-002 (Targeted Group & Role Assignment): Admin approves accounts via ApproveAndAssignDialog with options to: (a) approve only, (b) assign directly to an existing Susu group as ORGANIZER or MEMBER with a turn slot, or (c) create a new Susu group for the user.
- FR-003 (Susu Circle Creation & Rotation Setup): Creating a circle initializes Cycle #1 (ACTIVE), places the designated Organizer in Turn #1, and assigns initial members to sequential payout turns.
- FR-004 (Multi-Channel Contributions): Supports Lonestar MTN Mobile Money, Orange Money Liberia, Cash Deposit, and Direct Bank Transfer with unique transaction reference indexing.
- FR-005 (Organizer Payment Verification): Organizers verify or reject SUBMITTED payments. Verification updates Contribution to PAID, increments pot volume, and issues receipt SUSU-2026-XXXXXX.
- FR-006 (Lump-Sum Payout & Cycle Rollover): Releasing payout records the disbursement, issues a payout receipt, marks Cycle N COMPLETED, and initializes Cycle N+1.
- FR-007 (Immutable Audit Trail): Every state change logs actor ID, actor name, action code, entity reference, and timestamp in audit_logs.
"""

private const val PRD_DOCUMENT_TEXT = """# PRODUCT REQUIREMENTS DOCUMENT (PRD)
## Digital Susu — Community Savings & ROSCA Management App
Product Owner: Prince Gbolon (princegbolon3@gmail.com) | Market: Republic of Liberia

1. PRODUCT VISION
Empower Liberian market women, small business owners, and community savings associations with a trustworthy, mobile-first Susu ledger that eliminates lost passbooks, payment disputes, and opaque payout rotations.

2. KEY USER PERSONAS
- Prince Gbolon (System Admin): Vets all new accounts, oversees all Susu groups, assigns organizers/members, and audits platform integrity.
- Susu Mother / Organizer: Manages weekly/monthly collections, verifies MTN MoMo & Orange Money transfers, and releases lump-sum payouts on schedule.
- Susu Member: Tracks payout turn order, submits contributions in seconds, and receives verifiable digital receipts.

3. END-TO-END USER JOURNEYS
- Journey 1 (Onboarding & Admin Vetting): User registers -> Account held in PENDING_APPROVAL -> Admin reviews and assigns user to their specific Susu circle as Organizer or Member.
- Journey 2 (Group & Role Governance): Admin/Organizer creates Susu circle, sets L$ contribution rate & frequency, designates Group Organizer, and enrolls members into turn slots.
- Journey 3 (Contribution & Verification): Member submits MoMo/Cash reference -> Organizer verifies -> System issues serialized receipt (SUSU-2026-XXXXXX).
- Journey 4 (Payout & Cycle Rollover): Organizer releases lump-sum pot to scheduled recipient -> System advances circle to next cycle automatically.
"""

private const val SDD_DOCUMENT_TEXT = """# SOFTWARE DESIGN DOCUMENT (SDD)
## Digital Susu — Architecture & Technical Specification
Architecture: Clean MVVM + Reactive Kotlin StateFlow + Room SQLite (Schema v4)

1. LAYERED ARCHITECTURE
- UI Layer (Jetpack Compose Material 3): AuthScreen, AccountPendingApprovalScreen, AdminDashboardScreen, OrganizerDashboardScreen, MemberDashboardScreen, TurnsScreen, GroupMembersScreen, ReceiptsScreen, ReportsScreen, NotificationsScreen.
- Presentation Layer: SusuViewModel (combines reactive Room Flows into UI StateFlows, manages active user session & selected group).
- Domain & Data Layer: SusuRepository (atomic coroutine operations for approval, group assignment, payment verification, receipt generation, and cycle advancement).
- Persistence Layer: SusuDatabase (11 normalized SQLite tables via SusuDao & MemberDao).
- Remote Gateway: MobileMoneyGateway (OAuth2 REST client for Lonestar Cell MTN MoMo & Orange Money Liberia with sandbox/production auto-detection).

2. DATABASE SCHEMA (11 ENTITIES)
1. users (id, fullName, phone, email, role, password, status, createdAt)
2. susu_groups (id, name, groupCode, description, organizerId, contributionAmount, frequency, startDate, numberOfCycles, currentCycle, status)
3. group_members (id, groupId, userId, payoutPosition, status, joinedAt) [Unique(groupId, userId)]
4. contribution_cycles (id, groupId, cycleNumber, startDate, endDate, recipientUserId, payoutAmount, status)
5. contributions (id, groupId, cycleId, cycleNumber, memberUserId, amount, dueDate, status, paidAt)
6. payments (id, contributionId, groupId, cycleId, memberUserId, amount, paymentMethod, transactionReference [Unique], status, rejectionReason, verifiedByUserId)
7. payouts (id, groupId, cycleId, cycleNumber, recipientUserId, amount, status, paymentMethod, transactionReference, completedAt)
8. receipts (id, receiptNumber [Unique], paymentId, groupId, groupName, memberUserId, memberName, amount, cycleNumber, paymentMethod, transactionReference, issuedAt)
9. notifications (id, userId, title, message, type, isRead, createdAt)
10. audit_logs (id, userId, userName, action, entityType, entityId, description, timestamp)
11. members (auxiliary member directory table)
"""

private const val SSD_DOCUMENT_TEXT = """# SYSTEM SEQUENCE DIAGRAMS (SSD)
## Digital Susu — Core Interaction Flows

SSD-01: REGISTRATION & TARGETED ADMIN APPROVAL
1. User -> System: registerUser(name, phone, email, role, password)
2. System -> DB: Insert User(status = PENDING_APPROVAL) + AuditLog(USER_REGISTERED)
3. Admin -> System: approveUser(userId, assignGroupId, roleInGroup, payoutPosition)
4. System -> DB: Update User(status = ACTIVE), enroll in target SusuGroup as ORGANIZER or MEMBER, notify User.

SSD-02: CONTRIBUTION SUBMISSION & VERIFICATION
1. Member -> System: submitPayment(groupId, amount, method, txReference)
2. System -> DB: Insert Payment(status = SUBMITTED), Update Contribution(status = PENDING), Notify Organizer.
3. Organizer -> System: verifyPayment(paymentId, verifierId)
4. System -> DB: Update Payment(VERIFIED), Update Contribution(PAID), Insert Receipt(SUSU-2026-XXXXXX), Insert AuditLog(PAYMENT_VERIFIED).

SSD-03: LUMP-SUM PAYOUT & CYCLE ADVANCEMENT
1. Organizer -> System: completeCyclePayout(groupId, paymentMethod, txReference)
2. System -> DB: Insert Payout(COMPLETED), Insert Payout Receipt, Mark Cycle N COMPLETED, Initialize Cycle N+1 (ACTIVE) with fresh Contributions for all members.
"""
