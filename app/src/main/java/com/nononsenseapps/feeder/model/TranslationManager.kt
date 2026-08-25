package com.nononsenseapps.feeder.model

import android.app.Application
import com.nononsenseapps.feeder.ai.AIClient
import com.nononsenseapps.feeder.ai.TranslatableText
import com.nononsenseapps.feeder.ai.model.AISettings
import com.nononsenseapps.feeder.ai.model.TranslationLanguage
import com.nononsenseapps.feeder.archmodel.Repository
import com.nononsenseapps.feeder.ui.compose.feed.FeedListItem
import com.nononsenseapps.feeder.util.FilePathProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import java.io.File
import java.security.MessageDigest

/**
 * Translates feed list items (titles and snippets) via the active AI
 * provider and caches results on disk.
 *
 * Ported from upstream master's TranslationManager, re-typed onto the
 * multi-provider ai/ architecture: settings are [AISettings] and
 * translation goes through [AIClient]. The on-disk cache format is
 * unchanged so caches written by upstream builds remain readable.
 */
class TranslationManager(
    override val di: DI,
) : DIAware {
    private val application: Application by instance()
    private val repository: Repository by instance()
    private val filePathProvider: FilePathProvider by instance()
    private val json =
        Json {
            ignoreUnknownKeys = true
            prettyPrint = false
        }

    /**
     * Returns the cached translation of a feed list item, if any.
     * Does not perform any network requests.
     */
    suspend fun getCachedTranslatedFeedListItem(
        item: FeedListItem,
        settings: AISettings,
        language: TranslationLanguage,
    ): CachedFeedListItemTranslation =
        withContext(Dispatchers.IO) {
            val targetLanguage = language.resolvedTargetCode()
            if (!settings.isValid || targetLanguage.isBlank()) {
                return@withContext CachedFeedListItemTranslation(
                    item = item,
                    hasCachedTranslation = false,
                    isFullyCached = false,
                )
            }

            val cache = loadCache(item.id, settings, targetLanguage)
            val titleHash = sha256(item.title)
            val snippetHash = sha256(item.snippet)

            val hasCachedTitle =
                item.title.isNotBlank() &&
                    cache.titleHash == titleHash &&
                    cache.translatedTitle.isUsableCachedTranslation(
                        original = item.title,
                        sourceLanguage = cache.sourceLanguage,
                        targetLanguage = targetLanguage,
                    )
            val hasCachedSnippet =
                item.snippet.isNotBlank() &&
                    cache.snippetHash == snippetHash &&
                    cache.translatedSnippet.isUsableCachedTranslation(
                        original = item.snippet,
                        sourceLanguage = cache.sourceLanguage,
                        targetLanguage = targetLanguage,
                    )
            val titleReady = item.title.isBlank() || hasCachedTitle
            val snippetReady = item.snippet.isBlank() || hasCachedSnippet

            CachedFeedListItemTranslation(
                item =
                    item.copy(
                        title = cache.translatedTitle.takeIf { hasCachedTitle } ?: item.title,
                        snippet = cache.translatedSnippet.takeIf { hasCachedSnippet } ?: item.snippet,
                    ),
                hasCachedTranslation = hasCachedTitle || hasCachedSnippet,
                isFullyCached = titleReady && snippetReady,
            )
        }

    /**
     * Translates a feed list item's title and snippet, returning cached
     * results when the source text is unchanged.
     */
    suspend fun translateFeedListItem(
        item: FeedListItem,
        settings: AISettings,
        language: TranslationLanguage,
    ): FeedListItem =
        withContext(Dispatchers.IO) {
            val targetLanguage = language.resolvedTargetCode()
            if (!settings.isValid || targetLanguage.isBlank()) {
                return@withContext item
            }

            val cache = loadCache(item.id, settings, targetLanguage)
            val titleHash = sha256(item.title)
            val snippetHash = sha256(item.snippet)
            val cachedTitle =
                cache.translatedTitle.takeIf {
                    cache.titleHash == titleHash &&
                        it.isUsableCachedTranslation(
                            original = item.title,
                            sourceLanguage = cache.sourceLanguage,
                            targetLanguage = targetLanguage,
                        )
                }
            val cachedSnippet =
                cache.translatedSnippet.takeIf {
                    cache.snippetHash == snippetHash &&
                        it.isUsableCachedTranslation(
                            original = item.snippet,
                            sourceLanguage = cache.sourceLanguage,
                            targetLanguage = targetLanguage,
                        )
                }

            val titleReady = item.title.isBlank() || cachedTitle != null
            val snippetReady = item.snippet.isBlank() || cachedSnippet != null
            if (titleReady && snippetReady) {
                return@withContext item.copy(
                    title = cachedTitle ?: item.title,
                    snippet = cachedSnippet ?: item.snippet,
                )
            }

            // Already in the target language: record and skip translation
            val detectedSameLanguage =
                detectSourceLanguageIfAlreadyTargetLanguage(
                    content =
                        listOf(item.title, item.snippet)
                            .filter(String::isNotBlank)
                            .joinToString(separator = "\n\n"),
                    targetLanguage = targetLanguage,
                )
            if (detectedSameLanguage != null) {
                val updatedCache =
                    cache.copy(
                        sourceLanguage = detectedSameLanguage,
                        titleHash = if (item.title.isBlank()) null else titleHash,
                        translatedTitle = if (item.title.isBlank()) null else item.title,
                        snippetHash = if (item.snippet.isBlank()) null else snippetHash,
                        translatedSnippet = if (item.snippet.isBlank()) null else item.snippet,
                    )
                if (updatedCache != cache) {
                    saveCache(item.id, settings, targetLanguage, updatedCache)
                }
                return@withContext item
            }

            val client = AIClient.create(settings, di)
            val translatedSnippet =
                cachedSnippet
                    ?: item.snippet
                        .takeIf { it.isNotBlank() }
                        ?.let { snippet ->
                            translatePlainText(client, snippet, language)
                        }.orEmpty()
            val translatedTitle =
                cachedTitle
                    ?: item.title
                        .takeIf { it.isNotBlank() }
                        ?.let { title ->
                            translatePlainText(client, title, language)
                        }.orEmpty()

            val updatedCache =
                cache.copy(
                    sourceLanguage = cache.sourceLanguage,
                    titleHash =
                        when {
                            item.title.isBlank() -> null
                            translatedTitle.isNotBlank() -> titleHash
                            else -> cache.titleHash
                        },
                    translatedTitle =
                        when {
                            item.title.isBlank() -> null
                            translatedTitle.isNotBlank() -> translatedTitle
                            else -> cache.translatedTitle
                        },
                    snippetHash =
                        when {
                            item.snippet.isBlank() -> null
                            translatedSnippet.isNotBlank() -> snippetHash
                            else -> cache.snippetHash
                        },
                    translatedSnippet =
                        when {
                            item.snippet.isBlank() -> null
                            translatedSnippet.isNotBlank() -> translatedSnippet
                            else -> cache.translatedSnippet
                        },
                )

            if (updatedCache != cache) {
                saveCache(item.id, settings, targetLanguage, updatedCache)
            }

            item.copy(
                title = translatedTitle.ifBlank { item.title },
                snippet = translatedSnippet.ifBlank { item.snippet },
            )
        }

    private suspend fun translatePlainText(
        client: AIClient,
        text: String,
        language: TranslationLanguage,
    ): String =
        when (
            val result =
                client.translate(listOf(TranslatableText.fromPlainText(text)), language)
        ) {
            is AIClient.TranslationResult.Success ->
                result.paragraphs
                    .firstOrNull()
                    ?.takeIf { it.isNotBlank() }
                    .orEmpty()
            is AIClient.TranslationResult.Error -> ""
        }

    private fun loadCache(
        itemId: Long,
        settings: AISettings,
        targetLanguage: String,
    ): CachedTranslations =
        cacheFile(itemId, settings, targetLanguage)
            .takeIf(File::isFile)
            ?.readText()
            ?.let { runCatching { json.decodeFromString<CachedTranslations>(it) }.getOrNull() }
            ?: CachedTranslations()

    private fun saveCache(
        itemId: Long,
        settings: AISettings,
        targetLanguage: String,
        cache: CachedTranslations,
    ) {
        val file = cacheFile(itemId, settings, targetLanguage)
        file.parentFile?.mkdirs()
        file.writeText(json.encodeToString(cache))
    }

    private fun cacheFile(
        itemId: Long,
        settings: AISettings,
        targetLanguage: String,
    ): File {
        val provider =
            when (settings) {
                is AISettings.OpenAI -> "openai"
                is AISettings.Anthropic -> "anthropic"
                is AISettings.DeepL -> "deepl"
                is AISettings.OnDevice -> "local"
            }
        return filePathProvider.cacheDir
            .resolve("translations")
            .resolve(
                "$itemId.$provider.${
                    targetLanguage
                        .trim()
                        .lowercase()
                        .replace(Regex("[^a-z0-9._-]+"), "_")
                        .ifBlank { "default" }
                }.json",
            )
    }

    private fun sha256(value: String): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString(separator = "") { byte -> "%02x".format(byte) }

    private fun detectSourceLanguageIfAlreadyTargetLanguage(
        content: String,
        targetLanguage: String,
    ): String? =
        runCatching {
            val detectionSamples =
                prepareTextSamplesForLanguageDetection(content)
                    .filter(::hasEnoughTextForLanguageDetection)
            if (detectionSamples.isEmpty()) {
                return@runCatching null
            }

            detectionSamples
                .map { sample ->
                    val detectedLanguage =
                        application
                            .detectLocaleFromText(
                                text = sample,
                                minConfidence = 95.0f,
                            ).firstOrNull()
                            ?.locale
                            ?.toLanguageTag()
                            ?: return@runCatching null

                    if (!detectedLanguageMatchesTranslationTarget(detectedLanguage, targetLanguage)) {
                        return@runCatching null
                    }

                    detectedLanguage
                }.firstOrNull()
        }.getOrNull()
}

data class CachedFeedListItemTranslation(
    val item: FeedListItem,
    val hasCachedTranslation: Boolean,
    val isFullyCached: Boolean,
)

@Serializable
internal data class CachedTranslations(
    val sourceLanguage: String = "",
    val titleHash: String? = null,
    val translatedTitle: String? = null,
    val snippetHash: String? = null,
    val translatedSnippet: String? = null,
    val articleHtmlHash: String? = null,
    val translatedArticleHtml: String? = null,
    val fullArticleHtmlHash: String? = null,
    val translatedFullArticleHtml: String? = null,
)
