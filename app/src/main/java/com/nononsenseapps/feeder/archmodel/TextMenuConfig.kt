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
)

/**
 * Types of menu items available in text selection menu
 */
@Serializable
enum class MenuItemType {
    COPY,
    PASTE,
    CUT,
    SELECT_ALL,
    TEXT_PROCESSOR,
}
