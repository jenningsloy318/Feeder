# Technical Specification: Circle Progress Icon for Summarize/Translate

**Date:** 2026-03-17
**Author:** Claude
**Status:** Draft

## 1. Overview

### 1.1 Summary
Replace inline `LinearProgressIndicator` cards in article content with circular progress indicators directly on the summarize/translate toolbar icons. When an operation is in progress, the icon transforms into a circular progress ring with a small stop square in the center (Apple App Store style). Tapping the progress icon cancels the operation.

### 1.2 Goals
- Remove progress bars from article content area so users can read undisturbed
- Show operation progress directly on toolbar icons (indeterminate for summarize, determinate for translate)
- Enable cancellation of in-progress summarize/translate operations via the stop square
- Maintain existing result/error display cards in article content

### 1.3 Non-Goals
- Animated transitions between idle and progress states (simple swap, no `AnimatedContent`)
- Custom Canvas drawing (use Material 3 `CircularProgressIndicator`)
- Changes to the AI summary result card or translation error card rendering
- Changes to inline translated paragraph display

## 2. Background

### 2.1 Context
> From Research Report: Material 3's `CircularProgressIndicator` supports both determinate and indeterminate variants. Best approach is `Box(contentAlignment = Center)` layering `CircularProgressIndicator` + small filled square. Job cancellation uses `private var job: Job? = null` pattern with `CancellationException` rethrow.

### 2.2 Current State
> From Assessment: Both `summarize()` and `translate()` in `ArticleViewModel.kt` launch coroutines without storing Job references. No cancellation support exists. `CancellationException` extends `Exception` in Kotlin, so current `catch (e: Exception)` blocks would incorrectly set error state on cancellation. The project already uses `CircularProgressIndicator` in 4+ other files.

## 3. Technical Design

### 3.1 Architecture

```
┌─────────────────────────┐
│   ArticleScreen.kt      │
│ (toolbar actions area)   │
│                         │
│ CircleProgressIconButton │──── New composable
│ (summarize + translate)  │
└──────────┬──────────────┘
           │ onCancel callbacks
           ▼
┌─────────────────────────┐
│  ArticleViewModel.kt    │
│                         │
│ summarizeJob: Job?      │──── New Job tracking
│ translateJob: Job?      │
│ cancelSummarize()       │──── New cancel methods
│ cancelTranslation()     │
└─────────────────────────┘
```

### 3.2 Components

#### Component 1: CircleProgressIconButton

- **Purpose:** Reusable toolbar icon that switches between idle (normal icon) and in-progress (circular progress ring + stop square) states
- **File Location:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/CircleProgressIconButton.kt`

```kotlin
@Composable
fun CircleProgressIconButton(
    isInProgress: Boolean,
    progressFraction: (() -> Float)?,  // null = indeterminate, lambda = determinate
    icon: ImageVector,
    idleContentDescription: String,
    progressContentDescription: String,
    onAction: () -> Unit,              // called when idle (start operation)
    onCancel: () -> Unit,              // called when in-progress (stop operation)
    modifier: Modifier = Modifier,
)
```

**Rendering logic:**
- When `isInProgress == false`: render standard `IconButton(onClick = onAction)` containing `Icon(icon, contentDescription = idleContentDescription)`
- When `isInProgress == true`: render `Box(Modifier.size(48.dp).clickable(onClick = onCancel, role = Role.Button))` containing:
  - `CircularProgressIndicator` sized to `24.dp` with `strokeWidth = 2.dp`
  - If `progressFraction != null`: determinate variant with `progress = progressFraction`, `trackColor = MaterialTheme.colorScheme.surfaceVariant`, `gapSize = 0.dp`
  - If `progressFraction == null`: indeterminate variant
  - Centered `Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.onSurface, RoundedCornerShape(1.dp)))` as stop square
  - Semantics: `contentDescription = progressContentDescription`, `role = Role.Button`

#### Component 2: ViewModel Job Tracking

- **Purpose:** Store coroutine Job references and provide cancel methods
- **File Location:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt`

New fields (near line 123-125, alongside `aiSummary` and `translationState`):
```kotlin
private var summarizeJob: Job? = null
private var translateJob: Job? = null
```

New methods:
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

Modified `summarize()` (line 466-484):
```kotlin
fun summarize() {
    summarizeJob?.cancel()
    summarizeJob = viewModelScope.launch(Dispatchers.IO) {
        try {
            aiSummary.value = AISummaryState.Loading
            val content = loadArticleContent()
            aiSummary.value = AISummaryState.Result(
                value = aiApi.summarize(content),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            aiSummary.value = AISummaryState.Result(
                value = AIClient.SummaryResult.Error(content = e.message ?: "Unknown error"),
            )
        }
    }
}
```

Modified `translate()` (line 496-577):
```kotlin
fun translate() {
    translateJob?.cancel()
    translateJob = viewModelScope.launch(Dispatchers.IO) {
        try {
            // ... existing logic unchanged ...
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            translationState.value = TranslationState.Error(
                errorMessage = e.message ?: "Translation failed",
            )
        }
    }
}
```

### 3.3 State Flow

```
Summarize Icon States:

  ┌──────┐  onAction()   ┌──────────┐  complete   ┌──────┐
  │ IDLE │──────────────▶│ PROGRESS │───────────▶│ IDLE │
  │(icon)│               │(spinner) │            │(icon)│
  └──────┘               └────┬─────┘            └──────┘
                              │ onCancel()
                              ▼
                         ┌──────┐
                         │ IDLE │
                         │(icon)│
                         └──────┘

  State mapping:
  - AISummaryState.Empty       → IDLE
  - AISummaryState.Loading     → PROGRESS (indeterminate)
  - AISummaryState.Result      → IDLE


Translate Icon States:

  ┌──────┐  onAction()   ┌──────────┐  complete   ┌──────┐
  │ IDLE │──────────────▶│ PROGRESS │───────────▶│ IDLE │
  │(icon)│               │(fraction)│            │(icon)│
  └──────┘               └────┬─────┘            └──────┘
                              │ onCancel()
                              ▼
                         ┌──────┐
                         │ IDLE │
                         │(icon)│
                         └──────┘

  State mapping:
  - TranslationState.Empty       → IDLE
  - TranslationState.Translating → PROGRESS (determinate: completed/total)
  - TranslationState.Translated  → IDLE
  - TranslationState.Error       → IDLE
```

### 3.4 ArticleScreen Changes

#### Toolbar Icon Replacement (lines 270-302)

Replace the summarize `IconButton` block (lines 270-281) with:
```kotlin
if (viewState.showSummarize) {
    val isSummarizing = viewState.aiSummary is AISummaryState.Loading
    PlainTooltipBox(
        tooltip = {
            Text(
                stringResource(
                    if (isSummarizing) R.string.cancel_summarize
                    else R.string.summarize,
                ),
            )
        },
    ) {
        CircleProgressIconButton(
            isInProgress = isSummarizing,
            progressFraction = null,
            icon = Icons.Default.AutoFixHigh,
            idleContentDescription = stringResource(R.string.summarize),
            progressContentDescription = stringResource(R.string.summarizing_tap_to_cancel),
            onAction = onSummarize,
            onCancel = onCancelSummarize,
        )
    }
}
```

Replace the translate `IconButton` block (lines 283-302) with:
```kotlin
if (viewState.showSummarize) {
    val isTranslating = viewState.translation is TranslationState.Translating
    val translationProgressFraction: (() -> Float)? = if (isTranslating) {
        val articleTranslation = (viewState.translation as TranslationState.Translating).articleTranslation
        val completed = articleTranslation.paragraphCompletedCount
        val total = articleTranslation.paragraphTotalCount
        { if (total > 0) completed.toFloat() / total else 0f }
    } else {
        null
    }
    PlainTooltipBox(
        tooltip = {
            Text(
                stringResource(
                    if (isTranslating) R.string.cancel_translation
                    else R.string.translate,
                ),
            )
        },
    ) {
        CircleProgressIconButton(
            isInProgress = isTranslating,
            progressFraction = translationProgressFraction,
            icon = Icons.Default.Translate,
            idleContentDescription = stringResource(R.string.translate_article_content_description),
            progressContentDescription = if (isTranslating) {
                val articleTranslation = (viewState.translation as TranslationState.Translating).articleTranslation
                stringResource(
                    R.string.translating_x_of_y_tap_to_cancel,
                    articleTranslation.paragraphCompletedCount,
                    articleTranslation.paragraphTotalCount,
                )
            } else {
                stringResource(R.string.translate_article_content_description)
            },
            onAction = onTranslate,
            onCancel = onCancelTranslation,
        )
    }
}
```

#### Callback Chain (3 layers)

Add two new callbacks through the composable chain:

**Layer 1 - Top-level `ArticleScreen` (line 89-153):**
Add to the inner `ArticleScreen(...)` call:
```kotlin
onCancelSummarize = { viewModel.cancelSummarize() },
onCancelTranslation = { viewModel.cancelTranslation() },
```

**Layer 2 - Public stateless `ArticleScreen` (line 159-205):**
Add parameters:
```kotlin
onCancelSummarize: () -> Unit,
onCancelTranslation: () -> Unit,
```
Pass through to `ArticleScreenInternal(...)`.

**Layer 3 - Private `ArticleScreenInternal` (line 211-480):**
Add same parameters. Used directly in the toolbar actions area at lines 270-302.

#### Remove Inline Progress Bars

**SummarySection (lines 616-656):**
Remove the `AISummaryState.Loading` branch (lines 622-636). The composable only renders for `Result` state now.

Update the LazyColumn condition (line 534) from:
```kotlin
if (viewState.aiSummary !is AISummaryState.Empty) {
```
to:
```kotlin
if (viewState.aiSummary is AISummaryState.Result) {
```

**TranslationStatusSection (lines 695-736):**
Remove the entire `TranslationState.Translating` branch (lines 699-721).

Update the LazyColumn condition (line 542) from:
```kotlin
if (viewState.translation !is TranslationState.Empty) {
```
to:
```kotlin
if (viewState.translation is TranslationState.Translated || viewState.translation is TranslationState.Error) {
```

### 3.5 String Resources

New strings in `app/src/main/res/values/strings.xml`:

```xml
<string name="cancel_summarize">Cancel summarize</string>
<string name="summarizing_tap_to_cancel">Summarizing in progress, tap to cancel</string>
<string name="cancel_translation">Cancel translation</string>
<string name="translating_x_of_y_tap_to_cancel">Translating %1$d of %2$d paragraphs, tap to cancel</string>
```

### 3.6 Import Additions

**CircleProgressIconButton.kt** needs:
- `androidx.compose.foundation.background`
- `androidx.compose.foundation.clickable`
- `androidx.compose.foundation.layout.Box`
- `androidx.compose.foundation.layout.size`
- `androidx.compose.foundation.shape.RoundedCornerShape`
- `androidx.compose.material3.CircularProgressIndicator`
- `androidx.compose.material3.Icon`
- `androidx.compose.material3.IconButton`
- `androidx.compose.material3.MaterialTheme`
- `androidx.compose.ui.Alignment`
- `androidx.compose.ui.graphics.vector.ImageVector`
- `androidx.compose.ui.semantics.Role`
- `androidx.compose.ui.semantics.contentDescription`
- `androidx.compose.ui.semantics.role`
- `androidx.compose.ui.semantics.semantics`
- `androidx.compose.ui.unit.dp`

**ArticleScreen.kt** adds:
- `import androidx.compose.material3.CircularProgressIndicator` (remove `LinearProgressIndicator` import if unused)

**ArticleViewModel.kt** adds:
- `import kotlinx.coroutines.CancellationException`
- `import kotlinx.coroutines.Job`

## 4. Testing Strategy

### 4.1 Unit Tests

**File:** `app/src/test/java/com/nononsenseapps/feeder/ui/compose/feedarticle/CircleProgressIconButtonTest.kt`

| Test Name | What It Verifies | Scenarios |
|-----------|-----------------|-----------|
| `idleState_showsIconButton` | Icon and content description visible when not in progress | SCENARIO-001, SCENARIO-006 |
| `indeterminateProgress_showsSpinnerAndStopSquare` | CircularProgressIndicator (no progress param) + stop square visible | SCENARIO-001 |
| `determinateProgress_showsFractionAndStopSquare` | CircularProgressIndicator with progress fraction + stop square visible | SCENARIO-006, SCENARIO-007 |
| `idleState_clickCallsOnAction` | Clicking idle icon fires onAction callback | SCENARIO-005 |
| `progressState_clickCallsOnCancel` | Clicking progress icon fires onCancel callback | SCENARIO-002, SCENARIO-008 |
| `progressContentDescription_isSet` | Accessibility content description matches progressContentDescription | SCENARIO-013, SCENARIO-014 |

### 4.2 ViewModel Cancel Tests

**File:** `app/src/test/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModelCancelTest.kt` (or add to existing test file)

| Test Name | What It Verifies | Scenarios |
|-----------|-----------------|-----------|
| `cancelSummarize_resetsStateToEmpty` | Calling cancelSummarize sets aiSummary to Empty | SCENARIO-002 |
| `cancelTranslation_resetsStateToEmpty` | Calling cancelTranslation sets translationState to Empty | SCENARIO-008 |
| `summarize_storesJob` | After calling summarize(), summarizeJob is non-null | SCENARIO-001 |
| `translate_storesJob` | After calling translate(), translateJob is non-null | SCENARIO-006 |
| `cancellationException_notCaughtAsError` | CancellationException is rethrown, not turned into error state | SCENARIO-002, SCENARIO-008 |

### 4.3 BDD Scenario References

| Scenario ID | Title | Test Type | Test Location |
|-------------|-------|-----------|---------------|
| SCENARIO-001 | Summarize shows circular progress | Unit | CircleProgressIconButtonTest |
| SCENARIO-002 | Cancel summarize returns to idle | Unit | CircleProgressIconButtonTest, ArticleViewModelCancelTest |
| SCENARIO-003 | Completed summarize restores idle icon | Unit | CircleProgressIconButtonTest |
| SCENARIO-004 | Summarize error restores idle icon | Unit | CircleProgressIconButtonTest |
| SCENARIO-005 | Re-trigger summarize after completion | Unit | CircleProgressIconButtonTest |
| SCENARIO-006 | Translate shows determinate progress | Unit | CircleProgressIconButtonTest |
| SCENARIO-007 | Translate progress reflects fraction | Unit | CircleProgressIconButtonTest |
| SCENARIO-008 | Cancel translation returns to idle | Unit | CircleProgressIconButtonTest, ArticleViewModelCancelTest |
| SCENARIO-009 | Completed translation restores idle icon | Unit | CircleProgressIconButtonTest |
| SCENARIO-010 | Translation error restores idle icon | Unit | CircleProgressIconButtonTest |
| SCENARIO-011 | Concurrent summarize and translate | Unit | ArticleViewModelCancelTest |
| SCENARIO-012 | Rapid cancel and restart | Unit | ArticleViewModelCancelTest |
| SCENARIO-013 | Summarize accessibility | Unit | CircleProgressIconButtonTest |
| SCENARIO-014 | Translate accessibility | Unit | CircleProgressIconButtonTest |
| SCENARIO-015 | Progress fits toolbar dimensions | Unit | CircleProgressIconButtonTest |

## 5. Security Considerations

No security implications. This change is purely UI/UX with coroutine lifecycle management. No user input, network, or data storage changes.

## 6. Performance Considerations

- `CircularProgressIndicator` uses Compose's built-in animation system (no custom animation overhead)
- Determinate progress uses `() -> Float` lambda form (deferred read, avoids unnecessary recomposition)
- Job cancellation is cooperative via structured concurrency — cancels at next suspension point
- `channelFlow` inside `ParagraphTranslationCoordinator` respects parent Job cancellation automatically

## 7. References

- Requirements: `./01-requirements.md`
- BDD Scenarios: `./01.1-behavior-scenarios.md`
- Research Report: `./02-research-report.md`
- Code Assessment: `./03-code-assessment.md`
