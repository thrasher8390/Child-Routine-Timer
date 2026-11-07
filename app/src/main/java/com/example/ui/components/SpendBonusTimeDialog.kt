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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.CatalogTask
import com.example.ui.theme.BonusAmber
import com.example.ui.theme.DoneButtonMagenta
import java.util.Locale

@Composable
fun SpendBonusTimeDialog(
    bonusTimeSeconds: Int,
    catalogTasks: List<CatalogTask> = emptyList(),
    onSpendBonus: (
        title: String,
        minutes: Int,
        iconKey: String,
        colorHex: String,
        catalogTaskId: String?,
        startImmediately: Boolean
    ) -> Unit,
    onGrantBonus: (minutes: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val availableMinutes = bonusTimeSeconds / 60
    val availableSecondsRem = bonusTimeSeconds % 60
    val hasBonus = bonusTimeSeconds >= 60

    // Quick selects for how much bonus time to use at the top
    var selectedMinutes by remember {
        mutableIntStateOf(
            if (availableMinutes > 0) minOf(2, availableMinutes).coerceAtLeast(1) else 2
        )
    }

    // Default activity name is "Special Break"
    var activityName by remember { mutableStateOf("Special Break") }
    var selectedCatalogTask by remember { mutableStateOf<CatalogTask?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 500.dp)
                .padding(vertical = 16.dp)
                .testTag("spend_bonus_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with sparkle & close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFFFDE68A), Color(0xFFF59E0B))
                                    ),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✨", fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Bonus Time Break",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Take a special break using earned bonus time",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("close_bonus_dialog_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Available Bonus Time Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (hasBonus) Color(0xFFFEF3C7) else Color(0xFFF1F5F9)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "AVAILABLE BONUS TIME",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (hasBonus) Color(0xFFB45309) else Color(0xFF64748B),
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = String.format(Locale.getDefault(), "%d min %02d sec", availableMinutes, availableSecondsRem),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = if (hasBonus) Color(0xFFD97706) else Color(0xFF475569)
                            )
                        }

                        // Parent Quick Gift Button
                        OutlinedButton(
                            onClick = {
                                onGrantBonus(2)
                                selectedMinutes = minOf(selectedMinutes, (bonusTimeSeconds / 60) + 2).coerceAtLeast(1)
                            },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("grant_bonus_minutes_btn")
                        ) {
                            Text(
                                text = "+2m Gift 💝",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD97706)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Top: Quick selects for how much bonus time to use
                Text(
                    text = "HOW MUCH BONUS TIME TO USE?",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF475569),
                    modifier = Modifier.fillMaxWidth(),
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val durationOptions = mutableListOf(1, 2, 3, 5)
                    if (availableMinutes > 5 && !durationOptions.contains(availableMinutes)) {
                        durationOptions.add(availableMinutes)
                    }

                    durationOptions.forEach { mins ->
                        val isSelected = selectedMinutes == mins
                        val isAffordable = (mins * 60) <= bonusTimeSeconds

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) BonusAmber else Color(0xFFF8FAFC))
                                .border(
                                    1.5.dp,
                                    if (isSelected) Color(0xFFD97706) else Color(0xFFE2E8F0),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedMinutes = mins }
                                .padding(vertical = 10.dp)
                                .testTag("bonus_duration_${mins}m"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${mins}m",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isSelected) Color.White else Color(0xFF1E293B)
                                )
                                if (!isAffordable) {
                                    Text(
                                        text = "Gift req.",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White.copy(alpha = 0.85f) else Color(0xFFEF4444)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Activity Name Text Box with default "Special Break"
                Text(
                    text = "ACTIVITY NAME",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF475569),
                    modifier = Modifier.fillMaxWidth(),
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = activityName,
                    onValueChange = {
                        activityName = it
                        selectedCatalogTask = null
                    },
                    label = { Text("Activity") },
                    placeholder = { Text("Special Break (e.g. Cuddle with Mom, Lotion, Read a Story)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_bonus_title_input")
                )

                // Quick picker from Task Catalog at the bottom of the selector
                if (catalogTasks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "OR PICK FROM TASK CATALOG:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        modifier = Modifier.fillMaxWidth(),
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(catalogTasks) { catTask ->
                            val isCatSelected = selectedCatalogTask?.id == catTask.id
                            val catColor = try {
                                Color(android.graphics.Color.parseColor(catTask.colorHex))
                            } catch (_: Exception) {
                                Color(0xFF0284C7)
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isCatSelected) catColor.copy(alpha = 0.2f) else Color(0xFFF8FAFC))
                                    .border(
                                        if (isCatSelected) 2.dp else 1.dp,
                                        if (isCatSelected) catColor else Color(0xFFE2E8F0),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        selectedCatalogTask = catTask
                                        activityName = catTask.title
                                        selectedMinutes = catTask.defaultMinutes.coerceIn(1, 5)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("bonus_catalog_${catTask.id}")
                            ) {
                                Text(
                                    text = catTask.title,
                                    fontSize = 12.sp,
                                    fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isCatSelected) catColor else Color(0xFF334155)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Single Primary "Start" Button (>= 48dp touch target)
                Button(
                    onClick = {
                        val finalTitle = activityName.ifBlank { "Special Break" }
                        val iconKey = selectedCatalogTask?.iconKey ?: "cuddle"
                        val colorHex = selectedCatalogTask?.colorHex ?: "#EC4899"
                        val catId = selectedCatalogTask?.id

                        onSpendBonus(
                            finalTitle,
                            selectedMinutes,
                            iconKey,
                            colorHex,
                            catId,
                            true // start immediately
                        )
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DoneButtonMagenta),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .shadow(6.dp, RoundedCornerShape(18.dp))
                        .testTag("bonus_start_btn")
                ) {
                    Text(
                        text = "Start",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
