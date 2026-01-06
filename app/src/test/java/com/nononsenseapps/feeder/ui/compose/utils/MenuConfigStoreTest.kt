package com.nononsenseapps.feeder.ui.compose.utils

import android.content.SharedPreferences
import com.nononsenseapps.feeder.ui.compose.settings.MenuConfig
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for MenuConfigStore.
 */
class MenuConfigStoreTest {
    private lateinit var mockSharedPreferences: SharedPreferences
    private lateinit var menuConfigStore: MenuConfigStoreImpl

    @Before
    fun setup() {
        mockSharedPreferences = mockk()
        menuConfigStore = MenuConfigStoreImpl(mockSharedPreferences)
    }

    @Test
    fun `getConfig returns MenuConfig from SharedPreferences`() {
        // Given
        val validJson = """{"order":["copy","translate"],"visibility":{"copy":true,"translate":true}}"""
        every { mockSharedPreferences.getString("menu_config", null) } returns validJson

        // When
        val store = MenuConfigStoreImpl(mockSharedPreferences)
        val config = store.getConfig()

        // Then
        assertNotNull(config)
        assertEquals(listOf("copy", "translate"), config.order)
        assertTrue(config.visibility["copy"] == true)
        assertTrue(config.visibility["translate"] == true)
    }

    @Test
    fun `getConfig returns default config when JSON is invalid`() {
        // Given
        val invalidJson = """{invalid json}"""
        every { mockSharedPreferences.getString("menu_config", null) } returns invalidJson

        // When
        val store = MenuConfigStoreImpl(mockSharedPreferences)
        val config = store.getConfig()

        // Then
        assertEquals(MenuConfig.Default, config)
    }

    @Test
    fun `getConfig returns default config when key is missing`() {
        // Given
        every { mockSharedPreferences.getString("menu_config", null) } returns null

        // When
        val store = MenuConfigStoreImpl(mockSharedPreferences)
        val config = store.getConfig()

        // Then
        assertEquals(MenuConfig.Default, config)
    }

    @Test
    fun `getConfig returns cached config on subsequent calls`() {
        // Given
        val validJson = """{"order":["copy"],"visibility":{"copy":true}}"""
        every { mockSharedPreferences.getString("menu_config", null) } returns validJson

        // When
        val store = MenuConfigStoreImpl(mockSharedPreferences)
        val config1 = store.getConfig()
        val config2 = store.getConfig()

        // Then
        // Should only read SharedPreferences once (cached)
        verify(exactly = 1) { mockSharedPreferences.getString("menu_config", null) }
        assertEquals(config1, config2)
    }

    @Test
    fun `getConfigFlow emits initial config`() =
        runTest {
            // Given
            val validJson = """{"order":["copy","translate"],"visibility":{"copy":true,"translate":false}}"""
            every { mockSharedPreferences.getString("menu_config", null) } returns validJson

            // When
            val store = MenuConfigStoreImpl(mockSharedPreferences)
            val flow = store.getConfigFlow()

            // Then
            flow.collect { config ->
                assertEquals(listOf("copy", "translate"), config.order)
                assertEquals(true, config.visibility["copy"])
                assertEquals(false, config.visibility["translate"])
            }
        }

    @Test
    fun `getConfigFlow handles empty config`() {
        // Given
        val emptyJson = """{"order":[],"visibility":{}}"""
        every { mockSharedPreferences.getString("menu_config", null) } returns emptyJson

        // When
        val store = MenuConfigStoreImpl(mockSharedPreferences)
        val config = store.getConfig()

        // Then
        assertTrue(config.isEmpty())
        assertEquals(emptyList<String>(), config.order)
        assertEquals(emptyMap<String, Boolean>(), config.visibility)
    }

    @Test
    fun `getConfig uses isVisible helper correctly`() {
        // Given
        val validJson = """{"order":["copy","paste"],"visibility":{"copy":true,"paste":false}}"""
        every { mockSharedPreferences.getString("menu_config", null) } returns validJson

        // When
        val store = MenuConfigStoreImpl(mockSharedPreferences)
        val config = store.getConfig()

        // Then
        assertEquals(true, config.isVisible("copy"))
        assertEquals(false, config.isVisible("paste"))
        assertEquals(true, config.isVisible("translate")) // Default for missing keys
    }
}
