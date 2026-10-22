package com.dermatrace.app.admin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dermatrace.app.admin.data.model.ShiftGap
import com.dermatrace.app.admin.data.repository.AdminRepository
import com.dermatrace.app.admin.ui.components.*
import com.dermatrace.app.admin.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffingBoardScreen(
    repository: AdminRepository,
    onNavigateReviewQueue: () -> Unit,
    onShowToast: (String) -> Unit
) {
    val weekIndex by repository.getWeekIndex().collectAsState()
    val selectedDay by repository.getSelectedDay().collectAsState()

    var activeGapForAssignment by remember { mutableStateOf<ShiftGap?>(null) }
    val kpis = repository.getKpis()
    val dayData = repository.getDemandCapacityList()
    val gaps = repository.getGaps()
    val onShiftMembers = repository.getOnShiftTeam(selectedDay)
    val candidates = repository.getCandidates()

    val openGapsCount = gaps.count { it.assignedReviewer == null }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContentItem(distance = 20.dp, durationMillis = 350) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
            ) {
                // Segmented Week Toggle with identical sliding glide animation
                item {
                    SegmentedWeekToggle(
                        selectedIndex = weekIndex,
                        onSelect = { repository.setWeekIndex(it) }
                    )
                }

                // 4 KPI Cards (2x2 grid)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            KpiCard(
                                kpi = kpis[0],
                                modifier = Modifier.weight(1f),
                                onClick = onNavigateReviewQueue
                            )
                            KpiCard(
                                kpi = kpis[1],
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            KpiCard(
                                kpi = kpis[2],
                                modifier = Modifier.weight(1f)
                            )
                            KpiCard(
                                kpi = kpis[3],
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Demand vs Capacity Chart
                item {
                    DemandCapacityBarChart(
                        days = dayData,
                        selectedDayIndex = selectedDay,
                        onDaySelect = { repository.setSelectedDay(it) }
                    )
                }

                // Quick Access to Clinical Review Queue
                item {
                    AdminCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onNavigateReviewQueue
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
                                        .background(Color(0x24FFFFFF))
                                        .border(1.dp, Color(0x33FFFFFF), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Assignment,
                                        contentDescription = "Review Queue",
                                        tint = AdminWhite,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Clinical Review Queue",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AdminWhite,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Triage cases needing dermatologist sign-off",
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

                // Coverage Gaps Section
                item {
                    AdminCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Coverage gaps",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AdminTextPrimary
                            )

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (openGapsCount > 0) AdminAmberBg else Color(0x1AFFFFFF))
                                    .border(
                                        1.dp,
                                        if (openGapsCount > 0) AdminAmber.copy(alpha = 0.5f) else AdminCardBorder,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (openGapsCount > 0) "$openGapsCount open" else "All filled",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (openGapsCount > 0) AdminAmber else AdminTextMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (gaps.isEmpty()) {
                            Text(
                                text = "No shift windows for this week.",
                                fontSize = 13.sp,
                                color = AdminTextMuted,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                gaps.forEach { gap ->
                                    val isAssigned = gap.assignedReviewer != null

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0x0FFFFFFF))
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = gap.whenText,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = AdminWhite
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = if (isAssigned) "${gap.assignedReviewer} assigned" else "Needs ${gap.roleNeeded}",
                                                fontSize = 12.sp,
                                                color = if (isAssigned) AdminTextMuted else AdminAmber
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isAssigned) Color.Transparent else Color(0x29FFFFFF))
                                                .border(
                                                    1.dp,
                                                    if (isAssigned) Color(0x33FFFFFF) else Color(0x4DFFFFFF),
                                                    RoundedCornerShape(10.dp)
                                                )
                                                .clickable {
                                                    if (isAssigned) {
                                                        repository.unassignGap(gap.id)
                                                        onShowToast("Assignment removed")
                                                    } else {
                                                        activeGapForAssignment = gap
                                                    }
                                                }
                                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                        ) {
                                            Text(
                                                text = if (isAssigned) "Undo" else "Assign",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = AdminWhite
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // On Shift Team List
                item {
                    val selectedDayName = dayData.getOrNull(selectedDay)?.fullName ?: "Day"
                    AdminCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "On shift · $selectedDayName",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AdminTextPrimary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            onShiftMembers.forEach { member ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0x0FFFFFFF))
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0x29FFFFFF)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = member.initials,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = AdminWhite
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = member.name,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = AdminWhite
                                                )
                                                Text(
                                                    text = "${member.role} · ${member.casesText}",
                                                    fontSize = 11.sp,
                                                    color = AdminTextMuted
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(if (member.status == "On shift") Color(0xFF34D399) else AdminTextMuted)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = member.status,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (member.status == "On shift") Color(0xFF34D399) else AdminTextMuted
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Workload Progress Bar
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(Color(0x1FFFFFFF))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(member.loadPercent / 100f)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(if (member.loadPercent >= 90) AdminAmber else AdminWhite.copy(alpha = 0.8f))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Footer Seed Notice
                if (repository.isSeedMode()) {
                    item {
                        Text(
                            text = "Illustrative figures only. Live values appear once real analysis and rota data are connected.",
                            fontSize = 11.sp,
                            color = AdminTextMuted.copy(alpha = 0.8f),
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Unified Obsidian Glass Bottom Sheet for Assigning Reviewer
        activeGapForAssignment?.let { gap ->
            ModalBottomSheet(
                onDismissRequest = { activeGapForAssignment = null },
                containerColor = AdminDialogBg,
                tonalElevation = 0.dp,
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 10.dp)
                            .size(width = 38.dp, height = 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0x40FFFFFF))
                    )
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .padding(bottom = 36.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Assign reviewer",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = AdminWhite
                            )
                            Text(
                                text = "${gap.whenText} • ${gap.roleNeeded}",
                                fontSize = 12.sp,
                                color = AdminAmber
                            )
                        }
                        IconButton(onClick = { activeGapForAssignment = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = AdminTextMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Available staff candidates:",
                        fontSize = 12.sp,
                        color = AdminTextMuted
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    candidates.forEach { cand ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x14FFFFFF))
                                .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(12.dp))
                                .clickable {
                                    repository.assignReviewer(gap.id, cand.name)
                                    onShowToast("${cand.name} assigned to ${gap.whenText}")
                                    activeGapForAssignment = null
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = cand.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AdminWhite
                                )
                                Text(
                                    text = cand.role,
                                    fontSize = 12.sp,
                                    color = AdminTextMuted
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x24FFFFFF))
                                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Available",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = AdminWhite
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}
