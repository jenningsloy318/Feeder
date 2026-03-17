# Code Assessment: Circle Progress Icon for Summarize/Translate

**Date:** 2026-03-17
**Scope:** ArticleScreen.kt, ArticleViewModel.kt, icons/, components/
**Focus:** Architecture, patterns, parameter chains, risk areas

---

## Executive Summary

1. **Both progress bars are inline `LinearProgressIndicator`** in lazy list items -- straightforward to remove
2. **No cancellation support exists** -- `summarize()` and `translate()` launch coroutines without storing `Job` references
3. **No custom progress-icon composable exists** -- a new `CircleProgressIconButton` composable is needed
4. **`CircularProgressIndicator` is already used** elsewhere in the project (settings screens, pull-to-refresh) -- consistent pattern to follow
5. **Parameter chain is deep but uniform** -- `onSummarize`/`onTranslate` callbacks pass through 3 composable layers; adding `onCancelSummarize`/`onCancelTranslation` follows the same pattern

---

## 1. ArticleScreen.kt - Current Implementation

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt` (788 lines)

### 1.1 Summarize Icon Button (lines 270-281)

```kotlin
if (viewState.showSummarize) {
    PlainTooltipBox(tooltip = { Text(stringResource(R.string.summarize)) }) {
        IconButton(onClick = onSummarize) {
            Icon(
                Icons.Default.AutoFixHigh,
                contentDescription = stringResource(R.string.summarize),
            )
        }
    }
}
```

**Key observations:**
- Guarded by `viewState.showSummarize` (true when AI settings valid + article has link)
- Standard `IconButton` + `Icon` pattern
- Wrapped in `PlainTooltipBox` for long-press tooltip
- **No awareness of loading state** -- always shows the static icon
- Always enabled, always clickable

### 1.2 Translate Icon Button (lines 283-302)

```kotlin
if (viewState.showSummarize) {  // NOTE: reuses showSummarize flag
    PlainTooltipBox(tooltip = { Text(stringResource(R.string.translate)) }) {
        val isTranslationInProgress = viewState.translation is TranslationState.Translating
        IconButton(
            onClick = onTranslate,
            enabled = !isTranslationInProgress,
        ) {
            Icon(
                Icons.Default.Translate,
                contentDescription = if (isTranslationInProgress) {
                    "Translating article, please wait"
                } else {
                    stringResource(R.string.translate_article_content_description)
                },
            )
        }
    }
}
```

**Key observations:**
- Also guarded by `viewState.showSummarize` (same condition for both buttons)
- **Already reads `viewState.translation`** to check `Translating` state
- Currently **disables** the button during translation (will change to show progress + stop)
- Has hardcoded accessibility string `"Translating article, please wait"` (not in resources)
- `onClick = onTranslate` during progress does nothing (disabled)

### 1.3 SummarySection Composable (lines 616-656)

```kotlin
@Composable
private fun SummarySection(summary: AISummaryState) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        when (summary) {
            AISummaryState.Empty -> {}
            AISummaryState.Loading -> Column(...) {
                Text("Summarizing...")
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())  // LINE 633-634
            }
            is AISummaryState.Result -> { MarkdownText(...) }
        }
    }
}
```

**What to change:**
- Remove the `AISummaryState.Loading` branch entirely (progress moves to icon)
- Keep `AISummaryState.Result` branch (summary display card stays)
- The wrapping `OutlinedCard` still needed for Result state
- The `Empty` branch can stay as-is (renders nothing inside card)

### 1.4 TranslationStatusSection Composable (lines 695-736)

```kotlin
@Composable
private fun TranslationStatusSection(translation: TranslationState) {
    when (translation) {
        TranslationState.Empty -> {}
        is TranslationState.Translating -> {
            // Lines 699-721: OutlinedCard with LinearProgressIndicator
            val progressFraction = completedCount.toFloat() / totalCount
            LinearProgressIndicator(progress = { progressFraction })
        }
        is TranslationState.Translated -> {
            if (failedCount > 0) TranslationErrorSection(...)
        }
        is TranslationState.Error -> TranslationErrorSection(...)
    }
}
```

**What to change:**
- Remove the entire `Translating` branch (lines 699-721)
- Keep `Translated` with error display and `Error` branch
- The composable still needed for error states

### 1.5 LazyColumn Placement (lines 534-547)

```kotlin
if (viewState.aiSummary !is AISummaryState.Empty) {
    offsetCounter++
    item { SummarySection(viewState.aiSummary) }
}
if (viewState.translation !is TranslationState.Empty) {
    offsetCounter++
    item { TranslationStatusSection(viewState.translation) }
}
```

**What to change:**
- `SummarySection` condition: change to only render when `Result` (not `Loading`)
- `TranslationStatusSection` condition: adjust to exclude `Translating` (only show for `Translated` with errors, or `Error`)

### 1.6 Parameter Passing Chain

The callback chain passes through **3 layers**:

```
ArticleScreen(viewModel)                    [line 89]   -- top-level, creates callbacks
  -> ArticleScreen(viewState, onSummarize, onTranslate, ...)  [line 159]  -- public stateless
    -> ArticleScreenInternal(viewState, onSummarize, onTranslate, ...)  [line 211]  -- private, has Scaffold
```

**New callbacks needed:**
- `onCancelSummarize: () -> Unit`
- `onCancelTranslation: () -> Unit`

These must be added to all 3 layers. The pattern is straightforward -- every existing callback follows this exact chain.

---

## 2. ArticleViewModel.kt - State and Coroutine Patterns

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt` (755 lines)

### 2.1 summarize() Function (lines 466-484)

```kotlin
fun summarize() {
    viewModelScope.launch(Dispatchers.IO) {
        try {
            aiSummary.value = AISummaryState.Loading
            val content = loadArticleContent()
            aiSummary.value = AISummaryState.Result(
                value = aiApi.summarize(content),
            )
        } catch (e: Exception) {
            aiSummary.value = AISummaryState.Result(
                value = AIClient.SummaryResult.Error(content = e.message ?: "Unknown error"),
            )
        }
    }
}
```

**Key findings:**
- Launches via `viewModelScope.launch(Dispatchers.IO)` -- **does NOT store the Job**
- Simple try/catch, no structured cancellation
- Sets state to `Loading` immediately, then to `Result` on completion/error
- `loadArticleContent()` is a suspending function (network/IO)

**Required change:** Store `Job` in a `private var summarizeJob: Job? = null` field, add `cancelSummarize()` that calls `summarizeJob?.cancel()` and resets `aiSummary` to `Empty`.

### 2.2 translate() Function (lines 496-577)

```kotlin
fun translate() {
    viewModelScope.launch(Dispatchers.IO) {
        try {
            // 1. Extract paragraphs
            // 2. Build initial ArticleTranslation
            // 3. Set Translating state
            // 4. Create coordinator
            // 5. Collect channelFlow progress
            paragraphCoordinator.translateParagraphs(translatableTexts, targetLanguage)
                .collect { paragraphProgress ->
                    translationState.update { ... }
                }
        } catch (e: Exception) {
            translationState.value = TranslationState.Error(...)
        }
    }
}
```

**Key findings:**
- Also launches via `viewModelScope.launch(Dispatchers.IO)` -- **does NOT store the Job**
- Uses `ParagraphTranslationCoordinator.translateParagraphs()` which returns a `Flow<ParagraphTranslationProgress>`
- Progress updates via `translationState.update { }` atomically
- The `channelFlow` inside `ParagraphTranslationCoordinator` uses `Semaphore(3)` for concurrency
- **Cancellation risk:** When the Job is cancelled, the `collect` will throw `CancellationException` which propagates upward. The `catch (e: Exception)` block might catch it -- need to check if `CancellationException` is a subclass of `Exception` in Kotlin (it IS since Kotlin 1.4). This means **cancellation would be caught and set Error state instead of Empty**. Fix: either rethrow `CancellationException` or reset state in a `finally` block or `invokeOnCompletion`.

### 2.3 AISummaryState Sealed Interface (lines 710-718)

```kotlin
sealed interface AISummaryState {
    data object Empty : AISummaryState
    data object Loading : AISummaryState
    data class Result(val value: AIClient.SummaryResult) : AISummaryState
}
```

- No progress fraction for summarize -- **indeterminate** progress is correct
- `Result` wraps `AIClient.SummaryResult` which can be `Success` or `Error`

### 2.4 TranslationState Sealed Interface (lines 720-738)

```kotlin
sealed interface TranslationState {
    data object Empty : TranslationState
    data class Translating(val articleTranslation: ArticleTranslation) : TranslationState
    data class Translated(val articleTranslation: ArticleTranslation) : TranslationState
    data class Error(val errorMessage: String) : TranslationState
}
```

- `Translating` contains `ArticleTranslation` with `paragraphCompletedCount` and `paragraphTotalCount`
- **Determinate** progress fraction = `completedCount / totalCount` -- already computed in `TranslationStatusSection`

### 2.5 ViewState Flow (lines 127-190)

The `viewState` is built by combining 11 flows via `combine(...)`. Both `aiSummary` and `translationState` are direct `MutableStateFlow` fields already included.

**No ViewState changes needed** -- the existing `aiSummary: AISummaryState` and `translation: TranslationState` fields already carry all needed state to the UI.

---

## 3. Existing Composable Patterns

### 3.1 CircularProgressIndicator Usage

The project already uses `CircularProgressIndicator` in 4 files:
| File | Usage | Style |
|------|-------|-------|
| `AIProviderSection.kt:421` | Indeterminate, inside Button | Default size |
| `ProviderEditScreen.kt:122` | Indeterminate, centered in Box | Default + alignment |
| `ProviderEditScreen.kt:440` | Indeterminate, in save button | `height(24.dp)`, `strokeWidth = 2.dp` |
| `SearchFeedScreen.kt:546` | Indeterminate, standalone | Default |
| `PullToRefreshIndicator.kt:81` | Indeterminate, inside Surface | Custom color + strokeWidth |
| `SelectionMenuSettingsScreen.kt:282` | Indeterminate, inside Box | Default |

**Convention:** The project uses Material 3's `CircularProgressIndicator` directly with optional size/strokeWidth customization. No wrapper composable exists.

### 3.2 Custom Icons Directory

`app/src/main/java/com/nononsenseapps/feeder/ui/compose/icons/`:
- `CustomFilled.kt` -- defines `Icons.CustomFilled` extension object
- `TextToSpeech.kt` -- defines `CustomFilledIcons.TextToSpeech` ImageVector

**Convention for custom icons:** ImageVector defined via `materialIcon` + `materialPath` builders, accessed via `Icons.CustomFilled.XXX`. The new `CircleProgressIconButton` composable would NOT go here (it's a composable, not an icon vector). It should be a new file in the `feedarticle/` package or `components/` package.

### 3.3 PlainTooltipBox Pattern

Defined at `FeedScreen.kt:1655-1671`. Wraps `TooltipBox` with `PlainTooltip`. Used for all toolbar icon buttons in ArticleScreen.

**Impact:** The `PlainTooltipBox` wrapper around the icon buttons should change its tooltip text dynamically based on state (e.g., "Summarize" vs "Cancel summarize").

### 3.4 Components Directory

`app/src/main/java/com/nononsenseapps/feeder/ui/compose/components/`:
- `AutoCompleteText.kt`, `BottomAppBar.kt`, `ConfirmDialog.kt`, `OkCancel.kt`, `Utils.kt`

Small reusable composables live here. The new `CircleProgressIconButton` could go here, but since it's tightly coupled to ArticleScreen's toolbar, placing it in the same `feedarticle/` package is also valid.

---

## 4. Convention Assessment

### 4.1 Composable Parameter Structure

- **Individual parameters**, not data classes for callbacks (see `ArticleScreen` at line 159)
- ViewState is passed as a single `viewState: ArticleScreenViewState` interface
- Callbacks are individual `() -> Unit` lambdas
- `modifier: Modifier = Modifier` is always the last parameter

### 4.2 Material 3 Theme Tokens

From existing code:
- `MaterialTheme.colorScheme.primary` -- used for progress indicators (default)
- `MaterialTheme.colorScheme.onSurfaceVariant` -- used for secondary text
- `MaterialTheme.colorScheme.error` -- used for error borders/text
- `MaterialTheme.colorScheme.onSurface` -- suitable for stop square icon

### 4.3 Accessibility Patterns

- `contentDescription` on all `Icon` composables
- `stringResource(R.string.xxx)` for all user-visible strings (except the hardcoded "Translating article, please wait" at line 294)
- `safeSemantics { }` wrapper used for custom semantics (line 429)
- No `@Composable` preview functions in ArticleScreen.kt

### 4.4 String Resources

Existing relevant strings in `values/strings.xml`:
| Key | Value |
|-----|-------|
| `R.string.summarize` | "Summarize" |
| `R.string.summarizing_progress` | "Summarizing..." |
| `R.string.translate` | "Translate" |
| `R.string.translating_progress` | "Translating..." |
| `R.string.translate_article_content_description` | "Translate article content" |

**New strings needed:**
- `cancel_summarize` / `summarizing_tap_to_cancel` -- for accessibility during summarize progress
- `cancel_translation` / `translating_tap_to_cancel` -- for accessibility during translate progress
- Parameterized: `translating_progress_description` -- "Translating %1$d of %2$d paragraphs, tap to cancel"

---

## 5. Risk Areas

### 5.1 CancellationException in translate() (HIGH RISK)

**Location:** `ArticleViewModel.kt:572` (`catch (e: Exception)`)

In Kotlin, `CancellationException` extends `Exception` (since Kotlin 1.4 / kotlinx.coroutines). When `cancelTranslation()` cancels the Job:
1. The `collect` on the channelFlow will throw `CancellationException`
2. It will be caught by `catch (e: Exception)` at line 572
3. This sets `translationState` to `Error("Job was cancelled")` instead of `Empty`

**Fix needed:** Either:
- Add `if (e is CancellationException) throw e` before the error state set
- Or use `finally { }` + check `job.isCancelled` to reset to `Empty`
- Or set state to `Empty` in `cancelTranslation()` **before** cancelling the job (race-safe if using `MutableStateFlow`)

### 5.2 summarize() Also Has CancellationException Risk (MEDIUM RISK)

**Location:** `ArticleViewModel.kt:475` (`catch (e: Exception)`)

Same issue -- cancelling `summarizeJob` would cause:
1. `loadArticleContent()` or `aiApi.summarize()` to throw `CancellationException`
2. Caught by catch block, setting state to `Result(Error("Job was cancelled"))`

**Fix:** Same approaches as translate.

### 5.3 Race Condition: Cancel + Immediate Restart (LOW RISK)

If user cancels summarize and immediately taps again:
1. `cancelSummarize()` sets `aiSummary = Empty`, cancels old Job
2. `summarize()` launches new Job, sets `aiSummary = Loading`
3. Old Job's `finally`/catch might race with new state

**Mitigation:** Set state to `Empty` in cancel method, and in `summarize()`/`translate()` always assign a new Job to the field (overwriting old reference). The old Job is already cancelled so its state updates are no-ops after cancellation.

### 5.4 PlainTooltipBox During Progress State (LOW RISK)

The current code wraps each icon button in `PlainTooltipBox`. When replacing the `IconButton` content with a progress indicator composable, ensure the tooltip text updates and the clickable area remains the standard 48dp touch target.

---

## 6. Recommended New Composable Design

### CircleProgressIconButton

A reusable composable for the toolbar that switches between idle/progress states:

```
@Composable
fun CircleProgressIconButton(
    isInProgress: Boolean,
    progress: Float?,           // null = indeterminate, 0f..1f = determinate
    icon: ImageVector,
    contentDescription: String,
    progressContentDescription: String,
    onAction: () -> Unit,       // click when idle
    onCancel: () -> Unit,       // click when in progress (stop)
    modifier: Modifier = Modifier,
)
```

**Placement options:**
- `feedarticle/CircleProgressIconButton.kt` -- co-located with ArticleScreen (recommended, since it's specific to this feature)
- `components/CircleProgressIconButton.kt` -- if intended for reuse elsewhere

**Implementation approach:**
- Use `Box(modifier = Modifier.size(48.dp))` for consistent touch target
- Inside: `CircularProgressIndicator` (24dp) with `modifier = Modifier.align(Center)`
- Center: small `Icon(Icons.Filled.Stop)` or a `Canvas` drawing a filled square (~8dp)
- `IconButton` in idle state, `Box + clickable` in progress state

---

## 7. Files to Modify Summary

| File | Changes | Effort |
|------|---------|--------|
| `ArticleViewModel.kt:466-484` | Store Job in `summarizeJob`, add `cancelSummarize()`, handle CancellationException | Low |
| `ArticleViewModel.kt:496-577` | Store Job in `translateJob`, add `cancelTranslation()`, handle CancellationException | Low |
| `ArticleScreen.kt:89-152` | Add `onCancelSummarize`/`onCancelTranslation` to top-level composable | Low |
| `ArticleScreen.kt:159-205` | Pass new callbacks through public composable | Low |
| `ArticleScreen.kt:211-480` | Pass new callbacks through internal composable, replace icon buttons at lines 270-302 | Medium |
| `ArticleScreen.kt:534-547` | Update conditions for SummarySection/TranslationStatusSection rendering | Low |
| `ArticleScreen.kt:616-656` | Remove Loading branch from SummarySection | Low |
| `ArticleScreen.kt:695-721` | Remove Translating branch from TranslationStatusSection | Low |
| New: `CircleProgressIconButton.kt` | New composable for progress-aware icon button | Medium |
| `values/strings.xml` | Add 2-3 new accessibility strings | Low |

**Total: ~10 change points, mostly small, one new file**

---

## 8. Files Examined

- `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt` -- main UI with progress bars and icon buttons
- `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt` -- state management, coroutine launches
- `app/src/main/java/com/nononsenseapps/feeder/ai/ArticleTranslation.kt` -- progress fraction data model
- `app/src/main/java/com/nononsenseapps/feeder/ui/compose/icons/CustomFilled.kt` -- custom icon extension pattern
- `app/src/main/java/com/nononsenseapps/feeder/ui/compose/icons/TextToSpeech.kt` -- custom icon implementation pattern
- `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feed/FeedScreen.kt:1655-1671` -- PlainTooltipBox pattern
- `app/src/main/java/com/nononsenseapps/feeder/ui/compose/settings/ProviderEditScreen.kt` -- CircularProgressIndicator usage pattern
- `app/src/main/java/com/nononsenseapps/feeder/ui/compose/pullrefresh/PullToRefreshIndicator.kt` -- CircularProgressIndicator usage pattern
- `app/src/main/res/values/strings.xml` -- existing string resources
- `specification/033-circle-progress-icon/01-requirements.md` -- requirements document
- `specification/033-circle-progress-icon/01.1-behavior-scenarios.md` -- BDD scenarios
