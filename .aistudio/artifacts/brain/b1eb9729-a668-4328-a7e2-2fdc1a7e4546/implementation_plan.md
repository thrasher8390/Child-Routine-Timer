# Streamlining UX, Bonus Time Reversal & Task Catalog Hub

## 1. User Decisions & Observations

Based on your observations and the uploaded screenshots:
1. **Bonus Time & Preserved Remaining Time on Unmarking**:
   - When a task is unmarked after completion, its banked bonus time is deducted from the bank.
   - The task's remaining time when originally completed is restored so that going back to the task displays the time left rather than showing an expired timer.
2. **Dedicated Task Catalog & Routine Picker**:
   - Provide a dedicated, accessible view to browse, add, edit, and organize shared tasks in the catalog.
   - When customizing routine tasks in the routine editor, provide a clear **"Choose from Catalog"** picker.
3. **Streamlined Dashboard (Screenshot 1)**:
   - **Clutter Elimination**: Replace the dual side-by-side circular clocks (which consumed 35% of the screen height) with a clean **Minimal Digital Time & Departure Countdown Pill**.
   - **Header Clean-Up**: Fix the cramped vertical wrapping of the family code badge (`-ADR \n 6`) into a sleek compact pill.
   - **Unified Navigation & Quick Tools**: Organize the routine tabs, quick tools (Quick Timer, Quick Routine, Task Catalog), and live session banner into a harmonious layout without competing stacked cards.
4. **Streamlined Run Mode Screen (Screenshot 2)**:
   - **Single-Line Top Control Bar**: Replace the tall stacked control card and broken vertically wrapped text (`Ta \n sk \n s \n :三 \n (1/6)`) with a compact, unified 1-line control strip.
   - **Full-Size Visual Hourglass**: Give the animated hourglass timer central stage and room to breathe so the child can focus on the countdown without visual overwhelm.
   - **Protected Action Targets**: Maintain the 48dp touch target rule for Up Next, Done, and controls.

---

## 2. Technical Architecture & Component Plans

### A. Bonus Time & Remaining Seconds Reversal Logic

- **Model Update (`ActiveSession` in `RoutineModels.kt`)**:
  - Add `stepBankedSecondsMap: Map<String, Int> = emptyMap()` to record the exact bonus seconds banked per step.
  - In `nextStep(bankBonus = true)`:
    - Save the banked seconds in `stepBankedSecondsMap[stepId]`.
    - **Do NOT overwrite `stepRemainingSecondsMap[stepId]` with 0.** Preserve `session.stepRemainingSeconds` so the exact time left is saved.
  - In `setStepCompletionStatus(stepId, isDone, isAssisted)`:
    - When unmarking a completed task (`wasDone == true && isDone == false`):
      1. Deduct banked seconds: `bonusTimeSeconds = (bonusTimeSeconds - bankedSeconds).coerceAtLeast(0)`.
      2. Remove entry from `stepBankedSecondsMap`.
      3. Restore `stepRemainingSeconds` from `stepRemainingSecondsMap[stepId]` (or step default duration).
      4. Deduct any awarded stars if completed independently.
      5. If routine was marked completed, return status to `SessionStatus.RUNNING`.
    - When marking a task completed from the dialog:
      1. Bank whatever remaining time was left for that task into `bonusTimeSeconds`.
      2. Record entry in `stepBankedSecondsMap`.
  - In `jumpToStep`:
    - Restore `stepRemainingSeconds = map[targetStep.id] ?: stepDurationSec` (which now preserves the time left rather than 0).

### B. Dedicated Task Catalog Hub & Picker

- **New Screen (`TaskCatalogScreen.kt`)**:
  - Top bar with Back button, "Task Catalog" title, search/filter chips (All, Morning, Hygiene, Bedtime, School, Departure, etc.), and `+ New Task` button.
  - Card list showing each catalog task:
    - Task icon, title, category tag, default duration (min/max bounds).
    - Star reward indicator.
    - Quick Edit and Delete buttons.
  - Add / Edit Dialog for creating or modifying catalog tasks.
- **Routine Editor Integration (`TaskListScreen.kt`)**:
  - Add **`[ 📖 Choose from Catalog ]`** button alongside `+ Add Activity`.
  - Opens a clean bottom sheet / dialog with categorized catalog tasks, search bar, and checkboxes to quickly append multiple tasks into the routine.
- **Custom Task Reuse**:
  - In `TaskEditDialog`, include a checkbox: *"Save to Catalog for reuse in other routines"*.

### C. Dashboard Simplification (`DashboardScreen.kt`)

- **Top Bar**:
  - Title: *"Child Routine Timer"*.
  - Compact horizontal pill row: `[ Help ]`, `[ ⭐ Stars ]`, and single-line `[ 🟢 FAM-XXXX ]`.
- **Hero Departure & Time Strip**:
  - Replaces dual analog and radial clocks with:
    - Clean, bold digital clock (e.g. `9:06 AM`).
    - Departure badge: `🎯 Leave at 8:30 PM • 54m window (+6m buffer)`.
    - Pace chip: `🟢 On Track` / `⚡ Rush` / `🌱 Relaxed`.
- **Quick Action Strip**:
  - 3 balanced cards/pills:
    - `[ ⏳ Quick Timer ]`
    - `[ 🚀 Quick Routine ]`
    - `[ 📚 Task Catalog ]`
- **Routine Tabs & Sequence**:
  - Clean selector tabs with active indicator.
  - Compact task sequence cards showing step icon, title, duration, and today's toggle.
- **Bottom Launch Bar**:
  - Prominent primary button: `[ ▶️ Start Routine ]` or `[ 🟢 Resume Routine ]`.

### D. Run Mode Simplification (`RunModeScreen.kt`)

- **Unified Top Bar (Single Line)**:
  - Back arrow, live time, step progress indicator `Task 2 of 6`, pace pill `⚡ Rush`, and expand/hide arrow.
- **Compact Floating Controls Row**:
  - Centered clean pill containing: `[ ⏸️ ]`, `[ -1m ]`, `[ +2m ]`, `[ ⏭️ Skip ]`, `[ 🔒 Lock ]`.
  - No awkwardly wrapped vertical text.
- **Spacious Center Stage**:
  - Prominent task title and icon with star reward encouragement.
  - **Full-sized visual hourglass** with animated sand flow and glowing ambient glow.
  - Crisp, large digital countdown timer (`03:42`).
- **Bottom Navigation**:
  - Minimal secondary text buttons (`Back`, `Skip Task`, `Stop Routine`).
  - Large 48dp+ primary execution buttons:
    - `[ UP NEXT ➔ Task Name ]` (independent completion).
    - `[ 🤝 Assisted ]` (parent assisted completion).

---

## 3. Verification & Validation

1. **Unit Testing (`ElasticSchedulingTest.kt`)**:
   - Verify unmarking a completed task deducts banked bonus time from `bonusTimeSeconds`.
   - Verify unmarking a completed task preserves and restores the remaining seconds.
   - Verify catalog tasks can be converted to routine steps and added to routines.
2. **Compilation**:
   - Run `compile_applet` to ensure zero compilation or syntax errors.
3. **Robolectric Local Test Execution**:
   - Run `gradle :app:testDebugUnitTest` to verify all test suites pass.
