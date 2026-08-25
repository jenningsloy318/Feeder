package com.nononsenseapps.feeder.ai.provider

import com.nononsenseapps.feeder.ai.AIClient
import com.nononsenseapps.feeder.ai.TranslatableText
import com.nononsenseapps.feeder.ai.model.AISettings
import com.nononsenseapps.feeder.ai.model.DeepLSettings
import com.nononsenseapps.feeder.ai.model.TranslationLanguage
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Locale

class DeepLClientTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun client(
        key: String = "test-key",
        baseUrl: String = server.url("/").toString(),
    ): DeepLClient = DeepLClient(DeepLSettings(key = key, baseUrl = baseUrl))

    private fun okBody(vararg texts: String): String {
        val translations =
            texts.joinToString(",") { text ->
                """{"detected_source_language":"EN","text":"$text"}"""
            }
        return """{"translations":[$translations]}"""
    }

    @Test
    fun `translate sends batch request with auth header and target language`() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setBody(okBody("Hallo", "Welt"))
                    .setHeader("Content-Type", "application/json"),
            )

            val result =
                client().translate(
                    translatableTexts =
                        listOf(
                            TranslatableText.fromPlainText("Hello"),
                            TranslatableText.fromPlainText("World"),
                        ),
                    targetLanguage = TranslationLanguage.GERMAN,
                )

            val recorded = server.takeRequest()
            assertEquals("/v2/translate", recorded.path)
            assertEquals("DeepL-Auth-Key test-key", recorded.getHeader("Authorization"))
            val body = recorded.body.readUtf8()
            assertTrue(body.contains("\"target_lang\":\"DE\""))
            assertTrue(body.contains("\"preserve_formatting\":true"))

            assertTrue(result is AIClient.TranslationResult.Success)
            assertEquals(listOf("Hallo", "Welt"), (result as AIClient.TranslationResult.Success).paragraphs)
        }

    @Test
    fun `translate resolves device default language from locale`() =
        runTest {
            val originalLocale = Locale.getDefault()
            Locale.setDefault(Locale("pt", "BR"))
            try {
                server.enqueue(MockResponse().setBody(okBody("Olá")))

                client().translate(
                    translatableTexts = listOf(TranslatableText.fromPlainText("Hello")),
                    targetLanguage = TranslationLanguage.DEVICE_DEFAULT,
                )

                val body = server.takeRequest().body.readUtf8()
                assertTrue("Expected PT-BR target, was: $body", body.contains("\"target_lang\":\"PT-BR\""))
            } finally {
                Locale.setDefault(originalLocale)
            }
        }

    @Test
    fun `translate returns error for unsupported target language`() =
        runTest {
            val result =
                client().translate(
                    translatableTexts = listOf(TranslatableText.fromPlainText("Hello")),
                    targetLanguage = TranslationLanguage.HINDI,
                )

            assertTrue(result is AIClient.TranslationResult.Error)
            assertTrue((result as AIClient.TranslationResult.Error).content.contains("Hindi"))
            assertEquals(0, server.requestCount)
        }

    @Test
    fun `translate returns error with server message on http failure`() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setResponseCode(429)
                    .setBody("""{"message":"Too many requests"}"""),
            )

            val result =
                client().translate(
                    translatableTexts = listOf(TranslatableText.fromPlainText("Hello")),
                    targetLanguage = TranslationLanguage.GERMAN,
                )

            assertTrue(result is AIClient.TranslationResult.Error)
            val message = (result as AIClient.TranslationResult.Error).content
            assertTrue(message.contains("429"))
            assertTrue(message.contains("Too many requests"))
        }

    @Test
    fun `translate returns error when count mismatch`() =
        runTest {
            server.enqueue(MockResponse().setBody(okBody("Hallo")))

            val result =
                client().translate(
                    translatableTexts =
                        listOf(
                            TranslatableText.fromPlainText("Hello"),
                            TranslatableText.fromPlainText("World"),
                        ),
                    targetLanguage = TranslationLanguage.GERMAN,
                )

            assertTrue(result is AIClient.TranslationResult.Error)
        }

    @Test
    fun `translate returns error without key`() =
        runTest {
            val result =
                client(key = "").translate(
                    translatableTexts = listOf(TranslatableText.fromPlainText("Hello")),
                    targetLanguage = TranslationLanguage.GERMAN,
                )

            assertTrue(result is AIClient.TranslationResult.Error)
            assertEquals(0, server.requestCount)
        }

    @Test
    fun `generateSummary returns error for translation-only provider`() =
        runTest {
            val result = client().generateSummary("Some content")

            assertTrue(result is AIClient.SummaryResult.Error)
        }

    @Test
    fun `listModels verifies credentials and returns empty list`() =
        runTest {
            server.enqueue(MockResponse().setBody(okBody("Hallo")))

            val models = client().listModels()

            assertEquals(emptyList<String>(), models)
            assertEquals("/v2/translate", server.takeRequest().path)
        }

    @Test(expected = Exception::class)
    fun `listModels throws on auth failure`() =
        runTest {
            server.enqueue(
                MockResponse()
                    .setResponseCode(401)
                    .setBody("""{"message":"Wrong endpoint"}"""),
            )

            client().listModels()
        }

    @Test
    fun `factory dispatches deepL settings to DeepLClient`() {
        val client = AIClient.create(AISettings.DeepL(DeepLSettings(key = "k")))
        assertTrue(client is DeepLClient)
    }

    @Test
    fun `free tier key selects api-free host`() {
        val settings = DeepLSettings(key = "abc:fx")
        assertEquals("https://api-free.deepl.com/v2/translate", settings.toDeepLTranslateUrl())
    }

    @Test
    fun `pro key selects api host by default`() {
        val settings = DeepLSettings(key = "pro-key")
        assertEquals("https://api.deepl.com/v2/translate", settings.toDeepLTranslateUrl())
    }

    @Test
    fun `custom base url is used as-is`() {
        val settings = DeepLSettings(key = "k", baseUrl = "https://deepl-proxy.example.com/")
        assertEquals("https://deepl-proxy.example.com/v2/translate", settings.toDeepLTranslateUrl())
    }

    @Test
    fun `all selectable languages map to supported deepl codes`() {
        val expected =
            mapOf(
                TranslationLanguage.ENGLISH to "EN",
                TranslationLanguage.CHINESE to "ZH",
                TranslationLanguage.SPANISH to "ES",
                TranslationLanguage.FRENCH to "FR",
                TranslationLanguage.GERMAN to "DE",
                TranslationLanguage.JAPANESE to "JA",
                TranslationLanguage.KOREAN to "KO",
                TranslationLanguage.PORTUGUESE to "PT",
                TranslationLanguage.RUSSIAN to "RU",
                TranslationLanguage.ARABIC to "AR",
                TranslationLanguage.HINDI to null,
            )
        expected.forEach { (language, code) ->
            assertEquals("$language -> $code", code, language.toDeepLTargetLanguageCode())
        }
    }

    @Test
    fun `device locale variants map to regional deepl codes`() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale("en", "GB"))
            assertEquals("EN-GB", TranslationLanguage.DEVICE_DEFAULT.toDeepLTargetLanguageCode())
            Locale.setDefault(Locale("pt", "PT"))
            assertEquals("PT-PT", TranslationLanguage.DEVICE_DEFAULT.toDeepLTargetLanguageCode())
            Locale.setDefault(Locale.CHINA)
            assertEquals("ZH", TranslationLanguage.DEVICE_DEFAULT.toDeepLTargetLanguageCode())
        } finally {
            Locale.setDefault(original)
        }
    }
}
