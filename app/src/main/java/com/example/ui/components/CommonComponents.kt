package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Receipt
import com.example.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CurrencyText(
    amount: Double,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
    fontWeight: FontWeight = FontWeight.Bold
) {
    val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 2
    }
    Text(
        text = "L$ " + formatter.format(amount),
        modifier = modifier,
        style = style,
        color = color,
        fontWeight = fontWeight
    )
}

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (bg, fg, label, icon) = when (status.uppercase()) {
        "VERIFIED", "PAID", "COMPLETED" -> Quadruple(
            StatusPaidContainer,
            Color(0xFF047857),
            if (status == "VERIFIED") "Verified ✓" else if (status == "COMPLETED") "Completed" else "Paid ✓",
            Icons.Default.CheckCircle
        )
        "SUBMITTED", "PENDING", "PENDING_PAYOUT" -> Quadruple(
            StatusPendingContainer,
            SusuGoldDark,
            if (status == "SUBMITTED") "Pending Verification" else "Pending",
            Icons.Default.HourglassTop
        )
        "REJECTED", "FAILED" -> Quadruple(
            StatusRejectedContainer,
            StatusRejectedRed,
            "Rejected",
            Icons.Default.Cancel
        )
        else -> Quadruple(
            Color(0xFFF3F4F6),
            Color(0xFF6B7280),
            "Unpaid",
            Icons.Default.RadioButtonUnchecked
        )
    }

    Surface(
        modifier = modifier.testTag("status_badge_${status.lowercase()}"),
        shape = RoundedCornerShape(16.dp),
        color = bg
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = label,
                color = fg,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun DigitalReceiptDialog(
    receipt: Receipt,
    onDismiss: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.US)
    val formattedDate = dateFormat.format(Date(receipt.issuedAt))

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("receipt_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header seal
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(SusuEmeraldContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = "Official Receipt",
                        tint = SusuEmeraldPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "DIGITAL SUSU RECEIPT",
                    style = MaterialTheme.typography.labelMedium,
                    color = SusuEmeraldPrimary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Text(
                    text = receipt.groupName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = receipt.receiptNumber,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                // Amount
                Text(
                    text = "AMOUNT RECEIVED",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                CurrencyText(
                    amount = receipt.amount,
                    style = MaterialTheme.typography.headlineMedium,
                    color = SusuEmeraldPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Key Value grid
                ReceiptDetailRow("Member", receipt.memberName)
                ReceiptDetailRow("Cycle", "Cycle ${receipt.cycleNumber}")
                ReceiptDetailRow("Method", receipt.paymentMethod)
                ReceiptDetailRow("Tx Reference", receipt.transactionReference)
                ReceiptDetailRow("Date & Time", formattedDate)
                ReceiptDetailRow("Verification Status", "OFFICIALLY VERIFIED ✓")

                Spacer(modifier = Modifier.height(16.dp))

                // Decorative barcode simulation
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(24) { index ->
                        Box(
                            modifier = Modifier
                                .width(if (index % 3 == 0) 3.dp else 1.5.dp)
                                .fillMaxHeight(if (index % 4 == 0) 0.9f else 0.7f)
                                .background(Color.Gray)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("close_receipt_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Close")
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_receipt_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = SusuEmeraldPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Slip")
                    }
                }
            }
        }
    }
}

@Composable
fun HowItWorksDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("how_it_works_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SusuEmeraldContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("💡", fontSize = 18.sp)
                        }
                        Text(
                            text = "How Susu Works",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Text(
                    text = "Traditional Liberian community rotational savings, made stress-free and transparent on your phone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 4 Easy Steps
                SusuStepRow(
                    step = "1",
                    title = "Form or Join a Circle 🤝",
                    description = "A trusted group of members agrees on a regular contribution (e.g. L$ 2,500 every week)."
                )

                SusuStepRow(
                    step = "2",
                    title = "Pay Your Share Stress-Free 📱",
                    description = "Submit your contribution using Lonestar MTN MoMo (*156#), Orange Money (*144#), or direct Cash."
                )

                SusuStepRow(
                    step = "3",
                    title = "Get Verified Instant Receipts 🧾",
                    description = "The club organizer verifies the funds. Every deposit generates an authentic digital receipt with a seal."
                )

                SusuStepRow(
                    step = "4",
                    title = "Collect Your Full Lump Sum Pot 🎉",
                    description = "In your scheduled turn, the entire collection (e.g. L$ 12,500) is paid out directly to you!"
                )

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SusuEmeraldPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Got It! Easy & Stress-Free", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SusuStepRow(
    step: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(SusuGold),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = step,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                style = MaterialTheme.typography.labelMedium
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ReceiptDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End
        )
    }
}
