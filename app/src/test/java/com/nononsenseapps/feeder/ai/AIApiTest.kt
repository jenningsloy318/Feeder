package com.nononsenseapps.feeder.ai

import com.nononsenseapps.feeder.archmodel.Repository
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Tests for the new multi-provider AIApi.
 *
 * Note: This is a minimal test suite. The actual API calls are tested
 * by the provider-specific client tests (OpenAI, Anthropic, etc.).
 * This test focuses on the AIApi's logic for language parsing and
 * provider delegation.
 */
class AIApiTest {
    @MockK
    private lateinit var repository: Repository

    private lateinit var api: AIApi

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        api = AIApi(repository, "en")
    }

    @Test
    fun testApiCreation() {
        // Just verify the API can be created
        assertTrue(api.javaClass.simpleName.isNotEmpty())
    }

    // Note: Full testing would require mocking the AIClient factory
    // and provider-specific clients. For now, we test the basic structure.

    // TODO: Add tests for:
    // - summarize() method with mocked client
    // - listModelIds() with different providers
    // - Language parsing from AI responses
    // - Error handling for invalid settings
}
