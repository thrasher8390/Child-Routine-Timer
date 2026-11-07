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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
fun TimePickerDialog(
    title: String,
    initialTimeString: String, // "07:15" or "19:30"
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val initialParts = initialTimeString.split(":")
    val initialHour24 = initialParts.getOrNull(0)?.toIntOrNull() ?: 7
    val initialMin = initialParts.getOrNull(1)?.toIntOrNull() ?: 0

    var hour24 by remember { mutableIntStateOf(initialHour24) }
    var minute by remember { mutableIntStateOf(initialMin) }

    val isPm = hour24 >= 12
    val hour12 = when {
        hour24 == 0 -> 12
        hour24 > 12 -> hour24 - 12
        else -> hour24
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("time_picker_dialog")
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Time Display with Hour, Minute, and AM/PM Toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hour Column
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = {
                                val nextH24 = (hour24 + 1) % 24
                                hour24 = nextH24
                            },
                            modifier = Modifier.size(36.dp).background(Color(0xFFF1F5F9), CircleShape)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Hour +", tint = Color(0xFF1E293B))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format("%02d", hour12),
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0284C7)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        IconButton(
                            onClick = {
                                val prevH24 = if (hour24 - 1 < 0) 23 else hour24 - 1
                                hour24 = prevH24
                            },
                            modifier = Modifier.size(36.dp).background(Color(0xFFF1F5F9), CircleShape)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Hour -", tint = Color(0xFF1E293B))
                        }
                        Text("Hour", fontSize = 11.sp, color = Color(0xFF64748B))
                    }

                    Text(
                        text = ":",
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )

                    // Minute Column
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(
                            onClick = {
                                minute = (minute + 5) % 60
                            },
                            modifier = Modifier.size(36.dp).background(Color(0xFFF1F5F9), CircleShape)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Minute +", tint = Color(0xFF1E293B))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format("%02d", minute),
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0284C7)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        IconButton(
                            onClick = {
                                minute = if (minute - 5 < 0) 55 else minute - 5
                            },
                            modifier = Modifier.size(36.dp).background(Color(0xFFF1F5F9), CircleShape)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Minute -", tint = Color(0xFF1E293B))
                        }
                        Text("Minute", fontSize = 11.sp, color = Color(0xFF64748B))
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // AM / PM Switcher Column
                    Column(
                        modifier = Modifier
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(14.dp))
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (!isPm) Color(0xFF0284C7) else Color.Transparent)
                                .clickable {
                                    if (isPm) hour24 = (hour24 - 12).coerceAtLeast(0)
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text("AM", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (!isPm) Color.White else Color(0xFF64748B))
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isPm) Color(0xFF0284C7) else Color.Transparent)
                                .clickable {
                                    if (!isPm) hour24 = (hour24 + 12).coerceAtMost(23)
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text("PM", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (isPm) Color.White else Color(0xFF64748B))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Quick presets
                Text("Quick Presets", fontSize = 12.sp, color = Color(0xFF64748B), modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val presets = listOf(
                        "06:30" to "6:30 AM",
                        "07:00" to "7:00 AM",
                        "07:15" to "7:15 AM",
                        "07:30" to "7:30 AM",
                        "08:00" to "8:00 AM",
                        "19:00" to "7:00 PM",
                        "19:30" to "7:30 PM",
                        "20:00" to "8:00 PM",
                        "20:30" to "8:30 PM"
                    )
                    items(presets) { (val24, label) ->
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
                                .clickable {
                                    val parts = val24.split(":")
                                    hour24 = parts[0].toInt()
                                    minute = parts[1].toInt()
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(label, fontSize = 12.sp, color = Color(0xFF1E293B))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Actions
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
                            val formatted = String.format("%02d:%02d", hour24, minute)
                            onConfirm(formatted)
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SaveNavy),
                        modifier = Modifier.weight(1f).height(46.dp).testTag("confirm_time_btn")
                    ) {
                        Text("Set Time")
                    }
                }
            }
        }
    }
}
