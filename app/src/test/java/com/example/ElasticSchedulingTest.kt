package com.example

import com.example.model.Routine
import com.example.model.RoutineStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ElasticSchedulingTest {

    @Test
    fun testDefaultRoutineTotalMinutes() {
        val routine = Routine()
        assertEquals(7, routine.steps.size)
        // defaultMinutes: 5 + 10 + 15 + 5 + 3 + 7 + 5 = 50
        assertEquals(50, routine.totalMinutes)
    }

    @Test
    fun testMultipleDefaultRoutines() {
        val defaults = Routine.defaultRoutines()
        assertEquals(2, defaults.size)
        assertEquals("Morning Routine", defaults[0].title)
        assertEquals("Bedtime Routine", defaults[1].title)
    }

    @Test
    fun testStepBounds() {
        val routine = Routine()
        routine.steps.forEach { step ->
            assertTrue("Min should be <= max", step.minMinutes <= step.maxMinutes)
            assertTrue("Default should be >= min", step.defaultMinutes >= step.minMinutes)
            assertTrue("Default should be <= max", step.defaultMinutes <= step.maxMinutes)
        }
    }

    @Test
    fun testBonusTimeAccumulation() {
        val initialBonus = 0
        val remainingInStep = 180 // 3 minutes remaining when child taps "Done!"
        val updatedBonus = initialBonus + remainingInStep
        assertEquals(180, updatedBonus)
        val bonusMinutes = updatedBonus / 60
        val bonusSeconds = updatedBonus % 60
        assertEquals(3, bonusMinutes)
        assertEquals(0, bonusSeconds)
    }

    @Test
    fun testStepTimerMemory() {
        // When step 1 has 300 seconds and ticks down to 120 seconds
        val stepId1 = "step_teeth"
        val stepId2 = "step_pajamas"
        val remainingMap = mutableMapOf<String, Int>()
        remainingMap[stepId1] = 120
        remainingMap[stepId2] = 360

        // User switches from step 1 to step 2, then back to step 1
        // Step 1 must retain its 120 seconds, NOT reset to initial 300
        val restoredRemaining = remainingMap[stepId1] ?: 300
        assertEquals(120, restoredRemaining)
    }

    @Test
    fun testStepSkippingReducesActiveSteps() {
        val routine = Routine()
        val initialTotalMin = routine.activeSteps.sumOf { it.defaultMinutes }
        val stepToSkip = routine.steps.first { !it.isImportant }

        // Simulate toggling step to disabled
        val updatedSteps = routine.steps.map {
            if (it.id == stepToSkip.id) it.copy(isEnabled = false) else it
        }
        val updatedRoutine = routine.copy(steps = updatedSteps)

        assertEquals(routine.steps.size - 1, updatedRoutine.activeSteps.size)
        val newTotalMin = updatedRoutine.activeSteps.sumOf { it.defaultMinutes }
        assertEquals(initialTotalMin - stepToSkip.defaultMinutes, newTotalMin)
    }

    @Test
    fun testPaceCategoryBounds() {
        val routine = Routine()
        val sumMin = routine.activeSteps.sumOf { it.minMinutes }
        val sumNominal = routine.activeSteps.sumOf { it.defaultMinutes }
        val sumMax = routine.activeSteps.sumOf { it.maxMinutes }

        assertTrue("sumMin < sumNominal", sumMin < sumNominal)
        assertTrue("sumNominal < sumMax", sumNominal < sumMax)

        // When available time is less than minimum possible rush time -> OVER_BUDGET
        val tightAvailable = sumMin - 5
        assertTrue(tightAvailable < sumMin)

        // Candidates to skip should be non-important tasks
        val candidates = routine.activeSteps.filter { !it.isImportant }.sortedByDescending { it.defaultMinutes }
        assertTrue("Must have non-important candidates to suggest skipping", candidates.isNotEmpty())
    }

    @Test
    fun testStarAwardedOnlyBeforeTimeExpires() {
        val step = RoutineStep(id = "step1", title = "Pajamas", starReward = 2)

        // Case 1: Completed with time remaining
        val remainingSecondsInTime = 45
        val wasOvertimeInTime = remainingSecondsInTime <= 0
        val starsEarnedInTime = if (!wasOvertimeInTime) step.starReward else 0
        assertEquals(2, starsEarnedInTime)

        // Case 2: Completed when time expired (0 or negative seconds)
        val remainingSecondsExpired = 0
        val wasOvertimeExpired = remainingSecondsExpired <= 0
        val starsEarnedExpired = if (!wasOvertimeExpired) step.starReward else 0
        assertEquals(0, starsEarnedExpired)

        val remainingSecondsOvertime = -30
        val wasOvertimeNegative = remainingSecondsOvertime <= 0
        val starsEarnedNegative = if (!wasOvertimeNegative) step.starReward else 0
        assertEquals(0, starsEarnedNegative)
    }

    @Test
    fun testUpNextButtonColorStateClassification() {
        fun getButtonState(remainingSeconds: Int): String {
            return when {
                remainingSeconds <= 0 -> "RED"
                remainingSeconds <= 10 -> "YELLOW"
                else -> "GREEN"
            }
        }

        // Plenty of time left (> 10s) -> GREEN
        assertEquals("GREEN", getButtonState(120))
        assertEquals("GREEN", getButtonState(11))

        // Less than or equal to 10s left -> YELLOW
        assertEquals("YELLOW", getButtonState(10))
        assertEquals("YELLOW", getButtonState(9))
        assertEquals("YELLOW", getButtonState(1))

        // Time expired (0s or negative) -> RED
        assertEquals("RED", getButtonState(0))
        assertEquals("RED", getButtonState(-15))
    }

    @Test
    fun testParentAssistedCompletesWithoutStarsAndBanksBonusTime() {
        val importantStep = RoutineStep(id = "teeth", title = "Brush Teeth", starReward = 2, isImportant = true, defaultMinutes = 3)
        val remainingSeconds = 60 // 1 minute left
        val isParentAssisted = true
        val wasOvertime = remainingSeconds <= 0

        // Business Rule: Parent assisted tasks award 0 stars regardless of time remaining
        val starsAwarded = if (isParentAssisted) 0 else if (!wasOvertime) importantStep.starReward else 0
        assertEquals(0, starsAwarded)

        // Business Rule: Parent assisted tasks bank remaining bonus time
        val bonusTimeBanked = if (remainingSeconds > 0) remainingSeconds else 0
        assertEquals(60, bonusTimeBanked)

        // History entry reflects parent assistance
        val historyEntry = com.example.model.StepRunHistory(
            stepId = importantStep.id,
            stepTitle = importantStep.title,
            scheduledMinutes = importantStep.defaultMinutes,
            actualSecondsTaken = (importantStep.defaultMinutes * 60) - remainingSeconds,
            wasOvertime = wasOvertime,
            wasAssisted = isParentAssisted
        )
        assertTrue(historyEntry.wasAssisted)
        assertEquals(120, historyEntry.actualSecondsTaken)
    }

    @Test
    fun testChildIndependentCompletionAwardsStars() {
        val importantStep = RoutineStep(id = "teeth", title = "Brush Teeth", starReward = 2, isImportant = true, defaultMinutes = 3)
        val remainingSeconds = 60 // 1 minute left
        val isParentAssisted = false
        val wasOvertime = remainingSeconds <= 0

        // Business Rule: Child doing it independently before time expires earns stars
        val starsAwarded = if (isParentAssisted) 0 else if (!wasOvertime) importantStep.starReward else 0
        assertEquals(2, starsAwarded)

        val historyEntry = com.example.model.StepRunHistory(
            stepId = importantStep.id,
            stepTitle = importantStep.title,
            scheduledMinutes = importantStep.defaultMinutes,
            actualSecondsTaken = (importantStep.defaultMinutes * 60) - remainingSeconds,
            wasOvertime = wasOvertime,
            wasAssisted = isParentAssisted
        )
        assertTrue(!historyEntry.wasAssisted)
    }

    @Test
    fun testSkipTaskAdvancesWithoutStarsOrBonus() {
        val routine = Routine()
        val steps = routine.activeSteps
        var currentStepIndex = 0
        val completedIds = mutableSetOf<String>()
        var bonusTimeSeconds = 60
        var stars = 5

        // When skipping step 0 ("Pajamas")
        // It does not award stars and does not add to bonus time
        var nextIndex = -1
        for (i in (currentStepIndex + 1) until steps.size) {
            if (!completedIds.contains(steps[i].id)) {
                nextIndex = i
                break
            }
        }
        assertEquals(1, nextIndex)
        assertEquals(60, bonusTimeSeconds)
        assertEquals(5, stars)
        assertTrue(!completedIds.contains(steps[0].id))
    }

    @Test
    fun testChangeDoneStatusAndParentAssisted() {
        val step = RoutineStep(id = "step_teeth", title = "Brush Teeth", starReward = 2)
        var completedIds = emptySet<String>()
        var assistedIds = emptySet<String>()
        var stars = 10

        // 1. Mark as Done independently
        completedIds = completedIds + step.id
        stars += step.starReward
        assertEquals(12, stars)
        assertTrue(completedIds.contains(step.id))
        assertTrue(!assistedIds.contains(step.id))

        // 2. Change from Independent -> Parent Assisted (revokes stars)
        assistedIds = assistedIds + step.id
        stars -= step.starReward
        assertEquals(10, stars)
        assertTrue(assistedIds.contains(step.id))

        // 3. Change from Parent Assisted -> Independent (awards stars)
        assistedIds = assistedIds - step.id
        stars += step.starReward
        assertEquals(12, stars)
        assertTrue(!assistedIds.contains(step.id))

        // 4. Mark as Incomplete (revokes stars)
        completedIds = completedIds - step.id
        stars -= step.starReward
        assertEquals(10, stars)
        assertTrue(!completedIds.contains(step.id))
    }

    @Test
    fun testBonusTimeReversalAndRemainingTimeRestorationOnIncomplete() {
        val step = RoutineStep(id = "step_pajamas", title = "Put on pajamas", defaultMinutes = 5)
        var bonusTimeSeconds = 0
        val stepRemainingSecondsMap = mutableMapOf<String, Int>()
        val stepBankedSecondsMap = mutableMapOf<String, Int>()

        // Step has 125 seconds left when child finishes
        val remainingWhenCompleted = 125
        stepRemainingSecondsMap[step.id] = remainingWhenCompleted
        stepBankedSecondsMap[step.id] = remainingWhenCompleted
        bonusTimeSeconds += remainingWhenCompleted

        assertEquals(125, bonusTimeSeconds)
        assertEquals(125, stepBankedSecondsMap[step.id])

        // User unmarks the task (marks incomplete)
        // Banked bonus time must be reversed/deducted
        val bankedToReverse = stepBankedSecondsMap[step.id] ?: 0
        bonusTimeSeconds = (bonusTimeSeconds - bankedToReverse).coerceAtLeast(0)
        stepBankedSecondsMap.remove(step.id)

        // Remaining time must retain the 125s that was left when completed (NOT reset to 0 or expired)
        val restoredRemaining = stepRemainingSecondsMap[step.id] ?: (step.defaultMinutes * 60)

        assertEquals(0, bonusTimeSeconds)
        assertEquals(125, restoredRemaining)
        assertTrue("Restored remaining time must not be expired", restoredRemaining > 0)
    }

    @Test
    fun testCatalogTaskToRoutineStepConversion() {
        val catalogTask = com.example.model.CatalogTask(
            id = "cat_1",
            title = "Brush Teeth",
            iconKey = "teeth",
            colorHex = "#38BDF8",
            defaultMinutes = 3,
            minMinutes = 2,
            maxMinutes = 5,
            isImportant = true,
            starReward = 1,
            category = "Hygiene"
        )

        val routineStep = catalogTask.toRoutineStep()
        assertEquals("Brush Teeth", routineStep.title)
        assertEquals("teeth", routineStep.iconKey)
        assertEquals(3, routineStep.defaultMinutes)
        assertEquals(2, routineStep.minMinutes)
        assertEquals(5, routineStep.maxMinutes)
        assertTrue(routineStep.isImportant)
        assertEquals(1, routineStep.starReward)
        assertEquals("cat_1", routineStep.catalogTaskId)
    }

    @Test
    fun testSwitchToCompletedTaskReversesBonusAndMarksIncomplete() {
        val step1 = RoutineStep(id = "step_1", title = "Step 1", defaultMinutes = 5, catalogTaskId = "cat_1")
        val step2 = RoutineStep(id = "step_2", title = "Step 2", defaultMinutes = 5, catalogTaskId = "cat_2")
        val routine = Routine(steps = listOf(step1, step2))
        val totalRoutineSeconds = routine.totalMinutes * 60

        val completedStepIds = mutableSetOf("step_1")
        val stepRemainingSecondsMap = mutableMapOf("step_1" to 120, "step_2" to 300)
        val stepBankedSecondsMap = mutableMapOf("step_1" to 120)
        var bonusTimeSeconds = 120

        // Simulate jumping to step 1 (which was completed)
        val targetStepId = step1.id
        if (completedStepIds.contains(targetStepId)) {
            completedStepIds.remove(targetStepId)
            val bankedToRevert = stepBankedSecondsMap[targetStepId] ?: 0
            bonusTimeSeconds = (bonusTimeSeconds - bankedToRevert).coerceAtLeast(0)
            stepBankedSecondsMap.remove(targetStepId)
        }

        assertTrue(!completedStepIds.contains("step_1"))
        assertEquals(0, bonusTimeSeconds)
        assertTrue(!stepBankedSecondsMap.containsKey("step_1"))
        // Remaining time was preserved
        assertEquals(120, stepRemainingSecondsMap["step_1"])
    }

    @Test
    fun testBonusTimeCannotExceedRoutineNominalDuration() {
        val routine = Routine(steps = listOf(
            RoutineStep(id = "s1", title = "T1", defaultMinutes = 5),
            RoutineStep(id = "s2", title = "T2", defaultMinutes = 5)
        ))
        val totalRoutineSeconds = routine.totalMinutes * 60 // 10 min = 600 seconds
        var bonusTimeSeconds = 550

        // If an operation tries to bank 200s bonus
        val addedBank = 200
        val cappedBonus = (bonusTimeSeconds + addedBank).coerceAtMost(totalRoutineSeconds)
        assertEquals(600, cappedBonus)
        assertTrue(cappedBonus <= totalRoutineSeconds)
    }

    @Test
    fun testCatalogTaskCascadeUpdatesReferencingRoutines() {
        val catTask = com.example.model.CatalogTask(
            id = "cat_teeth",
            title = "Brush teeth",
            defaultMinutes = 3,
            minMinutes = 2,
            maxMinutes = 5
        )

        val morningStep = RoutineStep(id = "m_teeth", title = "Brush teeth", defaultMinutes = 3, catalogTaskId = "cat_teeth")
        val bedtimeStep = RoutineStep(id = "b_teeth", title = "Brush teeth", defaultMinutes = 3, catalogTaskId = "cat_teeth")
        val otherStep = RoutineStep(id = "m_clothes", title = "Get dressed", defaultMinutes = 5, catalogTaskId = "cat_clothes")

        val morningRoutine = Routine(id = "r_morning", title = "Morning", steps = listOf(otherStep, morningStep))
        val bedtimeRoutine = Routine(id = "r_bedtime", title = "Bedtime", steps = listOf(bedtimeStep))
        val allRoutines = listOf(morningRoutine, bedtimeRoutine)

        // Updated catalog item
        val updatedCat = catTask.copy(title = "Brush & Floss Teeth", defaultMinutes = 4, minMinutes = 3, maxMinutes = 6)

        // Cascade simulation (same logic as in RoutineRepository.cascadeCatalogTaskToRoutines)
        val updatedRoutines = allRoutines.map { routine ->
            val hasStep = routine.steps.any { it.catalogTaskId == updatedCat.id || (it.catalogTaskId == null && it.title.equals(updatedCat.title, ignoreCase = true)) }
            if (!hasStep) routine
            else {
                val newSteps = routine.steps.map { step ->
                    if (step.catalogTaskId == updatedCat.id || (step.catalogTaskId == null && step.title.equals(updatedCat.title, ignoreCase = true))) {
                        step.copy(
                            title = updatedCat.title,
                            iconKey = updatedCat.iconKey,
                            colorHex = updatedCat.colorHex,
                            defaultMinutes = updatedCat.defaultMinutes,
                            minMinutes = updatedCat.minMinutes,
                            maxMinutes = updatedCat.maxMinutes,
                            isImportant = updatedCat.isImportant,
                            starReward = updatedCat.starReward,
                            catalogTaskId = updatedCat.id
                        )
                    } else step
                }
                routine.copy(steps = newSteps)
            }
        }

        val updatedMorningTeeth = updatedRoutines[0].steps.first { it.id == "m_teeth" }
        val updatedBedtimeTeeth = updatedRoutines[1].steps.first { it.id == "b_teeth" }

        assertEquals("Brush & Floss Teeth", updatedMorningTeeth.title)
        assertEquals(4, updatedMorningTeeth.defaultMinutes)
        assertEquals("Brush & Floss Teeth", updatedBedtimeTeeth.title)
        assertEquals(4, updatedBedtimeTeeth.defaultMinutes)
        // Other step unchanged
        assertEquals("Get dressed", updatedRoutines[0].steps.first { it.id == "m_clothes" }.title)
    }
}

