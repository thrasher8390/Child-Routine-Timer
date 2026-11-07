package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.Bathtub
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.CleanHands
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Wc
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ActiveSession
import com.example.model.Routine
import com.example.model.RoutineStep

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VerticalTimelineBar(
    routine: Routine,
    session: ActiveSession,
    onStepClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    sizingMode: com.example.model.TimelineSizingMode = com.example.model.TimelineSizingMode.BALANCED,
    calculatedFinishTime: String? = null,
    deltaMinutes: Int? = null,
    onAdjustTargetClick: (() -> Unit)? = null,
    onStepLongClick: ((RoutineStep, Int) -> Unit)? = null,
    onHeaderClick: (() -> Unit)? = null
) {
    // Consolidate routine steps and session-specific bonus break activities in exact ordered sequence
    val steps = session.getAllSteps(routine.activeSteps)
    val totalSteps = steps.size.coerceAtLeast(1)

    // Calculate tab weights based on ID-keyed duration map
    val weights = steps.map { step ->
        val durSec = session.getStepDurationSeconds(step.id, step.defaultMinutes * 60)
        when (sizingMode) {
            com.example.model.TimelineSizingMode.BALANCED -> {
                // Square-root duration scaling + baseline floor so short tasks remain easily viewable & clickable
                kotlin.math.sqrt(durSec.toFloat().coerceAtLeast(30f)) + 4f
            }
            com.example.model.TimelineSizingMode.EQUAL -> {
                // Uniform equal height for every task
                1f
            }
            com.example.model.TimelineSizingMode.PROPORTIONAL -> {
                // Strict proportional seconds
                durSec.toFloat().coerceAtLeast(30f)
            }
            com.example.model.TimelineSizingMode.FOCUS -> {
                // Magnify active task while keeping others compact
                val base = kotlin.math.sqrt(durSec.toFloat().coerceAtLeast(30f)) + 4f
                val isActive = if (session.currentStepId.isNotEmpty()) step.id == session.currentStepId else false
                if (isActive) base * 2.5f else base
            }
        }
    }
    val totalWeight = weights.sum().coerceAtLeast(1f)

    val activeIdx = steps.indexOfFirst { it.id == session.currentStepId }
        .takeIf { it >= 0 } ?: session.currentStepIndex.coerceIn(0, totalSteps - 1)

    val displayStartTime = if (session.startedAtWallTime.isNotEmpty()) session.startedAtWallTime else routine.startTime
    val displayEndTime = if (session.targetEndTime.isNotEmpty()) session.targetEndTime else routine.targetDepartureTime

    // Parse start time and target end time in wall-clock minutes
    val startParts = displayStartTime.split(":")
    val startH = startParts.getOrNull(0)?.toIntOrNull() ?: 7
    val startM = startParts.getOrNull(1)?.toIntOrNull() ?: 0
    val startTotalMin = startH * 60 + startM

    val targetParts = displayEndTime.split(":")
    val targetH = targetParts.getOrNull(0)?.toIntOrNull() ?: 8
    val targetM = targetParts.getOrNull(1)?.toIntOrNull() ?: 0
    val targetTotalMin = targetH * 60 + targetM

    // Total window in seconds between routine start and target end
    var totalWindowMin = targetTotalMin - startTotalMin
    if (totalWindowMin <= 0) totalWindowMin += 24 * 60 // handle wrap around midnight
    val totalWindowSec = (totalWindowMin * 60).coerceAtLeast(60)

    // Current wall time in seconds since midnight
    val cal = java.util.Calendar.getInstance()
    val nowH = cal.get(java.util.Calendar.HOUR_OF_DAY)
    val nowM = cal.get(java.util.Calendar.MINUTE)
    val nowS = cal.get(java.util.Calendar.SECOND)
    val nowTotalSec = nowH * 3600 + nowM * 60 + nowS
    val startTotalSec = startTotalMin * 60

    var elapsedWindowSec = nowTotalSec - startTotalSec
    if (elapsedWindowSec < 0) elapsedWindowSec += 24 * 3600

    // Visual Progress (0.0 at Start Time -> 1.0 at Target End Time):
    // Steadily tracks current time across the entire scheduled window. It never jumps around when switching tasks!
    val visualProgress = (elapsedWindowSec.toFloat() / totalWindowSec.toFloat()).coerceIn(0f, 1f)

    // Net remaining seconds:
    // 1. Current active step: actual remaining seconds (or 0 if overtime)
    // 2. Completed steps: 0 seconds remaining
    // 3. Skipped steps: 0 seconds remaining
    // 4. Other uncompleted steps: preserved remaining seconds from session map, or allocated default
    val totalNetRemainingSec = steps.sumOf { step ->
        when {
            step.id == session.currentStepId -> session.stepRemainingSeconds.coerceAtLeast(0)
            session.completedStepIds.contains(step.id) -> 0
            session.skippedStepIds.contains(step.id) -> 0
            else -> {
                val stored = session.stepRemainingSecondsMap[step.id]
                val alloc = session.getStepDurationSeconds(step.id, step.defaultMinutes * 60)
                if (stored != null && stored > 0) minOf(stored, alloc) else alloc
            }
        }
    }

    // Computed finish time and delta from total net remaining seconds
    val totalRemainingMin = (totalNetRemainingSec + 59) / 60
    val nowMin = nowH * 60 + nowM
    val calcFinishTotalMin = (nowMin + totalRemainingMin) % (24 * 60)
    val calcFinishHour = calcFinishTotalMin / 60
    val calcFinishM = calcFinishTotalMin % 60
    val ampm = if (calcFinishHour >= 12) "PM" else "AM"
    val displayHour = if (calcFinishHour == 0) 12 else if (calcFinishHour > 12) calcFinishHour - 12 else calcFinishHour
    val autoFinishDisplay = String.format(java.util.Locale.getDefault(), "%d:%02d %s", displayHour, calcFinishM, ampm)

    var autoDelta = calcFinishTotalMin - targetTotalMin
    if (autoDelta > 720) autoDelta -= 1440
    if (autoDelta < -720) autoDelta += 1440

    val effectiveFinishTime = calculatedFinishTime ?: autoFinishDisplay
    val effectiveDelta = deltaMinutes ?: autoDelta

    Column(
        modifier = modifier
            .width(76.dp)
            .fillMaxHeight()
            .testTag("vertical_timeline_bar"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Start time label at top with sizing mode indicator
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable(enabled = onHeaderClick != null) { onHeaderClick?.invoke() }
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .testTag("timeline_header_btn"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = displayStartTime,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = sizingMode.iconEmoji,
                fontSize = 9.sp
            )
        }

        // Main stacked timeline column
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                steps.forEachIndexed { index, step ->
                    val stepWeight = weights.getOrElse(index) { 1f }
                    val stepColor = try {
                        Color(android.graphics.Color.parseColor(step.colorHex))
                    } catch (_: Exception) {
                        Color(0xFF4ADE80)
                    }
                    val isCompleted = session.completedStepIds.contains(step.id)
                    val isActive = if (session.currentStepId.isNotEmpty()) {
                        step.id == session.currentStepId
                    } else {
                        index == session.currentStepIndex
                    }

                    val isAssisted = session.assistedStepIds.contains(step.id)
                    val isSkipped = session.skippedStepIds.contains(step.id)

                    // Calculate partial spent fraction for this individual step
                    val stepAllocSec = session.getStepDurationSeconds(step.id, step.defaultMinutes * 60).coerceAtLeast(1)
                    val stepRemainingSec = when {
                        isActive -> session.stepRemainingSeconds.coerceAtLeast(0)
                        isCompleted -> 0
                        isSkipped -> 0
                        else -> session.stepRemainingSecondsMap[step.id]?.coerceAtLeast(0) ?: stepAllocSec
                    }
                    val stepSpentFraction = ((stepAllocSec - stepRemainingSec).toFloat() / stepAllocSec.toFloat()).coerceIn(0f, 1f)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(stepWeight)
                            .background(
                                if (isCompleted || isSkipped) stepColor.copy(alpha = 0.45f) else stepColor
                            )
                            .then(
                                if (isActive) {
                                    Modifier.border(2.5.dp, Color(0xFF0F172A), RoundedCornerShape(4.dp))
                                } else {
                                    Modifier
                                }
                            )
                            .combinedClickable(
                                onClick = { onStepClick(index) },
                                onLongClick = { onStepLongClick?.invoke(step, index) }
                            )
                            .padding(horizontal = 2.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // For partially completed or running steps, render an inner spent progress wash
                        if (!isCompleted && !isSkipped && stepSpentFraction > 0.05f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(stepSpentFraction)
                                    .align(Alignment.TopCenter)
                                    .background(Color.Black.copy(alpha = 0.12f))
                            )
                        }

                        // Completed badge in top-left (✓ for independent, 🤝 for assisted, 👁️ for skipped)
                        if (isCompleted || isSkipped) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .size(15.dp)
                                    .background(
                                        when {
                                            isSkipped -> Color(0xFF94A3B8)
                                            isAssisted -> Color(0xFF64748B)
                                            else -> Color(0xFF16A34A)
                                        },
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when {
                                        isSkipped -> "👁️"
                                        isAssisted -> "🤝"
                                        else -> "✓"
                                    },
                                    color = Color.White,
                                    fontSize = if (isAssisted || isSkipped) 7.sp else 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Active Glowing Pulse Tag on Right Edge
                        if (isActive) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .size(width = 4.dp, height = 18.dp)
                                    .background(Color(0xFF0F172A), RoundedCornerShape(2.dp))
                            )
                        }

                        // Step Icon and Duration Label
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            TimelineStepIcon(
                                iconKey = step.iconKey,
                                tint = if (isActive) Color(0xFF0F172A) else Color(0xFF1E293B)
                            )
                            val allocSec = session.getStepDurationSeconds(step.id, step.defaultMinutes * 60)
                            val allocMin = (allocSec + 59) / 60
                            Text(
                                text = "${allocMin}m",
                                fontSize = 8.sp,
                                fontWeight = if (isActive) FontWeight.Black else FontWeight.Bold,
                                color = if (isActive) Color(0xFF0F172A) else Color(0xFF1E293B).copy(alpha = 0.75f),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Red Tracker Line with Circle Indicator precisely tracking along the tabs
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val lineY = size.height * visualProgress.coerceIn(0f, 1f)
                    val redColor = Color(0xFFEF4444)
                    val shadowColor = Color(0x66000000)

                    // Subtle shadow under the red line for high contrast over any task color
                    drawLine(
                        color = shadowColor,
                        start = androidx.compose.ui.geometry.Offset(0f, lineY + 1.dp.toPx()),
                        end = androidx.compose.ui.geometry.Offset(size.width, lineY + 1.dp.toPx()),
                        strokeWidth = 5.dp.toPx()
                    )

                    // Thicker, prominent red horizontal line across the bar
                    drawLine(
                        color = redColor,
                        start = androidx.compose.ui.geometry.Offset(0f, lineY),
                        end = androidx.compose.ui.geometry.Offset(size.width, lineY),
                        strokeWidth = 5.dp.toPx()
                    )

                    // Prominent Red Circle Dot on the right edge
                    drawCircle(
                        color = shadowColor,
                        radius = 8.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(size.width, lineY + 1.dp.toPx())
                    )
                    drawCircle(
                        color = redColor,
                        radius = 7.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(size.width, lineY)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.5.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(size.width, lineY)
                    )
                }
            }
        }

        // Calculated Finish Time & early/late delta badge at bottom
        Column(
            modifier = Modifier
                .padding(top = 4.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .clickable(enabled = onAdjustTargetClick != null) {
                    onAdjustTargetClick?.invoke()
                }
                .padding(horizontal = 2.dp, vertical = 2.dp)
                .testTag("timeline_bottom_finish_badge"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = effectiveFinishTime,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0F172A),
                maxLines = 1
            )
            val (badgeText, badgeBg, badgeTextColor) = when {
                effectiveDelta < 0 -> Triple("🎯 ${-effectiveDelta}m early", Color(0xFFDCFCE7), Color(0xFF15803D))
                effectiveDelta == 0 -> Triple("🎯 On time", Color(0xFFE0F2FE), Color(0xFF0369A1))
                else -> Triple("⚠️ +${effectiveDelta}m late", Color(0xFFFEE2E2), Color(0xFFB91C1C))
            }
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .background(badgeBg, RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = badgeText,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeTextColor,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun TimelineStepIcon(
    iconKey: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val key = iconKey.trim()
    val isToilet = key.equals("toilet", ignoreCase = true) ||
                   key.equals("potty", ignoreCase = true) ||
                   key == "🚽" ||
                   key.equals("restroom", ignoreCase = true) ||
                   key.equals("pee", ignoreCase = true) ||
                   key.equals("poop", ignoreCase = true)

    if (isToilet) {
        Box(
            modifier = modifier.size(26.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🚽",
                fontSize = 18.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        return
    }

    if (key.length <= 4 && key.any { Character.isSurrogate(it) || Character.getType(it) == Character.OTHER_SYMBOL.toInt() }) {
        Box(
            modifier = modifier.size(26.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = key,
                fontSize = 18.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        return
    }

    val icon = when (key.lowercase()) {
        "brush_teeth", "toothbrush", "teeth" -> Icons.Default.Brush
        "wash_hands", "wash", "soap", "hands" -> Icons.Default.CleanHands
        "drink_water", "water", "drink", "cup" -> Icons.Default.LocalDrink
        "read_book", "book", "reading", "story", "stories" -> Icons.Default.AutoStories
        "pick_book", "books", "library", "choose_book" -> Icons.Default.LibraryBooks
        "pajamas", "pjs", "sleepwear", "nightgown" -> Icons.Default.Nightlight
        "clothes", "dressed", "dress", "shirt", "pants" -> Icons.Default.Checkroom
        "shoes", "coat", "boots", "jacket", "sneakers" -> Icons.Default.DirectionsWalk
        "backpack", "bag", "school" -> Icons.Default.Backpack
        "breakfast", "toast", "food", "eat", "meal", "snack" -> Icons.Default.Restaurant
        "bath", "shower", "bathtub" -> Icons.Default.Bathtub
        "wake_up", "sun", "morning" -> Icons.Default.WbSunny
        "tidy", "clean", "toys", "organize" -> Icons.Default.CleaningServices
        "bed", "sleep", "lights_out", "nap" -> Icons.Default.Bed
        "hair", "comb", "hairbrush" -> Icons.Default.ContentCut
        "medicine", "vitamins", "meds" -> Icons.Default.Medication
        "play", "fun", "games", "playtime" -> Icons.Default.SportsEsports
        "cuddle", "hug", "snuggle", "love" -> Icons.Default.Favorite
        "lotion", "cream", "moisturizer", "skincare" -> Icons.Default.CleanHands
        "leave_home", "bus", "launch", "leave" -> Icons.Default.RocketLaunch
        "disney", "castle", "magic", "themepark" -> Icons.Default.Celebration
        else -> Icons.Default.WbSunny
    }

    Icon(
        imageVector = icon,
        contentDescription = iconKey,
        tint = tint,
        modifier = modifier.size(26.dp)
    )
}
