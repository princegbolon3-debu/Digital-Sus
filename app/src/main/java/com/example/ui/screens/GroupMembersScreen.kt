package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.sp
import com.example.data.model.Member
import com.example.data.model.SusuGroup
import com.example.ui.components.AddMemberDialog
import com.example.ui.components.DeleteMemberConfirmDialog
import com.example.ui.components.EditMemberDialog
import com.example.ui.theme.*

/**
 * Screen providing comprehensive CRUD operations for Susu group members:
 * - View member directory with reactive Room database updates
 * - Search by member name or phone
 * - Add new member with auto-suggested turn position
 * - Edit existing member details
 * - Delete member with confirmation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupMembersScreen(
    group: SusuGroup?,
    members: List<Member>,
    onAddMember: (name: String, phone: String, email: String, position: Int, status: String, role: String, notes: String?, emergencyContact: String?) -> Unit,
    onUpdateMember: (Member) -> Unit,
    onDeleteMember: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var memberToEdit by remember { mutableStateOf<Member?>(null) }
    var memberToDelete by remember { mutableStateOf<Member?>(null) }

    // Filter members based on search query
    val filteredMembers = remember(members, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) {
            members.sortedBy { it.payoutPosition }
        } else {
            members.filter {
                it.fullName.lowercase().contains(q) ||
                it.phone.contains(q) ||
                it.email.lowercase().contains(q)
            }.sortedBy { it.payoutPosition }
        }
    }

    // Add Member Dialog
    if (showAddDialog) {
        val nextPos = (members.maxOfOrNull { it.payoutPosition } ?: 0) + 1
        AddMemberDialog(
            groupName = group?.name ?: "Susu Group",
            suggestedPosition = nextPos,
            onDismiss = { showAddDialog = false },
            onAdd = { name, phone, email, pos, status, role, notes, emergency ->
                showAddDialog = false
                onAddMember(name, phone, email, pos, status, role, notes, emergency)
            }
        )
    }

    // Edit Member Dialog
    memberToEdit?.let { member ->
        EditMemberDialog(
            member = member,
            onDismiss = { memberToEdit = null },
            onUpdate = { updated ->
                memberToEdit = null
                onUpdateMember(updated)
            },
            onDelete = { id ->
                memberToEdit = null
                onDeleteMember(id)
            }
        )
    }

    // Delete Confirmation Dialog
    memberToDelete?.let { member ->
        DeleteMemberConfirmDialog(
            memberName = member.fullName,
            onDismiss = { memberToDelete = null },
            onConfirm = {
                val id = member.id
                memberToDelete = null
                onDeleteMember(id)
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("group_members_screen"),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = SusuEmeraldPrimary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                text = { Text("Add Member", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("fab_add_member")
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(Modifier.height(4.dp))
                // Overview & Stats Bar
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SusuEmeraldContainer.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = group?.name ?: "Group Members",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SusuOnEmeraldContainer
                            )
                            Text(
                                text = "${members.size} Total Members • Room Local Persistence",
                                style = MaterialTheme.typography.bodySmall,
                                color = SusuOnEmeraldContainer.copy(alpha = 0.8f)
                            )
                        }

                        Button(
                            onClick = { showAddDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = SusuEmeraldPrimary),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("header_add_member_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("New Member", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name, phone, or email...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("member_search_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // Empty state if no members match query
            if (filteredMembers.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GroupOff,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Text(
                                text = if (searchQuery.isNotEmpty()) "No members match '$searchQuery'" else "No members in group yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (searchQuery.isEmpty()) {
                                OutlinedButton(
                                    onClick = { showAddDialog = true },
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Add First Member")
                                }
                            }
                        }
                    }
                }
            }

            // Member list items
            items(filteredMembers, key = { it.id }) { member ->
                MemberCard(
                    member = member,
                    onEdit = { memberToEdit = member },
                    onDelete = { memberToDelete = member }
                )
            }

            item {
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun MemberCard(
    member: Member,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("member_card_${member.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Turn Position Avatar Badge
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(SusuEmeraldPrimary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "TURN",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = SusuEmeraldPrimary
                        )
                        Text(
                            text = "#${member.payoutPosition}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SusuEmeraldPrimary
                        )
                    }
                }

                // Member Info
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = member.fullName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (member.role == "ORGANIZER") {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SusuGold.copy(alpha = 0.2f),
                                contentColor = SusuOnGoldContainer
                            ) {
                                Text(
                                    text = "Leader",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = member.phone,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (member.email.isNotBlank()) {
                        Text(
                            text = member.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                // Status Chip & Edit Button
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when (member.status) {
                            "ACTIVE" -> Color(0xFFE8F5E9)
                            "PAUSED" -> Color(0xFFFFF8E1)
                            else -> Color(0xFFFFEBEE)
                        },
                        contentColor = when (member.status) {
                            "ACTIVE" -> Color(0xFF2E7D32)
                            "PAUSED" -> Color(0xFFF57F17)
                            else -> Color(0xFFC62828)
                        }
                    ) {
                        Text(
                            text = member.status,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(32.dp).testTag("edit_member_icon_${member.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Member",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(32.dp).testTag("delete_member_icon_${member.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Remove Member",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Optional note / business description
            member.notes?.let { note ->
                if (note.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Storefront,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
