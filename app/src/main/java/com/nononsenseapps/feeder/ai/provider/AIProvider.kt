package com.nononsenseapps.feeder.ai.provider

/**
 * Enum representing supported AI providers.
 */
enum class AIProvider {
    /**
     * OpenAI-compatible API provider.
     * Supports OpenAI, Azure OpenAI, and other OpenAI-compatible endpoints.
     */
    OPENAI_COMPATIBLE,

    /**
     * Anthropic Claude API provider.
     * Supports Anthropic's Claude models and Anthropic-compatible endpoints.
     */
    ANTHROPIC,

    /**
     * DeepL translation provider.
     * Translation-only: no summarization or model listing.
     */
    DEEPL,

    /**
     * On-device offline translation provider.
     * Uses Android system translation with Bergamot WebView fallback;
     * translation models are downloaded from the translation settings.
     */
    ON_DEVICE,
    ;

    companion object {
        /**
         * Parse string to AIProvider, defaulting to OPENAI_COMPATIBLE.
         */
        fun fromString(value: String?): AIProvider =
            entries.firstOrNull {
                it.name.equals(value, ignoreCase = true)
            } ?: OPENAI_COMPATIBLE
    }
}
