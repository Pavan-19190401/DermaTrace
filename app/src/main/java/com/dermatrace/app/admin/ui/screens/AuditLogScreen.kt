package com.dermatrace.app.admin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dermatrace.app.admin.data.repository.AdminRepository
import com.dermatrace.app.admin.ui.components.AdminCard
import com.dermatrace.app.admin.ui.components.AnimatedContentItem
import com.dermatrace.app.admin.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditLogScreen(
    repository: AdminRepository,
    onBack: () -> Unit
) {
    val auditLogs by repository.getAuditLogs().collectAsState()

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
                        text = "System Audit Log",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AdminWhite
                    )
                    Text(
                        text = "Immutable compliance & action record",
                        fontSize = 11.sp,
                        color = AdminTextMuted
                    )
                }
            }
        },
        containerColor = AdminBg
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp)
        ) {
            itemsIndexed(auditLogs) { index, entry ->
                val badgeColor = when {
                    entry.action.contains("SUSPEND") -> AdminAmber
                    else -> AdminWhite
                }

                AnimatedContentItem(delayMillis = index * 40) {
                    AdminCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(badgeColor.copy(alpha = 0.15f))
                                    .border(1.dp, badgeColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = entry.action,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeColor
                                )
                            }

                            Text(
                                text = entry.timestamp,
                                fontSize = 11.sp,
                                color = AdminTextMuted
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = entry.details,
                            fontSize = 13.sp,
                            color = AdminWhite,
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "By: ${entry.adminEmail} • Target: ${entry.target}",
                            fontSize = 11.sp,
                            color = AdminTextMuted
                        )
                    }
                }
            }
        }
    }
}
