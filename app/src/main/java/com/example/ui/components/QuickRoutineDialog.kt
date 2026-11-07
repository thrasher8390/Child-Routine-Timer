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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.example.model.CatalogTask
import java.util.Calendar
import java.util.Locale

@Composable
fun QuickRoutineDialog(
    catalogTasks: List<CatalogTask>,
    onDismiss: () -> Unit,
    onAddNewCatalogTask: (CatalogTask) -> Unit,
    onLaunchQuickRoutine: (title: String, targetDepartureTime: String, selectedTasks: List<CatalogTask>, startNow: Boolean) -> Unit
) {
    var title by remember { mutableStateOf("Leave the House") }
    val selectedTaskIds = remember {
        mutableStateListOf<String>().apply {
            // Pre-select popular departure tasks if available
            val defaults = listOf("cat_teeth", "cat_clothes", "cat_shoes", "cat_backpack")
            addAll(catalogTasks.filter { it.id in defaults }.map { it.id })
            if (isEmpty() && catalogTasks.isNotEmpty()) {
                addAll(catalogTasks.take(3).map { it.id })
            }
        }
    }

    // Departure time computation
    val cal = Calendar.getInstance()
    var departureOffsetMinutes by remember { mutableStateOf(30) }
    var customDepartureTime by remember { mutableStateOf("") }
    var isAddingNewTask by remember { mutableStateOf(false) }
    var newTaskTitle by remember { mutableStateOf("") }
    var newTaskMinutes by remember { mutableStateOf("5") }

    fun calculateDepartureTimeStr(offset: Int): String {
        val c = Calendar.getInstance().apply { add(Calendar.MINUTE, offset) }
        return String.format(Locale.getDefault(), "%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE))
    }

    val selectedTasks = catalogTasks.filter { it.id in selectedTaskIds }
    val totalNominalMinutes = selectedTasks.sumOf { it.defaultMinutes }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("quick_routine_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
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
                                .background(Color(0xFFE0E7FF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.RocketLaunch,
                                contentDescription = null,
                                tint = Color(0xFF4F46E5),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Build Quick Routine",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "From shared task catalog",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp).testTag("close_quick_routine_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF64748B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Routine Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("quick_routine_title_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Quick Preset Name Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Leave the House" to "🏠", "Disney Trip" to "🏰", "Weekend Outing" to "🎈").forEach { (presetName, emoji) ->
                        val isSelected = title == presetName
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSelected) Color(0xFFE0E7FF) else Color(0xFFF1F5F9),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    title = presetName
                                    if (presetName == "Disney Trip") {
                                        val disneyIds = catalogTasks.filter {
                                            it.id in listOf("cat_disney", "cat_clothes", "cat_shoes", "cat_sunscreen", "cat_water", "cat_backpack")
                                        }.map { it.id }
                                        if (disneyIds.isNotEmpty()) {
                                            selectedTaskIds.clear()
                                            selectedTaskIds.addAll(disneyIds)
                                        }
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "$emoji $presetName",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color(0xFF4338CA) else Color(0xFF475569)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Target Departure Time selection
                Text(
                    text = "Target Departure:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(15, 30, 45, 60).forEach { offset ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (departureOffsetMinutes == offset) Color(0xFF4F46E5) else Color(0xFFF1F5F9),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    departureOffsetMinutes = offset
                                    customDepartureTime = ""
                                }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "in ${offset}m",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (departureOffsetMinutes == offset) Color.White else Color(0xFF334155)
                                )
                                Text(
                                    text = calculateDepartureTimeStr(offset),
                                    fontSize = 10.sp,
                                    color = if (departureOffsetMinutes == offset) Color(0xFFE0E7FF) else Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Task Catalog Checklist Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pick Tasks (${selectedTaskIds.size} selected • ${totalNominalMinutes}m)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF1E293B)
                    )

                    Row(
                        modifier = Modifier
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                            .clickable { isAddingNewTask = !isAddingNewTask }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "New Task",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF4F46E5)
                        )
                    }
                }

                if (isAddingNewTask) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = newTaskTitle,
                                    onValueChange = { newTaskTitle = it },
                                    label = { Text("Task name") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                OutlinedTextField(
                                    value = newTaskMinutes,
                                    onValueChange = { newTaskMinutes = it },
                                    label = { Text("Mins") },
                                    modifier = Modifier.width(70.dp),
                                    singleLine = true
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = {
                                    val safeMin = newTaskMinutes.toIntOrNull() ?: 5
                                    if (newTaskTitle.isNotBlank()) {
                                        val newTask = CatalogTask(
                                            id = "cat_${System.currentTimeMillis()}",
                                            title = newTaskTitle.trim(),
                                            defaultMinutes = safeMin,
                                            minMinutes = (safeMin / 2).coerceAtLeast(1),
                                            maxMinutes = safeMin * 2,
                                            isImportant = true,
                                            starReward = 1,
                                            category = "Custom"
                                        )
                                        onAddNewCatalogTask(newTask)
                                        selectedTaskIds.add(newTask.id)
                                        newTaskTitle = ""
                                        isAddingNewTask = false
                                    }
                                },
                                modifier = Modifier.align(Alignment.End),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                            ) {
                                Text("Add to Catalog & Routine", fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Scrollable Catalog Task List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .height(200.dp)
                ) {
                    items(catalogTasks, key = { it.id }) { task ->
                        val isSelected = task.id in selectedTaskIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .background(
                                    if (isSelected) Color(0xFFEEF2FF) else Color(0xFFFAFAFA),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    if (isSelected) {
                                        selectedTaskIds.remove(task.id)
                                    } else {
                                        selectedTaskIds.add(task.id)
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    if (checked) selectedTaskIds.add(task.id) else selectedTaskIds.remove(task.id)
                                },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4F46E5)),
                                modifier = Modifier.size(24.dp)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = task.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "${task.defaultMinutes}m • ${task.category}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }

                            if (task.isImportant) {
                                Text(
                                    text = "⭐ ${task.starReward}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD97706)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Buttons (48dp height minimum)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val depTime = if (customDepartureTime.isNotBlank()) customDepartureTime else calculateDepartureTimeStr(departureOffsetMinutes)
                            onLaunchQuickRoutine(title, depTime, selectedTasks, false)
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("save_quick_routine_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save Only", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val depTime = if (customDepartureTime.isNotBlank()) customDepartureTime else calculateDepartureTimeStr(departureOffsetMinutes)
                            onLaunchQuickRoutine(title, depTime, selectedTasks, true)
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                            .testTag("launch_quick_routine_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Start Now", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
