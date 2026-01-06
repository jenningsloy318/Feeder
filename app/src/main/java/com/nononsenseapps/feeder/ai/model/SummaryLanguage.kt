package com.nononsenseapps.feeder.ai.model

import androidx.annotation.StringRes
import com.nononsenseapps.feeder.R

/**
 * Summary language configuration for AI-generated summaries.
 *
 * Users can select a specific language for summaries or use auto-detect.
 * When auto-detect is selected, the AI will detect the article's language
 * and summarize in that same language.
 *
 * @property code ISO 639-1 language code (empty for auto-detect)
 * @property displayName String resource ID for display name
 * @property languageName Human-readable language name for prompts
 */
enum class SummaryLanguage(
    val code: String,
    @param:StringRes val displayName: Int,
    val languageName: String,
) {
    /**
     * Auto-detect: AI detects the article's language and summarizes in that same language.
     * The response will include a "Lang:" prefix with the detected language code.
     */
    AUTO_DETECT(
        code = "",
        displayName = R.string.summary_language_auto_detect,
        languageName = "the article's original",
    ),

    /**
     * English summaries.
     */
    ENGLISH(
        code = "en",
        displayName = R.string.summary_language_english,
        languageName = "English",
    ),

    /**
     * Chinese summaries.
     */
    CHINESE(
        code = "zh",
        displayName = R.string.summary_language_chinese,
        languageName = "Chinese",
    ),

    /**
     * Spanish summaries.
     */
    SPANISH(
        code = "es",
        displayName = R.string.summary_language_spanish,
        languageName = "Spanish",
    ),

    /**
     * French summaries.
     */
    FRENCH(
        code = "fr",
        displayName = R.string.summary_language_french,
        languageName = "French",
    ),

    /**
     * German summaries.
     */
    GERMAN(
        code = "de",
        displayName = R.string.summary_language_german,
        languageName = "German",
    ),

    /**
     * Japanese summaries.
     */
    JAPANESE(
        code = "ja",
        displayName = R.string.summary_language_japanese,
        languageName = "Japanese",
    ),

    /**
     * Korean summaries.
     */
    KOREAN(
        code = "ko",
        displayName = R.string.summary_language_korean,
        languageName = "Korean",
    ),

    /**
     * Portuguese summaries.
     */
    PORTUGUESE(
        code = "pt",
        displayName = R.string.summary_language_portuguese,
        languageName = "Portuguese",
    ),

    /**
     * Russian summaries.
     */
    RUSSIAN(
        code = "ru",
        displayName = R.string.summary_language_russian,
        languageName = "Russian",
    ),

    /**
     * Arabic summaries.
     */
    ARABIC(
        code = "ar",
        displayName = R.string.summary_language_arabic,
        languageName = "Arabic",
    ),

    /**
     * Hindi summaries.
     */
    HINDI(
        code = "hi",
        displayName = R.string.summary_language_hindi,
        languageName = "Hindi",
    ),
    ;

    companion object {
        /**
         * Parse language code to SummaryLanguage, defaulting to AUTO_DETECT.
         */
        fun fromCode(code: String?): SummaryLanguage = entries.firstOrNull { it.code == code } ?: AUTO_DETECT
    }
}
