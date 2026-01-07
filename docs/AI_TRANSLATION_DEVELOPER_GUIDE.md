# AI Translation Developer Guide

## Overview

This guide explains how to use the AI translation API in Feeder, including the new async translation feature with progress tracking for long-form content.

## Table of Contents

- [API Overview](#api-overview)
- [Quick Start](#quick-start)
- [Translation Methods](#translation-methods)
- [Progress Tracking](#progress-tracking)
- [Error Handling](#error-handling)
- [Configuration](#configuration)
- [Implementation Examples](#implementation-examples)
- [Testing](#testing)
- [Migration Guide](#migration-guide)

---

## API Overview

The translation API consists of two main methods:

### 1. `translate()` - Simple Translation
**Use for:** Short to medium content (up to ~2000 characters)

```kotlin
suspend fun translate(
    texts: List<String>,
    sourceLanguage: String? = null,
    targetLanguage: String
): List<String>
```

**Characteristics:**
- Single-shot translation
- No progress reporting
- Simpler API surface
- Suitable for articles, feed descriptions
- Can timeout on very long content (>30k words)

### 2. `translateWithProgress()` - Async Translation with Progress
**Use for:** Long-form content (2000+ characters) or when progress tracking is needed

```kotlin
fun translateWithProgress(
    texts: List<String>,
    scope: CoroutineScope,
    sourceLanguage: String? = null,
    targetLanguage: String
): Flow<TranslationProgress>
```

**Characteristics:**
- Parallel chunk processing (3 concurrent by default)
- Real-time progress reporting
- Cancellable via coroutine scope
- Intelligent retry with exponential backoff
- Handles 30,000+ word documents
- Partial success support

---

## Quick Start

### Simple Translation (Short Content)

```kotlin
class ArticleViewModel(
    private val aiApi: AIApi
) : ViewModel() {

    fun translateArticle(article: Article) {
        viewModelScope.launch {
            try {
                val translated = aiApi.translate(
                    texts = article.translatableTexts,
                    targetLanguage = "Spanish"
                )
                // Update UI with translated content
                _translationState.value = TranslationState.Success(translated)
            } catch (e: Exception) {
                _translationState.value = TranslationState.Error(e)
            }
        }
    }
}
```

### Async Translation with Progress (Long Content)

```kotlin
class ArticleViewModel(
    private val aiApi: AIApi
) : ViewModel() {

    private val translationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun translateLongArticle(article: Article) {
        translationScope.launch {
            aiApi.translateWithProgress(
                texts = article.translatableTexts,
                scope = this,
                targetLanguage = "Spanish"
            ).collect { progress ->
                when (progress) {
                    is TranslationProgress.Starting -> {
                        _translationState.value = TranslationState.Starting(
                            totalChunks = progress.totalChunks
                        )
                    }
                    is TranslationProgress.Translating -> {
                        _translationState.value = TranslationState.InProgress(
                            percentage = progress.getProgressPercentage(),
                            currentChunk = progress.currentChunk,
                            totalChunks = progress.totalChunks
                        )
                    }
                    is TranslationProgress.ChunkComplete -> {
                        // Individual chunk finished
                        _translationState.value = TranslationState.ChunkComplete(
                            current = progress.current,
                            total = progress.total
                        )
                    }
                    is TranslationProgress.Complete -> {
                        _translationState.value = TranslationState.Success(
                            progress.translatedParagraphs
                        )
                    }
                    is TranslationProgress.Error -> {
                        _translationState.value = TranslationState.Error(
                            progress.error
                        )
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        translationScope.cancel() // Cancel any ongoing translations
    }
}
```

---

## Translation Methods

### Method Selection Guide

| Scenario | Recommended Method | Reason |
|----------|-------------------|---------|
| Article translation (<2000 chars) | `translate()` | Simpler, faster |
| Feed description translation | `translate()` | Small content |
| Long-form article (>2000 chars) | `translateWithProgress()` | Avoids timeout |
| E-book translation | `translateWithProgress()` | Progress tracking |
| User wants to see progress | `translateWithProgress()` | Better UX |
| Cancellable operation needed | `translateWithProgress()` | Coroutine scope |

### Automatic Content Detection

`translateWithProgress()` automatically detects content size:
- **< 2000 characters**: Uses fast path with regular `translate()`
- **≥ 2000 characters**: Uses chunked translation with progress

You can always use `translateWithProgress()` - it will choose the optimal strategy.

---

## Progress Tracking

### Progress States

The `TranslationProgress` sealed class represents different translation states:

```kotlin
sealed class TranslationProgress {
    /** Translation is starting - shows total chunks */
    data class Starting(val totalChunks: Int) : TranslationProgress()

    /** Translation is in progress - call getProgressPercentage() for 0-100 */
    data class Translating(
        val currentChunk: Int,
        val totalChunks: Int,
        val currentText: String
    ) : TranslationProgress() {
        fun getProgressPercentage(): Int = (currentChunk * 100) / totalChunks
    }

    /** Individual chunk completed - optional for UI updates */
    data class ChunkComplete(
        val current: Int,
        val total: Int,
        val translatedText: String
    ) : TranslationProgress()

    /** Translation completed successfully */
    data class Complete(val translatedParagraphs: List<String>) : TranslationProgress()

    /** Translation failed */
    data class Error(val error: Throwable, val partialResult: List<String>? = null) : TranslationProgress()
}
```

### UI Implementation Example

```kotlin
@Composable
fun TranslationProgressScreen(progress: TranslationProgress) {
    when (progress) {
        is TranslationProgress.Starting -> {
            CircularProgressIndicator()
            Text("Starting translation (${progress.totalChunks} chunks)")
        }
        is TranslationProgress.Translating -> {
            LinearProgressIndicator(
                progress = progress.getProgressPercentage() / 100f
            )
            Text("Translating: ${progress.getProgressPercentage()}%")
            Text("Chunk ${progress.currentChunk} of ${progress.totalChunks}")
        }
        is TranslationProgress.ChunkComplete -> {
            Text("Completed chunk ${progress.current} of ${progress.total}")
        }
        is TranslationProgress.Complete -> {
            Icon(Icons.Default.CheckCircle, contentDescription = "Complete")
            Text("Translation complete!")
            // Display translated content
            LazyColumn {
                items(progress.translatedParagraphs) { paragraph ->
                    Text(paragraph)
                }
            }
        }
        is TranslationProgress.Error -> {
            Icon(Icons.Default.Error, contentDescription = "Error")
            Text("Translation failed: ${progress.error.message}")
            progress.partialResult?.let { partial ->
                Text("Partial translation available: ${partial.size} chunks")
            }
        }
    }
}
```

---

## Error Handling

### Error Types

The translation system handles several error scenarios:

1. **Network Errors** - Automatic retry (3 attempts with exponential backoff)
   - Timeouts
   - Connection failures
   - Rate limits

2. **API Errors** - Reported to caller
   - Invalid API key
   - Over quota
   - Service unavailable

3. **Chunk Errors** - Per-chunk error tracking
   - Some chunks may fail while others succeed
   - Partial results available in `TranslationProgress.Error`

### Error Handling Example

```kotlin
fun translateWithErrorHandling(article: Article) {
    translationScope.launch {
        aiApi.translateWithProgress(
            texts = article.translatableTexts,
            scope = this,
            targetLanguage = "Spanish"
        ).catch { e ->
            // Flow-level errors (e.g., coroutine cancellation)
            _translationState.value = TranslationState.Error(e)
        }.collect { progress ->
            when (progress) {
                is TranslationProgress.Error -> {
                    // Translation-level errors
                    when {
                        progress.error is CancellationException -> {
                            _translationState.value = TranslationState.Cancelled
                        }
                        progress.partialResult != null -> {
                            // Partial success
                            _translationState.value = TranslationState.PartialSuccess(
                                translated = progress.partialResult,
                                error = progress.error
                            )
                        }
                        else -> {
                            // Complete failure
                            _translationState.value = TranslationState.Error(progress.error)
                        }
                    }
                }
                // ... handle other states
            }
        }
    }
}
```

---

## Configuration

### Chunk Size

Default: 2000 characters per chunk

Configurable in `TranslationChunker`:

```kotlin
val chunker = TranslationChunker(
    maxChunkSize = 2000, // Default
    overlap = 200       // Paragraph overlap for context
)
```

### Concurrency

Default: 3 concurrent chunks

Configurable in `ChunkTranslationCoordinator`:

```kotlin
val coordinator = ChunkTranslationCoordinator(
    concurrency = 3,  // Number of parallel chunks (1-5 recommended)
    maxRetries = 3   // Retry attempts
)
```

### Provider Selection

The API automatically uses the configured provider (OpenAI-compatible or Anthropic):

```kotlin
// Set in app settings
aiRepository.setProvider(
    provider = AIProvider.OpenAI,
    baseUrl = "https://api.openai.com/v1",
    apiKey = "your-api-key"
)
```

---

## Implementation Examples

### Example 1: Translate Article Button

```kotlin
@Composable
fun ArticleScreen(
    article: Article,
    viewModel: ArticleViewModel
) {
    val translationState by viewModel.translationState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(article.title) },
                actions = {
                    if (translationState !is TranslationState.InProgress) {
                        IconButton(onClick = { viewModel.translateArticle(article) }) {
                            Icon(Icons.Default.Translate, "Translate")
                        }
                    } else {
                        IconButton(onClick = { viewModel.cancelTranslation() }) {
                            Icon(Icons.Default.Cancel, "Cancel")
                        }
                    }
                }
            )
        }
    ) { padding ->
        when (val state = translationState) {
            is TranslationState.Idle -> {
                ArticleContent(article)
            }
            is TranslationState.Starting -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                    Text("Starting translation (${state.totalChunks} chunks)")
                }
            }
            is TranslationState.InProgress -> {
                Column(modifier = Modifier.padding(padding)) {
                    LinearProgressIndicator(
                        progress = state.percentage / 100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Translating: ${state.percentage}%")
                    ArticleContent(article) // Show original while translating
                }
            }
            is TranslationState.Success -> {
                ArticleContent(state.translatedArticle)
            }
            is TranslationState.Error -> {
                Column(modifier = Modifier.padding(padding)) {
                    Text("Translation failed", color = MaterialTheme.colors.error)
                    Text(state.error.message ?: "Unknown error")
                    Button(onClick = { viewModel.translateArticle(article) }) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}
```

### Example 2: Batch Translation with Queue

```kotlin
class BatchTranslationViewModel(
    private val aiApi: AIApi
) : ViewModel() {

    private val translationQueue = Channel<Article>(Channel.UNLIMITED)
    private val translationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        // Process queue sequentially
        translationScope.launch {
            translationQueue.consumeAsFlow().collect { article ->
                translateArticle(article)
            }
        }
    }

    fun addToQueue(article: Article) {
        translationScope.launch {
            translationQueue.send(article)
        }
    }

    private suspend fun translateArticle(article: Article) {
        aiApi.translateWithProgress(
            texts = article.translatableTexts,
            scope = translationScope,
            targetLanguage = "Spanish"
        ).collect { progress ->
            // Update UI state
            _batchProgress.value = progress
        }
    }

    override fun onCleared() {
        super.onCleared()
        translationScope.cancel()
        translationQueue.close()
    }
}
```

---

## Testing

### Unit Test Example

```kotlin
class TranslationChunkerTest {

    @Test
    fun `chunks long text correctly`() {
        val chunker = TranslationChunker(maxChunkSize = 2000)

        val longText = generateLongText(5000) // 5000 characters
        val texts = listOf(longText)

        val chunks = chunker.chunk(texts)

        assertTrue(chunks.size > 1) // Should be multiple chunks
        assertTrue(chunks.all { it.text.length <= 2000 })
    }

    @Test
    fun `preserves order and indices`() {
        val chunker = TranslationChunker()
        val texts = listOf("para1", "para2", "para3")

        val chunks = chunker.chunk(texts)

        assertEquals(listOf(0, 1, 2), chunks.map { it.index })
    }
}
```

### Integration Test Example

```kotlin
@ExperimentalCoroutinesApi
class TranslationFlowTest {

    @Test
    fun `emits correct progress states`() = runTest {
        val aiApi = mockAIApi()
        val texts = List(100) { "Paragraph $it content here. " }

        val progressStates = mutableListOf<TranslationProgress>()

        val job = launch {
            aiApi.translateWithProgress(
                texts = texts,
                scope = this,
                targetLanguage = "Spanish"
            ).collect { progress ->
                progressStates.add(progress)
            }
        }

        // Wait for completion
        job.join()

        // Verify states
        assertTrue(progressStates.first() is TranslationProgress.Starting)
        assertTrue(progressStates.last() is TranslationProgress.Complete)
        assertTrue(progressStates.any { it is TranslationProgress.Translating })
    }
}
```

---

## Migration Guide

### From `translate()` to `translateWithProgress()`

**Before (Simple Translation):**

```kotlin
viewModelScope.launch {
    val translated = aiApi.translate(
        texts = article.translatableTexts,
        targetLanguage = "Spanish"
    )
    // Update UI
}
```

**After (Async with Progress):**

```kotlin
translationScope.launch {
    aiApi.translateWithProgress(
        texts = article.translatableTexts,
        scope = this,
        targetLanguage = "Spanish"
    ).collect { progress ->
        when (progress) {
            is TranslationProgress.Starting -> {
                // Show initial UI
            }
            is TranslationProgress.Translating -> {
                // Update progress bar
                updateProgress(progress.getProgressPercentage())
            }
            is TranslationProgress.Complete -> {
                // Display translated content
                displayTranslation(progress.translatedParagraphs)
            }
            is TranslationProgress.Error -> {
                // Handle error
                showError(progress.error)
            }
        }
    }
}
```

### Migration Checklist

- [ ] Update ViewModel to use `translateWithProgress()`
- [ ] Add `translationScope` for cancellation
- [ ] Implement progress UI components
- [ ] Add error handling for partial results
- [ ] Update UI state to handle progress states
- [ ] Test with long-form content (>30k words)
- [ ] Add cancellation on ViewModel clear

---

## Performance Considerations

### Memory Usage

- **Chunk Size**: 2000 characters = ~2KB per chunk
- **30k words** = ~150k characters = ~75 chunks = ~150KB in memory
- **Parallel Processing**: 3 concurrent chunks = ~6KB peak memory

### Network Usage

- **30k words** = ~75 API requests
- **Each request**: ~2KB payload
- **Total**: ~150KB uploaded, ~150KB downloaded

### Optimization Tips

1. **Adjust concurrency** based on rate limits:
   ```kotlin
   val coordinator = ChunkTranslationCoordinator(concurrency = 5)
   ```

2. **Increase chunk size** for faster translation (but higher memory):
   ```kotlin
   val chunker = TranslationChunker(maxChunkSize = 5000)
   ```

3. **Cache translations** to avoid re-translation:
   ```kotlin
   val cached = translationCache.get(texts, targetLanguage)
   if (cached != null) return cached
   ```

---

## Troubleshooting

### Common Issues

**Issue: Translation times out**
- **Solution**: Use `translateWithProgress()` instead of `translate()`

**Issue: Progress not updating**
- **Solution**: Ensure you're collecting the Flow in a coroutine scope

**Issue: Can't cancel translation**
- **Solution**: Use the coroutine scope passed to `translateWithProgress()`

**Issue: Partial translation result**
- **Solution**: Check `TranslationProgress.Error.partialResult`

---

## API Reference

See source code for detailed API documentation:
- `AIApi.kt` - Main API surface
- `TranslationProgress.kt` - Progress states
- `TranslationChunker.kt` - Chunking algorithm
- `ChunkTranslationCoordinator.kt` - Parallel processing

---

## Related Documentation

- [AI Summary Developer Guide](./AI_SUMMARY_DEVELOPER_GUIDE.md)
- [AI Provider Configuration](./AI_SUMMARY_SETTINGS.md)

---

## Changelog

See [CHANGELOG.md](../CHANGELOG.md) for recent updates to the translation system.
