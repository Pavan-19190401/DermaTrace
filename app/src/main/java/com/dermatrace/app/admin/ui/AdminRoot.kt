package com.dermatrace.app.admin.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import com.dermatrace.app.admin.data.repository.AdminRepository
import com.dermatrace.app.admin.ui.components.AdminBottomNav
import com.dermatrace.app.admin.ui.components.AdminToastBanner
import com.dermatrace.app.admin.ui.components.AdminTopBar
import com.dermatrace.app.admin.ui.screens.*
import com.dermatrace.app.admin.ui.theme.AdminBg
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AdminRoot(
    repository: AdminRepository,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var currentTab by remember { mutableStateOf("staffing") }
    var currentSubScreen by remember { mutableStateOf<String?>(null) } // "review_queue" or "audit_log"
    var toastMessage by remember { mutableStateOf("") }
    var isNavVisible by remember { mutableStateOf(false) }

    // Intercept back swipe/press: navigate within admin hierarchy rather than quitting to sign in
    BackHandler(enabled = true) {
        if (currentSubScreen != null) {
            currentSubScreen = null
        } else if (currentTab != "staffing") {
            currentTab = "staffing"
        } else {
            // Keep admin session active and minimize app safely
            activity?.moveTaskToBack(true)
        }
    }

    val scope = rememberCoroutineScope()

    // Trigger pop-up entrance animation for the bottom navigation bar matching patient app (pn-in)
    LaunchedEffect(Unit) {
        delay(80)
        isNavVisible = true
    }

    fun showToast(msg: String) {
        toastMessage = msg
        scope.launch {
            delay(2400)
            if (toastMessage == msg) {
                toastMessage = ""
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AdminBg)
    ) {
        AnimatedContent(
            targetState = currentSubScreen,
            transitionSpec = {
                if (targetState != null) {
                    (slideInVertically(
                        initialOffsetY = { 80 },
                        animationSpec = tween(280, easing = FastOutSlowInEasing)
                    ) + fadeIn(tween(280))).togetherWith(
                        slideOutVertically(
                            targetOffsetY = { -40 },
                            animationSpec = tween(200)
                        ) + fadeOut(tween(200))
                    )
                } else {
                    (slideInVertically(
                        initialOffsetY = { -40 },
                        animationSpec = tween(280, easing = FastOutSlowInEasing)
                    ) + fadeIn(tween(280))).togetherWith(
                        slideOutVertically(
                            targetOffsetY = { 80 },
                            animationSpec = tween(200)
                        ) + fadeOut(tween(200))
                    )
                }
            },
            label = "ScreenTransition"
        ) { subScreen ->
            when (subScreen) {
                "review_queue" -> {
                    ReviewQueueScreen(
                        repository = repository,
                        onBack = { currentSubScreen = null },
                        onShowToast = { showToast(it) }
                    )
                }
                "audit_log" -> {
                    AuditLogScreen(
                        repository = repository,
                        onBack = { currentSubScreen = null }
                    )
                }
                else -> {
                    Scaffold(
                        topBar = {
                            val subtitle = when (currentTab) {
                                "staffing" -> "STAFFING & ROTA"
                                "overview" -> "SYSTEM & MODEL HEALTH"
                                "users" -> "USER DIRECTORY & ACCESS"
                                "profile" -> "ADMINISTRATOR PREFERENCES"
                                else -> "CLINICAL TRIAGE"
                            }
                            AdminTopBar(
                                title = "Admin workspace",
                                subtitle = subtitle,
                                onAvatarClick = { currentTab = "profile" }
                            )
                        },
                        bottomBar = {
                            // Pop-up bottom navigation bar matching patient app .pn-in animation
                            AnimatedVisibility(
                                visible = isNavVisible,
                                enter = slideInVertically(
                                    initialOffsetY = { it * 2 },
                                    animationSpec = spring(
                                        dampingRatio = 0.72f,
                                        stiffness = Spring.StiffnessLow
                                    )
                                ) + fadeIn(animationSpec = tween(350))
                            ) {
                                AdminBottomNav(
                                    currentTab = currentTab,
                                    onTabSelected = { currentTab = it }
                                )
                            }
                        },
                        containerColor = AdminBg
                    ) { innerPadding ->
                        val adminTabs = remember { listOf("staffing", "overview", "users", "profile") }
                        var dragAccumulatorX by remember { mutableFloatStateOf(0f) }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .pointerInput(currentTab) {
                                    detectHorizontalDragGestures(
                                        onDragStart = { dragAccumulatorX = 0f },
                                        onDragEnd = {
                                            val currentIndex = adminTabs.indexOf(currentTab)
                                            if (dragAccumulatorX < -45f && currentIndex < adminTabs.lastIndex) {
                                                // Swiped Left -> Advance to next section smoothly at 120 FPS
                                                currentTab = adminTabs[currentIndex + 1]
                                            } else if (dragAccumulatorX > 45f && currentIndex > 0) {
                                                // Swiped Right -> Return to previous section smoothly at 120 FPS
                                                currentTab = adminTabs[currentIndex - 1]
                                            }
                                            dragAccumulatorX = 0f
                                        },
                                        onDragCancel = { dragAccumulatorX = 0f },
                                        onHorizontalDrag = { _, dragAmount ->
                                            dragAccumulatorX += dragAmount
                                        }
                                    )
                                }
                        ) {
                            AnimatedContent(
                                targetState = currentTab,
                                transitionSpec = {
                                    val initialIdx = adminTabs.indexOf(initialState)
                                    val targetIdx = adminTabs.indexOf(targetState)
                                    if (targetIdx >= initialIdx) {
                                        (slideInHorizontally(
                                            initialOffsetX = { it },
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                        ) + fadeIn(tween(220))).togetherWith(
                                            slideOutHorizontally(
                                                targetOffsetX = { -it / 3 },
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                                    stiffness = Spring.StiffnessMediumLow
                                                )
                                            ) + fadeOut(tween(180))
                                        )
                                    } else {
                                        (slideInHorizontally(
                                            initialOffsetX = { -it },
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = Spring.StiffnessMediumLow
                                            )
                                        ) + fadeIn(tween(220))).togetherWith(
                                            slideOutHorizontally(
                                                targetOffsetX = { it / 3 },
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                                    stiffness = Spring.StiffnessMediumLow
                                                )
                                            ) + fadeOut(tween(180))
                                        )
                                    }
                                },
                                label = "TabTransition"
                            ) { tab ->
                                when (tab) {
                                    "staffing" -> StaffingBoardScreen(
                                        repository = repository,
                                        onNavigateReviewQueue = { currentSubScreen = "review_queue" },
                                        onShowToast = { showToast(it) }
                                    )
                                    "overview" -> OverviewScreen(
                                        repository = repository,
                                        onShowToast = { showToast(it) }
                                    )
                                    "users" -> UsersScreen(
                                        repository = repository,
                                        onShowToast = { showToast(it) }
                                    )
                                    "profile" -> ProfileScreen(
                                        repository = repository,
                                        onNavigateAuditLog = { currentSubScreen = "audit_log" },
                                        onSignOut = onSignOut,
                                        onShowToast = { showToast(it) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Toast Banner
        AdminToastBanner(
            message = toastMessage,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
