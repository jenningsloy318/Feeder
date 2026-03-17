# Implementation Plan: Circle Progress Icon for Summarize/Translate

**Specification:** `./06-technical-specification.md`
**Total Phases:** 4
**Total Tasks:** 14

**CRITICAL:** All phases defined in this plan MUST be implemented in a single continuous execution. The execution-coordinator will NOT pause between phases.

## File Inventory

### Files to be Created
| File Path | Purpose |
|-----------|---------|
| `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/CircleProgressIconButton.kt` | Reusable composable: circular progress + stop square overlay on toolbar icon |
| `app/src/test/java/com/nononsenseapps/feeder/ui/compose/feedarticle/CircleProgressIconButtonTest.kt` | Unit tests for the composable |

### Files to be Modified
| File Path | Changes Required |
|-----------|-----------------|
| `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt` | Add `summarizeJob`/`translateJob` fields, `cancelSummarize()`/`cancelTranslation()` methods, `CancellationException` handling, `Job`/`CancellationException` imports |
| `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt` | Add `onCancelSummarize`/`onCancelTranslation` callbacks to 3 composable layers, replace icon buttons with `CircleProgressIconButton`, remove inline progress bars, update LazyColumn conditions |
| `app/src/main/res/values/strings.xml` | Add 4 new accessibility/tooltip strings |

### Files to be Deleted
None.

### File Summary
- **Total Files Created:** 2
- **Total Files Modified:** 3
- **Total Files Deleted:** 0
- **Total Files Affected:** 5

---

## Phase A: Create CircleProgressIconButton Composable

**Goal:** Create the reusable composable that renders either a normal IconButton or a circular progress ring with stop square.
**Dependencies:** None

### Tasks

- [ ] **T1.1** Create `CircleProgressIconButton.kt` in `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/`
  - **Details:** Implement the composable with these parameters: `isInProgress: Boolean`, `progressFraction: (() -> Float)?`, `icon: ImageVector`, `idleContentDescription: String`, `progressContentDescription: String`, `onAction: () -> Unit`, `onCancel: () -> Unit`, `modifier: Modifier = Modifier`
  - **Idle state:** Standard `IconButton(onClick = onAction)` with `Icon(icon, contentDescription = idleContentDescription)`
  - **Progress state:** `Box(Modifier.size(48.dp).clickable(onClick = onCancel, role = Role.Button).semantics { contentDescription = progressContentDescription; role = Role.Button }, contentAlignment = Center)` containing:
    - If `progressFraction != null`: `CircularProgressIndicator(progress = progressFraction, Modifier.size(24.dp), strokeWidth = 2.dp, trackColor = MaterialTheme.colorScheme.surfaceVariant, gapSize = 0.dp)`
    - If `progressFraction == null`: `CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)`
    - Center stop square: `Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.onSurface, RoundedCornerShape(1.dp)))`
  - **Scenarios:** SCENARIO-001, SCENARIO-006, SCENARIO-007, SCENARIO-013, SCENARIO-014, SCENARIO-015
  - **Acceptance:** Composable compiles, shows icon when idle, shows progress + stop square when in-progress

---

## Phase B: Add Job Tracking and Cancel Methods to ArticleViewModel

**Goal:** Store coroutine Job references and add cancel methods with proper CancellationException handling.
**Dependencies:** None (independent of Phase A)

### Tasks

- [ ] **T2.1** Add Job fields and import statements to `ArticleViewModel.kt`
  - **File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt`
  - **Details:** Add `import kotlinx.coroutines.CancellationException` and `import kotlinx.coroutines.Job` to imports. Add `private var summarizeJob: Job? = null` and `private var translateJob: Job? = null` near line 123-125 (alongside `aiSummary` and `translationState` declarations)
  - **Acceptance:** File compiles with new fields

- [ ] **T2.2** Modify `summarize()` to store Job and handle CancellationException
  - **File:** `ArticleViewModel.kt` lines 466-484
  - **Details:**
    1. Add `summarizeJob?.cancel()` before the launch (handles rapid re-trigger)
    2. Change `viewModelScope.launch(Dispatchers.IO) {` to `summarizeJob = viewModelScope.launch(Dispatchers.IO) {`
    3. Add `catch (e: CancellationException) { throw e }` BEFORE the existing `catch (e: Exception)` block
  - **Scenarios:** SCENARIO-001, SCENARIO-002, SCENARIO-004, SCENARIO-012
  - **Acceptance:** `summarize()` stores Job reference; CancellationException is rethrown, not caught as error

- [ ] **T2.3** Modify `translate()` to store Job and handle CancellationException
  - **File:** `ArticleViewModel.kt` lines 496-577
  - **Details:**
    1. Add `translateJob?.cancel()` before the launch
    2. Change `viewModelScope.launch(Dispatchers.IO) {` to `translateJob = viewModelScope.launch(Dispatchers.IO) {`
    3. Add `catch (e: CancellationException) { throw e }` BEFORE the existing `catch (e: Exception)` block at line 572
  - **Scenarios:** SCENARIO-006, SCENARIO-008, SCENARIO-010, SCENARIO-012
  - **Acceptance:** `translate()` stores Job reference; CancellationException is rethrown

- [ ] **T2.4** Add `cancelSummarize()` and `cancelTranslation()` methods
  - **File:** `ArticleViewModel.kt` (add after `summarize()` and `translate()` methods)
  - **Details:**
    ```kotlin
    fun cancelSummarize() {
        summarizeJob?.cancel()
        summarizeJob = null
        aiSummary.value = AISummaryState.Empty
    }

    fun cancelTranslation() {
        translateJob?.cancel()
        translateJob = null
        translationState.value = TranslationState.Empty
    }
    ```
  - **Scenarios:** SCENARIO-002, SCENARIO-008, SCENARIO-012
  - **Acceptance:** Both methods compile; calling them cancels the Job and resets state to Empty

---

## Phase C: Wire Up ArticleScreen

**Goal:** Replace icon buttons with CircleProgressIconButton, add cancel callbacks through composable chain, remove inline progress bars.
**Dependencies:** Phase A (CircleProgressIconButton exists), Phase B (cancel methods exist)

### Tasks

- [ ] **T3.1** Add new string resources
  - **File:** `app/src/main/res/values/strings.xml`
  - **Details:** Add after the existing `summarizing_progress` / `translating_progress` entries (around line 276-278):
    ```xml
    <string name="cancel_summarize">Cancel summarize</string>
    <string name="summarizing_tap_to_cancel">Summarizing in progress, tap to cancel</string>
    <string name="cancel_translation">Cancel translation</string>
    <string name="translating_x_of_y_tap_to_cancel">Translating %1$d of %2$d paragraphs, tap to cancel</string>
    ```
  - **Scenarios:** SCENARIO-013, SCENARIO-014
  - **Acceptance:** App compiles with new strings

- [ ] **T3.2** Add `onCancelSummarize` and `onCancelTranslation` callbacks to composable chain
  - **File:** `ArticleScreen.kt`
  - **Details:** Add two new parameters `onCancelSummarize: () -> Unit` and `onCancelTranslation: () -> Unit` to three composable signatures:
    1. **Top-level `ArticleScreen`** (line 89): Add to the inner `ArticleScreen(...)` call at line 104: `onCancelSummarize = { viewModel.cancelSummarize() }` and `onCancelTranslation = { viewModel.cancelTranslation() }`
    2. **Public stateless `ArticleScreen`** (line 159): Add parameters to signature, pass through to `ArticleScreenInternal(...)` at line 184
    3. **Private `ArticleScreenInternal`** (line 211): Add parameters to signature
  - **Acceptance:** File compiles with new callbacks threaded through all 3 layers

- [ ] **T3.3** Replace summarize IconButton with CircleProgressIconButton
  - **File:** `ArticleScreen.kt` lines 270-281
  - **Details:** Replace the `if (viewState.showSummarize) { PlainTooltipBox { IconButton { Icon(AutoFixHigh) } } }` block with the `CircleProgressIconButton`-based version from the technical specification section 3.4. The `PlainTooltipBox` tooltip text changes dynamically based on `isSummarizing` state.
  - **Scenarios:** SCENARIO-001, SCENARIO-002, SCENARIO-003, SCENARIO-004, SCENARIO-005, SCENARIO-013
  - **Acceptance:** Summarize icon shows indeterminate circular progress during Loading state, stop square cancels

- [ ] **T3.4** Replace translate IconButton with CircleProgressIconButton
  - **File:** `ArticleScreen.kt` lines 283-302
  - **Details:** Replace the `if (viewState.showSummarize) { PlainTooltipBox { IconButton { Icon(Translate) } } }` block with the `CircleProgressIconButton`-based version from the technical specification section 3.4. Compute `translationProgressFraction` lambda from `articleTranslation.paragraphCompletedCount / paragraphTotalCount`. Dynamic `progressContentDescription` uses `R.string.translating_x_of_y_tap_to_cancel`.
  - **Scenarios:** SCENARIO-006, SCENARIO-007, SCENARIO-008, SCENARIO-009, SCENARIO-010, SCENARIO-014
  - **Acceptance:** Translate icon shows determinate circular progress during Translating state, fraction updates as paragraphs complete

- [ ] **T3.5** Remove inline progress bars and update LazyColumn conditions
  - **File:** `ArticleScreen.kt`
  - **Details:**
    1. **SummarySection** (lines 616-656): Remove the `AISummaryState.Loading` branch (lines 622-636). Keep `Empty` and `Result` branches.
    2. **LazyColumn SummarySection condition** (line 534): Change `viewState.aiSummary !is AISummaryState.Empty` to `viewState.aiSummary is AISummaryState.Result`
    3. **TranslationStatusSection** (lines 695-736): Remove the entire `TranslationState.Translating` branch (lines 699-721). Keep `Empty`, `Translated`, and `Error` branches.
    4. **LazyColumn TranslationStatusSection condition** (line 542): Change `viewState.translation !is TranslationState.Empty` to `viewState.translation is TranslationState.Translated || viewState.translation is TranslationState.Error`
    5. Remove `LinearProgressIndicator` import if no longer used in the file.
  - **Scenarios:** SCENARIO-001, SCENARIO-006
  - **Acceptance:** No LinearProgressIndicator visible in article content; summary result card and translation error card still display correctly

---

## Phase D: Tests and Verification

**Goal:** Add tests, verify build, run existing tests.
**Dependencies:** Phases A, B, C complete

### Tasks

- [ ] **T4.1** Create unit tests for CircleProgressIconButton
  - **File:** `app/src/test/java/com/nononsenseapps/feeder/ui/compose/feedarticle/CircleProgressIconButtonTest.kt`
  - **Details:** Write Compose UI tests using `createComposeRule()`:
    - `idleState_showsIconButton`: Assert icon and idle content description visible, progress indicator not visible
    - `indeterminateProgress_showsSpinnerAndStopSquare`: Assert CircularProgressIndicator and stop square visible when `isInProgress = true, progressFraction = null`
    - `determinateProgress_showsFractionAndStopSquare`: Assert CircularProgressIndicator with progress and stop square visible when `isInProgress = true, progressFraction = { 0.5f }`
    - `idleState_clickCallsOnAction`: Assert clicking fires onAction, not onCancel
    - `progressState_clickCallsOnCancel`: Assert clicking fires onCancel, not onAction
    - `progressContentDescription_isSet`: Assert semantics contentDescription matches progressContentDescription string
  - **Scenarios:** SCENARIO-001 through SCENARIO-015
  - **Acceptance:** All tests pass

- [ ] **T4.2** Build and run all existing tests
  - **Command:** `./gradlew :app:compileFdroidDebugKotlin` then `./gradlew :app:testFdroidDebugUnitTest`
  - **Acceptance:** Build succeeds, all existing tests pass, no regressions

---

## Task Dependencies

```
T1.1 (CircleProgressIconButton) ──┐
                                  ├──▶ T3.3, T3.4
T2.1 ──▶ T2.2 ──┐                │
         T2.3 ──┤                │
         T2.4 ──┘────────────────┘
                                  │
T3.1 (strings) ──────────────────┤
T3.2 (callbacks) ────────────────┤
                                  ├──▶ T3.5 ──▶ T4.1 ──▶ T4.2
T3.3 (summarize icon) ──────────┤
T3.4 (translate icon) ──────────┘
```

**Parallel execution possible:**
- Phase A (T1.1) and Phase B (T2.1-T2.4) are independent — can run in parallel
- T3.1 and T3.2 are independent of each other
- T4.1 and T4.2 are sequential (tests must pass before final verification)

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| CancellationException caught by `catch (e: Exception)` | High (confirmed in assessment) | High (cancel sets error state) | T2.2 and T2.3 add explicit `catch (e: CancellationException) { throw e }` |
| `CircularProgressIndicator` gapSize parameter not available in project's M3 version | Low | Medium (visual gap in ring) | Check M3 version; omit `gapSize` if unavailable |
| PlainTooltipBox breaks with non-IconButton child | Low | Low (tooltip gone) | Tooltip wraps the CircleProgressIconButton; IconButton is still used in idle state |
| Race condition on rapid cancel + restart | Low | Low (state flicker) | `summarizeJob?.cancel()` at start of `summarize()` handles this |

## Success Metrics

- [ ] No `LinearProgressIndicator` visible in article content during summarize or translate
- [ ] Summarize icon shows indeterminate circular progress with stop square during Loading
- [ ] Translate icon shows determinate circular progress with fraction during Translating
- [ ] Tapping stop square cancels operation and returns icon to idle
- [ ] After completion, icons return to normal and are re-clickable
- [ ] Summary result card and translation error card still display in article content
- [ ] All existing tests pass
- [ ] New CircleProgressIconButton tests pass
- [ ] Accessibility content descriptions present on progress icons
