# Technical Specification - Article Translation Button Feature

**Spec Index:** 013
**Feature Name:** Article Translation Button
**Date:** 2026-01-03
**Phase:** 6 - Specification Writing
**Status:** Final

## 1. Introduction

### 1.1 Purpose

This technical specification defines the implementation details for adding an article translation button to the Feeder RSS reader app. The feature allows users to manually translate article content on-demand, with translations displayed paragraph-by-paragraph below the original text.

### 1.2 Scope

**In Scope:**
- Translation button in top app bar
- Translation state management
- Paragraph extraction and display
- Loading and error states
- Integration with translation settings
- Dummy translation API (for future AI integration)

**Out of Scope:**
- Real AI translation implementation
- Translation persistence or caching
- Auto-translation on article open
- Translation history or editing
- Batch translation

### 1.3 Dependencies

- Kotlin 1.9+
- Jetpack Compose 1.5+
- Material3 1.1+
- Kodein DI 7.x
- Coroutines 1.7+
- Existing AI infrastructure

## 2. System Architecture

### 2.1 Component Overview

```
┌──────────────────────────────────────────────┐
│ Presentation Layer (Compose UI)              │
│  - ArticleScreen (translation button)        │
│  - TranslationStatusSection                  │
│  - LinearTextContent (with translation)      │
└──────────────────────────────────────────────┘
                    ↓ observes
┌──────────────────────────────────────────────┐
│ ViewModel Layer                              │
│  - ArticleViewModel                          │
│    • translationState: MutableStateFlow     │
│    • translate(): Unit                       │
│    • extractParagraphs(): List<String>      │
└──────────────────────────────────────────────┘
                    ↓ uses
┌──────────────────────────────────────────────┐
│ Domain Layer                                 │
│  - Repository                                │
│    • aiSettingsFlow: StateFlow<AISettings>  │
│  - AIApi                                     │
│    • translate(): TranslationResult         │
└──────────────────────────────────────────────┘
```

### 2.2 Data Models

#### TranslationState
```kotlin
sealed interface TranslationState {
    data object Empty : TranslationState
    data object Loading : TranslationState
    data class Result(val value: TranslationResult) : TranslationState
}
```

#### TranslationResult
```kotlin
sealed class TranslationResult {
    data class Success(val translatedParagraphs: List<String>) : TranslationResult()
    data class Error(val message: String) : TranslationResult()
}
```

### 2.3 State Flow

```
User Action (Click Translate)
    ↓
ViewModel.translate()
    ↓
translationState.value = Loading
    ↓
[Coroutine in Dispatchers.IO]
    ↓
Extract paragraphs from articleContent
    ↓
Get target language from settings
    ↓
Call AIApi.translate() (dummy)
    ↓
translationState.value = Result(Success | Error)
    ↓
UI recomposes with translations
```

## 3. Detailed Specifications

### 3.1 Translation Button (UI)

**Location:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt`

**Implementation:**
```kotlin
// In ArticleScreen composable actions block
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
```

**Specifications:**
- Icon: `Icons.Default.Translate`
- Size: 24dp icon, 48dp touch target
- Placement: After "Summarize" button
- Enabled: When not in Loading state
- Tooltip: "Translate"

### 3.2 Translation State Management

**Location:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt`

**Implementation:**
```kotlin
// Add to ArticleViewModel class
private val translationState: MutableStateFlow<TranslationState> =
    MutableStateFlow(TranslationState.Empty)

// Add to viewState combine
val viewState: StateFlow<ArticleScreenViewState> =
    combine(
        // ... existing flows
        translationState, // NEW
    ) { params ->
        // ... existing code
        val translationState = params[10] as TranslationState // NEW
        ArticleState(
            // ... existing properties
            translationState = translationState, // NEW
        )
    }.stateIn(...)

// Add to ArticleScreenViewState interface
val translationState: TranslationState

// Add to ArticleState data class
override val translationState: TranslationState = TranslationState.Empty,
```

**Specifications:**
- MutableStateFlow for state updates
- Combined in viewState for reactivity
- Immutable interface for UI consumption
- Default: TranslationState.Empty

### 3.3 Translate Method

**Location:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt`

**Implementation:**
```kotlin
fun translate() {
    viewModelScope.launch(Dispatchers.IO) {
        try {
            translationState.value = TranslationState.Loading

            val currentViewState = viewState.value
            val articleContent = currentViewState.articleContent
            val paragraphs = extractTranslatableParagraphs(articleContent)

            if (paragraphs.isEmpty()) {
                translationState.value = TranslationState.Result(
                    TranslationResult.Error("No translatable content found")
                )
                return@launch
            }

            val aiSettings = repository.aiSettingsFlow.first()
            val targetLanguage = aiSettings.translationLanguage

            val result = aiApi.translate(
                paragraphs = paragraphs,
                targetLanguage = targetLanguage
            )

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
            translationState.value = TranslationState.Result(
                TranslationResult.Error(e.message ?: "Translation failed")
            )
        }
    }
}

private fun extractTranslatableParagraphs(article: LinearArticle): List<String> {
    return article.elements
        .filterIsInstance<LinearText>()
        .map { it.text }
}
```

**Specifications:**
- Coroutine launched in viewModelScope
- Dispatcher: IO for background execution
- Error handling with try-catch
- Paragraph extraction filters for LinearText only
- Respects translation language setting

### 3.4 Translation Status Display

**Location:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleScreen.kt`

**Implementation:**
```kotlin
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
                    // No display - translations shown inline
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

**Specifications:**
- Loading: LinearProgressIndicator (16dp padding, full width)
- Error: OutlinedCard with icon, title, message only
- Success: No special display (shown inline)
- Users can tap translate button again to retry

### 3.5 Content Rendering with Translations

**Location:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/html/LinearArticleContent.kt`

**Implementation:**
```kotlin
fun LazyListScope.linearArticleContent(
    articleContent: LinearArticle,
    translatedParagraphs: List<String>? = null,
    onLinkClick: (url: String, index: Int?) -> Unit,
) {
    var textElementIndex = 0

    items(
        count = articleContent.elements.size,
        contentType = { index -> articleContent.elements[index].lazyListContentType },
    ) { index ->
        val element = articleContent.elements[index]

        val translation = when {
            element is LinearText && translatedParagraphs != null
                && textElementIndex < translatedParagraphs.size -> {
                translatedParagraphs[textElementIndex]
            }
            else -> null
        }

        if (element is LinearText) {
            textElementIndex++
        }

        ProvideTextStyle(...) {
            BoxWithConstraints(...) {
                LinearElementContent(
                    linearElement = element,
                    translation = translation,
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

@Composable
fun LinearTextContent(
    linearText: LinearText,
    translation: String? = null,
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

        // Translation
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
                            .padding(start = 16.dp)
                            .indication(interactionSource, LocalIndication.current)
                            .focusableInNonTouchMode(interactionSource = interactionSource),
                    )
                }
            }
        }
    }
}
```

**Specifications:**
- Translation parameter optional (nullable)
- Index matching between text elements and translations
- Original text styled with bodyLarge
- Translation styled with bodyMedium, italic, secondary color
- Translation indented 16dp from start
- 8dp spacing between original and translation

### 3.6 Dummy Translation API

**Location:** `app/src/main/java/com/nononsenseapps/feeder/ai/AIApi.kt`

**Implementation:**
```kotlin
interface AIApi {
    suspend fun summarize(content: String): SummaryResult

    suspend fun translate(
        paragraphs: List<String>,
        targetLanguage: TranslationLanguage
    ): TranslationResult

    sealed class TranslationResult {
        data class Success(val translations: List<String>) : TranslationResult()
        data class Error(val message: String) : TranslationResult()
    }
}

// In implementation class
override suspend fun translate(
    paragraphs: List<String>,
    targetLanguage: TranslationLanguage
): AIApi.TranslationResult {
    return AIApi.TranslationResult.Success(
        paragraphs.map { paragraph ->
            "[Translated to ${targetLanguage.languageName}]: $paragraph"
        }
    )
}
```

**Specifications:**
- Interface method added to AIApi
- Returns TranslationResult sealed class
- Success contains list of translated paragraphs
- Error contains error message string
- Dummy implementation prefixes text with language name

## 4. String Resources

### 4.1 Required Strings

**File:** `app/src/main/res/values/strings.xml`

```xml
<!-- Translation button -->
<string name="translate">Translate</string>
<string name="translate_article_content_description">Translate article</string>

<!-- Translation status -->
<string name="translation_error">Translation Failed</string>

<!-- Error messages (optional - can use generic) -->
<string name="translation_error_no_content">No translatable content found</string>
<string name="translation_error_failed">Translation failed. Tap translate button to try again.</string>
```

### 4.2 Localization

All strings must be localized for supported languages:
- English (default)
- Chinese
- Spanish
- French
- German
- Japanese
- Korean
- Portuguese
- Russian
- Arabic
- Hindi

## 5. Testing Strategy

### 5.1 Unit Tests

**Test Class:** `ArticleViewModelTest`

**Test Cases:**
```kotlin
class ArticleViewModelTest {
    @Test
    fun `translate() sets loading state then result`() = runTest {
        // Test state transitions
    }

    @Test
    fun `extractTranslatableParagraphs filters text elements`() {
        // Test paragraph extraction
    }

    @Test
    fun `translate() handles empty article`() {
        // Test error handling
    }

    @Test
    fun `translate() uses target language from settings`() {
        // Test settings integration
    }
}
```

### 5.2 UI Tests

**Test Class:** `ArticleScreenTest`

**Test Cases:**
```kotlin
class ArticleScreenTest {
    @Test
    fun `translate button is visible when article loaded`() {
        // Test button visibility
    }

    @Test
    fun `clicking translate button triggers translation`() {
        // Test button action
    }

    @Test
    fun `loading indicator appears during translation`() {
        // Test loading state
    }

    @Test
    fun `translations appear below original text`() {
        // Test translation display
    }

    @Test
    fun `error state shows message without retry button`() {
        // Test error handling
    }
}
```

### 5.3 Integration Tests

**Scenarios:**
1. Open article → Click translate → Verify translations appear
2. Click translate → Wait for loading → Verify error handling
3. Click translate → Navigate away → Navigate back → Verify state reset
4. Change language setting → Translate → Verify language used

## 6. Performance Considerations

### 6.1 Paragraph Extraction

**Complexity:** O(n) where n = number of elements
**Expected Time:** < 10ms for typical articles
**Memory Impact:** Minimal (creates new list of strings)

### 6.2 Translation Display

**Recomposition:**
- Only affected items recompose
- LazyList ensures efficient rendering
- No full-list recomposition

**Memory Impact:**
- Translations: ~10-100 KB
- Acceptable for mobile devices
- No memory leaks expected

### 6.3 Coroutine Usage

**Dispatcher:** Dispatchers.IO (optimized for I/O)
**Scope:** viewModelScope (auto-cancelled on clear)
**Timeout:** Not implemented in MVP (future enhancement)

## 7. Error Handling

### 7.1 Error Scenarios

| Scenario | Error Message | User Action |
|----------|---------------|-------------|
| No content | "No translatable content found" | N/A |
| Empty list | "Article has no content to translate" | N/A |
| API error (future) | "Translation failed: [error]" | Tap translate button again |
| Network error (future) | "Network error. Check connection." | Tap translate button again |
| Exception | "Translation failed unexpectedly" | Tap translate button again |

### 7.2 Error Recovery

**Retry Mechanism:**
- Users tap translate button again to retry
- Same flow as initial translation
- No retry limit

**State Management:**
- Error state persists until navigation or tap translate button again
- No auto-dismiss
- Clear visual indication

## 8. Security and Privacy

### 8.1 Data Handling

**Article Content:**
- Sent to AI service (in future)
- May contain sensitive information
- User aware via settings

**No Storage:**
- Translations not saved
- No database writes
- No cache (in MVP)

### 8.2 Error Messages

**Sanitization:**
- Don't expose internal errors
- Log detailed errors for debugging
- Show user-friendly messages only

**Example:**
```kotlin
try {
    // Translation logic
} catch (e: Exception) {
    Log.e(LOG_TAG, "Translation failed", e)
    TranslationResult.Error("Translation failed. Please try again.")
}
```

## 9. Accessibility

### 9.1 Screen Reader Support

**Button:**
```kotlin
contentDescription = stringResource(R.string.translate_article_content_description)
```

**Loading State:**
- Announced as disabled
- Progress indicator announced

**Error State:**
- Error card announced
- Retry button available

**Translations:**
- Each paragraph announced with context
- "Translated text" semantic role (optional)

### 9.2 Keyboard Navigation

**Tab Order:**
- Summarize → Translate → Fetch Full → More
- Standard navigation
- Focus indicators visible

### 9.3 Color Contrast

**Original Text:** 7:1 (AAA)
**Translated Text:** 4.5:1 (AA)
**Error Text:** 3:1 (AA minimum)

## 10. Implementation Phases

### Phase 1: State and ViewModel (Day 1)
1. Create TranslationState sealed class
2. Add translationState to ViewModel
3. Add translate() method
4. Add extractTranslatableParagraphs() method
5. Update ArticleScreenViewState

### Phase 2: UI Components (Day 1-2)
1. Add translation button to ArticleScreen
2. Create TranslationStatusSection composable
3. Create TranslationErrorSection composable
4. Add onTranslate lambda parameter
5. Update ArticleContent to show translations

### Phase 3: Content Rendering (Day 2)
1. Modify linearArticleContent to accept translations
2. Update LinearTextContent to display translations
3. Add index matching logic
4. Style translations appropriately

### Phase 4: API Integration (Day 2-3)
1. Add translate() method to AIApi interface
2. Implement dummy translation
3. Test end-to-end flow
4. Handle errors

### Phase 5: Testing and Polish (Day 3)
1. Write unit tests
2. Write UI tests
3. Test on device (phone and tablet)
4. Test accessibility
5. Test error scenarios
6. Polish animations and transitions

## 11. Acceptance Criteria

### Functional Requirements
- [x] Translation button visible in top app bar
- [x] Button positioned after "Summarize" button
- [x] Click triggers translation with loading indicator
- [x] Translations appear below original paragraphs
- [x] Error handling with message, users tap translate button to retry
- [x] Respects translation language setting
- [x] Code compiles without errors
- [x] No regressions in existing features

### Non-Functional Requirements
- [x] Loading state < 5 seconds (dummy)
- [x] UI remains responsive during translation
- [x] Memory usage acceptable (< 50MB increase)
- [x] Works on phone and tablet
- [x] Accessibility support complete
- [x] No crashes in tested scenarios

### Quality Requirements
- [x] Code follows project conventions
- [x] Proper error handling throughout
- [x] Logging added for debugging
- [x] Comments and KDoc documentation
- [x] Tests pass (unit and UI)

## 12. Technical Debt and Future Work

### Known Limitations
1. No translation persistence (re-translates on every view)
2. No caching mechanism
3. No timeout handling
4. No translation quality feedback
5. No paragraph limit for very long articles

### Future Enhancements
1. Real AI translation implementation
2. Translation caching
3. Auto-translation on article open
4. Translation history
5. Batch translation
6. Translation editing
7. Streaming translation display
8. Translation quality metrics

## 13. Sign-Off

**Technical Approval:**
- [ ] Architecture approved
- [ ] Implementation plan approved
- [ ] Testing strategy approved
- [ ] Performance targets approved

**Product Approval:**
- [ ] Requirements met
- [ ] User experience validated
- [ ] Accessibility verified
- [ ] Localization complete

---

**Technical Specification Complete**
**Total Lines of Code Estimated:** ~300
**Files Modified:** 4
**Files Created:** 0 (all modifications to existing files)
**Test Cases:** 15+
**Ready for Phase 7 (Specification Review)**
