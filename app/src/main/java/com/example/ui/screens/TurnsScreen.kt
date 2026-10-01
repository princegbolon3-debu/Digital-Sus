package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.CurrencyText
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*

@Composable
fun TurnsScreen(
    group: SusuGroup?,
    members: List<MemberWithPosition>,
    currentCycle: Int,
    currentUserId: Long,
    modifier: Modifier = Modifier
) {
    val totalPot = (group?.contributionAmount ?: 0.0) * (group?.numberOfCycles ?: members.size)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("turns_screen"),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SusuEmeraldContainer)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "ROTATIONAL SAVINGS SCHEDULE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SusuEmeraldPrimary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = group?.name ?: "Digital Susu",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = SusuEmeraldPrimary
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SusuEmeraldPrimary
                        ) {
                            Text(
                                text = "Round $currentCycle of ${group?.numberOfCycles ?: members.size}",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    HorizontalDivider(color = SusuEmeraldPrimary.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Each Round Contribution", style = MaterialTheme.typography.labelSmall, color = SusuEmeraldPrimary)
                            CurrencyText(amount = group?.contributionAmount ?: 0.0, style = MaterialTheme.typography.titleMedium, color = SusuEmeraldPrimary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Full Lump Sum Pot", style = MaterialTheme.typography.labelSmall, color = SusuEmeraldPrimary)
                            CurrencyText(amount = totalPot, style = MaterialTheme.typography.titleLarge, color = SusuEmeraldPrimary)
                        }
                    }
                }
            }
        }

        // Friendly Explanation Card
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = SusuEmeraldPrimary)
                    Text(
                        text = "Members receive the full pot one by one in numerical order. When all members contribute for a round, the pot is released!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Section Title
        item {
            Text(
                text = "Payout Order (Turns)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Members list in order of their payoutPosition
        items(members.sortedBy { it.membership.payoutPosition }) { item ->
            val isMe = item.user.id == currentUserId
            val isCurrentTurn = item.membership.payoutPosition == currentCycle
            val isPastTurn = item.membership.payoutPosition < currentCycle
            val isFutureTurn = item.membership.payoutPosition > currentCycle

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("turn_card_${item.membership.payoutPosition}"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        isCurrentTurn -> SusuGoldContainer
                        isMe -> SusuEmeraldContainer.copy(alpha = 0.4f)
                        else -> MaterialTheme.colorScheme.surface
                    }
                ),
                border = if (isMe) androidx.compose.foundation.BorderStroke(1.5.dp, SusuEmeraldPrimary) else null,
                elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrentTurn) 3.dp else 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Turn Number badge
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isCurrentTurn -> SusuGold
                                    isPastTurn -> Color(0xFF047857)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isPastTurn) {
                            Icon(Icons.Default.Check, contentDescription = "Completed", tint = Color.White)
                        } else {
                            Text(
                                text = "#${item.membership.payoutPosition}",
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrentTurn) Color.Black else MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp
                            )
                        }
                    }

                    // Member Info
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = item.user.fullName,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (isMe) {
                                Surface(shape = RoundedCornerShape(4.dp), color = SusuEmeraldPrimary) {
                                    Text(
                                        text = "YOU",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Text(
                            text = when {
                                isCurrentTurn -> "Receiving the pot this round! 🎉"
                                isPastTurn -> "Received pot in Round ${item.membership.payoutPosition} ✓"
                                else -> "Takes pot in Round ${item.membership.payoutPosition}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isCurrentTurn) SusuOnGoldContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Status / Pot
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (isCurrentTurn) {
                            Surface(shape = RoundedCornerShape(8.dp), color = SusuGold) {
                                Text(
                                    text = "POT RECIPIENT",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        } else {
                            StatusBadge(status = item.currentCycleContribution?.status ?: "UNPAID")
                        }
                    }
                }
            }
        }
    }
}
