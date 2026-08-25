package com.nononsenseapps.feeder.ai.provider

import com.nononsenseapps.feeder.ai.AIClient
import com.nononsenseapps.feeder.ai.TranslatableText
import com.nononsenseapps.feeder.ai.model.DeepLSettings
import com.nononsenseapps.feeder.ai.model.SummaryLanguage
import com.nononsenseapps.feeder.ai.model.TranslationLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * DeepL translation API client.
 *
 * DeepL is a translation-only provider: it does not support summarization
 * or model listing. [listModels] performs a cheap verification request
 * instead, mirroring upstream behaviour, so connection tests in the
 * provider settings still work.
 *
 * Paragraph translation uses DeepL's native batch endpoint: all input
 * texts are sent in a single request and the response list maps 1:1 to
 * the input order.
 */
class DeepLClient(
    private val settings: DeepLSettings,
) : AIClient {
    private val json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            encodeDefaults = true
        }

    @Serializable
    private data class DeepLTranslateRequest(
        val text: List<String>,
        val target_lang: String,
        val split_sentences: String = "nonewlines",
        val preserve_formatting: Boolean = true,
    )

    @Serializable
    private data class DeepLTranslation(
        val detected_source_language: String = "",
        val text: String,
    )

    @Serializable
    private data class DeepLTranslateResponse(
        val translations: List<DeepLTranslation> = emptyList(),
    )

    @Serializable
    private data class DeepLErrorResponse(
        val message: String = "",
    )

    override suspend fun listModels(): List<String> {
        if (settings.key.isEmpty()) {
            throw DeepLClientException("Missing API key")
        }
        // DeepL has no models endpoint; verify credentials with a minimal
        // translation request instead.
        val result =
            translateTexts(
                texts = listOf("Hello"),
                targetLanguageCode = "DE",
            )
        if (result.isEmpty()) {
            throw DeepLClientException("DeepL verification failed: no translation was returned")
        }
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
        if (settings.key.isEmpty()) {
            return AIClient.TranslationResult.Error(content = "Missing DeepL API key")
        }

        val targetLanguageCode =
            targetLanguage.toDeepLTargetLanguageCode()
                ?: return AIClient.TranslationResult.Error(
                    content = "DeepL does not support translating to ${targetLanguage.languageName}",
                )

        return try {
            val texts = translatableTexts.map { it.text }
            val translatedParagraphs = translateTexts(texts, targetLanguageCode)
            if (translatedParagraphs.size != translatableTexts.size) {
                AIClient.TranslationResult.Error(
                    content =
                        "DeepL returned ${translatedParagraphs.size} translations " +
                            "for ${translatableTexts.size} texts",
                )
            } else {
                AIClient.TranslationResult.Success(paragraphs = translatedParagraphs)
            }
        } catch (e: Exception) {
            AIClient.TranslationResult.Error(content = e.message ?: e.cause?.message ?: "DeepL request failed")
        }
    }

    private suspend fun translateTexts(
        texts: List<String>,
        targetLanguageCode: String,
    ): List<String> =
        withContext(Dispatchers.IO) {
            val requestBody =
                json.encodeToString(
                    DeepLTranslateRequest(
                        text = texts,
                        target_lang = targetLanguageCode,
                    ),
                )

            val request =
                Request
                    .Builder()
                    .url(settings.toDeepLTranslateUrl())
                    .header("Authorization", "DeepL-Auth-Key ${settings.key}")
                    .post(requestBody.toRequestBody("application/json".toMediaType()))
                    .build()

            client.newCall(request).execute().use { response ->
                val body = response.body.string()
                if (!response.isSuccessful) {
                    throw DeepLClientException(
                        "DeepL request failed (${response.code}): ${body.deepLErrorsOrFallback(response.message)}",
                    )
                }
                val parsed = json.decodeFromString<DeepLTranslateResponse>(body)
                parsed.translations.map { it.text }
            }
        }

    private val client: OkHttpClient by lazy {
        OkHttpClient
            .Builder()
            .callTimeout(
                settings.timeoutSeconds.coerceIn(MIN_TIMEOUT_SECONDS, MAX_TIMEOUT_SECONDS).toLong(),
                TimeUnit.SECONDS,
            ).build()
    }

    private fun String.deepLErrorsOrFallback(fallback: String): String =
        runCatching { json.decodeFromString<DeepLErrorResponse>(this).message }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: fallback

    private class DeepLClientException(
        message: String,
    ) : Exception(message)

    companion object {
        private const val MIN_TIMEOUT_SECONDS = 30
        private const val MAX_TIMEOUT_SECONDS = 600
    }
}

/**
 * Normalize the configured base URL for DeepL requests.
 *
 * An empty base URL selects the official endpoint based on the API key
 * (`:fx` suffix = free tier). Explicitly configured official URLs are
 * normalized the same way; anything else is treated as a self-hosted
 * proxy and used as-is.
 */
fun DeepLSettings.toDeepLTranslateUrl(): String = "${normalizedDeepLBaseUrl()}/v2/translate"

private fun DeepLSettings.normalizedDeepLBaseUrl(): String {
    val normalizedBaseUrl = baseUrl.trim().trimEnd('/').removeSuffix("/v2/translate")
    val defaultBaseUrl =
        if (key.endsWith(":fx")) {
            "https://api-free.deepl.com"
        } else {
            "https://api.deepl.com"
        }

    return when {
        normalizedBaseUrl.isBlank() -> defaultBaseUrl
        normalizedBaseUrl.equals("https://api.deepl.com", ignoreCase = true) -> defaultBaseUrl
        normalizedBaseUrl.equals("https://api-free.deepl.com", ignoreCase = true) -> defaultBaseUrl
        else -> normalizedBaseUrl
    }
}

/**
 * Map a [TranslationLanguage] to a DeepL target language code.
 *
 * Returns null when DeepL does not support the target language
 * (currently only Hindi among the selectable languages).
 *
 * DEVICE_DEFAULT resolves from the device locale, including regional
 * variants DeepL distinguishes (EN-GB/EN-US, PT-BR/PT-PT).
 */
fun TranslationLanguage.toDeepLTargetLanguageCode(): String? =
    when (this) {
        TranslationLanguage.DEVICE_DEFAULT -> Locale.getDefault().deviceLocaleToDeepLCode()
        TranslationLanguage.ENGLISH -> "EN"
        TranslationLanguage.CHINESE -> "ZH"
        TranslationLanguage.SPANISH -> "ES"
        TranslationLanguage.FRENCH -> "FR"
        TranslationLanguage.GERMAN -> "DE"
        TranslationLanguage.JAPANESE -> "JA"
        TranslationLanguage.KOREAN -> "KO"
        TranslationLanguage.PORTUGUESE -> "PT"
        TranslationLanguage.RUSSIAN -> "RU"
        TranslationLanguage.ARABIC -> "AR"
        TranslationLanguage.HINDI -> null
    }

private fun Locale.deviceLocaleToDeepLCode(): String {
    val language = language.uppercase(Locale.ROOT)
    val region = country.uppercase(Locale.ROOT)
    return when {
        language == "PT" && region == "BR" -> "PT-BR"
        language == "PT" && region == "PT" -> "PT-PT"
        language == "EN" && region == "GB" -> "EN-GB"
        language == "EN" && region == "US" -> "EN-US"
        else -> language
    }
}
