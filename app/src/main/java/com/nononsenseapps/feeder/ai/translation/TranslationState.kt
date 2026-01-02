package com.nononsenseapps.feeder.ai.translation

/**
 * Sealed class representing the state of a translation operation.
 *
 * This state is used to track the progress of article translation,
 * from idle to loading to completion or error.
 */
sealed class TranslationState {
    /**
     * No translation operation has been initiated.
     */
    object Idle : TranslationState()

    /**
     * Translation is currently in progress.
     *
     * @property progress Current number of paragraphs translated
     * @property total Total number of paragraphs to translate
     */
    data class Loading(
        val progress: Int,
        val total: Int,
    ) : TranslationState()

    /**
     * Partial progress update during translation.
     *
     * @property translations List of translations completed so far
     */
    data class Progress(
        val translations: List<ParagraphTranslation>,
    ) : TranslationState()

    /**
     * Translation completed successfully.
     *
     * @property translations Complete list of translated paragraphs
     */
    data class Success(
        val translations: List<ParagraphTranslation>,
    ) : TranslationState()

    /**
     * Translation failed with an error.
     *
     * @property message Human-readable error message
     * @property retryable Whether the operation can be retried
     * @property partialTranslations Any translations that completed before the error
     */
    data class Error(
        val message: String,
        val retryable: Boolean = true,
        val partialTranslations: List<ParagraphTranslation> = emptyList(),
    ) : TranslationState()
}
