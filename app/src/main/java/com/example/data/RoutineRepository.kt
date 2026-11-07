package com.example.data

import android.content.Context
import android.util.Log
import com.example.model.ActiveSession
import com.example.model.CatalogTask
import com.example.model.Routine
import com.example.model.RoutineStep
import com.example.model.SessionEvent
import com.example.model.SessionStatus
import com.example.model.StarPrize
import com.example.model.StepRunHistory
import com.example.model.TaskInterval
import com.example.util.AppLogger
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class RoutineRepository(
    context: Context,
    private val scope: CoroutineScope
) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = try {
        val dbId = context.getString(com.example.R.string.firestore_database_id)
        if (dbId.isNotEmpty() && dbId != "(default)") {
            FirebaseFirestore.getInstance(dbId)
        } else {
            FirebaseFirestore.getInstance()
        }
    } catch (_: Exception) {
        FirebaseFirestore.getInstance()
    }
    private val prefs = context.getSharedPreferences("launchpad_sync_prefs", Context.MODE_PRIVATE)

    val deviceId: String = prefs.getString("local_device_id", null) ?: java.util.UUID.randomUUID().toString().also {
        prefs.edit().putString("local_device_id", it).apply()
    }

    private val defaultList = loadCachedRoutines().ifEmpty { Routine.defaultRoutines() }
    private val _allRoutines = MutableStateFlow<List<Routine>>(defaultList)
    val allRoutines: StateFlow<List<Routine>> = _allRoutines.asStateFlow()

    private val _currentRoutine = MutableStateFlow<Routine>(defaultList.first())
    val currentRoutine: StateFlow<Routine> = _currentRoutine.asStateFlow()

    private val _activeSession = MutableStateFlow<ActiveSession?>(null)
    val activeSession: StateFlow<ActiveSession?> = _activeSession.asStateFlow()

    // Family Share / Household Code for spouse synchronization
    private val _familyCode = MutableStateFlow(prefs.getString("family_code", "") ?: "")
    val familyCode: StateFlow<String> = _familyCode.asStateFlow()

    // Star Bank & Prizes
    private val _earnedStars = MutableStateFlow(prefs.getInt("earned_stars", 12))
    val earnedStars: StateFlow<Int> = _earnedStars.asStateFlow()

    private val _starPrizes = MutableStateFlow<List<StarPrize>>(Routine.defaultPrizes())
    val starPrizes: StateFlow<List<StarPrize>> = _starPrizes.asStateFlow()

    private val _stepRunHistory = MutableStateFlow<List<StepRunHistory>>(defaultBaselineHistory())
    val stepRunHistory: StateFlow<List<StepRunHistory>> = _stepRunHistory.asStateFlow()

    fun recordStepRun(
        routineId: String,
        stepId: String,
        stepTitle: String,
        scheduledMinutes: Int,
        actualSecondsTaken: Int,
        wasOvertime: Boolean,
        wasAssisted: Boolean = false,
        catalogTaskId: String? = null
    ) {
        val entry = StepRunHistory(
            id = "run_${System.currentTimeMillis()}",
            routineId = routineId,
            stepId = stepId,
            stepTitle = stepTitle,
            catalogTaskId = catalogTaskId,
            scheduledMinutes = scheduledMinutes,
            actualSecondsTaken = actualSecondsTaken,
            wasOvertime = wasOvertime,
            wasAssisted = wasAssisted,
            timestamp = System.currentTimeMillis()
        )
        _stepRunHistory.value = _stepRunHistory.value + entry
        syncFamilyDataToCloud()
    }

    private fun defaultBaselineHistory(): List<StepRunHistory> = listOf(
        // Bedtime routine steps history
        StepRunHistory("h1", "routine_bedtime", "bed_bath", "Take a bath", "cat_bath", 15, 14 * 60, false),
        StepRunHistory("h2", "routine_bedtime", "bed_bath", "Take a bath", "cat_bath", 15, 16 * 60, true),
        StepRunHistory("h3", "routine_bedtime", "bed_pajamas", "Put on pajamas", "cat_pajamas", 6, 8 * 60 + 30, true),
        StepRunHistory("h4", "routine_bedtime", "bed_pajamas", "Put on pajamas", "cat_pajamas", 6, 8 * 60, true),
        StepRunHistory("h5", "routine_bedtime", "bed_teeth", "Brush teeth", "cat_teeth", 3, 2 * 60, false),
        StepRunHistory("h6", "routine_bedtime", "bed_teeth", "Brush teeth", "cat_teeth", 3, 1 * 60 + 50, false),
        StepRunHistory("h7", "routine_bedtime", "bed_books", "Pick bedtime books", "cat_story", 5, 4 * 60 + 40, false),
        StepRunHistory("h8", "routine_bedtime", "bed_story", "Story time", "cat_story", 15, 15 * 60, false),
        StepRunHistory("h9", "routine_bedtime", "bed_sleep", "Lights out & sleep", "cat_sleep", 10, 10 * 60, false),
        // Morning routine steps history
        StepRunHistory("h10", "routine_morning", "step_wake_up", "Wake up", null, 5, 4 * 60, false),
        StepRunHistory("h11", "routine_morning", "step_dressed", "Get dressed", "cat_clothes", 10, 13 * 60, true),
        StepRunHistory("h12", "routine_morning", "step_breakfast", "Breakfast", "cat_breakfast", 15, 14 * 60, false),
        StepRunHistory("h13", "routine_morning", "step_teeth", "Brush teeth", "cat_teeth", 3, 2 * 60, false),
        StepRunHistory("h14", "routine_morning", "step_shoes", "Shoes and Coat", "cat_shoes", 7, 6 * 60 + 30, false)
    )

    fun addStars(count: Int) {
        val newTotal = (_earnedStars.value + count).coerceAtLeast(0)
        _earnedStars.value = newTotal
        prefs.edit().putInt("earned_stars", newTotal).apply()
        syncFamilyDataToCloud()
    }

    fun redeemPrize(prizeId: String) {
        val prize = _starPrizes.value.find { it.id == prizeId } ?: return
        if (_earnedStars.value >= prize.starsNeeded) {
            val newStars = _earnedStars.value - prize.starsNeeded
            _earnedStars.value = newStars
            prefs.edit().putInt("earned_stars", newStars).apply()
            _starPrizes.value = _starPrizes.value.map {
                if (it.id == prizeId) it.copy(isRedeemed = true, redeemedDate = "Today") else it
            }
            syncFamilyDataToCloud()
        }
    }

    fun addCustomPrize(title: String, stars: Int, type: String, emoji: String) {
        val newPrize = StarPrize(
            id = "prize_${System.currentTimeMillis()}",
            title = title,
            starsNeeded = stars,
            type = type,
            iconEmoji = emoji
        )
        _starPrizes.value = _starPrizes.value + newPrize
        syncFamilyDataToCloud()
    }

    // Reusable Task Catalog (Persisted locally in SharedPreferences + synced to Firestore)
    private val _taskCatalog = MutableStateFlow<List<CatalogTask>>(loadCachedCatalog().ifEmpty { CatalogTask.defaultCatalog() })
    val taskCatalog: StateFlow<List<CatalogTask>> = _taskCatalog.asStateFlow()

    private var sessionListener: ListenerRegistration? = null
    private var routinesCollectionListener: ListenerRegistration? = null
    private var familyDataListener: ListenerRegistration? = null
    private var catalogListener: ListenerRegistration? = null
    private val syncLock = Any()
    private var activeSyncFamily: String? = null

    init {
        // Auto-recover any tasks from routines into catalog on startup so no tasks are ever lost
        recoverCatalogFromRoutines(defaultList)

        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                scope.launch(Dispatchers.IO) {
                    try {
                        val userDoc = firestore.collection("users").document(user.uid).get().await()
                        val cloudFamily = userDoc.getString("familyId")
                        if (!cloudFamily.isNullOrBlank()) {
                            _familyCode.value = cloudFamily
                            prefs.edit().putString("family_code", cloudFamily).apply()
                        } else {
                            val code = _familyCode.value.ifEmpty { "FAM-${user.uid.take(4).uppercase()}" }
                            _familyCode.value = code
                            prefs.edit().putString("family_code", code).apply()
                            firestore.collection("users").document(user.uid)
                                .set(mapOf("familyId" to code, "email" to (user.email ?: "")), SetOptions.merge())
                                .await()
                        }
                    } catch (e: Exception) {
                        Log.w("RoutineRepository", "Error syncing user profile", e)
                        if (_familyCode.value.isEmpty()) {
                            val defaultCode = "FAM-${user.uid.take(4).uppercase()}"
                            _familyCode.value = defaultCode
                            prefs.edit().putString("family_code", defaultCode).apply()
                        }
                    }
                    startSync()
                }
            } else {
                stopSync()
            }
        }
    }

    fun joinFamilyCode(code: String) {
        val cleanCode = code.trim().uppercase()
        if (cleanCode.isNotEmpty()) {
            _familyCode.value = cleanCode
            prefs.edit().putString("family_code", cleanCode).apply()
            val user = auth.currentUser
            if (user != null) {
                scope.launch(Dispatchers.IO) {
                    try {
                        firestore.collection("users").document(user.uid)
                            .set(mapOf("familyId" to cleanCode), SetOptions.merge())
                            .await()
                    } catch (e: Exception) {
                        Log.w("RoutineRepository", "Error updating user familyId", e)
                    }
                }
            }
            startSync()
        }
    }

    fun forceSync() {
        startSync()
    }

    private fun getActiveSessionDocId(routineId: String): String {
        val fam = _familyCode.value.ifEmpty { auth.currentUser?.uid ?: "local" }
        return "session_$fam"
    }

    private fun startSync() {
        val user = auth.currentUser ?: return
        val currentFam = _familyCode.value.ifEmpty { "FAM-${user.uid.take(4).uppercase()}" }

        synchronized(syncLock) {
            if (activeSyncFamily == currentFam && sessionListener != null && routinesCollectionListener != null) {
                // Already actively synced to this family space
                return
            }
            stopSync()
            activeSyncFamily = currentFam

            // 1. Listen to active session for this family (instant multi-device sync)
            val sessionDocId = getActiveSessionDocId(_currentRoutine.value.id)
            sessionListener = firestore.collection("active_sessions")
            .document(sessionDocId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    AppLogger.w("RoutineRepository", "Listen failed for active_sessions", error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    parseActiveSession(snapshot)?.let { rawRemoteSession ->
                        // Anchor updatedAt to THIS device's clock at reception to neutralize inter-device clock skew
                        val remoteSession = rawRemoteSession.copy(updatedAt = System.currentTimeMillis())
                        AppLogger.i("SessionSync", "Active session snapshot: routine=${remoteSession.routineId}, task=${remoteSession.currentStepId}, rem=${remoteSession.stepRemainingSeconds}s, bonus=${remoteSession.bonusTimeSeconds}s, controller=${remoteSession.controllerDeviceId}")
                        _activeSession.value = remoteSession
                        // If running session is for a different routine, not loaded, or step IDs differ, ensure matching routine is active
                        if (remoteSession.status == SessionStatus.RUNNING || remoteSession.status == SessionStatus.PAUSED) {
                            val match = _allRoutines.value.find { it.id == remoteSession.routineId }
                            val hasCurrentStepId = remoteSession.currentStepId.isEmpty() || (match != null && match.steps.any { it.id == remoteSession.currentStepId }) || remoteSession.bonusSteps.any { it.id == remoteSession.currentStepId }

                            if (match != null && hasCurrentStepId) {
                                if (match.id != _currentRoutine.value.id) {
                                    _currentRoutine.value = match
                                }
                            } else {
                                // Routine document in _allRoutines is missing or stale: fetch directly
                                firestore.collection("routines").document(remoteSession.routineId).get().addOnSuccessListener { rDoc ->
                                    if (rDoc.exists()) {
                                        parseRoutine(rDoc)?.let { fetchedRoutine ->
                                            AppLogger.i("SessionSync", "Fetched routine ${fetchedRoutine.title} with ${fetchedRoutine.steps.size} steps for active session")
                                            val updated = _allRoutines.value.filter { it.id != fetchedRoutine.id } + fetchedRoutine
                                            _allRoutines.value = updated
                                            _currentRoutine.value = fetchedRoutine
                                            saveCachedRoutines(updated)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (snapshot != null && !snapshot.exists()) {
                    AppLogger.i("SessionSync", "Active session ended remotely")
                    _activeSession.value = null
                }
            }

        // 2. Listen to all routines shared with this family
        routinesCollectionListener = firestore.collection("routines")
            .whereEqualTo("familyId", currentFam)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    AppLogger.w("RoutineRepository", "Listen failed for routines", error)
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.isEmpty) {
                    val list = snapshot.documents.mapNotNull { parseRoutine(it) }
                    if (list.isNotEmpty()) {
                        _allRoutines.value = list
                        saveCachedRoutines(list)
                        recoverCatalogFromRoutines(list)
                        val currentId = _currentRoutine.value.id
                        _currentRoutine.value = list.find { it.id == currentId } ?: list.first()
                        AppLogger.d("RoutineRepository", "Routines synced from cloud (${list.size} routines)")
                    }
                } else {
                    // Upload defaults to this family space so spouse can see them immediately
                    uploadInitialRoutines(currentFam, user.uid)
                }
            }

        // 3. Listen to Family Data (Earned Stars, Star Prizes, Step Run History)
        familyDataListener = firestore.collection("families")
            .document(currentFam)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("RoutineRepository", "Listen failed for family data", error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val stars = snapshot.getLong("earnedStars")?.toInt()
                    if (stars != null) {
                        _earnedStars.value = stars
                        prefs.edit().putInt("earned_stars", stars).apply()
                    }
                    val rawPrizes = snapshot.get("starPrizes") as? List<Map<String, Any>>
                    if (rawPrizes != null) {
                        val parsedPrizes = rawPrizes.mapNotNull { parseStarPrize(it) }
                        if (parsedPrizes.isNotEmpty()) {
                            _starPrizes.value = parsedPrizes
                        }
                    }
                    val rawHistory = snapshot.get("stepRunHistory") as? List<Map<String, Any>>
                    if (rawHistory != null) {
                        val parsedHistory = rawHistory.mapNotNull { parseHistoryEntry(it) }
                        if (parsedHistory.isNotEmpty()) {
                            _stepRunHistory.value = parsedHistory
                        }
                    }
                } else if (snapshot != null && !snapshot.exists()) {
                    uploadInitialFamilyData(currentFam)
                }
            }

        // 4. Listen to Shared Task Catalog
        catalogListener = firestore.collection("task_catalog")
            .whereEqualTo("familyId", currentFam)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("RoutineRepository", "Listen failed for task_catalog", error)
                    return@addSnapshotListener
                }
                if (snapshot != null && !snapshot.isEmpty) {
                    val remoteTasks = snapshot.documents.mapNotNull { parseCatalogTask(it) }
                    if (remoteTasks.isNotEmpty()) {
                        // Safe merge: keep default catalog, any locally created/recovered tasks, and remote tasks
                        val currentLocal = _taskCatalog.value
                        val merged = (CatalogTask.defaultCatalog() + currentLocal + remoteTasks)
                            .distinctBy { it.id }
                        _taskCatalog.value = merged
                        saveCachedCatalog(merged)
                    }
                } else {
                    uploadInitialCatalog(currentFam, user.uid)
                }
            }
        }
    }

    private fun uploadInitialRoutines(famId: String, userId: String) {
        scope.launch(Dispatchers.IO) {
            try {
                for (r in _allRoutines.value) {
                    val updated = r.copy(familyId = famId, userId = userId)
                    firestore.collection("routines")
                        .document(r.id)
                        .set(routineToMap(updated, userId, famId), SetOptions.merge())
                        .await()
                }
            } catch (e: Exception) {
                Log.w("RoutineRepository", "Error uploading initial routines", e)
            }
        }
    }

    private fun stopSync() {
        synchronized(syncLock) {
            activeSyncFamily = null
            sessionListener?.remove()
            sessionListener = null
            routinesCollectionListener?.remove()
            routinesCollectionListener = null
            familyDataListener?.remove()
            familyDataListener = null
            catalogListener?.remove()
            catalogListener = null
        }
    }

    fun selectRoutine(id: String) {
        val found = _allRoutines.value.find { it.id == id }
        if (found != null) {
            _currentRoutine.value = found
        }
    }

    fun createRoutine(
        title: String,
        startTime: String,
        targetDepartureTime: String,
        repeatType: String = "Everyday"
    ): Routine {
        val user = auth.currentUser
        val fam = _familyCode.value
        val newId = "routine_${System.currentTimeMillis()}"
        val newRoutine = Routine(
            id = newId,
            userId = user?.uid ?: "",
            familyId = fam,
            title = title.ifEmpty { "New Routine" },
            startTime = startTime.ifEmpty { "07:00" },
            targetDepartureTime = targetDepartureTime.ifEmpty { "08:00" },
            repeatType = repeatType.ifEmpty { "Everyday" },
            steps = listOf(
                RoutineStep(
                    id = "step_${System.currentTimeMillis()}_1",
                    title = "First Activity",
                    iconKey = "wake_up",
                    minMinutes = 5,
                    maxMinutes = 15,
                    defaultMinutes = 10,
                    colorHex = "#FACC15"
                ),
                RoutineStep(
                    id = "step_${System.currentTimeMillis()}_2",
                    title = "Finish & Launch",
                    iconKey = "leave_home",
                    minMinutes = 5,
                    maxMinutes = 15,
                    defaultMinutes = 10,
                    colorHex = "#38BDF8"
                )
            )
        )
        val updatedList = _allRoutines.value + newRoutine
        _allRoutines.value = updatedList
        _currentRoutine.value = newRoutine
        syncRoutineToCloud(newRoutine)
        return newRoutine
    }

    fun deleteRoutine(id: String) {
        val updated = _allRoutines.value.filter { it.id != id }
        _allRoutines.value = if (updated.isEmpty()) Routine.defaultRoutines() else updated
        _currentRoutine.value = _allRoutines.value.first()
        val user = auth.currentUser
        if (user != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    firestore.collection("routines").document(id).delete().await()
                } catch (e: Exception) {
                    Log.w("RoutineRepository", "Error deleting routine from firestore", e)
                }
            }
        }
    }

    fun updateLocalRoutine(routine: Routine) {
        val withFamily = routine.copy(
            familyId = _familyCode.value.ifEmpty { routine.familyId },
            userId = auth.currentUser?.uid ?: routine.userId
        )
        _currentRoutine.value = withFamily
        val exists = _allRoutines.value.any { it.id == withFamily.id }
        val updatedList = if (exists) {
            _allRoutines.value.map { if (it.id == withFamily.id) withFamily else it }
        } else {
            _allRoutines.value + withFamily
        }
        _allRoutines.value = updatedList
        saveCachedRoutines(updatedList)
        syncRoutineToCloud(withFamily)
    }

    private fun syncRoutineToCloud(routine: Routine) {
        val user = auth.currentUser
        if (user != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val fam = _familyCode.value.ifEmpty { "FAM-${user.uid.take(4).uppercase()}" }
                    val routineMap = routineToMap(routine, user.uid, fam)
                    firestore.collection("routines")
                        .document(routine.id)
                        .set(routineMap, SetOptions.merge())
                        .await()
                    AppLogger.d("RoutineRepository", "Routine ${routine.title} synced to cloud")
                } catch (e: Exception) {
                    AppLogger.w("RoutineRepository", "Error syncing routine to Firestore", e)
                }
            }
        }
    }

    fun recordSessionEvent(
        eventType: String,
        session: ActiveSession,
        stepId: String = session.currentStepId,
        stepTitle: String = "",
        allocatedSec: Int = 0,
        remainingSec: Int = session.stepRemainingSeconds,
        savedBonusSec: Int = 0,
        isAssisted: Boolean = false,
        paceMode: String = session.paceMode
    ) {
        val event = SessionEvent(
            eventId = "evt_${System.currentTimeMillis()}_${(1000..9999).random()}",
            eventType = eventType,
            stepId = stepId,
            stepTitle = stepTitle,
            stepIndex = session.currentStepIndex,
            deviceId = deviceId,
            timestamp = System.currentTimeMillis(),
            allocatedDurationSeconds = if (allocatedSec > 0) allocatedSec else (session.stepDurationsSecondsMap[stepId] ?: session.stepTotalSeconds),
            remainingSeconds = remainingSec,
            savedBonusSeconds = savedBonusSec,
            bonusTimeSeconds = session.bonusTimeSeconds,
            isAssisted = isAssisted,
            paceMode = paceMode
        )
        val updated = session.copy(
            eventLog = (session.eventLog + event).takeLast(50),
            updatedAt = System.currentTimeMillis()
        )
        updateActiveSession(updated, syncToCloud = true)
    }

    fun updateActiveSession(session: ActiveSession, syncToCloud: Boolean = true) {
        val fam = _familyCode.value.ifEmpty { "FAM-${auth.currentUser?.uid?.take(4)?.uppercase() ?: "local"}" }
        val activeSteps = _currentRoutine.value.activeSteps
        val resolvedStepId = if (session.currentStepId.isNotEmpty()) {
            session.currentStepId
        } else {
            activeSteps.getOrNull(session.currentStepIndex)?.id ?: ""
        }
        val withFamily = session.copy(
            familyId = fam,
            currentStepId = resolvedStepId,
            controllerDeviceId = deviceId
        )
        _activeSession.value = withFamily
        if (!syncToCloud) return

        val user = auth.currentUser
        if (user != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val sessionDocId = getActiveSessionDocId(session.routineId)
                    val sessionMap = sessionToMap(withFamily, user.uid, fam)
                    firestore.collection("active_sessions")
                        .document(sessionDocId)
                        .set(sessionMap, SetOptions.merge())
                        .await()
                    AppLogger.d("SessionSync", "Atomic event write: task=${resolvedStepId}, rem=${withFamily.stepRemainingSeconds}s, bonus=${withFamily.bonusTimeSeconds}s, authorDevice=$deviceId")
                } catch (e: Exception) {
                    AppLogger.w("RoutineRepository", "Error syncing session to Firestore", e)
                }
            }
        }
    }

    fun clearActiveSession() {
        val routineId = _currentRoutine.value.id
        val sessionDocId = getActiveSessionDocId(routineId)
        AppLogger.i("SessionSync", "Clearing active session for routine $routineId (docId: $sessionDocId)")
        _activeSession.value = null
        val user = auth.currentUser
        if (user != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    firestore.collection("active_sessions")
                        .document(sessionDocId)
                        .delete()
                        .await()
                } catch (e: Exception) {
                    AppLogger.w("RoutineRepository", "Error deleting active session", e)
                }
            }
        }
    }

    private fun routineToMap(routine: Routine, userId: String, famId: String): Map<String, Any?> {
        val stepsMap = routine.steps.map { step ->
            mapOf(
                "id" to step.id,
                "catalogTaskId" to step.catalogTaskId,
                "title" to step.title,
                "iconKey" to step.iconKey,
                "minMinutes" to step.minMinutes,
                "maxMinutes" to step.maxMinutes,
                "defaultMinutes" to step.defaultMinutes,
                "colorHex" to step.colorHex,
                "drawableResName" to step.drawableResName,
                "isImportant" to step.isImportant,
                "starReward" to step.starReward,
                "isEnabled" to step.isEnabled
            )
        }
        return mapOf(
            "id" to routine.id,
            "userId" to userId,
            "familyId" to famId,
            "title" to routine.title,
            "targetDepartureTime" to routine.targetDepartureTime,
            "startTime" to routine.startTime,
            "starCount" to routine.starCount,
            "repeatType" to routine.repeatType,
            "voiceName" to routine.voiceName,
            "reminderEnabled" to routine.reminderEnabled,
            "lastCompletedDate" to routine.lastCompletedDate,
            "steps" to stepsMap
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseRoutine(doc: DocumentSnapshot): Routine? {
        return try {
            val title = doc.getString("title") ?: "Routine"
            val targetTime = doc.getString("targetDepartureTime") ?: "07:10"
            val startTime = doc.getString("startTime") ?: "06:20"
            val famId = doc.getString("familyId") ?: ""
            val starCount = doc.getLong("starCount")?.toInt() ?: 0
            val repeatType = doc.getString("repeatType") ?: "Everyday"
            val voiceName = doc.getString("voiceName") ?: "Luma"
            val reminder = doc.getBoolean("reminderEnabled") ?: true
            val lastCompleted = doc.getString("lastCompletedDate")

            val rawSteps = doc.get("steps") as? List<Map<String, Any>>
            val steps = rawSteps?.map { map ->
                RoutineStep(
                    id = map["id"] as? String ?: "",
                    catalogTaskId = map["catalogTaskId"] as? String,
                    title = map["title"] as? String ?: "",
                    iconKey = map["iconKey"] as? String ?: "clothes",
                    minMinutes = (map["minMinutes"] as? Long)?.toInt() ?: 5,
                    maxMinutes = (map["maxMinutes"] as? Long)?.toInt() ?: 15,
                    defaultMinutes = (map["defaultMinutes"] as? Long)?.toInt() ?: 10,
                    colorHex = map["colorHex"] as? String ?: "#4ADE80",
                    drawableResName = map["drawableResName"] as? String,
                    isImportant = map["isImportant"] as? Boolean ?: false,
                    starReward = (map["starReward"] as? Long)?.toInt() ?: 1,
                    isEnabled = map["isEnabled"] as? Boolean ?: true
                )
            } ?: Routine.defaultMorningSteps()

            Routine(
                id = doc.id,
                userId = doc.getString("userId") ?: "",
                familyId = famId,
                title = title,
                targetDepartureTime = targetTime,
                startTime = startTime,
                starCount = starCount,
                repeatType = repeatType,
                voiceName = voiceName,
                reminderEnabled = reminder,
                lastCompletedDate = lastCompleted,
                steps = steps
            )
        } catch (e: Exception) {
            Log.e("RoutineRepository", "Error parsing routine doc", e)
            null
        }
    }

    private fun sessionToMap(session: ActiveSession, userId: String, famId: String): Map<String, Any?> {
        val intervalsMap = session.taskIntervals.map { interval ->
            mapOf(
                "stepId" to interval.stepId,
                "enterEpochMillis" to interval.enterEpochMillis,
                "exitEpochMillis" to interval.exitEpochMillis,
                "allocatedDurationSeconds" to interval.allocatedDurationSeconds
            )
        }
        val eventsMap = session.eventLog.takeLast(40).map { evt ->
            mapOf(
                "eventId" to evt.eventId,
                "eventType" to evt.eventType,
                "stepId" to evt.stepId,
                "stepTitle" to evt.stepTitle,
                "stepIndex" to evt.stepIndex,
                "deviceId" to evt.deviceId,
                "timestamp" to evt.timestamp,
                "allocatedDurationSeconds" to evt.allocatedDurationSeconds,
                "remainingSeconds" to evt.remainingSeconds,
                "savedBonusSeconds" to evt.savedBonusSeconds,
                "bonusTimeSeconds" to evt.bonusTimeSeconds,
                "isAssisted" to evt.isAssisted,
                "paceMode" to evt.paceMode
            )
        }
        val bonusStepsMap = session.bonusSteps.map { step ->
            mapOf(
                "id" to step.id,
                "catalogTaskId" to step.catalogTaskId,
                "title" to step.title,
                "iconKey" to step.iconKey,
                "minMinutes" to step.minMinutes,
                "maxMinutes" to step.maxMinutes,
                "defaultMinutes" to step.defaultMinutes,
                "colorHex" to step.colorHex,
                "drawableResName" to step.drawableResName,
                "isImportant" to step.isImportant,
                "starReward" to step.starReward,
                "isEnabled" to step.isEnabled
            )
        }
        return mapOf(
            "sessionId" to session.sessionId,
            "routineId" to session.routineId,
            "routineTitle" to session.routineTitle,
            "currentStepId" to (session.currentStepId.ifEmpty { _currentRoutine.value.activeSteps.getOrNull(session.currentStepIndex)?.id ?: "" }),
            "userId" to userId,
            "familyId" to famId,
            "status" to session.status.name,
            "currentStepIndex" to session.currentStepIndex,
            "stepRemainingSeconds" to session.stepRemainingSeconds,
            "stepTotalSeconds" to session.stepTotalSeconds,
            "bonusTimeSeconds" to session.bonusTimeSeconds,
            "totalElapsedSeconds" to session.totalElapsedSeconds,
            "completedStepIds" to session.completedStepIds,
            "assistedStepIds" to session.assistedStepIds,
            "skippedStepIds" to session.skippedStepIds,
            "orderedStepIds" to session.orderedStepIds,
            "bonusSteps" to bonusStepsMap,
            "stepDurationsSeconds" to session.stepDurationsSeconds,
            "stepDurationsSecondsMap" to session.stepDurationsSecondsMap,
            "stepRemainingSecondsMap" to session.stepRemainingSecondsMap,
            "stepBankedSecondsMap" to session.stepBankedSecondsMap,
            "targetEndTime" to session.targetEndTime,
            "startedAtWallTime" to session.startedAtWallTime,
            "initialAvailableMinutes" to session.initialAvailableMinutes,
            "startEpochMillis" to session.startEpochMillis,
            "stepStartEpochMillis" to session.stepStartEpochMillis,
            "paceMode" to session.paceMode,
            "controllerDeviceId" to session.controllerDeviceId,
            "taskIntervals" to intervalsMap,
            "eventLog" to eventsMap,
            "updatedAt" to session.updatedAt
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseActiveSession(doc: DocumentSnapshot): ActiveSession? {
        return try {
            val statusStr = doc.getString("status") ?: SessionStatus.RUNNING.name
            val status = try { SessionStatus.valueOf(statusStr) } catch (_: Exception) { SessionStatus.RUNNING }
            val currentStepId = doc.getString("currentStepId") ?: ""
            val durations = (doc.get("stepDurationsSeconds") as? List<Long>)?.map { it.toInt() } ?: emptyList()
            val completed = (doc.get("completedStepIds") as? List<String>) ?: emptyList()
            val assisted = (doc.get("assistedStepIds") as? List<String>) ?: emptyList()
            val skipped = (doc.get("skippedStepIds") as? List<String>) ?: emptyList()
            val orderedIds = (doc.get("orderedStepIds") as? List<String>) ?: emptyList()

            val rawDurationsMap = doc.get("stepDurationsSecondsMap") as? Map<String, Any>
            val durationsMap = rawDurationsMap?.mapNotNull { (k, v) ->
                val sec = (v as? Long)?.toInt() ?: (v as? Number)?.toInt()
                if (sec != null) k to sec else null
            }?.toMap() ?: emptyMap()

            val rawMap = doc.get("stepRemainingSecondsMap") as? Map<String, Any>
            val remainingMap = rawMap?.mapNotNull { (k, v) ->
                val sec = (v as? Long)?.toInt() ?: (v as? Number)?.toInt()
                if (sec != null) k to sec else null
            }?.toMap() ?: emptyMap()
            val rawBankedMap = doc.get("stepBankedSecondsMap") as? Map<String, Any>
            val bankedMap = rawBankedMap?.mapNotNull { (k, v) ->
                val sec = (v as? Long)?.toInt() ?: (v as? Number)?.toInt()
                if (sec != null) k to sec else null
            }?.toMap() ?: emptyMap()

            val rawIntervals = doc.get("taskIntervals") as? List<Map<String, Any>>
            val intervals = rawIntervals?.mapNotNull { map ->
                val stepId = map["stepId"] as? String ?: return@mapNotNull null
                val enter = (map["enterEpochMillis"] as? Long) ?: (map["enterEpochMillis"] as? Number)?.toLong() ?: 0L
                val exit = (map["exitEpochMillis"] as? Long) ?: (map["exitEpochMillis"] as? Number)?.toLong()
                val allocated = (map["allocatedDurationSeconds"] as? Long)?.toInt() ?: (map["allocatedDurationSeconds"] as? Number)?.toInt() ?: 0
                TaskInterval(
                    stepId = stepId,
                    enterEpochMillis = enter,
                    exitEpochMillis = exit,
                    allocatedDurationSeconds = allocated
                )
            } ?: emptyList()

            val rawEvents = doc.get("eventLog") as? List<Map<String, Any>>
            val events = rawEvents?.mapNotNull { map ->
                val eventId = map["eventId"] as? String ?: return@mapNotNull null
                SessionEvent(
                    eventId = eventId,
                    eventType = map["eventType"] as? String ?: "",
                    stepId = map["stepId"] as? String ?: "",
                    stepTitle = map["stepTitle"] as? String ?: "",
                    stepIndex = (map["stepIndex"] as? Long)?.toInt() ?: 0,
                    deviceId = map["deviceId"] as? String ?: "",
                    timestamp = (map["timestamp"] as? Long) ?: System.currentTimeMillis(),
                    allocatedDurationSeconds = (map["allocatedDurationSeconds"] as? Long)?.toInt() ?: (map["allocatedDurationSeconds"] as? Number)?.toInt() ?: 0,
                    remainingSeconds = (map["remainingSeconds"] as? Long)?.toInt() ?: 0,
                    savedBonusSeconds = (map["savedBonusSeconds"] as? Long)?.toInt() ?: (map["savedBonusSeconds"] as? Number)?.toInt() ?: 0,
                    bonusTimeSeconds = (map["bonusTimeSeconds"] as? Long)?.toInt() ?: 0,
                    isAssisted = (map["isAssisted"] as? Boolean) ?: false,
                    paceMode = map["paceMode"] as? String ?: ""
                )
            } ?: emptyList()

            val rawBonusSteps = doc.get("bonusSteps") as? List<Map<String, Any>>
            val bonusSteps = rawBonusSteps?.mapNotNull { map ->
                val id = map["id"] as? String ?: return@mapNotNull null
                RoutineStep(
                    id = id,
                    catalogTaskId = map["catalogTaskId"] as? String,
                    title = map["title"] as? String ?: "",
                    iconKey = map["iconKey"] as? String ?: "cuddle",
                    minMinutes = (map["minMinutes"] as? Long)?.toInt() ?: (map["minMinutes"] as? Number)?.toInt() ?: 1,
                    maxMinutes = (map["maxMinutes"] as? Long)?.toInt() ?: (map["maxMinutes"] as? Number)?.toInt() ?: 5,
                    defaultMinutes = (map["defaultMinutes"] as? Long)?.toInt() ?: (map["defaultMinutes"] as? Number)?.toInt() ?: 2,
                    colorHex = map["colorHex"] as? String ?: "#EC4899",
                    drawableResName = map["drawableResName"] as? String,
                    isImportant = map["isImportant"] as? Boolean ?: false,
                    starReward = (map["starReward"] as? Long)?.toInt() ?: (map["starReward"] as? Number)?.toInt() ?: 0,
                    isEnabled = map["isEnabled"] as? Boolean ?: true
                )
            } ?: emptyList()

            ActiveSession(
                sessionId = doc.getString("sessionId") ?: doc.id,
                routineId = doc.getString("routineId") ?: "routine_morning",
                routineTitle = doc.getString("routineTitle") ?: "Morning Routine",
                currentStepId = currentStepId,
                userId = doc.getString("userId") ?: "",
                familyId = doc.getString("familyId") ?: "",
                status = status,
                currentStepIndex = doc.getLong("currentStepIndex")?.toInt() ?: 0,
                stepRemainingSeconds = doc.getLong("stepRemainingSeconds")?.toInt() ?: 0,
                stepTotalSeconds = doc.getLong("stepTotalSeconds")?.toInt() ?: 0,
                bonusTimeSeconds = doc.getLong("bonusTimeSeconds")?.toInt() ?: 0,
                totalElapsedSeconds = doc.getLong("totalElapsedSeconds")?.toInt() ?: 0,
                completedStepIds = completed,
                assistedStepIds = assisted,
                skippedStepIds = skipped,
                stepDurationsSeconds = durations,
                stepDurationsSecondsMap = durationsMap,
                stepRemainingSecondsMap = remainingMap,
                stepBankedSecondsMap = bankedMap,
                orderedStepIds = orderedIds,
                bonusSteps = bonusSteps,
                targetEndTime = doc.getString("targetEndTime") ?: "",
                startedAtWallTime = doc.getString("startedAtWallTime") ?: "",
                initialAvailableMinutes = doc.getLong("initialAvailableMinutes")?.toInt() ?: 0,
                startEpochMillis = doc.getLong("startEpochMillis") ?: System.currentTimeMillis(),
                stepStartEpochMillis = doc.getLong("stepStartEpochMillis") ?: System.currentTimeMillis(),
                paceMode = doc.getString("paceMode") ?: "ELASTIC",
                controllerDeviceId = doc.getString("controllerDeviceId") ?: "",
                taskIntervals = intervals,
                eventLog = events,
                updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.e("RoutineRepository", "Error parsing active session doc", e)
            null
        }
    }

    private fun saveCachedRoutines(routines: List<Routine>) {
        try {
            val arr = org.json.JSONArray()
            for (r in routines) {
                val obj = org.json.JSONObject()
                obj.put("id", r.id)
                obj.put("userId", r.userId)
                obj.put("familyId", r.familyId)
                obj.put("title", r.title)
                obj.put("targetDepartureTime", r.targetDepartureTime)
                obj.put("startTime", r.startTime)
                obj.put("starCount", r.starCount)
                obj.put("repeatType", r.repeatType)
                obj.put("voiceName", r.voiceName)
                obj.put("reminderEnabled", r.reminderEnabled)
                obj.put("lastCompletedDate", r.lastCompletedDate)
                val stepsArr = org.json.JSONArray()
                for (s in r.steps) {
                    val sObj = org.json.JSONObject()
                    sObj.put("id", s.id)
                    sObj.put("catalogTaskId", s.catalogTaskId ?: "")
                    sObj.put("title", s.title)
                    sObj.put("iconKey", s.iconKey)
                    sObj.put("minMinutes", s.minMinutes)
                    sObj.put("maxMinutes", s.maxMinutes)
                    sObj.put("defaultMinutes", s.defaultMinutes)
                    sObj.put("colorHex", s.colorHex)
                    sObj.put("isImportant", s.isImportant)
                    sObj.put("starReward", s.starReward)
                    sObj.put("isEnabled", s.isEnabled)
                    stepsArr.put(sObj)
                }
                obj.put("steps", stepsArr)
                arr.put(obj)
            }
            prefs.edit().putString("cached_routines_json", arr.toString()).apply()
        } catch (e: Exception) {
            Log.w("RoutineRepository", "Error caching routines", e)
        }
    }

    private fun loadCachedRoutines(): List<Routine> {
        val raw = prefs.getString("cached_routines_json", null) ?: return emptyList()
        return try {
            val arr = org.json.JSONArray(raw)
            val list = mutableListOf<Routine>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val stepsArr = obj.optJSONArray("steps")
                val steps = mutableListOf<RoutineStep>()
                if (stepsArr != null) {
                    for (j in 0 until stepsArr.length()) {
                        val sObj = stepsArr.getJSONObject(j)
                        val catId = sObj.optString("catalogTaskId").ifEmpty { null }
                        steps.add(
                            RoutineStep(
                                id = sObj.getString("id"),
                                catalogTaskId = catId,
                                title = sObj.optString("title", ""),
                                iconKey = sObj.optString("iconKey", "clothes"),
                                minMinutes = sObj.optInt("minMinutes", 5),
                                maxMinutes = sObj.optInt("maxMinutes", 15),
                                defaultMinutes = sObj.optInt("defaultMinutes", 10),
                                colorHex = sObj.optString("colorHex", "#4ADE80"),
                                isImportant = sObj.optBoolean("isImportant", false),
                                starReward = sObj.optInt("starReward", 1),
                                isEnabled = sObj.optBoolean("isEnabled", true)
                            )
                        )
                    }
                }
                list.add(
                    Routine(
                        id = obj.getString("id"),
                        userId = obj.optString("userId", ""),
                        familyId = obj.optString("familyId", ""),
                        title = obj.optString("title", "Routine"),
                        targetDepartureTime = obj.optString("targetDepartureTime", "07:10"),
                        startTime = obj.optString("startTime", "06:20"),
                        starCount = obj.optInt("starCount", 0),
                        repeatType = obj.optString("repeatType", "Everyday"),
                        voiceName = obj.optString("voiceName", "Luma"),
                        reminderEnabled = obj.optBoolean("reminderEnabled", true),
                        lastCompletedDate = if (obj.has("lastCompletedDate") && !obj.isNull("lastCompletedDate")) {
                            obj.getString("lastCompletedDate").takeIf { it != "null" && it.isNotBlank() }
                        } else null,
                        steps = if (steps.isNotEmpty()) steps else Routine.defaultMorningSteps()
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.w("RoutineRepository", "Error loading cached routines", e)
            emptyList()
        }
    }

    private fun syncFamilyDataToCloud() {
        val user = auth.currentUser ?: return
        val fam = _familyCode.value.ifEmpty { "FAM-${user.uid.take(4).uppercase()}" }
        scope.launch(Dispatchers.IO) {
            try {
                val data = mapOf(
                    "familyId" to fam,
                    "earnedStars" to _earnedStars.value,
                    "starPrizes" to _starPrizes.value.map { prizeToMap(it) },
                    "stepRunHistory" to _stepRunHistory.value.takeLast(50).map { historyToMap(it) },
                    "lastUpdated" to System.currentTimeMillis()
                )
                firestore.collection("families").document(fam).set(data, SetOptions.merge()).await()
            } catch (e: Exception) {
                Log.w("RoutineRepository", "Error syncing family data", e)
            }
        }
    }

    private fun uploadInitialFamilyData(famId: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val data = mapOf(
                    "familyId" to famId,
                    "earnedStars" to _earnedStars.value,
                    "starPrizes" to _starPrizes.value.map { prizeToMap(it) },
                    "stepRunHistory" to _stepRunHistory.value.map { historyToMap(it) },
                    "lastUpdated" to System.currentTimeMillis()
                )
                firestore.collection("families").document(famId).set(data, SetOptions.merge()).await()
            } catch (e: Exception) {
                Log.w("RoutineRepository", "Error uploading initial family data", e)
            }
        }
    }

    fun saveCatalogTask(task: CatalogTask) {
        val current = _taskCatalog.value
        val updated = if (current.any { it.id == task.id }) {
            current.map { if (it.id == task.id) task else it }
        } else {
            current + task
        }
        _taskCatalog.value = updated
        saveCachedCatalog(updated)

        // Cascade catalog updates to all routines that reference this catalog item (Source of Truth!)
        cascadeCatalogTaskToRoutines(task)

        val user = auth.currentUser
        if (user != null) {
            val fam = _familyCode.value.ifEmpty { "FAM-${user.uid.take(4).uppercase()}" }
            scope.launch(Dispatchers.IO) {
                try {
                    firestore.collection("task_catalog")
                        .document(task.id)
                        .set(catalogTaskToMap(task, user.uid, fam), SetOptions.merge())
                        .await()
                } catch (e: Exception) {
                    Log.w("RoutineRepository", "Error syncing catalog task", e)
                }
            }
        }
    }

    /**
     * Propagates changes from a catalog item into all routine steps referencing it.
     * Catalog items are the source of truth!
     */
    fun cascadeCatalogTaskToRoutines(task: CatalogTask) {
        val updatedRoutines = _allRoutines.value.map { routine ->
            if (routine.steps.any { it.catalogTaskId == task.id }) {
                val updatedSteps = routine.steps.map { step ->
                    if (step.catalogTaskId == task.id) {
                        step.copy(
                            title = task.title,
                            iconKey = task.iconKey,
                            colorHex = task.colorHex,
                            defaultMinutes = task.defaultMinutes,
                            minMinutes = task.minMinutes,
                            maxMinutes = task.maxMinutes,
                            isImportant = task.isImportant,
                            starReward = task.starReward
                        )
                    } else {
                        step
                    }
                }
                routine.copy(steps = updatedSteps)
            } else {
                routine
            }
        }
        _allRoutines.value = updatedRoutines

        val cur = _currentRoutine.value
        if (cur.steps.any { it.catalogTaskId == task.id }) {
            val updatedSteps = cur.steps.map { step ->
                if (step.catalogTaskId == task.id) {
                    step.copy(
                        title = task.title,
                        iconKey = task.iconKey,
                        colorHex = task.colorHex,
                        defaultMinutes = task.defaultMinutes,
                        minMinutes = task.minMinutes,
                        maxMinutes = task.maxMinutes,
                        isImportant = task.isImportant,
                        starReward = task.starReward
                    )
                } else {
                    step
                }
            }
            _currentRoutine.value = cur.copy(steps = updatedSteps)
        }

        val user = auth.currentUser
        if (user != null) {
            val fam = _familyCode.value.ifEmpty { "FAM-${user.uid.take(4).uppercase()}" }
            scope.launch(Dispatchers.IO) {
                for (routine in updatedRoutines.filter { r -> r.steps.any { it.catalogTaskId == task.id } }) {
                    try {
                        firestore.collection("routines").document(routine.id)
                            .set(routineToMap(routine, user.uid, fam), SetOptions.merge())
                            .await()
                    } catch (e: Exception) {
                        Log.w("RoutineRepository", "Error syncing cascaded routine ${routine.id}", e)
                    }
                }
            }
        }
    }

    fun deleteCatalogTask(taskId: String) {
        val updated = _taskCatalog.value.filter { it.id != taskId }
        _taskCatalog.value = updated
        saveCachedCatalog(updated)
        val user = auth.currentUser
        if (user != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    firestore.collection("task_catalog").document(taskId).delete().await()
                } catch (e: Exception) {
                    Log.w("RoutineRepository", "Error deleting catalog task", e)
                }
            }
        }
    }

    private fun uploadInitialCatalog(famId: String, userId: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val tasksToUpload = _taskCatalog.value.ifEmpty { CatalogTask.defaultCatalog() }
                for (task in tasksToUpload) {
                    firestore.collection("task_catalog")
                        .document(task.id)
                        .set(catalogTaskToMap(task, userId, famId), SetOptions.merge())
                        .await()
                }
            } catch (e: Exception) {
                Log.w("RoutineRepository", "Error uploading initial catalog", e)
            }
        }
    }

    private fun saveCachedCatalog(tasks: List<CatalogTask>) {
        try {
            val arr = org.json.JSONArray()
            for (t in tasks) {
                val obj = org.json.JSONObject()
                obj.put("id", t.id)
                obj.put("title", t.title)
                obj.put("iconKey", t.iconKey)
                obj.put("defaultMinutes", t.defaultMinutes)
                obj.put("minMinutes", t.minMinutes)
                obj.put("maxMinutes", t.maxMinutes)
                obj.put("isImportant", t.isImportant)
                obj.put("starReward", t.starReward)
                obj.put("colorHex", t.colorHex)
                obj.put("category", t.category)
                arr.put(obj)
            }
            prefs.edit().putString("cached_catalog_json", arr.toString()).apply()
        } catch (e: Exception) {
            Log.w("RoutineRepository", "Error caching task catalog", e)
        }
    }

    private fun loadCachedCatalog(): List<CatalogTask> {
        val raw = prefs.getString("cached_catalog_json", null) ?: return emptyList()
        return try {
            val arr = org.json.JSONArray(raw)
            val list = mutableListOf<CatalogTask>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    CatalogTask(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        iconKey = obj.optString("iconKey", "clothes"),
                        defaultMinutes = obj.optInt("defaultMinutes", 5),
                        minMinutes = obj.optInt("minMinutes", 2),
                        maxMinutes = obj.optInt("maxMinutes", 10),
                        isImportant = obj.optBoolean("isImportant", false),
                        starReward = obj.optInt("starReward", 1),
                        colorHex = obj.optString("colorHex", "#4ADE80"),
                        category = obj.optString("category", "General")
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.w("RoutineRepository", "Error loading cached catalog", e)
            emptyList()
        }
    }

    fun recoverCatalogFromRoutines(routines: List<Routine>) {
        val current = _taskCatalog.value
        val newTasks = mutableListOf<CatalogTask>()
        for (r in routines) {
            for (step in r.steps) {
                val catId = step.catalogTaskId ?: "cat_${step.title.lowercase().replace(" ", "_").filter { it.isLetterOrDigit() || it == '_' }}"
                val exists = current.any { it.id == catId || (step.catalogTaskId != null && it.id == step.catalogTaskId) || it.title.equals(step.title, ignoreCase = true) }
                if (!exists && newTasks.none { it.id == catId || it.title.equals(step.title, ignoreCase = true) }) {
                    newTasks.add(
                        CatalogTask(
                            id = catId,
                            title = step.title,
                            iconKey = step.iconKey,
                            defaultMinutes = step.defaultMinutes,
                            minMinutes = step.minMinutes,
                            maxMinutes = step.maxMinutes,
                            isImportant = step.isImportant,
                            starReward = step.starReward,
                            colorHex = step.colorHex,
                            category = "General"
                        )
                    )
                }
            }
        }
        if (newTasks.isNotEmpty()) {
            val merged = (CatalogTask.defaultCatalog() + current + newTasks).distinctBy { it.id }
            _taskCatalog.value = merged
            saveCachedCatalog(merged)
            Log.i("RoutineRepository", "Auto-recovered ${newTasks.size} tasks from routines into task catalog")
        }
    }

    private fun catalogTaskToMap(task: CatalogTask, userId: String, famId: String): Map<String, Any?> = mapOf(
        "id" to task.id,
        "userId" to userId,
        "familyId" to famId,
        "title" to task.title,
        "iconKey" to task.iconKey,
        "defaultMinutes" to task.defaultMinutes,
        "minMinutes" to task.minMinutes,
        "maxMinutes" to task.maxMinutes,
        "isImportant" to task.isImportant,
        "starReward" to task.starReward,
        "colorHex" to task.colorHex,
        "category" to task.category
    )

    private fun parseCatalogTask(doc: DocumentSnapshot): CatalogTask? = try {
        CatalogTask(
            id = doc.id,
            title = doc.getString("title") ?: "",
            iconKey = doc.getString("iconKey") ?: "clothes",
            defaultMinutes = doc.getLong("defaultMinutes")?.toInt() ?: 5,
            minMinutes = doc.getLong("minMinutes")?.toInt() ?: 2,
            maxMinutes = doc.getLong("maxMinutes")?.toInt() ?: 10,
            isImportant = doc.getBoolean("isImportant") ?: false,
            starReward = doc.getLong("starReward")?.toInt() ?: 1,
            colorHex = doc.getString("colorHex") ?: "#4ADE80",
            category = doc.getString("category") ?: "General"
        )
    } catch (_: Exception) { null }

    private fun prizeToMap(prize: StarPrize): Map<String, Any?> = mapOf(
        "id" to prize.id,
        "title" to prize.title,
        "starsNeeded" to prize.starsNeeded,
        "type" to prize.type,
        "iconEmoji" to prize.iconEmoji,
        "isRedeemed" to prize.isRedeemed,
        "redeemedDate" to prize.redeemedDate
    )

    private fun parseStarPrize(map: Map<String, Any>): StarPrize? = try {
        StarPrize(
            id = map["id"] as? String ?: "",
            title = map["title"] as? String ?: "",
            starsNeeded = (map["starsNeeded"] as? Long)?.toInt() ?: 10,
            type = map["type"] as? String ?: "WEEKLY",
            iconEmoji = map["iconEmoji"] as? String ?: "⭐",
            isRedeemed = map["isRedeemed"] as? Boolean ?: false,
            redeemedDate = map["redeemedDate"] as? String
        )
    } catch (_: Exception) { null }

    private fun historyToMap(history: StepRunHistory): Map<String, Any?> = mapOf(
        "id" to history.id,
        "routineId" to history.routineId,
        "stepId" to history.stepId,
        "stepTitle" to history.stepTitle,
        "catalogTaskId" to history.catalogTaskId,
        "scheduledMinutes" to history.scheduledMinutes,
        "actualSecondsTaken" to history.actualSecondsTaken,
        "wasOvertime" to history.wasOvertime,
        "wasAssisted" to history.wasAssisted,
        "timestamp" to history.timestamp
    )

    private fun parseHistoryEntry(map: Map<String, Any>): StepRunHistory? = try {
        StepRunHistory(
            id = map["id"] as? String ?: "",
            routineId = map["routineId"] as? String ?: "",
            stepId = map["stepId"] as? String ?: "",
            stepTitle = map["stepTitle"] as? String ?: "",
            catalogTaskId = map["catalogTaskId"] as? String,
            scheduledMinutes = (map["scheduledMinutes"] as? Long)?.toInt() ?: 5,
            actualSecondsTaken = (map["actualSecondsTaken"] as? Long)?.toInt() ?: 300,
            wasOvertime = map["wasOvertime"] as? Boolean ?: false,
            wasAssisted = map["wasAssisted"] as? Boolean ?: false,
            timestamp = (map["timestamp"] as? Long) ?: System.currentTimeMillis()
        )
    } catch (_: Exception) { null }
}
