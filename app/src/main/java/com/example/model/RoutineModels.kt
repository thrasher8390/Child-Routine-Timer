package com.example.model

data class RoutineStep(
    val id: String = "",
    val catalogTaskId: String? = null,
    val title: String = "",
    val iconKey: String = "clothes",
    val minMinutes: Int = 5,
    val maxMinutes: Int = 15,
    val defaultMinutes: Int = 10,
    val colorHex: String = "#4ADE80",
    val drawableResName: String? = null,
    val isImportant: Boolean = false,
    val starReward: Int = 1,
    val isEnabled: Boolean = true
)

data class StarPrize(
    val id: String = "",
    val title: String = "",
    val starsNeeded: Int = 10,
    val type: String = "WEEKLY", // "WEEKLY" or "MONTHLY"
    val iconEmoji: String = "🎁",
    val isRedeemed: Boolean = false,
    val redeemedDate: String? = null
)

enum class TimelineSizingMode(
    val id: String,
    val title: String,
    val iconEmoji: String,
    val shortName: String,
    val description: String,
    val pros: String
) {
    BALANCED(
        id = "BALANCED",
        title = "Balanced Hybrid (Recommended)",
        iconEmoji = "⚖️",
        shortName = "Balanced",
        description = "Square-root duration scaling with a comfortable minimum touch height.",
        pros = "Short tasks (2-3m) stay easy to see & tap; longer tasks (15-20m) are visibly larger without dwarfing the rest."
    ),
    EQUAL(
        id = "EQUAL",
        title = "Uniform / Equal Blocks",
        iconEmoji = "▦",
        shortName = "Equal",
        description = "All tasks receive identical vertical height regardless of duration.",
        pros = "Clean, predictable checklist aesthetic. Maximizes tap target size for all tasks equally."
    ),
    PROPORTIONAL(
        id = "PROPORTIONAL",
        title = "Strict Proportional Time",
        iconEmoji = "⏱️",
        shortName = "Proportional",
        description = "Task height is strictly proportional to its scheduled minutes.",
        pros = "True clock-time representation. Instantly shows which tasks take the lion's share of routine time."
    ),
    FOCUS(
        id = "FOCUS",
        title = "Active Task Magnifier",
        iconEmoji = "🎯",
        shortName = "Focus",
        description = "Expands the active task dynamically, compressing completed and future tasks.",
        pros = "Draws the child's attention directly to what they are doing right now."
    )
}

data class Routine(
    val id: String = "routine_morning",
    val userId: String = "",
    val familyId: String = "",
    val title: String = "Morning Routine",
    val targetDepartureTime: String = "07:10",
    val startTime: String = "06:20",
    val starCount: Int = 0,
    val repeatType: String = "Everyday",
    val voiceName: String = "Luma",
    val reminderEnabled: Boolean = true,
    val steps: List<RoutineStep> = defaultMorningSteps(),
    val lastCompletedDate: String? = null
) {
    fun isCompletedToday(): Boolean {
        if (lastCompletedDate == null) return false
        val cal = java.util.Calendar.getInstance()
        val todayKey = String.format(java.util.Locale.getDefault(), "%04d-%02d-%02d", cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1, cal.get(java.util.Calendar.DAY_OF_MONTH))
        return lastCompletedDate == todayKey
    }

    val activeSteps: List<RoutineStep>
        get() = steps.filter { it.isEnabled }.ifEmpty { steps }

    val totalMinutes: Int
        get() = activeSteps.sumOf { it.defaultMinutes }

    val nominalMinutes: Int
        get() = activeSteps.sumOf { it.defaultMinutes }

    val rushMinutes: Int
        get() = activeSteps.sumOf { it.minMinutes }

    val relaxedMinutes: Int
        get() = activeSteps.sumOf { it.maxMinutes }

    fun getWindowMinutes(): Int {
        val startParts = startTime.split(":")
        val startH = startParts.getOrNull(0)?.toIntOrNull() ?: 7
        val startM = startParts.getOrNull(1)?.toIntOrNull() ?: 0
        val startTotal = startH * 60 + startM

        val endParts = targetDepartureTime.split(":")
        val endH = endParts.getOrNull(0)?.toIntOrNull() ?: 8
        val endM = endParts.getOrNull(1)?.toIntOrNull() ?: 0
        val endTotal = endH * 60 + endM

        var diff = endTotal - startTotal
        if (diff <= 0) diff += 24 * 60 // wrap around midnight
        return diff
    }

    fun getBufferMinutes(): Int {
        return (getWindowMinutes() - nominalMinutes).coerceAtLeast(0)
    }

    companion object {
        val AVAILABLE_ICONS = listOf(
            "toilet" to "Toilet 🚽",
            "potty" to "Go Potty 🚽",
            "brush_teeth" to "Brush Teeth",
            "wash_hands" to "Wash Hands",
            "drink_water" to "Drink Water",
            "read_book" to "Read a Book",
            "pick_book" to "Pick a Book",
            "pajamas" to "Put on Pajamas",
            "clothes" to "Clothes & Dress",
            "shoes" to "Shoes & Coat",
            "backpack" to "Pack Backpack",
            "breakfast" to "Eat Breakfast",
            "bath" to "Bath & Shower",
            "wake_up" to "Wake Up",
            "tidy" to "Tidy Up Toys",
            "bed" to "Bed & Sleep",
            "hair" to "Brush Hair",
            "medicine" to "Vitamins / Meds",
            "play" to "Play Time",
            "cuddle" to "Hug & Cuddle",
            "lotion" to "Lotion & Cream",
            "leave_home" to "Ready to Launch"
        )

        val AVAILABLE_COLORS = listOf(
            "#4ADE80" to "Mint Green",
            "#FACC15" to "Sunny Yellow",
            "#FB923C" to "Coral Orange",
            "#2DD4BF" to "Vibrant Teal",
            "#38BDF8" to "Sky Blue",
            "#A855F7" to "Lavender Purple",
            "#F43F5E" to "Rose Pink",
            "#818CF8" to "Soft Indigo"
        )

        fun defaultMorningSteps(): List<RoutineStep> = listOf(
            RoutineStep(
                id = "step_wake_up",
                title = "Wake up",
                iconKey = "wake_up",
                minMinutes = 3,
                maxMinutes = 10,
                defaultMinutes = 5,
                colorHex = "#FACC15"
            ),
            RoutineStep(
                id = "step_dressed",
                catalogTaskId = "cat_clothes",
                title = "Get dressed",
                iconKey = "clothes",
                minMinutes = 5,
                maxMinutes = 15,
                defaultMinutes = 10,
                colorHex = "#4ADE80",
                drawableResName = "art_get_dressed"
            ),
            RoutineStep(
                id = "step_breakfast",
                catalogTaskId = "cat_breakfast",
                title = "Breakfast",
                iconKey = "breakfast",
                minMinutes = 10,
                maxMinutes = 25,
                defaultMinutes = 15,
                colorHex = "#FACC15",
                drawableResName = "art_breakfast"
            ),
            RoutineStep(
                id = "step_restroom",
                catalogTaskId = "cat_potty",
                title = "Go potty & wash hands",
                iconKey = "potty",
                minMinutes = 3,
                maxMinutes = 8,
                defaultMinutes = 5,
                colorHex = "#FB923C"
            ),
            RoutineStep(
                id = "step_teeth",
                catalogTaskId = "cat_teeth",
                title = "Brush teeth",
                iconKey = "brush_teeth",
                minMinutes = 2,
                maxMinutes = 5,
                defaultMinutes = 3,
                colorHex = "#2DD4BF"
            ),
            RoutineStep(
                id = "step_shoes",
                catalogTaskId = "cat_shoes",
                title = "Shoes and Coat",
                iconKey = "shoes",
                minMinutes = 4,
                maxMinutes = 10,
                defaultMinutes = 7,
                colorHex = "#FB923C"
            ),
            RoutineStep(
                id = "step_leave",
                title = "Leave home",
                iconKey = "leave_home",
                minMinutes = 3,
                maxMinutes = 8,
                defaultMinutes = 5,
                colorHex = "#A855F7"
            )
        )

        fun defaultBedtimeSteps(): List<RoutineStep> = listOf(
            RoutineStep(
                id = "bed_bath",
                catalogTaskId = "cat_bath",
                title = "Take a bath",
                iconKey = "bath",
                minMinutes = 10,
                maxMinutes = 20,
                defaultMinutes = 15,
                colorHex = "#38BDF8",
                isImportant = true,
                starReward = 1
            ),
            RoutineStep(
                id = "bed_pajamas",
                catalogTaskId = "cat_pajamas",
                title = "Put on pajamas",
                iconKey = "pajamas",
                minMinutes = 4,
                maxMinutes = 10,
                defaultMinutes = 6,
                colorHex = "#A855F7"
            ),
            RoutineStep(
                id = "bed_teeth",
                catalogTaskId = "cat_teeth",
                title = "Brush teeth",
                iconKey = "brush_teeth",
                minMinutes = 2,
                maxMinutes = 5,
                defaultMinutes = 3,
                colorHex = "#2DD4BF",
                isImportant = true,
                starReward = 1
            ),
            RoutineStep(
                id = "bed_books",
                catalogTaskId = "cat_story",
                title = "Pick bedtime books",
                iconKey = "pick_book",
                minMinutes = 3,
                maxMinutes = 8,
                defaultMinutes = 5,
                colorHex = "#FACC15"
            ),
            RoutineStep(
                id = "bed_story",
                catalogTaskId = "cat_story",
                title = "Story time",
                iconKey = "read_book",
                minMinutes = 10,
                maxMinutes = 20,
                defaultMinutes = 15,
                colorHex = "#818CF8"
            ),
            RoutineStep(
                id = "bed_sleep",
                catalogTaskId = "cat_sleep",
                title = "Lights out & sleep",
                iconKey = "bed",
                minMinutes = 5,
                maxMinutes = 15,
                defaultMinutes = 10,
                colorHex = "#4ADE80",
                isImportant = true,
                starReward = 2
            )
        )

        fun defaultPrizes(): List<StarPrize> = listOf(
            StarPrize("p1", "Ice Cream Treat", 10, "WEEKLY", "🍦"),
            StarPrize("p2", "Movie & Popcorn Night", 15, "WEEKLY", "🎬"),
            StarPrize("p3", "Extra 30m Free Play / Gaming", 12, "WEEKLY", "🎮"),
            StarPrize("p4", "Choose Weekend Family Activity", 20, "WEEKLY", "🌟"),
            StarPrize("p5", "Pick a Toy or Book at Store", 40, "MONTHLY", "🧸"),
            StarPrize("p6", "Trampoline / Theme Park Trip", 50, "MONTHLY", "🎢"),
            StarPrize("p7", "Pizza & Arcade Party", 45, "MONTHLY", "🍕"),
            StarPrize("p8", "Disney Trip", 100, "MONTHLY", "🏰")
        )

        fun defaultRoutines(): List<Routine> = listOf(
            Routine(
                id = "routine_morning",
                title = "Morning Routine",
                startTime = "06:20",
                targetDepartureTime = "07:10",
                repeatType = "Everyday",
                steps = defaultMorningSteps()
            ),
            Routine(
                id = "routine_bedtime",
                title = "Bedtime Routine",
                startTime = "19:30",
                targetDepartureTime = "20:30",
                repeatType = "Everyday",
                steps = defaultBedtimeSteps()
            )
        )
    }
}

enum class SessionStatus {
    RUNNING,
    PAUSED,
    COMPLETED
}

data class ActiveSession(
    val sessionId: String = "default_session",
    val routineId: String = "routine_morning",
    val routineTitle: String = "Morning Routine",
    val currentStepId: String = "",
    val userId: String = "",
    val familyId: String = "",
    val status: SessionStatus = SessionStatus.RUNNING,
    val currentStepIndex: Int = 0,
    val stepRemainingSeconds: Int = 0,
    val stepTotalSeconds: Int = 0,
    val bonusTimeSeconds: Int = 0,
    val totalElapsedSeconds: Int = 0,
    val completedStepIds: List<String> = emptyList(),
    val assistedStepIds: List<String> = emptyList(),
    val skippedStepIds: List<String> = emptyList(),
    val stepDurationsSeconds: List<Int> = emptyList(),
    val stepDurationsSecondsMap: Map<String, Int> = emptyMap(),
    val stepRemainingSecondsMap: Map<String, Int> = emptyMap(),
    val stepBankedSecondsMap: Map<String, Int> = emptyMap(),
    val orderedStepIds: List<String> = emptyList(),
    val bonusSteps: List<RoutineStep> = emptyList(),
    val targetEndTime: String = "",
    val startedAtWallTime: String = "",
    val initialAvailableMinutes: Int = 0,
    val startEpochMillis: Long = System.currentTimeMillis(),
    val stepStartEpochMillis: Long = System.currentTimeMillis(),
    val paceMode: String = "ELASTIC",
    val controllerDeviceId: String = "",
    val taskIntervals: List<TaskInterval> = emptyList(),
    val eventLog: List<SessionEvent> = emptyList(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isOvertime: Boolean
        get() = stepRemainingSeconds <= 0

    val overtimeSeconds: Int
        get() = if (stepRemainingSeconds < 0) -stepRemainingSeconds else 0

    fun getDynamicRemainingSeconds(): Int {
        if (status != SessionStatus.RUNNING) return stepRemainingSeconds
        val elapsedSec = ((System.currentTimeMillis() - stepStartEpochMillis) / 1000).toInt()
        return stepRemainingSeconds - elapsedSec
    }

    /**
     * Resolves the complete ordered list of active steps for this session,
     * including routine tasks and session-specific bonus break activities.
     */
    fun getAllSteps(routineSteps: List<RoutineStep>): List<RoutineStep> {
        val all = (routineSteps + bonusSteps).distinctBy { it.id }
        if (orderedStepIds.isEmpty()) return all
        val map = all.associateBy { it.id }
        val ordered = orderedStepIds.mapNotNull { map[it] }
        val remaining = all.filter { !orderedStepIds.contains(it.id) }
        return ordered + remaining
    }

    /**
     * Resolves the allocated duration in seconds for a specific step ID from the ID-keyed map,
     * falling back to default minutes from the routine if needed.
     */
    fun getStepDurationSeconds(stepId: String, defaultFallbackSec: Int = 300): Int {
        return stepDurationsSecondsMap[stepId] ?: defaultFallbackSec
    }

    /**
     * Calculates the exact elapsed time spent on a step across all entry/exit intervals.
     */
    fun getSecondsSpentOnStep(stepId: String, currentEpochMillis: Long = System.currentTimeMillis()): Int {
        var totalMs = 0L
        for (interval in taskIntervals) {
            if (interval.stepId == stepId) {
                val end = interval.exitEpochMillis ?: currentEpochMillis
                totalMs += (end - interval.enterEpochMillis).coerceAtLeast(0)
            }
        }
        return (totalMs / 1000).toInt()
    }
}

data class SessionEvent(
    val eventId: String = "",
    val eventType: String = "", // START_ROUTINE, SWITCH_STEP, COMPLETE_STEP, SKIP_STEP, ADJUST_TIME, ADD_BONUS_TASK, PACE_CHANGE
    val stepId: String = "",
    val stepTitle: String = "",
    val stepIndex: Int = 0,
    val deviceId: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val allocatedDurationSeconds: Int = 0,
    val remainingSeconds: Int = 0,
    val savedBonusSeconds: Int = 0,
    val bonusTimeSeconds: Int = 0,
    val isAssisted: Boolean = false,
    val paceMode: String = ""
)

data class TaskInterval(
    val stepId: String = "",
    val enterEpochMillis: Long = System.currentTimeMillis(),
    val exitEpochMillis: Long? = null,
    val allocatedDurationSeconds: Int = 0
)

data class StepRunHistory(
    val id: String = "",
    val routineId: String = "",
    val stepId: String = "",
    val stepTitle: String = "",
    val catalogTaskId: String? = null,
    val scheduledMinutes: Int = 5,
    val actualSecondsTaken: Int = 300,
    val wasOvertime: Boolean = false,
    val wasAssisted: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

enum class RecommendationType {
    INCREASE,
    DECREASE,
    BALANCED
}

data class TimerRecommendation(
    val stepId: String,
    val stepTitle: String,
    val currentMinutes: Int,
    val recommendedMinutes: Int,
    val deltaMinutes: Int,
    val avgActualMinutes: Float,
    val reason: String,
    val type: RecommendationType
)

enum class PaceCategory {
    RELAXED,      // Plenty of time; tasks stretched to max with cushion
    NOMINAL,      // Normal standard speed; tasks at default duration
    RUSH,         // Tight time; tasks compressed proportionally toward minimums
    OVER_BUDGET   // Available time is less than minimum rush duration; tasks need cutting
}

data class PaceCoachState(
    val targetEndTime: String,
    val availableMinutes: Int,
    val paceCategory: PaceCategory,
    val calculatedFinishTime: String,
    val deltaMinutes: Int, // positive = minutes late/short, negative = minutes early/cushion, 0 = on time
    val allocatedStepDurationsMinutes: List<Int>,
    val suggestedTasksToSkip: List<RoutineStep> = emptyList(),
    val coachingMessage: String = ""
)

data class CatalogTask(
    val id: String = "",
    val title: String = "",
    val iconKey: String = "clothes",
    val defaultMinutes: Int = 5,
    val minMinutes: Int = 2,
    val maxMinutes: Int = 10,
    val isImportant: Boolean = false,
    val starReward: Int = 1,
    val colorHex: String = "#4ADE80",
    val category: String = "General"
) {
    fun toRoutineStep(newId: String = "step_${System.currentTimeMillis()}"): RoutineStep {
        return RoutineStep(
            id = newId,
            catalogTaskId = id,
            title = title,
            iconKey = iconKey,
            defaultMinutes = defaultMinutes,
            minMinutes = minMinutes,
            maxMinutes = maxMinutes,
            isImportant = isImportant,
            starReward = starReward,
            colorHex = colorHex
        )
    }

    companion object {
        fun defaultCatalog(): List<CatalogTask> = listOf(
            CatalogTask("cat_teeth", "Brush teeth", "teeth", 3, 2, 5, true, 2, "#38BDF8", "Hygiene"),
            CatalogTask("cat_clothes", "Get dressed", "clothes", 7, 4, 12, true, 1, "#4ADE80", "Morning"),
            CatalogTask("cat_shoes", "Shoes and Coat", "shoes", 5, 3, 8, true, 1, "#F97316", "Departure"),
            CatalogTask("cat_backpack", "Pack backpack", "backpack", 4, 2, 8, false, 1, "#A855F7", "School"),
            CatalogTask("cat_breakfast", "Eat breakfast", "breakfast", 15, 10, 20, false, 1, "#FBBF24", "Meals"),
            CatalogTask("cat_water", "Fill water bottle", "water", 2, 1, 4, false, 1, "#0284C7", "Departure"),
            CatalogTask("cat_potty", "Bathroom break", "toilet", 3, 2, 6, false, 1, "#6366F1", "Hygiene"),
            CatalogTask("cat_sunscreen", "Put on sunscreen", "sunscreen", 3, 2, 5, false, 1, "#F59E0B", "Outdoors"),
            CatalogTask("cat_tidy", "Clean up toys", "tidy", 8, 5, 15, false, 1, "#10B981", "Home"),
            CatalogTask("cat_bed", "Make bed", "bed", 4, 2, 6, false, 1, "#EC4899", "Morning"),
            CatalogTask("cat_pajamas", "Put on pajamas", "pajamas", 6, 4, 10, true, 1, "#818CF8", "Bedtime"),
            CatalogTask("cat_bath", "Take a bath", "bath", 15, 10, 20, false, 1, "#3B82F6", "Bedtime"),
            CatalogTask("cat_story", "Story & Reading time", "story", 12, 8, 20, false, 1, "#8B5CF6", "Bedtime"),
            CatalogTask("cat_sleep", "Lights out & sleep", "sleep", 5, 3, 10, false, 1, "#64748B", "Bedtime"),
            CatalogTask("cat_disney", "Pack for Disney", "disney", 10, 5, 15, true, 2, "#EC4899", "Travel")
        )
    }
}


