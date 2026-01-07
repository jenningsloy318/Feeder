# Architecture Design & Technical Specification

## Document Information
- **Spec ID**: 030
- **Feature**: Async Translation with Content Chunking
- **Created**: 2026-01-07
- **Status**: Final Specification

---

## Part 1: Architecture Design

### 1.1 System Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                         UI Layer                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  ArticleDetailScreen                                     │  │
│  │    - Translate button                                   │  │
│  │    - Progress bar (LinearProgressIndicator)             │  │
│  │    - Chunk counter ("Translating 5/25...")             │  │
│  │    - Cancel button                                      │  │
│  └──────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  TranslationSettingsScreen                              │  │
│  │    - Chunk size slider (1000-5000 chars)               │  │
│  │    - Concurrency slider (1-5 parallel)                  │  │
│  │    - Timeout settings                                   │  │
│  └──────────────────────────────────────────────────────────┘  │
└──────────────────────┬──────────────────────────────────────────┘
                       │ StateFlow<TranslationProgress>
                       ▼
┌─────────────────────────────────────────────────────────────────┐
│                   Presentation Layer                             │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  TranslationViewModel                                    │  │
│  │    - translationProgress: StateFlow<Progress>           │  │
│  │    - translateArticle(articleId)                        │  │
│  │    - cancelTranslation()                                │  │
│  └──────────────────────────────────────────────────────────┘  │
└──────────────────────┬──────────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────────┐
│                     Domain Layer                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  AIApi (Enhanced)                                        │  │
│  │    - translate() [existing, backward compatible]         │  │
│  │    - translateWithProgress() [NEW]                       │  │
│  │    - translateChunk() [NEW]                              │  │
│  └──────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  TranslationChunker [NEW]                                │  │
│  │    - createChunks(texts): List<TranslationChunk>         │  │
│  └──────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  ChunkTranslationCoordinator [NEW]                       │  │
│  │    - translateChunksParallel()                          │  │
│  │    - Flow<TranslationProgress>                          │  │
│  └──────────────────────────────────────────────────────────┘  │
└──────────────────────┬──────────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Data Layer                                  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Repository                                              │  │
│  │    - translationChunkSize: StateFlow<Int>               │  │
│  │    - translationConcurrency: StateFlow<Int>             │  │
│  │    - saveTranslationState()                             │  │
│  │    - getTranslationState()                              │  │
│  └──────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  TranslationStateStore [NEW]                             │  │
│  │    - Entity: TranslationStateEntity                     │  │
│  │    - DAO: TranslationStateDao                           │  │
│  └──────────────────────────────────────────────────────────┘  │
└──────────────────────┬──────────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────────┐
│                    External Layer                                │
│  ┌──────────────────────────┐  ┌──────────────────────────┐    │
│  │  OpenAICompatibleClient  │  │   AnthropicClient        │    │
│  │    - translateChunk()    │  │     - translateChunk()   │    │
│  └──────────────────────────┘  └──────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

### 1.2 Data Models

```kotlin
/**
 * Represents a chunk of content for translation
 */
@Serializable
data class TranslationChunk(
    val id: Int,
    val texts: List<TranslatableText>,
    val characterCount: Int,
    val estimatedTokens: Int,
    val startIndex: Int,
    val endIndex: Int
)

/**
 * Result of translating a single chunk
 */
sealed class ChunkTranslationResult {
    data class Success(
        val chunkId: Int,
        val translatedTexts: List<String>
    ) : ChunkTranslationResult()

    data class Error(
        val chunkId: Int,
        val error: String,
        val canRetry: Boolean
    ) : ChunkTranslationResult()
}

/**
 * Progress updates during translation
 */
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
        val result: ChunkTranslationResult
    ) : TranslationProgress()
    data class Complete(
        val translatedParagraphs: List<String>
    ) : TranslationProgress()
    data class Error(val error: String) : TranslationProgress()
    data object Cancelled : TranslationProgress()
}
```

### 1.3 Component Specifications

#### Component 1: TranslationChunker

**Responsibility**: Split content into chunks

```kotlin
class TranslationChunker(
    private val maxChunkSize: Int = 2000
) {
    fun createChunks(
        texts: List<TranslatableText>
    ): List<TranslationChunk> {
        val chunks = mutableListOf<TranslationChunk>()
        var currentChunk = mutableListOf<TranslatableText>()
        var currentSize = 0
        var startIndex = 0
        var chunkId = 0

        for (text in texts) {
            val wouldExceed = currentSize + text.text.length > maxChunkSize

            if (wouldExceed && currentChunk.isNotEmpty()) {
                chunks.add(
                    TranslationChunk(
                        id = chunkId++,
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
                    id = chunkId,
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
}
```

#### Component 2: ChunkTranslationCoordinator

**Responsibility**: Coordinate parallel chunk translation

```kotlin
class ChunkTranslationCoordinator(
    private val aiClient: AIClient,
    private val concurrency: Int = 3
) {
    fun translateChunksParallel(
        chunks: List<TranslationChunk>,
        language: TranslationLanguage
    ): Flow<TranslationProgress> = flow {
        emit(TranslationProgress.Starting(chunks.size))

        val semaphore = Semaphore(concurrency)
        val results = mutableListOf<ChunkTranslationResult>()

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

        val assembledResults = assembleResults(results)
        emit(TranslationProgress.Complete(assembledResults))
    }

    private suspend fun translateWithRetry(
        chunk: TranslationChunk,
        language: TranslationLanguage,
        maxRetries: Int = 3
    ): ChunkTranslationResult {
        var lastException: Exception? = null
        var delay = 1_000L

        repeat(maxRetries) { attempt ->
            try {
                return aiClient.translateChunk(chunk, language)
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

    private fun isRetryable(error: Exception): Boolean {
        return when (error) {
            is SocketTimeoutException -> true
            is UnknownHostException -> true
            is HttpException -> error.code() == 429
            else -> false
        }
    }

    private fun assembleResults(
        results: List<ChunkTranslationResult>
    ): List<String> {
        val sortedResults = results
            .filterIsInstance<ChunkTranslationResult.Success>()
            .sortedBy { it.chunkId }

        return sortedResults.flatMap { it.translatedTexts }
    }
}
```

---

## Part 2: Implementation Plan

### 2.1 Implementation Phases

#### Phase 1: Foundation (2-3 days)

**Task 1.1**: Create data models
- [ ] Create `TranslationChunk.kt`
- [ ] Create `TranslationProgress.kt`
- [ ] Create `ChunkTranslationResult.kt`
- [ ] Write unit tests for data models

**Task 1.2**: Create TranslationChunker
- [ ] Implement chunking logic
- [ ] Add paragraph boundary preservation
- [ ] Write unit tests for various edge cases
- [ ] Test with real article content

**Task 1.3**: Add new settings
- [ ] Add `translationChunkSize` to SettingsStore
- [ ] Add `translationConcurrency` to SettingsStore
- [ ] Expose via Repository
- [ ] Update settings UI

#### Phase 2: Async Translation (3-4 days)

**Task 2.1**: Extend AIClient interface
- [ ] Add `translateChunk()` method to interface
- [ ] Implement in OpenAICompatibleClient
- [ ] Implement in AnthropicClient
- [ ] Write integration tests

**Task 2.2**: Create ChunkTranslationCoordinator
- [ ] Implement parallel translation logic
- [ ] Add semaphore-based concurrency control
- [ ] Implement exponential backoff retry
- [ ] Write unit tests

**Task 2.3**: Integrate with AIApi
- [ ] Add `translateWithProgress()` method
- [ ] Implement chunking threshold logic
- [ ] Maintain backward compatibility
- [ ] Write integration tests

#### Phase 3: UI & Progress (2-3 days)

**Task 3.1**: Create progress UI
- [ ] Add linear progress indicator
- [ ] Add chunk counter text
- [ ] Add cancel button
- [ ] Style and animate

**Task 3.2**: Update ViewModel
- [ ] Add progress StateFlow
- [ ] Implement cancel logic
- [ ] Handle configuration changes
- [ ] Write UI tests

**Task 3.3**: Connect to existing screens
- [ ] Update ArticleDetailScreen
- [ ] Update TranslationSettingsScreen
- [ ] Test on real devices
- [ ] Handle edge cases

#### Phase 4: Persistence (2 days)

**Task 4.1**: Create database schema
- [ ] Add TranslationStateEntity
- [ ] Create TranslationStateDao
- [ ] Update database version
- [ ] Write migration

**Task 4.2**: Implement state persistence
- [ ] Create TranslationStateStore
- [ ] Save state before each chunk
- [ ] Restore state on app start
- [ ] Clear state on completion

#### Phase 5: Testing & Polish (2-3 days)

**Task 5.1**: Performance testing
- [ ] Test with short articles (500 words)
- [ ] Test with medium articles (2,500 words)
- [ ] Test with long articles (30,000 words)
- [ ] Measure translation times

**Task 5.2**: Error handling testing
- [ ] Test network interruption
- [ ] Test timeout scenarios
- [ ] Test rate limit handling
- [ ] Test cancellation

**Task 5.3**: Documentation
- [ ] Update inline code comments
- [ ] Document new APIs
- [ ] Create user guide
- [ ] Update CHANGELOG

### 2.2 Task Breakdown

| Task ID | Task | Estimate | Priority | Dependencies |
|---------|------|----------|----------|--------------|
| T1 | Create data models | 4h | P0 | None |
| T2 | Implement TranslationChunker | 6h | P0 | T1 |
| T3 | Add new settings | 4h | P0 | None |
| T4 | Extend AIClient interface | 6h | P0 | T1 |
| T5 | Implement translateChunk() | 8h | P0 | T4 |
| T6 | Create ChunkTranslationCoordinator | 10h | P0 | T1, T5 |
| T7 | Integrate with AIApi | 6h | P0 | T2, T6 |
| T8 | Create progress UI | 8h | P1 | T1 |
| T9 | Update ViewModel | 6h | P1 | T8 |
| T10 | Connect to screens | 6h | P1 | T9 |
| T11 | Create database schema | 4h | P2 | None |
| T12 | Implement state persistence | 8h | P2 | T11 |
| T13 | Performance testing | 6h | P1 | T7 |
| T14 | Error handling testing | 4h | P1 | T7 |
| T15 | Documentation | 4h | P2 | All |

**Total Estimate**: 11-14 days

---

## Part 3: Acceptance Criteria

### 3.1 Functional Requirements

**FR-1: Content Chunking**
- [ ] Content exceeding 2000 characters is automatically chunked
- [ ] Chunk boundaries respect paragraph boundaries
- [ ] Each chunk preserves structure metadata (element type, nesting)
- [ ] Chunks don't exceed 4000 tokens (~16,000 characters)

**FR-2: Asynchronous Processing**
- [ ] Translation executes in background coroutine
- [ ] UI remains responsive during translation
- [ ] User can navigate away while translation progresses
- [ ] User can cancel translation via cancel button

**FR-3: Progress Reporting**
- [ ] Progress indicator shows 0-100%
- [ ] Chunk counter displays "Translating X of Y..."
- [ ] Progress updates in real-time
- [ ] Progress survives configuration changes

**FR-4: Parallel Processing**
- [ ] Up to 3 chunks translate concurrently
- [ ] No rate limit errors from provider
- [ ] Translated paragraphs maintain original order
- [ ] Failed chunks trigger retry (max 3 attempts)

**FR-5: Error Handling**
- [ ] Individual chunk failures don't fail entire translation
- [ ] Partial results displayed if some chunks fail
- [ ] Clear error messages shown to user
- [ ] "Retry Failed" action available

**FR-6: Result Assembly**
- [ ] Chunks assembled in original order
- [ ] HTML structure preserved
- [ ] Missing chunks show placeholders
- [ ] Results cached to avoid re-translation

### 3.2 Non-Functional Requirements

**Performance**:
- [ ] 30,000-word article translates in < 5 minutes
- [ ] Each chunk processes in < 30 seconds
- [ ] UI frame time < 16ms during translation
- [ ] Memory overhead < 100MB

**Reliability**:
- [ ] > 95% success rate for long-form translations
- [ ] > 80% retry success rate for failed chunks
- [ ] Translation state persists across app crashes
- [ ] Translations resume after network loss

**Usability**:
- [ ] Progress always visible to user
- [ ] User can cancel any time
- [ ] Error messages are clear and actionable
- [ ] Behavior consistent for short and long content

### 3.3 Testing Checklist

**Unit Tests**:
- [ ] TranslationChunker with various content sizes
- [ ] Chunk boundary preservation
- [ ] Parallel chunk translation
- [ ] Exponential backoff retry logic
- [ ] Result assembly

**Integration Tests**:
- [ ] End-to-end translation with mocked API
- [ ] Progress reporting
- [ ] Error handling and recovery
- [ ] Cancellation
- [ ] State persistence

**UI Tests**:
- [ ] Progress bar updates correctly
- [ ] Cancel button works
- [ ] Configuration changes handled
- [ ] Error messages display correctly

**Performance Tests**:
- [ ] Short article (< 1000 chars)
- [ ] Medium article (10,000 chars)
- [ ] Long article (50,000 chars)
- [ ] Very long article (150,000 chars)

---

## Part 4: Risk Mitigation

### 4.1 Technical Risks

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| Rate limiting | High | Medium | Implement semaphore + exponential backoff |
| Memory issues | Medium | Low | Limit concurrent chunks, test with large content |
| State corruption | Medium | Low | Use database transactions, validation |
| UI jank | Low | Medium | Use Flow for progress, offload work |
| Provider changes | Medium | Low | Abstract provider interface, adapter pattern |

### 4.2 Implementation Risks

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| Timeline overrun | Medium | Medium | Phased rollout, prioritize MVP |
| Breaking changes | High | Low | Maintain backward compatibility |
| Test coverage gaps | Medium | Medium | Require tests for each phase |
| Integration issues | Medium | Low | Incremental integration, frequent testing |

---

## Conclusion

This architecture design provides a comprehensive solution for async translation with content chunking. The design preserves existing architecture while adding the necessary components for reliable long-form translation.

**Next Steps**:
1. Review and approve this specification
2. Begin Phase 1 implementation (Foundation)
3. Proceed through phases sequentially
4. Conduct thorough testing at each phase

**Success Metrics**:
- Long-form translation success rate: >95%
- Translation time for 30k words: <5 minutes
- User satisfaction: Positive feedback
- Zero breaking changes to existing functionality

---

**Document Version**: 1.0 - Final Specification
**Ready for Implementation**: Yes
**Estimated Timeline**: 11-14 days
