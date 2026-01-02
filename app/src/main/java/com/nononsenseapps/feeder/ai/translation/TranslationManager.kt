package com.nononsenseapps.feeder.ai.translation

import com.nononsenseapps.feeder.ai.AIClient
import com.nononsenseapps.feeder.ai.model.TargetLanguage
import com.nononsenseapps.feeder.archmodel.Repository
import com.nononsenseapps.feeder.db.room.Translation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow

/**
 * Manager class for orchestrating AI article translation operations.
 *
 * Handles:
 * - Checking cache for existing translations
 * - Splitting articles into paragraphs
 * - Batch translation of paragraphs
 * - Caching translation results
 * - State management for UI observation
 */
class TranslationManager(
    private val repository: Repository,
) {
    private val _state = MutableStateFlow<TranslationState>(TranslationState.Idle)
    val state: Flow<TranslationState> = _state.asStateFlow()

    /**
     * Translates an article into the target language.
     *
     * This method:
     * 1. Checks if translations already exist in cache
     * 2. Splits the article content into paragraphs
     * 3. Translates paragraphs in batches
     * 4. Caches the results
     * 5. Emits state updates for UI observation
     *
     * @param articleId The ID of the article to translate
     * @param content The article content to translate
     * @param targetLanguage The target language code
     */
    fun translateArticle(
        articleId: Long,
        content: String,
        targetLanguage: TargetLanguage,
    ) = flow {
        emit(TranslationState.Idle)

        // Check if translations already exist in cache
        val cachedTranslations = repository.db().translationDao()
            .getTranslations(articleId, targetLanguage.code)

        if (cachedTranslations.isNotEmpty()) {
            val paragraphs = cachedTranslations.map { translation ->
                ParagraphTranslation(
                    index = translation.paragraphIndex,
                    original = translation.originalParagraph,
                    translated = translation.translatedParagraph,
                )
            }
            emit(TranslationState.Success(paragraphs))
            return@flow
        }

        // Split content into paragraphs
        val paragraphs = splitIntoParagraphs(content)

        if (paragraphs.isEmpty()) {
            emit(TranslationState.Error(
                message = "No paragraphs to translate",
                retryable = false,
            ))
            return@flow
        }

        emit(TranslationState.Loading(progress = 0, total = paragraphs.size))

        val translatedParagraphs = mutableListOf<ParagraphTranslation>()
        val translationEntities = mutableListOf<Translation>()

        // Get AI client
        val aiClient = AIClient.create(repository.aiSettings)

        // Translate paragraphs in batches
        val batchSize = 5
        for (i in paragraphs.indices step batchSize) {
            val batch = paragraphs.drop(i).take(batchSize)
            val batchResults = mutableListOf<ParagraphTranslation>()

            for ((index, paragraph) in batch.withIndex()) {
                val globalIndex = i + index
                when (val result = aiClient.translate(paragraph, targetLanguage.code)) {
                    is AIClient.TranslationResult.Success -> {
                        val translation = ParagraphTranslation(
                            index = globalIndex,
                            original = paragraph,
                            translated = result.translatedText,
                        )
                        batchResults.add(translation)
                        translatedParagraphs.add(translation)

                        // Create database entity
                        translationEntities.add(
                            Translation(
                                articleId = articleId,
                                targetLanguage = targetLanguage.code,
                                originalParagraph = paragraph,
                                translatedParagraph = result.translatedText,
                                paragraphIndex = globalIndex,
                                aiProvider = aiClient.providerName,
                                aiModel = aiClient.modelName,
                            )
                        )
                    }
                    is AIClient.TranslationResult.Error -> {
                        emit(TranslationState.Error(
                            message = result.message,
                            retryable = result.retryable,
                            partialTranslations = translatedParagraphs.toList(),
                        ))
                        return@flow
                    }
                }

                // Emit progress update
                emit(TranslationState.Loading(
                    progress = translatedParagraphs.size,
                    total = paragraphs.size,
                ))
            }

            // Save batch to database
            repository.db().translationDao().insertAll(translationEntities)
            translationEntities.clear()

            // Emit intermediate progress
            emit(TranslationState.Progress(translatedParagraphs.toList()))
        }

        emit(TranslationState.Success(translatedParagraphs.toList()))
    }.catch { e ->
        emit(TranslationState.Error(
            message = e.message ?: "Translation failed",
            retryable = true,
        ))
    }

    /**
     * Clears cached translations for an article in a specific language.
     */
    suspend fun clearTranslations(
        articleId: Long,
        targetLanguage: TargetLanguage,
    ) {
        repository.db().translationDao().delete(articleId, targetLanguage.code)
        _state.value = TranslationState.Idle
    }

    /**
     * Clears all cached translations for an article (all languages).
     */
    suspend fun clearAllTranslations(articleId: Long) {
        repository.db().translationDao().deleteAll(articleId)
        _state.value = TranslationState.Idle
    }

    /**
     * Splits article content into paragraphs.
     *
     * Paragraphs are split by double newlines. Empty paragraphs are filtered out.
     *
     * @param content The article content
     * @return List of non-empty paragraphs
     */
    private fun splitIntoParagraphs(content: String): List<String> {
        return content
            .split("\n\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }
}
