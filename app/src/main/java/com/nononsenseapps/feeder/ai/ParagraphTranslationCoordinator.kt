package com.nononsenseapps.feeder.ai

import com.nononsenseapps.feeder.ai.model.TranslationLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.math.pow
import kotlin.time.Duration.Companion.seconds

sealed interface ParagraphTranslationProgress {
    data class ParagraphComplete(
        val paragraphIndex: Int,
        val translatedText: String,
    ) : ParagraphTranslationProgress

    data class ParagraphFailed(
        val paragraphIndex: Int,
        val errorMessage: String,
    ) : ParagraphTranslationProgress
}

class ParagraphTranslationCoordinator(
    private val aiClient: AIClient,
    private val paragraphConcurrency: Int = DEFAULT_PARAGRAPH_CONCURRENCY,
    private val paragraphMaxRetries: Int = DEFAULT_PARAGRAPH_MAX_RETRIES,
) {
    init {
        require(paragraphConcurrency > 0) { "Concurrency must be positive" }
        require(paragraphMaxRetries >= 0) { "Max retries must be non-negative" }
    }

    fun translateParagraphs(
        paragraphTexts: List<TranslatableText>,
        targetLanguage: TranslationLanguage,
    ): Flow<ParagraphTranslationProgress> =
        channelFlow {
            val semaphore = Semaphore(paragraphConcurrency)
            coroutineScope {
                paragraphTexts.forEachIndexed { zeroBasedIndex, paragraphText ->
                    launch(Dispatchers.IO) {
                        semaphore.withPermit {
                            val paragraphIndex = zeroBasedIndex + 1
                            val translationResult =
                                translateParagraphWithRetry(
                                    paragraphText,
                                    targetLanguage,
                                    paragraphIndex,
                                )
                            send(translationResult)
                        }
                    }
                }
            }
        }

    private suspend fun translateParagraphWithRetry(
        paragraphText: TranslatableText,
        targetLanguage: TranslationLanguage,
        paragraphIndex: Int,
    ): ParagraphTranslationProgress {
        repeat(paragraphMaxRetries) { attempt ->
            try {
                val translationResult =
                    aiClient.translate(
                        listOf(paragraphText),
                        targetLanguage,
                    )
                return when (translationResult) {
                    is AIClient.TranslationResult.Success -> {
                        ParagraphTranslationProgress.ParagraphComplete(
                            paragraphIndex = paragraphIndex,
                            translatedText = translationResult.paragraphs.firstOrNull() ?: "",
                        )
                    }
                    is AIClient.TranslationResult.Error -> {
                        val isLastAttempt = attempt == paragraphMaxRetries - 1
                        if (!isRetryableErrorMessage(translationResult.content) || isLastAttempt) {
                            ParagraphTranslationProgress.ParagraphFailed(
                                paragraphIndex = paragraphIndex,
                                errorMessage = translationResult.content,
                            )
                        } else {
                            val backoffDelaySeconds = 2.0.pow(attempt).toLong()
                            delay(backoffDelaySeconds.seconds)
                            return@repeat
                        }
                    }
                }
            } catch (e: Exception) {
                val isLastAttempt = attempt == paragraphMaxRetries - 1
                if (!isRetryableError(e) || isLastAttempt) {
                    return ParagraphTranslationProgress.ParagraphFailed(
                        paragraphIndex = paragraphIndex,
                        errorMessage = e.message ?: "Unknown error",
                    )
                }
                val backoffDelaySeconds = 2.0.pow(attempt).toLong()
                delay(backoffDelaySeconds.seconds)
            }
        }
        return ParagraphTranslationProgress.ParagraphFailed(
            paragraphIndex = paragraphIndex,
            errorMessage = "Max retries exceeded",
        )
    }

    private fun isRetryableErrorMessage(errorMessage: String): Boolean {
        val message = errorMessage.lowercase()
        return when {
            message.contains("timeout") -> true
            message.contains("rate limit") -> true
            message.contains("server error") -> true
            Regex("\\b5\\d{2}\\b").containsMatchIn(message) -> true
            message.contains("invalid api key") -> false
            message.contains("quota exceeded") -> false
            message.contains("insufficient quota") -> false
            else -> false
        }
    }

    private fun isRetryableError(translationException: Exception): Boolean {
        if (translationException is java.net.SocketTimeoutException) return true
        return isRetryableErrorMessage(translationException.message ?: "")
    }

    companion object {
        const val DEFAULT_PARAGRAPH_CONCURRENCY = 3
        const val DEFAULT_PARAGRAPH_MAX_RETRIES = 3
    }
}
