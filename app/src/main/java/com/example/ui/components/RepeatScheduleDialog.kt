package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.SaveNavy

@Composable
fun RepeatScheduleDialog(
    initialRepeatString: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    var selectedDays by remember {
        val initialSelected = when (initialRepeatString.lowercase()) {
            "everyday" -> daysOfWeek.toSet()
            "weekdays" -> setOf("Mon", "Tue", "Wed", "Thu", "Fri")
            "weekends" -> setOf("Sat", "Sun")
            else -> {
                val parsed = initialRepeatString.split(",").map { it.trim() }.filter { daysOfWeek.contains(it) }.toSet()
                if (parsed.isNotEmpty()) parsed else daysOfWeek.toSet()
            }
        }
        mutableStateOf(initialSelected)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("repeat_schedule_dialog")
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Repeat Schedule",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Presets (Everyday, Weekdays, Weekends)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val presets = listOf(
                        "Everyday" to daysOfWeek.toSet(),
                        "Weekdays" to setOf("Mon", "Tue", "Wed", "Thu", "Fri"),
                        "Weekends" to setOf("Sat", "Sun")
                    )
                    presets.forEach { (label, set) ->
                        val isPresetActive = selectedDays == set
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isPresetActive) Color(0xFF0284C7) else Color(0xFFF1F5F9))
                                .clickable { selectedDays = set }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (isPresetActive) FontWeight.Bold else FontWeight.Medium,
                                color = if (isPresetActive) Color.White else Color(0xFF475569)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Individual Day Chips (M, T, W, T, F, S, S)
                Text(
                    text = "Custom Days",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF64748B),
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    daysOfWeek.forEach { day ->
                        val isDaySelected = selectedDays.contains(day)
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isDaySelected) Color(0xFF0284C7) else Color(0xFFF1F5F9))
                                .clickable {
                                    selectedDays = if (isDaySelected) {
                                        if (selectedDays.size > 1) selectedDays - day else selectedDays
                                    } else {
                                        selectedDays + day
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day.take(1),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDaySelected) Color.White else Color(0xFF475569)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Result Summary
                val summaryText = when {
                    selectedDays.size == 7 -> "Everyday"
                    selectedDays == setOf("Mon", "Tue", "Wed", "Thu", "Fri") -> "Weekdays"
                    selectedDays == setOf("Sat", "Sun") -> "Weekends"
                    else -> daysOfWeek.filter { selectedDays.contains(it) }.joinToString(", ")
                }
                Text(
                    text = "Summary: $summaryText",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1E293B)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            onConfirm(summaryText)
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SaveNavy),
                        modifier = Modifier.weight(1f).height(46.dp).testTag("confirm_repeat_btn")
                    ) {
                        Text("Set Schedule")
                    }
                }
            }
        }
    }
}
