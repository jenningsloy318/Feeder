package com.nononsenseapps.feeder.model

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.util.Log
import com.nononsenseapps.feeder.archmodel.DiscoveredApp
import com.nononsenseapps.feeder.archmodel.MenuItemConfig
import com.nononsenseapps.feeder.archmodel.MenuItemType

/**
 * Service for discovering various types of text and share handlers on the device
 */
class TextProcessorDiscoveryService(
    private val context: Context
) {
    private val packageManager: PackageManager by lazy { context.packageManager }
    private val displayNameComparator by lazy { ResolveInfo.DisplayNameComparator(packageManager) }

    /**
     * Discover all available text processors and share handlers
     */
    fun discoverAllTextProcessors(): List<DiscoveredApp> {
        val discoveredApps = mutableListOf<DiscoveredApp>()

        // 1. Discover ACTION_PROCESS_TEXT handlers (existing functionality)
        discoveredApps.addAll(discoverTextProcessors())

        // 2. Discover ACTION_SEND handlers (new - global share menu items)
        discoveredApps.addAll(discoverShareHandlers())

        // 3. Discover other relevant intent handlers
        discoveredApps.addAll(discoverOtherTextHandlers())

        // Remove duplicates based on package name and intent action
        return discoveredApps.distinctBy { "${it.packageName}|${it.intentAction}" }
            .sortedBy { it.label.lowercase() }
    }

    /**
     * Discover apps that handle ACTION_PROCESS_TEXT (existing functionality)
     */
    fun discoverTextProcessors(): List<DiscoveredApp> {
        val intent = Intent(Intent.ACTION_PROCESS_TEXT).apply {
            type = "text/plain"
        }

        return queryIntentActivities(intent).map { resolveInfo ->
            DiscoveredApp(
                packageName = resolveInfo.activityInfo.packageName,
                activityName = resolveInfo.activityInfo.name,
                label = resolveInfo.loadLabel(packageManager).toString(),
                intentAction = Intent.ACTION_PROCESS_TEXT,
                intentType = "text/plain"
            )
        }
    }

    /**
     * Discover apps that handle ACTION_SEND for text/plain (global share menu items)
     */
    fun discoverShareHandlers(): List<DiscoveredApp> {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            addCategory(Intent.CATEGORY_DEFAULT)
        }

        return queryIntentActivities(intent).map { resolveInfo ->
            DiscoveredApp(
                packageName = resolveInfo.activityInfo.packageName,
                activityName = resolveInfo.activityInfo.name,
                label = resolveInfo.loadLabel(packageManager).toString(),
                intentAction = Intent.ACTION_SEND,
                intentType = "text/plain"
            )
        }
    }

    /**
     * Discover other relevant text handlers
     */
    fun discoverOtherTextHandlers(): List<DiscoveredApp> {
        val discoveredApps = mutableListOf<DiscoveredApp>()

        // Discover ACTION_VIEW handlers for text/plain
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            type = "text/plain"
            addCategory(Intent.CATEGORY_DEFAULT)
        }
        discoveredApps.addAll(
            queryIntentActivities(viewIntent).map { resolveInfo ->
                DiscoveredApp(
                    packageName = resolveInfo.activityInfo.packageName,
                    activityName = resolveInfo.activityInfo.name,
                    label = resolveInfo.loadLabel(packageManager).toString(),
                    intentAction = Intent.ACTION_VIEW,
                    intentType = "text/plain"
                )
            }
        )

        // Discover ACTION_SENDTO handlers for text messaging
        val sendToIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = android.net.Uri.parse("smsto:")
            addCategory(Intent.CATEGORY_DEFAULT)
        }
        discoveredApps.addAll(
            queryIntentActivities(sendToIntent).map { resolveInfo ->
                DiscoveredApp(
                    packageName = resolveInfo.activityInfo.packageName,
                    activityName = resolveInfo.activityInfo.name,
                    label = resolveInfo.loadLabel(packageManager).toString(),
                    intentAction = Intent.ACTION_SENDTO,
                    intentType = null
                )
            }
        )

        return discoveredApps
    }

    /**
     * Convert discovered apps to menu item configurations
     */
    fun discoveredAppsToMenuConfigs(discoveredApps: List<DiscoveredApp>): List<MenuItemConfig> {
        return discoveredApps.map { app ->
            val menuItemType = when (app.intentAction) {
                Intent.ACTION_PROCESS_TEXT -> MenuItemType.TEXT_PROCESSOR
                Intent.ACTION_SEND -> MenuItemType.ACTION_SEND_HANDLER
                Intent.ACTION_VIEW -> MenuItemType.SHARE_HANDLER
                Intent.ACTION_SENDTO -> MenuItemType.OTHER_SYSTEM_HANDLER
                else -> MenuItemType.OTHER_SYSTEM_HANDLER
            }

            MenuItemConfig(
                type = menuItemType,
                enabled = true,
                specificApp = app
            )
        }
    }

    /**
     * Helper function to query intent activities with proper API level handling
     */
    private fun queryIntentActivities(intent: Intent): List<ResolveInfo> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                packageManager.queryIntentActivities(intent, 0)
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error querying intent activities for ${intent.action}", e)
            emptyList()
        }
    }

    companion object {
        private const val LOG_TAG = "TextProcessorDiscovery"
    }
}