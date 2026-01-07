package com.nononsenseapps.feeder.ai

import kotlinx.serialization.Serializable

/**
 * Sealed class representing translation progress states.
 *
 * Provides real-time updates during the translation process, allowing the UI
 * to display progress bars, chunk counters, and handle user cancellation.
 */
@Serializable
sealed class TranslationProgress {
    /**
     * No translation in progress.
     */
    @Serializable
    data object Idle : TranslationProgress()

    /**
     * Translation is starting, preparing chunks.
     *
     * @property totalChunks Number of chunks to translate
     */
    @Serializable
    data class Starting(val totalChunks: Int) : TranslationProgress()

    /**
     * Currently translating a chunk.
     *
     * @property current Current chunk number (1-indexed)
     * @property total Total number of chunks
     * @property message Human-readable progress message
     */
    @Serializable
    data class Translating(
        val current: Int,
        val total: Int,
        val message: String = "Translating chunk $current of $total...",
    ) : TranslationProgress()

    /**
     * A chunk translation completed.
     *
     * @property current Chunk number that completed (1-indexed)
     * @property total Total number of chunks
     * @property result Result of the chunk translation
     */
    @Serializable
    data class ChunkComplete(
        val current: Int,
        val total: Int,
        val result: ChunkTranslationResult,
    ) : TranslationProgress()

    /**
     * Translation completed successfully.
     *
     * @property translatedParagraphs List of translated paragraphs in order
     */
    @Serializable
    data class Complete(
        val translatedParagraphs: List<String>,
    ) : TranslationProgress()

    /**
     * Translation failed with an error.
     *
     * @property error Human-readable error message
     */
    @Serializable
    data class Error(
        val error: String,
    ) : TranslationProgress()

    /**
     * Translation was cancelled by the user.
     */
    @Serializable
    data object Cancelled : TranslationProgress()

    /**
     * Returns the progress percentage (0-100) for progress states.
     * Returns null for terminal states (Complete, Error, Cancelled, Idle).
     */
    fun getProgressPercentage(): Int? =
        when (this) {
            is Translating -> if (total > 0) (current * 100) / total else 0
            is ChunkComplete -> if (total > 0) (current * 100) / total else 0
            is Starting -> 0
            else -> null
        }
}

/**
 * Result of translating a single chunk.
 */
@Serializable
sealed class ChunkTranslationResult {
    /**
     * Chunk translation succeeded.
     *
     * @property chunkId ID of the chunk that was translated
     * @property translatedTexts List of translated texts
     */
    @Serializable
    data class Success(
        val chunkId: Int,
        val translatedTexts: List<String>,
    ) : ChunkTranslationResult()

    /**
     * Chunk translation failed.
     *
     * @property chunkId ID of the chunk that failed
     * @property error Error message
     * @property canRetry Whether this error is retryable
     */
    @Serializable
    data class Error(
        val chunkId: Int,
        val error: String,
        val canRetry: Boolean = true,
    ) : ChunkTranslationResult()
}
