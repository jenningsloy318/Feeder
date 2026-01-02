package com.nononsenseapps.feeder.ai.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TargetLanguageTest {

    @Test
    fun `fromCode returns correct language for valid codes`() {
        assertEquals(TargetLanguage.ENGLISH, TargetLanguage.fromCode("en"))
        assertEquals(TargetLanguage.CHINESE, TargetLanguage.fromCode("zh"))
        assertEquals(TargetLanguage.SPANISH, TargetLanguage.fromCode("es"))
        assertEquals(TargetLanguage.FRENCH, TargetLanguage.fromCode("fr"))
        assertEquals(TargetLanguage.GERMAN, TargetLanguage.fromCode("de"))
        assertEquals(TargetLanguage.JAPANESE, TargetLanguage.fromCode("ja"))
        assertEquals(TargetLanguage.KOREAN, TargetLanguage.fromCode("ko"))
        assertEquals(TargetLanguage.PORTUGUESE, TargetLanguage.fromCode("pt"))
        assertEquals(TargetLanguage.RUSSIAN, TargetLanguage.fromCode("ru"))
        assertEquals(TargetLanguage.ITALIAN, TargetLanguage.fromCode("it"))
        assertEquals(TargetLanguage.ARABIC, TargetLanguage.fromCode("ar"))
        assertEquals(TargetLanguage.HINDI, TargetLanguage.fromCode("hi"))
    }

    @Test
    fun `fromCode returns null for invalid code`() {
        assertNull(TargetLanguage.fromCode("xx"))
        assertNull(TargetLanguage.fromCode(""))
        assertNull(TargetLanguage.fromCode(null))
    }

    @Test
    fun `language codes are unique`() {
        val codes = TargetLanguage.entries.map { it.code }
        assertEquals(codes.size, codes.distinct().size)
    }

    @Test
    fun `language names are human-readable`() {
        TargetLanguage.entries.forEach { language ->
            assert(language.languageName.isNotEmpty()) {
                "Language name for ${language.code} should not be empty"
            }
        }
    }
}
