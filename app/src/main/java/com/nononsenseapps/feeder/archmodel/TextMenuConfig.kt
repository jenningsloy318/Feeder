package com.nononsenseapps.feeder.archmodel

import kotlinx.serialization.Serializable

/**
 * Configuration for text selection menu items
 */
@Serializable
data class TextMenuConfig(
    val items: List<MenuItemConfig> = defaultMenuItems,
) {
    companion object {
        val defaultMenuItems =
            listOf(
                MenuItemConfig(MenuItemType.COPY, enabled = true),
                MenuItemConfig(MenuItemType.PASTE, enabled = true),
                MenuItemConfig(MenuItemType.CUT, enabled = true),
                MenuItemConfig(MenuItemType.SELECT_ALL, enabled = true),
                MenuItemConfig(MenuItemType.TEXT_PROCESSOR, enabled = true),
            )
    }
}

/**
 * Configuration for a single menu item
 */
@Serializable
data class MenuItemConfig(
    val type: MenuItemType,
    val enabled: Boolean,
    val order: Int = type.defaultOrder,
    val specificApp: DiscoveredApp? = null,
)

/**
 * Represents a discovered app that can handle text or share intents
 */
@Serializable
data class DiscoveredApp(
    val packageName: String,
    val activityName: String? = null,
    val label: String,
    val intentAction: String,
    val intentType: String? = null,
)

/**
 * Types of menu items available in text selection menu
 */
@Serializable
enum class MenuItemType(val defaultOrder: Int) {
    COPY(0),
    PASTE(1),
    CUT(2),
    SELECT_ALL(3),
    TEXT_PROCESSOR(4),
    SHARE_HANDLER(5),
    ACTION_SEND_HANDLER(6),
    OTHER_SYSTEM_HANDLER(7),
}
