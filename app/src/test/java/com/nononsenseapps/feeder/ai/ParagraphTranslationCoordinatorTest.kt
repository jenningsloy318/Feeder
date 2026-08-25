package com.nononsenseapps.feeder.ai

import com.nononsenseapps.feeder.ai.model.TranslationLanguage
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ParagraphTranslationCoordinatorTest {
    private val targetLanguage = TranslationLanguage.CHINESE

    private fun createParagraphs(count: Int): List<TranslatableText> = (1..count).map { TranslatableText.fromPlainText("Paragraph $it") }

    private fun createSuccessMock(delayMs: Long = 0): MockAIClient = MockAIClient(delayMs = delayMs)

    @Test
    fun translateParagraphs_should_emitCompleteForEachParagraph() =
        runTest {
            val mockClient = createSuccessMock()
            val coordinator = ParagraphTranslationCoordinator(aiClient = mockClient)
            val paragraphs = createParagraphs(3)

            val results = coordinator.translateParagraphs(paragraphs, targetLanguage).toList()

            val completedResults = results.filterIsInstance<ParagraphTranslationProgress.ParagraphComplete>()
            assertEquals(3, completedResults.size)

            val indices = completedResults.map { it.paragraphIndex }.sorted()
            assertEquals(listOf(1, 2, 3), indices)
        }

    @Test
    fun translateParagraphs_should_limitConcurrentCalls() =
        runTest {
            val maxConcurrent = AtomicInteger(0)
            val currentConcurrent = AtomicInteger(0)

            val mockClient =
                MockAIClient(
                    delayMs = 100,
                    onTranslateStart = {
                        val current = currentConcurrent.incrementAndGet()
                        maxConcurrent.updateAndGet { max -> maxOf(max, current) }
                    },
                    onTranslateEnd = {
                        currentConcurrent.decrementAndGet()
                    },
                )

            val concurrencyLimit = 2
            val coordinator =
                ParagraphTranslationCoordinator(
                    aiClient = mockClient,
                    paragraphConcurrency = concurrencyLimit,
                )
            val paragraphs = createParagraphs(6)

            coordinator.translateParagraphs(paragraphs, targetLanguage).toList()

            assertTrue(
                maxConcurrent.get() <= concurrencyLimit,
                "Max concurrent calls (${maxConcurrent.get()}) should not exceed limit ($concurrencyLimit)",
            )
        }

    @Test
    fun translateParagraphs_should_emitImmediately_when_paragraphCompletes() =
        runTest {
            val mockClient = createSuccessMock()
            val coordinator = ParagraphTranslationCoordinator(aiClient = mockClient)
            val paragraphs = createParagraphs(3)

            val results = coordinator.translateParagraphs(paragraphs, targetLanguage).toList()

            assertEquals(3, results.size)
            results.forEach { result ->
                assertIs<ParagraphTranslationProgress>(result)
            }
        }

    @Test
    fun translateParagraphs_should_handleSingleParagraph() =
        runTest {
            val mockClient = createSuccessMock()
            val coordinator = ParagraphTranslationCoordinator(aiClient = mockClient)
            val paragraphs = createParagraphs(1)

            val results = coordinator.translateParagraphs(paragraphs, targetLanguage).toList()

            assertEquals(1, results.size)
            val result = results.first()
            assertIs<ParagraphTranslationProgress.ParagraphComplete>(result)
            assertEquals(1, result.paragraphIndex)
        }

    @Test
    fun translateParagraphs_should_returnEmpty_when_inputIsEmpty() =
        runTest {
            val mockClient = createSuccessMock()
            val coordinator = ParagraphTranslationCoordinator(aiClient = mockClient)

            val results = coordinator.translateParagraphs(emptyList(), targetLanguage).toList()

            assertTrue(results.isEmpty())
        }

    @Test
    fun translateParagraphs_should_emitParagraphFailed_when_allRetriesExhausted() =
        runTest {
            val mockClient = MockAIClient(alwaysFail = true)
            val coordinator =
                ParagraphTranslationCoordinator(
                    aiClient = mockClient,
                    paragraphMaxRetries = 3,
                )
            val paragraphs = createParagraphs(1)

            val results = coordinator.translateParagraphs(paragraphs, targetLanguage).toList()

            assertEquals(1, results.size)
            val result = results.first()
            assertIs<ParagraphTranslationProgress.ParagraphFailed>(result)
            assertEquals(1, result.paragraphIndex)
        }

    @Test
    fun translateParagraphs_should_retryOnRetryableError() =
        runTest {
            var callCount = 0
            val mockClient =
                MockAIClient(
                    onTranslate = {
                        callCount++
                        if (callCount == 1) {
                            throw SocketTimeoutException("Connection timed out")
                        }
                        AIClient.TranslationResult.Success(paragraphs = listOf("Translated"))
                    },
                )
            val coordinator =
                ParagraphTranslationCoordinator(
                    aiClient = mockClient,
                    paragraphMaxRetries = 3,
                )
            val paragraphs = createParagraphs(1)

            val results = coordinator.translateParagraphs(paragraphs, targetLanguage).toList()

            assertEquals(1, results.size)
            assertIs<ParagraphTranslationProgress.ParagraphComplete>(results.first())
            assertTrue(callCount >= 2, "Should have retried at least once, callCount=$callCount")
        }

    @Test
    fun isRetryableError_should_returnTrue_when_socketTimeout() =
        runTest {
            var callCount = 0
            val mockClient =
                MockAIClient(
                    onTranslate = {
                        callCount++
                        if (callCount <= 2) {
                            throw SocketTimeoutException("Connection timed out")
                        }
                        AIClient.TranslationResult.Success(paragraphs = listOf("Translated"))
                    },
                )
            val coordinator =
                ParagraphTranslationCoordinator(
                    aiClient = mockClient,
                    paragraphMaxRetries = 3,
                )
            val paragraphs = createParagraphs(1)

            val results = coordinator.translateParagraphs(paragraphs, targetLanguage).toList()

            // SocketTimeoutException is retryable, so after 2 failures it should succeed on 3rd call
            assertEquals(1, results.size)
            assertIs<ParagraphTranslationProgress.ParagraphComplete>(results.first())
            assertEquals(3, callCount, "Should have retried twice before succeeding")
        }

    @Test
    fun isRetryableError_should_returnFalse_when_invalidApiKey() =
        runTest {
            var callCount = 0
            val mockClient =
                MockAIClient(
                    onTranslate = {
                        callCount++
                        throw Exception("invalid api key")
                    },
                )
            val coordinator =
                ParagraphTranslationCoordinator(
                    aiClient = mockClient,
                    paragraphMaxRetries = 3,
                )
            val paragraphs = createParagraphs(1)

            val results = coordinator.translateParagraphs(paragraphs, targetLanguage).toList()

            // "invalid api key" is non-retryable, should fail immediately without retry
            assertEquals(1, results.size)
            assertIs<ParagraphTranslationProgress.ParagraphFailed>(results.first())
            assertEquals(1, callCount, "Should not retry on non-retryable error")
        }
}

/**
 * Mock AIClient for testing ParagraphTranslationCoordinator.
 *
 * Provides configurable behavior for translate() calls.
 */
private class MockAIClient(
    private val delayMs: Long = 0,
    private val alwaysFail: Boolean = false,
    private val onTranslateStart: (() -> Unit)? = null,
    private val onTranslateEnd: (() -> Unit)? = null,
    private val onTranslate: (suspend () -> AIClient.TranslationResult)? = null,
) : AIClient {
    override suspend fun listModels(): List<String> = emptyList()

    override suspend fun generateSummary(
        content: String,
        language: com.nononsenseapps.feeder.ai.model.SummaryLanguage,
    ): AIClient.SummaryResult = AIClient.SummaryResult.Error(content = "Not implemented")

    override suspend fun translate(
        translatableTexts: List<TranslatableText>,
        targetLanguage: TranslationLanguage,
    ): AIClient.TranslationResult {
        onTranslateStart?.invoke()
        try {
            if (onTranslate != null) {
                return onTranslate.invoke()
            }

            if (delayMs > 0) {
                kotlinx.coroutines.delay(delayMs)
            }

            if (alwaysFail) {
                return AIClient.TranslationResult.Error(content = "Mock translation error")
            }

            val translatedParagraphs =
                translatableTexts.map { "Translated: ${it.text}" }
            return AIClient.TranslationResult.Success(paragraphs = translatedParagraphs)
        } finally {
            onTranslateEnd?.invoke()
        }
    }
}
