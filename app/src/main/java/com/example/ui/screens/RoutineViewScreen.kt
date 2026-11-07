package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.model.PaceCategory
import com.example.model.RoutineStep
import com.example.model.SessionStatus
import com.example.ui.components.TaskEditDialog
import com.example.ui.components.TimePickerDialog
import com.example.ui.components.TimelineStepIcon
import com.example.ui.theme.DoneButtonMagenta
import com.example.ui.theme.SkyBlue
import com.example.ui.viewmodel.RoutineViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RoutineViewScreen(
    viewModel: RoutineViewModel,
    modifier: Modifier = Modifier
) {
    val currentRoutine by viewModel.currentRoutine.collectAsState()
    val allRoutines by viewModel.allRoutines.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val taskCatalog by viewModel.taskCatalog.collectAsState()
    val preLaunchTargetTime by viewModel.preLaunchTargetTime.collectAsState()

    var editingStep by remember { mutableStateOf<RoutineStep?>(null) }
    var showPreLaunchTimePicker by remember { mutableStateOf(false) }

    val isCompletedToday = viewModel.isRoutineCompletedToday(currentRoutine.id)

    BackHandler {
        viewModel.navigateBack()
    }

    val windowMins = currentRoutine.getWindowMinutes()
    val bufferMins = currentRoutine.getBufferMinutes()
    val startAmPm = viewModel.formatTimeAmPm(currentRoutine.startTime)
    val effectiveTargetTime = preLaunchTargetTime ?: currentRoutine.targetDepartureTime
    val endAmPm = viewModel.formatTimeAmPm(effectiveTargetTime)

    val paceCoach = viewModel.getPaceCoachState(currentRoutine, effectiveTargetTime)

    // Calculate target end times for each active step in sequence
    val stepEndTimes = remember(currentRoutine, effectiveTargetTime) {
        viewModel.calculateTaskTargetEndTimes(currentRoutine, currentRoutine.startTime).toMap()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SkyBlue)
            .windowInsetsPadding(WindowInsets.statusBars)
            .testTag("routine_view_screen")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Bar with Back Button, Routine Title, and Edit Routine Button
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = { viewModel.navigateBack() },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                                .testTag("routine_view_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Dashboard",
                                tint = Color(0xFF1E293B)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = currentRoutine.title,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF1E293B),
                                maxLines = 1
                            )
                            Text(
                                text = "$startAmPm - $endAmPm • $windowMins min window",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF334155)
                            )
                        }
                    }

                    // Edit Routine Button (opens EditRoutineScreen)
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.95f), RoundedCornerShape(14.dp))
                            .clickable { viewModel.openEditRoutineFromRoutineView() }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("routine_view_edit_routine_btn")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Routine",
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Edit",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0284C7)
                            )
                        }
                    }
                }
            }

            // Completed Today Celebration Banner (if completed today)
            if (isCompletedToday) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF86EFAC)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("routine_completed_today_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🎉", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Completed Today! 🌟",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF15803D)
                                )
                                Text(
                                    text = "This routine has been finished for today. Great job keeping on track!",
                                    fontSize = 11.sp,
                                    color = Color(0xFF166534)
                                )
                            }
                        }
                    }
                }
            }

            // Three-Way Pace Breakdown (Nominal, Rush, Relaxed)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFFE0F2FE), RoundedCornerShape(12.dp))
                            .padding(vertical = 8.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("NOMINAL", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                            Text(
                                text = "${currentRoutine.nominalMinutes}m ${if (bufferMins > 0) "(+$bufferMins buffer)" else ""}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0369A1)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFFFEE2E2), RoundedCornerShape(12.dp))
                            .padding(vertical = 8.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("RUSH", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                            Text(
                                text = "${currentRoutine.rushMinutes}m min",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFB91C1C)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFFDCFCE7), RoundedCornerShape(12.dp))
                            .padding(vertical = 8.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("RELAXED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                            Text(
                                text = "${currentRoutine.relaxedMinutes}m max",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF15803D)
                            )
                        }
                    }
                }
            }

            // Target Departure & Pace Coach Card
            // CRITICAL RULE: If the routine was already completed for the day, DO NOT show red over budget!
            item {
                if (isCompletedToday) {
                    // Friendly celebratory pace state without red warnings
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF86EFAC)),
                        modifier = Modifier.fillMaxWidth().testTag("pace_coach_completed_card")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF16A34A),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Target Departure: $endAmPm",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF15803D)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF16A34A), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "✅ Done Today",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "All routine objectives accomplished for today. You can still run again anytime if needed!",
                                fontSize = 11.sp,
                                color = Color(0xFF334155)
                            )
                        }
                    }
                } else {
                    // Live Pre-Launch Pace Coach Card
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when (paceCoach.paceCategory) {
                                PaceCategory.OVER_BUDGET -> Color(0xFFFEF2F2)
                                PaceCategory.RUSH -> Color(0xFFFFFBEB)
                                PaceCategory.RELAXED -> Color(0xFFF0FDF4)
                                PaceCategory.NOMINAL -> Color(0xFFF0F9FF)
                            }
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            when (paceCoach.paceCategory) {
                                PaceCategory.OVER_BUDGET -> Color(0xFFF87171)
                                PaceCategory.RUSH -> Color(0xFFFBBF24)
                                PaceCategory.RELAXED -> Color(0xFF86EFAC)
                                PaceCategory.NOMINAL -> Color(0xFF7DD3FC)
                            }
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("pace_coach_card")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Clickable Target Departure Chip
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(Color.White, RoundedCornerShape(12.dp))
                                        .clickable { showPreLaunchTimePicker = true }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                        .testTag("routine_view_target_departure_chip")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = Color(0xFF0284C7),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = "TARGET DEPARTURE",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF64748B)
                                        )
                                        Text(
                                            text = endAmPm,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF0F172A)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit departure time",
                                        tint = Color(0xFF0284C7),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }

                                // Pace Badge
                                val (paceLabel, paceBg, paceTextColor) = when (paceCoach.paceCategory) {
                                    PaceCategory.OVER_BUDGET -> Triple("⚠️ Over Budget", Color(0xFFDC2626), Color.White)
                                    PaceCategory.RUSH -> Triple("⚡ Rush Pace", Color(0xFFD97706), Color.White)
                                    PaceCategory.RELAXED -> Triple("🌱 Relaxed", Color(0xFF16A34A), Color.White)
                                    PaceCategory.NOMINAL -> Triple("⚖️ Normal Speed", Color(0xFF0284C7), Color.White)
                                }
                                Box(
                                    modifier = Modifier
                                        .background(paceBg, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                        .testTag("routine_view_pace_badge")
                                ) {
                                    Text(
                                        text = paceLabel,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = paceTextColor
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = paceCoach.coachingMessage,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF334155)
                            )

                            // If Over Budget: Suggest tasks to toggle off with 1 tap
                            if (paceCoach.paceCategory == PaceCategory.OVER_BUDGET && paceCoach.suggestedTasksToSkip.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Short on time by ${paceCoach.deltaMinutes}m! Tap to skip for today:",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB91C1C)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(paceCoach.suggestedTasksToSkip) { candidateStep ->
                                        Box(
                                            modifier = Modifier
                                                .background(Color.White, RoundedCornerShape(10.dp))
                                                .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(10.dp))
                                                .clickable { viewModel.toggleStepEnabled(candidateStep.id) }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "👁️ Skip ${candidateStep.title}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFB91C1C)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "(-${candidateStep.defaultMinutes}m)",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFF64748B)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Start / Resume Routine Button
            item {
                val currentActiveSession = activeSession
                val isCurrentlyActive = currentActiveSession != null &&
                        (currentActiveSession.status == SessionStatus.RUNNING || currentActiveSession.status == SessionStatus.PAUSED) &&
                        currentActiveSession.routineId == currentRoutine.id

                Button(
                    onClick = {
                        if (isCurrentlyActive) {
                            viewModel.resumeSession()
                        } else {
                            viewModel.startRoutine()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DoneButtonMagenta),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .shadow(6.dp, RoundedCornerShape(20.dp))
                        .testTag("routine_view_start_routine_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            isCurrentlyActive -> "Resume ${currentRoutine.title}"
                            isCompletedToday -> "Restart ${currentRoutine.title}"
                            else -> "Start ${currentRoutine.title}"
                        },
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Tasks Section Header with "Customize Tasks & Order" Button
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Tasks Sequence",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFE0F2FE), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${currentRoutine.activeSteps.size}/${currentRoutine.steps.size} Active",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0369A1)
                                )
                            }
                        }
                        Text(
                            text = "Tap 👁️ to toggle for today • Press & hold to edit",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                    }

                    // Button to open TaskListScreen to reorder / add tasks
                    Button(
                        onClick = { viewModel.openTaskListFromRoutineView() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("routine_view_customize_tasks_btn")
                    ) {
                        Text(
                            text = "Add / Order ➜",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Tasks Sequence List with Individual Target End Times and Long-Press Edit
            items(currentRoutine.steps) { step ->
                val isEnabled = step.isEnabled
                val isCompleted = activeSession?.completedStepIds?.contains(step.id) == true
                val targetEndTime = stepEndTimes[step]

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isEnabled) Color.White else Color(0xFFF1F5F9).copy(alpha = 0.8f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isEnabled) 2.dp else 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {
                                // Tap toggles or gives info; long-press opens edit dialog
                            },
                            onLongClick = {
                                editingStep = step
                            }
                        )
                        .testTag("routine_view_task_${step.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier.size(36.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                TimelineStepIcon(
                                    iconKey = step.iconKey,
                                    tint = try {
                                        if (isEnabled) {
                                            Color(android.graphics.Color.parseColor(step.colorHex))
                                        } else {
                                            Color(0xFF94A3B8)
                                        }
                                    } catch (_: Exception) {
                                        Color(0xFFFBBF24)
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = step.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isEnabled) Color(0xFF1E293B) else Color(0xFF64748B)
                                    )
                                    if (!isEnabled) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "Skipped today",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                    } else if (step.isImportant) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFFFEF3C7), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "⭐ ${step.starReward}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFB45309)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = if (isEnabled) {
                                        "${step.defaultMinutes}m nominal • ⚡${step.minMinutes}m rush • 🌱${step.maxMinutes}m relaxed"
                                    } else {
                                        "${step.defaultMinutes}m (excluded from this run)"
                                    },
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )

                                // Individual Target End Time for this task
                                if (isEnabled && targetEndTime != null) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "🎯 Target End: $targetEndTime",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0284C7)
                                    )
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Inclusion/Skip Eye Toggle Button (e.g. skip breakfast today)
                            IconButton(
                                onClick = { viewModel.toggleStepEnabled(step.id) },
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("routine_view_toggle_${step.id}")
                            ) {
                                Icon(
                                    imageVector = if (isEnabled) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (isEnabled) "Included (Tap to skip)" else "Skipped (Tap to include)",
                                    tint = if (isEnabled) Color(0xFF0284C7) else Color(0xFF94A3B8),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Completion status checkmark if routine is active
                            if (isCompleted) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .background(Color(0xFF4ADE80), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("✓", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Task Edit Dialog on Long Press
        editingStep?.let { step ->
            TaskEditDialog(
                initialStep = step,
                catalogTasks = taskCatalog,
                allRoutines = allRoutines,
                onQuickAddCatalogTask = { viewModel.addCatalogTask(it) },
                onSave = { updatedStep ->
                    val updatedSteps = currentRoutine.steps.map {
                        if (it.id == updatedStep.id) updatedStep else it
                    }
                    viewModel.updateRoutine(currentRoutine.copy(steps = updatedSteps))

                    // If linked to catalog item, sync changes to catalog
                    val targetCatId = updatedStep.catalogTaskId
                        ?: taskCatalog.firstOrNull { it.title.equals(step.title, ignoreCase = true) }?.id
                    if (targetCatId != null) {
                        val existingCat = taskCatalog.firstOrNull { it.id == targetCatId }
                        if (existingCat != null) {
                            val updatedCat = existingCat.copy(
                                title = updatedStep.title,
                                iconKey = updatedStep.iconKey,
                                colorHex = updatedStep.colorHex,
                                defaultMinutes = updatedStep.defaultMinutes,
                                minMinutes = updatedStep.minMinutes,
                                maxMinutes = updatedStep.maxMinutes,
                                isImportant = updatedStep.isImportant,
                                starReward = updatedStep.starReward
                            )
                            viewModel.addCatalogTask(updatedCat)
                        }
                    }

                    editingStep = null
                },
                onDismiss = { editingStep = null }
            )
        }

        // Pre-launch Target Departure Time Picker Dialog
        if (showPreLaunchTimePicker) {
            TimePickerDialog(
                title = "Target Departure Time",
                initialTimeString = effectiveTargetTime,
                onConfirm = { newTime ->
                    viewModel.setPreLaunchTargetTime(newTime)
                    showPreLaunchTimePicker = false
                },
                onDismiss = { showPreLaunchTimePicker = false }
            )
        }
    }
}
