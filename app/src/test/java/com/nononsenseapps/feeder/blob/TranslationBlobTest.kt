package com.nononsenseapps.feeder.blob

import com.nononsenseapps.feeder.ai.ArticleTranslation
import com.nononsenseapps.feeder.ai.ParagraphTranslation
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.GZIPOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TranslationBlobTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun createTranslationsDir(): File = tempFolder.newFolder("translations")

    private fun sampleTranslation(paragraphCount: Int = 3): ArticleTranslation =
        ArticleTranslation(
            contents = (1..paragraphCount).map { i ->
                ParagraphTranslation(
                    index = i,
                    text = "Source paragraph $i",
                    translation = "Translated paragraph $i",
                    translated = 1,
                )
            },
            status = "translated",
        )

    @Test
    fun saveAndLoadRoundTrip_returnsIdenticalArticleTranslation() {
        val dir = createTranslationsDir()
        val translation = sampleTranslation()

        saveTranslation(itemId = 42, languageCode = "zh", translationsDir = dir, translation = translation)
        val loaded = loadTranslation(itemId = 42, languageCode = "zh", translationsDir = dir)

        assertNotNull(loaded)
        assertEquals(translation, loaded)
    }

    @Test
    fun loadTranslation_returnsNullWhenFileDoesNotExist() {
        val dir = createTranslationsDir()

        val loaded = loadTranslation(itemId = 99, languageCode = "en", translationsDir = dir)

        assertNull(loaded)
    }

    @Test
    fun loadTranslation_returnsNullForCorruptedGzipFile() {
        val dir = createTranslationsDir()
        val file = translationFile(itemId = 42, languageCode = "zh", translationsDir = dir)
        file.writeBytes(byteArrayOf(0x1F, 0x8B.toByte(), 0x00, 0x01, 0x02, 0x03))

        val loaded = loadTranslation(itemId = 42, languageCode = "zh", translationsDir = dir)

        assertNull(loaded)
    }

    @Test
    fun loadTranslation_returnsNullForInvalidJsonInValidGzip() {
        val dir = createTranslationsDir()
        val file = translationFile(itemId = 42, languageCode = "zh", translationsDir = dir)
        GZIPOutputStream(file.outputStream()).bufferedWriter().use { writer ->
            writer.write("this is not valid json")
        }

        val loaded = loadTranslation(itemId = 42, languageCode = "zh", translationsDir = dir)

        assertNull(loaded)
    }

    @Test
    fun saveTranslation_createsDirectoryIfNotExists() {
        val dir = File(tempFolder.root, "nonexistent/translations")
        val translation = sampleTranslation()

        saveTranslation(itemId = 42, languageCode = "en", translationsDir = dir, translation = translation)

        assertTrue(dir.isDirectory)
        val loaded = loadTranslation(itemId = 42, languageCode = "en", translationsDir = dir)
        assertEquals(translation, loaded)
    }

    @Test
    fun saveTranslation_overwritesExistingFile() {
        val dir = createTranslationsDir()
        val original = sampleTranslation(paragraphCount = 2)
        val updated = sampleTranslation(paragraphCount = 5)

        saveTranslation(itemId = 42, languageCode = "zh", translationsDir = dir, translation = original)
        saveTranslation(itemId = 42, languageCode = "zh", translationsDir = dir, translation = updated)

        val loaded = loadTranslation(itemId = 42, languageCode = "zh", translationsDir = dir)
        assertNotNull(loaded)
        assertEquals(5, loaded.contents.size)
        assertEquals(updated, loaded)
    }

    @Test
    fun multipleLanguagesForSameItemCoexistIndependently() {
        val dir = createTranslationsDir()
        val zhTranslation = ArticleTranslation(
            contents = listOf(
                ParagraphTranslation(index = 1, text = "Hello", translation = "你好", translated = 1),
            ),
            status = "translated",
        )
        val enTranslation = ArticleTranslation(
            contents = listOf(
                ParagraphTranslation(index = 1, text = "Bonjour", translation = "Hello", translated = 1),
            ),
            status = "translated",
        )

        saveTranslation(itemId = 42, languageCode = "zh", translationsDir = dir, translation = zhTranslation)
        saveTranslation(itemId = 42, languageCode = "en", translationsDir = dir, translation = enTranslation)

        assertEquals(zhTranslation, loadTranslation(itemId = 42, languageCode = "zh", translationsDir = dir))
        assertEquals(enTranslation, loadTranslation(itemId = 42, languageCode = "en", translationsDir = dir))
    }

    @Test
    fun deleteTranslationCache_removesAllLanguageFilesForItemId() {
        val dir = createTranslationsDir()
        val translation = sampleTranslation()

        saveTranslation(itemId = 42, languageCode = "zh", translationsDir = dir, translation = translation)
        saveTranslation(itemId = 42, languageCode = "en", translationsDir = dir, translation = translation)
        saveTranslation(itemId = 42, languageCode = "fr", translationsDir = dir, translation = translation)

        deleteTranslationCache(itemId = 42, translationsDir = dir)

        assertNull(loadTranslation(itemId = 42, languageCode = "zh", translationsDir = dir))
        assertNull(loadTranslation(itemId = 42, languageCode = "en", translationsDir = dir))
        assertNull(loadTranslation(itemId = 42, languageCode = "fr", translationsDir = dir))
    }

    @Test
    fun deleteTranslationCache_doesNotRemoveFilesForOtherItemIds() {
        val dir = createTranslationsDir()
        val translation = sampleTranslation()

        saveTranslation(itemId = 42, languageCode = "zh", translationsDir = dir, translation = translation)
        saveTranslation(itemId = 99, languageCode = "zh", translationsDir = dir, translation = translation)

        deleteTranslationCache(itemId = 42, translationsDir = dir)

        assertNull(loadTranslation(itemId = 42, languageCode = "zh", translationsDir = dir))
        assertNotNull(loadTranslation(itemId = 99, languageCode = "zh", translationsDir = dir))
    }

    @Test
    fun deleteTranslationCache_isNoOpWhenDirectoryDoesNotExist() {
        val dir = File(tempFolder.root, "nonexistent_dir")

        // Should not throw
        deleteTranslationCache(itemId = 42, translationsDir = dir)
    }

    @Test
    fun largeArticleRoundTrip_worksCorrectly() {
        val dir = createTranslationsDir()
        val translation = sampleTranslation(paragraphCount = 120)

        saveTranslation(itemId = 200, languageCode = "zh", translationsDir = dir, translation = translation)
        val loaded = loadTranslation(itemId = 200, languageCode = "zh", translationsDir = dir)

        assertNotNull(loaded)
        assertEquals(120, loaded.contents.size)
        assertEquals(translation, loaded)
    }

    @Test
    fun fileWithUnknownJsonKeys_deserializesSuccessfully() {
        val dir = createTranslationsDir()
        // Write JSON with extra unknown keys
        dir.mkdirs()
        val file = translationFile(itemId = 42, languageCode = "zh", translationsDir = dir)
        val jsonWithExtraKeys = """
            {
                "contents": [
                    {"index": 1, "text": "Hello", "translation": "你好", "translated": 1, "futureField": "ignored"}
                ],
                "status": "translated",
                "newTopLevelField": 42
            }
        """.trimIndent()
        GZIPOutputStream(file.outputStream()).bufferedWriter().use { writer ->
            writer.write(jsonWithExtraKeys)
        }

        val loaded = loadTranslation(itemId = 42, languageCode = "zh", translationsDir = dir)

        assertNotNull(loaded)
        assertEquals("translated", loaded.status)
        assertEquals(1, loaded.contents.size)
        assertEquals("你好", loaded.contents[0].translation)
    }
}
