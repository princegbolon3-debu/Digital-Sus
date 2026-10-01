package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Member
import com.example.data.model.Payment
import com.example.data.model.SusuGroup
import com.example.data.model.User
import com.example.data.remote.MobileMoneyGateway
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SubmitPaymentDialog(
    group: SusuGroup,
    cycleNumber: Int,
    memberPhone: String = "",
    onDismiss: () -> Unit,
    onSubmit: (amount: Double, method: String, reference: String, note: String?) -> Unit
) {
    var amountText by remember { mutableStateOf(group.contributionAmount.toInt().toString()) }
    var selectedMethod by remember { mutableStateOf("Lonestar MTN Mobile Money") }
    var transactionRef by remember {
        mutableStateOf("MTN-${(100000..999999).random()}")
    }
    var noteText by remember { mutableStateOf("") }
    var memberPhoneNumber by remember { mutableStateOf(memberPhone.ifBlank { "0770000000" }) }
    var isProcessingApiPayment by remember { mutableStateOf(false) }
    var apiStatusMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val methods = listOf(
        Triple("Lonestar MTN Mobile Money", "📱 *156#", Color(0xFFFFF9C4)),
        Triple("Orange Money Liberia", "📱 *144#", Color(0xFFFFE0B2)),
        Triple("Cash Deposit", "💵 In Person", Color(0xFFE8F5E9)),
        Triple("Direct Bank Transfer", "🏦 Bank App", Color(0xFFE1F5FE))
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("submit_payment_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Submit Contribution",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${group.name} • Cycle $cycleNumber",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Friendly Tip
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SusuEmeraldContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("💡", fontSize = 16.sp)
                        Text(
                            text = "Pay via direct Mobile Money USSD push API or enter your transaction reference below.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SusuOnEmeraldContainer
                        )
                    }
                }

                // Amount
                Text(
                    text = "Contribution Amount",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (L$)") },
                    leadingIcon = { Text("L$", modifier = Modifier.padding(start = 12.dp), fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("payment_amount_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Visual Payment Method Selector
                Text(
                    text = "Select Payment Method",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    methods.forEach { (methodName, badge, _) ->
                        val isSelected = selectedMethod == methodName
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedMethod = methodName
                                    transactionRef = when (methodName) {
                                        "Orange Money Liberia" -> "ORG-${(100000..999999).random()}"
                                        "Cash Deposit" -> "CSH-${(100000..999999).random()}"
                                        "Direct Bank Transfer" -> "BNK-${(100000..999999).random()}"
                                        else -> "MTN-${(100000..999999).random()}"
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) SusuEmeraldContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = if (isSelected) BorderStroke(1.5.dp, SusuEmeraldPrimary) else null
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedMethod = methodName },
                                        colors = RadioButtonDefaults.colors(selectedColor = SusuEmeraldPrimary)
                                    )
                                    Text(
                                        text = methodName,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Text(
                                    text = badge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Dedicated Mobile Money API Checkout Card (MTN & Orange)
                if (selectedMethod.contains("MTN") || selectedMethod.contains("Orange")) {
                    val isOrange = selectedMethod.contains("Orange")
                    val isConfigured = if (isOrange) MobileMoneyGateway.isOrangeConfigured() else MobileMoneyGateway.isMtnConfigured()

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isOrange) Color(0xFFFFF3E0) else Color(0xFFFFFDE7)
                        ),
                        border = BorderStroke(1.dp, if (isOrange) Color(0xFFFF9800) else Color(0xFFFBC02D))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(if (isOrange) "🟠" else "🟡", fontSize = 16.sp)
                                    Text(
                                        text = if (isOrange) "Orange Money M-Payment API" else "MTN MoMo Open API",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isConfigured) SusuEmeraldContainer else Color(0xFFEEEEEE)
                                ) {
                                    Text(
                                        text = if (isConfigured) "LIVE API" else "SANDBOX ACTIVE",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isConfigured) SusuEmeraldPrimary else Color.DarkGray
                                    )
                                }
                            }

                            Text(
                                text = if (isOrange)
                                    "Sends an automated payment prompt to your Orange Money Liberia wallet for instant confirmation."
                                else
                                    "Sends an automated USSD push prompt (*156#) to your MTN phone for PIN entry and instant verification.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = memberPhoneNumber,
                                onValueChange = { memberPhoneNumber = it },
                                label = { Text(if (isOrange) "Orange Phone Number" else "MTN MoMo Number") },
                                placeholder = { Text("e.g. 0771234567") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )

                            if (apiStatusMessage != null) {
                                Text(
                                    text = apiStatusMessage ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SusuEmeraldPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Button(
                                onClick = {
                                    val amt = amountText.toDoubleOrNull() ?: group.contributionAmount
                                    if (memberPhoneNumber.isNotBlank()) {
                                        isProcessingApiPayment = true
                                        apiStatusMessage = "Initiating ${if (isOrange) "Orange Money" else "MTN MoMo"} USSD prompt..."
                                        coroutineScope.launch {
                                            val result = if (isOrange) {
                                                MobileMoneyGateway.processOrangeMoneyPayment(memberPhoneNumber, amt)
                                            } else {
                                                MobileMoneyGateway.processMtnMoMoPayment(memberPhoneNumber, amt)
                                            }
                                            isProcessingApiPayment = false
                                            if (result.isSuccess) {
                                                transactionRef = result.transactionReference
                                                apiStatusMessage = "Payment Confirmed! Ref: ${result.transactionReference}"
                                                onSubmit(amt, selectedMethod, result.transactionReference, "[API Verified] $memberPhoneNumber")
                                            } else {
                                                apiStatusMessage = result.message
                                            }
                                        }
                                    }
                                },
                                enabled = !isProcessingApiPayment && memberPhoneNumber.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isOrange) Color(0xFFF57C00) else Color(0xFFFBC02D),
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                if (isProcessingApiPayment) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = Color.Black,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Authorizing USSD Prompt...", fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "⚡ Send USSD Push Payment (L$$amountText)",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f))
                        Text(
                            text = "  OR ENTER REFERENCE MANUALLY  ",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f))
                    }
                }

                // Reference Number with auto-generate helper
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transaction Reference #",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = {
                            val prefix = when (selectedMethod) {
                                "Orange Money Liberia" -> "ORG"
                                "Cash Deposit" -> "CSH"
                                "Direct Bank Transfer" -> "BNK"
                                else -> "MTN"
                            }
                            transactionRef = "$prefix-${(100000..999999).random()}"
                        },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("🎲 Auto-Fill", style = MaterialTheme.typography.labelSmall)
                    }
                }

                OutlinedTextField(
                    value = transactionRef,
                    onValueChange = { transactionRef = it },
                    label = { Text("SMS / Receipt Code *") },
                    placeholder = { Text("e.g. MTN-839201") },
                    supportingText = { Text("Unique code from your Mobile Money SMS or paper receipt.") },
                    modifier = Modifier.fillMaxWidth().testTag("payment_ref_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Optional Note
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Note / Agent Details (Optional)") },
                    placeholder = { Text("e.g. Paid at Broad St agent") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull() ?: group.contributionAmount
                        if (transactionRef.isNotBlank()) {
                            onSubmit(amt, selectedMethod, transactionRef, noteText.ifBlank { null })
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("submit_payment_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = SusuEmeraldPrimary),
                    shape = RoundedCornerShape(14.dp),
                    enabled = transactionRef.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Confirm & Record Payment", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun VerifyPaymentDialog(
    payment: Payment,
    member: User?,
    onDismiss: () -> Unit,
    onConfirm: (isApproved: Boolean, reason: String?) -> Unit
) {
    var isRejecting by remember { mutableStateOf(false) }
    var rejectReason by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("verify_payment_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Verify Contribution",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Member:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(member?.fullName ?: "Member #${payment.memberUserId}", fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Amount:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            CurrencyText(amount = payment.amount, style = MaterialTheme.typography.titleMedium, color = SusuEmeraldPrimary)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Method:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(payment.paymentMethod, fontWeight = FontWeight.Medium)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Reference:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(payment.transactionReference, fontWeight = FontWeight.Bold, color = SusuEmeraldPrimary)
                        }
                        if (!payment.note.isNullOrBlank()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Note:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(payment.note, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                if (isRejecting) {
                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it },
                        label = { Text("Rejection Reason") },
                        placeholder = { Text("e.g. Reference not found in mobile money statement") },
                        modifier = Modifier.fillMaxWidth().testTag("reject_reason_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { isRejecting = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Back")
                        }
                        Button(
                            onClick = { onConfirm(false, rejectReason.ifBlank { "Unverified reference" }) },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusRejectedRed),
                            modifier = Modifier.weight(1f).testTag("confirm_reject_button")
                        ) {
                            Text("Reject Payment")
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { isRejecting = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRejectedRed),
                            modifier = Modifier.weight(1f).testTag("reject_payment_button")
                        ) {
                            Text("Reject")
                        }

                        Button(
                            onClick = { onConfirm(true, null) },
                            colors = ButtonDefaults.buttonColors(containerColor = SusuEmeraldPrimary),
                            modifier = Modifier.weight(1f).testTag("approve_payment_button")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Verify ✓")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompletePayoutDialog(
    recipient: User?,
    cycleNumber: Int,
    payoutAmount: Double,
    isFullyVerified: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (reference: String, notes: String?) -> Unit
) {
    var reference by remember { mutableStateOf("PAYOUT-ROT-C$cycleNumber-${(1000..9999).random()}") }
    var notes by remember { mutableStateOf("Full rotation payout for Cycle $cycleNumber") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("payout_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Release Susu Payout",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                if (!isFullyVerified) {
                    Surface(
                        color = StatusPendingContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = SusuGoldDark)
                            Text(
                                text = "Notice: Some members have not yet verified payments for this cycle. Releasing payout will close this cycle.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SusuOnGoldContainer
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Recipient:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(recipient?.fullName ?: "Scheduled Recipient", fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Phone:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(recipient?.phone ?: "-", fontWeight = FontWeight.Medium)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Payout:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            CurrencyText(amount = payoutAmount, style = MaterialTheme.typography.titleLarge, color = SusuEmeraldPrimary)
                        }
                    }
                }

                OutlinedTextField(
                    value = reference,
                    onValueChange = { reference = it },
                    label = { Text("Payout Transaction Reference *") },
                    placeholder = { Text("e.g. MTN-PAY-987654") },
                    modifier = Modifier.fillMaxWidth().testTag("payout_ref_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Verification Details") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = { onConfirm(reference, notes) },
                        colors = ButtonDefaults.buttonColors(containerColor = SusuEmeraldPrimary),
                        modifier = Modifier.weight(1.5f).testTag("confirm_payout_button"),
                        enabled = reference.isNotBlank()
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Complete Payout")
                    }
                }
            }
        }
    }
}

@Composable
fun EditProfileDialog(
    currentUser: User,
    onDismiss: () -> Unit,
    onSaveProfile: (fullName: String, phone: String, email: String, newPassword: String?) -> Unit,
    onLogout: () -> Unit
) {
    var fullName by remember { mutableStateOf(currentUser.fullName) }
    var phone by remember { mutableStateOf(currentUser.phone) }
    var email by remember { mutableStateOf(currentUser.email) }
    var newPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val formattedDate = remember(currentUser.createdAt) {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        sdf.format(Date(currentUser.createdAt))
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("edit_profile_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header with Avatar and Role
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(
                                    when (currentUser.role) {
                                        "ADMIN" -> SusuGold
                                        "ORGANIZER" -> SusuEmeraldPrimary
                                        else -> SusuEmeraldDark
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentUser.fullName.take(1).uppercase(),
                                color = if (currentUser.role == "ADMIN") Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            )
                        }

                        Column {
                            Text(
                                text = "My Profile",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = when (currentUser.role) {
                                        "ADMIN" -> SusuGoldContainer
                                        "ORGANIZER" -> SusuEmeraldContainer
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ) {
                                    Text(
                                        text = currentUser.role,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = when (currentUser.role) {
                                            "ADMIN" -> SusuOnGoldContainer
                                            "ORGANIZER" -> SusuOnEmeraldContainer
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }

                                Text(
                                    text = "ID: #${currentUser.id}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider()

                // Security & Status Notice
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp), tint = SusuEmeraldPrimary)
                            Text(
                                text = "Account & Security",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Joined $formattedDate • Status: ${currentUser.status}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Profile Fields
                Text(
                    text = "Personal Details",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name *") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_fullname_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number (Liberia) *") },
                    placeholder = { Text("+231 77 000 0000") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_phone_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    placeholder = { Text("name@example.lr") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_email_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password (Optional)") },
                    placeholder = { Text("Leave blank to keep current") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_password_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Action Buttons
                Button(
                    onClick = {
                        onSaveProfile(
                            fullName,
                            phone,
                            email,
                            if (newPassword.isNotBlank()) newPassword else null
                        )
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SusuEmeraldPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_profile_button"),
                    shape = RoundedCornerShape(12.dp),
                    enabled = fullName.isNotBlank() && phone.isNotBlank()
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onLogout()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("profile_logout_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out of Account")
                }
            }
        }
    }
}

@Composable
fun UserSwitcherDialog(
    users: List<User>,
    currentUser: User?,
    onDismiss: () -> Unit,
    onSelectUser: (User) -> Unit,
    onRegisterNew: () -> Unit,
    onLogout: () -> Unit = {}
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("user_switcher_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Switch Profile / Role",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Currently logged in: ${currentUser?.fullName ?: "None"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SusuEmeraldPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SusuGoldContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("💡", fontSize = 16.sp)
                        Text(
                            text = "Switch to Member to test making payments. Switch to Organizer to verify money and release the pot!",
                            style = MaterialTheme.typography.bodySmall,
                            color = SusuOnGoldContainer
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(users) { user ->
                        val isSelected = user.id == currentUser?.id
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectUser(user) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) SusuEmeraldContainer else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = when (user.role) {
                                        "ORGANIZER" -> Icons.Default.SupervisedUserCircle
                                        "ADMIN" -> Icons.Default.AdminPanelSettings
                                        else -> Icons.Default.Person
                                    },
                                    contentDescription = null,
                                    tint = if (isSelected) SusuEmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = user.fullName,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) SusuOnEmeraldContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${user.role} • ${user.phone}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSelected) SusuOnEmeraldContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = SusuEmeraldPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onRegisterNew,
                        modifier = Modifier.weight(1f).testTag("register_new_profile_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ New User", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            onDismiss()
                            onLogout()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f).testTag("dialog_logout_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sign Out", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun CreateGroupDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, code: String, desc: String, amount: Double, freq: String, cycles: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("SUSU-${(10..99).random()}") }
    var desc by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("1000") }
    var frequency by remember { mutableStateOf("WEEKLY") }
    var cyclesText by remember { mutableStateOf("10") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("create_group_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Create Susu Group",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Group Name *") },
                    placeholder = { Text("e.g. Sinkor Traders Susu") },
                    modifier = Modifier.fillMaxWidth().testTag("group_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Group Code (Invite Key) *") },
                    modifier = Modifier.fillMaxWidth().testTag("group_code_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description") },
                    placeholder = { Text("Community goals or location") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Contribution per Member (L$) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("group_amount_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Frequency selection
                Text("Contribution Frequency", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("DAILY", "WEEKLY", "BIWEEKLY", "MONTHLY").forEach { freq ->
                        FilterChip(
                            selected = frequency == freq,
                            onClick = { frequency = freq },
                            label = { Text(freq.lowercase().replaceFirstChar { it.uppercase() }) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                OutlinedTextField(
                    value = cyclesText,
                    onValueChange = { cyclesText = it },
                    label = { Text("Number of Members / Cycles *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("group_cycles_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 1000.0
                            val cyc = cyclesText.toIntOrNull() ?: 10
                            onCreate(name, code, desc, amt, frequency, cyc)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SusuEmeraldPrimary),
                        modifier = Modifier.weight(1.5f).testTag("confirm_create_group_button"),
                        enabled = name.isNotBlank() && code.isNotBlank()
                    ) {
                        Text("Create Susu")
                    }
                }
            }
        }
    }
}

@Composable
fun JoinGroupDialog(
    onDismiss: () -> Unit,
    onJoin: (code: String) -> Unit
) {
    var code by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("join_group_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Join Susu Group",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Enter the group invite code provided by your Susu organizer (e.g. WP-SUSU-26).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Group Code *") },
                    placeholder = { Text("e.g. WP-SUSU-26") },
                    modifier = Modifier.fillMaxWidth().testTag("join_code_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { onJoin(code) },
                        colors = ButtonDefaults.buttonColors(containerColor = SusuEmeraldPrimary),
                        modifier = Modifier.weight(1.5f).testTag("confirm_join_group_button"),
                        enabled = code.isNotBlank()
                    ) {
                        Text("Join Group")
                    }
                }
            }
        }
    }
}

@Composable
fun RegisterUserDialog(
    onDismiss: () -> Unit,
    onRegister: (name: String, phone: String, email: String, role: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("+231 ") }
    var email by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("MEMBER") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("register_user_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Register Susu Account",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name *") },
                    modifier = Modifier.fillMaxWidth().testTag("reg_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number (Liberia) *") },
                    placeholder = { Text("+231 77 XXX XXXX") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth().testTag("reg_phone_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    placeholder = { Text("name@example.lr") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                var password by remember { mutableStateOf("") }
                var passwordVisible by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Account Password *") },
                    placeholder = { Text("••••••••") },
                    visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("reg_password_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Text("Account Role", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("MEMBER", "ORGANIZER", "ADMIN").forEach { r ->
                        FilterChip(
                            selected = role == r,
                            onClick = { role = r },
                            label = { Text(r) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { onRegister(name, phone, email, role) },
                        colors = ButtonDefaults.buttonColors(containerColor = SusuEmeraldPrimary),
                        modifier = Modifier.weight(1.5f).testTag("confirm_register_button"),
                        enabled = name.isNotBlank() && phone.length > 5
                    ) {
                        Text("Register")
                    }
                }
            }
        }
    }
}

@Composable
fun AddMemberDialog(
    groupName: String,
    suggestedPosition: Int,
    onDismiss: () -> Unit,
    onAdd: (name: String, phone: String, email: String, position: Int, status: String, role: String, notes: String?, emergencyContact: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var positionText by remember { mutableStateOf(suggestedPosition.toString()) }
    var status by remember { mutableStateOf("ACTIVE") }
    var role by remember { mutableStateOf("MEMBER") }
    var notes by remember { mutableStateOf("") }
    var emergencyContact by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("add_member_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Add Group Member", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(groupName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name *") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().testTag("member_name_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Mobile Money Phone *") },
                    placeholder = { Text("+231 77 000 0000") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth().testTag("member_phone_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email (Optional)") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth().testTag("member_email_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = positionText,
                        onValueChange = { positionText = it },
                        label = { Text("Turn # *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("member_position_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = emergencyContact,
                        onValueChange = { emergencyContact = it },
                        label = { Text("Emergency Contact") },
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Text("Status", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("ACTIVE", "PAUSED").forEach { st ->
                        FilterChip(
                            selected = status == st,
                            onClick = { status = st },
                            label = { Text(st) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Business Description") },
                    placeholder = { Text("e.g. Waterside stall merchant") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 2
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val pos = positionText.toIntOrNull() ?: suggestedPosition
                            onAdd(
                                name.trim(),
                                phone.trim(),
                                email.trim(),
                                pos,
                                status,
                                role,
                                notes.takeIf { it.isNotBlank() },
                                emergencyContact.takeIf { it.isNotBlank() }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SusuEmeraldPrimary),
                        modifier = Modifier.weight(1.5f).testTag("confirm_add_member_button"),
                        enabled = name.isNotBlank() && phone.length > 5
                    ) {
                        Text("Add Member")
                    }
                }
            }
        }
    }
}

@Composable
fun EditMemberDialog(
    member: Member,
    onDismiss: () -> Unit,
    onUpdate: (Member) -> Unit,
    onDelete: (Long) -> Unit
) {
    var name by remember { mutableStateOf(member.fullName) }
    var phone by remember { mutableStateOf(member.phone) }
    var email by remember { mutableStateOf(member.email) }
    var positionText by remember { mutableStateOf(member.payoutPosition.toString()) }
    var status by remember { mutableStateOf(member.status) }
    var notes by remember { mutableStateOf(member.notes ?: "") }
    var emergencyContact by remember { mutableStateOf(member.emergencyContact ?: "") }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        DeleteMemberConfirmDialog(
            memberName = member.fullName,
            onDismiss = { showDeleteConfirm = false },
            onConfirm = {
                showDeleteConfirm = false
                onDelete(member.id)
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("edit_member_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Edit Member", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Member #${member.id} • Turn #${member.payoutPosition}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name *") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().testTag("edit_member_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number *") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth().testTag("edit_member_phone_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = positionText,
                        onValueChange = { positionText = it },
                        label = { Text("Turn # *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("edit_member_position_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = emergencyContact,
                        onValueChange = { emergencyContact = it },
                        label = { Text("Emergency Contact") },
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Text("Membership Status", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("ACTIVE", "PAUSED", "INACTIVE").forEach { st ->
                        FilterChip(
                            selected = status == st,
                            onClick = { status = st },
                            label = { Text(st, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("delete_member_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Remove")
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                val pos = positionText.toIntOrNull() ?: member.payoutPosition
                                onUpdate(
                                    member.copy(
                                        fullName = name.trim(),
                                        phone = phone.trim(),
                                        email = email.trim(),
                                        payoutPosition = pos,
                                        status = status,
                                        notes = notes.takeIf { it.isNotBlank() },
                                        emergencyContact = emergencyContact.takeIf { it.isNotBlank() }
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SusuEmeraldPrimary),
                            modifier = Modifier.testTag("confirm_update_member_button"),
                            enabled = name.isNotBlank() && phone.length > 5
                        ) {
                            Text("Save Changes")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteMemberConfirmDialog(
    memberName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Remove Member?", fontWeight = FontWeight.Bold)
        },
        text = {
            Text("Are you sure you want to remove $memberName from this Susu group? This will delete their record from the local Room database.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("confirm_delete_button")
            ) {
                Text("Yes, Remove")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CreateCycleDialog(
    suggestedCycleNumber: Int,
    groupName: String,
    onDismiss: () -> Unit,
    onCreate: (cycleNumber: Int, daysDue: Int, status: String) -> Unit
) {
    var cycleNumText by remember { mutableStateOf(suggestedCycleNumber.toString()) }
    var selectedDurationDays by remember { mutableIntStateOf(7) }
    var selectedStatus by remember { mutableStateOf("ACTIVE") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("create_cycle_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Create Contribution Cycle",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = groupName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SusuEmeraldContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("📅", fontSize = 16.sp)
                        Text(
                            text = "Creating a cycle generates contribution obligations for all members and schedules the next recipient in the rotation.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SusuOnEmeraldContainer
                        )
                    }
                }

                OutlinedTextField(
                    value = cycleNumText,
                    onValueChange = { cycleNumText = it },
                    label = { Text("Cycle Number *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("cycle_number_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Text("Cycle Duration (Due Date)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1 to "1 Day", 7 to "7 Days", 14 to "14 Days", 30 to "30 Days").forEach { (days, label) ->
                        FilterChip(
                            selected = selectedDurationDays == days,
                            onClick = { selectedDurationDays = days },
                            label = { Text(label) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Text("Cycle Status", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("ACTIVE", "UPCOMING").forEach { st ->
                        FilterChip(
                            selected = selectedStatus == st,
                            onClick = { selectedStatus = st },
                            label = { Text(st) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val num = cycleNumText.toIntOrNull() ?: suggestedCycleNumber
                            onCreate(num, selectedDurationDays, selectedStatus)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SusuEmeraldPrimary),
                        modifier = Modifier.weight(1.5f).testTag("confirm_create_cycle_button"),
                        enabled = cycleNumText.isNotBlank()
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create Cycle")
                    }
                }
            }
        }
    }
}

