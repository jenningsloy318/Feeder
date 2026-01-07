package com.nononsenseapps.feeder.ai

import kotlinx.serialization.Serializable

/**
 * Represents a chunk of content prepared for translation.
 *
 * Chunks are created by splitting long content into smaller pieces that can be
 * translated efficiently without hitting provider timeouts or token limits.
 *
 * @property id Unique identifier for this chunk (0-indexed)
 * @property texts List of translatable texts in this chunk with structure metadata
 * @property characterCount Total characters in this chunk
 * @property estimatedTokens Estimated token count (roughly characters / 4)
 * @property startIndex Starting index in original text list
 * @property endIndex Ending index in original text list
 */
@Serializable
data class TranslationChunk(
    val id: Int,
    val texts: List<TranslatableText>,
    val characterCount: Int,
    val estimatedTokens: Int,
    val startIndex: Int,
    val endIndex: Int,
) {
    /**
     * Returns the number of paragraphs/texts in this chunk.
     */
    val size: Int
        get() = texts.size

    /**
     * Returns a formatted description of this chunk.
     */
    fun getDescription(): String =
        "Chunk $id (${startIndex + 1}-$endIndex): $characterCount chars, ~$estimatedTokens tokens"

    companion object {
        /**
         * Maximum chunk size in characters (configurable).
         * Default 2000 characters provides good balance between context and speed.
         */
        const val DEFAULT_MAX_CHUNK_SIZE = 2000

        /**
         * Minimum chunk size in characters.
         * Prevents creating too many tiny chunks.
         */
        const val MIN_CHUNK_SIZE = 500
    }
}
