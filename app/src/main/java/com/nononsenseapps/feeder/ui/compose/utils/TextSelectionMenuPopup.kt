package com.nononsenseapps.feeder.ui.compose.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.nononsenseapps.feeder.R
import com.nononsenseapps.feeder.ui.compose.settings.MenuConfig
import com.nononsenseapps.feeder.ui.compose.settings.MenuType
import com.nononsenseapps.feeder.ui.compose.settings.SelectionMenuItem
import com.nononsenseapps.feeder.util.ActivityLauncher

/**
 * Compose Popup-based text selection menu.
 *
 * Displays a horizontal toolbar with user-configured items when text is selected,
 * similar to the Android system floating toolbar.
 *
 * @param menuState Mutable state holding toolbar state (null = menu hidden)
 * @param menuConfig Current menu configuration (order + visibility)
 * @param menuItems List of all available menu items
 * @param activityLauncher Launcher for third-party activities
 * @param onActionExecuted Callback invoked when any menu action completes
 */
@Composable
fun TextSelectionMenuPopup(
    menuState: MutableState<ToolbarState?>,
    menuConfig: MenuConfig,
    menuItems: List<SelectionMenuItem>,
    activityLauncher: ActivityLauncher,
    onActionExecuted: () -> Unit,
) {
    val state = menuState.value
    if (state == null) {
        return
    }
    val context = LocalContext.current

    // Calculate popup position based on selection rectangle
    val offset = rememberMenuOffset(state.rect)

    // Filter and sort items according to configuration
    val enabledItems = rememberMenuItems(menuItems, menuConfig)

    // Calculate toolbar width based on item count
    val toolbarWidth = with(LocalDensity.current) {
        (enabledItems.size * 80).dp.toPx()  // Approx 80dp per item
    }.toInt()

    Popup(
        alignment = androidx.compose.ui.Alignment.TopStart,
        offset = IntOffset(
            x = (offset.x - toolbarWidth / 2),
            y = offset.y - 96  // 96dp above first line
        ),
        onDismissRequest = {
            menuState.value = null
            onActionExecuted()
        },
        // Make popup non-focusable to preserve text selection
        // This prevents the toolbar from stealing focus from SelectionContainer
        properties = PopupProperties(
            focusable = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        ),
    ) {
        Surface(
            modifier = Modifier,
            shape = RoundedCornerShape(8.dp),
            tonalElevation = 2.dp,
            shadowElevation = 8.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                enabledItems.forEach { item ->
                    ToolbarItem(
                        name = item.name,
                        onClick = {
                            executeAction(
                                item = item,
                                state = state,
                                context = context,
                                activityLauncher = activityLauncher,
                            )
                            menuState.value = null
                            onActionExecuted()
                        },
                    )
                }
            }
        }
    }
}

/**
 * Single toolbar item with smaller text size.
 */
@Composable
private fun ToolbarItem(
    name: String,
    onClick: () -> Unit,
) {
    Text(
        text = name,
        modifier = Modifier
            .clickable(
                onClick = onClick,
                // Don't steal focus when clicking toolbar item
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

/**
 * Execute a menu item action.
 *
 * Routes to appropriate handler based on menu item type:
 * - SYSTEM: Copy, Paste, Cut, Select All
 * - APPLICATION: Read Aloud, Translate
 * - THIRD_PARTY: Launch external app
 *
 * @param item The menu item to execute
 * @param state Current toolbar state containing callbacks and text
 * @param context Application context
 * @param activityLauncher Launcher for third-party activities
 */
private fun executeAction(
    item: SelectionMenuItem,
    state: ToolbarState,
    context: Context,
    activityLauncher: ActivityLauncher,
) {
    when (item.type) {
        MenuType.SYSTEM -> executeSystemAction(item.id, state)
        MenuType.APPLICATION -> executeApplicationAction(item.id, state, context)
        MenuType.THIRD_PARTY -> executeThirdPartyAction(
            item = item,
            state = state,
            context = context,
            activityLauncher = activityLauncher,
        )
    }
}

/**
 * Execute system action (Copy, Paste, Cut, Select All).
 */
private fun executeSystemAction(
    actionId: String,
    state: ToolbarState,
) {
    when (actionId) {
        "android.intent.action.COPY" -> state.onCopyRequested?.invoke()
        "android.intent.action.PASTE" -> state.onPasteRequested?.invoke()
        "android.intent.action.CUT" -> state.onCutRequested?.invoke()
        "android.intent.action.SELECT_ALL" -> state.onSelectAllRequested?.invoke()
    }
}

/**
 * Execute Feeder application action (Read Aloud, Translate).
 */
private fun executeApplicationAction(
    actionId: String,
    state: ToolbarState,
    context: Context,
) {
    when (actionId) {
        "com.nononsenseapps.feeder.action.READ_ALOUD" -> {
            // Extract text on-demand to avoid dismissing selection when toolbar shows
            val selectedText = extractSelectedText(context, state)
            state.onReadAloud?.invoke(selectedText)
        }
        "com.nononsenseapps.feeder.action.TRANSLATE" -> {
            // Extract text on-demand to avoid dismissing selection when toolbar shows
            val selectedText = extractSelectedText(context, state)
            state.onTranslate?.invoke(selectedText)
        }
    }
}

/**
 * Execute third-party app action.
 *
 * Extracts selected text via clipboard workaround and launches external app
 * with ACTION_PROCESS_TEXT intent.
 * Shows error toast if launch fails.
 */
private fun executeThirdPartyAction(
    item: SelectionMenuItem,
    state: ToolbarState,
    context: Context,
    activityLauncher: ActivityLauncher,
) {
    val componentName = item.componentName ?: return

    // Extract text on-demand for third-party apps using clipboard workaround
    val selectedText = extractSelectedText(context, state)

    try {
        val intent = Intent(Intent.ACTION_PROCESS_TEXT).apply {
            type = "text/plain"
            component = componentName
            putExtra(Intent.EXTRA_PROCESS_TEXT, selectedText)
        }
        activityLauncher.startActivity(
            openAdjacentIfSuitable = true,
            intent = intent,
        )
    } catch (e: Exception) {
        Toast.makeText(
            context,
            context.getString(R.string.unable_to_open_app, item.name),
            Toast.LENGTH_SHORT,
        ).show()
    }
}

/**
 * Extract selected text using clipboard workaround.
 *
 * Called on-demand for third-party apps only to avoid dismissing
 * text selection when toolbar is shown.
 */
private fun extractSelectedText(
    context: Context,
    state: ToolbarState,
): String {
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val previousClip = clipboardManager.primaryClip

    // Copy selected text to clipboard
    state.onCopyRequested?.invoke()

    // Read the selected text from clipboard
    val selectedText = clipboardManager.primaryClip?.getItemAt(0)?.text?.toString() ?: ""

    // Restore previous clipboard content
    if (previousClip != null) {
        clipboardManager.setPrimaryClip(previousClip)
    } else {
        clipboardManager.clearPrimaryClip()
    }

    return selectedText
}

/**
 * Calculate menu position offset based on selection rectangle.
 *
 * Positions menu:
 * - Horizontally: centered on selection
 * - Vertically: above the first line of selection
 *
 * @param rect The text selection rectangle
 * @return IntOffset for Popup positioning
 */
@Composable
private fun rememberMenuOffset(rect: androidx.compose.ui.geometry.Rect): IntOffset {
    // Center horizontally on selection
    val x = (rect.left + rect.right) / 2

    // Position above the first line of selection
    val y = rect.top.toInt()

    return IntOffset(x.toInt(), y)
}

/**
 * Remember filtered and sorted menu items.
 *
 * Filters items by visibility flag and sorts by custom order.
 *
 * @param allItems All available menu items
 * @param config Menu configuration with visibility and order
 * @return Filtered and sorted list of menu items
 */
@Composable
private fun rememberMenuItems(
    allItems: List<SelectionMenuItem>,
    config: MenuConfig,
): List<SelectionMenuItem> {
    // Filter visible items
    val visibleItems = allItems.filter { item ->
        config.isVisible(item.id)
    }

    // Sort by custom order
    val orderMap = config.order.mapIndexed { index, id -> id to index }.toMap()

    return visibleItems.sortedBy { item ->
        orderMap[item.id] ?: Int.MAX_VALUE
    }
}
