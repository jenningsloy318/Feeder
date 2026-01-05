package com.nononsenseapps.feeder.ui.compose.utils

import android.content.Context
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.TextToolbarStatus
import com.nononsenseapps.feeder.ui.compose.settings.MenuConfig
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for CustomFeederTextToolbar.
 */
class CustomFeederTextToolbarTest {
    private lateinit var mockContext: Context
    private lateinit var mockMenuConfigStore: MenuConfigStore
    private lateinit var mockActivityLauncher: com.nononsenseapps.feeder.util.ActivityLauncher
    private lateinit var toolbar: CustomFeederTextToolbar

    private val onReadAloud: (String) -> Unit = {}
    private val onTranslate: (String) -> Unit = {}

    @Before
    fun setup() {
        mockContext = mockk()
        mockMenuConfigStore = mockk()
        mockActivityLauncher = mockk()

        every { mockMenuConfigStore.getConfig() } returns MenuConfig.Default

        toolbar = CustomFeederTextToolbar(
            context = mockContext,
            menuConfigStore = mockMenuConfigStore,
            activityLauncher = mockActivityLauncher,
            onReadAloud = onReadAloud,
            onTranslate = onTranslate,
        )
    }

    @Test
    fun `initial status is Hidden`() {
        assertEquals(TextToolbarStatus.Hidden, toolbar.status)
    }

    @Test
    fun `initial menuState is null`() {
        assertNull(toolbar.menuState.value)
    }

    @Test
    fun `showMenu updates menuState`() {
        // Given
        val rect = Rect(10f, 20f, 100f, 50f)
        val onCopyRequested: () -> Unit = {}
        val onSelectAllRequested: () -> Unit = {}

        // When
        toolbar.showMenu(
            rect = rect,
            onCopyRequested = onCopyRequested,
            onPasteRequested = null,
            onCutRequested = null,
            onSelectAllRequested = onSelectAllRequested,
        )

        // Then
        val state = toolbar.menuState.value
        assertNotNull(state)
        assertEquals(rect, state?.rect)
        assertEquals(onCopyRequested, state?.onCopyRequested)
        assertEquals(onSelectAllRequested, state?.onSelectAllRequested)
        assertEquals(onReadAloud, state?.onReadAloud)
        assertEquals(onTranslate, state?.onTranslate)
    }

    @Test
    fun `showMenu updates status to Shown`() {
        // Given
        val rect = Rect.Zero
        val onCopy: () -> Unit = {}

        // When
        toolbar.showMenu(
            rect = rect,
            onCopyRequested = onCopy,
            onPasteRequested = null,
            onCutRequested = null,
            onSelectAllRequested = null,
        )

        // Then
        assertEquals(TextToolbarStatus.Shown, toolbar.status)
    }

    @Test
    fun `hide clears menuState`() {
        // Given
        val rect = Rect.Zero
        toolbar.showMenu(
            rect = rect,
            onCopyRequested = {},
            onPasteRequested = null,
            onCutRequested = null,
            onSelectAllRequested = null,
        )
        assertNotNull(toolbar.menuState.value)

        // When
        toolbar.hide()

        // Then
        assertNull(toolbar.menuState.value)
    }

    @Test
    fun `hide updates status to Hidden`() {
        // Given
        val rect = Rect.Zero
        toolbar.showMenu(
            rect = rect,
            onCopyRequested = {},
            onPasteRequested = null,
            onCutRequested = null,
            onSelectAllRequested = null,
        )
        assertEquals(TextToolbarStatus.Shown, toolbar.status)

        // When
        toolbar.hide()

        // Then
        assertEquals(TextToolbarStatus.Hidden, toolbar.status)
    }

    @Test
    fun `getMenuConfig returns config from store`() {
        // Given
        val expectedConfig = MenuConfig(
            order = listOf("copy", "translate"),
            visibility = mapOf("copy" to true, "translate" to true),
        )
        every { mockMenuConfigStore.getConfig() } returns expectedConfig

        // When
        val config = toolbar.getMenuConfig()

        // Then
        assertEquals(expectedConfig, config)
        verify { mockMenuConfigStore.getConfig() }
    }

    @Test
    fun `callbacks are stored correctly in ToolbarState`() {
        // Given
        val rect = Rect.Zero
        val onCopy: () -> Unit = {}
        val onPaste: () -> Unit = {}
        val onCut: () -> Unit = {}
        val onSelectAll: () -> Unit = {}
        val onReadAloud: (String) -> Unit = { _ -> }
        val onTranslate: (String) -> Unit = { _ -> }

        // When
        toolbar.showMenu(
            rect = rect,
            onCopyRequested = onCopy,
            onPasteRequested = onPaste,
            onCutRequested = onCut,
            onSelectAllRequested = onSelectAll,
        )

        // Then
        val state = toolbar.menuState.value
        assertNotNull(state)
        assertEquals(onCopy, state?.onCopyRequested)
        assertEquals(onPaste, state?.onPasteRequested)
        assertEquals(onCut, state?.onCutRequested)
        assertEquals(onSelectAll, state?.onSelectAllRequested)
        assertEquals(onReadAloud, state?.onReadAloud)
        assertEquals(onTranslate, state?.onTranslate)
    }

    @Test
    fun `multiple showMenu calls update state correctly`() {
        // Given
        val rect1 = Rect(0f, 0f, 50f, 50f)
        val rect2 = Rect(100f, 100f, 150f, 150f)

        // When
        toolbar.showMenu(
            rect = rect1,
            onCopyRequested = {},
            onPasteRequested = null,
            onCutRequested = null,
            onSelectAllRequested = null,
        )
        val state1 = toolbar.menuState.value

        toolbar.showMenu(
            rect = rect2,
            onCopyRequested = {},
            onPasteRequested = null,
            onCutRequested = null,
            onSelectAllRequested = null,
        )
        val state2 = toolbar.menuState.value

        // Then
        assertEquals(rect1, state1?.rect)
        assertEquals(rect2, state2?.rect)
    }
}
