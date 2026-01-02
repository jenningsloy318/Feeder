package com.nononsenseapps.feeder.ai.translation

/**
 * Data class representing a single translated paragraph.
 *
 * @property index The zero-based index of this paragraph in the article
 * @property original The original paragraph text in the source language
 * @property translated The translated paragraph text in the target language
 */
data class ParagraphTranslation(
    val index: Int,
    val original: String,
    val translated: String,
)
