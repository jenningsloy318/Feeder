package com.nononsenseapps.feeder.ui.compose.utils

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import com.nononsenseapps.feeder.ui.compose.settings.MenuConfig
import com.nononsenseapps.feeder.util.ActivityLauncher

/**
 * Custom text toolbar implementation using Compose Popup instead of ActionMode.
 *
 * Replaces FeederTextToolbar with a Compose-based approach that:
 * - Bypasses Android View ActionMode system (bypassed on Android 13+)
 * - Reads configuration from MenuConfigStore
 * - Displays user's customized menu (order + visibility)
 * - Works consistently on Android 7-15+
 *
 * @property context Application context
 * @property menuConfigStore Store for menu configuration
 * @property activityLauncher Launcher for third-party activities
 * @property onReadAloud Optional callback for read aloud action
 * @property onTranslate Optional callback for translate action
 */
class CustomFeederTextToolbar(
    private val context: Context,
    private val menuConfigStore: MenuConfigStore,
    private val activityLauncher: ActivityLauncher,
    private val onReadAloud: ((String) -> Unit)?,
    private val onTranslate: ((String) -> Unit)?,
) : TextToolbar {
    /**
     * Mutable state for menu visibility and data.
     * When non-null, the popup menu should be displayed.
     */
    private val _menuState: MutableState<ToolbarState?> = mutableStateOf(null)

    /**
     * Public read-only menu state for Compose UI to observe.
     */
    val menuState: MutableState<ToolbarState?> = _menuState

    override var status: TextToolbarStatus = TextToolbarStatus.Hidden
        private set

    /**
     * Hide the text toolbar.
     * Clears menuState which dismisses the popup.
     */
    override fun hide() {
        status = TextToolbarStatus.Hidden
        _menuState.value = null
    }

    /**
     * Show the text selection menu.
     *
     * Called by Compose SelectionContainer when text is selected.
     * Updates menuState which triggers the popup to appear.
     *
     * Note: The TextToolbar.showMenu() interface doesn't provide the selected text.
     * We use empty text string to avoid clipboard workaround which dismisses selection.
     * For third-party apps, text is extracted when action is clicked via clipboard.
     *
     * @param rect The rectangle coordinates where text was selected
     * @param onCopyRequested Callback for copy action (system)
     * @param onPasteRequested Callback for paste action (system)
     * @param onCutRequested Callback for cut action (system)
     * @param onSelectAllRequested Callback for select all action (system)
     */
    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?,
    ) {
        status = TextToolbarStatus.Shown

        // Don't use clipboard workaround here as it dismisses text selection.
        // Store empty text for now - will extract on-demand for third-party actions.
        _menuState.value =
            ToolbarState(
                rect = rect,
                text = "", // Empty to preserve selection
                onCopyRequested = onCopyRequested,
                onPasteRequested = onPasteRequested,
                onCutRequested = onCutRequested,
                onSelectAllRequested = onSelectAllRequested,
                onReadAloud = onReadAloud,
                onTranslate = onTranslate,
            )
    }

    /**
     * Get the current menu configuration.
     * Loads from MenuConfigStore (cached).
     *
     * @return Current MenuConfig
     */
    fun getMenuConfig(): MenuConfig = menuConfigStore.getConfig()
}

/**
 * Immutable state for the text selection toolbar menu.
 *
 * Contains all data needed to render and interact with the popup menu.
 *
 * @property rect The selection rectangle coordinates
 * @property text The selected text (may be empty initially)
 * @property onCopyRequested Callback for copy action
 * @property onPasteRequested Callback for paste action
 * @property onCutRequested Callback for cut action
 * @property onSelectAllRequested Callback for select all action
 * @property onReadAloud Callback for read aloud action
 * @property onTranslate Callback for translate action
 */
@Immutable
data class ToolbarState(
    val rect: Rect,
    val text: String,
    val onCopyRequested: (() -> Unit)?,
    val onPasteRequested: (() -> Unit)?,
    val onCutRequested: (() -> Unit)?,
    val onSelectAllRequested: (() -> Unit)?,
    val onReadAloud: ((String) -> Unit)?,
    val onTranslate: ((String) -> Unit)?,
)
