package com.nononsenseapps.feeder.ai.model

import androidx.annotation.StringRes
import com.nononsenseapps.feeder.R

/**
 * Supported target languages for AI translation.
 *
 * @property code ISO 639-1 language code (e.g., "en", "zh", "es")
 * @property displayName String resource ID for display name
 * @property languageName Human-readable language name for prompts
 */
enum class TargetLanguage(
    val code: String,
    @StringRes val displayName: Int,
    val languageName: String,
) {
    ENGLISH("en", R.string.translation_language_english, "English"),
    CHINESE("zh", R.string.translation_language_chinese, "Chinese"),
    SPANISH("es", R.string.translation_language_spanish, "Spanish"),
    FRENCH("fr", R.string.translation_language_french, "French"),
    GERMAN("de", R.string.translation_language_german, "German"),
    JAPANESE("ja", R.string.translation_language_japanese, "Japanese"),
    KOREAN("ko", R.string.translation_language_korean, "Korean"),
    PORTUGUESE("pt", R.string.translation_language_portuguese, "Portuguese"),
    RUSSIAN("ru", R.string.translation_language_russian, "Russian"),
    ITALIAN("it", R.string.translation_language_italian, "Italian"),
    ARABIC("ar", R.string.translation_language_arabic, "Arabic"),
    HINDI("hi", R.string.translation_language_hindi, "Hindi"),
    ;

    companion object {
        /**
         * Find a TargetLanguage by its ISO 639-1 code.
         *
         * @param code The language code (e.g., "en", "zh")
         * @return The matching TargetLanguage, or null if not found
         */
        fun fromCode(code: String?): TargetLanguage? {
            return entries.firstOrNull { it.code == code }
        }
    }
}
