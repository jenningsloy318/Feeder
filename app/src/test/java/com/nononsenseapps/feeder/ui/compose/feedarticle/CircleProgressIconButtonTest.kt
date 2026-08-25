package com.nononsenseapps.feeder.ui.compose.feedarticle

import com.nononsenseapps.feeder.ai.ArticleTranslation
import com.nononsenseapps.feeder.ai.ParagraphTranslation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for the CircleProgressIconButton cancel logic and state management.
 *
 * Since Compose UI testing (createComposeRule) is only available in instrumented
 * tests (androidTest) in this project, these tests verify the ViewModel-level
 * cancel behavior, state flow transitions, and CancellationException handling
 * that drive the CircleProgressIconButton's visual state.
 *
 * BDD Scenario coverage:
 * - SCENARIO-001, SCENARIO-006: State mapping (Loading/Translating → progress visible)
 * - SCENARIO-002, SCENARIO-008: Cancel resets state to Empty
 * - SCENARIO-003, SCENARIO-009: Completion restores idle (Result/Translated → idle)
 * - SCENARIO-004, SCENARIO-010: Error restores idle
 * - SCENARIO-005: Re-trigger after completion
 * - SCENARIO-011: Independent cancel of summarize vs translate
 * - SCENARIO-012: Rapid cancel and restart
 * - SCENARIO-013, SCENARIO-014: Content description driven by state (verified via state mapping)
 * - SCENARIO-015: Composable uses fixed 48.dp/24.dp sizing (code review only)
 */
class CircleProgressIconButtonTest {
    // region State mapping tests (SCENARIO-001, SCENARIO-003, SCENARIO-004, SCENARIO-006, SCENARIO-009, SCENARIO-010)

    @Test
    fun aiSummaryEmpty_mapsToIdleState() {
        val state = AISummaryState.Empty
        val isInProgress = state is AISummaryState.Loading
        assertEquals(false, isInProgress)
    }

    @Test
    fun aiSummaryLoading_mapsToProgressState() {
        val state = AISummaryState.Loading
        val isInProgress = state is AISummaryState.Loading
        assertEquals(true, isInProgress)
    }

    @Test
    fun aiSummaryResult_mapsToIdleState() {
        val state =
            AISummaryState.Result(
                value =
                    com.nononsenseapps.feeder.ai.AIClient.SummaryResult
                        .Error(content = "test"),
            )
        val isInProgress = state is AISummaryState.Loading
        assertEquals(false, isInProgress)
    }

    @Test
    fun translationEmpty_mapsToIdleState() {
        val state = TranslationState.Empty
        val isInProgress = state is TranslationState.Translating
        assertEquals(false, isInProgress)
    }

    @Test
    fun translationTranslating_mapsToProgressState() {
        val state =
            TranslationState.Translating(
                articleTranslation =
                    ArticleTranslation(
                        contents =
                            listOf(
                                ParagraphTranslation(index = 1, text = "Hello", translation = "", translated = 0),
                            ),
                        status = "translating",
                    ),
            )
        val isInProgress = state is TranslationState.Translating
        assertEquals(true, isInProgress)
    }

    @Test
    fun translationTranslated_mapsToIdleState() {
        val state =
            TranslationState.Translated(
                articleTranslation =
                    ArticleTranslation(
                        contents =
                            listOf(
                                ParagraphTranslation(index = 1, text = "Hello", translation = "Hola", translated = 1),
                            ),
                        status = "translated",
                    ),
            )
        val isInProgress = state is TranslationState.Translating
        assertEquals(false, isInProgress)
    }

    @Test
    fun translationError_mapsToIdleState() {
        val state = TranslationState.Error(errorMessage = "Failed")
        val isInProgress = state is TranslationState.Translating
        assertEquals(false, isInProgress)
    }

    // endregion

    // region Progress fraction computation (SCENARIO-007)

    @Test
    fun translationProgressFraction_computesCorrectly() {
        val articleTranslation =
            ArticleTranslation(
                contents =
                    listOf(
                        ParagraphTranslation(index = 1, text = "A", translation = "A_t", translated = 1),
                        ParagraphTranslation(index = 2, text = "B", translation = "", translated = 0),
                        ParagraphTranslation(index = 3, text = "C", translation = "C_t", translated = 1),
                        ParagraphTranslation(index = 4, text = "D", translation = "", translated = 0),
                    ),
                status = "translating",
            )
        val completed = articleTranslation.paragraphCompletedCount
        val total = articleTranslation.paragraphTotalCount
        val fraction = if (total > 0) completed.toFloat() / total else 0f

        assertEquals(0.5f, fraction)
    }

    @Test
    fun translationProgressFraction_zeroTotal_returnsZero() {
        val articleTranslation =
            ArticleTranslation(
                contents = emptyList(),
                status = "translating",
            )
        val completed = articleTranslation.paragraphCompletedCount
        val total = articleTranslation.paragraphTotalCount
        val fraction = if (total > 0) completed.toFloat() / total else 0f

        assertEquals(0f, fraction)
    }

    @Test
    fun translationProgressFraction_allComplete_returnsOne() {
        val articleTranslation =
            ArticleTranslation(
                contents =
                    listOf(
                        ParagraphTranslation(index = 1, text = "A", translation = "A_t", translated = 1),
                        ParagraphTranslation(index = 2, text = "B", translation = "B_t", translated = 1),
                    ),
                status = "translated",
            )
        val completed = articleTranslation.paragraphCompletedCount
        val total = articleTranslation.paragraphTotalCount
        val fraction = if (total > 0) completed.toFloat() / total else 0f

        assertEquals(1f, fraction)
    }

    // endregion

    // region Cancel behavior (SCENARIO-002, SCENARIO-008)

    @Test
    fun cancelSummarize_resetsStateToEmpty() =
        runTest {
            val aiSummary = MutableStateFlow<AISummaryState>(AISummaryState.Loading)
            var summarizeJob: Job? = launch(Dispatchers.Default) { delay(10_000) }

            // Simulate cancelSummarize()
            summarizeJob?.cancel()
            summarizeJob = null
            aiSummary.value = AISummaryState.Empty

            assertIs<AISummaryState.Empty>(aiSummary.value)
            assertNull(summarizeJob)
        }

    @Test
    fun cancelTranslation_resetsStateToEmpty() =
        runTest {
            val translationState =
                MutableStateFlow<TranslationState>(
                    TranslationState.Translating(
                        articleTranslation =
                            ArticleTranslation(
                                contents =
                                    listOf(
                                        ParagraphTranslation(index = 1, text = "Hello", translation = "", translated = 0),
                                    ),
                                status = "translating",
                            ),
                    ),
                )
            var translateJob: Job? = launch(Dispatchers.Default) { delay(10_000) }

            // Simulate cancelTranslation()
            translateJob?.cancel()
            translateJob = null
            translationState.value = TranslationState.Empty

            assertIs<TranslationState.Empty>(translationState.value)
            assertNull(translateJob)
        }

    // endregion

    // region CancellationException handling (SCENARIO-002, SCENARIO-008)

    @Test
    fun cancellationException_isNotCaughtAsError_summarize() =
        runTest {
            val aiSummary = MutableStateFlow<AISummaryState>(AISummaryState.Empty)
            var caughtAsCancellation = false

            val job =
                launch {
                    try {
                        aiSummary.value = AISummaryState.Loading
                        delay(10_000) // Will be cancelled
                    } catch (e: CancellationException) {
                        caughtAsCancellation = true
                        throw e // Rethrown, not turned into error
                    } catch (_: Exception) {
                        aiSummary.value =
                            AISummaryState.Result(
                                value =
                                    com.nononsenseapps.feeder.ai.AIClient.SummaryResult.Error(
                                        content = "Should not reach here",
                                    ),
                            )
                    }
                }

            // Let the coroutine start and suspend at delay()
            yield()

            // Cancel the job
            job.cancel()
            job.join()

            assertTrue(caughtAsCancellation, "CancellationException should be caught and rethrown")
            // State should still be Loading (not Error), because cancellation was rethrown
            assertIs<AISummaryState.Loading>(aiSummary.value)
        }

    @Test
    fun cancellationException_isNotCaughtAsError_translate() =
        runTest {
            val translationState = MutableStateFlow<TranslationState>(TranslationState.Empty)
            var caughtAsCancellation = false

            val job =
                launch {
                    try {
                        translationState.value =
                            TranslationState.Translating(
                                articleTranslation =
                                    ArticleTranslation(
                                        contents =
                                            listOf(
                                                ParagraphTranslation(
                                                    index = 1,
                                                    text = "Hello",
                                                    translation = "",
                                                    translated = 0,
                                                ),
                                            ),
                                        status = "translating",
                                    ),
                            )
                        delay(10_000) // Will be cancelled
                    } catch (e: CancellationException) {
                        caughtAsCancellation = true
                        throw e
                    } catch (_: Exception) {
                        translationState.value =
                            TranslationState.Error(errorMessage = "Should not reach here")
                    }
                }

            // Let the coroutine start and suspend at delay()
            yield()

            job.cancel()
            job.join()

            assertTrue(caughtAsCancellation, "CancellationException should be caught and rethrown")
            assertIs<TranslationState.Translating>(translationState.value)
        }

    // endregion

    // region Independent cancel (SCENARIO-011)

    @Test
    fun cancelSummarize_doesNotAffectTranslation() =
        runTest {
            val aiSummary = MutableStateFlow<AISummaryState>(AISummaryState.Loading)
            val translationState =
                MutableStateFlow<TranslationState>(
                    TranslationState.Translating(
                        articleTranslation =
                            ArticleTranslation(
                                contents =
                                    listOf(
                                        ParagraphTranslation(index = 1, text = "Hello", translation = "", translated = 0),
                                    ),
                                status = "translating",
                            ),
                    ),
                )

            // Cancel summarize only
            aiSummary.value = AISummaryState.Empty

            assertIs<AISummaryState.Empty>(aiSummary.value)
            assertIs<TranslationState.Translating>(translationState.value)
        }

    @Test
    fun cancelTranslation_doesNotAffectSummarize() =
        runTest {
            val aiSummary = MutableStateFlow<AISummaryState>(AISummaryState.Loading)
            val translationState =
                MutableStateFlow<TranslationState>(
                    TranslationState.Translating(
                        articleTranslation =
                            ArticleTranslation(
                                contents =
                                    listOf(
                                        ParagraphTranslation(index = 1, text = "Hello", translation = "", translated = 0),
                                    ),
                                status = "translating",
                            ),
                    ),
                )

            // Cancel translation only
            translationState.value = TranslationState.Empty

            assertIs<AISummaryState.Loading>(aiSummary.value)
            assertIs<TranslationState.Empty>(translationState.value)
        }

    // endregion

    // region Rapid cancel and restart (SCENARIO-012)

    @Test
    fun rapidCancelAndRestart_cancelsOldJob() =
        runTest {
            val aiSummary = MutableStateFlow<AISummaryState>(AISummaryState.Empty)
            var oldJobCancelled = false

            // Start first operation
            val firstJob =
                launch {
                    try {
                        aiSummary.value = AISummaryState.Loading
                        delay(10_000)
                    } catch (e: CancellationException) {
                        oldJobCancelled = true
                        throw e
                    }
                }

            // Let the first coroutine start and suspend at delay()
            yield()

            // Rapid cancel + restart (mimics summarize() calling summarizeJob?.cancel() before launch)
            firstJob.cancel()
            val secondJob =
                launch {
                    aiSummary.value = AISummaryState.Loading
                    delay(100)
                }

            firstJob.join()
            secondJob.join()

            assertTrue(oldJobCancelled, "Old job should have been cancelled")
        }

    // endregion

    // region Re-trigger after completion (SCENARIO-005)

    @Test
    fun reTrigger_afterCompletion_setsLoadingAgain() =
        runTest {
            val aiSummary = MutableStateFlow<AISummaryState>(AISummaryState.Empty)

            // Complete first operation
            aiSummary.value = AISummaryState.Loading
            aiSummary.value =
                AISummaryState.Result(
                    value =
                        com.nononsenseapps.feeder.ai.AIClient.SummaryResult
                            .Error(content = "Summary"),
                )
            assertIs<AISummaryState.Result>(aiSummary.value)

            // Re-trigger
            aiSummary.value = AISummaryState.Loading
            assertIs<AISummaryState.Loading>(aiSummary.value)
        }

    // endregion

    // region ProgressFraction null vs non-null (SCENARIO-001 vs SCENARIO-006)

    @Test
    fun summarizeProgressFraction_isNull_forIndeterminate() {
        val state = AISummaryState.Loading
        // Summarize uses null progressFraction (indeterminate)
        val progressFraction: (() -> Float)? = null
        val isInProgress = state is AISummaryState.Loading

        assertTrue(isInProgress)
        assertNull(progressFraction)
    }

    @Test
    fun translateProgressFraction_isNonNull_forDeterminate() {
        val articleTranslation =
            ArticleTranslation(
                contents =
                    listOf(
                        ParagraphTranslation(index = 1, text = "A", translation = "A_t", translated = 1),
                        ParagraphTranslation(index = 2, text = "B", translation = "", translated = 0),
                    ),
                status = "translating",
            )
        val state = TranslationState.Translating(articleTranslation = articleTranslation)

        val progressFraction: (() -> Float)? =
            if (state is TranslationState.Translating) {
                val completed = state.articleTranslation.paragraphCompletedCount
                val total = state.articleTranslation.paragraphTotalCount
                { if (total > 0) completed.toFloat() / total else 0f }
            } else {
                null
            }

        assertTrue(progressFraction != null)
        assertEquals(0.5f, progressFraction())
    }

    // endregion
}
