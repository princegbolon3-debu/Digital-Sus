package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Receipt
import com.example.ui.components.CurrencyText
import com.example.ui.components.DigitalReceiptDialog
import com.example.ui.theme.SusuEmeraldContainer
import com.example.ui.theme.SusuEmeraldPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReceiptsScreen(
    receipts: List<Receipt>,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedMethodFilter by remember { mutableStateOf("All") }
    var selectedReceipt by remember { mutableStateOf<Receipt?>(null) }

    val filterChips = listOf("All", "MTN MoMo", "Orange", "Cash")

    val filtered = receipts.filter {
        val matchesQuery = it.receiptNumber.contains(searchQuery, ignoreCase = true) ||
                it.memberName.contains(searchQuery, ignoreCase = true) ||
                it.groupName.contains(searchQuery, ignoreCase = true) ||
                it.transactionReference.contains(searchQuery, ignoreCase = true)

        val matchesFilter = when (selectedMethodFilter) {
            "MTN MoMo" -> it.paymentMethod.contains("MTN", ignoreCase = true)
            "Orange" -> it.paymentMethod.contains("Orange", ignoreCase = true)
            "Cash" -> it.paymentMethod.contains("Cash", ignoreCase = true)
            else -> true
        }

        matchesQuery && matchesFilter
    }

    val totalSettled = receipts.sumOf { it.amount }
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)

    selectedReceipt?.let { receipt ->
        DigitalReceiptDialog(receipt = receipt, onDismiss = { selectedReceipt = null })
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("receipts_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SusuEmeraldContainer)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "DIGITAL RECEIPTS ARCHIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = SusuEmeraldPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${receipts.size} Receipts Issued",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = SusuEmeraldPrimary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Total Settled",
                        style = MaterialTheme.typography.labelSmall,
                        color = SusuEmeraldPrimary
                    )
                    CurrencyText(
                        amount = totalSettled,
                        style = MaterialTheme.typography.titleLarge,
                        color = SusuEmeraldPrimary
                    )
                }
            }
        }

        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by receipt #, member, or code...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().testTag("receipt_search_input"),
            shape = RoundedCornerShape(14.dp)
        )

        // Filter chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filterChips.forEach { chip ->
                val isSelected = selectedMethodFilter == chip
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedMethodFilter = chip },
                    label = { Text(chip) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SusuEmeraldPrimary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "No Receipts Found",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Only your personal verified contribution receipts are stored here for financial privacy and security.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("receipts_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered) { receipt ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReceipt = receipt }
                            .testTag("receipt_card_${receipt.receiptNumber}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = SusuEmeraldPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = receipt.receiptNumber,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "${receipt.memberName} • Cycle ${receipt.cycleNumber}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${receipt.paymentMethod} • ${dateFormat.format(Date(receipt.issuedAt))}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                CurrencyText(
                                    amount = receipt.amount,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = SusuEmeraldPrimary
                                )
                                Text(
                                    text = "View >",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SusuEmeraldPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
