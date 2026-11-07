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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun QuickTimerDialog(
    onDismiss: () -> Unit,
    onStartTimer: (title: String, durationMinutes: Int, awardStars: Boolean, starReward: Int, iconKey: String) -> Unit
) {
    var title by remember { mutableStateOf("Quick Timer") }
    var durationMinutes by remember { mutableIntStateOf(5) }
    var awardStars by remember { mutableStateOf(true) }
    var starReward by remember { mutableIntStateOf(1) }
    var selectedIcon by remember { mutableStateOf("hourglass") }

    val presetMinutes = listOf(1, 2, 3, 5, 10, 15, 20, 30)

    val iconOptions = listOf(
        "hourglass" to "⏳ Hourglass",
        "play" to "🎮 Play",
        "book" to "📚 Reading",
        "tidy" to "🧹 Clean Up",
        "potty" to "🚽 Bathroom"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("quick_timer_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFFEF3C7), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.HourglassBottom,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Start Quick Timer",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Instant visual hourglass countdown",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp).testTag("close_quick_timer_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF64748B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Timer Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("quick_timer_title_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Duration Stepper & Display
                Text(
                    text = "Timer Duration",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (durationMinutes > 1) durationMinutes-- },
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(0xFFE2E8F0), CircleShape)
                            .testTag("decrease_quick_timer_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease 1 min")
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$durationMinutes min",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "${durationMinutes * 60} seconds total",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    IconButton(
                        onClick = { if (durationMinutes < 120) durationMinutes++ },
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(0xFFE2E8F0), CircleShape)
                            .testTag("increase_quick_timer_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Increase 1 min")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Preset Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetMinutes.take(4).forEach { min ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (durationMinutes == min) Color(0xFFD97706) else Color(0xFFF1F5F9),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { durationMinutes = min }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${min}m",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (durationMinutes == min) Color.White else Color(0xFF334155)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetMinutes.takeLast(4).forEach { min ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (durationMinutes == min) Color(0xFFD97706) else Color(0xFFF1F5F9),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { durationMinutes = min }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${min}m",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (durationMinutes == min) Color.White else Color(0xFF334155)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Award Stars Toggle & Reward
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (awardStars) Color(0xFFFFFBEB) else Color(0xFFF8FAFC)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (awardStars) Color(0xFFF59E0B) else Color(0xFF94A3B8),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Award Stars on Completion",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = if (awardStars) "Child earns stars if completed independently" else "No stars for this countdown",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            Switch(
                                checked = awardStars,
                                onCheckedChange = { awardStars = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFFF59E0B)
                                ),
                                modifier = Modifier.testTag("award_stars_switch")
                            )
                        }

                        if (awardStars) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Star Amount:",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF334155)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    (1..3).forEach { count ->
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (starReward == count) Color(0xFFF59E0B) else Color(0xFFFEF3C7),
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .clickable { starReward = count }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = "⭐ $count",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = if (starReward == count) Color.White else Color(0xFF92400E)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Activity Category / Icon
                Text(
                    text = "Timer Icon",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    iconOptions.forEach { (key, label) ->
                        Box(
                            modifier = Modifier
                                .background(
                                    if (selectedIcon == key) Color(0xFF0284C7) else Color(0xFFF1F5F9),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedIcon = key }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selectedIcon == key) Color.White else Color(0xFF334155)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Launch Button (minimum 48dp)
                Button(
                    onClick = {
                        onStartTimer(title, durationMinutes, awardStars, starReward, selectedIcon)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("launch_quick_timer_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.HourglassBottom,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Start $durationMinutes-Min Hourglass Timer",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
