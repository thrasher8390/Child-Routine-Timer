package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.ui.components.RepeatScheduleDialog
import com.example.ui.components.TimePickerDialog
import com.example.ui.theme.DeleteCoral
import com.example.ui.theme.SaveNavy
import com.example.ui.theme.SkyBlue
import com.example.ui.viewmodel.RoutineViewModel
import com.example.ui.viewmodel.Screen

@Composable
fun EditRoutineScreen(
    viewModel: RoutineViewModel,
    modifier: Modifier = Modifier
) {
    val routine by viewModel.currentRoutine.collectAsState()

    var title by remember(routine.title) { mutableStateOf(routine.title) }
    var startTime by remember(routine.startTime) { mutableStateOf(routine.startTime) }
    var targetDepartureTime by remember(routine.targetDepartureTime) { mutableStateOf(routine.targetDepartureTime) }
    var reminderEnabled by remember(routine.reminderEnabled) { mutableStateOf(routine.reminderEnabled) }
    var repeatType by remember(routine.repeatType) { mutableStateOf(routine.repeatType) }
    var isEditingTitle by remember { mutableStateOf(false) }

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showRepeatDialog by remember { mutableStateOf(false) }

    BackHandler {
        viewModel.navigateBack()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SkyBlue)
            .windowInsetsPadding(WindowInsets.statusBars)
            .testTag("edit_routine_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.navigateBack() },
                    modifier = Modifier.testTag("edit_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF1E293B)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "Edit Routine",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            }

            // Routine Title Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isEditingTitle = !isEditingTitle }
                    .testTag("edit_title_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (isEditingTitle) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Text(
                            text = title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                    }

                    IconButton(onClick = { isEditingTitle = !isEditingTitle }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit title",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Details Settings Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().testTag("routine_details_card")
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    SettingRowItem(
                        label = "Start time",
                        value = startTime,
                        onClick = {
                            showStartTimePicker = true
                        }
                    )
                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    SettingRowItem(
                        label = "Target End / Departure",
                        value = targetDepartureTime,
                        onClick = {
                            showEndTimePicker = true
                        }
                    )
                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    SettingRowItem(
                        label = "Planned Duration",
                        value = "${routine.totalMinutes} minutes",
                        onClick = { viewModel.navigateTo(Screen.TaskList) }
                    )
                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    // Reminder notification Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Reminder notification",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E293B)
                        )
                        Switch(
                            checked = reminderEnabled,
                            onCheckedChange = { reminderEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF38BDF8)
                            )
                        )
                    }
                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    SettingRowItem(
                        label = "Repeat",
                        value = repeatType,
                        onClick = {
                            showRepeatDialog = true
                        }
                    )
                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    SettingRowItem(
                        label = "Customize Task List",
                        value = "${routine.steps.size} tasks",
                        onClick = { viewModel.navigateTo(Screen.TaskList) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Focus & Flow Timer Recommendations (Hidden by default in Edit Mode, expandable)
            var showRecommendations by remember { mutableStateOf(false) }
            val recommendations = remember(routine) { viewModel.getTimerRecommendations(routine) }
            val hasAdjustments = recommendations.any { it.type != com.example.model.RecommendationType.BALANCED }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("timer_recommendations_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showRecommendations = !showRecommendations },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "⏱️", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Smart Timer Recommendations",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B)
                                )
                                Text(
                                    text = if (showRecommendations) "Fine-tuned from past runs" else "Hidden • Tap to view insights",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (hasAdjustments) {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFFEF3C7), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "Suggestions Available",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB45309)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = if (showRecommendations) "Hide" else "Show",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0284C7)
                            )
                        }
                    }

                    if (showRecommendations) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "💡 Based on past runs: helps your child require focus without feeling rushed. Timers are tuned so nominal pace matches real flow.",
                            fontSize = 13.sp,
                            color = Color(0xFF475569),
                            lineHeight = 18.sp
                        )

                        if (hasAdjustments) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { viewModel.applyAllRecommendations(routine) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("apply_all_recommendations_btn")
                            ) {
                                Text(
                                    text = "⚡ Apply All Suggested Timers",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        recommendations.forEach { rec ->
                            val isBalanced = rec.type == com.example.model.RecommendationType.BALANCED
                            val badgeColor = when (rec.type) {
                                com.example.model.RecommendationType.INCREASE -> Color(0xFFFEE2E2)
                                com.example.model.RecommendationType.DECREASE -> Color(0xFFE0F2FE)
                                com.example.model.RecommendationType.BALANCED -> Color(0xFFDCFCE7)
                            }
                            val textColor = when (rec.type) {
                                com.example.model.RecommendationType.INCREASE -> Color(0xFFDC2626)
                                com.example.model.RecommendationType.DECREASE -> Color(0xFF0284C7)
                                com.example.model.RecommendationType.BALANCED -> Color(0xFF16A34A)
                            }

                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = rec.stepTitle,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1E293B)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = rec.reason,
                                            fontSize = 12.sp,
                                            color = Color(0xFF64748B)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        val absDelta = kotlin.math.abs(rec.deltaMinutes)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .background(badgeColor, RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = when (rec.type) {
                                                        com.example.model.RecommendationType.INCREASE -> "🔺 +${absDelta}m (${rec.recommendedMinutes}m suggested)"
                                                        com.example.model.RecommendationType.DECREASE -> "🔻 -${absDelta}m (${rec.recommendedMinutes}m suggested)"
                                                        com.example.model.RecommendationType.BALANCED -> "✨ Sweet Spot (${rec.recommendedMinutes}m)"
                                                    },
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = textColor
                                                )
                                            }
                                        }
                                    }

                                    if (!isBalanced) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = { viewModel.applyTimerRecommendation(routine, rec.stepId, rec.recommendedMinutes) },
                                            colors = ButtonDefaults.buttonColors(containerColor = textColor),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.testTag("apply_rec_${rec.stepId}")
                                        ) {
                                            Text(
                                                text = "Apply",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Delete Routine Button
            OutlinedButton(
                onClick = {
                    viewModel.removeRoutine(routine.id)
                },
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DeleteCoral),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, DeleteCoral),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("delete_routine_btn")
            ) {
                Text(
                    text = "Delete Routine",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeleteCoral
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Save Button
            Button(
                onClick = {
                    val updated = routine.copy(
                        title = title,
                        startTime = startTime,
                        targetDepartureTime = targetDepartureTime,
                        reminderEnabled = reminderEnabled,
                        repeatType = repeatType
                    )
                    viewModel.updateRoutine(updated)
                    viewModel.navigateBack()
                },
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SaveNavy),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .shadow(4.dp, RoundedCornerShape(28.dp))
                    .testTag("save_routine_btn")
            ) {
                Text(
                    text = "Save Changes",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        if (showStartTimePicker) {
            TimePickerDialog(
                title = "Set Start Time",
                initialTimeString = startTime,
                onConfirm = {
                    startTime = it
                    showStartTimePicker = false
                },
                onDismiss = { showStartTimePicker = false }
            )
        }

        if (showEndTimePicker) {
            TimePickerDialog(
                title = "Set Target End Time",
                initialTimeString = targetDepartureTime,
                onConfirm = {
                    targetDepartureTime = it
                    showEndTimePicker = false
                },
                onDismiss = { showEndTimePicker = false }
            )
        }

        if (showRepeatDialog) {
            RepeatScheduleDialog(
                initialRepeatString = repeatType,
                onConfirm = {
                    repeatType = it
                    showRepeatDialog = false
                },
                onDismiss = { showRepeatDialog = false }
            )
        }
    }
}

@Composable
fun SettingRowItem(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1E293B)
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                fontSize = 15.sp,
                color = Color(0xFF64748B)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
