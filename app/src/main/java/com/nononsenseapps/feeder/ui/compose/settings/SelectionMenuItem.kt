package com.nononsenseapps.feeder.ui.compose.settings

import android.content.ComponentName
import androidx.compose.runtime.Immutable

/**
 * Represents a single selection menu item.
 *
 * @property id Unique identifier for the menu item
 * @property name Display name of the menu item
 * @property description Optional description
 * @property icon Optional icon resource name
 * @property enabled Whether the menu item is enabled
 * @property type The type of menu item (SYSTEM, APPLICATION, THIRD_PARTY)
 * @property componentName The ComponentName for third-party apps
 * @property packageName The package name for third-party apps
 * @property order Display order in the list
 * @property visible Whether the item is visible in the menu (user toggle)
 */
@Immutable
data class SelectionMenuItem(
    val id: String,
    val name: String,
    val description: String? = null,
    val icon: String? = null,
    val enabled: Boolean = true,
    val type: MenuType = MenuType.SYSTEM,
    val componentName: ComponentName? = null,
    val packageName: String? = null,
    val order: Int = 0,
    val visible: Boolean = true,
)

/**
 * Enum representing the type of selection menu item.
 */
enum class MenuType {
    /**
     * System menu items (copy, paste, cut, select_all).
     */
    SYSTEM,

    /**
     * Application-specific menu items (read_aloud, translate).
     */
    APPLICATION,

    /**
     * Third-party app menu items discovered via ACTION_PROCESS_TEXT.
     */
    THIRD_PARTY,
}
