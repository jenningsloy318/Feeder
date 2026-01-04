package com.nononsenseapps.feeder.ui.compose.settings

import androidx.compose.runtime.Immutable

/**
 * Represents a single selection menu item.
 *
 * @property id Unique identifier for the menu item
 * @property name Display name of the menu item
 * @property description Optional description
 * @property icon Optional icon resource name
 * @property enabled Whether the menu item is enabled
 */
@Immutable
data class SelectionMenuItem(
    val id: String,
    val name: String,
    val description: String? = null,
    val icon: String? = null,
    val enabled: Boolean = true,
)
