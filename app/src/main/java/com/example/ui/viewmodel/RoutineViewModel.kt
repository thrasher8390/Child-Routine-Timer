package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundEffects
import com.example.auth.AuthManager
import com.example.data.RoutineRepository
import com.example.model.ActiveSession
import com.example.model.CatalogTask
import com.example.model.PaceCategory
import com.example.model.PaceCoachState
import com.example.model.RecommendationType
import com.example.model.Routine
import com.example.model.RoutineStep
import com.example.model.SessionEvent
import com.example.model.SessionStatus
import com.example.model.StarPrize
import com.example.model.StepRunHistory
import com.example.model.TaskInterval
import com.example.model.TimelineSizingMode
import com.example.model.TimerRecommendation
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.example.util.RoutineNotificationHelper
import com.example.util.AppLogger
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

sealed interface Screen {
    data object Dashboard : Screen
    data object RoutineView : Screen
    data object RunMode : Screen
    data object EditRoutine : Screen
    data object TaskList : Screen
    data object CreateRoutine : Screen
    data object Auth : Screen
    data object TaskCatalog : Screen
}

data class ElasticScheduleResult(
    val durationsSeconds: List<Int>,
    val durationsMinutes: List<Int>,
    val isExpressMode: Boolean,
    val isRelaxedMode: Boolean,
    val surplusMinutes: Int
)

data class RoutineCompletionSummary(
    val routineTitle: String,
    val starsEarned: Int,
    val targetDepartureTime: String,
    val minutesBeforeDeparture: Int,
    val isAheadOfDeparture: Boolean,
    val totalElapsedMinutes: Int,
    val totalTasksCompleted: Int,
    val soloTasksCount: Int,
    val bonusMinutesBanked: Int
)

private data class ScheduleQuad(
    val durationsSec: List<Int>,
    val isExpress: Boolean,
    val isRelaxed: Boolean,
    val surplus: Int
)

class RoutineViewModel(application: Application) : AndroidViewModel(application) {
    val authManager = AuthManager(application)
    val soundEffects = SoundEffects()
    val notificationHelper = RoutineNotificationHelper(application)
    private val repository = RoutineRepository(application, viewModelScope)

    val allRoutines: StateFlow<List<Routine>> = repository.allRoutines
    val currentRoutine: StateFlow<Routine> = repository.currentRoutine
    val activeSession: StateFlow<ActiveSession?> = repository.activeSession
    val familyCode: StateFlow<String> = repository.familyCode
    val earnedStars: StateFlow<Int> = repository.earnedStars
    val starPrizes: StateFlow<List<StarPrize>> = repository.starPrizes
    val runHistory: StateFlow<List<StepRunHistory>> = repository.stepRunHistory
    val taskCatalog: StateFlow<List<CatalogTask>> = repository.taskCatalog

    fun addCatalogTask(task: CatalogTask) = repository.saveCatalogTask(task)
    fun deleteCatalogTask(taskId: String) = repository.deleteCatalogTask(taskId)
    fun restoreCatalogTasks() = repository.recoverCatalogFromRoutines(allRoutines.value)

    // Kid-Proof Child Lock: Only "Done / Next" button can be touched on the screen
    private val _isChildLockActive = MutableStateFlow(false)
    val isChildLockActive: StateFlow<Boolean> = _isChildLockActive.asStateFlow()

    fun toggleChildLock() {
        _isChildLockActive.value = !_isChildLockActive.value
    }

    // Vertical Timeline Bar Sizing Mode (Balanced, Equal, Proportional, Focus)
    private val _timelineSizingMode = MutableStateFlow(TimelineSizingMode.PROPORTIONAL)
    val timelineSizingMode: StateFlow<TimelineSizingMode> = _timelineSizingMode.asStateFlow()

    fun setTimelineSizingMode(mode: TimelineSizingMode) {
        _timelineSizingMode.value = mode
    }

    fun cycleTimelineSizingMode() {
        val modes = TimelineSizingMode.entries
        val nextIndex = (modes.indexOf(_timelineSizingMode.value) + 1) % modes.size
        _timelineSizingMode.value = modes[nextIndex]
    }

    fun redeemPrize(prizeId: String) {
        soundEffects.playCelebration(viewModelScope)
        repository.redeemPrize(prizeId)
    }

    fun addCustomPrize(title: String, stars: Int, type: String, emoji: String) {
        repository.addCustomPrize(title, stars, type, emoji)
    }

    private val _preLaunchTargetTime = MutableStateFlow<String?>(null)
    val preLaunchTargetTime: StateFlow<String?> = _preLaunchTargetTime.asStateFlow()

    fun setPreLaunchTargetTime(time: String?) {
        _preLaunchTargetTime.value = time
    }

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Dashboard)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _completionSummary = MutableStateFlow<RoutineCompletionSummary?>(null)
    val completionSummary: StateFlow<RoutineCompletionSummary?> = _completionSummary.asStateFlow()

    // Real-time ticking wall clock (hour, minute, second)
    private val _currentWallTime = MutableStateFlow(Calendar.getInstance())
    val currentWallTime: StateFlow<Calendar> = _currentWallTime.asStateFlow()

    private var timerJob: Job? = null
    private var lastNotifiedStartTimeRoutineKey: String? = null
    private var previousObservedSessionStatus: SessionStatus? = null

    init {
        // Start live 1-second wall-clock ticker & start-time arrival detector
        viewModelScope.launch {
            while (isActive) {
                val cal = Calendar.getInstance()
                _currentWallTime.value = cal

                // Check if target start time has arrived for the current routine
                val curHour = cal.get(Calendar.HOUR_OF_DAY)
                val curMin = cal.get(Calendar.MINUTE)
                val curTimeStr = String.format(Locale.getDefault(), "%02d:%02d", curHour, curMin)
                val todayKey = String.format(Locale.getDefault(), "%04d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))

                val r = currentRoutine.value
                val sess = activeSession.value
                if (sess == null || sess.status == SessionStatus.COMPLETED) {
                    if (r.startTime == curTimeStr) {
                        val alertKey = "${todayKey}_${r.id}_$curTimeStr"
                        if (lastNotifiedStartTimeRoutineKey != alertKey) {
                            lastNotifiedStartTimeRoutineKey = alertKey
                            notificationHelper.notifyStartTimeArrived(
                                routineTitle = r.title,
                                startTimeFormatted = formatTimeAmPm(r.startTime)
                            )
                        }
                    }
                }
                delay(1000)
            }
        }

        // Listen to active session changes from repository to keep multi-device timer loop synchronized and alert family
        viewModelScope.launch {
            repository.activeSession.collect { session ->
                if (session != null && session.status == SessionStatus.RUNNING) {
                    if (previousObservedSessionStatus != SessionStatus.RUNNING && previousObservedSessionStatus != SessionStatus.PAUSED) {
                        val title = session.routineTitle.ifEmpty { currentRoutine.value.title }
                        notificationHelper.notifyFamilyRoutineStarted(
                            routineTitle = title,
                            familyCode = familyCode.value,
                            totalTasks = session.stepDurationsSeconds.size.coerceAtLeast(currentRoutine.value.activeSteps.size)
                        )
                    }
                    if (timerJob == null) {
                        startRunLoop()
                    }
                } else if (session == null || session.status != SessionStatus.RUNNING) {
                    timerJob?.cancel()
                    timerJob = null
                }
                previousObservedSessionStatus = session?.status
            }
        }
    }

    private var previousScreenBeforeEdit: Screen = Screen.Dashboard

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun openRoutineView(routineId: String) {
        _completionSummary.value = null
        selectRoutine(routineId)
        _currentScreen.value = Screen.RoutineView
    }

    fun openEditRoutineFromRoutineView() {
        previousScreenBeforeEdit = Screen.RoutineView
        _currentScreen.value = Screen.EditRoutine
    }

    fun openTaskListFromRoutineView() {
        previousScreenBeforeEdit = Screen.RoutineView
        _currentScreen.value = Screen.TaskList
    }

    fun openTaskList() {
        previousScreenBeforeEdit = Screen.Dashboard
        _currentScreen.value = Screen.TaskList
    }

    fun navigateBack() {
        when (_currentScreen.value) {
            Screen.RoutineView -> _currentScreen.value = Screen.Dashboard
            Screen.EditRoutine -> {
                _currentScreen.value = previousScreenBeforeEdit
                previousScreenBeforeEdit = Screen.Dashboard
            }
            Screen.TaskList -> {
                _currentScreen.value = previousScreenBeforeEdit
                previousScreenBeforeEdit = Screen.Dashboard
            }
            Screen.RunMode -> _currentScreen.value = Screen.Dashboard
            Screen.CreateRoutine -> _currentScreen.value = Screen.Dashboard
            Screen.Auth -> _currentScreen.value = Screen.Dashboard
            Screen.TaskCatalog -> _currentScreen.value = Screen.Dashboard
            Screen.Dashboard -> Unit
        }
    }

    fun isRoutineCompletedToday(routineId: String): Boolean {
        val routine = allRoutines.value.find { it.id == routineId }
            ?: if (currentRoutine.value.id == routineId) currentRoutine.value else null
            ?: return false

        val cal = Calendar.getInstance()
        val todayKey = String.format(Locale.getDefault(), "%04d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
        if (routine.lastCompletedDate == todayKey) return true

        val summary = _completionSummary.value
        if (summary != null && summary.routineTitle == routine.title) return true

        val session = activeSession.value
        if (session != null && session.routineId == routineId && session.status == SessionStatus.COMPLETED) {
            val sessionDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(java.util.Date(session.updatedAt))
            if (sessionDate == todayKey) return true
        }
        return false
    }

    fun calculateTaskTargetEndTimes(
        routine: Routine,
        startTimeStr: String = routine.startTime
    ): List<Pair<RoutineStep, String>> {
        val steps = routine.activeSteps
        if (steps.isEmpty()) return emptyList()

        val parts = startTimeStr.split(":")
        val startH = parts.getOrNull(0)?.toIntOrNull() ?: 7
        val startM = parts.getOrNull(1)?.toIntOrNull() ?: 0
        var cumulativeMinutes = startH * 60 + startM

        return steps.map { step ->
            cumulativeMinutes += step.defaultMinutes
            val endH = (cumulativeMinutes / 60) % 24
            val endM = cumulativeMinutes % 60
            val endStr = String.format(Locale.getDefault(), "%02d:%02d", endH, endM)
            Pair(step, formatTimeAmPm(endStr))
        }
    }

    val localDeviceId: String get() = repository.deviceId

    /**
     * Auto-heals the routine schedule when behind time or banked time is depleted:
     * Takes time away from uncompleted, unskipped future tasks down towards their rushed time (minMinutes),
     * but NEVER reduces any task below its minMinutes.
     */
    fun autoHealFutureTasks(
        session: ActiveSession,
        routine: Routine,
        deficitSeconds: Int
    ): Pair<List<Int>, Int> {
        val steps = routine.activeSteps
        val durations = session.stepDurationsSeconds.toMutableList()
        var remainingDeficit = deficitSeconds

        val candidateIndices = ((session.currentStepIndex + 1 until steps.size) + (0 until session.currentStepIndex))
            .filter { idx ->
                val stepId = steps[idx].id
                !session.completedStepIds.contains(stepId) && !session.skippedStepIds.contains(stepId)
            }

        for (i in candidateIndices) {
            if (remainingDeficit <= 0) break
            val step = steps[i]
            val currentAllocatedSec = durations.getOrElse(i) { step.defaultMinutes * 60 }
            val minSec = step.minMinutes * 60
            val reducibleSec = (currentAllocatedSec - minSec).coerceAtLeast(0)
            if (reducibleSec > 0) {
                val trimSec = minOf(reducibleSec, remainingDeficit)
                durations[i] = currentAllocatedSec - trimSec
                remainingDeficit -= trimSec
            }
        }
        AppLogger.i("RoutineViewModel", "autoHealFutureTasks: initialDeficit=${deficitSeconds}s -> remainingDeficit=${remainingDeficit}s")
        return Pair(durations, remainingDeficit)
    }

    /**
     * Calculates the true real-time remaining seconds for the active task,
     * reconciling any background sleep or elapsed wall-clock time since session was last updated.
     */
    fun getDynamicStepRemainingSeconds(session: ActiveSession): Int {
        if (session.status != SessionStatus.RUNNING) return session.stepRemainingSeconds
        val nowMs = System.currentTimeMillis()
        val elapsedSec = ((nowMs - session.updatedAt).coerceAtLeast(0) / 1000).toInt()
        if (elapsedSec <= 0) return session.stepRemainingSeconds
        return session.stepRemainingSeconds - elapsedSec
    }

    /**
     * Calculates the true real-time slack buffer between target departure time
     * and total required remaining duration of all uncompleted, unskipped tasks.
     * When negative, routine will finish late; when positive, represents real available bonus cushion.
     */
    fun getEffectiveBonusTimeSeconds(session: ActiveSession, routine: Routine): Int {
        val activeSteps = routine.activeSteps
        val currentStep = activeSteps.getOrNull(session.currentStepIndex)
        val currentStepId = currentStep?.id
        val dynamicCurrentStepRemaining = getDynamicStepRemainingSeconds(session)

        var totalRequiredRemainingSec = 0
        activeSteps.forEachIndexed { idx, step ->
            if (step.id == currentStepId) {
                totalRequiredRemainingSec += dynamicCurrentStepRemaining.coerceAtLeast(0)
            } else if (!session.completedStepIds.contains(step.id) && !session.skippedStepIds.contains(step.id)) {
                val rem = session.stepRemainingSecondsMap[step.id]?.takeIf { it > 0 }
                    ?: session.stepDurationsSeconds.getOrElse(idx) { step.defaultMinutes * 60 }
                totalRequiredRemainingSec += rem
            }
        }

        val cal = Calendar.getInstance()
        val nowSec = cal.get(Calendar.HOUR_OF_DAY) * 3600 + cal.get(Calendar.MINUTE) * 60 + cal.get(Calendar.SECOND)
        val targetRaw = session.targetEndTime.ifEmpty { routine.targetDepartureTime }
        val parts = targetRaw.split(":")
        val targetH = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val targetM = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val targetSec = (targetH * 60 + targetM) * 60

        var secondsUntilTarget = targetSec - nowSec
        if (secondsUntilTarget > 12 * 3600) secondsUntilTarget -= 24 * 3600
        if (secondsUntilTarget < -12 * 3600) secondsUntilTarget += 24 * 3600

        val realSlackBufferSec = secondsUntilTarget - totalRequiredRemainingSec

        return if (realSlackBufferSec < 0) {
            realSlackBufferSec
        } else if (session.bonusTimeSeconds > 0) {
            minOf(session.bonusTimeSeconds, realSlackBufferSec)
        } else {
            realSlackBufferSec
        }
    }

    fun dismissCompletionSummary() {
        _completionSummary.value = null
        timerJob?.cancel()
        timerJob = null
        repository.clearActiveSession()
    }

    fun finishRoutineAndShowSummary() {
        val session = activeSession.value ?: return
        timerJob?.cancel()
        timerJob = null
        val routine = currentRoutine.value
        val cal = Calendar.getInstance()
        val todayKey = String.format(Locale.getDefault(), "%04d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
        repository.updateLocalRoutine(routine.copy(lastCompletedDate = todayKey))
        _completionSummary.value = buildCompletionSummary(session, routine)
        _currentScreen.value = Screen.Dashboard
    }

    private fun buildCompletionSummary(session: ActiveSession, routine: Routine): RoutineCompletionSummary {
        val steps = routine.activeSteps
        val earnedInSteps = steps.filter { session.completedStepIds.contains(it.id) && !session.assistedStepIds.contains(it.id) }
            .sumOf { if (it.starReward > 0) it.starReward else 1 }
        val totalStars = earnedInSteps + 1 // +1 bonus star for completing the routine

        val elapsedSec = session.totalElapsedSeconds.coerceAtLeast(1)
        val elapsedMin = (elapsedSec + 59) / 60
        val soloCount = session.completedStepIds.count { !session.assistedStepIds.contains(it) }

        val now = Calendar.getInstance()
        val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val effectiveTarget = session.targetEndTime.ifEmpty { routine.targetDepartureTime }
        val parts = effectiveTarget.split(":")
        val targetMinutes = (parts.getOrNull(0)?.toIntOrNull() ?: 8) * 60 + (parts.getOrNull(1)?.toIntOrNull() ?: 0)

        var diffMinutes = targetMinutes - nowMinutes
        if (diffMinutes < -12 * 60) diffMinutes += 24 * 60
        val isAhead = diffMinutes >= 0
        val minutesBefore = if (isAhead) diffMinutes else -diffMinutes

        return RoutineCompletionSummary(
            routineTitle = routine.title,
            starsEarned = totalStars,
            targetDepartureTime = formatTimeAmPm(effectiveTarget),
            minutesBeforeDeparture = minutesBefore,
            isAheadOfDeparture = isAhead,
            totalElapsedMinutes = elapsedMin,
            totalTasksCompleted = session.completedStepIds.size,
            soloTasksCount = soloCount,
            bonusMinutesBanked = session.bonusTimeSeconds / 60
        )
    }

    fun selectRoutine(id: String) {
        _completionSummary.value = null
        repository.selectRoutine(id)
    }

    fun createNewRoutine(
        title: String,
        startTime: String,
        targetDepartureTime: String,
        repeatType: String = "Everyday"
    ) {
        _completionSummary.value = null
        val created = repository.createRoutine(title, startTime, targetDepartureTime, repeatType)
        _currentScreen.value = Screen.TaskList
    }

    fun removeRoutine(id: String) {
        repository.deleteRoutine(id)
        _currentScreen.value = Screen.Dashboard
    }

    fun joinFamily(code: String) {
        repository.joinFamilyCode(code)
    }

    fun refreshSync() {
        repository.forceSync()
    }

    /**
     * Elastic backward-scheduling engine.
     */
    /**
     * Elastic backward-scheduling engine.
     * Takes available minutes from current time until target departure/end time,
     * and scales tasks elastically between rush (min), nominal (default), and relaxed (max).
     */
    fun calculateElasticSchedule(
        routine: Routine,
        targetEndTimeStr: String = routine.targetDepartureTime,
        paceMode: String = "ELASTIC"
    ): ElasticScheduleResult {
        val steps = routine.activeSteps
        if (steps.isEmpty()) {
            return ElasticScheduleResult(
                durationsSeconds = emptyList(),
                durationsMinutes = emptyList(),
                isExpressMode = false,
                isRelaxedMode = false,
                surplusMinutes = 0
            )
        }

        val sumMinSec = steps.sumOf { it.minMinutes * 60 }
        val sumNominalSec = steps.sumOf { it.defaultMinutes * 60 }
        val sumMaxSec = steps.sumOf { it.maxMinutes * 60 }

        if (paceMode == "RUSH") {
            val secs = steps.map { it.minMinutes * 60 }
            return ElasticScheduleResult(
                durationsSeconds = secs,
                durationsMinutes = steps.map { it.minMinutes },
                isExpressMode = true,
                isRelaxedMode = false,
                surplusMinutes = 0
            )
        } else if (paceMode == "NOMINAL") {
            val secs = steps.map { it.defaultMinutes * 60 }
            return ElasticScheduleResult(
                durationsSeconds = secs,
                durationsMinutes = steps.map { it.defaultMinutes },
                isExpressMode = false,
                isRelaxedMode = false,
                surplusMinutes = 0
            )
        } else if (paceMode == "RELAXED") {
            val secs = steps.map { it.maxMinutes * 60 }
            return ElasticScheduleResult(
                durationsSeconds = secs,
                durationsMinutes = steps.map { it.maxMinutes },
                isExpressMode = false,
                isRelaxedMode = true,
                surplusMinutes = 0
            )
        }

        // Parse target departure time HH:mm
        val cal = Calendar.getInstance()
        val nowMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        val parts = targetEndTimeStr.split(":")
        val targetHour = parts.getOrNull(0)?.toIntOrNull() ?: 20
        val targetMin = parts.getOrNull(1)?.toIntOrNull() ?: 30
        val targetDepartureMinutes = targetHour * 60 + targetMin

        var diff = targetDepartureMinutes - nowMinutes
        val availableMinutes: Int
        if (diff in 10..300) {
            availableMinutes = diff
        } else {
            // Started outside the window or after target departure:
            val window = routine.getWindowMinutes().coerceIn(15, 180)
            availableMinutes = window
        }

        val availableSec = availableMinutes * 60

        val (durationsSec, isExpress, isRelaxed, surplus) = when {
            availableSec <= sumMinSec -> {
                // Strict Express / Rush Mode
                val secs = steps.map { it.minMinutes * 60 }
                ScheduleQuad(secs, true, false, 0)
            }
            availableSec >= sumMaxSec -> {
                // Relaxed Mode with surplus cushion
                val secs = steps.map { it.maxMinutes * 60 }
                val surplusMin = (availableSec - sumMaxSec) / 60
                ScheduleQuad(secs, false, true, surplusMin)
            }
            availableSec <= sumNominalSec -> {
                // Scaling between Min (Rush) and Nominal (Default) to fit availableSec exactly
                val range = (sumNominalSec - sumMinSec).coerceAtLeast(1)
                val factor = (availableSec - sumMinSec).toDouble() / range
                val secs = steps.map { step ->
                    val minSec = step.minMinutes * 60
                    val nomSec = step.defaultMinutes * 60
                    (minSec + ((nomSec - minSec) * factor)).toInt().coerceAtLeast(30)
                }
                ScheduleQuad(secs, true, false, 0)
            }
            else -> {
                // Scaling between Nominal (Default) and Max (Relaxed) to fit availableSec exactly
                val range = (sumMaxSec - sumNominalSec).coerceAtLeast(1)
                val factor = (availableSec - sumNominalSec).toDouble() / range
                val secs = steps.map { step ->
                    val nomSec = step.defaultMinutes * 60
                    val maxSec = step.maxMinutes * 60
                    (nomSec + ((maxSec - nomSec) * factor)).toInt().coerceAtLeast(30)
                }
                ScheduleQuad(secs, false, true, 0)
            }
        }

        val durationsMin = durationsSec.map { (it + 59) / 60 }

        return ElasticScheduleResult(
            durationsSeconds = durationsSec,
            durationsMinutes = durationsMin,
            isExpressMode = isExpress,
            isRelaxedMode = isRelaxed,
            surplusMinutes = surplus
        )
    }

    /**
     * Toggles whether a routine step is active/included or skipped for today's run.
     */
    fun toggleStepEnabled(stepId: String) {
        val routine = currentRoutine.value
        val updatedSteps = routine.steps.map {
            if (it.id == stepId) it.copy(isEnabled = !it.isEnabled) else it
        }
        val updatedRoutine = routine.copy(steps = updatedSteps)
        updateRoutine(updatedRoutine)

        val session = activeSession.value ?: return
        val newActiveSteps = updatedRoutine.activeSteps
        if (newActiveSteps.isEmpty()) return

        val schedule = calculateElasticSchedule(
            updatedRoutine,
            targetEndTimeStr = session.targetEndTime.ifEmpty { updatedRoutine.targetDepartureTime },
            paceMode = session.paceMode
        )
        val durationsSec = schedule.durationsMinutes.map { it * 60 }
        val safeIndex = session.currentStepIndex.coerceIn(0, newActiveSteps.size - 1)
        val stepDurationSec = durationsSec.getOrElse(safeIndex) { newActiveSteps[safeIndex].defaultMinutes * 60 }
        val currentRemaining = session.stepRemainingSeconds.coerceAtMost(stepDurationSec)

        val updatedSession = session.copy(
            stepDurationsSeconds = durationsSec,
            currentStepIndex = safeIndex,
            stepTotalSeconds = stepDurationSec,
            stepRemainingSeconds = currentRemaining,
            updatedAt = System.currentTimeMillis()
        )
        repository.updateActiveSession(updatedSession, syncToCloud = true)
    }

    /**
     * Intelligent pace coaching engine.
     * Evaluates time remaining until target departure vs min, nominal, and relaxed task sums.
     * Tells user if pace is Relaxed, Normal Speed, Rush, or Over Budget (impossible without trimming).
     * If over budget, suggests non-important tasks to toggle off to leave on time.
     */
    fun getPaceCoachState(
        routine: Routine,
        overrideTargetTime: String? = null
    ): PaceCoachState {
        val targetTimeStr = overrideTargetTime ?: _preLaunchTargetTime.value ?: routine.targetDepartureTime
        val steps = routine.activeSteps

        val parts = targetTimeStr.split(":")
        val targetH = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val targetM = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val targetMinutes = targetH * 60 + targetM

        val cal = Calendar.getInstance()
        val nowMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        var availableMinutes = targetMinutes - nowMinutes
        if (availableMinutes < -120) {
            // Target is likely tomorrow morning or crossed midnight
            availableMinutes += 24 * 60
        }

        if (steps.isEmpty()) {
            return PaceCoachState(
                targetEndTime = targetTimeStr,
                availableMinutes = availableMinutes.coerceAtLeast(0),
                paceCategory = PaceCategory.NOMINAL,
                calculatedFinishTime = targetTimeStr,
                deltaMinutes = 0,
                allocatedStepDurationsMinutes = emptyList(),
                suggestedTasksToSkip = emptyList(),
                coachingMessage = "No active tasks in routine."
            )
        }

        val sumMin = steps.sumOf { it.minMinutes }
        val sumNominal = steps.sumOf { it.defaultMinutes }
        val sumRelaxed = steps.sumOf { it.maxMinutes }

        val schedule = calculateElasticSchedule(routine, targetTimeStr, paceMode = "ELASTIC")
        val allocatedMinutes = schedule.durationsMinutes

        return when {
            availableMinutes < sumMin -> {
                val shortMins = sumMin - availableMinutes
                val finishMin = (nowMinutes + sumMin) % (24 * 60)
                val finishTimeStr = String.format(Locale.getDefault(), "%02d:%02d", finishMin / 60, finishMin % 60)
                // Proactively identify candidate tasks to trim/skip (non-important tasks sorted by size)
                val candidates = steps.filter { !it.isImportant }.sortedByDescending { it.defaultMinutes }
                PaceCoachState(
                    targetEndTime = targetTimeStr,
                    availableMinutes = availableMinutes,
                    paceCategory = PaceCategory.OVER_BUDGET,
                    calculatedFinishTime = finishTimeStr,
                    deltaMinutes = shortMins,
                    allocatedStepDurationsMinutes = steps.map { it.minMinutes },
                    suggestedTasksToSkip = candidates,
                    coachingMessage = "Over budget by ${shortMins}m! Finishes at ${formatTimeAmPm(finishTimeStr)} even at maximum rush pace."
                )
            }
            availableMinutes <= sumNominal -> {
                val compressedMins = sumNominal - availableMinutes
                val finishMin = (nowMinutes + availableMinutes) % (24 * 60)
                val finishTimeStr = String.format(Locale.getDefault(), "%02d:%02d", finishMin / 60, finishMin % 60)
                PaceCoachState(
                    targetEndTime = targetTimeStr,
                    availableMinutes = availableMinutes,
                    paceCategory = PaceCategory.RUSH,
                    calculatedFinishTime = finishTimeStr,
                    deltaMinutes = -compressedMins,
                    allocatedStepDurationsMinutes = allocatedMinutes,
                    suggestedTasksToSkip = emptyList(),
                    coachingMessage = "Rush Pace (-${compressedMins}m): Tasks compressed to leave on time at ${formatTimeAmPm(targetTimeStr)}."
                )
            }
            availableMinutes <= sumRelaxed -> {
                val finishMin = (nowMinutes + availableMinutes) % (24 * 60)
                val finishTimeStr = String.format(Locale.getDefault(), "%02d:%02d", finishMin / 60, finishMin % 60)
                PaceCoachState(
                    targetEndTime = targetTimeStr,
                    availableMinutes = availableMinutes,
                    paceCategory = PaceCategory.NOMINAL,
                    calculatedFinishTime = finishTimeStr,
                    deltaMinutes = 0,
                    allocatedStepDurationsMinutes = allocatedMinutes,
                    suggestedTasksToSkip = emptyList(),
                    coachingMessage = "Normal Speed: On track with comfortable focus to finish at ${formatTimeAmPm(targetTimeStr)}."
                )
            }
            else -> {
                val cushion = availableMinutes - sumRelaxed
                val finishMin = (nowMinutes + sumRelaxed) % (24 * 60)
                val finishTimeStr = String.format(Locale.getDefault(), "%02d:%02d", finishMin / 60, finishMin % 60)
                PaceCoachState(
                    targetEndTime = targetTimeStr,
                    availableMinutes = availableMinutes,
                    paceCategory = PaceCategory.RELAXED,
                    calculatedFinishTime = finishTimeStr,
                    deltaMinutes = -cushion,
                    allocatedStepDurationsMinutes = steps.map { it.maxMinutes },
                    suggestedTasksToSkip = emptyList(),
                    coachingMessage = "Relaxed Pace (+${cushion}m cushion): Plenty of time before departure."
                )
            }
        }
    }

    /**
     * Calculates the estimated finish wall time for an active running session.
     * Returns Pair(formattedTimeString, deltaMinutesFromTarget).
     * deltaMinutes > 0 means running late; < 0 means finishing early with cushion; 0 means on time.
     */
    fun getCalculatedFinishTime(session: ActiveSession, routine: Routine): Pair<String, Int> {
        val activeSteps = routine.activeSteps
        val currentStep = activeSteps.getOrNull(session.currentStepIndex)
        val currentStepId = currentStep?.id
        val dynamicCurrentStepRemaining = getDynamicStepRemainingSeconds(session)

        // Accurately sum remaining seconds across ALL uncompleted active steps,
        // preserving any out-of-order tasks the child has yet to do!
        var totalRemainingSec = 0
        activeSteps.forEachIndexed { idx, step ->
            if (step.id == currentStepId) {
                totalRemainingSec += dynamicCurrentStepRemaining.coerceAtLeast(0)
            } else if (!session.completedStepIds.contains(step.id) && !session.skippedStepIds.contains(step.id)) {
                val rem = session.stepRemainingSecondsMap[step.id]?.takeIf { it > 0 }
                    ?: session.stepDurationsSeconds.getOrElse(idx) { step.defaultMinutes * 60 }
                totalRemainingSec += rem
            }
        }

        // Use current wall time with seconds to avoid 30-second rounding jitter
        val cal = Calendar.getInstance()
        val nowSec = cal.get(Calendar.HOUR_OF_DAY) * 3600 + cal.get(Calendar.MINUTE) * 60 + cal.get(Calendar.SECOND)
        val calcFinishTotalSec = (nowSec + totalRemainingSec) % (24 * 3600)
        val calcFinishTotalMin = ((calcFinishTotalSec + 30) / 60) % (24 * 60)
        val calcFinishTimeStr = String.format(Locale.getDefault(), "%02d:%02d", calcFinishTotalMin / 60, calcFinishTotalMin % 60)

        val targetRaw = session.targetEndTime.ifEmpty { routine.targetDepartureTime }
        val targetParts = targetRaw.split(":")
        val targetH = targetParts.getOrNull(0)?.toIntOrNull() ?: 8
        val targetM = targetParts.getOrNull(1)?.toIntOrNull() ?: 0
        val targetTotalSec = (targetH * 60 + targetM) * 60

        // Delta in seconds, rounded smoothly to nearest whole minute (eliminates alternating every 30s)
        var deltaSec = (nowSec + totalRemainingSec) - targetTotalSec
        if (deltaSec > 12 * 3600) deltaSec -= 24 * 3600
        if (deltaSec < -12 * 3600) deltaSec += 24 * 3600

        val deltaMin = if (deltaSec >= 0) {
            (deltaSec + 30) / 60
        } else {
            (deltaSec - 30) / 60
        }

        return Pair(calcFinishTimeStr, deltaMin)
    }

    /**
     * Start or resume an active routine run session taking actual start time into account.
     */
    fun startRoutine(
        startIndex: Int = 0,
        paceMode: String = "ELASTIC",
        customTargetEndTime: String? = null
    ) {
        val routine = currentRoutine.value
        val cal = Calendar.getInstance()
        val nowH = cal.get(Calendar.HOUR_OF_DAY)
        val nowM = cal.get(Calendar.MINUTE)
        val nowTotalMin = nowH * 60 + nowM
        val currentWallTimeStr = String.format(Locale.getDefault(), "%02d:%02d", nowH, nowM)

        val targetEndTimeStr = customTargetEndTime ?: _preLaunchTargetTime.value ?: routine.targetDepartureTime
        _preLaunchTargetTime.value = null // reset for next pre-launch
        val parts = targetEndTimeStr.split(":")
        val targetHour = parts.getOrNull(0)?.toIntOrNull() ?: 20
        val targetMin = parts.getOrNull(1)?.toIntOrNull() ?: 30
        val targetTotalMin = targetHour * 60 + targetMin

        // Determine effective target end time based on actual start time
        var diff = targetTotalMin - nowTotalMin
        if (diff < 0) diff += 24 * 60
        val effectiveTargetEndTime = if (diff in 5..360) {
            targetEndTimeStr
        } else {
            val window = routine.getWindowMinutes().coerceIn(15, 180)
            val endTotal = (nowTotalMin + window) % (24 * 60)
            String.format(Locale.getDefault(), "%02d:%02d", endTotal / 60, endTotal % 60)
        }

        val schedule = calculateElasticSchedule(routine, targetEndTimeStr = effectiveTargetEndTime, paceMode = paceMode)
        val durationsSec = schedule.durationsSeconds

        val steps = routine.activeSteps
        val initialStepIndex = startIndex.coerceIn(0, (steps.size - 1).coerceAtLeast(0))
        val currentStepDurationSec = durationsSec.getOrElse(initialStepIndex) { 10 * 60 }

        val initialDurationsMap = steps.mapIndexed { idx, step ->
            step.id to durationsSec.getOrElse(idx) { step.defaultMinutes * 60 }
        }.toMap()
        val initialRemainingMap = initialDurationsMap

        val orderedIds = steps.map { it.id }

        val initialStep = steps.getOrNull(initialStepIndex)
        val initialIntervals = if (initialStep != null) {
            listOf(
                TaskInterval(
                    stepId = initialStep.id,
                    enterEpochMillis = System.currentTimeMillis(),
                    exitEpochMillis = null,
                    allocatedDurationSeconds = currentStepDurationSec
                )
            )
        } else {
            emptyList()
        }

        val startEvent = SessionEvent(
            eventId = "evt_${System.currentTimeMillis()}_${(1000..9999).random()}",
            eventType = "START_ROUTINE",
            stepId = initialStep?.id ?: "",
            stepTitle = initialStep?.title ?: "",
            stepIndex = initialStepIndex,
            deviceId = repository.deviceId,
            timestamp = System.currentTimeMillis(),
            allocatedDurationSeconds = currentStepDurationSec,
            remainingSeconds = currentStepDurationSec,
            savedBonusSeconds = 0,
            bonusTimeSeconds = 0,
            isAssisted = false,
            paceMode = paceMode
        )

        val newSession = ActiveSession(
            sessionId = "session_${routine.id}",
            routineId = routine.id,
            routineTitle = routine.title,
            userId = authManager.currentUser.value?.uid ?: "",
            status = SessionStatus.RUNNING,
            currentStepId = initialStep?.id ?: "",
            currentStepIndex = initialStepIndex,
            stepRemainingSeconds = currentStepDurationSec,
            stepTotalSeconds = currentStepDurationSec,
            bonusTimeSeconds = 0,
            totalElapsedSeconds = 0,
            completedStepIds = steps.take(initialStepIndex).map { it.id },
            orderedStepIds = orderedIds,
            stepDurationsSeconds = durationsSec,
            stepDurationsSecondsMap = initialDurationsMap,
            stepRemainingSecondsMap = initialRemainingMap,
            targetEndTime = effectiveTargetEndTime,
            startedAtWallTime = currentWallTimeStr,
            startEpochMillis = System.currentTimeMillis(),
            stepStartEpochMillis = System.currentTimeMillis(),
            paceMode = paceMode,
            controllerDeviceId = repository.deviceId,
            taskIntervals = initialIntervals,
            eventLog = listOf(startEvent),
            updatedAt = System.currentTimeMillis()
        )
        AppLogger.i("RoutineViewModel", "Started routine ${routine.title} with targetEnd=$effectiveTargetEndTime, tasks=${orderedIds.size}")

        repository.updateActiveSession(newSession, syncToCloud = true)
        notificationHelper.notifyFamilyRoutineStarted(
            routineTitle = routine.title,
            familyCode = familyCode.value,
            totalTasks = steps.size
        )
        _currentScreen.value = Screen.RunMode
        startRunLoop()
    }

    /**
     * Dynamically adjust target end time on an active session and elastically re-scale remaining tasks.
     */
    fun adjustTargetEndTime(newTargetEndTime: String) {
        val session = activeSession.value ?: return
        val routine = currentRoutine.value
        val steps = routine.activeSteps
        if (steps.isEmpty()) return

        val withNewTarget = session.copy(
            targetEndTime = newTargetEndTime,
            paceMode = "ELASTIC",
            updatedAt = System.currentTimeMillis()
        )
        val rebalanced = rebalanceUpcomingElasticSchedule(withNewTarget, routine)
        repository.recordSessionEvent(
            eventType = "ADJUST_TIME",
            session = rebalanced,
            stepId = rebalanced.currentStepId,
            stepTitle = steps.find { it.id == rebalanced.currentStepId }?.title ?: "",
            remainingSec = rebalanced.stepRemainingSeconds,
            paceMode = "ELASTIC"
        )
    }

    /**
     * Core elastic schedule rebalancer for upcoming tasks.
     * When pace is ELASTIC:
     * - The currently running task is kept undisturbed (current remaining & total preserved).
     * - Past completed and skipped tasks remain 100% frozen / immutable.
     * - Only upcoming uncompleted tasks are scaled to fit available wall-clock time until target departure.
     * - Any spare time first expands upcoming tasks up to their maxMinutes.
     * - Only once all upcoming tasks have expanded to their maximum minutes is surplus time banked to bonus time!
     */
    fun rebalanceUpcomingElasticSchedule(
        session: ActiveSession,
        routine: Routine,
        surplusAvailableSec: Int = 0
    ): ActiveSession {
        if (session.paceMode != "ELASTIC") return session
        val steps = session.getAllSteps(routine.activeSteps)
        if (steps.isEmpty()) return session

        val activeStepId = session.currentStepId.ifEmpty {
            steps.getOrNull(session.currentStepIndex)?.id ?: ""
        }
        val currentIdx = steps.indexOfFirst { it.id == activeStepId }.takeIf { it >= 0 } ?: session.currentStepIndex.coerceIn(0, steps.size - 1)
        val activeStep = steps.getOrNull(currentIdx) ?: return session

        val updatedDurationsSec = (if (session.stepDurationsSeconds.size == steps.size) {
            session.stepDurationsSeconds
        } else {
            steps.map { it.defaultMinutes * 60 }
        }).toMutableList()

        val remainingMap = session.stepRemainingSecondsMap.toMutableMap()
        val durationsMap = session.stepDurationsSecondsMap.toMutableMap()

        // Identify upcoming uncompleted tasks (excluding the currently active running task and completed/skipped tasks)
        val upcomingSteps = steps.filterIndexed { idx, step ->
            idx != currentIdx && !session.completedStepIds.contains(step.id) && !session.skippedStepIds.contains(step.id)
        }

        if (upcomingSteps.isEmpty()) {
            return session
        }

        // Calculate available time until target end time after current running task completes
        val cal = Calendar.getInstance()
        val nowMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val parts = session.targetEndTime.ifEmpty { routine.targetDepartureTime }.split(":")
        val targetH = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val targetM = parts.getOrNull(1)?.toIntOrNull() ?: 0
        var diffMinutes = (targetH * 60 + targetM) - nowMinutes
        if (diffMinutes < 0) diffMinutes += 24 * 60
        val effectiveAvailableMin = if (diffMinutes in 5..360) {
            diffMinutes
        } else {
            routine.getWindowMinutes().coerceIn(15, 180)
        }
        val currentStepRemainingSec = getDynamicStepRemainingSeconds(session).coerceAtLeast(0)
        val availableForUpcomingSec = (effectiveAvailableMin * 60 - currentStepRemainingSec + surplusAvailableSec)

        val sumMinSec = upcomingSteps.sumOf { it.minMinutes * 60 }
        val sumNominalSec = upcomingSteps.sumOf { it.defaultMinutes * 60 }
        val sumMaxSec = upcomingSteps.sumOf { it.maxMinutes * 60 }
        val elasticDurations: Map<String, Int> = when {
            availableForUpcomingSec <= sumMinSec -> upcomingSteps.associate { it.id to it.minMinutes * 60 }
            availableForUpcomingSec >= sumMaxSec -> upcomingSteps.associate { it.id to it.maxMinutes * 60 }
            availableForUpcomingSec <= sumNominalSec -> {
                val range = (sumNominalSec - sumMinSec).coerceAtLeast(1)
                val factor = (availableForUpcomingSec - sumMinSec).toDouble() / range
                upcomingSteps.associate { step ->
                    val minSec = step.minMinutes * 60
                    val nomSec = step.defaultMinutes * 60
                    step.id to (minSec + ((nomSec - minSec) * factor)).toInt().coerceAtLeast(30)
                }
            }
            else -> {
                val range = (sumMaxSec - sumNominalSec).coerceAtLeast(1)
                val factor = (availableForUpcomingSec - sumNominalSec).toDouble() / range
                upcomingSteps.associate { step ->
                    val nomSec = step.defaultMinutes * 60
                    val maxSec = step.maxMinutes * 60
                    step.id to (nomSec + ((maxSec - nomSec) * factor)).toInt().coerceAtLeast(30)
                }
            }
        }

        // Freeze currently running task AND completed/skipped tasks; scale ONLY upcoming tasks!
        steps.forEachIndexed { idx, step ->
            if (idx == currentIdx || session.completedStepIds.contains(step.id) || session.skippedStepIds.contains(step.id)) {
                // FROZEN: Leave duration and remaining time intact
                return@forEachIndexed
            }

            val newAllocSec = elasticDurations[step.id] ?: (step.defaultMinutes * 60)
            if (idx < updatedDurationsSec.size) {
                updatedDurationsSec[idx] = newAllocSec
            }
            remainingMap[step.id] = newAllocSec
            durationsMap[step.id] = newAllocSec
        }

        // If explicit early-completion surplus was provided and exceeds maxMinutes of all upcoming tasks,
        // bank the excess into bonus time!
        val overflowSurplusBankedSec = if (surplusAvailableSec > 0 && availableForUpcomingSec > sumMaxSec) {
            minOf(surplusAvailableSec, availableForUpcomingSec - sumMaxSec)
        } else {
            0
        }

        val newBonus = session.bonusTimeSeconds + overflowSurplusBankedSec

        return session.copy(
            stepDurationsSeconds = updatedDurationsSec,
            stepDurationsSecondsMap = durationsMap,
            stepRemainingSecondsMap = remainingMap,
            bonusTimeSeconds = newBonus,
            currentStepId = activeStep.id,
            controllerDeviceId = session.controllerDeviceId.ifEmpty { repository.deviceId },
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Switch pace mode on the active session (RUSH, NOMINAL, RELAXED, or ELASTIC).
     * The currently running task is NOT affected (its allocation and timer continue undisturbed).
     * Past completed and skipped tasks also remain 100% frozen / immutable with their earned bonus intact.
     * Only upcoming uncompleted tasks are scaled to the new pace mode.
     */
    fun setPaceMode(newPaceMode: String) {
        val session = activeSession.value ?: return
        val routine = currentRoutine.value
        val steps = routine.activeSteps
        if (steps.isEmpty()) return

        val activeStepId = session.currentStepId.ifEmpty {
            steps.getOrNull(session.currentStepIndex)?.id ?: ""
        }
        val currentIdx = steps.indexOfFirst { it.id == activeStepId }.takeIf { it >= 0 } ?: session.currentStepIndex.coerceIn(0, steps.size - 1)
        val activeStep = steps.getOrNull(currentIdx) ?: return

        val updated = if (newPaceMode == "ELASTIC") {
            rebalanceUpcomingElasticSchedule(session.copy(paceMode = "ELASTIC"), routine)
        } else {
            val updatedDurationsSec = (if (session.stepDurationsSeconds.size == steps.size) {
                session.stepDurationsSeconds
            } else {
                steps.map { it.defaultMinutes * 60 }
            }).toMutableList()
            val remainingMap = session.stepRemainingSecondsMap.toMutableMap()
            val durationsMap = session.stepDurationsSecondsMap.toMutableMap()

            steps.forEachIndexed { idx, step ->
                if (idx == currentIdx || session.completedStepIds.contains(step.id) || session.skippedStepIds.contains(step.id)) {
                    return@forEachIndexed
                }
                val newAllocSec = when (newPaceMode) {
                    "RUSH" -> step.minMinutes * 60
                    "NOMINAL" -> step.defaultMinutes * 60
                    "RELAXED" -> step.maxMinutes * 60
                    else -> step.defaultMinutes * 60
                }
                updatedDurationsSec[idx] = newAllocSec
                remainingMap[step.id] = newAllocSec
                durationsMap[step.id] = newAllocSec
            }

            session.copy(
                stepDurationsSeconds = updatedDurationsSec,
                stepDurationsSecondsMap = durationsMap,
                stepRemainingSecondsMap = remainingMap,
                paceMode = newPaceMode,
                currentStepId = activeStep.id,
                controllerDeviceId = session.controllerDeviceId.ifEmpty { repository.deviceId },
                updatedAt = System.currentTimeMillis()
            )
        }

        AppLogger.i("RoutineViewModel", "setPaceMode: switched to $newPaceMode, active step ${activeStep.title} kept undisturbed (${session.stepRemainingSeconds}s remaining), upcoming tasks scaled")
        repository.recordSessionEvent(
            eventType = "PACE_CHANGE",
            session = updated,
            stepId = activeStep.id,
            stepTitle = activeStep.title,
            allocatedSec = updated.stepDurationsSecondsMap[activeStep.id] ?: session.stepTotalSeconds,
            remainingSec = session.stepRemainingSeconds,
            paceMode = newPaceMode
        )
    }

    fun resumeSession() {
        _currentScreen.value = Screen.RunMode
        reconcileSessionWithWallClock()
        startRunLoop()
    }

    private fun startRunLoop() {
        timerJob?.cancel()
        reconcileSessionWithWallClock()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                tickSession(1)
            }
        }
    }

    /**
     * Reconciles active session with elapsed real-world wall-clock time when app wakes up.
     */
    fun reconcileSessionWithWallClock() {
        val session = activeSession.value ?: return
        if (session.status != SessionStatus.RUNNING) return
        val nowMs = System.currentTimeMillis()
        val elapsedMs = (nowMs - session.updatedAt).coerceAtLeast(0)
        val deltaSec = (elapsedMs / 1000).toInt()
        if (deltaSec >= 2) {
            tickSession(deltaSec.coerceAtMost(14400))
        }
    }

    private fun tickSession(deltaSec: Int = 1) {
        val session = activeSession.value ?: return
        if (session.status != SessionStatus.RUNNING) return

        val nowMs = System.currentTimeMillis()
        val currentRemaining = session.stepRemainingSeconds
        val newRemaining = currentRemaining - deltaSec

        // Calculate if any overtime occurred during this elapsed period
        val overtimeAddedSec = when {
            currentRemaining > 0 && newRemaining < 0 -> -newRemaining
            currentRemaining <= 0 -> deltaSec
            else -> 0
        }

        val newBonus = if (overtimeAddedSec > 0 && session.bonusTimeSeconds > 0) {
            (session.bonusTimeSeconds - overtimeAddedSec).coerceAtLeast(0)
        } else {
            session.bonusTimeSeconds
        }

        val currentStep = currentRoutine.value.activeSteps.getOrNull(session.currentStepIndex)
        val updatedMap = if (currentStep != null) {
            session.stepRemainingSecondsMap + (currentStep.id to newRemaining)
        } else {
            session.stepRemainingSecondsMap
        }

        val newElapsed = session.totalElapsedSeconds + deltaSec

        if (deltaSec >= 5) {
            AppLogger.i("RoutineViewModel", "Wall-clock sync reconciled sleep: elapsed=${deltaSec}s, step=$currentRemaining->$newRemaining, bonus=${session.bonusTimeSeconds}->$newBonus")
        }

        val updated = session.copy(
            stepRemainingSeconds = newRemaining,
            stepRemainingSecondsMap = updatedMap,
            bonusTimeSeconds = newBonus,
            totalElapsedSeconds = newElapsed,
            currentStepId = currentStep?.id ?: session.currentStepId,
            updatedAt = nowMs
        )
        // Never push periodic heartbeats to cloud during task execution.
        // Cloud writes only happen on explicit user state transitions (Up Next, Skip, +2m, etc.)
        repository.updateActiveSession(updated, syncToCloud = false)

        if (currentRemaining > 30 && newRemaining <= 30 && newRemaining > 0) {
            soundEffects.playWarningTick(viewModelScope)
        } else if (currentRemaining > 0 && newRemaining <= 0) {
            soundEffects.playWarningTick(viewModelScope)
        }
    }

    /**
     * Jump directly to any task in the routine (allows tasks to be done out of order).
     * Preserves previously remaining time on both the exited task and the target task.
     */
    /**
     * Jump directly to any task in the routine (allows tasks to be done out of order).
     * Preserves previously remaining time on both the exited task and the target task.
     */
    fun jumpToStep(index: Int) {
        val session = activeSession.value ?: return
        val routine = currentRoutine.value
        val steps = session.getAllSteps(routine.activeSteps)
        if (steps.isEmpty()) return
        val safeIndex = index.coerceIn(0, steps.size - 1)

        // Save current step's remaining seconds before jumping
        val currentStep = if (session.currentStepId.isNotEmpty()) {
            steps.find { it.id == session.currentStepId } ?: steps.getOrNull(session.currentStepIndex)
        } else {
            steps.getOrNull(session.currentStepIndex)
        }
        val remainingMap = session.stepRemainingSecondsMap.toMutableMap()
        if (currentStep != null) {
            remainingMap[currentStep.id] = session.stepRemainingSeconds
        }

        val targetStep = steps[safeIndex]
        val stepDurationSec = session.getStepDurationSeconds(targetStep.id, targetStep.defaultMinutes * 60)

        // If targetStep was previously marked completed, switching to it MUST mark it as incomplete
        // and reverse the banked bonus time and stars so bonus time does not infinitely accumulate!
        val wasTargetCompleted = session.completedStepIds.contains(targetStep.id)
        val wasTargetAssisted = session.assistedStepIds.contains(targetStep.id)
        val bankedMap = session.stepBankedSecondsMap.toMutableMap()
        val totalRoutineSeconds = (routine.totalMinutes * 60).coerceAtLeast(steps.sumOf { it.defaultMinutes * 60 })

        var updatedBonus = session.bonusTimeSeconds
        var updatedCompleted = session.completedStepIds
        var updatedAssisted = session.assistedStepIds

        if (wasTargetCompleted) {
            updatedCompleted = updatedCompleted - targetStep.id
            updatedAssisted = updatedAssisted - targetStep.id

            // Revert stars if earned independently
            if (!wasTargetAssisted) {
                val reward = if (targetStep.starReward > 0) targetStep.starReward else 1
                repository.addStars(-reward)
            }

            // Deduct the banked bonus seconds from this task
            val banked = bankedMap.remove(targetStep.id) ?: 0
            updatedBonus = (updatedBonus - banked).coerceAtLeast(0)
        }

        // Restore remaining seconds if visited before/preserved; otherwise full step duration
        val targetRemaining = remainingMap[targetStep.id]?.takeIf { it > 0 } ?: stepDurationSec
        remainingMap[targetStep.id] = targetRemaining

        val now = System.currentTimeMillis()
        val closedIntervals = session.taskIntervals.map { interval ->
            if (interval.exitEpochMillis == null) {
                interval.copy(exitEpochMillis = now)
            } else {
                interval
            }
        }
        val targetInterval = TaskInterval(
            stepId = targetStep.id,
            enterEpochMillis = now,
            exitEpochMillis = null,
            allocatedDurationSeconds = stepDurationSec
        )
        val updatedIntervals = closedIntervals + targetInterval

        val updated = session.copy(
            status = if (session.status == SessionStatus.COMPLETED) SessionStatus.RUNNING else session.status,
            currentStepIndex = safeIndex,
            currentStepId = targetStep.id,
            stepTotalSeconds = stepDurationSec,
            stepRemainingSeconds = targetRemaining,
            bonusTimeSeconds = updatedBonus.coerceIn(0, totalRoutineSeconds),
            completedStepIds = updatedCompleted,
            assistedStepIds = updatedAssisted,
            stepRemainingSecondsMap = remainingMap,
            stepBankedSecondsMap = bankedMap,
            controllerDeviceId = session.controllerDeviceId.ifEmpty { repository.deviceId },
            taskIntervals = updatedIntervals,
            stepStartEpochMillis = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        AppLogger.i("RoutineViewModel", "jumpToStep: ${targetStep.title} (stepId=${targetStep.id}), controller=${updated.controllerDeviceId}")
        repository.recordSessionEvent(
            eventType = "SWITCH_STEP",
            session = updated,
            stepId = targetStep.id,
            stepTitle = targetStep.title,
            allocatedSec = stepDurationSec,
            remainingSec = targetRemaining
        )
    }

    fun jumpToStepById(stepId: String) {
        val session = activeSession.value ?: return
        val routine = currentRoutine.value
        val steps = session.getAllSteps(routine.activeSteps)
        val idx = steps.indexOfFirst { it.id == stepId }
        if (idx >= 0) {
            jumpToStep(idx)
        }
    }

    /**
     * Allows a child (or parent) to spend earned bonus time on a special activity
     * (e.g. Cuddle with Mom, Put on Lotion, Read a Story) during a live routine.
     *
     * @param title The activity name (e.g. "Cuddle with Mom")
     * @param durationMinutes The minutes to allocate from bonus time
     * @param iconKey Icon identifier ("cuddle", "lotion", etc.)
     * @param colorHex Color theme for the activity
     * @param startImmediately If true, switches to this task right now; if false, adds as the next task
     */
    fun addBonusActivity(
        title: String,
        durationMinutes: Int,
        iconKey: String = "cuddle",
        colorHex: String = "#EC4899",
        catalogTaskId: String? = null,
        startImmediately: Boolean = true
    ) {
        val session = activeSession.value ?: return
        val routine = currentRoutine.value
        val safeMinutes = durationMinutes.coerceAtLeast(1)
        val bonusSecondsCost = safeMinutes * 60

        // Deduct from bonus time
        val updatedBonus = (session.bonusTimeSeconds - bonusSecondsCost).coerceAtLeast(0)

        val newStepId = "bonus_${System.currentTimeMillis()}"
        val bonusStep = RoutineStep(
            id = newStepId,
            catalogTaskId = catalogTaskId,
            title = title,
            iconKey = iconKey,
            minMinutes = 1,
            maxMinutes = safeMinutes + 2,
            defaultMinutes = safeMinutes,
            colorHex = colorHex,
            isImportant = false,
            starReward = 0, // Bonus treats are their own reward
            isEnabled = true
        )

        val allCurrentSteps = session.getAllSteps(routine.activeSteps)
        val activeStepId = session.currentStepId.ifEmpty {
            allCurrentSteps.getOrNull(session.currentStepIndex)?.id ?: ""
        }

        // Preserve all existing maps
        val remainingMap = session.stepRemainingSecondsMap.toMutableMap()
        val durationsMap = session.stepDurationsSecondsMap.toMutableMap()

        // Set the bonus step's allocation and remaining time
        durationsMap[newStepId] = safeMinutes * 60
        remainingMap[newStepId] = safeMinutes * 60

        // If current step is running, preserve its remaining seconds
        if (activeStepId.isNotEmpty() && startImmediately) {
            remainingMap[activeStepId] = session.stepRemainingSeconds
        }

        // Determine insertion position in orderedStepIds
        val currentOrdered = if (session.orderedStepIds.isNotEmpty()) {
            session.orderedStepIds.toMutableList()
        } else {
            allCurrentSteps.map { it.id }.toMutableList()
        }

        val activeIdx = currentOrdered.indexOf(activeStepId).takeIf { it >= 0 }
            ?: session.currentStepIndex.coerceIn(0, currentOrdered.size)

        val insertIndex = if (startImmediately) {
            activeIdx.coerceIn(0, currentOrdered.size)
        } else {
            (activeIdx + 1).coerceIn(0, currentOrdered.size)
        }

        currentOrdered.add(insertIndex, newStepId)

        val updatedBonusSteps = (session.bonusSteps + bonusStep).distinctBy { it.id }

        // Durations array aligned with currentOrdered
        val updatedDurationsSec = currentOrdered.map { id ->
            durationsMap[id] ?: ((allCurrentSteps.find { it.id == id }?.defaultMinutes ?: safeMinutes) * 60)
        }

        val now = System.currentTimeMillis()
        val closedIntervals = session.taskIntervals.map { interval ->
            if (interval.exitEpochMillis == null) interval.copy(exitEpochMillis = now) else interval
        }
        val bonusInterval = TaskInterval(
            stepId = newStepId,
            enterEpochMillis = now,
            exitEpochMillis = null,
            allocatedDurationSeconds = safeMinutes * 60
        )
        val updatedIntervals = if (startImmediately) closedIntervals + bonusInterval else session.taskIntervals

        val updatedSession = session.copy(
            currentStepIndex = if (startImmediately) insertIndex else session.currentStepIndex,
            currentStepId = if (startImmediately) newStepId else session.currentStepId,
            stepTotalSeconds = if (startImmediately) safeMinutes * 60 else session.stepTotalSeconds,
            stepRemainingSeconds = if (startImmediately) safeMinutes * 60 else session.stepRemainingSeconds,
            bonusTimeSeconds = updatedBonus,
            bonusSteps = updatedBonusSteps,
            orderedStepIds = currentOrdered,
            stepDurationsSeconds = updatedDurationsSec,
            stepDurationsSecondsMap = durationsMap,
            stepRemainingSecondsMap = remainingMap,
            completedStepIds = session.completedStepIds, // STRICTLY PRESERVED: completed tasks remain 100% completed!
            assistedStepIds = session.assistedStepIds,
            controllerDeviceId = session.controllerDeviceId.ifEmpty { repository.deviceId },
            taskIntervals = updatedIntervals,
            stepStartEpochMillis = if (startImmediately) now else session.stepStartEpochMillis,
            updatedAt = now
        )

        repository.recordSessionEvent(
            eventType = "ADD_BONUS_TASK",
            session = updatedSession,
            stepId = newStepId,
            stepTitle = title,
            allocatedSec = safeMinutes * 60,
            remainingSec = safeMinutes * 60
        )
        soundEffects.playCelebration(viewModelScope)
    }

    /**
     * Parent grant bonus minutes (e.g. encouraging daughter or gifting extra bonus time).
     */
    fun grantBonusMinutes(minutes: Int) {
        val session = activeSession.value ?: return
        val current = session.bonusTimeSeconds
        val updated = current + (minutes * 60)
        val updatedSession = session.copy(
            bonusTimeSeconds = updated,
            controllerDeviceId = session.controllerDeviceId.ifEmpty { repository.deviceId },
            updatedAt = System.currentTimeMillis()
        )
        repository.updateActiveSession(updatedSession, syncToCloud = true)
        soundEffects.playStepCompleted(viewModelScope)
    }

    /**
     * Complete the current step early or advance to next.
     * When user taps "Done!", remaining seconds in this task bank into Bonus Time!
     */
    /**
     * Complete the current step early or advance to next.
     * isParentAssisted = true means parent came in to assist the child on this task.
     * - Child independent completion before timer expires: full star reward awarded + bonus time banked
     * - Parent assisted completion: advances routine + banks bonus time, but awards ZERO stars
     */
    fun completeCurrentStepEarly(isParentAssisted: Boolean = false) {
        val session = activeSession.value
        if (session?.status == SessionStatus.COMPLETED) {
            finishRoutineAndShowSummary()
            return
        }
        soundEffects.playStepCompleted(viewModelScope)
        nextStep(bankBonus = true, isParentAssisted = isParentAssisted)
    }

    fun nextStep(bankBonus: Boolean, isParentAssisted: Boolean = false) {
        val session = activeSession.value ?: return
        if (session.status == SessionStatus.COMPLETED) {
            finishRoutineAndShowSummary()
            return
        }
        val routine = currentRoutine.value
        val allSteps = session.getAllSteps(routine.activeSteps)
        val orderedSteps = if (session.orderedStepIds.isNotEmpty()) {
            val stepMap = allSteps.associateBy { it.id }
            session.orderedStepIds.mapNotNull { stepMap[it] }.ifEmpty { allSteps }
        } else {
            allSteps
        }
        if (orderedSteps.isEmpty()) return

        val totalRoutineSeconds = (routine.totalMinutes * 60).coerceAtLeast(orderedSteps.sumOf { it.defaultMinutes * 60 })
        val savedSeconds = if (bankBonus && session.stepRemainingSeconds > 0) {
            session.stepRemainingSeconds.coerceAtMost(session.stepTotalSeconds)
        } else {
            0
        }
        val overtimeSec = if (session.stepRemainingSeconds < 0) -session.stepRemainingSeconds else 0
        val (_, deltaMins) = getCalculatedFinishTime(session, routine)
        val scheduleDeficitSec = if (deltaMins > 0) deltaMins * 60 else 0
        val totalDeficitSec = maxOf(overtimeSec, scheduleDeficitSec)
        val fromBonus = minOf(totalDeficitSec, session.bonusTimeSeconds)
        val deficitToHeal = totalDeficitSec - fromBonus
        val newBonusSeconds = if (savedSeconds > 0) {
            (session.bonusTimeSeconds + savedSeconds).coerceIn(0, totalRoutineSeconds)
        } else {
            (session.bonusTimeSeconds - fromBonus).coerceAtLeast(0)
        }
        val (autoHealedDurations, _) = if (deficitToHeal > 0) {
            autoHealFutureTasks(session, routine, deficitToHeal)
        } else {
            Pair(session.stepDurationsSeconds, 0)
        }

        val currentStep = if (session.currentStepId.isNotEmpty()) {
            orderedSteps.find { it.id == session.currentStepId } ?: orderedSteps.getOrNull(session.currentStepIndex)
        } else {
            orderedSteps.getOrNull(session.currentStepIndex)
        }
        val currentStepId = currentStep?.id
        val wasOvertime = session.stepRemainingSeconds <= 0
        val scheduledSec = session.stepTotalSeconds
        val actualSec = if (session.stepRemainingSeconds > 0) {
            (scheduledSec - session.stepRemainingSeconds).coerceAtLeast(1)
        } else {
            scheduledSec + (-session.stepRemainingSeconds)
        }

        val isAlreadyCompleted = currentStepId != null && session.completedStepIds.contains(currentStepId)

        // Record past run execution history ONLY if newly completed (prevents extra counts on last task!)
        if (currentStep != null && !isAlreadyCompleted) {
            repository.recordStepRun(
                routineId = routine.id,
                stepId = currentStep.id,
                stepTitle = currentStep.title,
                scheduledMinutes = currentStep.defaultMinutes,
                actualSecondsTaken = actualSec,
                wasOvertime = wasOvertime,
                wasAssisted = isParentAssisted,
                catalogTaskId = currentStep.catalogTaskId
            )
        }

        val map = session.stepRemainingSecondsMap.toMutableMap()
        val bankedMap = session.stepBankedSecondsMap.toMutableMap()
        if (currentStepId != null) {
            // Track the exact seconds banked for this step so we can reverse it if unmarked
            bankedMap[currentStepId] = savedSeconds
            // Preserve the remaining time that was left when completed (instead of resetting to 0)
            map[currentStepId] = session.stepRemainingSeconds
        }

        val updatedAssisted = if (isParentAssisted && currentStepId != null && !session.assistedStepIds.contains(currentStepId)) {
            session.assistedStepIds + currentStepId
        } else {
            session.assistedStepIds
        }

        val newCompletedIds = if (currentStepId != null && !session.completedStepIds.contains(currentStepId)) {
            // Award stars ONLY if completed independently before the timer expires!
            if (!wasOvertime && !isParentAssisted) {
                val reward = if (currentStep != null && currentStep.starReward > 0) currentStep.starReward else 1
                repository.addStars(reward)
            }
            session.completedStepIds + currentStepId
        } else {
            session.completedStepIds
        }

        // Check if all steps in the routine are completed or skipped (even if done out of order)
        val allCompleted = orderedSteps.all { newCompletedIds.contains(it.id) || session.skippedStepIds.contains(it.id) }

        val now = System.currentTimeMillis()
        val closedIntervals = session.taskIntervals.map { interval ->
            if (interval.exitEpochMillis == null) {
                interval.copy(exitEpochMillis = now)
            } else {
                interval
            }
        }

        if (allCompleted) {
            // Entire routine finished!
            soundEffects.playCelebration(viewModelScope)
            timerJob?.cancel()
            timerJob = null

            val completedSession = session.copy(
                status = SessionStatus.COMPLETED,
                stepRemainingSeconds = 0,
                bonusTimeSeconds = newBonusSeconds,
                completedStepIds = newCompletedIds,
                assistedStepIds = updatedAssisted,
                stepDurationsSeconds = autoHealedDurations,
                stepRemainingSecondsMap = map,
                stepBankedSecondsMap = bankedMap,
                controllerDeviceId = session.controllerDeviceId.ifEmpty { repository.deviceId },
                taskIntervals = closedIntervals,
                updatedAt = System.currentTimeMillis()
            )
            repository.clearActiveSession()
            // Increment star count and mark last completed date
            val cal = Calendar.getInstance()
            val todayKey = String.format(Locale.getDefault(), "%04d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
            repository.updateLocalRoutine(routine.copy(lastCompletedDate = todayKey, starCount = routine.starCount + 1))

            // Show summary and navigate back to dashboard
            _completionSummary.value = buildCompletionSummary(completedSession, routine)
            _currentScreen.value = Screen.Dashboard
        } else {
            // Find next uncompleted step following ordered sequence from current step
            val currentIdxInOrdered = orderedSteps.indexOfFirst { it.id == currentStepId }.takeIf { it >= 0 }
                ?: session.currentStepIndex.coerceIn(0, orderedSteps.size - 1)

            var nextIndex = -1
            for (i in (currentIdxInOrdered + 1) until orderedSteps.size) {
                if (!newCompletedIds.contains(orderedSteps[i].id) && !session.skippedStepIds.contains(orderedSteps[i].id)) {
                    nextIndex = i
                    break
                }
            }
            if (nextIndex == -1) {
                for (i in 0 until currentIdxInOrdered) {
                    if (!newCompletedIds.contains(orderedSteps[i].id) && !session.skippedStepIds.contains(orderedSteps[i].id)) {
                        nextIndex = i
                        break
                    }
                }
            }
            if (nextIndex == -1) {
                nextIndex = (currentIdxInOrdered + 1).coerceAtMost(orderedSteps.size - 1)
            }

            val nextStep = orderedSteps[nextIndex]
            val nextStepDurationSec = session.getStepDurationSeconds(nextStep.id, nextStep.defaultMinutes * 60)
            val nextRemaining = minOf(map[nextStep.id]?.takeIf { it > 0 } ?: nextStepDurationSec, nextStepDurationSec)
            map[nextStep.id] = nextRemaining

            val nextInterval = TaskInterval(
                stepId = nextStep.id,
                enterEpochMillis = now,
                exitEpochMillis = null,
                allocatedDurationSeconds = nextStepDurationSec
            )
            val updatedIntervals = closedIntervals + nextInterval

            val updatedDurationsMap = session.stepDurationsSecondsMap.toMutableMap()
            orderedSteps.forEachIndexed { idx, s ->
                if (idx < autoHealedDurations.size) updatedDurationsMap[s.id] = autoHealedDurations[idx]
            }

            val baseSession = session.copy(
                currentStepIndex = nextIndex,
                currentStepId = nextStep.id,
                stepRemainingSeconds = nextRemaining,
                stepTotalSeconds = nextStepDurationSec,
                bonusTimeSeconds = newBonusSeconds,
                completedStepIds = newCompletedIds,
                assistedStepIds = updatedAssisted,
                stepDurationsSeconds = autoHealedDurations,
                stepDurationsSecondsMap = updatedDurationsMap,
                stepRemainingSecondsMap = map,
                stepBankedSecondsMap = bankedMap,
                controllerDeviceId = session.controllerDeviceId.ifEmpty { repository.deviceId },
                taskIntervals = updatedIntervals,
                stepStartEpochMillis = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            // When pace is ELASTIC, automatically re-balance all upcoming tasks against the target end time!
            // Any saved time first expands future tasks towards their maxMinutes.
            val updatedSession = if (baseSession.paceMode == "ELASTIC") {
                rebalanceUpcomingElasticSchedule(baseSession, routine)
            } else {
                baseSession
            }

            val finalRemaining = updatedSession.stepRemainingSecondsMap[nextStep.id] ?: nextRemaining
            val finalTotal = updatedSession.getStepDurationSeconds(nextStep.id, nextStepDurationSec)

            AppLogger.i("RoutineViewModel", "nextStep: ${nextStep.title} (stepId=${nextStep.id}), remaining=${finalRemaining}s, bonus=${updatedSession.bonusTimeSeconds}s, controller=${updatedSession.controllerDeviceId}")
            repository.recordSessionEvent(
                eventType = "COMPLETE_STEP",
                session = updatedSession,
                stepId = currentStepId ?: "",
                stepTitle = currentStep?.title ?: "",
                allocatedSec = scheduledSec,
                remainingSec = session.stepRemainingSeconds,
                savedBonusSec = savedSeconds,
                isAssisted = isParentAssisted
            )
            repository.recordSessionEvent(
                eventType = "SWITCH_STEP",
                session = updatedSession,
                stepId = nextStep.id,
                stepTitle = nextStep.title,
                allocatedSec = nextStepDurationSec,
                remainingSec = nextRemaining
            )
        }
    }

    fun toggleStepCompletion(stepId: String) {
        val session = activeSession.value ?: return
        val isCompleted = session.completedStepIds.contains(stepId)
        val isAssisted = session.assistedStepIds.contains(stepId)
        setStepCompletionStatus(stepId, isDone = !isCompleted, isAssisted = isAssisted)
    }

    /**
     * Skip the current task and advance directly to the next uncompleted task in the routine.
     * Does not award stars and does not bank bonus time for this skipped task.
     * The skipped task is excluded from remaining schedule calculation for the day.
     */
    fun skipCurrentStep() {
        val session = activeSession.value ?: return
        val routine = currentRoutine.value
        val steps = routine.activeSteps
        if (steps.isEmpty()) return
        val currentStep = steps.getOrNull(session.currentStepIndex)
        val currentStepId = currentStep?.id

        AppLogger.i("RoutineViewModel", "Skipping task ${currentStep?.title} ($currentStepId) for today")

        val newSkipped = if (currentStepId != null && !session.skippedStepIds.contains(currentStepId)) {
            session.skippedStepIds + currentStepId
        } else {
            session.skippedStepIds
        }

        // Check if all remaining steps are completed or skipped
        val allDoneOrSkipped = steps.all { session.completedStepIds.contains(it.id) || newSkipped.contains(it.id) }
        if (allDoneOrSkipped) {
            finishRoutineAndShowSummary()
            return
        }

        // Find the next uncompleted and unskipped step (searching forward, then wrapping)
        var nextIndex = -1
        for (i in (session.currentStepIndex + 1) until steps.size) {
            if (!session.completedStepIds.contains(steps[i].id) && !newSkipped.contains(steps[i].id)) {
                nextIndex = i
                break
            }
        }
        if (nextIndex == -1) {
            for (i in 0 until session.currentStepIndex) {
                if (!session.completedStepIds.contains(steps[i].id) && !newSkipped.contains(steps[i].id)) {
                    nextIndex = i
                    break
                }
            }
        }
        if (nextIndex == -1) {
            nextIndex = (session.currentStepIndex + 1).coerceAtMost(steps.size - 1)
        }

        val baseWithSkipped = session.copy(
            skippedStepIds = newSkipped,
            controllerDeviceId = session.controllerDeviceId.ifEmpty { repository.deviceId },
            updatedAt = System.currentTimeMillis()
        )
        // Automatically rebalance upcoming tasks when a task is skipped!
        val updatedWithSkipped = if (baseWithSkipped.paceMode == "ELASTIC") {
            rebalanceUpcomingElasticSchedule(baseWithSkipped, routine)
        } else {
            baseWithSkipped
        }
        repository.updateActiveSession(updatedWithSkipped, syncToCloud = true)

        jumpToStep(nextIndex)
    }

    fun toggleStepSkipped(stepId: String) {
        val session = activeSession.value ?: return
        val routine = currentRoutine.value
        val wasSkipped = session.skippedStepIds.contains(stepId)
        val newSkipped = if (wasSkipped) session.skippedStepIds - stepId else session.skippedStepIds + stepId
        val base = session.copy(
            skippedStepIds = newSkipped,
            controllerDeviceId = session.controllerDeviceId.ifEmpty { repository.deviceId },
            updatedAt = System.currentTimeMillis()
        )
        val updated = if (base.paceMode == "ELASTIC") {
            rebalanceUpcomingElasticSchedule(base, routine)
        } else {
            base
        }
        repository.recordSessionEvent(
            eventType = "SKIP_STEP",
            session = updated,
            stepId = stepId,
            stepTitle = routine.steps.find { it.id == stepId }?.title ?: ""
        )
        AppLogger.i("RoutineViewModel", "toggleStepSkipped: $stepId -> wasSkipped=$wasSkipped, nowSkipped=${!wasSkipped}, elasticRebalanced=${updated.paceMode == "ELASTIC"}")
    }

    /**
     * Explicitly set the done status and parent-assisted status of any task.
     * Accurately updates completedStepIds, assistedStepIds, star awards,
     * reverses bonus time if unmarking, and restores preserved remaining time.
     */
    fun setStepCompletionStatus(stepId: String, isDone: Boolean, isAssisted: Boolean = false) {
        val session = activeSession.value ?: return
        val routine = currentRoutine.value
        val step = routine.steps.find { it.id == stepId }
        val starReward = if (step != null && step.starReward > 0) step.starReward else 1

        val wasDone = session.completedStepIds.contains(stepId)
        val wasAssistedBefore = session.assistedStepIds.contains(stepId)

        val newCompleted = if (isDone) {
            if (!wasDone) session.completedStepIds + stepId else session.completedStepIds
        } else {
            session.completedStepIds - stepId
        }

        val newAssisted = if (isDone && isAssisted) {
            if (!wasAssistedBefore) session.assistedStepIds + stepId else session.assistedStepIds
        } else {
            session.assistedStepIds - stepId
        }

        // Adjust star rewards:
        if (!wasDone && isDone && !isAssisted) {
            repository.addStars(starReward)
        } else if (wasDone && isDone && !wasAssistedBefore && isAssisted) {
            repository.addStars(-starReward)
        } else if (wasDone && isDone && wasAssistedBefore && !isAssisted) {
            repository.addStars(starReward)
        } else if (wasDone && !isDone && !wasAssistedBefore) {
            repository.addStars(-starReward)
        }

        val remainingMap = session.stepRemainingSecondsMap.toMutableMap()
        val bankedMap = session.stepBankedSecondsMap.toMutableMap()
        var updatedBonus = session.bonusTimeSeconds

        val stepIndex = routine.activeSteps.indexOfFirst { it.id == stepId }
        val defaultStepDuration = if (stepIndex >= 0) {
            session.stepDurationsSeconds.getOrElse(stepIndex) { (step?.defaultMinutes ?: 5) * 60 }
        } else {
            (step?.defaultMinutes ?: 5) * 60
        }

        var newCurrentRemaining = session.stepRemainingSeconds
        var newCurrentTotal = session.stepTotalSeconds

        if (wasDone && !isDone) {
            // Task marked INCOMPLETE after having been completed:
            // 1. Correct bonus time by deducting whatever bonus was banked for this step
            val banked = bankedMap[stepId] ?: 0
            updatedBonus = (session.bonusTimeSeconds - banked).coerceAtLeast(0)
            bankedMap.remove(stepId)

            // 2. Retrieve the preserved remaining time from when it was completed
            val preservedRemaining = remainingMap[stepId]?.takeIf { it > 0 } ?: defaultStepDuration
            remainingMap[stepId] = preservedRemaining

            // 3. If the step being unmarked is the currently active step, restore its timer!
            val currentActiveStep = routine.activeSteps.getOrNull(session.currentStepIndex)
            if (currentActiveStep?.id == stepId) {
                newCurrentRemaining = preservedRemaining
                newCurrentTotal = defaultStepDuration
            }
        } else if (!wasDone && isDone) {
            // Task marked COMPLETED directly:
            val currentActiveStep = routine.activeSteps.getOrNull(session.currentStepIndex)
            val currentRem = if (currentActiveStep?.id == stepId) {
                session.stepRemainingSeconds
            } else {
                remainingMap[stepId] ?: defaultStepDuration
            }
            val totalRoutineSeconds = (routine.totalMinutes * 60).coerceAtLeast(routine.activeSteps.sumOf { it.defaultMinutes * 60 })
            val toBank = currentRem.coerceIn(0, defaultStepDuration)
            updatedBonus = (session.bonusTimeSeconds + toBank).coerceIn(0, totalRoutineSeconds)
            bankedMap[stepId] = toBank
            remainingMap[stepId] = currentRem
        }

        val totalRoutineSeconds = (routine.totalMinutes * 60).coerceAtLeast(routine.activeSteps.sumOf { it.defaultMinutes * 60 })
        val allDone = routine.activeSteps.all { newCompleted.contains(it.id) }
        val newStatus = if (allDone) {
            SessionStatus.COMPLETED
        } else if (session.status == SessionStatus.COMPLETED) {
            SessionStatus.RUNNING
        } else {
            session.status
        }

        val updated = session.copy(
            completedStepIds = newCompleted,
            assistedStepIds = newAssisted,
            bonusTimeSeconds = updatedBonus.coerceIn(0, totalRoutineSeconds),
            stepRemainingSeconds = newCurrentRemaining,
            stepTotalSeconds = newCurrentTotal,
            stepRemainingSecondsMap = remainingMap,
            stepBankedSecondsMap = bankedMap,
            status = newStatus,
            updatedAt = System.currentTimeMillis()
        )
        repository.updateActiveSession(updated, syncToCloud = true)
    }

    /**
     * Toggles whether a step was assisted by a parent or done independently.
     * Adjusts stars accordingly.
     */
    fun toggleStepAssisted(stepId: String) {
        val session = activeSession.value ?: return
        val isAssistedNow = session.assistedStepIds.contains(stepId)
        val isDone = session.completedStepIds.contains(stepId)
        setStepCompletionStatus(stepId, isDone = isDone, isAssisted = !isAssistedNow)
    }

    fun formatTimeAmPm(hhmm: String): String {
        val parts = hhmm.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: return hhmm
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val isPm = hour >= 12
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return String.format(Locale.getDefault(), "%d:%02d %s", displayHour, minute, if (isPm) "PM" else "AM")
    }

    fun previousStep() {
        val session = activeSession.value ?: return
        val routine = currentRoutine.value
        val steps = routine.activeSteps
        if (steps.isEmpty()) return
        val prevIndex = (session.currentStepIndex - 1).coerceAtLeast(0)

        // Save current step's remaining seconds before going back
        val currentStep = steps.getOrNull(session.currentStepIndex)
        val map = session.stepRemainingSecondsMap.toMutableMap()
        if (currentStep != null) {
            map[currentStep.id] = session.stepRemainingSeconds
        }

        val prevStep = steps[prevIndex]
        val prevStepDurationSec = session.stepDurationsSeconds.getOrElse(prevIndex) {
            prevStep.defaultMinutes * 60
        }
        val prevRemaining = map[prevStep.id] ?: prevStepDurationSec
        map[prevStep.id] = prevRemaining

        val now = System.currentTimeMillis()
        val closedIntervals = session.taskIntervals.map { interval ->
            if (interval.exitEpochMillis == null) interval.copy(exitEpochMillis = now) else interval
        }
        val prevInterval = TaskInterval(
            stepId = prevStep.id,
            enterEpochMillis = now,
            exitEpochMillis = null,
            allocatedDurationSeconds = prevStepDurationSec
        )
        val updatedIntervals = closedIntervals + prevInterval

        val updated = session.copy(
            currentStepIndex = prevIndex,
            currentStepId = prevStep.id,
            stepRemainingSeconds = prevRemaining,
            stepTotalSeconds = prevStepDurationSec,
            stepRemainingSecondsMap = map,
            controllerDeviceId = session.controllerDeviceId.ifEmpty { repository.deviceId },
            taskIntervals = updatedIntervals,
            stepStartEpochMillis = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        repository.updateActiveSession(updated, syncToCloud = true)
    }

    fun addExtraMinutes(minutes: Int = 2) {
        val session = activeSession.value ?: return
        val routine = currentRoutine.value
        val steps = routine.activeSteps
        val currentStep = steps.getOrNull(session.currentStepIndex)
        val addedSec = minutes * 60
        val newRemaining = session.stepRemainingSeconds + addedSec
        val map = session.stepRemainingSecondsMap.toMutableMap()
        if (currentStep != null) {
            map[currentStep.id] = newRemaining
        }

        // 1. Take extra minutes from banked bonus time first
        val fromBank = minOf(addedSec, session.bonusTimeSeconds)
        val remainingDeficitSec = addedSec - fromBank
        val newBonusSeconds = (session.bonusTimeSeconds - fromBank).coerceAtLeast(0)

        // 2. If banked time was insufficient to cover the added time, auto-heal
        // future uncompleted tasks by taking time away down towards their rushed time (minMinutes),
        // but never reducing them beyond minMinutes!
        val (autoHealedDurations, _) = if (remainingDeficitSec > 0) {
            autoHealFutureTasks(session, routine, remainingDeficitSec)
        } else {
            Pair(session.stepDurationsSeconds, 0)
        }

        val base = session.copy(
            stepRemainingSeconds = newRemaining,
            stepTotalSeconds = session.stepTotalSeconds + addedSec,
            bonusTimeSeconds = newBonusSeconds,
            stepDurationsSeconds = autoHealedDurations,
            stepRemainingSecondsMap = map,
            controllerDeviceId = session.controllerDeviceId.ifEmpty { repository.deviceId },
            stepStartEpochMillis = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val updated = if (base.paceMode == "ELASTIC") {
            rebalanceUpcomingElasticSchedule(base, routine)
        } else {
            base
        }
        repository.recordSessionEvent(
            eventType = "ADJUST_TIME",
            session = updated,
            stepId = currentStep?.id ?: session.currentStepId,
            stepTitle = currentStep?.title ?: "",
            allocatedSec = updated.stepTotalSeconds,
            remainingSec = updated.stepRemainingSeconds
        )
    }

    fun removeMinutes(minutes: Int = 1) {
        val session = activeSession.value ?: return
        val routine = currentRoutine.value
        val currentStep = routine.activeSteps.getOrNull(session.currentStepIndex)
        val subtractSec = minutes * 60
        val newRemaining = (session.stepRemainingSeconds - subtractSec).coerceAtLeast(5)
        val newTotal = (session.stepTotalSeconds - subtractSec).coerceAtLeast(5)
        val map = session.stepRemainingSecondsMap.toMutableMap()
        if (currentStep != null) {
            map[currentStep.id] = newRemaining
        }

        val base = session.copy(
            stepRemainingSeconds = newRemaining,
            stepTotalSeconds = newTotal,
            stepRemainingSecondsMap = map,
            controllerDeviceId = session.controllerDeviceId.ifEmpty { repository.deviceId },
            stepStartEpochMillis = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val updated = if (base.paceMode == "ELASTIC") {
            rebalanceUpcomingElasticSchedule(base, routine)
        } else {
            base
        }
        repository.recordSessionEvent(
            eventType = "ADJUST_TIME",
            session = updated,
            stepId = currentStep?.id ?: session.currentStepId,
            stepTitle = currentStep?.title ?: "",
            allocatedSec = updated.stepTotalSeconds,
            remainingSec = updated.stepRemainingSeconds
        )
    }

    fun startQuickTimer(
        title: String = "Quick Timer",
        durationMinutes: Int = 5,
        awardStars: Boolean = true,
        starReward: Int = 1,
        iconKey: String = "hourglass"
    ) {
        val safeDuration = durationMinutes.coerceAtLeast(1)
        val cal = Calendar.getInstance()
        val startH = cal.get(Calendar.HOUR_OF_DAY)
        val startM = cal.get(Calendar.MINUTE)
        val startTime = String.format(Locale.getDefault(), "%02d:%02d", startH, startM)
        val endCal = Calendar.getInstance().apply { add(Calendar.MINUTE, safeDuration) }
        val targetEndTime = String.format(Locale.getDefault(), "%02d:%02d", endCal.get(Calendar.HOUR_OF_DAY), endCal.get(Calendar.MINUTE))

        val quickStep = RoutineStep(
            id = "qstep_${System.currentTimeMillis()}",
            title = title.ifBlank { "Quick Timer" },
            iconKey = iconKey,
            minMinutes = 1,
            maxMinutes = safeDuration,
            defaultMinutes = safeDuration,
            colorHex = "#F59E0B",
            isImportant = awardStars,
            starReward = if (awardStars) starReward.coerceAtLeast(1) else 0
        )
        val quickRoutine = Routine(
            id = "quick_timer_${System.currentTimeMillis()}",
            title = title.ifBlank { "Quick Timer" },
            targetDepartureTime = targetEndTime,
            startTime = startTime,
            starCount = if (awardStars) starReward.coerceAtLeast(1) else 0,
            repeatType = "Once",
            steps = listOf(quickStep)
        )
        repository.updateLocalRoutine(quickRoutine)
        repository.selectRoutine(quickRoutine.id)
        startRoutine(paceMode = "NOMINAL")
        _currentScreen.value = Screen.RunMode
    }

    fun createQuickRoutine(
        title: String,
        targetDepartureTime: String,
        selectedCatalogTasks: List<CatalogTask>,
        startNow: Boolean = true
    ) {
        val safeTasks = selectedCatalogTasks.ifEmpty { CatalogTask.defaultCatalog().take(3) }
        val steps = safeTasks.mapIndexed { idx, task ->
            task.toRoutineStep("qstep_${System.currentTimeMillis()}_$idx")
        }
        val cal = Calendar.getInstance()
        val startH = cal.get(Calendar.HOUR_OF_DAY)
        val startM = cal.get(Calendar.MINUTE)
        val startTime = String.format(Locale.getDefault(), "%02d:%02d", startH, startM)

        val totalMin = steps.sumOf { it.defaultMinutes }
        val resolvedTargetTime = if (targetDepartureTime.isNotBlank()) {
            targetDepartureTime
        } else {
            val endCal = Calendar.getInstance().apply { add(Calendar.MINUTE, totalMin) }
            String.format(Locale.getDefault(), "%02d:%02d", endCal.get(Calendar.HOUR_OF_DAY), endCal.get(Calendar.MINUTE))
        }

        val routine = Routine(
            id = "routine_quick_${System.currentTimeMillis()}",
            title = title.ifBlank { "Quick Routine" },
            targetDepartureTime = resolvedTargetTime,
            startTime = startTime,
            starCount = steps.filter { it.isImportant }.sumOf { it.starReward },
            repeatType = "Once",
            steps = steps
        )
        repository.updateLocalRoutine(routine)
        repository.selectRoutine(routine.id)
        if (startNow) {
            startRoutine(paceMode = "ELASTIC")
            _currentScreen.value = Screen.RunMode
        } else {
            _currentScreen.value = Screen.Dashboard
        }
    }

    fun stopRoutine() {
        timerJob?.cancel()
        timerJob = null
        repository.clearActiveSession()
        _currentScreen.value = Screen.Dashboard
    }

    fun pauseRoutine() {
        val session = activeSession.value ?: return
        val updated = session.copy(
            status = if (session.status == SessionStatus.RUNNING) SessionStatus.PAUSED else SessionStatus.RUNNING,
            stepStartEpochMillis = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        repository.updateActiveSession(updated, syncToCloud = true)
    }

    fun updateRoutine(routine: Routine) {
        repository.updateLocalRoutine(routine)
    }

    /**
     * Analyze past run execution logs to recommend increasing or decreasing individual step timers.
     * Designed to help the child require focus while preventing feelings of rush or anxiety.
     */
    fun getTimerRecommendations(routine: Routine): List<TimerRecommendation> {
        val history = repository.stepRunHistory.value
        return routine.steps.map { step ->
            val stepRuns = history.filter { it.stepId == step.id }
            if (stepRuns.isEmpty()) {
                TimerRecommendation(
                    stepId = step.id,
                    stepTitle = step.title,
                    currentMinutes = step.defaultMinutes,
                    recommendedMinutes = step.defaultMinutes,
                    deltaMinutes = 0,
                    avgActualMinutes = step.defaultMinutes.toFloat(),
                    reason = "✨ Great starting pace. Complete a few more runs to get personalized suggestions.",
                    type = RecommendationType.BALANCED
                )
            } else {
                val avgMinutes = stepRuns.map { it.actualSecondsTaken / 60f }.average().toFloat()
                val overtimeCount = stepRuns.count { it.wasOvertime }
                val overtimeRate = overtimeCount.toFloat() / stepRuns.size

                when {
                    overtimeRate >= 0.40f || avgMinutes > (step.defaultMinutes * 1.15f) -> {
                        val addMin = ((avgMinutes - step.defaultMinutes).toInt() + 1).coerceIn(1, 5)
                        val target = (step.defaultMinutes + addMin).coerceAtMost(step.maxMinutes + 5)
                        TimerRecommendation(
                            stepId = step.id,
                            stepTitle = step.title,
                            currentMinutes = step.defaultMinutes,
                            recommendedMinutes = target,
                            deltaMinutes = addMin,
                            avgActualMinutes = avgMinutes,
                            reason = "Took ~${String.format(Locale.getDefault(), "%.1f", avgMinutes)}m in past runs (late in $overtimeCount of ${stepRuns.size} runs). Increase +${addMin}m so she doesn't feel rushed.",
                            type = RecommendationType.INCREASE
                        )
                    }
                    avgMinutes < (step.defaultMinutes * 0.70f) && (step.defaultMinutes - avgMinutes >= 1.0f) -> {
                        val trimMin = ((step.defaultMinutes - avgMinutes).toInt()).coerceIn(1, 3)
                        val target = (step.defaultMinutes - trimMin).coerceAtLeast(step.minMinutes.coerceAtLeast(1))
                        TimerRecommendation(
                            stepId = step.id,
                            stepTitle = step.title,
                            currentMinutes = step.defaultMinutes,
                            recommendedMinutes = target,
                            deltaMinutes = -trimMin,
                            avgActualMinutes = avgMinutes,
                            reason = "Consistently finishes in ~${String.format(Locale.getDefault(), "%.1f", avgMinutes)}m. Trim -${trimMin}m to maintain gentle focus.",
                            type = RecommendationType.DECREASE
                        )
                    }
                    else -> {
                        TimerRecommendation(
                            stepId = step.id,
                            stepTitle = step.title,
                            currentMinutes = step.defaultMinutes,
                            recommendedMinutes = step.defaultMinutes,
                            deltaMinutes = 0,
                            avgActualMinutes = avgMinutes,
                            reason = "✨ Sweet spot! Averaging ~${String.format(Locale.getDefault(), "%.1f", avgMinutes)}m. Great balance of focus without feeling rushed.",
                            type = RecommendationType.BALANCED
                        )
                    }
                }
            }
        }
    }

    fun applyTimerRecommendation(routine: Routine, stepId: String, newMinutes: Int) {
        val updatedSteps = routine.steps.map {
            if (it.id == stepId) it.copy(defaultMinutes = newMinutes) else it
        }
        updateRoutine(routine.copy(steps = updatedSteps))
    }

    fun applyAllRecommendations(routine: Routine) {
        val recommendations = getTimerRecommendations(routine)
        val updatedSteps = routine.steps.map { step ->
            val rec = recommendations.find { it.stepId == step.id }
            if (rec != null && rec.recommendedMinutes != step.defaultMinutes) {
                step.copy(defaultMinutes = rec.recommendedMinutes)
            } else {
                step
            }
        }
        updateRoutine(routine.copy(steps = updatedSteps))
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        soundEffects.release()
    }
}
