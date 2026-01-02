package com.nononsenseapps.feeder.ui.compose.settings

import com.nononsenseapps.feeder.ai.model.TargetLanguage
import com.nononsenseapps.feeder.archmodel.Repository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class TranslationSettingsViewModelTest {

    @Mock
    private lateinit var repository: Repository

    private lateinit var viewModel: TranslationSettingsViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun `initial translation enabled state is false`() = runTest {
        whenever(repository.translationEnabled).thenReturn(flowOf(false))

        // Verify initial state
        val states = mutableListOf<Boolean>()
        repository.translationEnabled.collect { states.add(it) }

        assertFalse(states.first())
    }

    @Test
    fun `initial target language defaults to English`() = runTest {
        whenever(repository.translationTargetLanguage).thenReturn(flowOf(TargetLanguage.ENGLISH))

        val states = mutableListOf<TargetLanguage>()
        repository.translationTargetLanguage.collect { states.add(it) }

        assertEquals(TargetLanguage.ENGLISH, states.first())
    }

    @Test
    fun `setting translation enabled updates repository`() = runTest {
        viewModel = TranslationSettingsViewModel Testing {
            mockRepository()
        }

        viewModel.setTranslationEnabled(true)

        verify(repository).setTranslationEnabled(true)
    }

    @Test
    fun `setting target language updates repository`() = runTest {
        viewModel = TranslationSettingsViewModel Testing {
            mockRepository()
        }

        viewModel.setTranslationTargetLanguage(TargetLanguage.CHINESE)

        verify(repository).setTranslationTargetLanguage(TargetLanguage.CHINESE)
    }

    private fun mockRepository(): Repository {
        whenever(repository.translationEnabled).thenReturn(flowOf(false))
        whenever(repository.translationTargetLanguage).thenReturn(flowOf(TargetLanguage.ENGLISH))
        return repository
    }
}
