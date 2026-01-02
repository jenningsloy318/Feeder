package com.nononsenseapps.feeder.ai.translation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TranslationStateTest {

    @Test
    fun `ParagraphTranslation contains all required fields`() {
        val translation = ParagraphTranslation(
            index = 0,
            original = "Hello world",
            translated = "Hola mundo",
        )

        assertEquals(0, translation.index)
        assertEquals("Hello world", translation.original)
        assertEquals("Hola mundo", translation.translated)
    }

    @Test
    fun `TranslationState Idle is the initial state`() {
        val state = TranslationState.Idle
        assertTrue(state is TranslationState.Idle)
    }

    @Test
    fun `TranslationState Loading contains progress info`() {
        val state = TranslationState.Loading(progress = 5, total = 10)
        assertTrue(state is TranslationState.Loading)
        assertEquals(5, state.progress)
        assertEquals(10, state.total)
    }

    @Test
    fun `TranslationState Success contains translations`() {
        val translations = listOf(
            ParagraphTranslation(0, "Hello", "Hola"),
            ParagraphTranslation(1, "World", "Mundo"),
        )
        val state = TranslationState.Success(translations)

        assertTrue(state is TranslationState.Success)
        assertEquals(2, state.translations.size)
        assertEquals("Hola", state.translations[0].translated)
    }

    @Test
    fun `TranslationState Error contains message and retry flag`() {
        val state = TranslationState.Error(
            message = "Network error",
            retryable = true,
        )

        assertTrue(state is TranslationState.Error)
        assertEquals("Network error", state.message)
        assertTrue(state.retryable)
    }

    @Test
    fun `TranslationState Progress contains intermediate results`() {
        val translations = listOf(
            ParagraphTranslation(0, "Hello", "Hola"),
        )
        val state = TranslationState.Progress(
            translations = translations,
            total = 10,
        )

        assertTrue(state is TranslationState.Progress)
        assertEquals(1, state.translations.size)
        assertEquals(10, state.total)
    }

    @Test
    fun `paragraph splitting preserves non-empty paragraphs`() {
        val content = """
            First paragraph.

            Second paragraph.

            Third paragraph.
        """.trimIndent()

        val paragraphs = content.split("\n\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        assertEquals(3, paragraphs.size)
        assertEquals("First paragraph.", paragraphs[0])
        assertEquals("Second paragraph.", paragraphs[1])
        assertEquals("Third paragraph.", paragraphs[2])
    }
}
