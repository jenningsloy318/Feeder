# Research Report: Async Translation with Content Chunking

## Document Information
- **Spec ID**: 030
- **Research Date**: 2026-01-07
- **Researcher**: AI Coordinator Agent
- **Status**: Complete

---

## Executive Summary

This report presents comprehensive research findings on implementing asynchronous translation with content chunking for long-form text in the Feeder Android application. The research covers chunking strategies, async processing patterns, Kotlin coroutines best practices, LLM provider constraints, and industry best practices.

**Key Findings**:
- **Optimal chunk size**: 1,500-2,500 characters (400-600 tokens) balances quality and speed
- **Parallelization**: 3-5 concurrent chunks provides optimal performance without rate limiting
- **Chunking strategy**: Semantic, paragraph-aware chunking preserves context
- **Async pattern**: Kotlin coroutines with structured concurrency and Flow for progress streaming
- **Retry strategy**: Exponential backoff with 3 retry attempts

---

## Table of Contents
1. [Chunking Strategies](#1-chunking-strategies)
2. [Async Processing Patterns](#2-async-processing-patterns)
3. [Kotlin Coroutines Best Practices](#3-kotlin-coroutines-best-practices)
4. [LLM Provider Constraints](#4-llm-provider-constraints)
5. [Error Handling & Retry](#5-error-handling--retry)
6. [Progress Reporting](#6-progress-reporting)
7. [Android Platform Considerations](#7-android-platform-considerations)
8. [Industry Examples](#8-industry-examples)
9. [Recommendations](#9-recommendations)
10. [References](#10-references)

---

## 1. Chunking Strategies

### 1.1 Chunk Size Considerations

Research from multiple sources (Pinecone, Weaviate, Mindee, Machine Learning Mastery) indicates:

**Optimal Chunk Sizes**:
- **Small chunks (500-1,000 chars)**: Better for precise retrieval, but more API calls
- **Medium chunks (1,500-2,500 chars)**: **Recommended balance** - good context, fewer calls
- **Large chunks (3,000-4,000 chars)**: More context, but may timeout or lose detail

**For Translation**:
- **Recommended**: 2,000 characters (~500 words, ~500 tokens)
- **Range**: 1,500-2,500 characters
- **Rationale**: Sufficient context for translation quality, fits within provider limits

### 1.2 Chunking Methods

Based on research, here are the primary chunking approaches:

#### 1.2.1 Fixed-Size Chunking
```kotlin
// Simple character-based chunking
fun chunkBySize(text: String, maxSize: Int = 2000): List<String> {
    return text.chunked(maxSize)
}
```
**Pros**: Simple, predictable
**Cons**: Breaks sentences, loses context

#### 1.2.2 Paragraph-Aware Chunking (RECOMMENDED)
```kotlin
// Preserve paragraph boundaries
fun chunkByParagraphs(
    translatableTexts: List<TranslatableText>,
    maxChunkSize: Int = 2000
): List<List<TranslatableText>> {
    val chunks = mutableListOf<List<TranslatableText>>()
    var currentChunk = mutableListOf<TranslatableText>()
    var currentSize = 0

    for (text in translatableTexts) {
        val textLength = text.text.length

        if (currentSize + textLength > maxChunkSize && currentChunk.isNotEmpty()) {
            chunks.add(currentChunk.toList())
            currentChunk = mutableListOf()
            currentSize = 0
        }

        currentChunk.add(text)
        currentSize += textLength
    }

    if (currentChunk.isNotEmpty()) {
        chunks.add(currentChunk)
    }

    return chunks
}
```
**Pros**: Preserves sentence boundaries, maintains structure context
**Cons**: Variable chunk sizes
**Verdict**: **Best for translation**

#### 1.2.3 Semantic Chunking
```kotlin
// Use embeddings to find semantic boundaries
// Requires additional AI calls
```
**Pros**: Best for topic coherence
**Cons**: Slower (requires pre-processing), more complex
**Verdict**: Overkill for translation, useful for RAG

#### 1.2.4 Recursive Chunking
```kotlin
// Try different separators in order: "\n\n", "\n", ". ", " "
fun chunkRecursively(
    text: String,
    separators: List<String> = listOf("\n\n", "\n", ". ", " "),
    maxSize: Int = 2000
): List<String> {
    // Implementation tries separators recursively
}
```
**Pros**: Good balance, respects structure
**Cons**: More complex logic
**Verdict**: Good alternative to paragraph-aware

### 1.3 Overlap Strategy

**Research Finding**: Overlap helps maintain context between chunks.

- **Overlap Size**: 10-20% of chunk size (200-400 characters)
- **For Translation**: **Not recommended** - translation should be self-contained to avoid inconsistency
- **For Summarization**: Recommended - maintains narrative flow

**Our Decision**: **No overlap** for translation to ensure consistency and avoid redundant API calls.

### 1.4 Chunk Metadata Preservation

Critical for translation quality:

```kotlin
data class TranslationChunk(
    val id: Int,
    val texts: List<TranslatableText>,
    val characterCount: Int,
    val estimatedTokens: Int,
    val startIndex: Int,  // Position in original text
    val endIndex: Int
)
```

**Preserve**:
- Element type (paragraph, heading, list, blockquote)
- Nesting level
- Position/sequence number

---

## 2. Async Processing Patterns

### 2.1 Sequential vs Parallel Processing

Research shows significant performance improvements with parallelization:

**Sequential Processing**:
```
Chunk 1 (30s) → Chunk 2 (30s) → Chunk 3 (30s) = 90 seconds total
```

**Parallel Processing (3 concurrent)**:
```
Chunk 1 (30s) ┐
Chunk 2 (30s) ├→ 30 seconds total (3x speedup)
Chunk 3 (30s) ┘
```

**Real-world example** (from PocketFlow research):
- Sequential: 1,136 seconds
- Parallel (8 concurrent): 209 seconds
- **Speedup: 5.4x**

### 2.2 Kotlin Coroutines Pattern

**Recommended Pattern**: Structured concurrency with async/await

```kotlin
suspend fun translateChunksParallel(
    chunks: List<List<TranslatableText>>,
    language: TranslationLanguage,
    concurrency: Int = 3
): TranslationResult {
    return coroutineScope {
        val semaphore = Semaphore(concurrency)

        val deferredResults = chunks.mapIndexed { index, chunk ->
            async {
                semaphore.acquire()
                try {
                    translateSingleChunk(chunk, language, index + 1, chunks.size)
                } finally {
                    semaphore.release()
                }
            }
        }

        val results = deferredResults.awaitAll()
        assembleResults(results)
    }
}
```

**Key Points**:
- `coroutineScope`: Ensures all coroutines complete or cancel on failure
- `Semaphore`: Limits concurrency to avoid rate limiting
- `async`: Allows parallel execution
- `awaitAll()`: Waits for all chunks to complete

### 2.3 Flow-Based Progress Streaming

For real-time progress updates:

```kotlin
fun translateChunksWithProgress(
    chunks: List<List<TranslatableText>>,
    language: TranslationLanguage
): Flow<TranslationProgress> = flow {
    emit(TranslationProgress.Starting(chunks.size))

    val results = mutableListOf<ChunkResult>()

    chunks.forEachIndexed { index, chunk ->
        emit(TranslationProgress.Translating(index + 1, chunks.size))

        val result = translateSingleChunk(chunk, language)
        results.add(result)

        emit(TranslationProgress.ChunkComplete(index + 1, chunks.size, result))
    }

    emit(TranslationProgress.Complete(assembleResults(results)))
}
```

**UI updates in real-time** as chunks complete.

---

## 3. Kotlin Coroutines Best Practices

### 3.1 Dispatchers

```kotlin
// IO dispatcher for network operations
withContext(Dispatchers.IO) {
    // API calls, database operations
}

// Main dispatcher for UI updates
withContext(Dispatchers.Main) {
    // Update UI
}
```

**Best Practice**:
- Use `Dispatchers.IO` for translation API calls
- Use `Dispatchers.Main.immediate` for progress updates
- Avoid `Dispatchers.Default` (computationally intensive)

### 3.2 Exception Handling

```kotlin
suspend fun translateWithErrorHandling(): TranslationResult {
    return try {
        translateChunks()
    } catch (e: CancellationException) {
        // User cancelled - clear partial results
        TranslationResult.Cancelled
    } catch (e: IOException) {
        // Network error - retry
        retryWithBackoff()
    } catch (e: HttpException) {
        // API error - handle based on status code
        handleApiError(e)
    } catch (e: Exception) {
        // Unexpected error
        TranslationResult.Error(e.message)
    }
}
```

### 3.3 Cancellation Support

```kotlin
val translationJob = viewModelScope.launch {
    translateChunks()
}

// Cancel button
fun onCancelClicked() {
    translationJob.cancel()
}
```

**Important**: Check `isActive` in long-running operations:

```kotlin
chunks.forEach { chunk ->
    ensureActive()  // Throws CancellationException if cancelled
    translateSingleChunk(chunk)
}
```

### 3.4 Context Preservation

```kotlin
class TranslationViewModel(
    private val repository: Repository
) : ViewModel() {

    private val _progress = MutableStateFlow<TranslationProgress>(...)
    val progress: StateFlow<TranslationProgress> = _progress.asStateFlow()

    fun translate(articleId: String) {
        viewModelScope.launch {
            repository.translateChunks(articleId)
                .collect { progress ->
                    _progress.value = progress
                }
        }
    }
}
```

**Benefits**:
- Survives configuration changes (rotation)
- Automatic cleanup on ViewModel clear
- Lifecycle-aware

---

## 4. LLM Provider Constraints

### 4.1 Token Limits

| Provider | Model | Context Window | Practical Limit |
|----------|-------|----------------|-----------------|
| OpenAI | GPT-4 | 128k tokens | 8k-16k tokens |
| OpenAI | GPT-3.5 | 16k tokens | 4k tokens |
| Anthropic | Claude 3 | 200k tokens | 16k-32k tokens |
| OpenAI | GPT-4o | 128k tokens | 8k-16k tokens |

**Rule of Thumb**: Use 4,000 tokens (~16,000 characters) as safe maximum per request

### 4.2 Rate Limits

**OpenAI**:
- Tier 1: 3 requests/minute
- Tier 2: 60 requests/minute
- Tier 3: 3,000 requests/minute
- Tier 4: 10,000 requests/minute

**Anthropic**:
- Default: 50 requests/minute
- Can request increase

**Strategy**:
- Implement semaphore with concurrency limit (3-5)
- Add exponential backoff on 429 errors
- Cache results to avoid re-translation

### 4.3 Timeout Settings

**Provider Timeouts**:
- Default: 60-120 seconds
- Can be configured in client

**Our Settings**:
- Per-chunk timeout: 60 seconds
- Overall translation timeout: 5 minutes (configurable)

### 4.4 Cost Considerations

**Pricing (examples)**:
- OpenAI GPT-4: ~$0.03/1k tokens (input), $0.06/1k tokens (output)
- Anthropic Claude 3: ~$0.003/1k tokens (input), $0.015/1k tokens (output)

**Chunking Impact**:
- Single 30k-word request: ~$1.80 (OpenAI GPT-4)
- 60 chunks of 500 words: ~$1.80 (same cost, better reliability)

**Conclusion**: Chunking doesn't significantly increase cost but dramatically improves reliability.

---

## 5. Error Handling & Retry

### 5.1 Exponential Backoff

```kotlin
suspend fun <T> retryWithBackoff(
    times: Int = 3,
    initialDelay: Long = 1_000,
    maxDelay: Long = 10_000,
    block: suspend () -> T
): T {
    var currentDelay = initialDelay
    repeat(times) { attempt ->
        try {
            return block()
        } catch (e: Exception) {
            if (attempt == times - 1) throw e

            delay(currentDelay)
            currentDelay = minOf(currentDelay * 2, maxDelay)
        }
    }
    throw RuntimeException("Should not reach here")
}
```

**Backoff Strategy**:
- Attempt 1: Immediate
- Attempt 2: Wait 1 second
- Attempt 3: Wait 2 seconds
- Attempt 4: Wait 4 seconds
- Max delay: 10 seconds

### 5.2 Error Classification

```kotlin
sealed class TranslationError {
    object Timeout : TranslationError()
    object RateLimit : TranslationError()
    object QuotaExceeded : TranslationError()
    object NetworkError : TranslationError()
    object InvalidResponse : TranslationError()
    data class Unknown(val message: String) : TranslationError()
}
```

**Retry Strategy**:
- **Retry**: Timeout, RateLimit, NetworkError (transient)
- **Don't Retry**: QuotaExceeded, InvalidResponse (permanent)

### 5.3 Partial Failure Handling

```kotlin
data class TranslationResult(
    val succeededChunks: List<ChunkResult>,
    val failedChunks: List<FailedChunk>,
    val partialSuccess: Boolean
)

data class FailedChunk(
    val chunkId: Int,
    val error: TranslationError,
    val canRetry: Boolean
)
```

**User Experience**:
- Show partial results with placeholders for failures
- Offer "Retry Failed" button
- Don't hide successful chunks

---

## 6. Progress Reporting

### 6.1 Progress State

```kotlin
sealed class TranslationProgress {
    data object Idle : TranslationProgress()
    data class Starting(val totalChunks: Int) : TranslationProgress()
    data class Translating(
        val current: Int,
        val total: Int,
        val message: String = "Translating chunk $current of $total..."
    ) : TranslationProgress()

    data class ChunkComplete(
        val current: Int,
        val total: Int,
        val result: ChunkResult
    ) : TranslationProgress()

    data class Complete(val result: TranslationResult) : TranslationProgress()
    data class Error(val error: String) : TranslationProgress()
    data object Cancelled : TranslationProgress()
}
```

### 6.2 UI Progress Bar

**Linear Progress Indicator**:
```
[████████████░░░░░░░░] 50% - Translating chunk 13 of 25...
```

**Circular Progress Indicator**:
```
(⏳) 13/25 chunks complete
```

**Recommended**: Linear indeterminate during chunk translation, determinate between chunks.

### 6.3 Cancellation UI

```
┌─────────────────────────────────────┐
│ Translating...                      │
│ [████████████░░░░░░░░░░] 60%        │
│ Translating chunk 15 of 25...       │
│                                     │
│ [ Cancel Translation ]              │
└─────────────────────────────────────┘
```

---

## 7. Android Platform Considerations

### 7.1 Lifecycle Awareness

```kotlin
class TranslationManager(
    private val repository: Repository,
    private val applicationScope: CoroutineScope
) {
    private val _translationState = MutableStateFlow<TranslationState?>(null)

    fun translate(articleId: String) {
        applicationScope.launch {
            // Survives process death
            repository.translateArticle(articleId)
                .collect { state ->
                    _translationState.value = state
                    repository.saveTranslationState(articleId, state)
                }
        }
    }

    fun restoreState(articleId: String) {
        applicationScope.launch {
            val saved = repository.getTranslationState(articleId)
            saved?.let { _translationState.value = it }
        }
    }
}
```

### 7.2 Background Processing

**For translations < 1 minute**: Regular coroutine (ViewModel scope)
**For translations > 1 minute**: WorkManager with notification

```kotlin
// Long-running translation with foreground service
class TranslationService : Service() {
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())

        scope.launch {
            // Perform translation
            // Update notification progress
        }

        return START_NOT_STICKY
    }
}
```

**Recommendation**: Start with coroutine-based approach, add WorkManager if needed.

### 7.3 Configuration Changes

ViewModel with SavedStateHandle automatically survives rotation:

```kotlin
class TranslationViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: Repository
) : ViewModel() {

    private val translationState: StateFlow<TranslationState> =
        savedStateHandle.getStateFlow("translation", TranslationState.Idle)

    fun translate(articleId: String) {
        viewModelScope.launch {
            repository.translateChunks(articleId)
                .collect { state ->
                    savedStateHandle["translation"] = state
                }
        }
    }
}
```

---

## 8. Industry Examples

### 8.1 PocketFlow - Parallel Translation

**Case Study**: Multi-language document translation

**Results**:
- Sequential: 1,136 seconds for 8 languages
- Parallel: 209 seconds for 8 languages
- **Speedup: 5.4x**

**Key Pattern**: AsyncParallelBatchNode with semaphore-based concurrency control.

### 8.2 VLLM - Long Text Processing

**Case Study**: Processing 150,000 tokens in 37 chunks

**Approach**:
- Chunk size: 4,096 tokens
- Parallel processing with batching
- Progress reporting

**Result**: Successfully processed input 37x the context window limit.

### 8.3 Chonkie - Token-Based Chunking

**Library**: Specialized in text chunking

**Features**:
- Token-aware chunking (not character-based)
- Preserves sentence boundaries
- Batch processing support

**Key Insight**: Token-based chunking more accurate than character-based for LLMs.

---

## 9. Recommendations

### 9.1 Chunking Strategy

**Recommendation**: **Paragraph-Aware Chunking with 2,000 character limit**

```kotlin
fun createTranslationChunks(
    texts: List<TranslatableText>,
    maxChunkSize: Int = 2000
): List<TranslationChunk> {
    val chunks = mutableListOf<TranslationChunk>()
    var currentChunk = mutableListOf<TranslatableText>()
    var currentSize = 0
    var startIndex = 0

    for (text in texts) {
        val wouldExceed = currentSize + text.text.length > maxChunkSize

        if (wouldExceed && currentChunk.isNotEmpty()) {
            chunks.add(
                TranslationChunk(
                    id = chunks.size,
                    texts = currentChunk.toList(),
                    characterCount = currentSize,
                    estimatedTokens = currentSize / 4,
                    startIndex = startIndex,
                    endIndex = startIndex + currentChunk.size
                )
            )
            currentChunk = mutableListOf()
            currentSize = 0
            startIndex += currentChunk.size
        }

        currentChunk.add(text)
        currentSize += text.text.length
    }

    // Add final chunk
    if (currentChunk.isNotEmpty()) {
        chunks.add(
            TranslationChunk(
                id = chunks.size,
                texts = currentChunk,
                characterCount = currentSize,
                estimatedTokens = currentSize / 4,
                startIndex = startIndex,
                endIndex = startIndex + currentChunk.size
            )
        )
    }

    return chunks
}
```

**Rationale**:
- Preserves paragraph boundaries (critical for translation quality)
- Maintains structure metadata (element type, nesting)
- Predictable chunk sizes for API limits
- No overlap needed for translation

### 9.2 Parallelization Strategy

**Recommendation**: **3 concurrent chunks with exponential backoff**

```kotlin
suspend fun translateParallel(
    chunks: List<TranslationChunk>,
    language: TranslationLanguage,
    concurrency: Int = 3
): Flow<TranslationProgress> = flow {
    emit(TranslationProgress.Starting(chunks.size))

    val semaphore = Semaphore(concurrency)
    val results = mutableListOf<ChunkResult>()

    val deferredResults = chunks.mapIndexed { index, chunk ->
        async {
            semaphore.acquire()
            try {
                emit(TranslationProgress.Translating(index + 1, chunks.size))
                val result = translateWithRetry(chunk, language)
                results.add(result)
                emit(TranslationProgress.ChunkComplete(index + 1, chunks.size, result))
                result
            } finally {
                semaphore.release()
            }
        }
    }

    deferredResults.awaitAll()
    emit(TranslationProgress.Complete(assembleResults(results)))
}
```

**Rationale**:
- 3 concurrent balances speed and rate limits
- Semaphore prevents overwhelming provider
- Flow-based progress for real-time UI updates
- Structured concurrency ensures cleanup

### 9.3 Retry Strategy

**Recommendation**: **Exponential backoff with 3 retries**

```kotlin
suspend fun translateWithRetry(
    chunk: TranslationChunk,
    language: TranslationLanguage,
    maxRetries: Int = 3
): ChunkResult {
    var lastException: Exception? = null
    var delay = 1_000L

    repeat(maxRetries) { attempt ->
        try {
            return translateSingleChunk(chunk, language)
        } catch (e: Exception) {
            lastException = e

            when {
                isRetryable(e) && attempt < maxRetries - 1 -> {
                    delay(delay)
                    delay = minOf(delay * 2, 10_000)
                }
                else -> throw e
            }
        }
    }

    throw lastException!!
}

fun isRetryable(error: Exception): Boolean {
    return when (error) {
        is SocketTimeoutException -> true
        is UnknownHostException -> true
        is HttpException -> error.code() == 429  // Rate limit
        else -> false
    }
}
```

### 9.4 State Persistence

**Recommendation**: **Save translation state to database**

```kotlin
@Entity(tableName = "translation_state")
data class TranslationStateEntity(
    @PrimaryKey val articleId: String,
    val status: String,  // "in_progress", "completed", "failed", "cancelled"
    val currentChunk: Int,
    val totalChunks: Int,
    val translatedChunks: String,  // JSON array of completed chunks
    val failedChunks: String,      // JSON array of failed chunk indices
    val timestamp: Long
)
```

**Benefits**:
- Survives app crashes
- Restores translation on app restart
- Supports "pause and resume" functionality

### 9.5 UI/UX Recommendations

1. **Progress Indicator**: Linear progress bar with percentage
2. **Chunk Counter**: "Translating chunk X of Y..."
3. **Cancel Button**: Always visible during translation
4. **Partial Results**: Display as they complete
5. **Error Messages**: User-friendly with retry options
6. **Notification**: For long-running translations (>1 minute)

---

## 10. References

### Research Sources
1. **Pinecone - Chunking Strategies for LLM Applications**
   - URL: https://www.pinecone.io/learn/chunking-strategies/
   - Key insights: Chunk size selection, semantic chunking

2. **Weaviate - Chunking Strategies to Improve RAG Performance**
   - URL: https://weaviate.io/blog/chunking-strategies-for-rag
   - Key insights: Recursive chunking, overlap strategies

3. **Mindee - LLM Chunking: Strategies, Benefits, and Implementation**
   - URL: https://www.mindee.com/blog/llm-chunking-strategies
   - Key insights: Why chunking matters, performance benefits

4. **Machine Learning Mastery - Essential Chunking Techniques**
   - URL: https://machinelearningmastery.com/essential-chunking-techniques-for-building-better-llm-applications/
   - Key insights: Practical chunking implementation

5. **Kotlin Coroutines Documentation**
   - URL: https://kotlinlang.org/docs/coroutines-guide.html
   - Key insights: Coroutine patterns, best practices

6. **PocketFlow - Parallel Batch Translation Example**
   - Repository: github.com/The-Pocket/PocketFlow
   - Key insights: 5.4x speedup with parallel processing

7. **Chonkie - Text Chunking Library**
   - URL: https://github.com/chonkie-inc/chonkie
   - Key insights: Token-based chunking, sentence preservation

### Code Examples
- Kotlin coroutines with async/await
- Flow-based progress streaming
- Semaphore-based concurrency control
- Exponential backoff retry logic
- Android lifecycle-aware components

---

## Conclusion

This research provides a comprehensive foundation for implementing async translation with content chunking in the Feeder app. The key recommendations are:

1. **Chunking**: 2,000 character chunks with paragraph boundary preservation
2. **Parallelization**: 3 concurrent chunks with semaphore control
3. **Retry**: Exponential backoff with 3 attempts for transient errors
4. **Progress**: Flow-based real-time progress reporting
5. **State**: Database persistence for crash recovery
6. **UI**: Linear progress bar with cancel support

These strategies balance performance, reliability, user experience, and implementation complexity.

---

**Document Version**: 1.0
**Next Update**: After architecture design review
