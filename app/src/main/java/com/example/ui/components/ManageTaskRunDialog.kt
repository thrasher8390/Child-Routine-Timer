package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Undo
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.RoutineStep
import com.example.ui.theme.DoneButtonMagenta
import com.example.ui.theme.SaveNavy

@Composable
fun ManageTaskRunDialog(
    step: RoutineStep,
    isCompleted: Boolean,
    isAssisted: Boolean,
    isCurrentStep: Boolean,
    isEnabled: Boolean,
    onToggleDone: (Boolean) -> Unit,
    onToggleAssisted: (Boolean) -> Unit,
    onToggleSkipToday: () -> Unit,
    onJumpToStep: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("manage_task_run_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Bar with Step Icon, Title, and Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFE0F2FE), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            TimelineStepIcon(
                                iconKey = step.iconKey,
                                tint = Color(0xFF0369A1),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = step.title,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = "${step.defaultMinutes} min" + if (step.isImportant) " • ⭐ ${step.starReward} stars" else "",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp).testTag("close_manage_task_dialog")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Card 1: Done Status
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCompleted) Color(0xFFF0FDF4) else Color(0xFFF8FAFC)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "COMPLETION STATUS",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B),
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = if (isCompleted) "Completed ✓" else "Not Completed Yet",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCompleted) Color(0xFF16A34A) else Color(0xFF475569)
                            )
                        }

                        Button(
                            onClick = { onToggleDone(!isCompleted) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCompleted) Color(0xFFE2E8F0) else Color(0xFF16A34A),
                                contentColor = if (isCompleted) Color(0xFF334155) else Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("toggle_done_btn")
                        ) {
                            Text(
                                text = if (isCompleted) "Mark Incomplete" else "Mark Done ✓",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Card 2: Parent Assistance (only relevant if completed or marking done)
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ASSISTANCE MODE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B),
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = if (isAssisted) "🤝 Parent Assisted (0⭐)" else "⭐ Done on Own (+⭐)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAssisted) Color(0xFF475569) else Color(0xFF15803D)
                            )
                        }

                        Button(
                            onClick = { onToggleAssisted(!isAssisted) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAssisted) Color(0xFFDCFCE7) else Color(0xFFE2E8F0),
                                contentColor = if (isAssisted) Color(0xFF15803D) else Color(0xFF334155)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("toggle_assisted_btn")
                        ) {
                            Text(
                                text = if (isAssisted) "Make Solo ⭐" else "Make Assisted 🤝",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Card 3: Skip Task for Today
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ROUTINE INCLUSION",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B),
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = if (isEnabled) "Included in Routine" else "Skipped for Today",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isEnabled) Color(0xFF0369A1) else Color(0xFFB91C1C)
                            )
                        }

                        Button(
                            onClick = onToggleSkipToday,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isEnabled) Color(0xFFFEE2E2) else Color(0xFFE0F2FE),
                                contentColor = if (isEnabled) Color(0xFFB91C1C) else Color(0xFF0284C7)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("toggle_skip_today_btn")
                        ) {
                            Text(
                                text = if (isEnabled) "Skip Today 👁️" else "Include Today ✓",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Card 4: Switch / Jump to Task (if not already current)
                if (!isCurrentStep && isEnabled) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onJumpToStep,
                        colors = ButtonDefaults.buttonColors(containerColor = DoneButtonMagenta),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("jump_to_task_btn")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Switch to this Task Now", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Done Dismiss Button
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = SaveNavy),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp).testTag("dismiss_manage_task_dialog")
                ) {
                    Text("Done")
                }
            }
        }
    }
}
