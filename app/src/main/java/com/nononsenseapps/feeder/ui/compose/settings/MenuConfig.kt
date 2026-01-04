package com.nononsenseapps.feeder.ui.compose.settings

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Persistence model for menu configuration.
 * Follows Moon+ Reader pattern with a flat list and visibility map.
 *
 * @property order Flat list of menu item IDs in display order
 * @property visibility Map of menu item IDs to their visibility state
 */
@Serializable
data class MenuConfig(
    val order: List<String> = emptyList(),
    val visibility: Map<String, Boolean> = emptyMap(),
) {
    companion object {
        /**
         * Default empty configuration.
         */
        val Default = MenuConfig()

        /**
         * Create MenuConfig from JSON string.
         */
        fun fromJson(jsonString: String): MenuConfig {
            return try {
                Json.decodeFromString(jsonString)
            } catch (e: Exception) {
                Default
            }
        }
    }

    /**
     * Check if the configuration is empty (no items).
     */
    fun isEmpty(): Boolean = order.isEmpty()

    /**
     * Check if a menu item is visible.
     * Returns true by default if item not in visibility map.
     *
     * @param itemId The menu item ID to check
     * @return true if visible, false otherwise
     */
    fun isVisible(itemId: String): Boolean {
        return visibility[itemId] ?: true
    }

    /**
     * Convert configuration to JSON string.
     */
    fun toJson(): String {
        return Json.encodeToString(this)
    }
}
