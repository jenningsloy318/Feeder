package com.nononsenseapps.feeder.ai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ArticleTranslationTest {
    @Test
    fun paragraphCompletedCount_should_countTranslatedParagraphs() {
        val articleTranslation =
            ArticleTranslation(
                contents =
                    listOf(
                        ParagraphTranslation(index = 1, text = "Hello", translation = "Hola", translated = 1),
                        ParagraphTranslation(index = 2, text = "World", translation = "", translated = 0),
                        ParagraphTranslation(index = 3, text = "Goodbye", translation = "Adiós", translated = 1),
                        ParagraphTranslation(index = 4, text = "Friend", translation = "", translated = -1),
                    ),
                status = "translating",
            )

        assertEquals(2, articleTranslation.paragraphCompletedCount)
    }

    @Test
    fun paragraphFailedCount_should_countFailedParagraphs() {
        val articleTranslation =
            ArticleTranslation(
                contents =
                    listOf(
                        ParagraphTranslation(index = 1, text = "Hello", translation = "Hola", translated = 1),
                        ParagraphTranslation(index = 2, text = "World", translation = "", translated = -1),
                        ParagraphTranslation(index = 3, text = "Goodbye", translation = "", translated = -1),
                        ParagraphTranslation(index = 4, text = "Friend", translation = "", translated = 0),
                    ),
                status = "translating",
            )

        assertEquals(2, articleTranslation.paragraphFailedCount)
    }

    @Test
    fun paragraphTotalCount_should_returnListSize() {
        val articleTranslation =
            ArticleTranslation(
                contents =
                    listOf(
                        ParagraphTranslation(index = 1, text = "A", translation = "", translated = 0),
                        ParagraphTranslation(index = 2, text = "B", translation = "", translated = 0),
                        ParagraphTranslation(index = 3, text = "C", translation = "", translated = 0),
                    ),
                status = "initial",
            )

        assertEquals(3, articleTranslation.paragraphTotalCount)
    }

    @Test
    fun isAllCompleted_should_returnTrue_when_allResolved() {
        val articleTranslation =
            ArticleTranslation(
                contents =
                    listOf(
                        ParagraphTranslation(index = 1, text = "Hello", translation = "Hola", translated = 1),
                        ParagraphTranslation(index = 2, text = "World", translation = "", translated = -1),
                        ParagraphTranslation(index = 3, text = "Goodbye", translation = "Adiós", translated = 1),
                    ),
                status = "translated",
            )

        assertTrue(articleTranslation.isAllCompleted)
    }

    @Test
    fun isAllCompleted_should_returnFalse_when_someStillPending() {
        val articleTranslation =
            ArticleTranslation(
                contents =
                    listOf(
                        ParagraphTranslation(index = 1, text = "Hello", translation = "Hola", translated = 1),
                        ParagraphTranslation(index = 2, text = "World", translation = "", translated = 0),
                        ParagraphTranslation(index = 3, text = "Goodbye", translation = "", translated = -1),
                    ),
                status = "translating",
            )

        assertFalse(articleTranslation.isAllCompleted)
    }

    @Test
    fun buildTranslatedParagraphsList_should_returnNullForPending() {
        val articleTranslation =
            ArticleTranslation(
                contents =
                    listOf(
                        ParagraphTranslation(index = 1, text = "Hello", translation = "", translated = 0),
                        ParagraphTranslation(index = 2, text = "World", translation = "Mundo", translated = 1),
                    ),
                status = "translating",
            )

        val result = articleTranslation.buildTranslatedParagraphsList()
        assertNull(result[0])
        assertEquals("Mundo", result[1])
    }

    @Test
    fun buildTranslatedParagraphsList_should_returnTextForCompleted() {
        val articleTranslation =
            ArticleTranslation(
                contents =
                    listOf(
                        ParagraphTranslation(index = 1, text = "Hello", translation = "Hola", translated = 1),
                        ParagraphTranslation(index = 2, text = "World", translation = "Mundo", translated = 1),
                    ),
                status = "translated",
            )

        val result = articleTranslation.buildTranslatedParagraphsList()
        assertEquals("Hola", result[0])
        assertEquals("Mundo", result[1])
    }

    @Test
    fun buildTranslatedParagraphsList_should_returnNullForFailed() {
        val articleTranslation =
            ArticleTranslation(
                contents =
                    listOf(
                        ParagraphTranslation(index = 1, text = "Hello", translation = "", translated = -1),
                        ParagraphTranslation(index = 2, text = "World", translation = "Mundo", translated = 1),
                    ),
                status = "translated",
            )

        val result = articleTranslation.buildTranslatedParagraphsList()
        assertNull(result[0])
        assertEquals("Mundo", result[1])
    }

    @Test
    fun status_initial_should_beCorrectString() {
        val articleTranslation =
            ArticleTranslation(
                contents =
                    listOf(
                        ParagraphTranslation(index = 1, text = "Hello", translation = "", translated = 0),
                    ),
                status = "initial",
            )

        assertEquals("initial", articleTranslation.status)
    }

    @Test
    fun status_translating_should_beCorrectString() {
        val articleTranslation =
            ArticleTranslation(
                contents =
                    listOf(
                        ParagraphTranslation(index = 1, text = "Hello", translation = "", translated = 0),
                    ),
                status = "translating",
            )

        assertEquals("translating", articleTranslation.status)
    }

    @Test
    fun status_translated_should_beCorrectString() {
        val articleTranslation =
            ArticleTranslation(
                contents =
                    listOf(
                        ParagraphTranslation(index = 1, text = "Hello", translation = "Hola", translated = 1),
                    ),
                status = "translated",
            )

        assertEquals("translated", articleTranslation.status)
    }
}
