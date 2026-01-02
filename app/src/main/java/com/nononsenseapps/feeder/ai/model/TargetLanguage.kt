package com.nononsenseapps.feeder.ai.model

/**
 * Supported target languages for AI translation.
 *
 * @property code ISO 639-1 language code (e.g., "en", "zh", "es")
 * @property displayName English display name for the language
 * @property nativeName Native name of the language in its own script
 */
enum class TargetLanguage(
    val code: String,
    val displayName: String,
    val nativeName: String,
) {
    ENGLISH("en", "English", "English"),
    CHINESE("zh", "Chinese", "中文"),
    SPANISH("es", "Spanish", "Español"),
    FRENCH("fr", "French", "Français"),
    GERMAN("de", "German", "Deutsch"),
    JAPANESE("ja", "Japanese", "日本語"),
    KOREAN("ko", "Korean", "한국어"),
    PORTUGUESE("pt", "Portuguese", "Português"),
    RUSSIAN("ru", "Russian", "Русский"),
    ITALIAN("it", "Italian", "Italiano"),
    ARABIC("ar", "Arabic", "العربية"),
    HINDI("hi", "Hindi", "हिन्दी"),
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
