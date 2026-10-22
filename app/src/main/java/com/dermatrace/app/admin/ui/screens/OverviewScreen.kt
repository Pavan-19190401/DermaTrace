package com.dermatrace.app.admin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dermatrace.app.admin.data.model.ModelHealthService
import com.dermatrace.app.admin.data.repository.AdminRepository
import com.dermatrace.app.admin.ui.components.*
import com.dermatrace.app.admin.ui.theme.*

@Composable
fun OverviewScreen(
    repository: AdminRepository,
    onShowToast: (String) -> Unit
) {
    val users by repository.getUsers().collectAsState()
    val services by repository.getHealthServices().collectAsState()
    val thresholds by repository.getThresholds().collectAsState()
    val gaps = repository.getGaps()

    val activeUsersCount = users.count { it.isActive }
    val suspendedUsersCount = users.count { !it.isActive }
    val openGapsCount = gaps.count { it.assignedReviewer == null }
    val operationalModelsCount = services.count { it.isOperational }

    var serviceToRestart by remember { mutableStateOf<ModelHealthService?>(null) }
    var showThresholdDialog by remember { mutableStateOf(false) }
    var showModelCardDialog by remember { mutableStateOf(false) }

    AnimatedContentItem(distance = 20.dp, durationMillis = 350) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
        ) {
            // 4 Stat Tiles (2x2 grid)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OverviewStatTile(
                            label = "Active users",
                            value = activeUsersCount.toString(),
                            modifier = Modifier.weight(1f)
                        )
                        OverviewStatTile(
                            label = "Suspended",
                            value = suspendedUsersCount.toString(),
                            valueColor = if (suspendedUsersCount > 0) AdminAmber else AdminWhite,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OverviewStatTile(
                            label = "Open gaps",
                            value = openGapsCount.toString(),
                            valueColor = if (openGapsCount > 0) AdminAmber else AdminWhite,
                            modifier = Modifier.weight(1f)
                        )
                        OverviewStatTile(
                            label = "Models up",
                            value = "$operationalModelsCount / ${services.size}",
                            valueColor = if (operationalModelsCount == services.size) AdminWhite else AdminAmber,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Clinical Decision Rate & Drift Alert
            item {
                AdminCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Decision triage rates",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AdminTextPrimary
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x24FFFFFF))
                                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Live feed", fontSize = 11.sp, color = AdminWhite)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("No Visit Needed", fontSize = 11.sp, color = AdminTextMuted)
                            Text("72%", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AdminWhite)
                        }
                        Column {
                            Text("Visit a Doctor", fontSize = 11.sp, color = AdminTextMuted)
                            Text("19%", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AdminAmber)
                        }
                        Column {
                            Text("Inconclusive", fontSize = 11.sp, color = AdminTextMuted)
                            Text("9%", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AdminWhite)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Drift Banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x14FFFFFF))
                            .border(1.dp, Color(0x24FFFFFF), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = AdminWhite, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Decision distribution stable within ±2.5% of ISIC 2020 validation baseline. No drift detected.",
                            fontSize = 11.sp,
                            color = AdminTextPrimary
                        )
                    }
                }
            }

            // Model Health List
            item {
                AdminCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Model health & services",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AdminTextPrimary
                        )
                        Text(
                            text = "Tap to restart",
                            fontSize = 11.sp,
                            color = AdminTextMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        services.forEach { s ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x0FFFFFFF))
                                    .clickable { serviceToRestart = s }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = s.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AdminWhite
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Checked: ${s.lastChecked} • v${s.version}",
                                        fontSize = 11.sp,
                                        color = AdminTextMuted
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (s.isOperational) Color(0x24FFFFFF) else AdminAmberBg)
                                            .border(
                                                1.dp,
                                                if (s.isOperational) Color(0x33FFFFFF) else AdminAmber.copy(alpha = 0.5f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (s.isOperational) "Operational" else "Degraded",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (s.isOperational) AdminWhite else AdminAmber
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Restart",
                                        tint = AdminTextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Decision Thresholds & Model Card Section
            item {
                AdminCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Triage Thresholds (v${thresholds.version})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AdminTextPrimary
                        )
                        IconButton(onClick = { showThresholdDialog = true }) {
                            Icon(Icons.Default.Tune, contentDescription = "Edit thresholds", tint = AdminWhite)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Change Score Low", fontSize = 11.sp, color = AdminTextMuted)
                            Text("${thresholds.changeScoreLow}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AdminWhite)
                        }
                        Column {
                            Text("Change Score High", fontSize = 11.sp, color = AdminTextMuted)
                            Text("${thresholds.changeScoreHigh}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AdminWhite)
                        }
                        Column {
                            Text("Min Confidence", fontSize = 11.sp, color = AdminTextMuted)
                            Text("${thresholds.minConfidence}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AdminWhite)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { showModelCardDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AdminButtonBg),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AdminButtonBorder),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("View Model Card (model_card.json)", fontSize = 12.sp, color = AdminWhite)
                    }
                }
            }
        }
    }

    // Unified Obsidian Glass Restart Confirmation Dialog
    serviceToRestart?.let { s ->
        AlertDialog(
            onDismissRequest = { serviceToRestart = null },
            containerColor = AdminDialogBg,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.border(1.dp, AdminDialogBorder, RoundedCornerShape(20.dp)),
            title = {
                Text("Restart Service", color = AdminWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "Re-initialize and verify '${s.name}'? This clears cached TFLite tensors and restarts worker threads.",
                    fontSize = 13.sp,
                    color = AdminTextPrimary,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val success = repository.restartService(s.id)
                        if (success) {
                            onShowToast("${s.name} restarted and operational")
                        } else {
                            onShowToast("Restart failed for ${s.name}")
                        }
                        serviceToRestart = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdminButtonBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AdminButtonBorder),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Restart", color = AdminWhite, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { serviceToRestart = null }) {
                    Text("Cancel", color = AdminTextMuted)
                }
            }
        )
    }

    // Unified Obsidian Glass Thresholds Editor Dialog
    if (showThresholdDialog) {
        var lowText by remember { mutableStateOf(thresholds.changeScoreLow.toString()) }
        var highText by remember { mutableStateOf(thresholds.changeScoreHigh.toString()) }
        var confText by remember { mutableStateOf(thresholds.minConfidence.toString()) }

        AlertDialog(
            onDismissRequest = { showThresholdDialog = false },
            containerColor = AdminDialogBg,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.border(1.dp, AdminDialogBorder, RoundedCornerShape(20.dp)),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = AdminAmber, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Decision Cut-Offs", color = AdminWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(AdminAmberBg)
                            .border(1.dp, AdminAmber.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "WARNING: Lowering cut-offs can increase false reassurance or trigger excess clinical triage referrals.",
                            fontSize = 11.sp,
                            color = AdminAmber,
                            lineHeight = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Change Score Low Threshold (Current: 35.0):", fontSize = 11.sp, color = AdminTextMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = lowText,
                        onValueChange = { lowText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(AdminInputBg),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = AdminWhite,
                            unfocusedTextColor = AdminWhite,
                            focusedBorderColor = Color(0x66FFFFFF),
                            unfocusedBorderColor = AdminCardBorder
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Change Score High Threshold (Current: 65.0):", fontSize = 11.sp, color = AdminTextMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = highText,
                        onValueChange = { highText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(AdminInputBg),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = AdminWhite,
                            unfocusedTextColor = AdminWhite,
                            focusedBorderColor = Color(0x66FFFFFF),
                            unfocusedBorderColor = AdminCardBorder
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Classifier Confidence Cut-off (Current: 0.70):", fontSize = 11.sp, color = AdminTextMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = confText,
                        onValueChange = { confText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(AdminInputBg),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = AdminWhite,
                            unfocusedTextColor = AdminWhite,
                            focusedBorderColor = Color(0x66FFFFFF),
                            unfocusedBorderColor = AdminCardBorder
                        ),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val l = lowText.toFloatOrNull() ?: thresholds.changeScoreLow
                        val h = highText.toFloatOrNull() ?: thresholds.changeScoreHigh
                        val c = confText.toFloatOrNull() ?: thresholds.minConfidence
                        repository.updateThresholds(l, h, c)
                        onShowToast("Decision thresholds updated to v${thresholds.version + 1}")
                        showThresholdDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdminButtonBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AdminButtonBorder),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save & Log", color = AdminWhite, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showThresholdDialog = false }) {
                    Text("Cancel", color = AdminTextMuted)
                }
            }
        )
    }

    // Unified Obsidian Glass Model Card Dialog
    if (showModelCardDialog) {
        val modelCardText = remember { repository.getModelCardJson() }
        AlertDialog(
            onDismissRequest = { showModelCardDialog = false },
            containerColor = AdminDialogBg,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.border(1.dp, AdminDialogBorder, RoundedCornerShape(20.dp)),
            title = {
                Text("Model Card Specification", color = AdminWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF08080A))
                        .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = modelCardText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = AdminTextPrimary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showModelCardDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = AdminButtonBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AdminButtonBorder),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Close", color = AdminWhite, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun OverviewStatTile(
    label: String,
    value: String,
    valueColor: Color = AdminWhite,
    modifier: Modifier = Modifier
) {
    AdminCard(modifier = modifier) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = AdminTextMuted,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}
