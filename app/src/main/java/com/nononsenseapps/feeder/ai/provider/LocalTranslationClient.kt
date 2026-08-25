package com.nononsenseapps.feeder.ai.provider

import com.nononsenseapps.feeder.ai.AIClient
import com.nononsenseapps.feeder.ai.TranslatableText
import com.nononsenseapps.feeder.ai.model.OnDeviceSettings
import com.nononsenseapps.feeder.ai.model.SummaryLanguage
import com.nononsenseapps.feeder.ai.model.TranslationLanguage
import com.nononsenseapps.feeder.localtranslation.LocalTranslator
import com.nononsenseapps.feeder.localtranslation.TranslationResult
import com.nononsenseapps.feeder.model.resolvedTargetCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.kodein.di.DI
import org.kodein.di.direct
import org.kodein.di.instance

/**
 * AIClient adapter for on-device offline translation.
 *
 * Routes translation through [LocalTranslator] (Android system
 * translation with a Bergamot WebView fallback). Summarization and
 * model listing are unsupported; models are managed from the
 * translation settings screen.
 *
 * Requires the app [DI] container; callers without DI access get a
 * clear error result instead.
 */
class LocalTranslationClient(
    private val di: DI?,
    private val settings: OnDeviceSettings = OnDeviceSettings(),
) : AIClient {
    private val localTranslator: LocalTranslator? by lazy { di?.direct?.instance() }

    override suspend fun listModels(): List<String> {
        // On-device translation has no models endpoint; model pairs are
        // managed from the translation settings screen.
        return emptyList()
    }

    override suspend fun generateSummary(
        content: String,
        language: SummaryLanguage,
    ): AIClient.SummaryResult =
        AIClient.SummaryResult.Error(
            content = "Summarization is not supported for this translation-only provider",
        )

    override suspend fun translate(
        translatableTexts: List<TranslatableText>,
        targetLanguage: TranslationLanguage,
    ): AIClient.TranslationResult {
        if (translatableTexts.isEmpty()) {
            return AIClient.TranslationResult.Error(
                content = "No translatable content found in this article",
            )
        }

        val translator =
            localTranslator
                ?: return AIClient.TranslationResult.Error(
                    content = "On-device translation is not available in this context",
                )

        val targetLanguageCode = targetLanguage.resolvedTargetCode()
        return withContext(Dispatchers.IO) {
            val translated =
                translatableTexts.map { text ->
                    when (
                        val result =
                            translator.translate(
                                content = text.text,
                                targetLanguage = targetLanguageCode,
                            )
                    ) {
                        is TranslationResult.Success -> result.content
                        is TranslationResult.Error ->
                            return@withContext AIClient.TranslationResult.Error(content = result.content)
                    }
                }
            AIClient.TranslationResult.Success(paragraphs = translated)
        }
    }
}
