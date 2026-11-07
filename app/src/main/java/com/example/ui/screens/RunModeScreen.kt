package com.example.ui.screens

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.SessionStatus
import com.example.ui.components.DiagnosticsDialog
import com.example.ui.components.HourglassTimerView
import com.example.ui.components.VerticalTimelineBar
import com.example.ui.theme.BonusAmber
import com.example.ui.theme.DoneButtonMagenta
import com.example.ui.theme.StageBackground
import com.example.ui.viewmodel.RoutineViewModel

import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.ui.draw.clip
import com.example.model.RoutineStep
import com.example.model.TimelineSizingMode
import com.example.ui.components.ManageTaskRunDialog
import com.example.ui.components.PrizeChestDialog
import com.example.ui.components.SpendBonusTimeDialog
import com.example.ui.components.TimePickerDialog
import com.example.ui.components.TimelineSizingDialog
import com.example.ui.components.TimelineStepIcon
import java.util.Calendar
import java.util.Locale
import android.widget.Toast

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RunModeScreen(
    viewModel: RoutineViewModel,
    modifier: Modifier = Modifier
) {
    val routine by viewModel.currentRoutine.collectAsState()
    val session by viewModel.activeSession.collectAsState()
    val wallCalendar by viewModel.currentWallTime.collectAsState()
    val earnedStars by viewModel.earnedStars.collectAsState()
    val starPrizes by viewModel.starPrizes.collectAsState()
    val runHistory by viewModel.runHistory.collectAsState()
    val taskCatalog by viewModel.taskCatalog.collectAsState()
    val isChildLockActive by viewModel.isChildLockActive.collectAsState()
    val timelineSizingMode by viewModel.timelineSizingMode.collectAsState()
    val context = LocalContext.current

    var showAdjustEndTimeDialog by remember { mutableStateOf(false) }
    var showPaceModeDialog by remember { mutableStateOf(false) }
    var showTaskPickerSheet by remember { mutableStateOf(false) }
    var showPrizeChest by remember { mutableStateOf(false) }
    var showStopConfirmDialog by remember { mutableStateOf(false) }
    var showSpendBonusDialog by remember { mutableStateOf(false) }
    var showTimelineSizingDialog by remember { mutableStateOf(false) }
    var showDiagnosticsDialog by remember { mutableStateOf(false) }
    var isTopSettingsExpanded by remember { mutableStateOf(false) }
    var managedStep by remember { mutableStateOf<RoutineStep?>(null) }

    // Keep screen awake while routine is running
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    LaunchedEffect(session?.status) {
        if (session?.status == SessionStatus.COMPLETED) {
            viewModel.finishRoutineAndShowSummary()
        }
    }

    BackHandler {
        viewModel.navigateBack()
    }

    val currentSession = session
    if (currentSession == null || currentSession.status == SessionStatus.COMPLETED) {
        Box(
            modifier = Modifier.fillMaxSize().background(StageBackground),
            contentAlignment = Alignment.Center
        ) {
            Button(onClick = { viewModel.startRoutine() }) {
                Text("Start Routine")
            }
        }
        return
    }

    val allActiveSteps = currentSession.getAllSteps(routine.activeSteps)
    val currentStep = if (currentSession.currentStepId.isNotEmpty()) {
        allActiveSteps.find { it.id == currentSession.currentStepId }
            ?: allActiveSteps.find { !currentSession.completedStepIds.contains(it.id) }
            ?: allActiveSteps.getOrNull(currentSession.currentStepIndex)
    } else {
        allActiveSteps.find { !currentSession.completedStepIds.contains(it.id) }
            ?: allActiveSteps.getOrNull(currentSession.currentStepIndex)
    } ?: allActiveSteps.firstOrNull() ?: return

    val bonusMin = currentSession.bonusTimeSeconds / 60
    val bonusSec = currentSession.bonusTimeSeconds % 60
    val bonusTimeFormatted = String.format(Locale.getDefault(), "%d:%02d", bonusMin, bonusSec)

    // Overtime tracking: when task time expires, screen stays on late task and depletes bonus time
    val isOvertime = currentSession.isOvertime
    val overtimeSec = currentSession.overtimeSeconds
    val overtimeFormatted = String.format(Locale.getDefault(), "%02d:%02d", overtimeSec / 60, overtimeSec % 60)

    // Formatted live current wall time: e.g. "7:40 PM"
    val nowHour = wallCalendar.get(Calendar.HOUR_OF_DAY)
    val nowMin = wallCalendar.get(Calendar.MINUTE)
    val currentWallFormatted = viewModel.formatTimeAmPm(String.format(Locale.getDefault(), "%02d:%02d", nowHour, nowMin))

    // Target end time formatted: e.g. "8:30 PM"
    val targetEndRaw = currentSession.targetEndTime.ifEmpty { routine.targetDepartureTime }
    val targetEndFormatted = viewModel.formatTimeAmPm(targetEndRaw)

    // Calculate remaining minutes until target end time
    val targetParts = targetEndRaw.split(":")
    val targetH = targetParts.getOrNull(0)?.toIntOrNull() ?: 20
    val targetM = targetParts.getOrNull(1)?.toIntOrNull() ?: 30
    val minutesUntilTarget = (targetH * 60 + targetM) - (nowHour * 60 + nowMin)

    val sandColor = try {
        Color(android.graphics.Color.parseColor(currentStep.colorHex))
    } catch (_: Exception) {
        Color(0xFFF59E0B)
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(StageBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
            .navigationBarsPadding()
            .testTag("run_mode_screen")
    ) {
        val (calcFinishRaw, deltaMins) = viewModel.getCalculatedFinishTime(currentSession, routine)
        val calcFinishFormatted = viewModel.formatTimeAmPm(calcFinishRaw)

        // Left Vertical Timeline Sidebar (Proportional stacked tabs + red line)
        VerticalTimelineBar(
            routine = routine,
            session = currentSession,
            onStepClick = { index ->
                // When Child Lock is active, timeline clicks are locked to prevent accidental navigation
                if (!isChildLockActive) {
                    viewModel.jumpToStep(index)
                }
            },
            calculatedFinishTime = calcFinishFormatted,
            deltaMinutes = deltaMins,
            onAdjustTargetClick = {
                if (!isChildLockActive) {
                    showAdjustEndTimeDialog = true
                }
            },
            onStepLongClick = { step, _ ->
                if (!isChildLockActive) {
                    managedStep = step
                }
            },
            sizingMode = timelineSizingMode,
            onHeaderClick = {
                if (!isChildLockActive) {
                    showTimelineSizingDialog = true
                }
            },
            modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp)
        )

        // Center Stage (Focus Area)
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            val availableHeight = maxHeight
            val availableWidth = maxWidth

            // Budget for non-hourglass elements (Header, Active Step Card, Timer text, Up Next button)
            val nonHourglassHeight = if (isTopSettingsExpanded) 420.dp else 295.dp
            val maxAllowedH = (availableHeight - nonHourglassHeight).coerceAtLeast(150.dp).coerceAtMost(520.dp)

            // Tablet screen optimization: Pixel Tablet width is 800dp-1280dp.
            // Allow width up to 430.dp on large tablets, or proportionally constrained on narrower phones.
            val maxAllowedW = (availableWidth * 0.72f).coerceAtLeast(140.dp).coerceAtMost(430.dp)
            val derivedHeightFromWidth = maxAllowedW / 0.82f
            val chosenHourglassHeight = minOf(maxAllowedH, derivedHeightFromWidth).coerceIn(150.dp, 520.dp)
            val chosenHourglassWidth = (chosenHourglassHeight * 0.82f).coerceIn(130.dp, maxAllowedW)

            val scrollState = rememberScrollState()
            val isConstrainedHeight = availableHeight < 480.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (isConstrainedHeight) Modifier.verticalScroll(scrollState)
                        else Modifier
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = if (isConstrainedHeight) Arrangement.spacedBy(8.dp) else Arrangement.SpaceBetween
            ) {
                // Top Header: Compact Live Bar with Expandable Settings Window
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp)
                        .padding(vertical = 2.dp)
                ) {
                // Sleek Compact Bar with FlowRow wrapping for narrow screens
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Left: Live Wall Time & Bonus Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = currentWallFormatted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A),
                            modifier = Modifier.testTag("live_wall_time"),
                            maxLines = 1,
                            softWrap = false
                        )
                        // Bonus / Overtime interactive pill: daughter can tap to spend bonus time on treats!
                        val effectiveBonusSeconds = viewModel.getEffectiveBonusTimeSeconds(currentSession, routine)
                        val isNegativeBonus = effectiveBonusSeconds < 0 || deltaMins > 0
                        val isBonusCountingDown = currentStep.id.startsWith("bonus_") || (currentSession.stepRemainingSeconds <= 0 && currentSession.bonusTimeSeconds > 0)

                        val (bonusBgColor, bonusTextColor) = when {
                            isNegativeBonus -> Color(0xFFDC2626) to Color.White
                            isBonusCountingDown -> Color(0xFFEAB308) to Color(0xFF78350F)
                            else -> Color(0xFF16A34A) to Color.White
                        }

                        val bonusDisplayText = if (isNegativeBonus) {
                            val deficitSeconds = if (effectiveBonusSeconds < 0) -effectiveBonusSeconds else (deltaMins * 60)
                            val defMin = deficitSeconds / 60
                            val defSec = deficitSeconds % 60
                            String.format(Locale.getDefault(), "-%d:%02d", defMin, defSec)
                        } else {
                            val bMin = effectiveBonusSeconds / 60
                            val bSec = effectiveBonusSeconds % 60
                            String.format(Locale.getDefault(), "✨ +%d:%02d", bMin, bSec)
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    bonusBgColor,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    showSpendBonusDialog = true
                                }
                                .padding(horizontal = 9.dp, vertical = 4.dp)
                                .testTag("bonus_time_badge")
                        ) {
                            Text(
                                text = bonusDisplayText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = bonusTextColor,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // Right: Child Lock Badge + Settings Expansion Toggle Button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isChildLockActive) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFFEE2E2), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Locked",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFDC2626)
                                    )
                                }
                            }
                        }

                        // Expansion Toggle Button (Hides/Shows all top settings)
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isTopSettingsExpanded) Color(0xFFE0F2FE) else Color.White.copy(alpha = 0.9f),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { isTopSettingsExpanded = !isTopSettingsExpanded }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("toggle_top_settings_btn")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isTopSettingsExpanded) Icons.Default.ExpandLess else Icons.Default.Tune,
                                    contentDescription = if (isTopSettingsExpanded) "Hide controls" else "Expand controls",
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isTopSettingsExpanded) "Hide Controls ▴" else "Controls ▾",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0284C7)
                                )
                            }
                        }
                    }
                }

                // Expanded Settings Window (Hidden by default to give maximum room to Hourglass!)
                if (isTopSettingsExpanded) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("executive_timing_bar")
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            // Section 1: Timing & Status Badges (Wraps onto multiple rows on narrow screens)
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Target End Time
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
                                        .clickable(enabled = !isChildLockActive) { showAdjustEndTimeDialog = true }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .testTag("target_end_time_badge")
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column {
                                            Text(
                                                text = "TARGET END",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF475569)
                                            )
                                            Text(
                                                text = targetEndFormatted,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF0284C7)
                                            )
                                        }
                                        if (!isChildLockActive) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Adjust",
                                                tint = Color(0xFF0284C7),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }

                                // Calculated Finish Box
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (deltaMins > 0) Color(0xFFFEE2E2) else Color(0xFFF0FDF4),
                                             RoundedCornerShape(10.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .testTag("calculated_finish_badge")
                                ) {
                                    Column {
                                        Text(
                                            text = "EST. FINISH",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (deltaMins > 0) Color(0xFF991B1B) else Color(0xFF166534)
                                        )
                                        Text(
                                            text = "$calcFinishFormatted ${if (deltaMins < 0) "(-${-deltaMins}m)" else if (deltaMins > 0) "(+${deltaMins}m)" else ""}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (deltaMins > 0) Color(0xFFDC2626) else Color(0xFF16A34A)
                                        )
                                    }
                                }

                                // Pace Mode Chip
                                Box(
                                    modifier = Modifier
                                        .background(
                                            when (currentSession.paceMode) {
                                                "RUSH" -> Color(0xFFFEE2E2)
                                                "RELAXED" -> Color(0xFFDCFCE7)
                                                else -> Color(0xFFE0F2FE)
                                            },
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable(enabled = !isChildLockActive) { showPaceModeDialog = true }
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                        .testTag("pace_mode_chip")
                                ) {
                                    Text(
                                        text = when (currentSession.paceMode) {
                                            "RUSH" -> "⚡ Rush"
                                            "RELAXED" -> "🌱 Relaxed"
                                            "NOMINAL" -> "⚖️ Nominal"
                                            else -> "🎯 Elastic"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (currentSession.paceMode) {
                                            "RUSH" -> Color(0xFFDC2626)
                                            "RELAXED" -> Color(0xFF16A34A)
                                            else -> Color(0xFF0284C7)
                                        }
                                    )
                                }

                                // Star Bank Chip
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFFEF3C7), RoundedCornerShape(10.dp))
                                        .clickable(enabled = !isChildLockActive) { showPrizeChest = true }
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                        .testTag("run_mode_star_chip")
                                ) {
                                    Text(
                                        text = "⭐ $earnedStars",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB45309)
                                    )
                                }

                                // Timeline Sizing Mode Chip
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFF3E8FF), RoundedCornerShape(10.dp))
                                        .clickable(enabled = !isChildLockActive) { showTimelineSizingDialog = true }
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                        .testTag("timeline_sizing_chip")
                                ) {
                                    Text(
                                        text = "${timelineSizingMode.iconEmoji} ${timelineSizingMode.shortName} ▾",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF7E22CE)
                                    )
                                }

                            }

                            // If running late, suggest skipping candidate upcoming steps
                            val upcomingNonImportant = routine.activeSteps
                                .drop(currentSession.currentStepIndex + 1)
                                .filter { !it.isImportant && !currentSession.completedStepIds.contains(it.id) }
                            if (deltaMins > 0 && upcomingNonImportant.isNotEmpty() && !isChildLockActive) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Running late (+${deltaMins}m). Skip:",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFDC2626)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        items(upcomingNonImportant) { stepToSkip ->
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0xFFFEE2E2), RoundedCornerShape(8.dp))
                                                    .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(8.dp))
                                                    .clickable { viewModel.toggleStepEnabled(stepToSkip.id) }
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "👁️ ${stepToSkip.title} (-${stepToSkip.defaultMinutes}m)",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFB91C1C)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Section 2: Timer Quick Adjustments & Action Controls (FlowRow drops items cleanly onto new rows)
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Pause / Resume Button
                                IconButton(
                                    onClick = { viewModel.pauseRoutine() },
                                    enabled = !isChildLockActive,
                                    modifier = Modifier.size(36.dp).testTag("pause_resume_btn")
                                ) {
                                    Icon(
                                        imageVector = if (currentSession.status == SessionStatus.PAUSED) Icons.Default.PlayArrow else Icons.Default.Pause,
                                        contentDescription = "Pause or Resume",
                                        tint = if (isChildLockActive) Color(0xFFCBD5E1) else Color(0xFF64748B),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // -1m Button
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isChildLockActive) Color(0xFFF1F5F9) else Color(0xFFF8FAFC),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable(enabled = !isChildLockActive) { viewModel.removeMinutes(1) }
                                        .padding(horizontal = 10.dp, vertical = 7.dp)
                                        .testTag("remove_one_min_btn")
                                ) {
                                    Text(
                                        text = "-1m",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isChildLockActive) Color(0xFF94A3B8) else Color(0xFF64748B),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }

                                // +2m Button
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isChildLockActive) Color(0xFFF1F5F9) else Color(0xFFF0F9FF),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable(enabled = !isChildLockActive) { viewModel.addExtraMinutes(2) }
                                        .padding(horizontal = 10.dp, vertical = 7.dp)
                                        .testTag("add_two_mins_btn")
                                ) {
                                    Text(
                                        text = "+2m",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isChildLockActive) Color(0xFF94A3B8) else Color(0xFF0284C7),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }

                                // Skip Current Task Button
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isChildLockActive) Color(0xFFF1F5F9) else Color(0xFFFEF2F2),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable(enabled = !isChildLockActive) { viewModel.skipCurrentStep() }
                                        .padding(horizontal = 10.dp, vertical = 7.dp)
                                        .testTag("skip_current_task_btn")
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.SkipNext,
                                            contentDescription = "Skip Current Task",
                                            tint = if (isChildLockActive) Color(0xFF94A3B8) else Color(0xFFDC2626),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "Skip",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isChildLockActive) Color(0xFF94A3B8) else Color(0xFFDC2626),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }

                                // Tasks Picker Chip
                                val completedCount = currentSession.completedStepIds.size
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isChildLockActive) Color(0xFFF1F5F9) else Color(0xFFF0F9FF),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable(enabled = !isChildLockActive) { showTaskPickerSheet = true }
                                        .padding(horizontal = 10.dp, vertical = 7.dp)
                                        .testTag("pick_task_btn")
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.FormatListBulleted,
                                            contentDescription = null,
                                            tint = if (isChildLockActive) Color(0xFF94A3B8) else Color(0xFF0284C7),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "$completedCount/${routine.activeSteps.size}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isChildLockActive) Color(0xFF94A3B8) else Color(0xFF0284C7),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }

                                // Child Lock Toggle
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isChildLockActive) Color(0xFFFEE2E2) else Color(0xFFF8FAFC),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { viewModel.toggleChildLock() }
                                        .padding(horizontal = 10.dp, vertical = 7.dp)
                                        .testTag("child_lock_btn")
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isChildLockActive) Icons.Default.Lock else Icons.Default.LockOpen,
                                            contentDescription = null,
                                            tint = if (isChildLockActive) Color(0xFFDC2626) else Color(0xFF64748B),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = if (isChildLockActive) "Locked" else "Lock",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isChildLockActive) Color(0xFFDC2626) else Color(0xFF64748B),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }

                                // Diagnostics & Sync Logs Button
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
                                        .clickable { showDiagnosticsDialog = true }
                                        .padding(horizontal = 10.dp, vertical = 7.dp)
                                        .testTag("open_diagnostics_logs_btn")
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Diagnostics Logs",
                                            tint = Color(0xFF475569),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "Logs",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF475569),
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

            // Active Step Focus Header: Icon badge, Step number, and Title
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
                    .testTag("active_step_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(sandColor.copy(alpha = 0.20f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        TimelineStepIcon(
                            iconKey = currentStep.iconKey,
                            tint = sandColor,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "TASK ${currentSession.currentStepIndex + 1} OF ${routine.activeSteps.size}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0284C7),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = currentStep.title,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0F172A),
                            modifier = Modifier.testTag("step_title")
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = if (isOvertime) {
                                "⚠️ Time Expired • Finish to move on (no star)"
                            } else {
                                "⭐ Finish before timer for ${if (currentStep.starReward > 0) currentStep.starReward else 1} Star!"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isOvertime) Color(0xFFDC2626) else Color(0xFFB45309)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Animated Hourglass Timer: scales dynamically up to 520dp on Pixel Tablet while keeping Up Next button 100% visible
            HourglassTimerView(
                remainingSeconds = viewModel.getDynamicStepRemainingSeconds(currentSession),
                totalSeconds = currentSession.stepTotalSeconds,
                sandColor = sandColor,
                width = chosenHourglassWidth,
                height = chosenHourglassHeight
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Identify the next step for the unified "Up Next" primary button using orderedStepIds and currentStepId
            val activeSteps = currentSession.getAllSteps(routine.activeSteps)
            val orderedSteps = remember(currentSession.orderedStepIds, activeSteps) {
                if (currentSession.orderedStepIds.isNotEmpty()) {
                    val map = activeSteps.associateBy { it.id }
                    currentSession.orderedStepIds.mapNotNull { map[it] }.ifEmpty { activeSteps }
                } else {
                    activeSteps
                }
            }
            val currentActiveIdx = remember(currentSession.currentStepId, currentSession.currentStepIndex, orderedSteps) {
                val idx = orderedSteps.indexOfFirst { it.id == currentSession.currentStepId }
                if (idx >= 0) idx else currentSession.currentStepIndex.coerceIn(0, (orderedSteps.size - 1).coerceAtLeast(0))
            }
            val nextTask = remember(currentActiveIdx, currentSession.completedStepIds, orderedSteps) {
                val uncompletedAfter = (currentActiveIdx + 1 until orderedSteps.size)
                    .firstOrNull { !currentSession.completedStepIds.contains(orderedSteps[it].id) }
                val nextIdx = uncompletedAfter
                    ?: (0 until currentActiveIdx)
                        .firstOrNull { !currentSession.completedStepIds.contains(orderedSteps[it].id) }
                if (nextIdx != null) orderedSteps[nextIdx] else null
            }

            // Bottom Section: Back/Stop links + Primary "Up Next" Action Button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Back & Stop (Hidden when Child Lock is active so child can only tap 'Up Next')
                if (isChildLockActive) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Child Lock Active • Only Up Next button is enabled",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF64748B),
                            modifier = Modifier.testTag("child_lock_indicator")
                        )
                    }
                } else {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Back",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .clickable { viewModel.previousStep() }
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                                .testTag("step_back_link")
                        )

                        Text(
                            text = "Skip Task",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFD97706),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .clickable { viewModel.skipCurrentStep() }
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                                .testTag("step_skip_link")
                        )

                        Text(
                            text = "Stop Routine",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFE11D48),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .clickable { showStopConfirmDialog = true }
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                                .testTag("step_stop_link")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Up Next Button dynamic color coding based on time left:
                // > 10s left: Green | <= 10s left: Yellow | Time expired: Red
                val upNextButtonColor = when {
                    currentSession.stepRemainingSeconds <= 0 -> Color(0xFFDC2626) // Red: Expired
                    currentSession.stepRemainingSeconds <= 10 -> Color(0xFFF59E0B) // Yellow: < 10s remaining
                    else -> Color(0xFF16A34A) // Green: Time remaining
                }
                val isYellowWarning = currentSession.stepRemainingSeconds in 1..10
                val upNextContentColor = if (isYellowWarning) Color(0xFF0F172A) else Color.White

                // Unified Primary "Up Next" Action Button + Parent Assistance Option
                // Replaces the old separate preview card and Done/Next button into one primary button!
                // Primary button remains 100% enabled and clickable when Child Lock is active.
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.96f)
                        .height(60.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.completeCurrentStepEarly(isParentAssisted = false) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = upNextButtonColor,
                            contentColor = upNextContentColor
                        ),
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .shadow(8.dp, RoundedCornerShape(28.dp))
                            .testTag("done_next_button")
                            .testTag("up_next_button")
                    ) {
                        if (nextTask != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (isYellowWarning) Color(0xFF0F172A).copy(alpha = 0.12f) else Color.White.copy(alpha = 0.25f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = when {
                                                currentSession.stepRemainingSeconds <= 0 -> "TIME'S UP ➜"
                                                isYellowWarning -> "HURRY ➜"
                                                else -> "UP NEXT ➜"
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = upNextContentColor
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    TimelineStepIcon(
                                        iconKey = nextTask.iconKey,
                                        tint = upNextContentColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = nextTask.title,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = upNextContentColor,
                                        maxLines = 1
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (nextTask.isImportant) {
                                        Text(
                                            text = "⭐ ${nextTask.starReward}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isYellowWarning) Color(0xFF78350F) else Color(0xFFFEF08A),
                                            modifier = Modifier.padding(end = 6.dp)
                                        )
                                    }
                                    Text(
                                        text = "${nextTask.defaultMinutes}m",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = upNextContentColor.copy(alpha = 0.9f)
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = if (currentSession.stepRemainingSeconds <= 0) "Finish Routine! ➜" else "Finish Routine! 🎉",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = upNextContentColor
                            )
                        }
                    }

                    // Secondary "🤝 Assisted" Button (Parent assisted completion)
                    // Hides when child lock is active so child can only tap the primary button
                    if (!isChildLockActive) {
                        Box(
                            modifier = Modifier
                                .width(64.dp)
                                .fillMaxHeight()
                                .shadow(4.dp, RoundedCornerShape(20.dp))
                                .background(Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
                                .clickable { viewModel.completeCurrentStepEarly(isParentAssisted = true) }
                                .padding(4.dp)
                                .testTag("parent_assisted_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("🤝", fontSize = 18.sp)
                                Text(
                                    "Assisted",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF475569)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }

        // Dialog: Adjust Target End Time
        if (showAdjustEndTimeDialog) {
            TimePickerDialog(
                title = "Adjust Target End Time",
                initialTimeString = targetEndRaw,
                onConfirm = { newTime ->
                    viewModel.adjustTargetEndTime(newTime)
                    showAdjustEndTimeDialog = false
                },
                onDismiss = { showAdjustEndTimeDialog = false }
            )
        }

        // Dialog: Select Pace Mode (Elastic, Rush, Nominal, Relaxed)
        if (showPaceModeDialog) {
            Dialog(onDismissRequest = { showPaceModeDialog = false }) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Routine Timing & Pace",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Target End: $targetEndFormatted ($minutesUntilTarget mins remaining)",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        val modes = listOf(
                            Triple("ELASTIC", "🎯 Elastic to End Time", "Scales remaining tasks to end at $targetEndFormatted"),
                            Triple("RUSH", "⚡ Rush / Express Pace", "Minimum task times (${routine.rushMinutes}m total)"),
                            Triple("NOMINAL", "⚖️ Nominal Pace", "Standard scheduled times (${routine.nominalMinutes}m total)"),
                            Triple("RELAXED", "🌱 Relaxed Pace", "Generous relaxed times (${routine.relaxedMinutes}m total)")
                        )

                        modes.forEach { (modeKey, title, subtitle) ->
                            val isSelected = currentSession.paceMode == modeKey
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(
                                        if (isSelected) Color(0xFFE0F2FE) else Color(0xFFF8FAFC),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        viewModel.setPaceMode(modeKey)
                                        showPaceModeDialog = false
                                    }
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text(
                                        text = title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color(0xFF0284C7) else Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = subtitle,
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { showPaceModeDialog = false },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Close")
                        }
                    }
                }
            }
        }

        // Dialog: Out of Order Task Picker (allows jumping to any task or toggling completion)
        if (showTaskPickerSheet) {
            Dialog(onDismissRequest = { showTaskPickerSheet = false }) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Choose Any Activity",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "Tap to switch to that task directly",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        val allSessionSteps = remember(currentSession.bonusSteps, routine.activeSteps) {
                            currentSession.getAllSteps(routine.activeSteps)
                        }
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().height(320.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(allSessionSteps) { index, step ->
                                val isCurrent = step.id == currentSession.currentStepId
                                val isDone = currentSession.completedStepIds.contains(step.id)
                                val durationSec = currentSession.getStepDurationSeconds(step.id)
                                val durationMin = durationSec / 60

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (isCurrent) Color(0xFFE0F2FE) else Color(0xFFF8FAFC),
                                            RoundedCornerShape(14.dp)
                                        )
                                        .clickable {
                                            viewModel.jumpToStepById(step.id)
                                            showTaskPickerSheet = false
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier.size(30.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            TimelineStepIcon(
                                                iconKey = step.iconKey,
                                                tint = try {
                                                    Color(android.graphics.Color.parseColor(step.colorHex))
                                                } catch (_: Exception) {
                                                    Color(0xFF0284C7)
                                                }
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = step.title,
                                                    fontSize = 14.sp,
                                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                                    color = Color(0xFF1E293B)
                                                )
                                                if (step.isImportant) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "⭐ ${step.starReward}",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFB45309)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "$durationMin min",
                                                fontSize = 11.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                    }

                                    // Checkmark button (tap to toggle completion)
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .background(
                                                if (isDone) Color(0xFF16A34A) else Color(0xFFE2E8F0),
                                                CircleShape
                                            )
                                            .clickable {
                                                viewModel.toggleStepCompletion(step.id)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isDone) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Completed",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { showTaskPickerSheet = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Done")
                        }
                    }
                }
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

        // Dialog: Spend Bonus Time (e.g. Cuddle with Mom, Put on Lotion, Read Story)
        if (showSpendBonusDialog) {
            val effBonus = viewModel.getEffectiveBonusTimeSeconds(currentSession, routine)
            SpendBonusTimeDialog(
                bonusTimeSeconds = effBonus.coerceAtLeast(0),
                catalogTasks = taskCatalog,
                onSpendBonus = { title, minutes, iconKey, colorHex, catalogTaskId, startImmediately ->
                    viewModel.addBonusActivity(
                        title = title,
                        durationMinutes = minutes,
                        iconKey = iconKey,
                        colorHex = colorHex,
                        catalogTaskId = catalogTaskId,
                        startImmediately = startImmediately
                    )
                },
                onGrantBonus = { minutes ->
                    viewModel.grantBonusMinutes(minutes)
                },
                onDismiss = { showSpendBonusDialog = false }
            )
        }

        // Dialog: Diagnostics and Multi-Device Sync Logs
        if (showDiagnosticsDialog) {
            DiagnosticsDialog(
                viewModel = viewModel,
                onDismiss = { showDiagnosticsDialog = false }
            )
        }

        // Dialog: Timeline Bar Sizing Combinations (Balanced, Equal, Proportional, Focus)
        if (showTimelineSizingDialog) {
            TimelineSizingDialog(
                currentMode = timelineSizingMode,
                onSelectMode = { mode ->
                    viewModel.setTimelineSizingMode(mode)
                },
                onDismiss = { showTimelineSizingDialog = false }
            )
        }

        // Dialog: Confirm Stop Routine with 3-second countdown safety lock
        if (showStopConfirmDialog) {
            StopRoutineConfirmDialog(
                routineTitle = routine.title,
                onConfirmStop = {
                    showStopConfirmDialog = false
                    viewModel.stopRoutine()
                },
                onDismiss = {
                    showStopConfirmDialog = false
                }
            )
        }

        // Dialog: Manage Task (Long-press on vertical timeline bar)
        managedStep?.let { step ->
            val isStepDone = currentSession.completedStepIds.contains(step.id)
            val isStepAssisted = currentSession.assistedStepIds.contains(step.id)
            val isCur = currentStep.id == step.id
            val isStepEnabled = step.isEnabled

            ManageTaskRunDialog(
                step = step,
                isCompleted = isStepDone,
                isAssisted = isStepAssisted,
                isCurrentStep = isCur,
                isEnabled = isStepEnabled,
                onToggleDone = { done ->
                    viewModel.setStepCompletionStatus(
                        stepId = step.id,
                        isDone = done,
                        isAssisted = isStepAssisted
                    )
                },
                onToggleAssisted = { assisted ->
                    viewModel.setStepCompletionStatus(
                        stepId = step.id,
                        isDone = true,
                        isAssisted = assisted
                    )
                },
                onToggleSkipToday = {
                    viewModel.toggleStepEnabled(step.id)
                    managedStep = null
                },
                onJumpToStep = {
                    viewModel.jumpToStepById(step.id)
                    managedStep = null
                },
                onDismiss = { managedStep = null }
            )
        }
    }
}

@Composable
fun StopRoutineConfirmDialog(
    routineTitle: String,
    onConfirmStop: () -> Unit,
    onDismiss: () -> Unit
) {
    var countdownSeconds by remember { mutableIntStateOf(3) }

    LaunchedEffect(Unit) {
        while (countdownSeconds > 0) {
            kotlinx.coroutines.delay(1000)
            countdownSeconds--
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("stop_routine_confirm_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Warning Icon in circle
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(Color(0xFFFEE2E2), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🛑", fontSize = 28.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Stop Routine?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Are you sure you want to exit $routineTitle? Completed tasks are saved to your family history.",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Actions: Keep Going (Primary, Safe) vs Stop (Delayed 3s button)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // "Keep Going" Button (Always immediately clickable)
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("confirm_keep_going_btn")
                    ) {
                        Text(
                            text = "Keep Going ➜",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // "Stop Routine" Button with 3-second countdown lock
                    Button(
                        onClick = {
                            if (countdownSeconds == 0) {
                                onConfirmStop()
                            }
                        },
                        enabled = countdownSeconds == 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDC2626),
                            disabledContainerColor = Color(0xFFE2E8F0),
                            disabledContentColor = Color(0xFF94A3B8)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("confirm_stop_routine_btn")
                    ) {
                        Text(
                            text = if (countdownSeconds > 0) "Stop Routine in ${countdownSeconds}s" else "Yes, Stop Routine",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (countdownSeconds == 0) Color.White else Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }
    }
}
