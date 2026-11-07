package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.viewmodel.RoutineViewModel
import com.example.util.AppLogger
import java.util.Calendar
import java.util.Locale

@Composable
fun DiagnosticsDialog(
    viewModel: RoutineViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentSession by viewModel.activeSession.collectAsState()
    val currentRoutine by viewModel.currentRoutine.collectAsState()
    val familyCode by viewModel.familyCode.collectAsState()
    val wallCalendar by viewModel.currentWallTime.collectAsState()

    var logsText by remember { mutableStateOf(AppLogger.getFormattedLogs()) }
    var copiedFeedback by remember { mutableStateOf(false) }

    val nowHour = wallCalendar.get(Calendar.HOUR_OF_DAY)
    val nowMin = wallCalendar.get(Calendar.MINUTE)
    val nowSec = wallCalendar.get(Calendar.SECOND)
    val wallTimeStr = String.format(Locale.getDefault(), "%02d:%02d:%02d", nowHour, nowMin, nowSec)

    val session = currentSession
    val (calcFinishTime, deltaMin) = if (session != null) {
        viewModel.getCalculatedFinishTime(session, currentRoutine)
    } else {
        Pair("--:--", 0)
    }

    val effectiveBonusSec = if (session != null) {
        viewModel.getEffectiveBonusTimeSeconds(session, currentRoutine)
    } else {
        0
    }

    val currentTask = if (session != null) {
        if (session.currentStepId.isNotEmpty()) {
            currentRoutine.activeSteps.find { it.id == session.currentStepId }
                ?: currentRoutine.activeSteps.getOrNull(session.currentStepIndex)
        } else {
            currentRoutine.activeSteps.getOrNull(session.currentStepIndex)
        }
    } else null

    val summaryText = buildString {
        appendLine("=== LAUNCHPAD SYSTEM DIAGNOSTICS ===")
        appendLine("Wall Time: $wallTimeStr")
        appendLine("Family Code: $familyCode")
        appendLine("Device ID: ${viewModel.localDeviceId}")
        appendLine("Session Architecture: Peer-to-Peer Event Log (Last Event Wins)")
        appendLine("Last Author Device: ${session?.controllerDeviceId ?: "none"}")
        appendLine("Current Routine: ${currentRoutine.title} (${currentRoutine.id})")
        appendLine("Active Tasks Count: ${currentRoutine.activeSteps.size}")
        appendLine("Active Session Status: ${session?.status?.name ?: "NO_SESSION"}")
        if (session != null) {
            val allSteps = session.getAllSteps(currentRoutine.activeSteps)
            val currentPos = if (session.currentStepId.isNotEmpty()) {
                val idx = allSteps.indexOfFirst { it.id == session.currentStepId }
                if (idx >= 0) "${idx + 1} of ${allSteps.size}" else "${session.currentStepIndex + 1} of ${allSteps.size}"
            } else {
                "${session.currentStepIndex + 1} of ${allSteps.size}"
            }
            appendLine("Current Task: ${currentTask?.title ?: "Unknown"} (Task $currentPos)")
            appendLine("Current Task ID: ${session.currentStepId.ifEmpty { currentTask?.id ?: "none" }}")
            appendLine("Step Remaining Sec: ${session.stepRemainingSeconds}s / ${session.stepTotalSeconds}s")
            appendLine("Target End Time: ${session.targetEndTime.ifEmpty { currentRoutine.targetDepartureTime }}")
            appendLine("Calculated Finish: $calcFinishTime (delta: ${deltaMin}m)")
            appendLine("Banked Bonus Sec: ${session.bonusTimeSeconds}s")
            appendLine("Effective Slack/Buffer: ${effectiveBonusSec}s (${effectiveBonusSec / 60}m ${effectiveBonusSec % 60}s)")
            appendLine("Completed Step IDs: ${session.completedStepIds}")
            appendLine("Assisted Step IDs: ${session.assistedStepIds}")
            appendLine("Step Durations Sec: ${session.stepDurationsSeconds}")
            if (session.eventLog.isNotEmpty()) {
                appendLine("Recent Event Log (${session.eventLog.size}):")
                session.eventLog.takeLast(5).forEach { evt ->
                    appendLine("  • [${evt.eventType}] ${evt.stepTitle.ifEmpty { evt.stepId }} (${evt.remainingSeconds}s) by ${evt.deviceId.take(8)}")
                }
            }
        }
        appendLine("=====================================")
        appendLine()
        appendLine("=== EVENT LOGS ===")
        appendLine(logsText)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("diagnostics_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Diagnostics & Sync Logs",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Status Mini Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text("WALL TIME", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                            Text(wallTimeStr, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFFE0F2FE), RoundedCornerShape(10.dp))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text("TARGET END", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0369A1))
                            Text(session?.targetEndTime ?: currentRoutine.targetDepartureTime, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0284C7))
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (deltaMin > 0) Color(0xFFFEE2E2) else Color(0xFFDCFCE7), RoundedCornerShape(10.dp))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text("BUFFER SLACK", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (deltaMin > 0) Color(0xFF991B1B) else Color(0xFF166534))
                            val buffStr = if (effectiveBonusSec >= 0) "+${effectiveBonusSec / 60}m" else "${effectiveBonusSec / 60}m"
                            Text(buffStr, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = if (deltaMin > 0) Color(0xFFDC2626) else Color(0xFF16A34A))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable log console with native text selection
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp, max = 280.dp)
                        .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    SelectionContainer {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            Text(
                                text = summaryText,
                                color = Color(0xFF38BDF8),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Launchpad Diagnostics", summaryText)
                            clipboard.setPrimaryClip(clip)
                            copiedFeedback = true
                            Log.i("LAUNCHPAD_DIAGNOSTICS", summaryText)
                            Toast.makeText(context, "Copied & logged to Logcat!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (copiedFeedback) Color(0xFF16A34A) else Color(0xFF4F46E5)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("copy_diagnostics_logs_btn")
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (copiedFeedback) "Copied!" else "Copy", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            Log.i("LAUNCHPAD_DIAGNOSTICS", summaryText)
                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, summaryText)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Share Diagnostics")
                            context.startActivity(shareIntent)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("share_diagnostics_logs_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                    }

                    OutlinedButton(
                        onClick = {
                            AppLogger.clear()
                            logsText = AppLogger.getFormattedLogs()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("clear_diagnostics_logs_btn")
                    ) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
