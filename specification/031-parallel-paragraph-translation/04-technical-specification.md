# Technical Specification: Parallel Per-Paragraph Translation

**Date:** 2026-03-15
**Author:** Claude
**Status:** Draft
**Spec:** 031

## 1. Overview

### 1.1 Summary

Replace the existing single-request and batch-based chunk translation with a new **per-paragraph parallel translation** system. Each paragraph is translated as an individual unit, with bounded concurrency via `Semaphore + channelFlow`, and the UI displays each translated paragraph immediately upon completion. All dead code from the previous chunk-based infrastructure (spec-30) is removed.

### 1.2 Goals

- Translate each paragraph independently to avoid timeouts and truncation on long articles
- Display translated paragraphs progressively as each completes (not batch-delayed)
- Use Semaphore-based concurrency (default 3) for immediate-emission parallelism
- Provide per-paragraph progress tracking in the UI (e.g., "4/10 paragraphs translated")
- Support cancellation, retry with exponential backoff, and partial failure display
- Remove all dead code from spec-30 chunk infrastructure

### 1.3 Non-Goals

- UI redesign of translation display layout
- User-configurable concurrency exposed in settings UI
- Persistent translation cache (database storage)
- Streaming/SSE responses from LLM providers
- Manual per-paragraph retry button
- Translation of non-text elements (images, code blocks)
- Cross-paragraph context in translation prompts (each paragraph translated in isolation)

## 2. Background

### 2.1 Context

> From Research Report: `Semaphore + channelFlow` is the best concurrency pattern for per-paragraph translation. The existing `chunked/awaitAll` pattern blocks progress emissions until each entire batch completes. With Semaphore, each paragraph emits progress the instant it completes, and the semaphore immediately releases a permit for the next waiting paragraph.

> From Research Report: `StateFlow<ArticleTranslation>` with copy-on-write via `update {}` is the correct pattern for Compose. Combined with `key()` in LazyColumn, Compose skips recomposition for unchanged paragraph items even when the list reference changes.

### 2.2 Current State

> From Assessment: Spec-30 infrastructure (`translateWithProgress()`, `ChunkTranslationCoordinator`, `TranslationChunker`, `TranslationProgress`) is fully implemented but `ArticleViewModel.translate()` still calls the single-request `AIApi.translate()`.

> From Assessment: `TranslationState` is too coarse — the current `Empty | Loading | Result` sealed interface cannot express per-paragraph progress. The `ChunkTranslationCoordinator` uses `chunks.chunked(batchSize).forEachIndexed` with `async/awaitAll`, meaning all items in a batch must complete before any progress is emitted.

### 2.3 Problem Statement

> From Requirements: Long articles exceed LLM provider response time limits, output token limits truncate translations, users see no progress feedback, and a single failure loses the entire translation. The existing chunk-based infrastructure is not wired and operates at chunk granularity (multiple paragraphs per chunk), not paragraph granularity.

## 3. Technical Design

### 3.1 Architecture

```
┌────────────────────────┐
│   ArticleViewModel     │
│                        │
│ - translate()          │──────────┐
│ - translationState     │          │
│ - extractTranslatable  │          │
│   Paragraphs()         │          │
└────────────────────────┘          │
         │                          │
         │ collect Flow             │ create initial
         │                          │ ArticleTranslation
         ▼                          ▼
┌────────────────────────┐   ┌─────────────────────────┐
│ ParagraphTranslation   │   │ ArticleTranslation      │
│ Coordinator (NEW)      │   │ (NEW data model)        │
│                        │   │                         │
│ - Semaphore(permits=3) │   │ - contents: List<       │
│ - channelFlow          │   │     ParagraphTranslation>│
│ - translateWithRetry() │   │ - status: String        │
│ - isRetryableError()   │   └─────────────────────────┘
└────────────────────────┘
         │
         │ AIClient.translate(
         │   listOf(singleParagraph))
         ▼
┌────────────────────────┐
│ AIClient               │
│ (NO CHANGE)            │
│                        │
│ - translate()          │
│ - listModels()         │
│ - generateSummary()    │
└────────────────────────┘
         │
         ▼
┌──────────────────┐   ┌──────────────────┐
│ OpenAICompatible │   │ AnthropicClient  │
│ Client           │   │                  │
│ (MINOR CLEANUP)  │   │ (MINOR CLEANUP)  │
└──────────────────┘   └──────────────────┘
```

### 3.2 Components

#### Component 1: ParagraphTranslation (Data Class)

- **Purpose:** Represents a single paragraph's translation state
- **File Location:** `app/src/main/java/com/nononsenseapps/feeder/ai/ParagraphTranslation.kt`
- **Interface:**
  ```kotlin
  @Serializable
  data class ParagraphTranslation(
      val index: Int,           // 1-based sequential identifier
      val text: String,         // Original paragraph text
      val translation: String,  // Translated text ("" until translated)
      val translated: Int,      // 0 = pending, 1 = completed, -1 = failed
  )
  ```
- **Naming:** Matches user-confirmed JSON structure exactly (`index`, `text`, `translation`, `translated`)

#### Component 2: ArticleTranslation (Data Class)

- **Purpose:** Represents the complete translation state of an article
- **File Location:** `app/src/main/java/com/nononsenseapps/feeder/ai/ArticleTranslation.kt`
- **Interface:**
  ```kotlin
  @Serializable
  data class ArticleTranslation(
      val contents: List<ParagraphTranslation>,
      val status: String,  // "initial" / "translating" / "translated"
  ) {
      val paragraphCompletedCount: Int
          get() = contents.count { it.translated == 1 }

      val paragraphFailedCount: Int
          get() = contents.count { it.translated == -1 }

      val paragraphTotalCount: Int
          get() = contents.size

      val isAllCompleted: Boolean
          get() = contents.all { it.translated != 0 }

      fun buildTranslatedParagraphsList(): List<String?> =
          contents.map { paragraph ->
              if (paragraph.translated == 1) paragraph.translation else null
          }
  }
  ```
- **Status transitions:**
  - `"initial"` — Created from extracted paragraphs, all `translated = 0`
  - `"translating"` — At least one paragraph translation is in-flight
  - `"translated"` — All paragraphs have `translated != 0` (completed or failed)

#### Component 3: ParagraphTranslationCoordinator (NEW Class)

- **Purpose:** Coordinates parallel per-paragraph translation with Semaphore-based concurrency and immediate progress emission
- **File Location:** `app/src/main/java/com/nononsenseapps/feeder/ai/ParagraphTranslationCoordinator.kt`
- **Interface:**
  ```kotlin
  class ParagraphTranslationCoordinator(
      private val aiClient: AIClient,
      private val paragraphConcurrency: Int = DEFAULT_PARAGRAPH_CONCURRENCY,
      private val paragraphMaxRetries: Int = DEFAULT_PARAGRAPH_MAX_RETRIES,
  ) {
      fun translateParagraphs(
          paragraphTexts: List<TranslatableText>,
          targetLanguage: TranslationLanguage,
      ): Flow<ParagraphTranslationProgress>

      companion object {
          const val DEFAULT_PARAGRAPH_CONCURRENCY = 3
          const val MAX_PARAGRAPH_CONCURRENCY = 5
          const val DEFAULT_PARAGRAPH_MAX_RETRIES = 3
      }
  }
  ```
- **Responsibilities:**
  - Launch one coroutine per paragraph within `channelFlow`
  - Use `Semaphore(permits = paragraphConcurrency)` to bound concurrent API calls
  - Emit `ParagraphTranslationProgress.ParagraphComplete` immediately when each paragraph finishes
  - Retry failed paragraphs with exponential backoff (`2^attempt` seconds)
  - Classify errors via `isRetryableError()` (reuse logic from old coordinator)
  - Support cancellation via structured concurrency (`coroutineScope`)

#### Component 4: ParagraphTranslationProgress (Sealed Interface)

- **Purpose:** Progress events emitted by `ParagraphTranslationCoordinator`
- **File Location:** Same file as `ParagraphTranslationCoordinator.kt`
- **Interface:**
  ```kotlin
  sealed interface ParagraphTranslationProgress {
      data class ParagraphComplete(
          val paragraphIndex: Int,        // 1-based index matching ParagraphTranslation.index
          val translatedText: String,     // Translated text for this paragraph
      ) : ParagraphTranslationProgress

      data class ParagraphFailed(
          val paragraphIndex: Int,        // 1-based index
          val errorMessage: String,       // Error description
      ) : ParagraphTranslationProgress
  }
  ```

#### Component 5: TranslationState (Modified Sealed Interface)

- **Purpose:** ViewModel-level state for article translation, enhanced for per-paragraph progress
- **File Location:** `app/src/main/java/com/nononsenseapps/feeder/ui/compose/feedarticle/ArticleViewModel.kt` (same file, replace existing)
- **Interface:**
  ```kotlin
  sealed interface TranslationState {
      /** No translation has been requested */
      data object Empty : TranslationState

      /** Translation in progress with per-paragraph tracking */
      data class Translating(
          val articleTranslation: ArticleTranslation,
      ) : TranslationState

      /** Translation completed (all paragraphs resolved) */
      data class Translated(
          val articleTranslation: ArticleTranslation,
      ) : TranslationState

      /** Fatal error preventing translation from starting */
      data class Error(
          val errorMessage: String,
      ) : TranslationState
  }
  ```
- **State machine:**
  ```
  Empty ──(user taps translate)──▶ Translating(status="translating")
    │                                    │
    │                                    ├──(paragraph completes)──▶ Translating(updated)
    │                                    │
    │                                    ├──(all paragraphs resolved)──▶ Translated(status="translated")
    │                                    │
    │                                    └──(fatal error)──▶ Error(message)
    │
    └──(empty article / settings error)──▶ Error(message)
  ```

### 3.3 Data Model (MANDATORY: User-Confirmed Structure)

```kotlin
/**
 * ParagraphTranslation - Represents a single paragraph's translation state.
 * Maps 1:1 to the user-confirmed JSON structure.
 */
@Serializable
data class ParagraphTranslation(
    val index: Int,           // 1-based sequential identifier
    val text: String,         // Original paragraph text
    val translation: String,  // Translated text ("" until translated)
    val translated: Int,      // 0 = pending, 1 = completed, -1 = failed
)

/**
 * ArticleTranslation - Represents the complete translation state of an article.
 * Maps 1:1 to the user-confirmed JSON structure.
 */
@Serializable
data class ArticleTranslation(
    val contents: List<ParagraphTranslation>,
    val status: String,  // "initial" / "translating" / "translated"
)
```

**JSON representation (user-confirmed):**
```json
{
  "contents": [
    {"index": 1, "text": "original text", "translation": "", "translated": 0},
    {"index": 2, "text": "original text", "translation": "", "translated": 0}
  ],
  "status": "initial"
}
```

**Naming Rules (MANDATORY):**
- `ParagraphTranslation.index` — always 1-based
- `ParagraphTranslation.translated` — integer flag: 0 = pending, 1 = completed, -1 = failed
- `ArticleTranslation.status` — string enum: `"initial"`, `"translating"`, `"translated"`
- `ArticleTranslation.contents` — ordered list matching document order

### 3.4 Function Specifications (MANDATORY: No Ambiguity)

#### Function: ParagraphTranslationCoordinator.translateParagraphs

```kotlin
/**
 * Translates paragraphs in parallel with bounded concurrency via Semaphore.
 * Emits ParagraphTranslationProgress immediately as each paragraph completes.
 *
 * @param paragraphTexts List of paragraphs to translate (from extractTranslatableParagraphs)
 * @param targetLanguage Target language for translation
 * @return Flow emitting ParagraphComplete or ParagraphFailed for each paragraph
 */
fun translateParagraphs(
    paragraphTexts: List<TranslatableText>,
    targetLanguage: TranslationLanguage,
): Flow<ParagraphTranslationProgress> = channelFlow {
    val semaphore = Semaphore(paragraphConcurrency)
    coroutineScope {
        paragraphTexts.forEachIndexed { zeroBasedIndex, paragraphText ->
            launch(Dispatchers.IO) {
                semaphore.withPermit {
                    val paragraphIndex = zeroBasedIndex + 1 // 1-based
                    val translationResult = translateParagraphWithRetry(
                        paragraphText, targetLanguage, paragraphIndex,
                    )
                    send(translationResult)
                }
            }
        }
    }
}
```

**Concurrency behavior:**
- All paragraphs launch immediately, but `semaphore.withPermit` suspends all beyond the concurrency limit
- When one paragraph completes, `send()` emits immediately, and the semaphore releases a permit for the next waiting paragraph
- No batch-waiting: paragraph 4 starts as soon as any of paragraphs 1-3 finishes

#### Function: ParagraphTranslationCoordinator.translateParagraphWithRetry

```kotlin
/**
 * Translates a single paragraph with exponential backoff retry.
 *
 * Retry strategy: 2^attempt seconds (1s, 2s, 4s).
 * Non-retryable errors fail immediately (invalid API key, quota exceeded).
 *
 * @param paragraphText The paragraph to translate
 * @param targetLanguage Target language
 * @param paragraphIndex 1-based paragraph index for progress reporting
 * @return ParagraphComplete on success, ParagraphFailed after all retries exhausted
 */
private suspend fun translateParagraphWithRetry(
    paragraphText: TranslatableText,
    targetLanguage: TranslationLanguage,
    paragraphIndex: Int,
): ParagraphTranslationProgress {
    repeat(paragraphMaxRetries) { attempt ->
        try {
            val translationResult = aiClient.translate(
                listOf(paragraphText), targetLanguage,
            )
            return when (translationResult) {
                is AIClient.TranslationResult.Success -> {
                    ParagraphTranslationProgress.ParagraphComplete(
                        paragraphIndex = paragraphIndex,
                        translatedText = translationResult.paragraphs.firstOrNull() ?: "",
                    )
                }
                is AIClient.TranslationResult.Error -> {
                    val isLastAttempt = attempt == paragraphMaxRetries - 1
                    if (isLastAttempt) {
                        ParagraphTranslationProgress.ParagraphFailed(
                            paragraphIndex = paragraphIndex,
                            errorMessage = translationResult.content,
                        )
                    } else {
                        val backoffDelaySeconds = 2.0.pow(attempt).toLong()
                        delay(backoffDelaySeconds.seconds)
                        return@repeat // continue retry loop
                    }
                }
            }
        } catch (e: Exception) {
            val isLastAttempt = attempt == paragraphMaxRetries - 1
            if (!isRetryableError(e) || isLastAttempt) {
                return ParagraphTranslationProgress.ParagraphFailed(
                    paragraphIndex = paragraphIndex,
                    errorMessage = e.message ?: "Unknown error",
                )
            }
            val backoffDelaySeconds = 2.0.pow(attempt).toLong()
            delay(backoffDelaySeconds.seconds)
        }
    }
    return ParagraphTranslationProgress.ParagraphFailed(
        paragraphIndex = paragraphIndex,
        errorMessage = "Max retries exceeded",
    )
}
```

#### Function: ParagraphTranslationCoordinator.isRetryableError

```kotlin
/**
 * Determines if an error is retryable based on exception type and message.
 * Retryable: timeouts, rate limits, server errors (5xx).
 * Non-retryable: invalid API key, quota exceeded, client errors (4xx).
 *
 * @param translationException The exception to classify
 * @return true if the error is retryable
 */
private fun isRetryableError(translationException: Exception): Boolean {
    val errorMessage = translationException.message?.lowercase() ?: ""
    return when {
        translationException is java.net.SocketTimeoutException -> true
        errorMessage.contains("timeout") -> true
        errorMessage.contains("rate limit") -> true
        errorMessage.contains("server error") -> true
        errorMessage.contains("5") -> true
        errorMessage.contains("invalid api key") -> false
        errorMessage.contains("quota exceeded") -> false
        errorMessage.contains("insufficient quota") -> false
        else -> false
    }
}
```

#### Function: ArticleViewModel.translate (Rewritten)

```kotlin
/**
 * Initiates per-paragraph parallel translation with progressive UI updates.
 *
 * Flow:
 * 1. Extract translatable paragraphs from article content
 * 2. Build initial ArticleTranslation with status="translating"
 * 3. Set TranslationState to Translating with initial model
 * 4. Create ParagraphTranslationCoordinator and collect its Flow
 * 5. On each ParagraphComplete/ParagraphFailed: atomically update the specific
 *    ParagraphTranslation item in ArticleTranslation via StateFlow.update{}
 * 6. When all paragraphs resolved: transition to Translated state
 */
fun translate() {
    viewModelScope.launch(Dispatchers.IO) {
        try {
            // Step 1: Extract paragraphs
            val translatableTexts = extractTranslatableParagraphs()
            if (translatableTexts.isEmpty()) {
                translationState.value = TranslationState.Error(
                    errorMessage = "No translatable content found",
                )
                return@launch
            }

            // Step 2: Build initial ArticleTranslation
            val initialArticleTranslation = ArticleTranslation(
                contents = translatableTexts.mapIndexed { zeroBasedIndex, translatableText ->
                    ParagraphTranslation(
                        index = zeroBasedIndex + 1,
                        text = translatableText.text,
                        translation = "",
                        translated = 0,
                    )
                },
                status = "translating",
            )

            // Step 3: Set initial translating state
            translationState.value = TranslationState.Translating(
                articleTranslation = initialArticleTranslation,
            )

            // Step 4: Get target language and create coordinator
            val targetLanguage = repository.translationLanguage.first()
            val translationTimeout = repository.translationTimeout.first()
            val settingsWithTimeout = createSettingsWithTimeout(translationTimeout)
            val paragraphCoordinator = ParagraphTranslationCoordinator(
                aiClient = AIClient.create(settingsWithTimeout),
            )

            // Step 5: Collect progress and update per-paragraph
            paragraphCoordinator.translateParagraphs(translatableTexts, targetLanguage)
                .collect { paragraphProgress ->
                    translationState.update { currentState ->
                        if (currentState !is TranslationState.Translating) return@update currentState

                        val currentArticleTranslation = currentState.articleTranslation
                        val updatedContents = currentArticleTranslation.contents.toMutableList()

                        when (paragraphProgress) {
                            is ParagraphTranslationProgress.ParagraphComplete -> {
                                val targetIndex = paragraphProgress.paragraphIndex - 1
                                updatedContents[targetIndex] = updatedContents[targetIndex].copy(
                                    translation = paragraphProgress.translatedText,
                                    translated = 1,
                                )
                            }
                            is ParagraphTranslationProgress.ParagraphFailed -> {
                                val targetIndex = paragraphProgress.paragraphIndex - 1
                                updatedContents[targetIndex] = updatedContents[targetIndex].copy(
                                    translated = -1,
                                )
                            }
                        }

                        val updatedArticleTranslation = currentArticleTranslation.copy(
                            contents = updatedContents,
                            status = if (updatedContents.all { it.translated != 0 }) "translated" else "translating",
                        )

                        // Step 6: Transition to Translated when all resolved
                        if (updatedArticleTranslation.status == "translated") {
                            TranslationState.Translated(articleTranslation = updatedArticleTranslation)
                        } else {
                            TranslationState.Translating(articleTranslation = updatedArticleTranslation)
                        }
                    }
                }
        } catch (e: Exception) {
            translationState.value = TranslationState.Error(
                errorMessage = e.message ?: "Translation failed",
            )
        }
    }
}
```

#### Function: ArticleViewModel.createSettingsWithTimeout (Helper)

```kotlin
/**
 * Creates AI settings with the specified translation timeout.
 * Extracted from existing pattern in AIApi.translate().
 *
 * @param translationTimeoutSeconds Timeout in seconds for each translation request
 * @return AISettings with updated timeout
 */
private fun createSettingsWithTimeout(translationTimeoutSeconds: Int): AISettings {
    return when (val currentSettings = repository.aiSettings) {
        is AISettings.OpenAI -> {
            val updatedOpenAiSettings = currentSettings.openaiSettings.copy(
                timeoutSeconds = translationTimeoutSeconds,
            )
            AISettings.OpenAI(updatedOpenAiSettings)
        }
        is AISettings.Anthropic -> {
            val updatedAnthropicSettings = currentSettings.anthropicSettings.copy(
                timeoutSeconds = translationTimeoutSeconds,
            )
            AISettings.Anthropic(updatedAnthropicSettings)
        }
    }
}
```

### 3.5 UI Display Changes

#### TranslationStatusSection (Modified)

The `TranslationStatusSection` composable in `ArticleScreen.kt` must handle the new `TranslationState` variants:

```kotlin
@Composable
private fun TranslationStatusSection(translation: TranslationState) {
    when (translation) {
        TranslationState.Empty -> {}

        is TranslationState.Translating -> {
            val articleTranslation = translation.articleTranslation
            val completedCount = articleTranslation.paragraphCompletedCount
            val totalCount = articleTranslation.paragraphTotalCount
            val progressFraction = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f

            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "$completedCount/$totalCount paragraphs translated",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        is TranslationState.Translated -> {
            // Check for partial failures
            val failedCount = translation.articleTranslation.paragraphFailedCount
            if (failedCount > 0) {
                TranslationErrorSection(
                    errorMessage = "$failedCount paragraph(s) failed to translate",
                )
            }
            // Success paragraphs are displayed inline — nothing else needed here
        }

        is TranslationState.Error ->
            TranslationErrorSection(errorMessage = translation.errorMessage)
    }
}
```

#### Translate Button (Modified)

```kotlin
// In ArticleScreen toolbar
val isTranslationInProgress = viewState.translation is TranslationState.Translating
IconButton(
    onClick = onTranslate,
    enabled = !isTranslationInProgress,
) { /* ... */ }
```

#### Paragraph Rendering (Modified)

The `translatedParagraphs: List<String>?` parameter of `linearArticleContent()` is populated from `ArticleTranslation`:

```kotlin
// In ArticleScreen, where translatedParagraphs is extracted
val translatedParagraphs = when (val translation = viewState.translation) {
    is TranslationState.Translating ->
        translation.articleTranslation.buildTranslatedParagraphsList()
            .map { it ?: "" } // pending paragraphs show as empty (original text displayed)
    is TranslationState.Translated ->
        translation.articleTranslation.buildTranslatedParagraphsList()
            .map { it ?: "" }
    else -> null
}
```

The existing `linearArticleContent()` function signature and behavior remain unchanged — the `translatedParagraphs` list now progressively fills in as each paragraph completes. Paragraphs with empty strings (`""`) show original text (existing behavior when translation is empty).

**Key insight:** The existing `computeParagraphIndices()` and `LinearElementContent` already handle `translatedParagraphs: List<String>?` with index-based lookup. By providing a list where completed paragraphs have translated text and pending paragraphs have empty string, the existing rendering pipeline handles progressive display without modification to `LinearArticleContent.kt` internals.

### 3.6 Auto-Translate Condition Update

The auto-translate check in `ArticleViewModel` init block (line 248) must handle the new state type:

```kotlin
// Existing condition — no change needed since TranslationState.Empty still exists
if (translationEnabled &&
    translationState.value is TranslationState.Empty &&
    article?.link != null &&
    articleContent.elements.isNotEmpty()
) {
    translate()
}
```

This condition requires no modification because `TranslationState.Empty` is preserved.

### 3.7 Error Handling

| Error Case | Handler | User Feedback | Error Variable Name |
|------------|---------|---------------|---------------------|
| Empty article (no paragraphs) | `translate()` early return | `TranslationState.Error("No translatable content found")` | `translationEmptyArticleError` |
| Language setting retrieval failure | `translate()` catch block | `TranslationState.Error(e.message)` | `translationLanguageError` |
| Individual paragraph timeout | `translateParagraphWithRetry()` retries 3x | Paragraph marked `translated = -1`, shown as original text | `paragraphTimeoutError` |
| Individual paragraph API error | `translateParagraphWithRetry()` retries 3x | Paragraph marked `translated = -1`, shown as original text | `paragraphApiError` |
| Rate limit (429) | `translateParagraphWithRetry()` backoff + retry | Transparent to user (retry handles it) | `paragraphRateLimitError` |
| Non-retryable error (invalid key) | `translateParagraphWithRetry()` immediate fail | Paragraph marked `translated = -1` | `paragraphAuthError` |
| All paragraphs fail | `translate()` final state check | `TranslationState.Translated` with all `translated = -1`, error section shows count | `translationAllFailedError` |
| Coroutine cancellation | Structured concurrency propagation | No error shown (user navigated away) | N/A |

## 4. Dead Code Cleanup

### 4.1 Files to DELETE

| File | Reason |
|------|--------|
| `app/src/main/java/com/nononsenseapps/feeder/ai/ChunkTranslationCoordinator.kt` | Replaced by `ParagraphTranslationCoordinator` |
| `app/src/main/java/com/nononsenseapps/feeder/ai/TranslationChunker.kt` | No longer needed — paragraphs are not grouped into chunks |
| `app/src/main/java/com/nononsenseapps/feeder/ai/TranslationChunk.kt` | No longer needed — per-paragraph uses `TranslatableText` directly |
| `app/src/main/java/com/nononsenseapps/feeder/ai/TranslationProgress.kt` | Replaced by `ParagraphTranslationProgress` and new `TranslationState` |

### 4.2 Methods to REMOVE from AIApi.kt

| Method | Lines | Reason |
|--------|-------|--------|
| `translateWithProgress()` | 178-266 | Dead code — ViewModel no longer calls this; coordinator is created directly in ViewModel |

After removing `translateWithProgress()`, also remove its now-unused imports:
- `import kotlinx.coroutines.CoroutineScope`
- `import kotlinx.coroutines.flow.Flow`

The `translate()` method at line 125 is also no longer called from ViewModel (replaced by direct `AIClient.translate()` calls). However, `AIApi.translate()` should be **kept** for potential future use by other callers. No other code currently calls it, but removing it is optional.

### 4.3 Methods to REMOVE from AIClient.kt Interface

| Method | Lines | Reason |
|--------|-------|--------|
| `translateChunk()` | 118-121 | Dead code — `ParagraphTranslationCoordinator` calls `translate()` with single-element list instead |

### 4.4 Methods to REMOVE from Provider Implementations

**OpenAICompatibleClient.kt:**
| Method | Lines | Reason |
|--------|-------|--------|
| `translateChunk()` | 354-420 | Implements removed `AIClient.translateChunk()` interface method |

Remove associated imports:
- `import com.nononsenseapps.feeder.ai.ChunkTranslationResult`
- `import com.nononsenseapps.feeder.ai.TranslationChunk`

**AnthropicClient.kt:**
| Method | Lines | Reason |
|--------|-------|--------|
| `translateChunk()` | 281-330 | Implements removed `AIClient.translateChunk()` interface method |

Remove associated imports:
- `import com.nononsenseapps.feeder.ai.ChunkTranslationResult`
- `import com.nononsenseapps.feeder.ai.TranslationChunk`

### 4.5 Cleanup Verification

After all deletions, verify:
1. No remaining `import` references to deleted classes/files
2. No remaining references to `ChunkTranslationResult`, `TranslationChunk`, `TranslationChunker`, `ChunkTranslationCoordinator`, `TranslationProgress`, or `translateChunk`
3. Build compiles successfully
4. All existing tests pass (note: there are no tests for the deleted classes)

## 5. Implementation Approach

### 5.1 Technology Stack

- Language: Kotlin
- Framework: Android Jetpack (ViewModel, Compose)
- Concurrency: kotlinx.coroutines (`Semaphore`, `channelFlow`, `coroutineScope`, `launch`)
- Serialization: kotlinx.serialization
- DI: Kodein

### 5.2 Dependencies

No new dependencies required. All concurrency primitives (`Semaphore`, `channelFlow`) are available in the existing `kotlinx.coroutines` dependency.

| Dependency | Already Present | Purpose |
|------------|----------------|---------|
| `kotlinx.coroutines` | Yes | `Semaphore`, `channelFlow`, `coroutineScope` |
| `kotlinx.serialization` | Yes | `@Serializable` on data classes |

### 5.3 Configuration

No new configuration. The concurrency level (`DEFAULT_PARAGRAPH_CONCURRENCY = 3`) and max retries (`DEFAULT_PARAGRAPH_MAX_RETRIES = 3`) are constants in `ParagraphTranslationCoordinator.companion`.

## 6. Testing Strategy

### 6.1 Unit Tests

| Component | Test File | Test Cases |
|-----------|-----------|------------|
| `ParagraphTranslationCoordinator` | `ParagraphTranslationCoordinatorTest.kt` | Concurrency limit, retry behavior, error classification, cancellation, empty input |
| `ArticleTranslation` | `ArticleTranslationTest.kt` | Status computation, `buildTranslatedParagraphsList()`, `paragraphCompletedCount`, `paragraphFailedCount` |

**Test naming convention:** `functionName_should_expectedOutcome_when_condition`

#### ParagraphTranslationCoordinatorTest

```kotlin
class ParagraphTranslationCoordinatorTest {
    // Concurrency tests
    @Test fun translateParagraphs_should_limitConcurrentCalls_when_moreParagraphsThanPermits()
    @Test fun translateParagraphs_should_emitImmediately_when_paragraphCompletes()
    @Test fun translateParagraphs_should_translateAllParagraphs_when_inputHasMultiple()

    // Retry tests
    @Test fun translateParagraphWithRetry_should_retryOnTimeout_when_firstAttemptFails()
    @Test fun translateParagraphWithRetry_should_useExponentialBackoff_when_retrying()
    @Test fun translateParagraphWithRetry_should_failImmediately_when_errorIsNotRetryable()
    @Test fun translateParagraphWithRetry_should_emitParagraphFailed_when_maxRetriesExceeded()

    // Error classification tests
    @Test fun isRetryableError_should_returnTrue_when_socketTimeout()
    @Test fun isRetryableError_should_returnTrue_when_rateLimitError()
    @Test fun isRetryableError_should_returnFalse_when_invalidApiKey()

    // Edge cases
    @Test fun translateParagraphs_should_emitNothing_when_inputIsEmpty()
    @Test fun translateParagraphs_should_cancelAllInFlight_when_scopeCancelled()
}
```

#### ArticleTranslationTest

```kotlin
class ArticleTranslationTest {
    @Test fun paragraphCompletedCount_should_countTranslatedParagraphs()
    @Test fun paragraphFailedCount_should_countFailedParagraphs()
    @Test fun isAllCompleted_should_returnTrue_when_allResolved()
    @Test fun isAllCompleted_should_returnFalse_when_someStillPending()
    @Test fun buildTranslatedParagraphsList_should_returnNullForPending()
    @Test fun buildTranslatedParagraphsList_should_returnTextForCompleted()
}
```

### 6.2 Integration Tests

- Verify that `ArticleViewModel.translate()` correctly transitions through `Empty -> Translating -> Translated` states
- Verify that partial failures result in `Translated` state with mixed `translated` values (1 and -1)
- Verify that the `translatedParagraphs` list extracted from `ArticleTranslation` renders correctly in `linearArticleContent()`

### 6.3 Edge Cases

| Edge Case | Expected Behavior | Test Function Name |
|-----------|-------------------|--------------------|
| Empty article (0 paragraphs) | `TranslationState.Error` immediately | `translate_should_setError_when_noParagraphs` |
| Single paragraph article | One API call, no concurrency overhead | `translateParagraphs_should_handleSingleParagraph` |
| All paragraphs fail | `Translated` state with all `translated = -1` | `translate_should_showTranslatedWithFailures_when_allFail` |
| Rapid cancellation | All coroutines cancelled, no state updates | `translate_should_cancelCleanly_when_scopeCleared` |
| Very long paragraph (> 4000 chars) | Single paragraph may timeout, retry, then fail | `translateParagraphWithRetry_should_handleLongParagraph` |

## 7. Security Considerations

### 7.1 Input Validation

| Input | Validation | Sanitization |
|-------|------------|--------------|
| `TranslatableText.text` | Non-blank check in `extractTranslatableParagraphs()` | None (text from parsed HTML) |
| `paragraphConcurrency` | `require(paragraphConcurrency > 0)` in constructor | Clamped to `MAX_PARAGRAPH_CONCURRENCY` |
| `paragraphMaxRetries` | `require(paragraphMaxRetries >= 0)` in constructor | None |

### 7.2 Authentication & Authorization

- **Auth required:** Yes — AI provider API key from `AISettings`
- **No new auth surface:** Uses existing `AIClient.create(settings)` pattern with existing credential management

### 7.3 OWASP Considerations

| Risk | Applicable | Mitigation |
|------|------------|------------|
| Injection | No | Text is sent to external LLM API, not to database queries |
| Broken Auth | No | API keys managed by existing `AISettings` |
| XSS | No | Translations displayed via Compose Text composables (no HTML rendering) |
| SSRF | No | Requests go to user-configured AI provider endpoint only |

## 8. Performance Considerations

### 8.1 Complexity Analysis

| Operation | Function Name | Time Complexity | Space Complexity |
|-----------|--------------|-----------------|------------------|
| Build initial ArticleTranslation | `translate()` | O(N) where N = paragraphs | O(N) |
| Per-paragraph state update | `translationState.update{}` | O(N) list copy | O(N) |
| All paragraphs translate | `translateParagraphs()` | O(N/C * T) where C = concurrency, T = avg translation time | O(N) |

### 8.2 API Call Analysis

Moving from chunk-based (3-5 API calls for 20 paragraphs) to per-paragraph (20 API calls):
- **More API calls** but each is faster (shorter content)
- **Same total tokens** — same content, just split differently
- **Prompt overhead:** ~600 tokens repeated per call (system instructions). For 20 paragraphs, this adds ~12K tokens of prompt overhead vs ~3K for 3 chunks
- **Bounded by Semaphore(3):** At most 3 concurrent API calls at any time

### 8.3 StateFlow Update Efficiency

Each paragraph completion triggers `translationState.update{}` which creates a new `ArticleTranslation` with a copied `contents` list. With `key()` on `paragraph.index` in LazyColumn, Compose skips recomposition for unchanged paragraph items.

For a 20-paragraph article with concurrency 3, there are 20 state updates. Each update copies a list of 20 items. This is negligible overhead on modern Android devices.

## 9. Rollout Plan

1. Create `ParagraphTranslation` and `ArticleTranslation` data classes
2. Create `ParagraphTranslationCoordinator` with `Semaphore + channelFlow`
3. Modify `TranslationState` sealed interface
4. Rewrite `ArticleViewModel.translate()` to use new coordinator
5. Update `ArticleScreen.kt` UI (status section, button, paragraph extraction)
6. Delete dead code files and methods
7. Write unit tests for new coordinator and data model
8. Verify build compiles and all tests pass

## 10. Unambiguous Implementation Requirements (MANDATORY)

### 10.1 Single Implementation Guarantee

- [x] **All function names are specified** — `translateParagraphs`, `translateParagraphWithRetry`, `isRetryableError`, `buildTranslatedParagraphsList`
- [x] **All parameter names are specified** — `paragraphTexts`, `targetLanguage`, `paragraphIndex`, `translationTimeoutSeconds`
- [x] **All variable names follow conventions** — `paragraphConcurrency`, `paragraphMaxRetries`, `initialArticleTranslation`, `paragraphCoordinator`
- [x] **All file paths are specified** — `ParagraphTranslation.kt`, `ArticleTranslation.kt`, `ParagraphTranslationCoordinator.kt`
- [x] **All conditional behaviors are documented** — retry conditions, state transitions, error handling paths
- [x] **All error cases are listed** — timeout, rate limit, API error, invalid key, empty article, all-fail
- [x] **All data structures are fully defined** — `ParagraphTranslation`, `ArticleTranslation`, `ParagraphTranslationProgress`, `TranslationState`

### 10.2 Ambiguity Checklist

- [x] **No pronouns** — Specific nouns used throughout
- [x] **No "etc." or "and so on"** — All items listed explicitly
- [x] **No "appropriate" or "suitable"** — Exact values specified
- [x] **No "handle" or "process"** — Exact actions specified (retry, emit, update)
- [x] **No "if needed" or "when applicable"** — Exact conditions documented
- [x] **No generic names** — All names are feature-specific
- [x] **No optional behaviors** — Everything is required or explicitly conditional

### 10.3 Naming Convention Verification

- [x] **No generic variable names** — no `data`, `item`, `value`, `result`, `temp`
- [x] **No single-letter names** — except loop index `i` (not used here; `zeroBasedIndex` used instead)
- [x] **No abbreviations** — except `ai` (well-known)
- [x] **All names use feature-specific prefixes** — `paragraph...`, `translation...`, `article...`
- [x] **All functions use verb-noun pattern** — `translateParagraphs`, `buildTranslatedParagraphsList`
- [x] **All constants use UPPER_CASE** — `DEFAULT_PARAGRAPH_CONCURRENCY`, `MAX_PARAGRAPH_CONCURRENCY`
- [x] **All booleans use is/has/should prefix** — `isRetryableError`, `isAllCompleted`, `isTranslationInProgress`

## 11. Open Questions

- [ ] Should very short articles (1-2 paragraphs, < 2000 chars total) use a single API call instead of per-paragraph calls for efficiency? (AC-9 suggests this but is marked as optimization, not requirement)
- [ ] Should translation context (previous paragraph text) be included in the prompt for each paragraph to improve cross-paragraph consistency?

## 12. References

- Requirements: `./01-requirements.md`
- Research Report: `./02-research-report.md`
- Code Assessment: `./03-code-assessment.md`
