package com.dermatrace.app.admin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
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
import com.dermatrace.app.admin.data.model.TriageCase
import com.dermatrace.app.admin.data.repository.AdminRepository
import com.dermatrace.app.admin.ui.components.*
import com.dermatrace.app.admin.ui.theme.*
import com.dermatrace.app.util.HapticFeedbackHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewQueueScreen(
    repository: AdminRepository,
    onBack: () -> Unit,
    onShowToast: (String) -> Unit
) {
    val cases by repository.getCases().collectAsState()
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf("All") }
    var caseToReview by remember { mutableStateOf<TriageCase?>(null) }
    var reviewNoteText by remember { mutableStateOf("") }
    var caseToAssign by remember { mutableStateOf<TriageCase?>(null) }

    val candidates = repository.getCandidates()

    val filteredCases = cases.filter { c ->
        when (selectedFilter) {
            "Visit a Doctor" -> c.outcome == "Visit a Doctor"
            "Inconclusive" -> c.outcome == "Inconclusive"
            "Unassigned" -> c.assignedTo == null
            else -> true
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AdminWhite)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Clinical Review Queue",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AdminWhite
                    )
                    Text(
                        text = "Pseudonymous triage cases (${cases.size} total)",
                        fontSize = 11.sp,
                        color = AdminTextMuted
                    )
                }
            }
        },
        containerColor = AdminBg
    ) { padding ->
        AnimatedContentItem(distance = 20.dp, durationMillis = 350) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Privacy Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x14FFFFFF))
                        .border(1.dp, Color(0x24FFFFFF), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Privacy Protected",
                            tint = AdminWhite,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Strict Clinical Privacy: Patient identities and raw lesion photos remain encrypted on patient devices. Only anonymized scores and ABCDE descriptors are presented.",
                            fontSize = 11.sp,
                            color = AdminTextPrimary,
                            lineHeight = 15.sp
                        )
                    }
                }

                // Filter Chips
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf("All", "Visit a Doctor", "Inconclusive", "Unassigned")
                    items(filters) { filter ->
                        val isSelected = selectedFilter == filter
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) Color(0x33FFFFFF) else Color(0x14FFFFFF))
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0x66FFFFFF) else Color(0x1FFFFFFF),
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable { selectedFilter = filter }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = filter,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) AdminWhite else AdminTextMuted
                            )
                        }
                    }
                }

                // Cases List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 40.dp)
                ) {
                    if (filteredCases.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No cases match current filter.",
                                    fontSize = 13.sp,
                                    color = AdminTextMuted
                                )
                            }
                        }
                    } else {
                        items(filteredCases) { triageCase ->
                            val outcomeColor = when (triageCase.outcome) {
                                "Visit a Doctor" -> AdminRed
                                "Inconclusive" -> AdminAmber
                                else -> AdminWhite
                            }

                            AdminCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = triageCase.caseId,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AdminWhite
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = triageCase.lesionId,
                                            fontSize = 12.sp,
                                            color = AdminTextMuted
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(outcomeColor.copy(alpha = 0.2f))
                                            .border(1.dp, outcomeColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = triageCase.outcome,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = outcomeColor
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Scores Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Change Score", fontSize = 11.sp, color = AdminTextMuted)
                                        Text(
                                            text = String.format("%.1f", triageCase.changeScore),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (triageCase.changeScore >= 60f) AdminAmber else AdminWhite
                                        )
                                    }
                                    Column {
                                        Text("Confidence", fontSize = 11.sp, color = AdminTextMuted)
                                        Text(
                                            text = "${(triageCase.confidence * 100).toInt()}%",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AdminWhite
                                        )
                                    }
                                    Column {
                                        Text("Body Site", fontSize = 11.sp, color = AdminTextMuted)
                                        Text(
                                            text = triageCase.bodySite,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = AdminWhite
                                        )
                                    }
                                    Column {
                                        Text("Status", fontSize = 11.sp, color = AdminTextMuted)
                                        Text(
                                            text = triageCase.status,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (triageCase.status == "Reviewed") AdminWhite else AdminAmber
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // ABCDE Summary
                                Text(
                                    text = "ABCDE: ${triageCase.abcdeSummary}",
                                    fontSize = 12.sp,
                                    color = AdminTextPrimary
                                )

                                triageCase.reviewerNote?.let { note ->
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Clinical Note: $note",
                                        fontSize = 11.sp,
                                        color = AdminTextMuted
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Action buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (triageCase.assignedTo == null) {
                                        Button(
                                            onClick = { caseToAssign = triageCase },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0x29FFFFFF)
                                            ),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x40FFFFFF)),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.PersonAdd,
                                                contentDescription = "Assign",
                                                modifier = Modifier.size(14.dp),
                                                tint = AdminWhite
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Assign", fontSize = 12.sp, color = AdminWhite)
                                        }
                                    } else {
                                        Text(
                                            text = "Assigned: ${triageCase.assignedTo}",
                                            fontSize = 11.sp,
                                            color = AdminTextMuted,
                                            modifier = Modifier.padding(end = 12.dp)
                                        )
                                    }

                                    if (triageCase.status != "Reviewed") {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = {
                                                caseToReview = triageCase
                                                reviewNoteText = ""
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = AdminButtonBg
                                            ),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, AdminButtonBorder),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                        ) {
                                            Text("Review", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AdminWhite)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Unified Obsidian Glass Review Action Dialog
        caseToReview?.let { c ->
            AlertDialog(
                onDismissRequest = { caseToReview = null },
                containerColor = AdminDialogBg,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.border(1.dp, AdminDialogBorder, RoundedCornerShape(20.dp)),
                title = {
                    Text("Clinical Sign-Off", color = AdminWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                },
                text = {
                    Column {
                        Text(
                            text = "Add clinical triage note for ${c.caseId} (${c.outcome}):",
                            fontSize = 13.sp,
                            color = AdminTextMuted
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = reviewNoteText,
                            onValueChange = { reviewNoteText = it },
                            placeholder = { Text("Enter recommendation / note...", color = AdminTextMuted) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(AdminInputBg),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = AdminWhite,
                                unfocusedTextColor = AdminWhite,
                                focusedBorderColor = Color(0x66FFFFFF),
                                unfocusedBorderColor = AdminCardBorder
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val note = if (reviewNoteText.isBlank()) "Reviewed and cleared by clinician." else reviewNoteText
                            repository.markCaseReviewed(c.caseId, note)
                            HapticFeedbackHelper.performSuccess(context)
                            onShowToast("Case ${c.caseId} marked as Reviewed")
                            caseToReview = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AdminButtonBg),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AdminButtonBorder),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Confirm Sign-off", color = AdminWhite, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { caseToReview = null }) {
                        Text("Cancel", color = AdminTextMuted)
                    }
                }
            )
        }

        // Unified Obsidian Glass Assign Case Dialog (Doctor Candidate Popup)
        caseToAssign?.let { c ->
            AlertDialog(
                onDismissRequest = { caseToAssign = null },
                containerColor = AdminDialogBg,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.border(1.dp, AdminDialogBorder, RoundedCornerShape(20.dp)),
                title = {
                    Column {
                        Text("Assign Reviewer", color = AdminWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Case ${c.caseId} • ${c.outcome}", color = AdminAmber, fontSize = 12.sp)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Select clinician to assign for review:",
                            fontSize = 13.sp,
                            color = AdminTextMuted
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        candidates.forEach { cand ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x14FFFFFF))
                                    .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(12.dp))
                                    .clickable {
                                        repository.assignCase(c.caseId, cand.name)
                                        HapticFeedbackHelper.performTick(null)
                                        onShowToast("Assigned ${cand.name} to ${c.caseId}")
                                        caseToAssign = null
                                    }
                                    .padding(vertical = 12.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(cand.name, color = AdminWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text(cand.role, color = AdminTextMuted, fontSize = 11.sp)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0x24FFFFFF))
                                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("Assign", color = AdminWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { caseToAssign = null }) {
                        Text("Cancel", color = AdminTextMuted)
                    }
                }
            )
        }
    }
}
