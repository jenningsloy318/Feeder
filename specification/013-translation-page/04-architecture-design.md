# Architecture Design Document - Article Translation Button Feature

**Spec Index:** 013
**Date:** 2026-01-03
**Phase:** 5.3 - Architecture Design
**Status:** Draft

## 1. Overview

This document defines the architecture for the article translation button feature. The design follows existing patterns in the Feeder codebase, particularly the AI summary feature, to ensure consistency and maintainability.

### 1.1 Design Principles

- **Consistency**: Mirror existing AI summary implementation patterns
- **Separation of Concerns**: UI, state management, and business logic clearly separated
- **Reactive**: Use Flow for reactive state management
- **Testability**: Architecture supports unit and UI testing
- **Performance**: Efficient handling of translation operations

### 1.2 Scope

**In Scope:**
- Translation button UI component
- Translation state management
- Paragraph extraction from article content
- Translation display in article view
- Error handling (users tap translate button to retry)
- Integration with translation settings

**Out of Scope:**
- Actual AI translation implementation (future spec)
- Translation persistence/caching
- Translation API implementation details
- Batch translation
- Translation history

## 2. System Architecture

### 2.1 Component Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                       ArticleScreen                          │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ Top App Bar Actions                                  │  │
│  │  [Summarize] [TRANSLATE] [Fetch Full] [More]        │  │
│  └──────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ ArticleContent                                       │  │
│  │  ┌────────────────────────────────────────────────┐  │  │
│  │  │ LinearArticleContent                           │  │  │
│  │  │   [Original Paragraph 1]                        │  │  │
│  │  │   [Translated Paragraph 1]                      │  │  │
│  │  │   [Original Paragraph 2]                        │  │  │
│  │  │   [Translated Paragraph 2]                      │  │  │
│  │  └────────────────────────────────────────────────┘  │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                            ↓ collects state
┌─────────────────────────────────────────────────────────────┐
│                    ArticleViewModel                          │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ State                                                │  │
│  │  • translationState: MutableStateFlow<TranslationState> │
│  │  • articleContent: StateFlow<LinearArticle>         │  │
│  │  • aiSettings: StateFlow<AISettings>                │  │
│  └──────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ Actions                                              │  │
│  │  • translate(): Unit                                 │  │
│  │  • extractParagraphs(): List<String>                │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                            ↓ uses
┌─────────────────────────────────────────────────────────────┐
│                       Repository                              │
│  • aiSettingsFlow: StateFlow<AISettings>                    │
│    └─ translationLanguage: TranslationLanguage              │
└─────────────────────────────────────────────────────────────┘
                            ↓ will use (future)
┌─────────────────────────────────────────────────────────────┐
│                         AIApi                                 │
│  • translate(paragraphs, targetLanguage): TranslationResult │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 Data Flow

```
User Action
    ↓
[Click Translate Button]
    ↓
ArticleScreen.onTranslate()
    ↓
ArticleViewModel.translate()
    ↓
[Update State: TranslationState.Loading]
    ↓
[Launch Coroutine in Dispatchers.IO]
    ↓
[Extract Paragraphs from articleContent]
    ↓
[Get Target Language from aiSettings]
    ↓
[Call AIApi.translate(paragraphs, language)]
    ↓
[Receive TranslationResult]
    ↓
[Update State: TranslationState.Result or Error]
    ↓
[UI Recomposes with Translations]
```

## 3. Component Design

### 3.1 Translation State

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt`

**Design:**
```kotlin
/**
 * Sealed class representing the state of article translation.
 * Follows AISummaryState pattern for consistency.
 */
sealed interface TranslationState {
    /**
     * No translation has been requested or completed.
     */
    data object Empty : TranslationState

    /**
     * Translation is currently in progress.
     */
    data object Loading : TranslationState

    /**
     * Translation completed with result (success or error).
     */
    data class Result(
        val value: TranslationResult
    ) : TranslationState
}

/**
 * Translation result containing translated paragraphs or error.
 */
sealed class TranslationResult {
    /**
     * Successful translation with list of translated paragraphs.
     * Each translation corresponds to a paragraph in the original article.
     */
    data class Success(
        val translatedParagraphs: List<String>
    ) : TranslationResult()

    /**
     * Translation failed with error message.
     */
    data class Error(
        val message: String
    ) : TranslationResult()
}
```

**Rationale:**
- Mirrors `AISummaryState` pattern exactly
- Three states: Empty, Loading, Result
- Result can contain success or error
- Immutable for thread safety

### 3.2 Translation State in ViewModel

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt`

**Design:**
```kotlin
class ArticleViewModel(di: DI, private val state: SavedStateHandle) : DIAwareViewModel(di) {
    // Existing dependencies
    private val repository: Repository by instance()
    private val aiApi: AIApi by instance()

    // Existing state
    private val aiSummary: MutableStateFlow<AISummaryState> = MutableStateFlow(AISummaryState.Empty)

    // NEW: Translation state
    private val translationState: MutableStateFlow<TranslationState> =
        MutableStateFlow(TranslationState.Empty)

    // View state combining all flows
    val viewState: StateFlow<ArticleScreenViewState> =
        combine(
            articleFlow,
            textToDisplay,
            articleContentFlow,
            toolbarVisible,
            repository.linkOpener,
            repository.useDetectLanguage,
            ttsStateHolder.ttsState,
            ttsStateHolder.availableLanguages,
            repository.aiSettingsFlow,
            aiSummary,
            translationState, // NEW
        ) { params ->
            // ... existing code

            val translationState = params[10] as TranslationState // NEW

            ArticleState(
                // ... existing properties
                translationState = translationState, // NEW
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ArticleState(),
        )
}
```

**Rationale:**
- Follows existing state management pattern
- Combined in viewState using `combine()`
- Exposed as immutable `StateFlow`
- Collected by UI using `collectAsStateWithLifecycle()`

### 3.3 Translate Method

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt`

**Design:**
```kotlin
/**
 * Triggers translation of the current article content.
 * Translation is performed in the background on IO dispatcher.
 */
fun translate() {
    viewModelScope.launch(Dispatchers.IO) {
        try {
            // Set loading state
            translationState.value = TranslationState.Loading

            // Get current article content
            val currentViewState = viewState.value
            val articleContent = currentViewState.articleContent

            // Extract translatable paragraphs
            val paragraphs = extractTranslatableParagraphs(articleContent)

            if (paragraphs.isEmpty()) {
                translationState.value = TranslationState.Result(
                    TranslationResult.Error("No translatable content found")
                )
                return@launch
            }

            // Get target language from settings
            val aiSettings = repository.aiSettingsFlow.first()
            val targetLanguage = aiSettings.translationLanguage

            // Call translation API (dummy for now)
            val result = aiApi.translate(
                paragraphs = paragraphs,
                targetLanguage = targetLanguage
            )

            // Update state with result
            translationState.value = when (result) {
                is AIApi.TranslationResult.Success -> {
                    TranslationState.Result(
                        TranslationResult.Success(result.translations)
                    )
                }
                is AIApi.TranslationResult.Error -> {
                    TranslationState.Result(
                        TranslationResult.Error(result.message)
                    )
                }
            }

        } catch (e: Exception) {
            // Handle unexpected errors
            translationState.value = TranslationState.Result(
                TranslationResult.Error(
                    e.message ?: "Translation failed unexpectedly"
                )
            )
        }
    }
}

/**
 * Extracts translatable text paragraphs from LinearArticle.
 * Only extracts LinearText elements, ignoring images, lists, etc.
 *
 * @return List of paragraph texts in order of appearance
 */
private fun extractTranslatableParagraphs(article: LinearArticle): List<String> {
    return article.elements
        .filterIsInstance<LinearText>()
        .map { it.text }
}
```

**Rationale:**
- Mirrors `summarize()` method pattern
- Error handling with try-catch
- Paragraph extraction only for text elements
- Respects translation language setting
- Updates state appropriately

### 3.4 ArticleScreenViewState Update

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt`

**Design:**
```kotlin
@Immutable
interface ArticleScreenViewState {
    // ... existing properties

    // NEW: Translation state
    val translationState: TranslationState
}

private data class ArticleState(
    // ... existing properties
    override val translationState: TranslationState = TranslationState.Empty,
) : ArticleScreenViewState
```

**Rationale:**
- Maintains immutable interface pattern
- Default value ensures backward compatibility
- Added to combine() flow for reactivity

### 3.5 Translation Button UI

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt`

**Design:**
```kotlin
@Composable
fun ArticleScreen(
    viewState: ArticleScreenViewState,
    onToggleFullText: () -> Unit,
    onMarkAsUnread: () -> Unit,
    onShare: () -> Unit,
    onOpenInCustomTab: () -> Unit,
    onFeedTitleClick: () -> Unit,
    onShowToolbarMenu: (Boolean) -> Unit,
    ttsOnPlay: () -> Unit,
    ttsOnPause: () -> Unit,
    ttsOnStop: () -> Unit,
    ttsOnSkipNext: () -> Unit,
    ttsOnSelectLanguage: (LocaleOverride) -> Unit,
    onToggleBookmark: () -> Unit,
    articleListState: LazyListState,
    onNavigateUp: () -> Unit,
    onSummarize: () -> Unit,
    onTranslate: () -> Unit, // NEW
    modifier: Modifier = Modifier,
) {
    // ... existing code

    topBar = {
        SensibleTopAppBar(
            // ... existing properties
            actions = {
                // Summarize button (conditional)
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

                // NEW: Translate button
                if (viewState.articleId > ID_UNSET) {
                    PlainTooltipBox(tooltip = { Text(stringResource(R.string.translate)) }) {
                        IconButton(
                            onClick = onTranslate,
                            enabled = viewState.translationState !is TranslationState.Loading
                        ) {
                            Icon(
                                Icons.Default.Translate,
                                contentDescription = stringResource(R.string.translate),
                            )
                        }
                    }
                }

                // Fetch Full Article button
                PlainTooltipBox(tooltip = { Text(stringResource(R.string.fetch_full_article)) }) {
                    IconButton(onClick = onToggleFullText) {
                        Icon(
                            Icons.AutoMirrored.Filled.Article,
                            contentDescription = stringResource(R.string.fetch_full_article),
                        )
                    }
                }

                // ... existing menu button
            }
        )
    }
}
```

**Rationale:**
- Placed after "Summarize" button (sequence: Summarize, Translate, Fetch Full)
- Uses same pattern as summarize button
- Button disabled during loading (prevent double-click)
- Only visible when article is loaded (articleId > ID_UNSET)
- Tooltip for accessibility

### 3.6 Translation Display in ArticleContent

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt`

**Design:**
```kotlin
@Composable
fun ArticleContent(
    viewState: ArticleScreenViewState,
    screenType: ScreenType,
    onFeedTitleClick: () -> Unit,
    articleListState: LazyListState,
    modifier: Modifier = Modifier,
) {
    // ... existing setup

    ReaderView(
        // ... existing parameters
    ) { indexOffset ->
        var offsetCounter = indexOffset

        // AI Summary section (existing)
        if (viewState.aiSummary !is AISummaryState.Empty) {
            offsetCounter++
            item {
                SummarySection(viewState.aiSummary)
            }
        }

        // NEW: Translation loading/error section
        if (viewState.translationState !is TranslationState.Empty) {
            offsetCounter++
            item {
                TranslationStatusSection(viewState.translationState)
            }
        }

        // Article content with translations
        if (viewState.articleId > ID_UNSET) {
            when (viewState.textToDisplay) {
                TextToDisplay.CONTENT -> {
                    // Get translated paragraphs if available
                    val translatedParagraphs = when (val state = viewState.translationState) {
                        is TranslationState.Result -> {
                            when (val result = state.value) {
                                is TranslationResult.Success -> result.translatedParagraphs
                                is TranslationResult.Error -> null
                            }
                        }
                        else -> null
                    }

                    linearArticleContent(
                        articleContent = viewState.articleContent,
                        translatedParagraphs = translatedParagraphs, // NEW
                        onLinkClick = { link, index ->
                            // ... existing link handling
                        },
                    )
                }
                // ... other textToDisplay states
            }
        }
    }
}

// NEW: Translation status section
@Composable
private fun TranslationStatusSection(translationState: TranslationState) {
    when (translationState) {
        TranslationState.Empty -> {}
        TranslationState.Loading ->
            LinearProgressIndicator(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
            )
        is TranslationState.Result -> {
            when (val result = translationState.value) {
                is TranslationResult.Success -> {
                    // Success - no special display needed
                    // Translations shown inline with content
                }
                is TranslationResult.Error -> {
                    TranslationErrorSection(
                        message = result.message
                    )
                }
            }
        }
    }
}

// NEW: Translation error section
@Composable
private fun TranslationErrorSection(
    message: String,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = stringResource(R.string.translation_error),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
```

**Rationale:**
- Translation status shown at top of content (like summary)
- Loading uses `LinearProgressIndicator` (consistent with summary)
- Error shows in `OutlinedCard` with message only
- Users tap translate button again to retry
- Translations passed to content renderer for inline display
- No special display needed for success (shown inline)

### 3.7 Content Rendering with Translations

**File:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/html/LinearArticleContent.kt`

**Design:**
```kotlin
/**
 * Displays article content with optional translations.
 *
 * @param articleContent The article content to display
 * @param translatedParagraphs Optional list of translated paragraphs
 * @param onLinkClick Link click handler
 */
fun LazyListScope.linearArticleContent(
    articleContent: LinearArticle,
    translatedParagraphs: List<String>? = null, // NEW
    onLinkClick: (url: String, index: Int?) -> Unit,
) {
    // Track which text element we're on (for matching translations)
    var textElementIndex = 0

    items(
        count = articleContent.elements.size,
        contentType = { index -> articleContent.elements[index].lazyListContentType },
    ) { index ->
        val element = articleContent.elements[index]

        ProvideTextStyle(
            MaterialTheme.typography.bodyLarge.merge(
                TextStyle(color = MaterialTheme.colorScheme.onBackground),
            ),
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                // Check if this element has a translation
                val translation = when {
                    element is LinearText && translatedParagraphs != null
                        && textElementIndex < translatedParagraphs.size -> {
                        translatedParagraphs[textElementIndex]
                    }
                    else -> null
                }

                // Increment text element counter if this is text
                if (element is LinearText) {
                    textElementIndex++
                }

                LinearElementContent(
                    linearElement = element,
                    translation = translation, // NEW
                    idToIndex = articleContent.idToIndex,
                    allowHorizontalScroll = true,
                    onLinkClick = onLinkClick,
                    modifier = Modifier
                        .widthIn(max = minOf(maxWidth, LocalDimens.current.maxReaderWidth))
                        .fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * Displays a text element with optional translation.
 */
@Composable
fun LinearTextContent(
    linearText: LinearText,
    translation: String? = null, // NEW
    idToIndex: Map<String, Int>,
    onLinkClick: (url: String, index: Int?) -> Unit,
    modifier: Modifier = Modifier,
    softWrap: Boolean = true,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Original text
        ProvideScaledText {
            WithBidiDeterminedLayoutDirection(linearText.text) {
                val interactionSource = remember { MutableInteractionSource() }
                val annotatedString = linearText.toAnnotatedString(
                    idToIndex = idToIndex,
                    onLinkClick = onLinkClick
                )

                Text(
                    text = annotatedString,
                    softWrap = softWrap,
                    modifier = modifier
                        .indication(interactionSource, LocalIndication.current)
                        .focusableInNonTouchMode(interactionSource = interactionSource),
                )
            }
        }

        // Translation (if present)
        if (translation != null) {
            ProvideScaledText {
                WithBidiDeterminedLayoutDirection(translation) {
                    val interactionSource = remember { MutableInteractionSource() }

                    Text(
                        text = translation,
                        softWrap = softWrap,
                        style = MaterialTheme.typography.bodyMedium.merge(
                            TextStyle(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontStyle = FontStyle.Italic
                            )
                        ),
                        modifier = modifier
                            .padding(start = 16.dp) // Indent translation
                            .indication(interactionSource, LocalIndication.current)
                            .focusableInNonTouchMode(interactionSource = interactionSource),
                    )
                }
            }
        }
    }
}
```

**Rationale:**
- Translation passed through content rendering pipeline
- Index matching between text elements and translations
- Translations displayed below original text
- Distinct styling (italic, secondary color, indented)
- Only `LinearText` elements have translations
- Handles case where translations count < text elements count

### 3.8 Dummy Translation API

**File:** `app/src/main/java/com/nononsenseapps/feeder/ai/AIApi.kt` (existing, add method)

**Design:**
```kotlin
interface AIApi {
    suspend fun summarize(content: String): SummaryResult

    // NEW: Translation method (dummy implementation for now)
    suspend fun translate(
        paragraphs: List<String>,
        targetLanguage: TranslationLanguage
    ): TranslationResult

    // Translation result types
    sealed class TranslationResult {
        data class Success(val translations: List<String>) : TranslationResult()
        data class Error(val message: String) : TranslationResult()
    }
}

// Dummy implementation (in actual class implementing AIApi)
override suspend fun translate(
    paragraphs: List<String>,
    targetLanguage: TranslationLanguage
): AIApi.TranslationResult {
    // Simulate API delay
    delay(1000)

    // Return dummy translations
    return AIApi.TranslationResult.Success(
        paragraphs.map { paragraph ->
            "[Translated to ${targetLanguage.languageName}]: $paragraph"
        }
    )
}
```

**Rationale:**
- Interface prepared for future real implementation
- Dummy returns prefixed text for now
- Easy to swap with real AI implementation later
- Maintains contract without breaking UI

## 4. State Management

### 4.1 State Flow Diagram

```
Repository (Settings)
    ↓ aiSettingsFlow
    ↓ translationLanguage
ArticleViewModel
    ↓ translationState (MutableStateFlow)
    ↓ Loading → Result/Empty
    ↓
ArticleScreenViewState
    ↓ (via combine)
    ↓
ArticleScreen (UI)
    ↓ collects as State
    ↓ Recomposes on change
```

### 4.2 State Transitions

```
Initial: TranslationState.Empty
    ↓ [User clicks Translate]
TranslationState.Loading
    ↓ [Translation completes]
TranslationState.Result(Success) OR TranslationState.Result(Error)
    ↓ [User navigates away]
TranslationState.Empty (reset on new article)
```

### 4.3 State Reset Strategy

**Current Behavior:**
- State persists while on same article
- State reset when navigating to different article
- No explicit reset method

**Implementation:**
```kotlin
// State automatically resets when articleFlow emits new article
// because translationState is independent and ViewModel is recreated
// or could be reset in init block if needed:

init {
    // Reset translation state when article changes
    viewModelScope.launch {
        articleFlow.collect { article ->
            translationState.value = TranslationState.Empty
        }
    }
}
```

## 5. Error Handling

### 5.1 Error Scenarios

| Scenario | Error Type | Handling |
|----------|------------|----------|
| No article content | Business logic | Show error: "No translatable content found" |
| Empty article | Business logic | Show error: "Article has no content to translate" |
| Network failure (future) | System error | Show error: "Translation failed: Network error" |
| API error (future) | System error | Show error with API message |
| Timeout (future) | System error | Show error: "Translation timed out" |
| Coroutines cancelled | System error | Empty state (no error shown) |

### 5.2 Error Display Pattern

```kotlin
when (translationState) {
    is TranslationState.Result -> {
        when (val result = translationState.value) {
            is TranslationResult.Error -> {
                TranslationErrorSection(
                    message = result.message
                )
            }
        }
    }
}
```

### 5.3 Retry Mechanism

- Users tap translate button again to retry
- Re-triggers `translate()` method
- Same flow as initial translation
- No retry limit (user can retry indefinitely)

## 6. Performance Considerations

### 6.1 Paragraph Extraction

**Complexity:** O(n) where n = number of elements

```kotlin
private fun extractTranslatableParagraphs(article: LinearArticle): List<String> {
    return article.elements
        .filterIsInstance<LinearText>() // O(n)
        .map { it.text } // O(m) where m = number of text elements
}
```

**Optimization:** None needed for typical articles (< 100 paragraphs)

### 6.2 Translation Display

**Complexity:** O(n) where n = number of elements

**Memory Impact:**
- Original article: ~10-100 KB
- Translations: ~10-100 KB (similar size)
- Total: ~20-200 KB (acceptable)

**Recomposition:**
- Only recomposes affected items
- LazyList handles large content efficiently
- Translation state change triggers efficient recomposition

### 6.3 Coroutine Management

**Dispatcher:** `Dispatchers.IO` for translation operation

**Scope:** `viewModelScope` (cancels on ViewModel clear)

**Timeout:** Consider adding for future real implementation:
```kotlin
withTimeout(30_000) {
    val result = aiApi.translate(...)
}
```

## 7. Testing Strategy

### 7.1 Unit Tests

**ViewModel Tests:**
```kotlin
class ArticleViewModelTest {
    @Test
    fun `translate() sets loading state then result`() = runTest {
        // Given
        val viewModel = ArticleViewModel(...)
        assertEquals(TranslationState.Empty, viewModel.translationState.value)

        // When
        viewModel.translate()

        // Then
        assertEquals(TranslationState.Loading, viewModel.translationState.value)
        advanceUntilIdle()
        assertTrue(viewModel.translationState.value is TranslationState.Result)
    }

    @Test
    fun `extractTranslatableParagraphs filters non-text elements`() {
        // Test paragraph extraction logic
    }
}
```

### 7.2 UI Tests

**Compose UI Tests:**
```kotlin
class ArticleScreenTest {
    @Test
    fun `translate button appears in correct position`() {
        composeTestRule.setContent {
            ArticleScreen(...)
        }

        composeTestRule
            .onNodeWithContentDescription("Translate")
            .assertIsDisplayed()
    }

    @Test
    fun `clicking translate button shows loading indicator`() {
        // Test button click and loading state
    }

    @Test
    fun `translation displays below original text`() {
        // Test translation display
    }
}
```

### 7.3 Integration Tests

**End-to-End:**
1. Open article
2. Click translate button
3. Verify loading indicator appears
4. Verify translations appear
5. Verify error handling works
6. Verify retry works

## 8. Future Considerations

### 8.1 Real AI Integration

**Interface Already Defined:**
```kotlin
suspend fun translate(
    paragraphs: List<String>,
    targetLanguage: TranslationLanguage
): TranslationResult
```

**Implementation Changes Needed:**
- Replace dummy implementation with real AI call
- Handle provider-specific errors
- Add rate limiting if needed
- Add caching if desired

### 8.2 Extensibility Points

**Potential Enhancements:**
1. **Caching:** Add translation caching layer
2. **History:** Track translation history
3. **Batch:** Support batch translation of multiple articles
4. **Quality:** Add translation quality feedback
5. **Editing:** Allow users to edit translations
6. **Streaming:** Stream translations as they complete

**Design Implications:**
- Current architecture supports these enhancements
- No breaking changes required
- Can add features incrementally

## 9. Security and Privacy

### 9.1 Data Privacy

**Considerations:**
- Article content sent to AI service
- May contain sensitive information
- User should be aware of translation privacy

**Current Approach:**
- Follows same pattern as AI summary
- Settings explain AI usage
- No additional privacy controls needed

### 9.2 Error Message Security

**Guideline:**
- Don't expose internal errors to users
- Sanitize error messages
- Log detailed errors for debugging

**Implementation:**
```kotlin
try {
    // Translation logic
} catch (e: Exception) {
    Log.e(LOG_TAG, "Translation failed", e)
    translationState.value = TranslationState.Result(
        TranslationResult.Error("Translation failed. Please try again.")
    )
}
```

## 10. Documentation Requirements

### 10.1 Code Documentation

**Required KDoc:**
- `TranslationState` sealed class
- `translate()` method
- `extractTranslatableParagraphs()` method
- `LinearTextContent` translation parameter
- New composables

### 10.2 Architecture Documentation

**Documents:**
- This architecture design (current)
- Technical specification (Phase 6)
- Implementation plan (Phase 6)
- Code comments

## 11. Conclusion

This architecture design provides a solid foundation for the article translation feature. Key strengths:

1. **Consistency**: Mirrors existing AI summary pattern
2. **Maintainability**: Clear separation of concerns
3. **Testability**: All components testable
4. **Performance**: Efficient state management
5. **Extensibility**: Ready for future enhancements

The design is ready for implementation with minimal risk. All components follow established patterns, ensuring seamless integration with the existing codebase.

---

**Architecture Design Complete**
**Total Components Designed:** 8
**State Classes Defined:** 3
**Integration Points Identified:** 6
**Ready for Phase 5.5 (UI/UX Design)**
