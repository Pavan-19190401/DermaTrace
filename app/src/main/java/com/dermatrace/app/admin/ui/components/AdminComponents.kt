package com.dermatrace.app.admin.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dermatrace.app.admin.data.model.AdminKpi
import com.dermatrace.app.admin.data.model.DayDemandCapacity
import com.dermatrace.app.admin.ui.theme.*
import com.dermatrace.app.util.HapticFeedbackHelper

// Unified Obsidian Glass Dialog & Sheet container color
val AdminDialogBg = Color(0xF5101014)
val AdminDialogBorder = Color(0x33FFFFFF)
val AdminButtonBg = Color(0x2EFFFFFF)
val AdminButtonBorder = Color(0x4DFFFFFF)

@Composable
fun AdminTopBar(
    title: String = "Admin workspace",
    subtitle: String = "CLINICAL TRIAGE & STAFFING",
    avatarText: String = "AD",
    onAvatarClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = AdminWhite
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = AdminTextMuted,
                letterSpacing = 1.sp
            )
        }

        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color(0x29FFFFFF))
                .border(1.dp, Color(0x40FFFFFF), CircleShape)
                .clickable { onAvatarClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = avatarText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = AdminWhite
            )
        }
    }
}

@Composable
fun AdminCard(
    modifier: Modifier = Modifier,
    border: Color = AdminCardBorder,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val mod = modifier
        .clip(RoundedCornerShape(16.dp))
        .background(AdminCardBg)
        .border(1.dp, border, RoundedCornerShape(16.dp))
        .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
        .padding(16.dp)

    Column(modifier = mod) {
        content()
    }
}

@Composable
fun KpiCard(
    kpi: AdminKpi,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    AdminCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Text(
            text = kpi.label,
            fontSize = 12.sp,
            color = AdminTextMuted,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = kpi.value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = AdminWhite
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = kpi.note,
            fontSize = 11.sp,
            color = if (kpi.note.contains("Needs cover") || kpi.note.contains("Assign")) AdminAmber else AdminTextMuted
        )
    }
}

/**
 * Segmented Week Toggle with identical sliding glide animation as the main nav bar
 */
@Composable
fun SegmentedWeekToggle(
    selectedIndex: Int, // 0 = This week, 1 = Next week
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0x1AFFFFFF))
            .border(1.dp, Color(0x24FFFFFF), RoundedCornerShape(999.dp))
            .padding(4.dp)
    ) {
        val pillWidth = maxWidth / 2

        // Smooth sliding glide pill matching the patient nav bar
        val animatedOffset by animateDpAsState(
            targetValue = pillWidth * selectedIndex,
            animationSpec = spring(
                dampingRatio = 0.72f,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "WeekGlide"
        )

        // Sliding active pill
        Box(
            modifier = Modifier
                .width(pillWidth)
                .fillMaxHeight()
                .offset(x = animatedOffset)
                .clip(RoundedCornerShape(999.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0x4DFFFFFF), Color(0x24FFFFFF))
                    )
                )
                .border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(999.dp))
        )

        // Clickable Slots
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val interactionSource = remember { MutableInteractionSource() }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        HapticFeedbackHelper.performTick(null)
                        onSelect(0)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "This week",
                    fontSize = 13.sp,
                    fontWeight = if (selectedIndex == 0) FontWeight.Bold else FontWeight.Medium,
                    color = if (selectedIndex == 0) AdminWhite else AdminTextMuted
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        HapticFeedbackHelper.performTick(null)
                        onSelect(1)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Next week",
                    fontSize = 13.sp,
                    fontWeight = if (selectedIndex == 1) FontWeight.Bold else FontWeight.Medium,
                    color = if (selectedIndex == 1) AdminWhite else AdminTextMuted
                )
            }
        }
    }
}

@Composable
fun DemandCapacityBarChart(
    days: List<DayDemandCapacity>,
    selectedDayIndex: Int,
    onDaySelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val maxDemand = 80f

    AdminCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Demand vs. capacity",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = AdminTextPrimary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(AdminBarMuted)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Within", fontSize = 11.sp, color = AdminTextMuted)
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(AdminAmber)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Over", fontSize = 11.sp, color = AdminAmber)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Chart Bars
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            days.forEach { day ->
                val isSelected = day.dayIndex == selectedDayIndex
                val barHeightFraction = (day.demand / maxDemand).coerceIn(0.1f, 1f)
                val capMarkerFraction = (day.capacity / maxDemand).coerceIn(0.1f, 1f)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color(0x1FFFFFFF) else Color.Transparent)
                        .clickable { onDaySelect(day.dayIndex) }
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        // Capacity marker line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = (capMarkerFraction * 100).dp)
                                .height(2.dp)
                                .background(Color(0x66FFFFFF))
                        )

                        // Demand bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.65f)
                                .fillMaxHeight(barHeightFraction)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(
                                    if (day.isOverCapacity) AdminAmber else AdminBarMuted
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = day.shortLabel,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) AdminWhite else AdminTextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Selected Day Detail Banner
        val sel = days.getOrNull(selectedDayIndex) ?: days.first()
        val diff = sel.demand - sel.capacity
        val statusText = if (diff > 0) "+$diff over capacity" else "${-diff} spare capacity"
        val statusColor = if (diff > 0) AdminAmber else AdminWhite

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0x14FFFFFF))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${sel.fullName.take(3)} · ${sel.demand} analyses, cap ${sel.capacity}",
                fontSize = 12.sp,
                color = AdminTextPrimary
            )
            Text(
                text = statusText,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = statusColor
            )
        }
    }
}

/**
 * Patient-side matching bottom navigation bar with fluid horizontal sliding glide pill (.pn__glide)
 */
@Composable
fun AdminBottomNav(
    currentTab: String, // "staffing", "overview", "users", "profile"
    onTabSelected: (String) -> Unit
) {
    val items = listOf(
        Triple("staffing", "Staffing", Icons.Default.DateRange),
        Triple("overview", "Overview", Icons.Default.Analytics),
        Triple("users", "Users", Icons.Default.People),
        Triple("profile", "Profile", Icons.Default.Person)
    )

    val selectedIndex = remember(currentTab) {
        items.indexOfFirst { it.first == currentTab }.coerceAtLeast(0)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xD91E1C24)) // rgba(38,35,46,.85) matching patient .pn
                .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(999.dp))
                .padding(4.dp)
        ) {
            val slotWidth = maxWidth / items.size

            // Animated sliding glide pill (.pn__glide) with patient spring curve
            val animatedOffset by animateDpAsState(
                targetValue = slotWidth * selectedIndex,
                animationSpec = spring(
                    dampingRatio = 0.72f, // cubic-bezier(.34, 1.45, .64, 1) overshoot
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "NavGlide"
            )

            // Active Glide Pill
            Box(
                modifier = Modifier
                    .width(slotWidth)
                    .fillMaxHeight()
                    .offset(x = animatedOffset)
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0x4DFFFFFF), Color(0x24FFFFFF))
                        )
                    )
                    .border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(999.dp))
            )

            // Clickable Navigation Slots
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val interactionSource = remember { MutableInteractionSource() }

                items.forEachIndexed { index, (tabId, label, icon) ->
                    val isSelected = currentTab == tabId

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) {
                                HapticFeedbackHelper.performTick(null)
                                onTabSelected(tabId)
                            },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (isSelected) AdminWhite else AdminTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        if (isSelected) {
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AdminWhite,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminToastBanner(
    message: String,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = message.isNotEmpty(),
        enter = slideInVertically(
            initialOffsetY = { -it * 2 },
            animationSpec = spring(
                dampingRatio = 0.65f,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + fadeIn(animationSpec = tween(200)) + scaleIn(
            initialScale = 0.8f,
            animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow)
        ),
        exit = slideOutVertically(
            targetOffsetY = { -it * 2 },
            animationSpec = tween(240, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(180)) + scaleOut(
            targetScale = 0.8f,
            animationSpec = tween(200)
        ),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 10.dp, start = 16.dp, end = 16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xF50A0A0C))
                    .border(1.2.dp, Color(0x38FFFFFF), RoundedCornerShape(999.dp))
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF38BDF8))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = message,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AdminWhite,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}
