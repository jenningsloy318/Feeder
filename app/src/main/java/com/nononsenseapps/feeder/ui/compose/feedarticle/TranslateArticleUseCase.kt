package com.nononsenseapps.feeder.ui.compose.feedarticle

import com.nononsenseapps.feeder.ai.translation.ParagraphTranslation
import com.nononsenseapps.feeder.ai.translation.TranslationManager
import com.nononsenseapps.feeder.ai.translation.TranslationState
import com.nononsenseapps.feeder.ai.model.TargetLanguage
import com.nononsenseapps.feeder.archmodel.Repository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Use case for translating article content.
 *
 * Wraps TranslationManager and converts TranslationState to ArticleTranslationState
 * for use in the UI layer.
 */
class TranslateArticleUseCase(
    private val repository: Repository,
) {
    /**
     * Translates an article into the target language.
     *
     * @param articleId The ID of the article to translate
     * @param content The article content to translate
     * @param targetLanguage The target language for translation
     * @return Flow of ArticleTranslationState for UI observation
     */
    operator fun invoke(
        articleId: Long,
        content: String,
        targetLanguage: TargetLanguage,
    ): Flow<ArticleTranslationState> {
        val translationManager = TranslationManager(repository)

        return translationManager.translateArticle(articleId, content, targetLanguage)
            .map { translationState ->
                when (translationState) {
                    is TranslationState.Idle -> ArticleTranslationState.Idle
                    is TranslationState.Loading -> ArticleTranslationState.Loading(
                        progress = translationState.progress,
                        total = translationState.total,
                    )
                    is TranslationState.Success -> ArticleTranslationState.Success(
                        translations = translationState.translations,
                    )
                    is TranslationState.Error -> ArticleTranslationState.Error(
                        message = translationState.message,
                        retryable = translationState.retryable,
                    )
                    is TranslationState.Progress -> ArticleTranslationState.Loading(
                        progress = translationState.translations.size,
                        total = translationState.total,
                    )
                }
            }
            .catch { e ->
                emit(ArticleTranslationState.Error(
                    message = e.message ?: "Translation failed",
                    retryable = true,
                ))
            }
    }

    /**
     * Clears cached translations for an article in a specific language.
     */
    suspend fun clearTranslations(
        articleId: Long,
        targetLanguage: TargetLanguage,
    ) {
        val translationManager = TranslationManager(repository)
        translationManager.clearTranslations(articleId, targetLanguage)
    }

    /**
     * Clears all cached translations for an article.
     */
    suspend fun clearAllTranslations(articleId: Long) {
        val translationManager = TranslationManager(repository)
        translationManager.clearAllTranslations(articleId)
    }
}
