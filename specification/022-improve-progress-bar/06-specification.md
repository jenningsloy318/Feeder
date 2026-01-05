# Technical Specification: Improve Progress Bar for Summary and Translation

## Overview
Add descriptive text labels to progress indicators for AI summary and translation features to improve user experience.

## Files to Modify

### 1. String Resources
**File:** `app/src/main/res/values/strings.xml`

Add two new string entries:
```xml
<!-- Progress indicator text -->
<string name="summarizing_progress">Summarizing...</string>
<string name="translating_progress">Translating...</string>
```

### 2. Article Screen UI
**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt`

#### Changes to `SummarySection` Composable (Lines 614-631)

**Before:**
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

**After:**
```kotlin
@Composable
private fun SummarySection(summary: AISummaryState) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        when (summary) {
            AISummaryState.Empty -> {}
            AISummaryState.Loading ->
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.summarizing_progress),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            is AISummaryState.Result ->
                MarkdownText(
                    modifier = Modifier.padding(8.dp),
                    markdown = summary.value.content,
                )
        }
    }
}
```

#### Changes to `TranslationStatusSection` Composable (Lines 672-693)

**Before:**
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

**After:**
```kotlin
@Composable
private fun TranslationStatusSection(translation: TranslationState) {
    when (translation) {
        TranslationState.Empty -> {}
        TranslationState.Loading ->
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.translating_progress),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
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

## Required Imports

Add these imports to `ArticleScreen.kt` if not already present:
```kotlin
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Alignment
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
```

## Implementation Details

### Layout Structure
1. **Column** - Vertical container for text + progress
2. **Text** - Descriptive label ("Summarizing..." or "Translating...")
3. **Spacer** - 8dp vertical spacing
4. **LinearProgressIndicator** - Progress bar

### Styling
- **Typography**: `MaterialTheme.typography.bodySmall`
- **Color**: `MaterialTheme.colorScheme.onSurfaceVariant`
- **Alignment**: `Alignment.CenterHorizontally`
- **Padding**: 16dp card padding
- **Spacing**: 8dp between text and indicator

### Accessibility
- Text is automatically readable by screen readers
- Content description inherited from text element
- Color contrast meets WCAG standards

## Testing

### Manual Test Cases
1. **Summary Loading Test**
   - Open an article
   - Tap the summarize button
   - Verify "Summarizing..." text appears above progress bar
   - Verify progress bar animates
   - Verify text disappears when summary completes

2. **Translation Loading Test**
   - Open an article
   - Tap the translate button
   - Verify "Translating..." text appears above progress bar
   - Verify progress bar animates
   - Verify text disappears when translation completes

3. **Error State Test**
   - Trigger a translation error
   - Verify error message displays correctly
   - Verify no interference with error UI

### Visual Verification
- [ ] Text is centered horizontally
- [ ] Text color contrasts well with background
- [ ] 8dp spacing between text and progress bar
- [ ] Progress bar fills full width (minus padding)
- [ ] Card padding is 16dp

## Rollback Plan
If issues occur:
1. Revert `ArticleScreen.kt` changes
2. Remove new string resources
3. Original functionality will be restored

## Notes
- No business logic changes required
- No ViewModel changes required
- No API changes required
- Pure UI enhancement for better UX
