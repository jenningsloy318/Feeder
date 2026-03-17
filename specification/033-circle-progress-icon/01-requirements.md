# Requirements: Circle Progress Icon for Summarize/Translate

**Date:** 2026-03-17
**Type:** Improvement
**Priority:** Medium

## Executive Summary

Replace the inline `LinearProgressIndicator` cards (progress bars embedded in article content) for summarize and translate operations with circular progress indicators directly on their respective toolbar icons. When an operation is in progress, the icon transforms into a circular progress ring with a small filled square in the center (similar to Apple App Store install progress), allowing users to stop the operation. After completion, the icon returns to its normal clickable state.

## The Real Need

### Surface Request
Delete the progress bar sections (SummarySection loading state and TranslationStatusSection) from the article content area. Instead, show progress directly on the summarize/translate icons in the top app bar.

### Job to Be Done
**When** I trigger summarize or translate on an article,
**I want to** see progress directly on the action icon without extra UI taking up content space,
**So I can** continue reading the article undisturbed while knowing the operation status at a glance, and stop it if needed.

## Current Behavior (What to Remove/Change)

### 1. Summarize Progress Bar
**File:** `ArticleScreen.kt:616-656` (`SummarySection` composable)
- **Loading state** (lines 622-636): Shows an `OutlinedCard` with "Summarizing..." text and an **indeterminate** `LinearProgressIndicator` in the article content area
- **Result state** (lines 637-653): Shows markdown-rendered summary text in a card
- **Placement:** Inserted as a LazyList item at `ArticleScreen.kt:534-538`

### 2. Translation Progress Bar
**File:** `ArticleScreen.kt:695-736` (`TranslationStatusSection` composable)
- **Translating state** (lines 699-721): Shows an `OutlinedCard` with "X/Y paragraphs translated" text and a **determinate** `LinearProgressIndicator(progress = completedCount/totalCount)` in the article content area
- **Translated state** (lines 724-731): Shows error section if paragraphs failed
- **Error state** (lines 733-734): Shows error section with message
- **Placement:** Inserted as a LazyList item at `ArticleScreen.kt:542-547`

### 3. Current Summarize Icon Button
**File:** `ArticleScreen.kt:270-281`
- `IconButton` with `Icons.Default.AutoFixHigh` icon
- Always enabled; clicking triggers `viewModel.summarize()`
- No visual feedback during loading (icon stays the same)
- Shown conditionally when `viewState.showSummarize` is true

### 4. Current Translate Icon Button
**File:** `ArticleScreen.kt:283-302`
- `IconButton` with `Icons.Default.Translate` icon
- **Already disabled** during translation (`enabled = !isTranslationInProgress` at line 289)
- Content description changes to "Translating article, please wait" during progress
- Shown conditionally when `viewState.showSummarize` is true

### 5. State Classes
**File:** `ArticleViewModel.kt:710-738`
- `AISummaryState`: `Empty | Loading | Result(SummaryResult)` - **no progress fraction** for summary (indeterminate)
- `TranslationState`: `Empty | Translating(ArticleTranslation) | Translated(ArticleTranslation) | Error(errorMessage)` - **has progress fraction** via `paragraphCompletedCount / paragraphTotalCount`

### 6. No Cancel Support Currently
- `ArticleViewModel.kt:466-484` (`summarize()`): Launches coroutine via `viewModelScope.launch` but does **not** store the `Job` reference
- `ArticleViewModel.kt:496-577` (`translate()`): Same - no stored `Job` reference, no cancellation mechanism

## Desired Behavior (Three States per Icon)

Reference UX: Apple App Store install button behavior.

### State 1: Idle (default)
- Normal icon button (AutoFixHigh for summarize, Translate for translate)
- Clickable - triggers the operation
- Shown when state is `Empty`, `Result`/`Translated`, or `Error`

### State 2: In-Progress (circle progress with stop square)
- The icon is replaced by a **circular progress indicator** (ring/arc)
  - **Summarize**: Indeterminate circular progress (no fraction available from `AISummaryState.Loading`)
  - **Translate**: Determinate circular progress showing `completedCount / totalCount` fraction
- A **small filled square** is drawn in the center of the circle (stop/cancel affordance)
- Tapping the circle+square **cancels** the operation
- The button is **not** an `IconButton` in this state - it's a custom composable (Box with Canvas or CircularProgressIndicator + stop icon)

### State 3: Completed / Error
- Returns to idle icon (State 1)
- Clickable again
- Summary result card and translation error cards remain in the article content (only the **progress bars** are removed, not the result display)

## Requirements

### Functional Requirements

#### FR-1: Remove Inline Progress Bars
- Remove the `LinearProgressIndicator` from `SummarySection` loading state (lines 632-635)
- Remove the entire `TranslationStatusSection` `Translating` state card with `LinearProgressIndicator` (lines 699-721)
- Keep `SummarySection` `Result` state (summary display card) intact
- Keep `TranslationStatusSection` `Translated` error display and `Error` state intact

#### FR-2: Circular Progress on Summarize Icon
- When `aiSummary` is `Loading`, replace the `AutoFixHigh` icon with an indeterminate circular progress indicator
- The progress circle should be sized to fit in the toolbar icon space (24dp icon area)
- A small filled square (stop icon) should appear in the center
- Tapping cancels the summarize coroutine and resets state to `Empty`

#### FR-3: Circular Progress on Translate Icon
- When `translation` is `Translating`, replace the `Translate` icon with a determinate circular progress indicator
- Progress fraction = `articleTranslation.paragraphCompletedCount / articleTranslation.paragraphTotalCount`
- A small filled square (stop icon) should appear in the center
- Tapping cancels the translate coroutine and resets state to `Empty`

#### FR-4: Cancel Support in ViewModel
- Store the `Job` reference from `viewModelScope.launch` in both `summarize()` and `translate()`
- Add `cancelSummarize()` method: cancels the job, resets `aiSummary` to `Empty`
- Add `cancelTranslation()` method: cancels the job, resets `translationState` to `Empty`
- Pass `onCancelSummarize` and `onCancelTranslation` callbacks down through the composable chain

#### FR-5: Re-clickable After Completion
- After summarize completes (state becomes `Result`), the icon returns to normal `AutoFixHigh`
- After translate completes (state becomes `Translated` or `Error`), the icon returns to normal `Translate`
- Both icons are fully clickable again (re-triggering restarts the operation)

### Non-Functional Requirements
- **Performance**: Circular progress animation should use Compose's built-in `CircularProgressIndicator` or lightweight `Canvas` drawing - no custom animation framework
- **Accessibility**: The in-progress state must have a content description like "Summarizing in progress, tap to cancel" / "Translating X of Y paragraphs, tap to cancel"
- **Visual consistency**: The circular progress should use `MaterialTheme.colorScheme.primary` for the progress arc and `MaterialTheme.colorScheme.onSurface` for the stop square, consistent with Material 3 styling
- **Size**: The circular progress indicator should visually match the standard icon button size (48dp touch target, ~24dp visual icon area)

## Edge Cases

| Scenario | Expected Behavior |
|---|---|
| User taps cancel during summarize | Job is cancelled, `aiSummary` resets to `Empty`, icon returns to normal |
| User taps cancel during translate | Job is cancelled, `translationState` resets to `Empty`, icon returns to normal, any partially translated paragraphs are discarded |
| Summarize errors | State becomes `Result(Error(...))`, icon returns to normal, error shown in summary card |
| Translate errors | State becomes `Error(msg)`, icon returns to normal, error shown in error card |
| User navigates away during progress | ViewModel scope handles cancellation naturally (viewModelScope) |
| User taps summarize while translate is running (or vice versa) | Both can run independently; each icon shows its own progress |
| Rapid cancel + restart | Cancel completes, state resets, new operation starts cleanly |

## Components to Modify

| File | Change |
|---|---|
| `ArticleScreen.kt:270-281` | Replace static summarize `IconButton` with progress-aware composable |
| `ArticleScreen.kt:283-302` | Replace static translate `IconButton` with progress-aware composable |
| `ArticleScreen.kt:622-636` | Remove `LinearProgressIndicator` from `SummarySection` loading state (or remove the entire loading branch since progress is now on icon) |
| `ArticleScreen.kt:699-721` | Remove `Translating` card with `LinearProgressIndicator` from `TranslationStatusSection` |
| `ArticleScreen.kt:534-538` | May simplify: `SummarySection` only needs to render for `Result` state now |
| `ArticleScreen.kt:542-547` | May simplify: `TranslationStatusSection` only needs to render for `Translated`(with errors) and `Error` states |
| `ArticleViewModel.kt:466-484` | Store Job, add `cancelSummarize()` |
| `ArticleViewModel.kt:496-577` | Store Job, add `cancelTranslation()` |
| New composable | `CircleProgressIconButton` - reusable composable for the circle progress + stop square pattern |

## Acceptance Criteria

- [ ] No `LinearProgressIndicator` visible in article content during summarize or translate
- [ ] Summarize icon shows indeterminate circular progress with center stop square during `Loading` state
- [ ] Translate icon shows determinate circular progress (fraction-based) with center stop square during `Translating` state
- [ ] Tapping the stop square on summarize icon cancels summarize and returns to idle icon
- [ ] Tapping the stop square on translate icon cancels translation and returns to idle icon
- [ ] After summarize completes, icon returns to `AutoFixHigh` and is clickable
- [ ] After translate completes, icon returns to `Translate` and is clickable
- [ ] Summary result card still displays in article content after completion
- [ ] Translation error card still displays in article content after errors
- [ ] Translated paragraph text still displays inline below original paragraphs
- [ ] Accessibility: progress icons have appropriate content descriptions
- [ ] Circular progress fits within standard toolbar icon dimensions
