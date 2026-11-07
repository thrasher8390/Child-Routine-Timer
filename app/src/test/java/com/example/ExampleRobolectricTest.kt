package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.ActiveSession
import com.example.model.RecommendationType
import com.example.model.Routine
import com.example.model.RoutineStep
import com.example.model.SessionStatus
import com.example.model.StepRunHistory
import com.example.model.TaskInterval
import com.example.ui.viewmodel.RoutineViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(
                context,
                FirebaseOptions.Builder()
                    .setApplicationId("1:1234567890:android:abcdef12345")
                    .setApiKey("AIzaSyFakeKeyForRobolectricTesting12345")
                    .setProjectId("child-routine-timer")
                    .build()
            )
        }
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Child Routine Timer", appName)
    }

    @Test
    fun `test overtime and bonus time calculation`() {
        val normalSession = ActiveSession(
            stepRemainingSeconds = 60,
            stepTotalSeconds = 120,
            bonusTimeSeconds = 30
        )
        assertFalse("Should not be overtime when remaining > 0", normalSession.isOvertime)
        assertEquals(0, normalSession.overtimeSeconds)

        val overtimeSession = ActiveSession(
            stepRemainingSeconds = -45,
            stepTotalSeconds = 120,
            bonusTimeSeconds = 10
        )
        assertTrue("Should be overtime when remaining <= 0", overtimeSession.isOvertime)
        assertEquals(45, overtimeSession.overtimeSeconds)
    }

    @Test
    fun `test child lock toggle`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = RoutineViewModel(app)

        assertFalse(viewModel.isChildLockActive.value)
        viewModel.toggleChildLock()
        assertTrue(viewModel.isChildLockActive.value)
        viewModel.toggleChildLock()
        assertFalse(viewModel.isChildLockActive.value)
    }

    @Test
    fun `test timer recommendations from history`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = RoutineViewModel(app)

        val routine = viewModel.currentRoutine.value
        val recommendations = viewModel.getTimerRecommendations(routine)

        assertTrue("Recommendations should be generated", recommendations.isNotEmpty())
        
        val dressedRec = recommendations.firstOrNull { it.stepId == "step_dressed" }
        if (dressedRec != null) {
            assertTrue("Dressed task should have a valid recommendedMinutes", dressedRec.recommendedMinutes > 0)
        }
    }

    @Test
    fun `test spend bonus time on activities during routine`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = RoutineViewModel(app)

        viewModel.startRoutine()
        val initialSession = viewModel.activeSession.value
        assertTrue("Session should be running", initialSession != null)

        // Grant 5 minutes of bonus time
        viewModel.grantBonusMinutes(5)
        assertEquals(300, viewModel.activeSession.value?.bonusTimeSeconds)

        // Spend 2 minutes on "Cuddle with Mom"
        viewModel.addBonusActivity(
            title = "Cuddle with Mom",
            durationMinutes = 2,
            iconKey = "cuddle",
            colorHex = "#EC4899",
            startImmediately = true
        )

        val updatedSession = viewModel.activeSession.value
        assertEquals("Bonus time should have 120s deducted", 180, updatedSession?.bonusTimeSeconds)
        assertEquals(120, updatedSession?.stepTotalSeconds)
        assertEquals(120, updatedSession?.stepRemainingSeconds)

        val allSteps = updatedSession?.getAllSteps(viewModel.currentRoutine.value.activeSteps).orEmpty()
        val currentTask = allSteps.find { it.id == updatedSession?.currentStepId }
        assertEquals("Cuddle with Mom", currentTask?.title)
        assertEquals("cuddle", currentTask?.iconKey)
    }

    @Test
    fun `test add bonus activity when previous tasks are completed does not mark task incomplete or corrupt duration`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = RoutineViewModel(app)

        viewModel.startRoutine()
        val session1 = viewModel.activeSession.value!!
        val routine1 = viewModel.currentRoutine.value
        val task0 = routine1.activeSteps[0]
        val task1 = routine1.activeSteps[1]
        val originalTask0Duration = session1.getStepDurationSeconds(task0.id)

        // Mark task 0 as completed and move to task 1
        viewModel.completeCurrentStepEarly()

        val sessionAfterComplete = viewModel.activeSession.value!!
        assertTrue("Task 0 must be in completedStepIds", sessionAfterComplete.completedStepIds.contains(task0.id))
        assertEquals("Current task should now be task 1", task1.id, sessionAfterComplete.currentStepId)

        // Grant bonus time
        viewModel.grantBonusMinutes(10)

        // Add special break for 2 minutes
        viewModel.addBonusActivity(
            title = "Special Break",
            durationMinutes = 2,
            iconKey = "break",
            colorHex = "#8B5CF6",
            startImmediately = true
        )

        val sessionAfterBreak = viewModel.activeSession.value!!

        // Task 0 MUST still be completed!
        assertTrue("Task 0 must remain completed", sessionAfterBreak.completedStepIds.contains(task0.id))
        // Task 0 duration MUST NOT be overwritten with 2 minutes!
        assertEquals("Task 0 duration must remain original", originalTask0Duration, sessionAfterBreak.getStepDurationSeconds(task0.id))

        // The current active step must now be the new special break
        val bonusStep = sessionAfterBreak.bonusSteps.find { it.title == "Special Break" }
        assertNotNull("Bonus step should exist in bonusSteps", bonusStep)
        assertEquals("Current step ID should be the bonus step", bonusStep?.id, sessionAfterBreak.currentStepId)
        assertEquals("Special break duration should be 120s", 120, sessionAfterBreak.getStepDurationSeconds(bonusStep!!.id))
        assertEquals("Step remaining seconds should be 120s", 120, sessionAfterBreak.stepRemainingSeconds)
    }

    @Test
    fun `test timeline sizing modes`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = RoutineViewModel(app)

        assertEquals(com.example.model.TimelineSizingMode.PROPORTIONAL, viewModel.timelineSizingMode.value)

        viewModel.setTimelineSizingMode(com.example.model.TimelineSizingMode.EQUAL)
        assertEquals(com.example.model.TimelineSizingMode.EQUAL, viewModel.timelineSizingMode.value)

        viewModel.cycleTimelineSizingMode()
        assertEquals(com.example.model.TimelineSizingMode.PROPORTIONAL, viewModel.timelineSizingMode.value)

        viewModel.cycleTimelineSizingMode()
        assertEquals(com.example.model.TimelineSizingMode.FOCUS, viewModel.timelineSizingMode.value)

        viewModel.cycleTimelineSizingMode()
        assertEquals(com.example.model.TimelineSizingMode.BALANCED, viewModel.timelineSizingMode.value)
    }

    @Test
    fun `test routine view navigation and back navigation`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = RoutineViewModel(app)

        assertEquals(com.example.ui.viewmodel.Screen.Dashboard, viewModel.currentScreen.value)

        val routine = viewModel.currentRoutine.value
        viewModel.openRoutineView(routine.id)
        assertEquals(com.example.ui.viewmodel.Screen.RoutineView, viewModel.currentScreen.value)

        // Opening edit from routine view
        viewModel.openEditRoutineFromRoutineView()
        assertEquals(com.example.ui.viewmodel.Screen.EditRoutine, viewModel.currentScreen.value)
        viewModel.navigateBack()
        assertEquals(com.example.ui.viewmodel.Screen.RoutineView, viewModel.currentScreen.value)

        // Opening task list from routine view
        viewModel.openTaskListFromRoutineView()
        assertEquals(com.example.ui.viewmodel.Screen.TaskList, viewModel.currentScreen.value)
        viewModel.navigateBack()
        assertEquals(com.example.ui.viewmodel.Screen.RoutineView, viewModel.currentScreen.value)

        // Returning to Dashboard
        viewModel.navigateBack()
        assertEquals(com.example.ui.viewmodel.Screen.Dashboard, viewModel.currentScreen.value)
    }

    @Test
    fun `test completion today status`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = RoutineViewModel(app)

        val routine = viewModel.currentRoutine.value
        val cal = java.util.Calendar.getInstance()
        val todayKey = String.format(java.util.Locale.getDefault(), "%04d-%02d-%02d", cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1, cal.get(java.util.Calendar.DAY_OF_MONTH))

        // Initially not completed today
        val notCompleted = routine.copy(lastCompletedDate = null)
        viewModel.updateRoutine(notCompleted)
        assertFalse("Should not be completed today initially", viewModel.isRoutineCompletedToday(routine.id))

        // Set last completed date to today
        val completedToday = routine.copy(lastCompletedDate = todayKey)
        viewModel.updateRoutine(completedToday)
        assertTrue("Should be completed today", viewModel.isRoutineCompletedToday(routine.id))
    }

    @Test
    fun `test add extra minutes deducts from banked bonus time`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = RoutineViewModel(app)

        viewModel.startRoutine()
        // Bank 3 minutes of bonus time (180s)
        viewModel.grantBonusMinutes(3)
        assertEquals(180, viewModel.activeSession.value?.bonusTimeSeconds)

        val initialRemaining = viewModel.activeSession.value?.stepRemainingSeconds ?: 0

        // Add 2 minutes (+120s) to current task
        viewModel.addExtraMinutes(2)

        val updatedSession = viewModel.activeSession.value
        assertEquals("Should deduct 120s from bonus time", 60, updatedSession?.bonusTimeSeconds)
        assertEquals("Remaining time on step should increase by 120s", initialRemaining + 120, updatedSession?.stepRemainingSeconds)
    }

    @Test
    fun `test auto heal compresses future tasks on deficit`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = RoutineViewModel(app)

        viewModel.startRoutine()
        // Ensure bonus time is 0
        val session = viewModel.activeSession.value?.copy(bonusTimeSeconds = 0)
        assertTrue(session != null)

        val routine = viewModel.currentRoutine.value
        val futureSteps = routine.activeSteps.drop(1)
        assertTrue("Should have future steps to auto-heal", futureSteps.isNotEmpty())

        val initialFutureDuration = session!!.stepDurationsSeconds.getOrElse(1) { futureSteps[0].defaultMinutes * 60 }
        val minFutureDuration = futureSteps[0].minMinutes * 60

        // Add 2 extra minutes (120s deficit with 0 bonus time)
        viewModel.addExtraMinutes(2)

        val updatedSession = viewModel.activeSession.value
        val newFutureDuration = updatedSession?.stepDurationsSeconds?.getOrElse(1) { 0 } ?: 0

        // The future task duration should be auto-healed (compressed downwards, but never below minMinutes)
        assertTrue("Future task duration should be reduced or equal to min", newFutureDuration <= initialFutureDuration)
        assertTrue("Future task duration must never be below rush minMinutes", newFutureDuration >= minFutureDuration)
    }

    @Test
    fun `test out of order task jump preserves remaining duration of uncompleted tasks`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = RoutineViewModel(app)

        viewModel.startRoutine()
        val routine = viewModel.currentRoutine.value
        assertTrue("Should have multiple steps", routine.activeSteps.size >= 3)

        val totalRoutineSeconds = routine.activeSteps.sumOf { it.defaultMinutes * 60 }

        val initialSession = viewModel.activeSession.value!!
        val initialFinishDelta = viewModel.getCalculatedFinishTime(initialSession, routine)

        // Jump to step index 2 without marking steps 0 or 1 complete
        viewModel.jumpToStep(2)

        val afterJumpSession = viewModel.activeSession.value!!
        val afterJumpFinishDelta = viewModel.getCalculatedFinishTime(afterJumpSession, routine)

        // The remaining calculated finish time should still include steps 0 and 1!
        // It must NOT drop 20+ minutes just because we started doing tasks out of order.
        assertEquals(
            "Hour of finish should match initial when jumping out of order with 0 elapsed",
            initialFinishDelta.first,
            afterJumpFinishDelta.first
        )
        // Minute difference should be within 1 minute
        val minDiff = kotlin.math.abs(initialFinishDelta.second - afterJumpFinishDelta.second)
        assertTrue("Finish minute delta should be identical or within 1m ($minDiff)", minDiff <= 1)
    }

    @Test
    fun `test effective bonus time bounded by actual real time slack buffer`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = RoutineViewModel(app)

        val cal = java.util.Calendar.getInstance()
        val nowH = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val nowM = cal.get(java.util.Calendar.MINUTE)

        // Set target departure to exactly 15 minutes from now
        val targetCal = java.util.Calendar.getInstance().apply { add(java.util.Calendar.MINUTE, 15) }
        val targetEndStr = String.format(java.util.Locale.getDefault(), "%02d:%02d", targetCal.get(java.util.Calendar.HOUR_OF_DAY), targetCal.get(java.util.Calendar.MINUTE))

        // 2 tasks: 7m and 5m (total 12m)
        val step1 = RoutineStep(id = "s1", title = "Task 1", minMinutes = 4, defaultMinutes = 7)
        val step2 = RoutineStep(id = "s2", title = "Task 2", minMinutes = 3, defaultMinutes = 5)
        val routine = Routine(
            id = "test_routine_slack",
            title = "Test Slack",
            targetDepartureTime = targetEndStr,
            steps = listOf(step1, step2)
        )

        // Session says bonusTimeSeconds is 14 minutes (840s), but real slack is 15m - 12m = 3m (180s)
        val session = ActiveSession(
            routineId = routine.id,
            currentStepIndex = 0,
            stepRemainingSeconds = 7 * 60,
            stepTotalSeconds = 7 * 60,
            bonusTimeSeconds = 14 * 60, // 840s
            targetEndTime = targetEndStr,
            stepDurationsSeconds = listOf(7 * 60, 5 * 60),
            stepRemainingSecondsMap = mapOf("s1" to 7 * 60, "s2" to 5 * 60)
        )

        val effectiveBonus = viewModel.getEffectiveBonusTimeSeconds(session, routine)
        // Effective bonus must be bounded around real slack buffer (< 200s, never 840s!)
        assertTrue("Effective bonus should be bounded by real-time slack buffer (< 200s) but was $effectiveBonus", effectiveBonus in 100..200 && effectiveBonus < 840)
    }

    @Test
    fun `test routine completed today status check`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = RoutineViewModel(app)

        val cal = java.util.Calendar.getInstance()
        val todayKey = String.format(java.util.Locale.getDefault(), "%04d-%02d-%02d", cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1, cal.get(java.util.Calendar.DAY_OF_MONTH))

        val completedRoutine = Routine(
            id = "test_completed_routine",
            title = "Bedtime Routine",
            lastCompletedDate = todayKey
        )
        viewModel.updateRoutine(completedRoutine)

        assertTrue("Routine should be completed today", viewModel.isRoutineCompletedToday("test_completed_routine"))
        assertFalse("Routine with no date should not be completed today", viewModel.isRoutineCompletedToday("unknown_routine"))
    }

    @Test
    fun `test wall clock timer synchronization after background sleep`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = RoutineViewModel(app)

        val cal = java.util.Calendar.getInstance()
        val targetMinTotal = (cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE) + 20) % (24 * 60)
        val targetEndStr = String.format(java.util.Locale.getDefault(), "%02d:%02d", targetMinTotal / 60, targetMinTotal % 60)

        val step1 = RoutineStep(id = "s1", title = "Story Time", defaultMinutes = 10)
        val step2 = RoutineStep(id = "s2", title = "Bedtime", defaultMinutes = 5)
        val routine = Routine(id = "r_sleep", title = "Bedtime", targetDepartureTime = targetEndStr, steps = listOf(step1, step2))

        // Session was last updated 8 minutes (480s) ago when phone went to sleep
        val eightMinutesAgo = System.currentTimeMillis() - (480 * 1000L)
        val session = ActiveSession(
            routineId = routine.id,
            status = SessionStatus.RUNNING,
            currentStepIndex = 0,
            stepRemainingSeconds = 600, // 10 minutes
            stepTotalSeconds = 600,
            bonusTimeSeconds = 0,
            stepDurationsSeconds = listOf(600, 300),
            stepRemainingSecondsMap = mapOf("s1" to 600, "s2" to 300),
            targetEndTime = targetEndStr,
            updatedAt = eightMinutesAgo
        )

        // Dynamic remaining should immediately reconcile 8 minutes of sleep (600 - 480 = ~120s remaining)
        val dynamicRemaining = viewModel.getDynamicStepRemainingSeconds(session)
        assertTrue("Dynamic remaining should reflect 480s elapsed sleep (expected ~120s, got $dynamicRemaining)", dynamicRemaining in 118..122)

        // Estimated finish time should still be ~4 minutes early (now + 2m story + 5m bed = now + 7m, target is now + 20m)
        val (_, deltaMin) = viewModel.getCalculatedFinishTime(session, routine)
        assertTrue("Delta minutes should be early/negative (< 0) with cushion, got $deltaMin", deltaMin < 0)
    }

    @Test
    fun `test task interval recording on routine lifecycle`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = RoutineViewModel(app)

        val s1 = RoutineStep(id = "s1", title = "Task 1", minMinutes = 2, defaultMinutes = 5, maxMinutes = 8)
        val s2 = RoutineStep(id = "s2", title = "Task 2", minMinutes = 2, defaultMinutes = 4, maxMinutes = 6)
        val routine = Routine(id = "r_interval", title = "Interval Test", steps = listOf(s1, s2))
        viewModel.updateRoutine(routine)
        viewModel.selectRoutine(routine.id)

        viewModel.startRoutine(paceMode = "NOMINAL")
        val session1 = viewModel.activeSession.value
        assertTrue("Session should be active", session1 != null)
        assertEquals("Should have 1 open task interval", 1, session1!!.taskIntervals.size)
        assertEquals("s1", session1.taskIntervals[0].stepId)
        assertTrue("Exit epoch should be null while task is active", session1.taskIntervals[0].exitEpochMillis == null)

        // Complete step 1 and advance to step 2
        viewModel.completeCurrentStepEarly()
        val session2 = viewModel.activeSession.value
        assertTrue("Session should still be active", session2 != null)
        assertEquals("Should have 2 task intervals", 2, session2!!.taskIntervals.size)
        assertTrue("First interval should now be closed", session2.taskIntervals[0].exitEpochMillis != null)
        assertEquals("s2", session2.taskIntervals[1].stepId)
        assertTrue("Second interval should be open", session2.taskIntervals[1].exitEpochMillis == null)

        // Jump back to step 1
        viewModel.jumpToStep(0)
        val session3 = viewModel.activeSession.value
        assertEquals("Should have 3 task intervals after jump", 3, session3!!.taskIntervals.size)
        assertTrue("Second interval should now be closed", session3.taskIntervals[1].exitEpochMillis != null)
        assertEquals("s1", session3.taskIntervals[2].stepId)
        assertTrue("Third interval (re-entered s1) should be open", session3.taskIntervals[2].exitEpochMillis == null)
    }

    @Test
    fun `test pace mode scaling freezes completed tasks and scales active and upcoming`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = RoutineViewModel(app)

        val s1 = RoutineStep(id = "s1", title = "Task 1", minMinutes = 3, defaultMinutes = 5, maxMinutes = 8)
        val s2 = RoutineStep(id = "s2", title = "Task 2", minMinutes = 2, defaultMinutes = 4, maxMinutes = 6)
        val s3 = RoutineStep(id = "s3", title = "Task 3", minMinutes = 4, defaultMinutes = 6, maxMinutes = 10)
        val routine = Routine(id = "r_pace_freeze", title = "Pace Freeze Test", steps = listOf(s1, s2, s3))
        viewModel.updateRoutine(routine)
        viewModel.selectRoutine(routine.id)

        // 1. Start routine in Relaxed mode: s1=8m (480s), s2=6m (360s), s3=10m (600s)
        viewModel.startRoutine(paceMode = "RELAXED")
        val session1 = viewModel.activeSession.value!!
        assertEquals("Relaxed s1 allocated should be 480s (8m)", 480, session1.stepDurationsSeconds[0])

        // 2. Complete step 1 -> banks bonus and moves to step 2 (s2 duration was 6m = 360s)
        viewModel.completeCurrentStepEarly()
        val session2 = viewModel.activeSession.value!!
        assertTrue("s1 should be in completedStepIds", session2.completedStepIds.contains("s1"))
        assertEquals("s1 duration should remain 480s", 480, session2.stepDurationsSeconds[0])
        assertEquals("s2 active duration should be 360s", 360, session2.stepDurationsSeconds[1])

        // 3. User switches pace mode to RUSH!
        viewModel.setPaceMode("RUSH")
        val sessionRush = viewModel.activeSession.value!!

        // CRITICAL CHECK: Completed task s1 MUST REMAIN 480s (8m) - FROZEN!
        assertEquals("Completed task s1 must remain 480s (frozen) in RUSH", 480, sessionRush.stepDurationsSeconds[0])

        // APPROACH 3: Currently running task s2 remains undisturbed (360s total, remaining untouched)
        assertEquals("Currently running task s2 must remain undisturbed (360s)", 360, sessionRush.stepDurationsSeconds[1])
        assertEquals("Currently running task s2 total seconds must remain 360s", 360, sessionRush.stepTotalSeconds)

        // Upcoming task s3 must scale to minMinutes (rush = 4m = 240s)
        assertEquals("Upcoming task s3 must scale to rush (4m = 240s)", 240, sessionRush.stepDurationsSeconds[2])

        // 4. User completes s2, entering s3: s3 must now run with its 240s rush duration!
        viewModel.completeCurrentStepEarly()
        val session3 = viewModel.activeSession.value!!
        assertEquals("s3 is now current step", 2, session3.currentStepIndex)
        assertEquals("s3 started with its rush duration of 240s", 240, session3.stepTotalSeconds)
        assertEquals(240, session3.stepRemainingSeconds)

        // 5. While s3 is running, user switches pace mode back to RELAXED!
        viewModel.setPaceMode("RELAXED")
        val sessionRelaxed = viewModel.activeSession.value!!

        // Completed tasks s1 and s2 remain frozen
        assertEquals(480, sessionRelaxed.stepDurationsSeconds[0])
        assertEquals(360, sessionRelaxed.stepDurationsSeconds[1])
        // Running task s3 remains undisturbed
        assertEquals(240, sessionRelaxed.stepDurationsSeconds[2])
        assertEquals(240, sessionRelaxed.stepTotalSeconds)
    }
}


