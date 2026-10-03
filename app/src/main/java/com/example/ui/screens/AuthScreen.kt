package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.*
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*

/**
 * Initial Gateway Authentication Screen (First Interface).
 * Supports dual portal views:
 * 1. Member & Organizer Portal
 * 2. System Administrator Portal
 * Allows users to Sign In or Create Account with phone/email and password.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onLogin: (identifier: String, password: String) -> Unit,
    onRegister: (fullName: String, phone: String, email: String, role: String, password: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPortal by remember { mutableIntStateOf(0) } // 0: Member & Organizer, 1: Administrator
    var isRegisterMode by remember { mutableStateOf(false) }

    // Form fields
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Register-specific fields
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("+231 ") }
    var email by remember { mutableStateOf("") }
    var regRole by remember { mutableStateOf("MEMBER") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .testTag("auth_screen")
    ) {
        // Hero Header with Community Art
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_susu_hero),
                contentDescription = "Digital Susu Community",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.4f),
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SusuGold,
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Text(
                        text = "🇱🇷 DIGITAL SUSU SYSTEM",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                Text(
                    text = "Welcome to Digital Susu",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "Organized savings, transparent cycles & verified payouts",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Portal Selector Tabs: Member & Organizer vs Admin
            TabRow(
                selectedTabIndex = selectedPortal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = SusuEmeraldPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .testTag("portal_tab_row")
            ) {
                Tab(
                    selected = selectedPortal == 0,
                    onClick = {
                        selectedPortal = 0
                        isRegisterMode = false
                        identifier = ""
                        password = ""
                    },
                    text = { Text("Member & Organizer", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Group, contentDescription = null) },
                    modifier = Modifier.testTag("portal_tab_member_organizer")
                )

                Tab(
                    selected = selectedPortal == 1,
                    onClick = {
                        selectedPortal = 1
                        isRegisterMode = false
                        identifier = ""
                        password = ""
                    },
                    text = { Text("Admin Portal", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null) },
                    modifier = Modifier.testTag("portal_tab_admin")
                )
            }

            // Mode Selector: Sign In vs Create Account
            if (selectedPortal == 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { isRegisterMode = false },
                        shape = RoundedCornerShape(10.dp),
                        color = if (!isRegisterMode) MaterialTheme.colorScheme.surface else Color.Transparent
                    ) {
                        Text(
                            text = "Sign In",
                            modifier = Modifier
                                .padding(vertical = 10.dp)
                                .fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontWeight = if (!isRegisterMode) FontWeight.Bold else FontWeight.Normal,
                            color = if (!isRegisterMode) SusuEmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { isRegisterMode = true },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isRegisterMode) MaterialTheme.colorScheme.surface else Color.Transparent
                    ) {
                        Text(
                            text = "Create Account",
                            modifier = Modifier
                                .padding(vertical = 10.dp)
                                .fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontWeight = if (isRegisterMode) FontWeight.Bold else FontWeight.Normal,
                            color = if (isRegisterMode) SusuEmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SusuGoldContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = SusuGoldDark)
                        Text(
                            text = "Administrator Access: Full oversight of system groups, users, and audit trail.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SusuOnGoldContainer
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (!isRegisterMode) {
                        // --- SIGN IN FORM ---
                        Text(
                            text = if (selectedPortal == 1) "Admin Sign In" else "Sign In to Your Account",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Enter your registered Liberia phone number or email address.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = identifier,
                            onValueChange = { identifier = it },
                            label = { Text("Phone or Email *") },
                            placeholder = { Text("+231 77 012 3456 or name@susu.lr") },
                            leadingIcon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_identifier_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password *") },
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
                                .testTag("login_password_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Button(
                            onClick = { onLogin(identifier, password) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("login_submit_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedPortal == 1) SusuSurfaceDark else SusuEmeraldPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            enabled = identifier.isNotBlank() && password.isNotBlank()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (selectedPortal == 1) "Access Admin Portal" else "Sign In",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        // --- CREATE ACCOUNT FORM ---
                        Text(
                            text = "Create Susu Account",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Full Name *") },
                            placeholder = { Text("e.g. Abraham Kollie") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_fullname_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Phone Number (Liberia) *") },
                            placeholder = { Text("+231 77 XXX XXXX") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_phone_input"),
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
                                .testTag("register_email_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password *") },
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
                                .testTag("register_password_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Role Selector
                        Text(
                            text = "Register as:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = regRole == "MEMBER",
                                onClick = { regRole = "MEMBER" },
                                label = { Text("Group Member") },
                                leadingIcon = {
                                    if (regRole == "MEMBER") Icon(Icons.Default.Check, contentDescription = null)
                                },
                                modifier = Modifier.weight(1f)
                            )

                            FilterChip(
                                selected = regRole == "ORGANIZER",
                                onClick = { regRole = "ORGANIZER" },
                                label = { Text("Group Organizer") },
                                leadingIcon = {
                                    if (regRole == "ORGANIZER") Icon(Icons.Default.Check, contentDescription = null)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Approval requirement notice
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = StatusPendingContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("⏳", fontSize = 18.sp)
                                Text(
                                    text = "Administrator Approval Required: When you register as a $regRole, the System Administrator (Prince Gbolon) must review and accept your account before you can log in.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SusuGoldDark
                                )
                            }
                        }

                        Button(
                            onClick = {
                                onRegister(fullName, phone, email, regRole, password)
                                isRegisterMode = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("register_submit_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = SusuEmeraldPrimary),
                            shape = RoundedCornerShape(12.dp),
                            enabled = fullName.isNotBlank() && phone.isNotBlank() && password.isNotBlank()
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Create $regRole Account (Submit for Approval)",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
