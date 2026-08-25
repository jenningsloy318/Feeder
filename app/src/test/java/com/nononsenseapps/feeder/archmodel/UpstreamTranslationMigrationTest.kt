package com.nononsenseapps.feeder.archmodel

import com.nononsenseapps.feeder.ai.model.AISettings
import com.nononsenseapps.feeder.ai.model.DeepLSettings
import com.nononsenseapps.feeder.ai.model.ProviderConfig
import com.nononsenseapps.feeder.ai.model.TranslationLanguage
import com.nononsenseapps.feeder.ai.provider.AIProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for the one-time migration of upstream (2.21+) translation
 * settings onto the multi-provider architecture.
 */
class UpstreamTranslationMigrationTest {
    private fun provider(
        type: AIProvider,
        name: String,
        isActive: Boolean = false,
    ): ProviderConfig =
        ProviderConfig(
            id = "id-$name",
            name = name,
            providerType = type,
            openAISettings =
                if (type == AIProvider.OPENAI_COMPATIBLE) {
                    com.nononsenseapps.feeder.ai.model
                        .OpenAISettings(key = "k")
                } else {
                    null
                },
            anthropicSettings =
                if (type == AIProvider.ANTHROPIC) {
                    com.nononsenseapps.feeder.ai.model
                        .AnthropicSettings(key = "k")
                } else {
                    null
                },
            deepLSettings =
                if (type == AIProvider.DEEPL) {
                    DeepLSettings(key = "k")
                } else {
                    null
                },
            onDeviceSettings =
                if (type == AIProvider.ON_DEVICE) {
                    com.nononsenseapps.feeder.ai.model
                        .OnDeviceSettings()
                } else {
                    null
                },
            isActive = isActive,
            createdAt = 0L,
            updatedAt = 0L,
        )

    @Test
    fun `deepL credentials with deepl url create provider`() {
        val result =
            buildMigratedDeepLProvider(
                legacyKey = "my-key",
                legacyUrl = "https://api-free.deepl.com",
                timeoutSeconds = 45,
                providers = emptyList(),
            )

        assertNotNull(result)
        result!!
        assertEquals(AIProvider.DEEPL, result.providerType)
        assertEquals("my-key", result.deepLSettings?.key)
        assertEquals("https://api-free.deepl.com", result.deepLSettings?.baseUrl)
        assertEquals(45, result.deepLSettings?.timeoutSeconds)
        // No active provider existed: the migrated one becomes active
        assertTrue(result.isActive)
    }

    @Test
    fun `free tier key suffix is detected without url`() {
        val result =
            buildMigratedDeepLProvider(
                legacyKey = "abc:fx",
                legacyUrl = "",
                timeoutSeconds = 90,
                providers = emptyList(),
            )

        assertNotNull(result)
        assertEquals("abc:fx", result!!.deepLSettings?.key)
    }

    @Test
    fun `non deeL credentials do not create provider`() {
        val result =
            buildMigratedDeepLProvider(
                legacyKey = "my-key",
                legacyUrl = "https://api.openai.com/v1",
                timeoutSeconds = 90,
                providers = emptyList(),
            )

        assertNull(result)
    }

    @Test
    fun `missing key does not create provider`() {
        assertNull(
            buildMigratedDeepLProvider(
                legacyKey = "",
                legacyUrl = "https://api.deepl.com",
                timeoutSeconds = 90,
                providers = emptyList(),
            ),
        )
    }

    @Test
    fun `existing deeL provider prevents duplicate migration`() {
        val existing = listOf(provider(AIProvider.DEEPL, "My DeepL"))

        assertNull(
            buildMigratedDeepLProvider(
                legacyKey = "my-key",
                legacyUrl = "https://api.deepl.com",
                timeoutSeconds = 90,
                providers = existing,
            ),
        )
    }

    @Test
    fun `migrated provider is inactive when another provider is active`() {
        val existing = listOf(provider(AIProvider.OPENAI_COMPATIBLE, "OpenAI", isActive = true))

        val result =
            buildMigratedDeepLProvider(
                legacyKey = "my-key",
                legacyUrl = "https://api.deepl.com",
                timeoutSeconds = 90,
                providers = existing,
            )

        assertNotNull(result)
        assertFalse(result!!.isActive)
    }

    @Test
    fun `timeout is coerced into the valid range`() {
        val result =
            buildMigratedDeepLProvider(
                legacyKey = "my-key",
                legacyUrl = "https://api.deepl.com",
                timeoutSeconds = 5000,
                providers = emptyList(),
            )

        assertEquals(600, result!!.deepLSettings?.timeoutSeconds)
    }

    @Test
    fun `upstream language names map onto the enum`() {
        assertEquals(TranslationLanguage.FRENCH, "French".toUpstreamPreferredLanguage())
        assertEquals(TranslationLanguage.GERMAN, "german".toUpstreamPreferredLanguage())
        assertEquals(TranslationLanguage.PORTUGUESE, "Portuguese".toUpstreamPreferredLanguage())
    }

    @Test
    fun `upstream language codes map onto the enum`() {
        assertEquals(TranslationLanguage.JAPANESE, "ja".toUpstreamPreferredLanguage())
        assertEquals(TranslationLanguage.ENGLISH, "en".toUpstreamPreferredLanguage())
    }

    @Test
    fun `unknown upstream language falls back to device default`() {
        assertEquals(TranslationLanguage.DEVICE_DEFAULT, "Klingon".toUpstreamPreferredLanguage())
        assertEquals(TranslationLanguage.DEVICE_DEFAULT, "".toUpstreamPreferredLanguage())
    }

    @Test
    fun `migrated provider round trips through aisSettings`() {
        val result =
            buildMigratedDeepLProvider(
                legacyKey = "my-key",
                legacyUrl = "https://api.deepl.com",
                timeoutSeconds = 90,
                providers = emptyList(),
            )!!

        val settings = result.toAISettings()
        assertTrue(settings is AISettings.DeepL)
        assertTrue(settings.isValid)
    }
}
