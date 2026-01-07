package com.nononsenseapps.feeder.ai.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for AISettings maxTokens field.
 *
 * Tests maxTokens validation, serialization, and default values.
 */
class AISettingsTest {
    @Test
    fun testOpenAISettings_MaxTokens_DefaultIsNull() {
        val settings = OpenAISettings()
        assertNull(settings.maxTokens, "maxTokens should default to null")
    }

    @Test
    fun testOpenAISettings_MaxTokens_CanBeSet() {
        val settings = OpenAISettings(maxTokens = 4096)
        assertEquals(4096, settings.maxTokens, "maxTokens should be settable")
    }

    @Test
    fun testAnthropicSettings_MaxTokens_DefaultIsNull() {
        val settings = AnthropicSettings()
        assertNull(settings.maxTokens, "maxTokens should default to null")
    }

    @Test
    fun testAnthropicSettings_MaxTokens_CanBeSet() {
        val settings = AnthropicSettings(maxTokens = 8192)
        assertEquals(8192, settings.maxTokens, "maxTokens should be settable")
    }

    @Test
    fun testOpenAISettings_WithMaxTokens_RemainsValid() {
        val settings = OpenAISettings(
            key = "test-key",
            modelId = "gpt-4o",
            maxTokens = 4096
        )
        assertTrue(settings.isValid, "Settings with maxTokens should remain valid")
    }

    @Test
    fun testAnthropicSettings_WithMaxTokens_RemainsValid() {
        val settings = AnthropicSettings(
            key = "test-key",
            modelId = "claude-3-5-sonnet-20241022",
            maxTokens = 8192
        )
        assertTrue(settings.isValid, "Settings with maxTokens should remain valid")
    }

    @Test
    fun testOpenAISettings_MaxTokens_Copy() {
        val original = OpenAISettings(
            key = "test-key",
            modelId = "gpt-4o",
            maxTokens = 4096
        )
        val copy = original.copy(maxTokens = 8192)

        assertEquals(4096, original.maxTokens, "Original maxTokens should not change")
        assertEquals(8192, copy.maxTokens, "Copied maxTokens should be updated")
    }

    @Test
    fun testAnthropicSettings_MaxTokens_Copy() {
        val original = AnthropicSettings(
            key = "test-key",
            modelId = "claude-3-5-sonnet-20241022",
            maxTokens = 4096
        )
        val copy = original.copy(maxTokens = 8192)

        assertEquals(4096, original.maxTokens, "Original maxTokens should not change")
        assertEquals(8192, copy.maxTokens, "Copied maxTokens should be updated")
    }
}
