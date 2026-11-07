package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Routine
import com.example.model.SessionStatus
import com.example.ui.components.DiagnosticsDialog
import com.example.ui.components.HelpAboutDialog
import com.example.ui.components.NotificationSettingsDialog
import com.example.ui.components.PrizeChestDialog
import com.example.ui.components.QuickRoutineDialog
import com.example.ui.components.QuickTimerDialog
import com.example.ui.components.RoutineCompletionSummaryDialog
import com.example.ui.theme.DoneButtonMagenta
import com.example.ui.theme.SkyBlue
import com.example.ui.viewmodel.RoutineViewModel
import com.example.ui.viewmodel.Screen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: RoutineViewModel,
    modifier: Modifier = Modifier
) {
    val allRoutines by viewModel.allRoutines.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val wallTime by viewModel.currentWallTime.collectAsState()
    val currentUser by viewModel.authManager.currentUser.collectAsState()
    val earnedStars by viewModel.earnedStars.collectAsState()
    val starPrizes by viewModel.starPrizes.collectAsState()
    val runHistory by viewModel.runHistory.collectAsState()
    val familyCode by viewModel.familyCode.collectAsState()
    val taskCatalog by viewModel.taskCatalog.collectAsState()
    val completionSummary by viewModel.completionSummary.collectAsState()

    var showMenu by remember { mutableStateOf(false) }
    var showNotificationSettings by remember { mutableStateOf(false) }
    var showPrizeChest by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showQuickTimerDialog by remember { mutableStateOf(false) }
    var showQuickRoutineDialog by remember { mutableStateOf(false) }
    var showDiagnosticsDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SkyBlue)
            .windowInsetsPadding(WindowInsets.statusBars)
            .testTag("dashboard_screen")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Top Bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Child Routine Timer",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1E293B),
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Star Bank chip button
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFEF3C7), RoundedCornerShape(16.dp))
                                .clickable { showPrizeChest = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("star_bank_chip"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("⭐", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$earnedStars",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFB45309)
                                )
                            }
                        }

                        // Hamburger Menu in Top Right
                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                                    .testTag("dashboard_hamburger_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Menu options",
                                    tint = Color(0xFF1E293B)
                                )
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "⭐ Star Bank & Prizes ($earnedStars)",
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        showPrizeChest = true
                                    },
                                    modifier = Modifier.testTag("menu_star_bank_item")
                                )

                                DropdownMenuItem(
                                    text = {
                                        val syncLabel = if (currentUser != null) {
                                            if (familyCode.isNotEmpty()) "☁️ Family Sync ($familyCode)" else "☁️ Account Synced"
                                        } else {
                                            "☁️ Account & Family Sync"
                                        }
                                        Text(text = syncLabel, fontWeight = FontWeight.SemiBold)
                                    },
                                    onClick = {
                                        showMenu = false
                                        viewModel.navigateTo(Screen.Auth)
                                    },
                                    modifier = Modifier.testTag("menu_sync_item")
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "📚 Task Catalog (${taskCatalog.size})",
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        viewModel.navigateTo(Screen.TaskCatalog)
                                    },
                                    modifier = Modifier.testTag("menu_catalog_item")
                                )

                                HorizontalDivider()

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "🔔 Notification Settings",
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        showNotificationSettings = true
                                    },
                                    modifier = Modifier.testTag("menu_notification_settings_item")
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "ℹ️ Help & About",
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        showHelpDialog = true
                                    },
                                    modifier = Modifier.testTag("menu_help_item")
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "📋 Diagnostics & Sync Logs",
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        showDiagnosticsDialog = true
                                    },
                                    modifier = Modifier.testTag("menu_diagnostics_logs_item")
                                )
                            }
                        }
                    }
                }
            }

            // Live Family Routine in Progress Banner (if active)
            val currentActive = activeSession
            if (currentActive != null && (currentActive.status == SessionStatus.RUNNING || currentActive.status == SessionStatus.PAUSED)) {
                item {
                    val remSec = currentActive.stepRemainingSeconds.coerceAtLeast(0)
                    val remMinStr = String.format(Locale.getDefault(), "%d:%02d", remSec / 60, remSec % 60)

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.resumeSession() }
                            .testTag("live_family_session_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(if (currentActive.status == SessionStatus.PAUSED) Color(0xFFFBBF24) else Color(0xFF22C55E), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (currentActive.status == SessionStatus.PAUSED) "⏸️ PAUSED • LIVE ROUTINE" else "🟢 RUNNING • LIVE ROUTINE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (currentActive.status == SessionStatus.PAUSED) Color(0xFFFBBF24) else Color(0xFF4ADE80),
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = currentActive.routineTitle.ifEmpty { "Active Routine" },
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "$remMinStr remaining on this task",
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }

                            Button(
                                onClick = { viewModel.resumeSession() },
                                colors = ButtonDefaults.buttonColors(containerColor = DoneButtonMagenta),
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Join Stage ➜",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Minimal Digital Clock Card on Dashboard
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("minimal_clock_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val nowHour = wallTime.get(Calendar.HOUR_OF_DAY)
                        val nowMin = wallTime.get(Calendar.MINUTE)
                        val nowSec = wallTime.get(Calendar.SECOND)
                        val wallTimeStr = viewModel.formatTimeAmPm(String.format(Locale.getDefault(), "%02d:%02d", nowHour, nowMin))
                        val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
                        val dateStr = dateFormat.format(wallTime.time)

                        Column {
                            Text(
                                text = "CURRENT TIME",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B),
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = wallTimeStr,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = String.format(Locale.getDefault(), ":%02d", nowSec),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        // Today's Date Badge
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFE0F2FE), RoundedCornerShape(14.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = dateStr,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0369A1)
                            )
                        }
                    }
                }
            }

            // Quick Tools Bar: Quick Timer, Quick Routine, Task Catalog
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quick_tools_bar"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Quick Timer Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFFFEF3C7), RoundedCornerShape(14.dp))
                            .clickable { showQuickTimerDialog = true }
                            .padding(horizontal = 8.dp, vertical = 10.dp)
                            .testTag("open_quick_timer_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⏳", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Quick Timer",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // Quick Routine Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFFEEF2FF), RoundedCornerShape(14.dp))
                            .clickable { showQuickRoutineDialog = true }
                            .padding(horizontal = 8.dp, vertical = 10.dp)
                            .testTag("open_quick_routine_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🚀", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Quick Routine",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4338CA),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // Task Catalog Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFFECFDF5), RoundedCornerShape(14.dp))
                            .clickable { viewModel.navigateTo(Screen.TaskCatalog) }
                            .padding(horizontal = 8.dp, vertical = 10.dp)
                            .testTag("open_task_catalog_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📚", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Task Catalog",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF065F46),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            // "Your Routines" Section Header with "+ Add Routine" Button
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Your Routines",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "Tap a routine to view details, or start directly",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    // "+ Add Routine" Button
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.95f), RoundedCornerShape(14.dp))
                            .clickable { viewModel.navigateTo(Screen.CreateRoutine) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("dashboard_add_routine_btn")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Routine",
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Add Routine",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0284C7)
                            )
                        }
                    }
                }
            }

            // Routine Cards / "Routine Bubbles"
            if (allRoutines.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🌱", fontSize = 36.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No Routines Yet",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = "Create your morning or bedtime routine to get started!",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { viewModel.navigateTo(Screen.CreateRoutine) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Create First Routine", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                items(allRoutines) { routine ->
                    val currentActiveForCard = activeSession
                    RoutineBubbleCard(
                        routine = routine,
                        isCompletedToday = viewModel.isRoutineCompletedToday(routine.id),
                        isActiveSession = currentActiveForCard != null &&
                                (currentActiveForCard.status == SessionStatus.RUNNING || currentActiveForCard.status == SessionStatus.PAUSED) &&
                                currentActiveForCard.routineId == routine.id,
                        onOpenView = {
                            viewModel.openRoutineView(routine.id)
                        },
                        onStartRoutine = {
                            viewModel.selectRoutine(routine.id)
                            viewModel.startRoutine()
                        },
                        onResumeRoutine = {
                            viewModel.resumeSession()
                        },
                        formatTime = { viewModel.formatTimeAmPm(it) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Star Prize Chest Dialog
        if (showPrizeChest) {
            PrizeChestDialog(
                earnedStars = earnedStars,
                prizes = starPrizes,
                runHistory = runHistory,
                onRedeem = { viewModel.redeemPrize(it) },
                onAddPrize = { title, stars, type, emoji ->
                    viewModel.addCustomPrize(title, stars, type, emoji)
                },
                onDismiss = { showPrizeChest = false }
            )
        }

        // Help & App Info Dialog
        if (showHelpDialog) {
            HelpAboutDialog(
                userEmail = currentUser?.email,
                familyCode = familyCode,
                onDismiss = { showHelpDialog = false }
            )
        }

        // Quick Hourglass Timer Dialog
        if (showQuickTimerDialog) {
            QuickTimerDialog(
                onDismiss = { showQuickTimerDialog = false },
                onStartTimer = { title, duration, awardStars, starReward, iconKey ->
                    viewModel.startQuickTimer(title, duration, awardStars, starReward, iconKey)
                }
            )
        }

        // Quick Routine Builder Dialog
        if (showQuickRoutineDialog) {
            QuickRoutineDialog(
                catalogTasks = taskCatalog,
                onDismiss = { showQuickRoutineDialog = false },
                onAddNewCatalogTask = { viewModel.addCatalogTask(it) },
                onLaunchQuickRoutine = { title, targetDep, selectedTasks, startNow ->
                    viewModel.createQuickRoutine(title, targetDep, selectedTasks, startNow)
                }
            )
        }

        // Routine Completion Summary Dialog
        completionSummary?.let { summary ->
            RoutineCompletionSummaryDialog(
                summary = summary,
                onDismiss = { viewModel.dismissCompletionSummary() }
            )
        }

        // Notification Settings Dialog
        if (showNotificationSettings) {
            NotificationSettingsDialog(
                notificationHelper = viewModel.notificationHelper,
                onDismiss = { showNotificationSettings = false }
            )
        }

        // Diagnostics & Multi-Device Sync Logs Dialog
        if (showDiagnosticsDialog) {
            DiagnosticsDialog(
                viewModel = viewModel,
                onDismiss = { showDiagnosticsDialog = false }
            )
        }
    }
}

/**
 * Routine Bubble Card:
 * Displays routine title, schedule window, task count, and completion badge.
 * Tapping any part of the bubble opens RoutineViewScreen.
 * In-bubble "▶ Start" quick action button starts the routine directly.
 */
@Composable
fun RoutineBubbleCard(
    routine: Routine,
    isCompletedToday: Boolean,
    isActiveSession: Boolean,
    onOpenView: () -> Unit,
    onStartRoutine: () -> Unit,
    onResumeRoutine: () -> Unit,
    formatTime: (String) -> String,
    modifier: Modifier = Modifier
) {
    val startAmPm = formatTime(routine.startTime)
    val endAmPm = formatTime(routine.targetDepartureTime)
    val windowMins = routine.getWindowMinutes()

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onOpenView() }
            .testTag("routine_card_${routine.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Row 1: Routine Title + Completed Today / Active Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(
                        text = routine.title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B),
                        maxLines = 1
                    )
                }

                if (isActiveSession) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF22C55E).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "🟢 In Progress",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF15803D)
                        )
                    }
                } else if (isCompletedToday) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFDCFCE7), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("routine_badge_completed_${routine.id}")
                    ) {
                        Text(
                            text = "✅ Completed Today! 🌟",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF15803D)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Row 2: Window Times & Task Count
            Text(
                text = "$startAmPm - $endAmPm • $windowMins min window",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF475569)
            )

            Text(
                text = "${routine.activeSteps.size} tasks • ~${routine.nominalMinutes}m nominal duration",
                fontSize = 11.sp,
                color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Row 3: Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Secondary link: "View Details ➜"
                Text(
                    text = "View Details ➜",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0284C7)
                )

                // Primary Quick Action Button inside the bubble: "▶ Start Routine" (>= 48dp touch target)
                Button(
                    onClick = {
                        if (isActiveSession) {
                            onResumeRoutine()
                        } else {
                            onStartRoutine()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DoneButtonMagenta),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("start_routine_bubble_btn_${routine.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isActiveSession) "Resume" else "Start Routine",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
