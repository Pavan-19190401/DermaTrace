package com.dermatrace.app.admin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.dermatrace.app.admin.data.repository.AdminRepository
import com.dermatrace.app.admin.ui.components.AdminCard
import com.dermatrace.app.admin.ui.components.AnimatedContentItem
import com.dermatrace.app.admin.ui.theme.*
import com.dermatrace.app.util.BiometricSecurityManager
import com.dermatrace.app.util.HapticFeedbackHelper

@Composable
fun ProfileScreen(
    repository: AdminRepository,
    onNavigateAuditLog: () -> Unit,
    onSignOut: () -> Unit,
    onShowToast: (String) -> Unit
) {
    var alertNewAnalyses by remember { mutableStateOf(true) }
    var alertCoverageGaps by remember { mutableStateOf(false) }
    var alertModelHealth by remember { mutableStateOf(true) }

    AnimatedContentItem(distance = 20.dp, durationMillis = 350) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
        ) {
            // Admin User Info Card
            item {
                AdminCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(Color(0x29FFFFFF))
                                .border(1.dp, Color(0x4DFFFFFF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "AD",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = AdminWhite
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = "Clinical Administrator",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = AdminWhite
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "admin@dermatrace.com",
                                fontSize = 13.sp,
                                color = AdminTextMuted
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0x1FFFFFFF))
                                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Firebase Custom Claim: role=admin",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = AdminWhite
                                )
                            }
                        }
                    }
                }
            }

            // Clinical Disclaimer Card (Required by prompt)
            item {
                AdminCard(
                    modifier = Modifier.fillMaxWidth(),
                    border = Color(0x33FFFFFF)
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Clinical Disclaimer",
                            tint = AdminWhite,
                            modifier = Modifier
                                .size(20.dp)
                                .padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Clinical Triage Disclaimer",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = AdminWhite
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "DermaTrace is a clinical decision-support and triage tool (Siamese CNN change score + classifier → No Visit Needed / Visit a Doctor / Inconclusive), not a diagnostic system. All algorithm outputs must be confirmed by qualified medical professionals.",
                                fontSize = 12.sp,
                                color = AdminTextPrimary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // Notification Alerts Toggles
            item {
                AdminCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Operational Alerts",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AdminTextPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        AlertToggleItem(
                            title = "New analysis alerts",
                            desc = "Notify when the clinical review queue grows",
                            checked = alertNewAnalyses,
                            onCheckedChange = {
                                alertNewAnalyses = it
                                onShowToast("Alert preferences updated")
                            }
                        )

                        AlertToggleItem(
                            title = "Coverage gap alerts",
                            desc = "Notify when a shift window is unfilled within 48h",
                            checked = alertCoverageGaps,
                            onCheckedChange = {
                                alertCoverageGaps = it
                                onShowToast("Alert preferences updated")
                            }
                        )

                        AlertToggleItem(
                            title = "Model health alerts",
                            desc = "Notify when inference latency or error rates spike",
                            checked = alertModelHealth,
                            onCheckedChange = {
                                alertModelHealth = it
                                onShowToast("Alert preferences updated")
                            }
                        )
                    }
                }
            }

            // Biometric Security & Hardware Lock Card
            item {
                val context = LocalContext.current
                var isBiometricEnabled by remember {
                    mutableStateOf(BiometricSecurityManager.isBiometricLockEnabled(context))
                }

                AdminCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x29FFFFFF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Fingerprint,
                                    contentDescription = "Biometric Lock",
                                    tint = AdminWhite,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Biometric App Lock",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AdminWhite
                                )
                                Text(
                                    text = "Require Fingerprint or Face ID for clinical access",
                                    fontSize = 11.sp,
                                    color = AdminTextMuted
                                )
                            }
                        }

                        Switch(
                            checked = isBiometricEnabled,
                            onCheckedChange = { enabled ->
                                val activity = context as? FragmentActivity
                                if (enabled && activity != null) {
                                    BiometricSecurityManager.authenticate(
                                        activity = activity,
                                        title = "Enable Biometric Security",
                                        subtitle = "Verify biometric identity to enable hardware-backed lock",
                                        onSuccess = {
                                            BiometricSecurityManager.setBiometricLockEnabled(context, true)
                                            isBiometricEnabled = true
                                            onShowToast("Biometric lock enabled")
                                        },
                                        onError = { err ->
                                            onShowToast("Authentication canceled: $err")
                                        }
                                    )
                                } else {
                                    BiometricSecurityManager.setBiometricLockEnabled(context, false)
                                    isBiometricEnabled = false
                                    onShowToast("Biometric lock disabled")
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AdminWhite,
                                checkedTrackColor = Color(0x66FFFFFF),
                                uncheckedThumbColor = AdminTextMuted,
                                uncheckedTrackColor = Color(0x24FFFFFF)
                            )
                        )
                    }
                }
            }

            // Audit Log Entry Link
            item {
                AdminCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onNavigateAuditLog
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x29FFFFFF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Security,
                                    contentDescription = null,
                                    tint = AdminWhite,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Audit Log & Security",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AdminWhite,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Immutable record of all admin modifications",
                                    fontSize = 11.sp,
                                    color = AdminTextMuted,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .wrapContentWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x24FFFFFF))
                                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "View",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AdminWhite,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            // Sign Out Button
            item {
                Button(
                    onClick = onSignOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0x33EF4444)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4DEF4444))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Sign Out",
                        tint = Color(0xFFFCA5A5),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sign out of Admin Workspace",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFCA5A5)
                    )
                }
            }
        }
    }
}

@Composable
fun AlertToggleItem(
    title: String,
    desc: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AdminWhite)
            Text(desc, fontSize = 11.sp, color = AdminTextMuted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AdminWhite,
                checkedTrackColor = Color(0x66FFFFFF),
                uncheckedThumbColor = AdminTextMuted,
                uncheckedTrackColor = Color(0x24FFFFFF)
            )
        )
    }
}
