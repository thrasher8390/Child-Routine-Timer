package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.CatalogTask
import com.example.model.Routine
import com.example.model.RoutineStep
import com.example.ui.theme.SaveNavy

/**
 * Common, unified modal dialog for creating and editing both routine activities (RoutineStep)
 * and reusable master tasks in the task catalog (CatalogTask).
 */
@Composable
fun TaskEditDialog(
    initialStep: RoutineStep? = null,
    initialCatalogTask: CatalogTask? = null,
    catalogTasks: List<CatalogTask> = emptyList(),
    allRoutines: List<Routine> = emptyList(),
    onSave: ((RoutineStep) -> Unit)? = null,
    onSaveCatalogTask: ((CatalogTask) -> Unit)? = null,
    onQuickAddCatalogTask: ((CatalogTask) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val isCatalogMode = initialCatalogTask != null || onSaveCatalogTask != null

    val linkedCatalogTask = remember(initialStep, initialCatalogTask, catalogTasks) {
        if (initialCatalogTask != null) {
            initialCatalogTask
        } else if (initialStep?.catalogTaskId != null) {
            catalogTasks.firstOrNull { it.id == initialStep.catalogTaskId }
        } else if (initialStep != null) {
            catalogTasks.firstOrNull { it.title.equals(initialStep.title, ignoreCase = true) }
        } else null
    }

    var assignedCatalogTaskId by remember {
        mutableStateOf(linkedCatalogTask?.id)
    }
    var isQuickAddedToCatalog by remember {
        mutableStateOf(false)
    }

    var title by remember {
        mutableStateOf(initialCatalogTask?.title ?: initialStep?.title ?: "")
    }
    var category by remember {
        mutableStateOf(initialCatalogTask?.category ?: linkedCatalogTask?.category ?: "General")
    }
    var selectedIcon by remember {
        mutableStateOf(initialCatalogTask?.iconKey ?: initialStep?.iconKey ?: "clothes")
    }
    var selectedColor by remember {
        mutableStateOf(initialCatalogTask?.colorHex ?: initialStep?.colorHex ?: "#4ADE80")
    }
    var defaultMinutes by remember {
        mutableIntStateOf(initialCatalogTask?.defaultMinutes ?: initialStep?.defaultMinutes ?: 10)
    }
    var minMinutes by remember {
        mutableIntStateOf(initialCatalogTask?.minMinutes ?: initialStep?.minMinutes ?: 5)
    }
    var maxMinutes by remember {
        mutableIntStateOf(initialCatalogTask?.maxMinutes ?: initialStep?.maxMinutes ?: 15)
    }
    var isImportant by remember {
        mutableStateOf(initialCatalogTask?.isImportant ?: initialStep?.isImportant ?: false)
    }
    var starReward by remember {
        mutableIntStateOf(initialCatalogTask?.starReward ?: initialStep?.starReward ?: 1)
    }

    val routinesUsingTask = remember(linkedCatalogTask, allRoutines) {
        if (linkedCatalogTask != null) {
            allRoutines.filter { r ->
                r.steps.any {
                    it.catalogTaskId == linkedCatalogTask.id ||
                    (it.catalogTaskId == null && it.title.equals(linkedCatalogTask.title, ignoreCase = true))
                }
            }
        } else emptyList()
    }

    val previewColor = try {
        Color(android.graphics.Color.parseColor(selectedColor))
    } catch (_: Exception) {
        Color(0xFF4ADE80)
    }

    val categoryPresets = listOf("General", "Hygiene", "Morning", "Bedtime", "Departure", "Meals", "School", "Home", "Outdoors", "Travel")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag(if (isCatalogMode) "catalog_task_edit_dialog" else "task_edit_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with title and close icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isCatalogMode) {
                            if (initialCatalogTask == null) "New Catalog Task" else "Edit Catalog Task"
                        } else {
                            if (initialStep == null) "Add Activity" else "Edit Activity"
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF64748B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Live Visual Preview Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .background(previewColor.copy(alpha = 0.20f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            TimelineStepIcon(
                                iconKey = selectedIcon,
                                tint = previewColor,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title.trim().ifEmpty { if (isCatalogMode) "Catalog Task Preview" else "Activity Preview" },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isCatalogMode && category.isNotBlank()) {
                                    Text(
                                        text = category,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4F46E5)
                                    )
                                    Text(text = "•", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                }
                                Text(
                                    text = "⚡${minMinutes}m  ⚖️${defaultMinutes}m  🌱${maxMinutes}m",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                                if (isImportant) {
                                    Text(text = "•", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                    Text(
                                        text = "⭐ $starReward",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFD97706)
                                    )
                                }
                            }
                        }
                    }
                }

                // Shared Catalog / Associated Routines Notice
                if (linkedCatalogTask != null || routinesUsingTask.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEEF2FF)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("shared_catalog_task_notice")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(text = "🔗", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isCatalogMode) "Master Catalog Item" else "Shared Catalog Item",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF312E81)
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                val routineNames = routinesUsingTask.map { it.title }
                                Text(
                                    text = if (routineNames.isNotEmpty()) {
                                        "Linked with ${routineNames.size} routine${if (routineNames.size > 1) "s" else ""} (${routineNames.joinToString(", ")}). Saving changes syncs icon, color, durations, and stars across all routines!"
                                    } else {
                                        "Available in the shared Task Catalog for use in any routine."
                                    },
                                    fontSize = 11.sp,
                                    color = Color(0xFF4338CA),
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                } else if (!isCatalogMode) {
                    // Task is not yet in catalog: provide Quick Add to Catalog!
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isQuickAddedToCatalog) Color(0xFFF0FDF4) else Color(0xFFF8FAFC)
                        ),
                        border = BorderStroke(1.dp, if (isQuickAddedToCatalog) Color(0xFF86EFAC) else Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quick_add_catalog_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = if (isQuickAddedToCatalog) "✅" else "📦", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (isQuickAddedToCatalog) "Added to Task Catalog" else "Not in Task Catalog",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isQuickAddedToCatalog) Color(0xFF166534) else Color(0xFF334155)
                                    )
                                    Text(
                                        text = if (isQuickAddedToCatalog) "Linked as a reusable master task" else "Quick add to reuse across other routines",
                                        fontSize = 11.sp,
                                        color = if (isQuickAddedToCatalog) Color(0xFF15803D) else Color(0xFF64748B)
                                    )
                                }
                            }

                            if (isQuickAddedToCatalog) {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFDCFCE7), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Saved ✓",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF15803D)
                                    )
                                }
                            } else {
                                Button(
                                    onClick = {
                                        val finalTitle = title.trim().ifEmpty { "Activity" }
                                        val catId = "cat_${System.currentTimeMillis()}"
                                        val newCatalogTask = CatalogTask(
                                            id = catId,
                                            title = finalTitle,
                                            iconKey = selectedIcon,
                                            defaultMinutes = defaultMinutes,
                                            minMinutes = minMinutes,
                                            maxMinutes = maxMinutes,
                                            isImportant = isImportant,
                                            starReward = starReward,
                                            colorHex = selectedColor,
                                            category = category.ifBlank { "General" }
                                        )
                                        assignedCatalogTaskId = catId
                                        isQuickAddedToCatalog = true
                                        onQuickAddCatalogTask?.invoke(newCatalogTask)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("quick_add_to_catalog_btn")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Quick Add",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Activity / Task Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (isCatalogMode) "Task Name" else "Activity Name") },
                    placeholder = { Text("e.g. Put on shoes") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_name_input")
                )

                // Category Selection (shown in catalog mode or when task has a category)
                if (isCatalogMode) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Category ($category)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E293B),
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("catalog_category_presets")
                    ) {
                        items(categoryPresets) { preset ->
                            val isSelected = category.equals(preset, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) Color(0xFF4F46E5) else Color(0xFFF1F5F9))
                                    .clickable { category = preset }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = preset,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color(0xFF475569)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 1. Nominal (Default) Duration Selector
                Text(
                    text = "⚖️ Nominal Duration: $defaultMinutes minutes",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E293B),
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = {
                            if (defaultMinutes > 1) {
                                defaultMinutes--
                                if (minMinutes > defaultMinutes) minMinutes = defaultMinutes
                            }
                        },
                        modifier = Modifier
                            .background(Color(0xFFF1F5F9), CircleShape)
                            .size(36.dp)
                            .testTag("catalog_nominal_decrement_btn")
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease nominal minutes", tint = Color(0xFF1E293B))
                    }

                    Text(
                        text = "$defaultMinutes min",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0284C7)
                    )

                    IconButton(
                        onClick = {
                            defaultMinutes++
                            if (maxMinutes < defaultMinutes) maxMinutes = defaultMinutes + 5
                        },
                        modifier = Modifier
                            .background(Color(0xFFF1F5F9), CircleShape)
                            .size(36.dp)
                            .testTag("catalog_nominal_increment_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase nominal minutes", tint = Color(0xFF1E293B))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Rush (Express / Minimum) Duration Selector
                Text(
                    text = "⚡ Rush Duration (Express pace): $minMinutes minutes",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFDC2626),
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = {
                            if (minMinutes > 1) minMinutes--
                        },
                        modifier = Modifier
                            .background(Color(0xFFFEE2E2), CircleShape)
                            .size(36.dp)
                            .testTag("catalog_rush_decrement_btn")
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease rush minutes", tint = Color(0xFFDC2626))
                    }

                    Text(
                        text = "$minMinutes min",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626)
                    )

                    IconButton(
                        onClick = {
                            if (minMinutes < defaultMinutes) minMinutes++
                        },
                        modifier = Modifier
                            .background(Color(0xFFFEE2E2), CircleShape)
                            .size(36.dp)
                            .testTag("catalog_rush_increment_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase rush minutes", tint = Color(0xFFDC2626))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Relaxed (Maximum) Duration Selector
                Text(
                    text = "🌱 Relaxed Duration (Slow pace): $maxMinutes minutes",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF16A34A),
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = {
                            if (maxMinutes > defaultMinutes) maxMinutes--
                        },
                        modifier = Modifier
                            .background(Color(0xFFDCFCE7), CircleShape)
                            .size(36.dp)
                            .testTag("catalog_relax_decrement_btn")
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease relaxed minutes", tint = Color(0xFF16A34A))
                    }

                    Text(
                        text = "$maxMinutes min",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF16A34A)
                    )

                    IconButton(
                        onClick = {
                            maxMinutes++
                        },
                        modifier = Modifier
                            .background(Color(0xFFDCFCE7), CircleShape)
                            .size(36.dp)
                            .testTag("catalog_relax_increment_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase relaxed minutes", tint = Color(0xFF16A34A))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Icon Selection
                val selectedLabel = Routine.AVAILABLE_ICONS.firstOrNull { it.first == selectedIcon }?.second ?: selectedIcon
                Text(
                    text = "Choose Icon ($selectedLabel)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E293B),
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(Routine.AVAILABLE_ICONS) { (iconKey, _) ->
                        val isSelected = selectedIcon == iconKey
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color(0xFF0284C7) else Color(0xFFF1F5F9))
                                .clickable { selectedIcon = iconKey }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            TimelineStepIcon(
                                iconKey = iconKey,
                                tint = if (isSelected) Color.White else Color(0xFF475569)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Color Selection
                Text(
                    text = "Choose Color Tag",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E293B),
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(Routine.AVAILABLE_COLORS) { (hex, _) ->
                        val color = try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { Color(0xFF4ADE80) }
                        val isSelected = selectedColor.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = hex }
                                .then(
                                    if (isSelected) Modifier.border(3.dp, Color(0xFF1E293B), CircleShape) else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Important Task & Star Reward Setting
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isImportant) Color(0xFFFEF3C7) else Color(0xFFF8FAFC)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "⭐ Important Task",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E)
                                )
                                Text(
                                    text = "Awards stars when finished in time!",
                                    fontSize = 12.sp,
                                    color = Color(0xFF78350F)
                                )
                            }
                            Switch(
                                checked = isImportant,
                                onCheckedChange = { isImportant = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFFF59E0B),
                                    checkedTrackColor = Color(0xFFFDE68A)
                                )
                            )
                        }

                        if (isImportant) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Reward Amount:",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF92400E)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(1, 2, 3).forEach { count ->
                                        val isSelected = starReward == count
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) Color(0xFFF59E0B) else Color.White)
                                                .clickable { starReward = count }
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "⭐ $count",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (isSelected) Color.White else Color(0xFF92400E)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons (48dp height minimum, fully visible, accessible touch targets)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("cancel_task_dialog_btn")
                    ) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            val finalTitle = title.trim().ifEmpty { if (isCatalogMode) "Catalog Task" else "Activity" }
                            if (isCatalogMode && onSaveCatalogTask != null) {
                                val catalogTask = CatalogTask(
                                    id = initialCatalogTask?.id ?: initialStep?.catalogTaskId ?: "cat_${System.currentTimeMillis()}",
                                    title = finalTitle,
                                    iconKey = selectedIcon,
                                    defaultMinutes = defaultMinutes,
                                    minMinutes = minMinutes,
                                    maxMinutes = maxMinutes,
                                    isImportant = isImportant,
                                    starReward = starReward,
                                    colorHex = selectedColor,
                                    category = category.trim().ifEmpty { "General" }
                                )
                                onSaveCatalogTask(catalogTask)
                            } else if (onSave != null) {
                                val newStep = RoutineStep(
                                    id = initialStep?.id ?: "step_${System.currentTimeMillis()}",
                                    title = finalTitle,
                                    iconKey = selectedIcon,
                                    minMinutes = minMinutes,
                                    maxMinutes = maxMinutes,
                                    defaultMinutes = defaultMinutes,
                                    colorHex = selectedColor,
                                    isImportant = isImportant,
                                    starReward = starReward,
                                    drawableResName = initialStep?.drawableResName,
                                    catalogTaskId = assignedCatalogTaskId ?: initialStep?.catalogTaskId ?: linkedCatalogTask?.id
                                )
                                onSave(newStep)
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCatalogMode) Color(0xFF4F46E5) else SaveNavy
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("save_task_dialog_btn")
                            .testTag("save_catalog_task_confirm_btn")
                    ) {
                        Text(
                            text = if (isCatalogMode) "Save to Catalog" else "Save Task",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
