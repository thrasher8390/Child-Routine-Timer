package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.example.model.CatalogTask
import com.example.model.Routine
import com.example.model.StepRunHistory
import com.example.ui.components.TaskEditDialog
import com.example.ui.components.TimelineStepIcon
import com.example.ui.viewmodel.RoutineViewModel

@Composable
fun TaskCatalogScreen(
    viewModel: RoutineViewModel,
    modifier: Modifier = Modifier
) {
    val catalogTasks by viewModel.taskCatalog.collectAsState()
    val allRoutines by viewModel.allRoutines.collectAsState()
    val runHistory by viewModel.runHistory.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var editingTask by remember { mutableStateOf<CatalogTask?>(null) }
    var viewingDetailsTask by remember { mutableStateOf<CatalogTask?>(null) }
    var isCreatingTask by remember { mutableStateOf(false) }
    var taskPendingDelete by remember { mutableStateOf<CatalogTask?>(null) }

    BackHandler {
        viewModel.navigateBack()
    }

    val categories = remember(catalogTasks) {
        val unique = catalogTasks.map { it.category }.distinct().filter { it.isNotBlank() }
        listOf("All") + unique
    }

    val filteredTasks = catalogTasks.filter { task ->
        val matchesCategory = selectedCategory == "All" || task.category.equals(selectedCategory, ignoreCase = true)
        val matchesQuery = searchQuery.isBlank() || task.title.contains(searchQuery, ignoreCase = true) || task.category.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesQuery
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .windowInsetsPadding(WindowInsets.statusBars)
            .testTag("task_catalog_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            // Header Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("catalog_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF1E293B)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Task Catalog",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Reusable tasks across all routines",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.restoreCatalogTasks() },
                        modifier = Modifier
                            .background(Color(0xFFEEF2FF), RoundedCornerShape(12.dp))
                            .size(40.dp)
                            .testTag("restore_catalog_tasks_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Restore Tasks from Routines",
                            tint = Color(0xFF4F46E5),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Add Task Button
                    Button(
                        onClick = { isCreatingTask = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("create_catalog_task_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Task", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search tasks in catalog...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color(0xFF64748B))
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFF64748B))
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("catalog_search_input"),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Category Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().testTag("catalog_category_row")
            ) {
                items(categories) { cat ->
                    val isSelected = cat == selectedCategory
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isSelected) Color(0xFF4F46E5) else Color.White,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = cat,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xFF475569)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Count summary
            Text(
                text = "${filteredTasks.size} tasks available",
                fontSize = 12.sp,
                color = Color(0xFF64748B),
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Tasks List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("catalog_task_list")
            ) {
                items(filteredTasks, key = { it.id }) { task ->
                    val associatedRoutines = remember(task.id, allRoutines) {
                        allRoutines.filter { r ->
                            r.steps.any { it.catalogTaskId == task.id || (it.catalogTaskId == null && it.title.equals(task.title, ignoreCase = true)) }
                        }
                    }
                    val totalRuns = remember(task.id, runHistory) {
                        runHistory.count { it.catalogTaskId == task.id || (it.catalogTaskId == null && it.stepTitle.equals(task.title, ignoreCase = true)) }
                    }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewingDetailsTask = task }
                            .testTag("catalog_card_${task.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                // Task Icon
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val taskColor = try {
                                        Color(android.graphics.Color.parseColor(task.colorHex.ifEmpty { "#0284C7" }))
                                    } catch (_: Exception) {
                                        Color(0xFF0284C7)
                                    }
                                    TimelineStepIcon(
                                        iconKey = task.iconKey,
                                        tint = taskColor,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = task.title,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                        if (task.isImportant) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0xFFFEF3C7), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "⭐ ${task.starReward}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color(0xFFD97706)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFFEEF2FF), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = task.category,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF4F46E5)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Text(
                                            text = "${task.defaultMinutes}m nominal (${task.minMinutes}m rush - ${task.maxMinutes}m relaxed)",
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Usage & History Pills
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .background(if (associatedRoutines.isNotEmpty()) Color(0xFFE0F2FE) else Color(0xFFF1F5F9), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (associatedRoutines.isNotEmpty()) "🔗 Used in ${associatedRoutines.size} routine${if (associatedRoutines.size > 1) "s" else ""}" else "🔗 Unused",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (associatedRoutines.isNotEmpty()) Color(0xFF0369A1) else Color(0xFF64748B)
                                            )
                                        }

                                        if (totalRuns > 0) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0xFFDCFCE7), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "✓ $totalRuns completed",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF15803D)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Actions
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { editingTask = task },
                                    modifier = Modifier.size(34.dp).testTag("edit_catalog_task_${task.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit task",
                                        tint = Color(0xFF0284C7),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { taskPendingDelete = task },
                                    modifier = Modifier.size(34.dp).testTag("delete_catalog_task_${task.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete task",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // Delete Confirmation Dialog
        taskPendingDelete?.let { task ->
            val associatedRoutines = remember(task.id, allRoutines) {
                allRoutines.filter { r ->
                    r.steps.any { it.catalogTaskId == task.id || (it.catalogTaskId == null && it.title.equals(task.title, ignoreCase = true)) }
                }
            }
            AlertDialog(
                onDismissRequest = { taskPendingDelete = null },
                title = {
                    Text(text = "Delete Catalog Task?", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                },
                text = {
                    Column {
                        Text(text = "Are you sure you want to delete \"${task.title}\"?")
                        if (associatedRoutines.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "⚠️ This task is currently used in ${associatedRoutines.size} routine(s): ${associatedRoutines.joinToString { it.title }}. Routines will keep their current copies, but will no longer be linked to this catalog item.",
                                fontSize = 12.sp,
                                color = Color(0xFFDC2626)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteCatalogTask(task.id)
                            taskPendingDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        modifier = Modifier.testTag("confirm_delete_catalog_task_btn")
                    ) {
                        Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { taskPendingDelete = null },
                        modifier = Modifier.testTag("cancel_delete_catalog_task_btn")
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Detail Dialog
        viewingDetailsTask?.let { taskToView ->
            CatalogTaskDetailDialog(
                task = taskToView,
                allRoutines = allRoutines,
                runHistory = runHistory,
                onDismiss = { viewingDetailsTask = null },
                onEdit = {
                    editingTask = taskToView
                    viewingDetailsTask = null
                }
            )
        }

        // Add / Edit Dialog using unified TaskEditDialog
        if (isCreatingTask || editingTask != null) {
            TaskEditDialog(
                initialCatalogTask = editingTask,
                allRoutines = allRoutines,
                onDismiss = {
                    isCreatingTask = false
                    editingTask = null
                },
                onSaveCatalogTask = { savedTask ->
                    viewModel.addCatalogTask(savedTask)
                    isCreatingTask = false
                    editingTask = null
                }
            )
        }
    }
}

@Deprecated(
    message = "CatalogTaskEditDialog is deprecated. Use common TaskEditDialog instead.",
    replaceWith = ReplaceWith("TaskEditDialog(initialCatalogTask = initialTask, onSaveCatalogTask = onSave, onDismiss = onDismiss)")
)
@Composable
fun CatalogTaskEditDialog(
    initialTask: CatalogTask?,
    onDismiss: () -> Unit,
    onSave: (CatalogTask) -> Unit
) {
    TaskEditDialog(
        initialCatalogTask = initialTask,
        onSaveCatalogTask = onSave,
        onDismiss = onDismiss
    )
}

@Composable
private fun LegacyCatalogTaskEditDialog(
    initialTask: CatalogTask?,
    onDismiss: () -> Unit,
    onSave: (CatalogTask) -> Unit
) {
    var title by remember { mutableStateOf(initialTask?.title ?: "") }
    var category by remember { mutableStateOf(initialTask?.category ?: "General") }
    var defaultMinutes by remember { mutableIntStateOf(initialTask?.defaultMinutes ?: 5) }
    var minMinutes by remember { mutableIntStateOf(initialTask?.minMinutes ?: 2) }
    var maxMinutes by remember { mutableIntStateOf(initialTask?.maxMinutes ?: 10) }
    var isImportant by remember { mutableStateOf(initialTask?.isImportant ?: true) }
    var starReward by remember { mutableIntStateOf(initialTask?.starReward ?: 1) }
    var iconKey by remember { mutableStateOf(initialTask?.iconKey ?: "clothes") }
    var colorHex by remember { mutableStateOf(initialTask?.colorHex ?: "#4ADE80") }

    val categoryPresets = listOf("General", "Hygiene", "Morning", "Bedtime", "Departure", "Meals", "School", "Home")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("catalog_task_edit_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialTask != null) "Edit Catalog Task" else "New Catalog Task",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Live Preview Card
                val previewColor = try {
                    Color(android.graphics.Color.parseColor(colorHex))
                } catch (_: Exception) {
                    Color(0xFF4ADE80)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .background(previewColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        TimelineStepIcon(
                            iconKey = iconKey,
                            tint = previewColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title.ifBlank { "Task Preview" },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            maxLines = 1
                        )
                        Text(
                            text = "$defaultMinutes min nominal • $category",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    if (isImportant) {
                        Text(
                            text = "⭐ $starReward",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD97706)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("catalog_task_title_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Icon Selection
                val selectedIconLabel = Routine.AVAILABLE_ICONS.firstOrNull { it.first == iconKey }?.second ?: iconKey
                Text(
                    text = "Choose Icon: $selectedIconLabel",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF334155)
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("catalog_icon_selector_row")
                ) {
                    items(Routine.AVAILABLE_ICONS) { (key, _) ->
                        val isSelected = iconKey == key
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color(0xFF4F46E5) else Color(0xFFF1F5F9))
                                .clickable { iconKey = key }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            TimelineStepIcon(
                                iconKey = key,
                                tint = if (isSelected) Color.White else Color(0xFF475569),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Color Selection
                Text(
                    text = "Choose Color Tag",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF334155)
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("catalog_color_selector_row")
                ) {
                    items(Routine.AVAILABLE_COLORS) { (hex, name) ->
                        val col = try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { Color(0xFF4ADE80) }
                        val isSelected = colorHex.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(col)
                                .clickable { colorHex = hex }
                                .then(
                                    if (isSelected) Modifier.border(3.dp, Color(0xFF0F172A), CircleShape) else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected $name",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Category Chips
                Text(text = "Category", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categoryPresets) { cat ->
                        Box(
                            modifier = Modifier
                                .background(
                                    if (category == cat) Color(0xFF4F46E5) else Color(0xFFF1F5F9),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { category = cat }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = cat,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (category == cat) Color.White else Color(0xFF334155)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 1. Nominal Duration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⏱️ Nominal Duration: $defaultMinutes min",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (defaultMinutes > 1) {
                                    defaultMinutes--
                                    if (minMinutes > defaultMinutes) minMinutes = defaultMinutes
                                    if (maxMinutes < defaultMinutes) maxMinutes = defaultMinutes + 1
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFF1F5F9), CircleShape)
                                .testTag("catalog_nominal_decrement_btn")
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease nominal minutes", tint = Color(0xFF334155), modifier = Modifier.size(18.dp))
                        }
                        Text(
                            text = "${defaultMinutes}m",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0284C7)
                        )
                        IconButton(
                            onClick = {
                                defaultMinutes++
                                if (maxMinutes < defaultMinutes) maxMinutes = defaultMinutes + 5
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFF1F5F9), CircleShape)
                                .testTag("catalog_nominal_increment_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase nominal minutes", tint = Color(0xFF334155), modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                // Quick Presets Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(1, 2, 3, 5, 10, 15, 20).forEach { m ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (defaultMinutes == m) Color(0xFF0284C7) else Color(0xFFF1F5F9),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    defaultMinutes = m
                                    minMinutes = (m / 2).coerceAtLeast(1)
                                    maxMinutes = (m * 2).coerceAtLeast(m + 1)
                                }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${m}m",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (defaultMinutes == m) Color.White else Color(0xFF334155)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Rush Duration (Express Pace)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "⚡ Rush Duration (Express pace)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626)
                        )
                        Text(
                            text = "Used when routine auto-heals behind schedule",
                            fontSize = 10.sp,
                            color = Color(0xFF991B1B)
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (minMinutes > 1) minMinutes--
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFFEE2E2), CircleShape)
                                .testTag("catalog_rush_decrement_btn")
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease rush minutes", tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                        }
                        Text(
                            text = "${minMinutes}m",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626)
                        )
                        IconButton(
                            onClick = {
                                if (minMinutes < defaultMinutes) minMinutes++
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFFEE2E2), CircleShape)
                                .testTag("catalog_rush_increment_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase rush minutes", tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Relaxed Duration (Slow Pace)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "🌱 Relaxed Duration (Slow pace)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF16A34A)
                        )
                        Text(
                            text = "Used when scaling routine with plenty of time",
                            fontSize = 10.sp,
                            color = Color(0xFF166534)
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (maxMinutes > defaultMinutes) maxMinutes--
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFDCFCE7), CircleShape)
                                .testTag("catalog_relax_decrement_btn")
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease relaxed minutes", tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
                        }
                        Text(
                            text = "${maxMinutes}m",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF16A34A)
                        )
                        IconButton(
                            onClick = {
                                maxMinutes++
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFDCFCE7), CircleShape)
                                .testTag("catalog_relax_increment_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase relaxed minutes", tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Important & Stars
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Important Task (Awards Stars)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Earned on independent completion", fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                    Switch(
                        checked = isImportant,
                        onCheckedChange = { isImportant = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFF59E0B))
                    )
                }

                if (isImportant) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Star Reward:", fontSize = 12.sp, color = Color(0xFF334155))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            (1..3).forEach { s ->
                                Box(
                                    modifier = Modifier
                                        .background(if (starReward == s) Color(0xFFF59E0B) else Color(0xFFFEF3C7), RoundedCornerShape(8.dp))
                                        .clickable { starReward = s }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(text = "⭐ $s", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (starReward == s) Color.White else Color(0xFF92400E))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Source of truth notice
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "💡 Master Task: Saving changes updates icon, color, name, and duration across all routines referencing this task.",
                        fontSize = 11.sp,
                        color = Color(0xFF475569)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Save Button (48dp height minimum)
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            val taskToSave = CatalogTask(
                                id = initialTask?.id ?: "cat_${System.currentTimeMillis()}",
                                title = title.trim(),
                                iconKey = iconKey,
                                colorHex = colorHex,
                                defaultMinutes = defaultMinutes,
                                minMinutes = minMinutes,
                                maxMinutes = maxMinutes,
                                isImportant = isImportant,
                                starReward = starReward,
                                category = category
                            )
                            onSave(taskToSave)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_catalog_task_confirm_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Task to Catalog", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CatalogTaskDetailDialog(
    task: CatalogTask,
    allRoutines: List<Routine>,
    runHistory: List<StepRunHistory>,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    val associatedRoutines = remember(task.id, allRoutines) {
        allRoutines.filter { r ->
            r.steps.any { it.catalogTaskId == task.id || (it.catalogTaskId == null && it.title.equals(task.title, ignoreCase = true)) }
        }
    }
    val matchingRuns = remember(task.id, runHistory) {
        runHistory.filter {
            it.catalogTaskId == task.id || (it.catalogTaskId == null && it.stepTitle.equals(task.title, ignoreCase = true))
        }
    }
    val totalRuns = matchingRuns.size
    val avgSeconds = if (totalRuns > 0) matchingRuns.map { it.actualSecondsTaken }.average().toInt() else 0
    val onTimeCount = matchingRuns.count { it.actualSecondsTaken <= (task.defaultMinutes * 60) }
    val onTimePercent = if (totalRuns > 0) (onTimeCount * 100) / totalRuns else 100
    val soloCount = matchingRuns.count { !it.wasAssisted }
    val soloPercent = if (totalRuns > 0) (soloCount * 100) / totalRuns else 100
    val totalStars = if (task.isImportant) soloCount * task.starReward else 0

    val taskColor = try {
        Color(android.graphics.Color.parseColor(task.colorHex.ifEmpty { "#0284C7" }))
    } catch (_: Exception) {
        Color(0xFF0284C7)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("catalog_task_detail_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(0xFFF1F5F9), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            TimelineStepIcon(
                                iconKey = task.iconKey,
                                tint = taskColor,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = task.title,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = task.category,
                                    fontSize = 12.sp,
                                    color = Color(0xFF4F46E5),
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (task.isImportant) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "• ⭐ ${task.starReward} Star${if (task.starReward > 1) "s" else ""}",
                                        fontSize = 12.sp,
                                        color = Color(0xFFD97706),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("close_catalog_detail_dialog_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Source of Truth Master Notice
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFEEF2FF), RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🏛️", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Source of Truth Task",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF312E81)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "This catalog item is the master definition. Editing it automatically updates the title and duration across all ${associatedRoutines.size} connected routine(s).",
                            fontSize = 11.sp,
                            color = Color(0xFF4338CA),
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Connected Routines Section
                Text(
                    text = "Connected Routines (${associatedRoutines.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (associatedRoutines.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "Not used in any routine yet. You can add this task when editing a routine or building a Quick Routine.",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        associatedRoutines.forEach { r ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "📋", fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = r.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF1E293B)
                                    )
                                }
                                Text(
                                    text = "${r.startTime} - ${r.targetDepartureTime}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Unified Lifetime Metrics Section
                Text(
                    text = "Lifetime Task Performance",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "Aggregated across all routines and runs",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Metrics Grid (2 columns)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricBox(
                        label = "Completions",
                        value = "$totalRuns",
                        sub = if (totalRuns > 0) "total runs" else "no runs yet",
                        modifier = Modifier.weight(1f)
                    )
                    val avgFormatted = if (avgSeconds > 0) "${avgSeconds / 60}m ${avgSeconds % 60}s" else "--"
                    MetricBox(
                        label = "Avg Duration",
                        value = avgFormatted,
                        sub = "target: ${task.defaultMinutes}m",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricBox(
                        label = "On-Time Rate",
                        value = if (totalRuns > 0) "$onTimePercent%" else "--",
                        sub = "within target time",
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        label = "Solo Independence",
                        value = if (totalRuns > 0) "$soloPercent%" else "--",
                        sub = "without parent help",
                        modifier = Modifier.weight(1f)
                    )
                }

                if (task.isImportant && totalStars > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFEF3C7), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "⭐", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Earned $totalStars total star${if (totalStars > 1) "s" else ""} from completing this task independently!",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF92400E)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions: Edit and Close
                Button(
                    onClick = onEdit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("edit_task_from_detail_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit Task Definition", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("dismiss_detail_dialog_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Close", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
                }
            }
        }
    }
}

@Composable
private fun MetricBox(
    label: String,
    value: String,
    sub: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Column {
            Text(text = label, fontSize = 11.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = sub, fontSize = 10.sp, color = Color(0xFF94A3B8))
        }
    }
}

