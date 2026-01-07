package com.nononsenseapps.feeder.ai

/**
 * Splits content into chunks for efficient translation.
 *
 * This class implements paragraph-aware chunking that:
 * - Respects paragraph boundaries (never breaks mid-paragraph)
 * - Preserves structure metadata (element type, nesting level)
 * - Creates chunks of approximately the target size
 * - Maintains ordering information for result assembly
 *
 * Usage:
 * ```kotlin
 * val chunker = TranslationChunker(maxChunkSize = 2000)
 * val chunks = chunker.createChunks(translatableTexts)
 * ```
 *
 * @param maxChunkSize Maximum characters per chunk (default: 2000)
 */
class TranslationChunker(
    private val maxChunkSize: Int = TranslationChunk.DEFAULT_MAX_CHUNK_SIZE,
) {
    init {
        require(maxChunkSize >= TranslationChunk.MIN_CHUNK_SIZE) {
            "maxChunkSize must be at least ${TranslationChunk.MIN_CHUNK_SIZE}"
        }
    }

    /**
     * Splits translatable texts into chunks for translation.
     *
     * Algorithm:
     * 1. Iterate through texts in order
     * 2. Add texts to current chunk until adding next text would exceed maxChunkSize
     * 3. When chunk is full, finalize it and start a new chunk
     * 4. Never break a text element - always respect paragraph boundaries
     *
     * @param texts List of translatable texts with structure metadata
     * @return List of translation chunks in order
     */
    fun createChunks(
        texts: List<TranslatableText>,
    ): List<TranslationChunk> {
        if (texts.isEmpty()) {
            return emptyList()
        }

        val chunks = mutableListOf<TranslationChunk>()
        var currentChunk = mutableListOf<TranslatableText>()
        var currentSize = 0
        var startIndex = 0
        var chunkId = 0

        for (text in texts) {
            val textLength = text.text.length
            val wouldExceed = currentSize + textLength > maxChunkSize

            // Start new chunk if current chunk would exceed max size
            // AND current chunk is not empty (avoid empty chunks)
            if (wouldExceed && currentChunk.isNotEmpty()) {
                chunks.add(
                    TranslationChunk(
                        id = chunkId++,
                        texts = currentChunk.toList(),
                        characterCount = currentSize,
                        estimatedTokens = currentSize / 4,
                        startIndex = startIndex,
                        endIndex = startIndex + currentChunk.size,
                    ),
                )

                currentChunk = mutableListOf()
                currentSize = 0
                startIndex += currentChunk.size
            }

            // Add text to current chunk
            currentChunk.add(text)
            currentSize += textLength
        }

        // Add final chunk if not empty
        if (currentChunk.isNotEmpty()) {
            chunks.add(
                TranslationChunk(
                    id = chunkId,
                    texts = currentChunk,
                    characterCount = currentSize,
                    estimatedTokens = currentSize / 4,
                    startIndex = startIndex,
                    endIndex = startIndex + currentChunk.size,
                ),
            )
        }

        return chunks
    }

    /**
     * Checks if content needs chunking based on total character count.
     *
     * @param texts List of translatable texts
     * @return true if total size exceeds maxChunkSize
     */
    fun needsChunking(texts: List<TranslatableText>): Boolean {
        val totalSize = texts.sumOf { it.text.length }
        return totalSize > maxChunkSize
    }

    /**
     * Estimates the number of chunks that will be created.
     *
     * @param texts List of translatable texts
     * @return Estimated number of chunks (may be approximate)
     */
    fun estimateChunkCount(texts: List<TranslatableText>): Int {
        val totalSize = texts.sumOf { it.text.length }
        return if (totalSize <= maxChunkSize) {
            1
        } else {
            // Rough estimate, actual may vary based on paragraph boundaries
            (totalSize + maxChunkSize - 1) / maxChunkSize
        }
    }
}
