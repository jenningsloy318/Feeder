# Research Report - Article Translation Button Feature

**Spec Index:** 013
**Date:** 2026-01-03
**Phase:** 3 - Research

## Executive Summary

This report documents research findings for implementing an article translation button in the Feeder RSS reader app. The research analyzed existing codebase patterns, Android UI best practices, and AI integration approaches to inform the implementation design.

## 1. Existing Codebase Analysis

### 1.1 Article Screen Architecture

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt`

**Key Findings:**
- Uses Jetpack Compose with Material3 design system
- Top app bar pattern: `SensibleTopAppBar` with actions
- Button sequence in actions:
  1. Conditional "Summarize" button (when `viewState.showSummarize` is true)
  2. "Fetch Full Article" button (always visible)
  3. "More" menu (dropdown with additional actions)

**Action Button Pattern:**
```kotlin
PlainTooltipBox(tooltip = { Text(stringResource(R.string.summarize)) }) {
    IconButton(onClick = onSummarize) {
        Icon(Icons.Default.AutoFixHigh, contentDescription = stringResource(R.string.summarize))
    }
}
```

**Recommendation:** Add translation button using identical pattern with `Icons.Default.Translate` icon.

### 1.2 ViewModel State Management

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt`

**Key Findings:**
- Uses `StateFlow` for reactive state management
- `ArticleScreenViewState` interface defines view state properties
- `AISummaryState` sealed class pattern for AI feature states:
  ```kotlin
  sealed interface AISummaryState {
      data object Empty : AISummaryState
      data object Loading : AISummaryState
      data class Result(val value: SummaryResult) : AISummaryState
  }
  ```

**Recommendation:** Create similar `TranslationState` sealed class:
```kotlin
sealed interface TranslationState {
    data object Empty : TranslationState
    data object Loading : TranslationState
    data class Result(val translatedParagraphs: List<ParagraphTranslation>) : TranslationState
    data class Error(val message: String) : TranslationState
}
```

### 1.3 Article Content Rendering

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/html/LinearArticleContent.kt`

**Key Findings:**
- `LinearArticle` contains list of `LinearElement` items
- Content rendered using `linearArticleContent()` extension on `LazyListScope`
- Each element type has dedicated composable:
  - `LinearTextContent` for text paragraphs
  - `LinearImageContent` for images
  - `LinearListItemContent` for lists
  - etc.

**Text Rendering Pattern:**
```kotlin
@Composable
fun LinearTextContent(
    linearText: LinearText,
    idToIndex: Map<String, Int>,
    onLinkClick: (url: String, index: Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Text rendering logic
}
```

**Recommendation:** Extend to support paired paragraph display:
```kotlin
@Composable
fun LinearTextContent(
    linearText: LinearText,
    translation: String? = null, // Add translation parameter
    // ... existing parameters
) {
    Column {
        // Original text
        Text(...)

        // Translation (if present)
        if (translation != null) {
            Text(
                text = translation,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
```

### 1.4 AI Integration Pattern

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt` (summarize function)

**Key Findings:**
- AI operations launched in `viewModelScope.launch(Dispatchers.IO)`
- State updates using `MutableStateFlow`
- Error handling with try-catch returning error result
- Content loading using `loadArticleContent()` helper

**Pattern:**
```kotlin
fun summarize() {
    viewModelScope.launch(Dispatchers.IO) {
        try {
            aiSummary.value = AISummaryState.Loading
            val content = loadArticleContent()
            aiSummary.value = AISummaryState.Result(aiApi.summarize(content))
        } catch (e: Exception) {
            aiSummary.value = AISummaryState.Result(
                value = SummaryResult.Error(content = e.message ?: "Unknown error")
            )
        }
    }
}
```

**Recommendation:** Use identical pattern for translation.

## 2. Translation Configuration

### 2.1 Translation Settings

**File:** `app/src/main/java/com/nononsenseapps/feeder/ai/model/TranslationLanguage.kt`

**Key Findings:**
- `TranslationLanguage` enum defines supported languages
- Each language has:
  - `code`: ISO 639-1 language code
  - `displayName`: String resource ID
  - `languageName`: Human-readable name for AI prompts
- Special `DEVICE_DEFAULT` option for system language

**Available Languages:**
- DEVICE_DEFAULT (empty code)
- ENGLISH, CHINESE, SPANISH, FRENCH, GERMAN
- JAPANESE, KOREAN, PORTUGUESE, RUSSIAN, ARABIC, HINDI

**Recommendation:** Read `TranslationLanguage` from `Repository.aiSettingsFlow` to get target language.

### 2.2 Settings Store

**File:** `app/src/main/java/com/nononsenseapps/feeder/archmodel/SettingsStore.kt`

**Key Findings:**
- Settings exposed as `StateFlow` properties
- `translationEnabled` flag exists (but manual translation ignores this)
- Translation language preference persisted in SharedPreferences

**Recommendation:** Access via `repository.aiSettingsFlow` in ViewModel.

## 3. Android UI Best Practices

### 3.1 Material Design Guidelines

**Loading States:**
- Use `LinearProgressIndicator` for indeterminate progress
- Place at top of content area
- Use `Modifier.padding(16.dp)` for proper spacing
- Color: `MaterialTheme.colorScheme.primary`

**Icon Buttons:**
- Use `IconButton` composable for icon-only buttons
- Wrap in `PlainTooltipBox` for accessibility
- Provide proper `contentDescription` string resource
- Size: 48.dp touch target, 24.dp icon

**Error States:**
- Use `OutlinedCard` for error messages
- Include action button (e.g., "Retry")
- Use clear, concise error text
- Consider `Icons.Outlined.ErrorOutline` for visual cue

### 3.2 Jetpack Compose Patterns

**State Hoisting:**
- Keep state in ViewModel, not UI
- Pass state down as parameters
- Pass event handlers up as lambdas
- Use `collectAsStateWithLifecycle()` in composables

**Side Effects:**
- Use `LaunchedEffect` for one-time events
- Use `rememberCoroutineScope` for user-initiated actions
- Avoid launching coroutines directly in composition

**Performance:**
- Use `Immutable` annotation on state classes
- Prefer `@Immutable` over `@Stable` when possible
- Keep recomposition scope minimal

## 4. AI Translation Implementation Considerations

### 4.1 Paragraph Extraction

**Challenge:** Extract translatable text from `LinearArticle`

**Approach:**
1. Filter `LinearArticle.elements` for `LinearText` items only
2. Extract `linearText.text` property
3. Maintain index mapping for pairing translations
4. Preserve order of paragraphs

**Code Pattern:**
```kotlin
val translatableParagraphs = articleContent.elements
    .filterIsInstance<LinearText>()
    .mapIndexed { index, text ->
        ParagraphIndex(index) to text.text
    }
```

### 4.2 Translation API Design (Future)

**Interface Contract:**
```kotlin
interface TranslationApi {
    suspend fun translate(
        paragraphs: List<String>,
        targetLanguage: TranslationLanguage
    ): TranslationResult

    sealed class TranslationResult {
        data class Success(val translations: List<String>) : TranslationResult()
        data class Error(val message: String) : TranslationResult()
    }
}
```

**Considerations:**
- Send all paragraphs in single request for efficiency
- Maintain paragraph order in response
- Handle partial failures gracefully
- Respect target language setting
- Provide meaningful error messages

### 4.3 Dummy Implementation

For current specification (before real AI integration):
```kotlin
suspend fun translate(
    paragraphs: List<String>,
    targetLanguage: TranslationLanguage
): TranslationResult {
    return TranslationResult.Success(
        paragraphs.map { "[Translated to ${targetLanguage.languageName}]: $it" }
    )
}
```

## 5. Performance Considerations

### 5.1 Content Loading

**Current Pattern:**
- Article content loaded from blob storage
- Full text may require fetching from network
- Content parsing happens on IO thread

**Translation Impact:**
- Translation adds additional async operation
- Should not block UI thread
- Consider timeout for long translations

**Recommendations:**
- Set reasonable timeout (e.g., 30 seconds)
- Show loading indicator during translation
- Handle timeout gracefully with error message

### 5.2 Memory Management

**Considerations:**
- Large articles may have many paragraphs
- Translated content doubles memory usage
- Consider pagination for very long articles

**Recommendation:**
- For MVP, load all translations at once
- Monitor memory usage in testing
- Consider pagination if issues arise

## 6. Accessibility

### 6.1 Screen Reader Support

**Requirements:**
- Translation button has proper `contentDescription`
- Loading state announced to screen readers
- Translated text marked with semantic role
- Error messages are accessible

**Implementation:**
```kotlin
IconButton(
    onClick = onTranslate,
    modifier = Modifier.semantics {
        contentDescription = "Translate article"
        role = Role.Button
    }
)
```

### 6.2 Visual Accessibility

**Considerations:**
- Translated text should be visually distinct
- Sufficient color contrast for translated text
- Support font scaling (Compose handles automatically)
- Support text-to-speech (TTS) for translated content

## 7. Testing Considerations

### 7.1 Unit Tests

**Test Cases:**
- Translation state transitions
- Paragraph extraction logic
- Error handling scenarios
- Language setting integration

### 7.2 UI Tests

**Test Scenarios:**
- Button click triggers translation
- Loading indicator appears
- Translated text displays correctly
- Error retry button works
- Screen reader announcements

### 7.3 Integration Tests

**Test Areas:**
- End-to-end translation flow
- Different article types (text-heavy, mixed content)
- Various language settings
- Network failure scenarios

## 8. Implementation Risks and Mitigation

### 8.1 Risk: Large Article Performance

**Risk:** Very long articles may cause slow translation or memory issues

**Mitigation:**
- Set reasonable paragraph limit (e.g., 100 paragraphs)
- Show warning if article exceeds limit
- Consider chunking for future enhancement

### 8.2 Risk: Mixed Content Handling

**Risk:** Articles with images, lists, code blocks within paragraphs

**Mitigation:**
- Only translate `LinearText` elements
- Preserve structure for non-text elements
- Test with various article formats

### 8.3 Risk: Translation Quality

**Risk:** AI may not translate technical terms, code, or quoted text correctly

**Mitigation:**
- This is out of scope for UI implementation
- Future AI integration can handle improvements
- User can view original text alongside translation

## 9. Recommendations Summary

### 9.1 Architecture
1. Mirror AI summary pattern for translation state
2. Use existing `LinearArticleContent` with extension
3. Keep translation logic in ViewModel
4. Prepare interface for future AI integration

### 9.2 UI/UX
1. Use Material3 `IconButton` with tooltip
2. Show `LinearProgressIndicator` during translation
3. Display translations below original text
4. Use distinct styling for translated text
5. Provide retry option on error

### 9.3 Implementation Approach
1. Start with UI changes (button, state display)
2. Add dummy translation function
3. Integrate with real settings
4. Prepare for future AI API integration
5. Test with various article types

## 10. Next Steps

1. **Phase 5:** Complete code assessment with focus on:
   - State management patterns
   - Content rendering structure
   - AI integration points

2. **Phase 5.3:** Design architecture including:
   - Translation state classes
   - View model updates
   - Content rendering modifications

3. **Phase 5.5:** Design UI/UX specifications for:
   - Translation button appearance
   - Loading state design
   - Translation text styling
   - Error state design

4. **Phase 6:** Write detailed technical specification

---

**Research Complete**
**Total Research Time:** ~2 hours
**Key Files Analyzed:** 8
**Code Patterns Identified:** 5
**Recommendations:** 15
