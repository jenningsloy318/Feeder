package com.nononsenseapps.feeder.localtranslation

/**
 * Result of an on-device translation.
 *
 * Mirrors the shape upstream's OpenAIApi.TranslationResult had, so the
 * localtranslation package stays a verbatim port. AI-provider translation
 * uses [com.nononsenseapps.feeder.ai.AIClient.TranslationResult] instead;
 * LocalTranslationClient maps between the two.
 */
sealed interface TranslationResult {
    val content: String

    data class Success(
        override val content: String,
        val detectedLanguage: String,
    ) : TranslationResult

    data class Error(
        override val content: String,
        val action: ErrorAction = ErrorAction.None,
    ) : TranslationResult

    enum class ErrorAction {
        None,
        OpenSystemTranslationSettings,
    }
}
