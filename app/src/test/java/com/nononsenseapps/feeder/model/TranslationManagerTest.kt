package com.nononsenseapps.feeder.model

import android.app.Application
import com.nononsenseapps.feeder.ai.model.AISettings
import com.nononsenseapps.feeder.ai.model.DeepLSettings
import com.nononsenseapps.feeder.ai.model.TranslationLanguage
import com.nononsenseapps.feeder.archmodel.Repository
import com.nononsenseapps.feeder.ui.compose.feed.FeedListItem
import com.nononsenseapps.feeder.util.FilePathProvider
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton
import java.io.File
import java.time.Instant
import java.util.Locale

class TranslationManagerTest : DIAware {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private val application: Application = mockk(relaxed = true)
    private val repository: Repository = mockk(relaxed = true)
    private val filePathProvider: FilePathProvider = mockk()
    private lateinit var cacheDir: File

    override val di =
        DI.lazy {
            bind<Application>() with instance(application)
            bind<Repository>() with instance(repository)
            bind<FilePathProvider>() with instance(filePathProvider)
            bind<TranslationManager>() with singleton { TranslationManager(di) }
        }

    private val translationManager: TranslationManager by instance()

    private val settings: AISettings = AISettings.DeepL(DeepLSettings(key = "test-key"))

    private fun item(
        id: Long = 1L,
        title: String = "Hello",
        snippet: String = "Hello world",
    ): FeedListItem =
        FeedListItem(
            id = id,
            title = title,
            snippet = snippet,
            feedTitle = "Feed",
            unread = true,
            pubDate = "",
            image = null,
            link = null,
            bookmarked = false,
            feedImageUrl = null,
            primarySortTime = Instant.EPOCH,
            rawPubDate = null,
            wordCount = 0,
        )

    @Before
    fun setUp() {
        cacheDir = tempFolder.newFolder("cache")
        every { filePathProvider.cacheDir } returns cacheDir
    }

    @Test
    fun `no cache and invalid settings returns untranslated item`() =
        runTest {
            val untranslated =
                item(
                    title = "Hello",
                    snippet = "Hello world",
                )
            val cached =
                translationManager.getCachedTranslatedFeedListItem(
                    item = untranslated,
                    settings = AISettings.DeepL(DeepLSettings(key = "")),
                    language = TranslationLanguage.GERMAN,
                )

            assertFalse(cached.hasCachedTranslation)
            assertFalse(cached.isFullyCached)
        }

    @Test
    fun `cached translation is read back and skips network`() =
        runTest {
            val original =
                item(
                    title = "Hello",
                    snippet = "Hello world",
                )
            val cacheFile =
                cacheDir
                    .resolve("translations")
                    .resolve("1.deepl.de.json")
            cacheFile.parentFile.mkdirs()
            cacheFile.writeText(
                """
                {
                  "sourceLanguage": "en",
                  "titleHash": "${sha256("Hello")}",
                  "translatedTitle": "Hallo",
                  "snippetHash": "${sha256("Hello world")}",
                  "translatedSnippet": "Hallo Welt"
                }
                """.trimIndent(),
            )

            val cached =
                translationManager.getCachedTranslatedFeedListItem(
                    item = original,
                    settings = settings,
                    language = TranslationLanguage.GERMAN,
                )

            assertTrue(cached.hasCachedTranslation)
            assertTrue(cached.isFullyCached)
            assertEquals("Hallo", cached.item.title)
            assertEquals("Hallo Welt", cached.item.snippet)
        }

    @Test
    fun `stale cache hash is not used`() =
        runTest {
            val original =
                item(
                    title = "Changed title",
                    snippet = "Hello world",
                )
            val cacheFile =
                cacheDir
                    .resolve("translations")
                    .resolve("1.deepl.de.json")
            cacheFile.parentFile.mkdirs()
            cacheFile.writeText(
                """
                {
                  "sourceLanguage": "en",
                  "titleHash": "${sha256("Old title")}",
                  "translatedTitle": "Alter Titel",
                  "snippetHash": "${sha256("Hello world")}",
                  "translatedSnippet": "Hallo Welt"
                }
                """.trimIndent(),
            )

            val cached =
                translationManager.getCachedTranslatedFeedListItem(
                    item = original,
                    settings = settings,
                    language = TranslationLanguage.GERMAN,
                )

            assertTrue(cached.hasCachedTranslation)
            assertFalse(cached.isFullyCached)
            assertEquals("Changed title", cached.item.title)
            assertEquals("Hallo Welt", cached.item.snippet)
        }

    @Test
    fun `same-language cache prevents translation when text is unchanged`() =
        runTest {
            // Cache where translated == original and source matches target:
            // a previous detection stored the item as already German
            val original =
                item(
                    title = "Hallo",
                    snippet = "Hallo Welt",
                )
            val cacheFile =
                cacheDir
                    .resolve("translations")
                    .resolve("1.deepl.de.json")
            cacheFile.parentFile.mkdirs()
            cacheFile.writeText(
                """
                {
                  "sourceLanguage": "de",
                  "titleHash": "${sha256("Hallo")}",
                  "translatedTitle": "Hallo",
                  "snippetHash": "${sha256("Hallo Welt")}",
                  "translatedSnippet": "Hallo Welt"
                }
                """.trimIndent(),
            )

            val cached =
                translationManager.getCachedTranslatedFeedListItem(
                    item = original,
                    settings = settings,
                    language = TranslationLanguage.GERMAN,
                )

            // Item already in target language: fully cached, no network needed
            assertTrue(cached.hasCachedTranslation)
            assertTrue(cached.isFullyCached)
        }

    @Test
    fun `detected language matching handles language and region`() {
        assertTrue(detectedLanguageMatchesTranslationTarget("en-US", "en"))
        // Region-agnostic detection does not match a region-specific target (upstream parity)
        assertFalse(detectedLanguageMatchesTranslationTarget("en", "en-US"))
        assertFalse(detectedLanguageMatchesTranslationTarget("en-GB", "en-US"))
        assertTrue(detectedLanguageMatchesTranslationTarget("pt-BR", "pt-BR"))
        assertTrue(detectedLanguageMatchesTranslationTarget("nb", "no"))
        assertFalse(detectedLanguageMatchesTranslationTarget("zh-CN", "en"))
    }

    @Test
    fun `detection samples split long text and require enough letters`() {
        val short = "Hi"
        assertFalse(hasEnoughTextForLanguageDetection(short))

        val long = (0 until 1000).joinToString(" ") { "word$it" }
        val samples = prepareTextSamplesForLanguageDetection(long)
        assertEquals(3, samples.size)
        assertTrue(samples.all { it.length <= 4000 })
    }

    @Test
    fun `device default resolves to device locale tag`() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale("de", "DE"))
            assertEquals("de-DE", TranslationLanguage.DEVICE_DEFAULT.resolvedTargetCode())
            assertEquals("en", TranslationLanguage.ENGLISH.resolvedTargetCode())
        } finally {
            Locale.setDefault(original)
        }
    }

    private fun sha256(value: String): String =
        java.security.MessageDigest
            .getInstance("SHA-256")
            .digest(value.toByteArray())
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
}
