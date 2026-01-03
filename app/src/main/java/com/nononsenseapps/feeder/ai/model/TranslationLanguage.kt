package com.nononsenseapps.feeder.ai.model

import androidx.annotation.StringRes
import com.nononsenseapps.feeder.R

/**
 * Translation language configuration for AI-powered translation.
 *
 * Users can select a specific language for translation or use device default.
 * When device default is selected, articles will be translated to the
 * device's configured language.
 *
 * @property code ISO 639-1 language code (empty for device default)
 * @property displayName String resource ID for display name
 * @property languageName Human-readable language name for prompts
 */
enum class TranslationLanguage(
    val code: String,
    @param:StringRes val displayName: Int,
    val languageName: String,
) {
    /**
     * Device Default: Translate to the device's configured language.
     * This setting automatically picks up the system language.
     */
    DEVICE_DEFAULT(
        code = "",
        displayName = R.string.translation_language_device_default,
        languageName = "the device's default",
    ),

    /**
     * English translation.
     */
    ENGLISH(
        code = "en",
        displayName = R.string.translation_language_english,
        languageName = "English",
    ),

    /**
     * Chinese translation.
     */
    CHINESE(
        code = "zh",
        displayName = R.string.translation_language_chinese,
        languageName = "Chinese",
    ),

    /**
     * Spanish translation.
     */
    SPANISH(
        code = "es",
        displayName = R.string.translation_language_spanish,
        languageName = "Spanish",
    ),

    /**
     * French translation.
     */
    FRENCH(
        code = "fr",
        displayName = R.string.translation_language_french,
        languageName = "French",
    ),

    /**
     * German translation.
     */
    GERMAN(
        code = "de",
        displayName = R.string.translation_language_german,
        languageName = "German",
    ),

    /**
     * Japanese translation.
     */
    JAPANESE(
        code = "ja",
        displayName = R.string.translation_language_japanese,
        languageName = "Japanese",
    ),

    /**
     * Korean translation.
     */
    KOREAN(
        code = "ko",
        displayName = R.string.translation_language_korean,
        languageName = "Korean",
    ),

    /**
     * Portuguese translation.
     */
    PORTUGUESE(
        code = "pt",
        displayName = R.string.translation_language_portuguese,
        languageName = "Portuguese",
    ),

    /**
     * Russian translation.
     */
    RUSSIAN(
        code = "ru",
        displayName = R.string.translation_language_russian,
        languageName = "Russian",
    ),

    /**
     * Arabic translation.
     */
    ARABIC(
        code = "ar",
        displayName = R.string.translation_language_arabic,
        languageName = "Arabic",
    ),

    /**
     * Hindi translation.
     */
    HINDI(
        code = "hi",
        displayName = R.string.translation_language_hindi,
        languageName = "Hindi",
    ),
    ;

    companion object {
        /**
         * Parse language code to TranslationLanguage, defaulting to DEVICE_DEFAULT.
         */
        fun fromCode(code: String?): TranslationLanguage =
            entries.firstOrNull { it.code == code } ?: DEVICE_DEFAULT
    }
}
