package com.nononsenseapps.feeder.ui.compose.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.nononsenseapps.feeder.ui.compose.settings.SelectionMenuItem
import com.nononsenseapps.feeder.util.ActivityLauncher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.kodein.di.compose.LocalDI
import org.kodein.di.instance

/**
 * Handles the text selection menu display and actions.
 *
 * This composable observes the CustomFeederTextToolbar state and displays
 * the TextSelectionMenuPopup when text is selected.
 *
 * Should be placed at the top level of the screen where text selection is needed.
 *
 * @param onReadAloud Optional callback for read aloud action
 * @param onTranslate Optional callback for translate action
 */
@Composable
fun TextSelectionMenuHandler(
    onReadAloud: ((String) -> Unit)? = null,
    onTranslate: ((String) -> Unit)? = null,
) {
    val activityLauncher: ActivityLauncher by LocalDI.current.instance()
    val menuDiscoveryService: com.nononsenseapps.feeder.ui.compose.settings.MenuDiscoveryService by LocalDI.current.instance()

    // Remember menu items (discovered once)
    var menuItems by remember { mutableStateOf<List<SelectionMenuItem>>(emptyList()) }

    // Load menu items on first composition
    LaunchedEffect(Unit) {
        if (menuItems.isEmpty()) {
            menuItems =
                withContext(Dispatchers.Default) {
                    menuDiscoveryService.discoverAll()
                }
        }
    }

    // Get current toolbar
    val textToolbar = androidx.compose.ui.platform.LocalTextToolbar.current

    // Check if this is our custom toolbar
    if (textToolbar is CustomFeederTextToolbar) {
        // Get menu config
        val menuConfig = textToolbar.getMenuConfig()

        // Display popup when menu is shown
        TextSelectionMenuPopup(
            menuState = textToolbar.menuState.value,
            onDismiss = { textToolbar.menuState.value = null },
            menuConfig = menuConfig,
            menuItems = menuItems,
            activityLauncher = activityLauncher,
            onActionExecute = {
                // Popup dismissed - no action needed
            },
        )
    }
}
