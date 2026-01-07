package com.nononsenseapps.feeder.ai

import com.nononsenseapps.feeder.ai.model.TranslationLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import kotlin.math.pow
import kotlin.time.Duration.Companion.seconds

/**
 * Coordinates parallel translation of content chunks with progress reporting.
 *
 * This coordinator:
 * - Translates multiple chunks in parallel with controlled concurrency
 * - Emits real-time progress updates via Flow
 * - Implements exponential backoff retry for failed chunks
 * - Supports cancellation via CoroutineScope
 * - Assembles results in correct order
 *
 * Usage:
 * ```kotlin
 * val coordinator = ChunkTranslationCoordinator(
 *     aiClient = client,
 *     concurrency = 3,
 *     maxRetries = 3
 * )
 *
 * coordinator.translateChunks(chunks, targetLanguage)
 *     .collect { progress ->
 *         when (progress) {
 *             is TranslationProgress.Translating -> updateUI(progress)
 *             is TranslationProgress.Complete -> showResults(progress)
 *             is TranslationProgress.Error -> handleError(progress)
 *         }
 *     }
 * ```
 *
 * @param aiClient AI client for translation (OpenAI or Anthropic)
 * @param concurrency Maximum number of parallel chunk translations (default: 3)
 * @param maxRetries Maximum retry attempts for failed chunks (default: 3)
 * @param scope Coroutine scope for structured concurrency
 */
class ChunkTranslationCoordinator(
    private val aiClient: AIClient,
    private val concurrency: Int = DEFAULT_CONCURRENCY,
    private val maxRetries: Int = DEFAULT_MAX_RETRIES,
    private val scope: CoroutineScope,
) {
    init {
        require(concurrency > 0) { "Concurrency must be positive" }
        require(maxRetries >= 0) { "Max retries must be non-negative" }
    }

    /**
     * Translates chunks in parallel with progress reporting.
     *
     * Algorithm:
     * 1. Emit Starting state with total chunk count
     * 2. Process chunks in batches limited by concurrency
     * 3. Each chunk translates with retry logic
     * 4. Emit progress for each chunk completion
     * 5. Assemble results in order
     * 6. Emit Complete or Error state
     *
     * @param chunks List of chunks to translate
     * @param targetLanguage Target language for translation
     * @return Flow<TranslationProgress> with real-time updates
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun translateChunks(
        chunks: List<TranslationChunk>,
        targetLanguage: TranslationLanguage,
    ): Flow<TranslationProgress> = flow {
        if (chunks.isEmpty()) {
            emit(TranslationProgress.Error("No chunks to translate"))
            return@flow
        }

        // Emit starting state
        emit(TranslationProgress.Starting(totalChunks = chunks.size))

        // Process chunks in batches for concurrency control
        val results = mutableListOf<ChunkTranslationResult>()
        val batchSize = concurrency

        chunks.chunked(batchSize).forEachIndexed { batchIndex, batch ->
            // Process batch in parallel
            val batchResults =
                withContext(Dispatchers.IO) {
                    batch.map { chunk ->
                        async {
                            translateChunkWithRetry(chunk, targetLanguage)
                        }
                    }.awaitAll()
                }

            // Emit progress for each chunk in batch
            batchResults.forEachIndexed { indexInBatch, result ->
                val globalIndex = batchIndex * batchSize + indexInBatch
                emit(
                    TranslationProgress.ChunkComplete(
                        current = globalIndex + 1,
                        total = chunks.size,
                        result = result,
                    ),
                )
                results.add(result)
            }
        }

        // Emit final state
        val errors = results.filterIsInstance<ChunkTranslationResult.Error>()
        val successes = results.filterIsInstance<ChunkTranslationResult.Success>()

        when {
            errors.isNotEmpty() && successes.isEmpty() -> {
                // All chunks failed
                emit(
                    TranslationProgress.Error(
                        "Translation failed for all chunks. First error: ${errors.first().error}",
                    ),
                )
            }
            errors.isNotEmpty() -> {
                // Partial success
                emit(
                    TranslationProgress.Error(
                        "Translation completed with ${errors.size} errors. First error: ${errors.first().error}",
                    ),
                )
            }
            else -> {
                // Complete success - assemble results in order
                val sortedParagraphs =
                    successes
                        .sortedBy { it.chunkId }
                        .flatMap { it.translatedTexts }
                emit(TranslationProgress.Complete(translatedParagraphs = sortedParagraphs))
            }
        }
    }
        .onStart {
            // Emit initial translating state
            emit(TranslationProgress.Translating(current = 0, total = chunks.size))
        }

    /**
     * Translates a single chunk with exponential backoff retry.
     *
     * Retry strategy:
     * - Attempt 1: Immediate
     * - Attempt 2: Wait 1 second
     * - Attempt 3: Wait 2 seconds
     * - Attempt 4: Wait 4 seconds
     *
     * @param chunk Chunk to translate
     * @param targetLanguage Target language
     * @return ChunkTranslationResult with translated texts or error
     */
    private suspend fun translateChunkWithRetry(
        chunk: TranslationChunk,
        targetLanguage: TranslationLanguage,
    ): ChunkTranslationResult {
        repeat(maxRetries) { attempt ->
            try {
                return aiClient.translateChunk(chunk, targetLanguage)
            } catch (e: Exception) {
                val isLastAttempt = attempt == maxRetries - 1

                if (!isRetryableError(e) || isLastAttempt) {
                    return ChunkTranslationResult.Error(
                        chunkId = chunk.id,
                        error = e.message ?: "Unknown error",
                        canRetry = false,
                    )
                }

                // Exponential backoff: 2^attempt seconds
                val backoffDelaySeconds = 2.0.pow(attempt).toLong()
                delay(backoffDelaySeconds.seconds)
            }
        }

        return ChunkTranslationResult.Error(
            chunkId = chunk.id,
            error = "Max retries exceeded",
            canRetry = false,
        )
    }

    /**
     * Determines if an error is retryable based on exception type and message.
     *
     * Retryable: timeouts, rate limits, server errors (5xx)
     * Non-retryable: invalid API key, quota exceeded, client errors (4xx)
     */
    private fun isRetryableError(e: Exception): Boolean {
        val message = e.message?.lowercase() ?: ""
        return when {
            e is java.net.SocketTimeoutException -> true
            message.contains("timeout") -> true
            message.contains("rate limit") -> true
            message.contains("server error") -> true
            message.contains("5") -> true  // 5xx errors
            message.contains("invalid api key") -> false
            message.contains("quota exceeded") -> false
            message.contains("insufficient quota") -> false
            else -> false
        }
    }

    companion object {
        /**
         * Default concurrency level.
         * 3 parallel chunks balance speed and API rate limits.
         */
        const val DEFAULT_CONCURRENCY = 3

        /**
         * Default maximum retry attempts.
         * 3 retries with exponential backoff (1s, 2s, 4s).
         */
        const val DEFAULT_MAX_RETRIES = 3

        /**
         * Minimum concurrency (1 = sequential).
         */
        const val MIN_CONCURRENCY = 1

        /**
         * Maximum concurrency to prevent API overload.
         */
        const val MAX_CONCURRENCY = 5
    }
}
