package com.dermatrace.app.admin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dermatrace.app.admin.data.model.AdminUser
import com.dermatrace.app.admin.data.repository.AdminRepository
import com.dermatrace.app.admin.ui.components.*
import com.dermatrace.app.admin.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersScreen(
    repository: AdminRepository,
    onShowToast: (String) -> Unit
) {
    val allUsers by repository.getUsers().collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var userToToggle by remember { mutableStateOf<AdminUser?>(null) }

    val filteredUsers = remember(allUsers, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) allUsers else allUsers.filter {
            it.name.lowercase().contains(q) || it.email.lowercase().contains(q)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContentItem(distance = 20.dp, durationMillis = 350) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
            ) {
                // Search Bar
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search users by name or email...", color = AdminTextMuted, fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = AdminTextMuted)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = AdminTextMuted)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
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

                // Count Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${filteredUsers.size} of ${allUsers.size} users",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = AdminTextMuted
                        )
                        Text(
                            text = "Tap to manage status",
                            fontSize = 11.sp,
                            color = AdminTextMuted
                        )
                    }
                }

                // User List
                if (filteredUsers.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No users match your search.",
                                fontSize = 14.sp,
                                color = AdminTextMuted
                            )
                        }
                    }
                } else {
                    items(filteredUsers) { user ->
                        AdminCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { userToToggle = user }
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
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(Color(0x29FFFFFF))
                                            .border(1.dp, Color(0x33FFFFFF), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = user.initials,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AdminWhite
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = user.name,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = AdminWhite,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = user.email,
                                            fontSize = 12.sp,
                                            color = AdminTextMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Joined ${user.joinDate} • ${user.lesionCount} lesions tracked",
                                            fontSize = 11.sp,
                                            color = AdminTextMuted.copy(alpha = 0.8f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                // Status text with indicator dot (not a button)
                                Row(
                                    modifier = Modifier.wrapContentWidth(Alignment.End),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (user.isActive) Color(0xFF34D399) else AdminAmber)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (user.isActive) "Active" else "Suspended",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (user.isActive) Color(0xFF34D399) else AdminAmber,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Unified Obsidian Glass Bottom Sheet to Suspend or Restore
        userToToggle?.let { u ->
            ModalBottomSheet(
                onDismissRequest = { userToToggle = null },
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
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .padding(bottom = 36.dp)
                ) {
                    Text(
                        text = if (u.isActive) "Suspend User Account" else "Restore User Account",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AdminWhite
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (u.isActive) {
                            "Suspending ${u.name} (${u.email}) will immediately revoke app access and block authentication until restored."
                        } else {
                            "Restoring ${u.name} (${u.email}) will re-enable login and access to their tracked lesion dashboard."
                        },
                        fontSize = 13.sp,
                        color = AdminTextMuted,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { userToToggle = null },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF))
                        ) {
                            Text("Cancel", color = AdminTextMuted)
                        }

                        Button(
                            onClick = {
                                val updated = repository.toggleUserStatus(u.id)
                                if (updated != null) {
                                    val actionName = if (updated.isActive) "restored" else "suspended"
                                    onShowToast("${u.name} $actionName")
                                }
                                userToToggle = null
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (u.isActive) AdminAmberBg else AdminButtonBg
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (u.isActive) AdminAmber.copy(alpha = 0.5f) else AdminButtonBorder
                            )
                        ) {
                            Text(
                                text = if (u.isActive) "Suspend" else "Restore",
                                color = if (u.isActive) AdminAmber else AdminWhite,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
