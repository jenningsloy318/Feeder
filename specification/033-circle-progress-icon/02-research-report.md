# Research Report: Circle Progress Icon for Summarize/Translate

**Date:** 2026-03-17
**Technologies:** Kotlin, Jetpack Compose, Material 3, Coroutines
**Freshness Score:** 85% of sources < 1 year old

## Summary

- Material 3's `CircularProgressIndicator` supports both determinate and indeterminate variants with customizable `strokeWidth`, `color`, `trackColor`, `gapSize`, and `strokeCap`
- Sizing is controlled via `Modifier.size()` — use `24.dp` for icon area, wrap in `48.dp` clickable Box for touch target
- Best approach for progress+stop overlay: `Box(contentAlignment = Center)` layering `CircularProgressIndicator` + small filled square `Icon`/`Canvas` — simpler and more maintainable than pure Canvas
- Job cancellation pattern: store `private var summarizeJob: Job? = null` in ViewModel, assign from `viewModelScope.launch`, cancel with `job?.cancel()` + state reset
- Accessibility: use `Modifier.semantics { contentDescription = "..." }` on the wrapping `Box`, plus `Role.Button` for clickability

## Options Comparison

### Option 1: Material 3 CircularProgressIndicator + Box Overlay

**Description:** Use the built-in `CircularProgressIndicator` composable sized to 24.dp, layered in a `Box` with a small stop square on top.

**Strengths:**
- Uses official Material 3 API — animation, theming, and accessibility built-in
- Minimal code: ~30 lines for the reusable composable
- Indeterminate/determinate switching is just whether you pass `progress` parameter or not
- Automatically follows M3 color tokens (`MaterialTheme.colorScheme.primary`)

**Weaknesses:**
- Default `CircularProgressIndicator` min size is 48.dp (need `Modifier.size(24.dp)` to override)
- Default stroke width (4.dp) may look thick at 24.dp — need to reduce to ~2.dp
- `gapSize` parameter (M3 1.2+) adds a gap between track and indicator that may need to be set to 0.dp

**Best For:**
- This project — standard M3 look, minimal code, proven API

**Implementation Pattern:**
```kotlin
@Composable
fun CircleProgressIconButton(
    isInProgress: Boolean,
    progress: (() -> Float)? = null, // null = indeterminate
    onClick: () -> Unit, // action or cancel
    icon: @Composable () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    if (isInProgress) {
        Box(
            modifier = modifier
                .size(48.dp)
                .clickable(onClick = onClick)
                .semantics {
                    this.contentDescription = contentDescription
                    this.role = Role.Button
                },
            contentAlignment = Alignment.Center,
        ) {
            if (progress != null) {
                CircularProgressIndicator(
                    progress = progress,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    gapSize = 0.dp,
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                )
            }
            // Stop square icon
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurface,
                        RoundedCornerShape(1.dp),
                    ),
            )
        }
    } else {
        IconButton(onClick = onClick) {
            icon()
        }
    }
}
```

### Option 2: Pure Canvas Drawing

**Description:** Draw the progress ring and stop square entirely using Canvas `drawArc` and `drawRect`.

**Strengths:**
- Full pixel control over sizing, stroke, colors, and animation
- No dependency on M3 component internals
- Can create exact Apple-style appearance

**Weaknesses:**
- Must manually implement indeterminate animation (infinite rotation transition)
- More code (~80+ lines) and harder to maintain
- Must manually handle accessibility semantics
- Must manually apply M3 color tokens

**Best For:**
- Highly custom designs that deviate significantly from Material 3

### Option 3: AnimatedContent Transition Between Icon and Progress

**Description:** Use `AnimatedContent` or `Crossfade` to transition between the normal `IconButton` and the progress composable.

**Strengths:**
- Smooth animated transition between idle and progress states
- Clean separation of states

**Weaknesses:**
- Adds animation complexity that may not be needed (state change is immediate/intentional)
- `AnimatedContent` inside TopAppBar actions can cause layout measurement issues
- Extra overhead for what is essentially a binary state switch

**Best For:**
- UIs where the transition between states should be visually smooth

### Comparison Matrix

| Criteria              | Option 1: M3 + Box | Option 2: Canvas | Option 3: AnimatedContent |
|-----------------------|---------------------|-------------------|--------------------------|
| Code Simplicity       | High                | Low               | Medium                   |
| Visual Accuracy       | High                | Very High         | High                     |
| Maintainability       | High                | Medium            | Medium                   |
| M3 Consistency        | Built-in            | Manual            | Built-in                 |
| Accessibility         | Built-in + manual   | Fully manual      | Built-in + manual        |
| Performance           | Optimized           | Optimized         | More recomposition       |
| Animation Quality     | Built-in spin       | Manual            | Extra transitions        |

### Recommendation

**Recommended:** Option 1 - Material 3 CircularProgressIndicator + Box Overlay

**Rationale:** This project uses Material 3 throughout. The built-in `CircularProgressIndicator` handles animation, theming, and accessibility with minimal code. The Box overlay pattern for the stop square is well-established and straightforward.

**Trade-offs:** Gives up pixel-perfect Apple-style appearance in favor of M3-native look and zero animation code.

## Key API Details

### CircularProgressIndicator (Material 3)

**Determinate variant** (for translate with progress fraction):
```kotlin
CircularProgressIndicator(
    progress = { completedCount.toFloat() / totalCount },
    modifier = Modifier.size(24.dp),
    color = MaterialTheme.colorScheme.primary,
    strokeWidth = 2.dp,
    trackColor = MaterialTheme.colorScheme.surfaceVariant,
    strokeCap = StrokeCap.Round,
    gapSize = 0.dp,
)
```

**Indeterminate variant** (for summarize):
```kotlin
CircularProgressIndicator(
    modifier = Modifier.size(24.dp),
    color = MaterialTheme.colorScheme.primary,
    strokeWidth = 2.dp,
)
```

**Key parameters:**
| Parameter | Type | Default | Notes |
|-----------|------|---------|-------|
| `progress` | `() -> Float` | (none = indeterminate) | Lambda for determinate, omit for indeterminate |
| `modifier` | `Modifier` | — | Use `.size(24.dp)` to fit toolbar icon area |
| `strokeWidth` | `Dp` | `4.dp` | Reduce to `2.dp` for 24dp circle |
| `trackColor` | `Color` | `surfaceVariant` | Background ring color |
| `gapSize` | `Dp` | `4.dp` | Set to `0.dp` for continuous ring |
| `strokeCap` | `StrokeCap` | `Round` (determinate) | Round ends look better at small sizes |

### Sizing for Toolbar Icons

- Standard Material 3 icon button: **48dp** touch target, **24dp** icon visual area
- `CircularProgressIndicator` needs `Modifier.size(24.dp)` to match icon visual area
- Wrap in `Box(Modifier.size(48.dp))` with `.clickable()` for full touch target
- Stop square: **8dp** with **1dp** corner radius for subtle rounding

## ViewModel Job Cancellation Pattern

### Recommended Pattern

```kotlin
class ArticleViewModel : ViewModel() {
    private var summarizeJob: Job? = null
    private var translateJob: Job? = null

    fun summarize() {
        summarizeJob?.cancel() // Cancel previous if running
        summarizeJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                aiSummary.value = AISummaryState.Loading
                // ... existing logic
            } catch (e: CancellationException) {
                // Don't set error state on cancellation — state is reset by cancelSummarize()
                throw e // Always rethrow CancellationException
            } catch (e: Exception) {
                aiSummary.value = AISummaryState.Result(
                    value = SummaryResult.Error(content = e.message ?: "Unknown error"),
                )
            }
        }
    }

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
}
```

### Critical Rules

1. **Always rethrow `CancellationException`** — catching it silently creates zombie coroutines
2. **Separate `CancellationException` from general `Exception`** in catch blocks — cancellation is not an error
3. **Reset state in the cancel method, not in the catch block** — the catch block may not execute immediately
4. **Set Job reference to null after cancellation** — prevents stale reference issues
5. **Cancel previous job before starting new one** — `summarizeJob?.cancel()` at start of `summarize()` handles rapid re-trigger
6. **`Dispatchers.IO` coroutines are cooperative** — the AI API calls use suspend functions (OkHttp/Ktor) that check for cancellation at suspension points, so `cancel()` will propagate correctly
7. **`channelFlow` in translate** — cancelling the parent job cancels the `channelFlow` collector, which cancels the underlying coroutines via structured concurrency

### channelFlow Cancellation (Translation-specific)

The existing `ParagraphTranslationCoordinator.translateParagraphs()` returns a `Flow` built with `channelFlow`. When the collecting coroutine (the translate Job) is cancelled:
- The `channelFlow` builder's scope is cancelled
- All child coroutines launched inside `channelFlow` (the parallel paragraph translations) are cancelled
- The `Semaphore` releases automatically — no cleanup needed

## Accessibility

### Semantics for Custom Progress Composable

```kotlin
Box(
    modifier = Modifier
        .size(48.dp)
        .clickable(
            onClick = onCancel,
            role = Role.Button,
        )
        .semantics {
            contentDescription = "Summarizing in progress, tap to cancel"
            // For determinate progress, also add:
            progressBarRangeInfo = ProgressBarRangeInfo(
                current = completedCount.toFloat(),
                range = 0f..totalCount.toFloat(),
            )
        },
    contentAlignment = Alignment.Center,
) { ... }
```

### Content Descriptions per State

| State | Content Description |
|-------|-------------------|
| Summarize idle | "Summarize article" |
| Summarize in progress | "Summarizing in progress, tap to cancel" |
| Translate idle | "Translate article" |
| Translate in progress | "Translating X of Y paragraphs, tap to cancel" |

### Key Accessibility Points

- Use `Role.Button` so TalkBack announces it as a button
- Use `progressBarRangeInfo` for determinate progress so TalkBack announces progress percentage
- Dynamic content description updates as progress changes
- `clickable()` modifier automatically handles focus and keyboard activation

## Anti-Patterns to Avoid

1. **Don't use `GlobalScope`** for cancellable operations — use `viewModelScope` for lifecycle awareness
2. **Don't catch `CancellationException` as `Exception`** — always separate them:
   ```kotlin
   // BAD
   catch (e: Exception) { handleError(e) }

   // GOOD
   catch (e: CancellationException) { throw e }
   catch (e: Exception) { handleError(e) }
   ```
3. **Don't cancel `viewModelScope` itself** — only cancel individual Jobs. Cancelling the scope prevents launching new coroutines
4. **Don't use `Modifier.drawBehind` for the progress ring** when `CircularProgressIndicator` exists — it duplicates animation logic
5. **Don't use `AnimatedVisibility` for the icon swap** inside `TopAppBar` actions — it can cause measurement issues
6. **Don't size the `CircularProgressIndicator` larger than 24dp** in toolbar — it will overflow the icon action area

## Edge Cases to Handle

1. **Rapid cancel + restart**: Cancel previous job at the start of `summarize()`/`translate()` — `summarizeJob?.cancel()` before launching new one
2. **CancellationException during state update**: The `translationState.update {}` lambda may throw if the coroutine is cancelled mid-update — wrap with try-catch or rely on structured concurrency
3. **Both operations running simultaneously**: Use separate Job references (`summarizeJob`, `translateJob`) — they are independent
4. **Cancel during `channelFlow` collection**: Structured concurrency handles this — all child coroutines are cancelled when the parent is cancelled
5. **State race condition**: Use `MutableStateFlow.update {}` (atomic) instead of `.value =` where possible for state transitions

## Existing Code Integration Points

| File | Line | Current Code | Change Needed |
|------|------|-------------|---------------|
| `ArticleViewModel.kt` | 123 | `private val aiSummary: MutableStateFlow<AISummaryState>` | Add `private var summarizeJob: Job? = null` nearby |
| `ArticleViewModel.kt` | 125 | `private val translationState: MutableStateFlow<TranslationState>` | Add `private var translateJob: Job? = null` nearby |
| `ArticleViewModel.kt` | 466-484 | `fun summarize()` | Store Job, add CancellationException handling |
| `ArticleViewModel.kt` | 496-577 | `fun translate()` | Store Job, add CancellationException handling |
| `ArticleViewModel.kt` | (new) | — | Add `cancelSummarize()` and `cancelTranslation()` methods |
| `ArticleScreen.kt` | 270-281 | Summarize `IconButton` | Replace with `CircleProgressIconButton` |
| `ArticleScreen.kt` | 283-302 | Translate `IconButton` | Replace with `CircleProgressIconButton` |
| `ArticleScreen.kt` | 616-656 | `SummarySection` | Remove Loading state's LinearProgressIndicator |
| `ArticleScreen.kt` | 695-736 | `TranslationStatusSection` | Remove Translating state card |
| (new file) | — | — | Create `CircleProgressIconButton.kt` composable |

## Sources

| # | Title | URL | Published | Freshness |
|---|-------|-----|-----------|-----------|
| 1 | ProgressIndicator in Compose: Loading States, Custom Progress & Overlays | dev.to | 2026-03-02 | Fresh |
| 2 | Progress indicators - Android Developers | developer.android.com/develop/ui/compose/components/progress | 2025 | Fresh |
| 3 | CircularProgressIndicator - Material 3 Compose API | kotlinlang.org/api/compose-multiplatform/material3 | 2025 | Fresh |
| 4 | Mastering ViewModelScope Cancellation in Kotlin Coroutines | medium.com | 2025-07-31 | Fresh |
| 5 | Mastering Coroutine Cancellation in Kotlin | proandroiddev.com | 2025-06-12 | Fresh |
| 6 | ViewModelScope Internals: A Deep Dive | droidcon.com | 2025-12-11 | Fresh |
| 7 | Kotlin Coroutine Cancellation: An Advanced Guide | medium.com (omaroid) | 2025 | Fresh |
| 8 | Cancellation and timeouts - Kotlin Documentation | kotlinlang.org/docs/cancellation-and-timeouts | 2025 | Fresh |
| 9 | Accessibility in Jetpack Compose | developer.android.com | 2026-01-16 | Fresh |
| 10 | Circular progressbar as trailing icon | stackoverflow.com | 2024-11-20 | Current |
| 11 | Custom Rounded Square Progress Indicator with Icon | stackoverflow.com | 2024-10-04 | Current |
| 12 | How to make CircularProgressIndicator smaller | stackoverflow.com | 2022-10-27 | Dated |

### Source Freshness Summary
- Fresh (< 6 months): 9 sources
- Current (6-12 months): 2 sources
- Dated (1-2 years): 1 source
- Potentially Outdated (> 2 years): 0 sources

## Deprecation Warnings

None identified. `CircularProgressIndicator` in Material 3 Compose is the current recommended API. The `progress` parameter changed from a `Float` to a `() -> Float` lambda in recent M3 versions (1.2+) — ensure using the lambda form.
