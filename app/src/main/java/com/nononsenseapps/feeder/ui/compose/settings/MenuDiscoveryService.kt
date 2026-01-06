package com.nononsenseapps.feeder.ui.compose.settings

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.nononsenseapps.feeder.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance

/**
 * Service for discovering selection menu items from various sources.
 *
 * Discovers menu items from:
 * - System actions (copy, paste, cut, select_all)
 * - Feeder application actions (read_aloud, translate)
 * - Third-party apps that handle ACTION_PROCESS_TEXT
 */
class MenuDiscoveryService(
    override val di: DI,
) : DIAware {
    private val context: Context by instance()

    private val packageManager: PackageManager
        get() = context.packageManager

    /**
     * Discover all menu items from all sources.
     *
     * @return Flat list of all discovered menu items
     */
    suspend fun discoverAll(): List<SelectionMenuItem> =
        withContext(Dispatchers.Default) {
            val systemMenus = discoverSystemMenus()
            val feederMenus = discoverFeederMenus()
            val thirdPartyMenus = discoverThirdPartyMenus()

            // Return flat list (no sections)
            systemMenus + feederMenus + thirdPartyMenus
        }

    /**
     * Discover system menu items.
     *
     * @return List of system menu items
     */
    private fun discoverSystemMenus(): List<SelectionMenuItem> =
        listOf(
            SelectionMenuItem(
                id = "android.intent.action.COPY",
                name = context.getString(android.R.string.copy),
                description = context.getString(R.string.selection_menu_copy_description),
                icon = null, // System default icon
                enabled = true,
                type = MenuType.SYSTEM,
                componentName = null,
                packageName = null,
                order = 0,
                visible = true,
            ),
            SelectionMenuItem(
                id = "android.intent.action.PASTE",
                name = context.getString(android.R.string.paste),
                description = context.getString(R.string.selection_menu_paste_description),
                icon = null,
                enabled = true,
                type = MenuType.SYSTEM,
                componentName = null,
                packageName = null,
                order = 1,
                visible = true,
            ),
            SelectionMenuItem(
                id = "android.intent.action.CUT",
                name = context.getString(android.R.string.cut),
                description = context.getString(R.string.selection_menu_cut_description),
                icon = null,
                enabled = true,
                type = MenuType.SYSTEM,
                componentName = null,
                packageName = null,
                order = 2,
                visible = true,
            ),
            SelectionMenuItem(
                id = "android.intent.action.SELECT_ALL",
                name = context.getString(android.R.string.selectAll),
                description = context.getString(R.string.selection_menu_select_all_description),
                icon = null,
                enabled = true,
                type = MenuType.SYSTEM,
                componentName = null,
                packageName = null,
                order = 3,
                visible = true,
            ),
        )

    /**
     * Discover Feeder application menu items.
     *
     * @return List of Feeder menu items
     */
    private fun discoverFeederMenus(): List<SelectionMenuItem> =
        listOf(
            SelectionMenuItem(
                id = "com.nononsenseapps.feeder.action.READ_ALOUD",
                name = context.getString(R.string.selection_menu_read_aloud),
                description = context.getString(R.string.selection_menu_read_aloud_description),
                icon = null, // TODO: Add icon
                enabled = true,
                type = MenuType.APPLICATION,
                componentName = null,
                packageName = null,
                order = 4,
                visible = true,
            ),
            SelectionMenuItem(
                id = "com.nononsenseapps.feeder.action.TRANSLATE",
                name = context.getString(R.string.selection_menu_translate),
                description = context.getString(R.string.selection_menu_translate_description),
                icon = null, // TODO: Add icon
                enabled = true,
                type = MenuType.APPLICATION,
                componentName = null,
                packageName = null,
                order = 5,
                visible = true,
            ),
        )

    /**
     * Discover third-party menu items that handle ACTION_PROCESS_TEXT.
     *
     * @return List of third-party menu items sorted by display name
     */
    private fun discoverThirdPartyMenus(): List<SelectionMenuItem> =
        try {
            val intent =
                android.content.Intent(ACTION_PROCESS_TEXT).apply {
                    type = "text/plain"
                }

            val resolveInfos =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.queryIntentActivities(
                        intent,
                        PackageManager.ResolveInfoFlags.of(PMATCH_DEFAULT_ONLY.toLong()),
                    )
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.queryIntentActivities(intent, PMATCH_DEFAULT_ONLY)
                }

            resolveInfos
                .mapNotNull { resolveInfo ->
                    try {
                        val appName = resolveInfo.loadLabel(packageManager).toString()
                        val componentName =
                            android.content.ComponentName(
                                resolveInfo.activityInfo.packageName,
                                resolveInfo.activityInfo.name,
                            )

                        SelectionMenuItem(
                            id = componentName.flattenToString(),
                            name = appName,
                            description =
                                context.getString(
                                    R.string.selection_menu_third_party_description,
                                    appName,
                                ),
                            icon = null,
                            enabled = true,
                            type = MenuType.THIRD_PARTY,
                            componentName = componentName,
                            packageName = resolveInfo.activityInfo.packageName,
                            order = 0, // Will be set later
                            visible = true,
                        )
                    } catch (e: Exception) {
                        null
                    }
                }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
        } catch (e: Exception) {
            // Log error and return empty list
            emptyList()
        }

    companion object {
        private const val ACTION_PROCESS_TEXT = "android.intent.action.PROCESS_TEXT"
        private const val PMATCH_DEFAULT_ONLY = 0x00010000
    }
}
