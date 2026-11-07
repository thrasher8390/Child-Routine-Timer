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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CatalogTask
import com.example.model.RoutineStep
import com.example.ui.components.TaskEditDialog
import com.example.ui.components.TimelineStepIcon
import com.example.ui.theme.DeleteCoral
import com.example.ui.theme.DoneButtonMagenta
import com.example.ui.theme.SaveNavy
import com.example.ui.viewmodel.RoutineViewModel
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Surface
import androidx.compose.ui.window.Dialog

@Composable
fun TaskListScreen(
    viewModel: RoutineViewModel,
    modifier: Modifier = Modifier
) {
    val routine by viewModel.currentRoutine.collectAsState()
    val catalogTasks by viewModel.taskCatalog.collectAsState()
    val allRoutines by viewModel.allRoutines.collectAsState()
    var currentSteps by remember(routine.steps) { mutableStateOf(routine.steps) }

    var isAddingTask by remember { mutableStateOf(false) }
    var isPickingFromCatalog by remember { mutableStateOf(false) }
    var editingStepIndex by remember { mutableStateOf<Int?>(null) }

    BackHandler {
        viewModel.navigateBack()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .windowInsetsPadding(WindowInsets.statusBars)
            .testTag("task_list_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.navigateBack() },
                    modifier = Modifier.testTag("task_list_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF1E293B)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "Customize Tasks",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    val nominal = currentSteps.sumOf { it.defaultMinutes }
                    val rush = currentSteps.sumOf { it.minMinutes }
                    val relaxed = currentSteps.sumOf { it.maxMinutes }
                    val window = routine.getWindowMinutes()
                    val buffer = (window - nominal).coerceAtLeast(0)
                    Text(
                        text = "${routine.title} • $window min window (${viewModel.formatTimeAmPm(routine.startTime)} - ${viewModel.formatTimeAmPm(routine.targetDepartureTime)})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF475569)
                    )
                    Text(
                        text = "⚖️ $nominal m nominal ${if (buffer > 0) "(+$buffer m buffer)" else ""} • ⚡ $rush m rush • 🌱 $relaxed m relaxed",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0284C7)
                    )
                }
            }

            // Task list cards
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(currentSteps) { index, step ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editingStepIndex = index }
                            .testTag("task_item_${step.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier.size(34.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    TimelineStepIcon(
                                        iconKey = step.iconKey,
                                        tint = try {
                                            Color(android.graphics.Color.parseColor(step.colorHex))
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
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (step.isEnabled) Color(0xFF1E293B) else Color(0xFF94A3B8)
                                        )
                                        if (!step.isEnabled) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "Skipped today",
                                                    fontSize = 10.sp,
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
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFB45309)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = if (step.isEnabled) {
                                            "${step.defaultMinutes} min (${step.minMinutes}m rush - ${step.maxMinutes}m relaxed)"
                                        } else {
                                            "${step.defaultMinutes} min (excluded from routine)"
                                        },
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Toggle enable / skip for today
                                IconButton(
                                    onClick = {
                                        val mutable = currentSteps.toMutableList()
                                        mutable[index] = step.copy(isEnabled = !step.isEnabled)
                                        currentSteps = mutable
                                    },
                                    modifier = Modifier.size(32.dp).testTag("toggle_step_${step.id}")
                                ) {
                                    Icon(
                                        imageVector = if (step.isEnabled) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (step.isEnabled) "Skip task for today" else "Include task",
                                        tint = if (step.isEnabled) Color(0xFF0284C7) else Color(0xFF94A3B8),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Edit button
                                IconButton(
                                    onClick = { editingStepIndex = index },
                                    modifier = Modifier.size(32.dp).testTag("edit_step_btn_${step.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit details",
                                        tint = Color(0xFF0284C7),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Up / Down reorder arrows
                                if (index > 0) {
                                    IconButton(
                                        onClick = {
                                            val mutable = currentSteps.toMutableList()
                                            val item = mutable.removeAt(index)
                                            mutable.add(index - 1, item)
                                            currentSteps = mutable
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowUp,
                                            contentDescription = "Move up",
                                            tint = Color(0xFF64748B)
                                        )
                                    }
                                }
                                if (index < currentSteps.size - 1) {
                                    IconButton(
                                        onClick = {
                                            val mutable = currentSteps.toMutableList()
                                            val item = mutable.removeAt(index)
                                            mutable.add(index + 1, item)
                                            currentSteps = mutable
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Move down",
                                            tint = Color(0xFF64748B)
                                        )
                                    }
                                }

                                // Red "X" delete button
                                IconButton(
                                    onClick = {
                                        currentSteps = currentSteps.filterIndexed { i, _ -> i != index }
                                    },
                                    modifier = Modifier.size(32.dp).testTag("delete_step_${step.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove task",
                                        tint = DeleteCoral,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Action Buttons: "+ From Catalog" and "+ New Custom"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { isPickingFromCatalog = true },
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4F46E5)),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF4F46E5)),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("choose_from_catalog_btn")
                ) {
                    Text(
                        text = "📖 From Catalog",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4F46E5)
                    )
                }

                OutlinedButton(
                    onClick = { isAddingTask = true },
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SaveNavy),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, SaveNavy),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("add_activity_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = SaveNavy,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Custom Task",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SaveNavy
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Save Tasks Button (Single full-width action)
            Button(
                onClick = {
                    viewModel.updateRoutine(routine.copy(steps = currentSteps))
                    viewModel.navigateBack()
                },
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SaveNavy),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_task_list_btn")
            ) {
                Text(
                    text = "Save Tasks",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Add New Task Dialog
        if (isAddingTask) {
            TaskEditDialog(
                initialStep = null,
                catalogTasks = catalogTasks,
                allRoutines = allRoutines,
                onQuickAddCatalogTask = { viewModel.addCatalogTask(it) },
                onSave = { newStep ->
                    currentSteps = currentSteps + newStep
                    isAddingTask = false
                },
                onDismiss = { isAddingTask = false }
            )
        }

        // Choose From Catalog Dialog
        if (isPickingFromCatalog) {
            CatalogTaskPickerDialog(
                catalogTasks = catalogTasks,
                onDismiss = { isPickingFromCatalog = false },
                onAddTasks = { selectedTasks ->
                    val converted = selectedTasks.mapIndexed { idx, task ->
                        task.toRoutineStep("step_${System.currentTimeMillis()}_$idx")
                    }
                    currentSteps = currentSteps + converted
                    isPickingFromCatalog = false
                }
            )
        }

        // Edit Existing Task Dialog
        editingStepIndex?.let { idx ->
            currentSteps.getOrNull(idx)?.let { stepToEdit ->
                TaskEditDialog(
                    initialStep = stepToEdit,
                    catalogTasks = catalogTasks,
                    allRoutines = allRoutines,
                    onQuickAddCatalogTask = { viewModel.addCatalogTask(it) },
                    onSave = { updatedStep ->
                        currentSteps = currentSteps.mapIndexed { i, s ->
                            if (i == idx) updatedStep else s
                        }

                        // If this step is linked to a catalog item (or matches by title), update/save the master catalog task as well!
                        val targetCatId = updatedStep.catalogTaskId
                            ?: catalogTasks.firstOrNull { it.title.equals(stepToEdit.title, ignoreCase = true) }?.id
                        if (targetCatId != null) {
                            val existingCat = catalogTasks.firstOrNull { it.id == targetCatId }
                            val catToSave = existingCat?.copy(
                                title = updatedStep.title,
                                iconKey = updatedStep.iconKey,
                                colorHex = updatedStep.colorHex,
                                defaultMinutes = updatedStep.defaultMinutes,
                                minMinutes = updatedStep.minMinutes,
                                maxMinutes = updatedStep.maxMinutes,
                                isImportant = updatedStep.isImportant,
                                starReward = updatedStep.starReward
                            ) ?: CatalogTask(
                                id = targetCatId,
                                title = updatedStep.title,
                                iconKey = updatedStep.iconKey,
                                colorHex = updatedStep.colorHex,
                                defaultMinutes = updatedStep.defaultMinutes,
                                minMinutes = updatedStep.minMinutes,
                                maxMinutes = updatedStep.maxMinutes,
                                isImportant = updatedStep.isImportant,
                                starReward = updatedStep.starReward,
                                category = "General"
                            )
                            viewModel.addCatalogTask(catToSave)
                        }
                        editingStepIndex = null
                    },
                    onDismiss = { editingStepIndex = null }
                )
            }
        }
    }
}

@Composable
fun CatalogTaskPickerDialog(
    catalogTasks: List<CatalogTask>,
    onDismiss: () -> Unit,
    onAddTasks: (List<CatalogTask>) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val selectedIds = remember { androidx.compose.runtime.mutableStateListOf<String>() }

    val filtered = catalogTasks.filter { task ->
        searchQuery.isBlank() || task.title.contains(searchQuery, ignoreCase = true) || task.category.contains(searchQuery, ignoreCase = true)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("catalog_picker_dialog")
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Choose from Catalog",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Select tasks to add to this routine",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search catalog...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                ) {
                    items(filtered.size) { idx ->
                        val task = filtered[idx]
                        val isSelected = selectedIds.contains(task.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(
                                    if (isSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    if (isSelected) selectedIds.remove(task.id) else selectedIds.add(task.id)
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    if (checked) selectedIds.add(task.id) else selectedIds.remove(task.id)
                                },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4F46E5)),
                                modifier = Modifier.size(22.dp)
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

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val tasksToAdd = catalogTasks.filter { it.id in selectedIds }
                        onAddTasks(tasksToAdd)
                    },
                    enabled = selectedIds.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("confirm_add_catalog_tasks_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (selectedIds.isEmpty()) "Select tasks above" else "Add ${selectedIds.size} Task(s) to Routine",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
