# Code Assessment: Progress Bar Implementation

## Target Files
- `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt`
- `app/src/main/res/values/strings.xml`

## Current Implementation Analysis

### Summary Section (Lines 614-631)
```kotlin
@Composable
private fun SummarySection(summary: AISummaryState) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        when (summary) {
            AISummaryState.Empty -> {}
            AISummaryState.Loading ->
                LinearProgressIndicator(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                )
            is AISummaryState.Result ->
                MarkdownText(
                    modifier = Modifier.padding(8.dp),
                    markdown = summary.value.content,
                )
        }
    }
}
```

**Issues:**
- No descriptive text during loading state
- Silent progress indicator doesn't indicate what operation is in progress
- Missing accessibility context for screen readers

### Translation Status Section (Lines 672-693)
```kotlin
@Composable
private fun TranslationStatusSection(translation: TranslationState) {
    when (translation) {
        TranslationState.Empty -> {}
        TranslationState.Loading ->
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
            ) {
                LinearProgressIndicator(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                )
            }
        is TranslationState.Result ->
            when (val result = translation.value) {
                is AIClient.TranslationResult.Error ->
                    TranslationErrorSection(errorMessage = result.content)
                is AIClient.TranslationResult.Success -> {
                    // Success - translations will be displayed inline with paragraphs
                    // Nothing to show here
                }
            }
    }
}
```

**Issues:**
- No descriptive text during loading state
- Same silent progress indicator as summary
- No way for users to distinguish between summary and translation loading

## Code Standards

### Project Patterns
- **Material Design 3**: Uses Material3 components and theming
- **Compose**: Modern Jetpack Compose UI
- **Typography**: `MaterialTheme.typography.*` for text styles
- **Colors**: `MaterialTheme.colorScheme.*` for colors
- **Spacing**: 8dp, 16dp standard spacing
- **i18n**: All user-facing strings in `strings.xml`

### String Resources
Current relevant strings:
- `summarize` - "Summarize"
- `translate` - "Translate"
- `translate_article_content_description` - "Translate article content"

Missing strings needed:
- `summarizing_progress` - "Summarizing..."
- `translating_progress` - "Translating..."

## Integration Points

### View State
- `AISummaryState.Loading` - Triggers summary progress display
- `TranslationState.Loading` - Triggers translation progress display

### State Management
- `ArticleViewModel` manages both states
- States flow to UI via `viewState: StateFlow<ArticleScreenViewState>`
- UI observes state changes with `collectAsStateWithLifecycle()`

## Technical Debt
None identified for this feature. The code is clean and follows modern patterns.

## Implementation Recommendations

### Minimal Change Approach
1. Add two new composable functions for text + progress
2. Update `SummarySection` to use new pattern
3. Update `TranslationStatusSection` to use new pattern
4. Add string resources
5. No changes to ViewModel or business logic

### Files to Modify
1. `ArticleScreen.kt` - Update two composables
2. `values/strings.xml` - Add two strings
3. (Optional) Other language files for translations

## Risk Assessment
- **Low Risk**: UI-only change, no business logic changes
- **Test Coverage**: Manual testing sufficient
- **Backward Compatibility**: No breaking changes
