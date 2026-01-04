package com.nononsenseapps.feeder.ui.compose.utils

import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.util.Log
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import com.nononsenseapps.feeder.ui.compose.settings.MenuConfig
import com.nononsenseapps.feeder.ui.compose.settings.MenuDiscoveryService
import com.nononsenseapps.feeder.ui.compose.settings.MenuType
import com.nononsenseapps.feeder.ui.compose.settings.SelectionMenuItem
import com.nononsenseapps.feeder.util.ActivityLauncher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.compose.LocalDI
import org.kodein.di.instance

private const val LOG_TAG = "FEEDER_TEXTTOOL"

@Composable
fun WithFeederTextToolbar(content: @Composable () -> Unit) {
    val activityLauncher: ActivityLauncher by LocalDI.current.instance()
    val di: DI = LocalDI.current
    CompositionLocalProvider(LocalTextToolbar provides FeederTextToolbar(LocalView.current, activityLauncher, di)) {
        content()
    }
}

class FeederTextToolbar(
    private val view: View,
    activityLauncher: ActivityLauncher,
    private val di: DI,
) : TextToolbar {
    private var actionMode: ActionMode? = null
    private val textActionModeCallback: FeederTextActionModeCallback =
        FeederTextActionModeCallback(
            context = view.context,
            activityLauncher = activityLauncher,
            di = di,
            onActionModeDestroy = {
                actionMode = null
            },
        )
    override var status: TextToolbarStatus = TextToolbarStatus.Hidden
        private set

    override fun hide() {
        status = TextToolbarStatus.Hidden
        actionMode?.finish()
        actionMode = null
    }

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?,
    ) {
        textActionModeCallback.rect = rect
        textActionModeCallback.onCopyRequested = onCopyRequested
        textActionModeCallback.onCutRequested = onCutRequested
        textActionModeCallback.onPasteRequested = onPasteRequested
        textActionModeCallback.onSelectAllRequested = onSelectAllRequested
        if (actionMode == null) {
            status = TextToolbarStatus.Shown
            actionMode =
                view.startActionMode(
                    FloatingTextActionModeCallback(textActionModeCallback),
                    ActionMode.TYPE_FLOATING,
                )
        } else {
            actionMode?.invalidate()
        }
    }
}

class FeederTextActionModeCallback(
    val context: Context,
    val onActionModeDestroy: (() -> Unit)? = null,
    var rect: Rect = Rect.Zero,
    val activityLauncher: ActivityLauncher,
    override val di: DI,
    var onCopyRequested: (() -> Unit)? = null,
    var onPasteRequested: (() -> Unit)? = null,
    var onCutRequested: (() -> Unit)? = null,
    var onSelectAllRequested: (() -> Unit)? = null,
) : ActionMode.Callback, DIAware {
    private val displayNameComparator by lazy {
        ResolveInfo.DisplayNameComparator(packageManager)
    }
    private val packageManager by lazy {
        context.packageManager
    }
    private val clipboardManager by lazy {
        context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
    }

    // DI-injected dependencies
    private val sp: SharedPreferences by instance()
    private val menuDiscoveryService: MenuDiscoveryService by instance()

    // Menu item storage
    private val textProcessors = mutableListOf<ComponentName>()
    private val feederItems = mutableMapOf<Int, SelectionMenuItem>()

    // Cache for discovered menu items
    private var cachedDiscoveredItems: List<SelectionMenuItem>? = null
    private var cacheTimestamp: Long = 0

    companion object {
        private const val CACHE_DURATION_MS = 5_000L // 5 seconds
    }

    override fun onCreateActionMode(
        mode: ActionMode?,
        menu: Menu?,
    ): Boolean {
        requireNotNull(menu)
        requireNotNull(mode)

        // Load configuration
        val config = loadMenuConfig()
        Log.d(LOG_TAG, "Loaded menu config: ${config.order.size} items")

        // Discover menu items (with caching)
        val discoveredItems = runBlocking {
            getDiscoveredItems()
        }
        Log.d(LOG_TAG, "Discovered ${discoveredItems.size} menu items")

        // Filter by visibility
        val visibleItems = filterByVisibility(discoveredItems, config)
        Log.d(LOG_TAG, "Filtered to ${visibleItems.size} visible items")

        // Sort by configured order
        val sortedItems = sortByConfigOrder(visibleItems, config)
        Log.d(LOG_TAG, "Sorted items by config order")

        // Build menu from sorted items
        sortedItems.forEachIndexed { index, item ->
            addMenuItemFromConfig(menu, item, index)
        }

        return true
    }

    override fun onPrepareActionMode(
        mode: ActionMode?,
        menu: Menu?,
    ): Boolean {
        // Menu is built dynamically in onCreateActionMode based on config
        // No preparation needed
        return true
    }

    override fun onActionItemClicked(
        mode: ActionMode?,
        item: MenuItem?,
    ): Boolean {
        val itemId = item!!.itemId

        return when {
            // System items (0-99)
            itemId < 100 -> {
                when (itemId) {
                    MenuItemOption.Copy.id -> onCopyRequested?.invoke()
                    MenuItemOption.Paste.id -> onPasteRequested?.invoke()
                    MenuItemOption.Cut.id -> onCutRequested?.invoke()
                    MenuItemOption.SelectAll.id -> onSelectAllRequested?.invoke()
                    else -> false
                }
                mode?.finish()
                true
            }
            // Third-party apps (100-199)
            itemId in 100..199 -> {
                handleThirdPartyClick(itemId)
                mode?.finish()
                true
            }
            // Feeder items (200-299)
            itemId in 200..299 -> {
                handleFeederItemClick(itemId)
                mode?.finish()
                true
            }
            else -> false
        }
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        onActionModeDestroy?.invoke()
    }

    // ========================================================================
    // Configuration Loading Methods
    // ========================================================================

    /**
     * Load menu configuration from SharedPreferences.
     * Returns MenuConfig.Default if no config exists or parsing fails.
     */
    private fun loadMenuConfig(): MenuConfig {
        val json = sp.getString("selection_menu_config", null)
        return if (json != null) {
            try {
                MenuConfig.fromJson(json)
            } catch (e: Exception) {
                Log.e(LOG_TAG, "Failed to parse menu config, using default", e)
                MenuConfig.Default
            }
        } else {
            MenuConfig.Default
        }
    }

    /**
     * Get discovered menu items with caching.
     * Caches items for 5 seconds to reduce PackageManager queries.
     */
    private suspend fun getDiscoveredItems(): List<SelectionMenuItem> {
        val now = System.currentTimeMillis()
        val cacheValid = cachedDiscoveredItems != null &&
                (now - cacheTimestamp) < CACHE_DURATION_MS

        if (cacheValid) {
            Log.d(LOG_TAG, "Using cached discovered items")
            return cachedDiscoveredItems!!
        }

        Log.d(LOG_TAG, "Discovering menu items...")
        val items = menuDiscoveryService.discoverAll()
        cachedDiscoveredItems = items
        cacheTimestamp = now
        return items
    }

    // ========================================================================
    // Filtering and Sorting Methods
    // ========================================================================

    /**
     * Filter menu items by visibility and availability.
     * - Keeps only items marked as visible in config
     * - Skips third-party apps that are not installed
     */
    private fun filterByVisibility(
        items: List<SelectionMenuItem>,
        config: MenuConfig,
    ): List<SelectionMenuItem> {
        return items.filter { item ->
            config.isVisible(item.id) && item.isAvailable()
        }
    }

    /**
     * Check if a menu item is available (installed).
     * For third-party apps, checks if the package is installed.
     */
    private fun SelectionMenuItem.isAvailable(): Boolean {
        return when (type) {
            MenuType.THIRD_PARTY -> {
                try {
                    packageManager.getApplicationInfo(packageName!!, 0)
                    true
                } catch (e: PackageManager.NameNotFoundException) {
                    Log.d(LOG_TAG, "Third-party app not installed: $packageName")
                    false
                }
            }
            else -> true
        }
    }

    /**
     * Sort menu items by configured order.
     * - If config is empty, uses default order (system → feeder → third-party)
     * - Otherwise, sorts by config.order position
     * - New items (not in config) are appended to the end
     */
    private fun sortByConfigOrder(
        items: List<SelectionMenuItem>,
        config: MenuConfig,
    ): List<SelectionMenuItem> {
        if (config.isEmpty()) {
            // Default order: system → feeder → third-party (by name)
            return items.sortedWith(
                compareBy<SelectionMenuItem> { it.type }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
            )
        }

        // Sort by config.order position
        val orderMap = config.order.mapIndexed { index, id ->
            id to index
        }.toMap()

        return items.sortedBy { item ->
            orderMap[item.id] ?: Int.MAX_VALUE
        }
    }

    // ========================================================================
    // Menu Building Methods
    // ========================================================================

    /**
     * Assign a unique ID to a menu item based on its type.
     * - System items: 0-3 (existing MenuItemOption IDs)
     * - Third-party items: 100-199 (existing range)
     * - Feeder items: 200-299 (new range)
     */
    private fun assignItemId(item: SelectionMenuItem, index: Int): Int {
        return when (item.type) {
            MenuType.SYSTEM -> {
                // Map to existing MenuItemOption IDs
                mapToMenuItemOption(item.id)?.id ?: index
            }
            MenuType.THIRD_PARTY -> 100 + index
            MenuType.APPLICATION -> 200 + index
        }
    }

    /**
     * Map system item IDs to MenuItemOption.
     * Returns null for non-system items.
     */
    private fun mapToMenuItemOption(itemId: String): MenuItemOption? {
        return when (itemId) {
            "android.intent.action.COPY" -> MenuItemOption.Copy
            "android.intent.action.PASTE" -> MenuItemOption.Paste
            "android.intent.action.CUT" -> MenuItemOption.Cut
            "android.intent.action.SELECT_ALL" -> MenuItemOption.SelectAll
            else -> null
        }
    }

    /**
     * Add a menu item from the configuration.
     * Handles system, Feeder, and third-party items differently.
     */
    private fun addMenuItemFromConfig(
        menu: Menu?,
        item: SelectionMenuItem,
        index: Int,
    ) {
        when (item.type) {
            MenuType.SYSTEM -> {
                // System items: use MenuItemOption if callback exists
                val option = mapToMenuItemOption(item.id)
                if (option != null && hasCallback(option)) {
                    menu?.add(0, option.id, index, item.name)
                        ?.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                }
            }
            MenuType.APPLICATION -> {
                // Feeder items: store in feederItems map
                val itemId = 200 + index
                menu?.add(2, itemId, index, item.name)
                    ?.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
                feederItems[itemId] = item
            }
            MenuType.THIRD_PARTY -> {
                // Third-party items: store in textProcessors list
                val itemId = 100 + index
                if (menu?.findItem(itemId) == null) {
                    menu?.add(1, itemId, index, item.name)
                        ?.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
                }
                item.componentName?.let { cn ->
                    textProcessors.add(cn)
                }
            }
        }
    }

    /**
     * Check if a system item has a callback registered.
     */
    private fun hasCallback(option: MenuItemOption): Boolean {
        return when (option) {
            MenuItemOption.Copy -> onCopyRequested != null
            MenuItemOption.Paste -> onPasteRequested != null
            MenuItemOption.Cut -> onCutRequested != null
            MenuItemOption.SelectAll -> onSelectAllRequested != null
        }
    }

    // ========================================================================
    // Click Handling Methods
    // ========================================================================

    /**
     * Handle third-party app clicks.
     * Launches the app with ACTION_PROCESS_TEXT intent.
     */
    private fun handleThirdPartyClick(itemId: Int) {
        // Since we can't access the selected text - hack it by using the clipboard
        val prevClip = clipboardManager.primaryClip
        onCopyRequested?.invoke()

        val clip = clipboardManager.primaryClip
        if (clip != null && clip.itemCount > 0) {
            textProcessors.getOrNull(itemId - 100)?.let { cn ->
                activityLauncher.startActivity(
                    openAdjacentIfSuitable = true,
                    intent = Intent(Intent.ACTION_PROCESS_TEXT).apply {
                        type = "text/plain"
                        component = cn
                        putExtra(Intent.EXTRA_PROCESS_TEXT, clip.getItemAt(0).text)
                    },
                )
            }
        }

        try {
            prevClip?.let { clipboardManager.setPrimaryClip(it) }
        } catch (e: Exception) {
            // This can crash if the content contains a fileUri for example
            // android.os.FileUriExposedException: file:/// exposed beyond app through ClipData.Item.getUri()
            Log.e(LOG_TAG, "Resetting clipboard failed", e)
        }
    }

    /**
     * Handle Feeder application item clicks.
     * Currently shows placeholder logs for read_aloud and translate.
     */
    private fun handleFeederItemClick(itemId: Int) {
        val feederItem = feederItems[itemId] ?: return

        when (feederItem.id) {
            "com.nononsenseapps.feeder.action.READ_ALOUD" -> {
                // TODO: Trigger read aloud functionality
                Log.d(LOG_TAG, "Read Aloud clicked (placeholder)")
            }
            "com.nononsenseapps.feeder.action.TRANSLATE" -> {
                // TODO: Trigger translate functionality
                Log.d(LOG_TAG, "Translate clicked (placeholder)")
            }
        }
    }
}

internal enum class MenuItemOption(
    val id: Int,
) {
    Copy(0),
    Paste(1),
    Cut(2),
    SelectAll(3),
    ;

    val titleResource: Int
        get() =
            when (this) {
                Copy -> android.R.string.copy
                Paste -> android.R.string.paste
                Cut -> android.R.string.cut
                SelectAll -> android.R.string.selectAll
            }

    /**
     * This item will be shown before all items that have order greater than this value.
     */
    val order = id
}

internal class FloatingTextActionModeCallback(
    private val callback: FeederTextActionModeCallback,
) : ActionMode.Callback2() {
    override fun onActionItemClicked(
        mode: ActionMode?,
        item: MenuItem?,
    ): Boolean = callback.onActionItemClicked(mode, item)

    override fun onCreateActionMode(
        mode: ActionMode?,
        menu: Menu?,
    ): Boolean = callback.onCreateActionMode(mode, menu)

    override fun onPrepareActionMode(
        mode: ActionMode?,
        menu: Menu?,
    ): Boolean = callback.onPrepareActionMode(mode, menu)

    override fun onDestroyActionMode(mode: ActionMode?) {
        callback.onDestroyActionMode(mode)
    }

    override fun onGetContentRect(
        mode: ActionMode?,
        view: View?,
        outRect: android.graphics.Rect?,
    ) {
        val rect = callback.rect
        outRect?.set(
            rect.left.toInt(),
            rect.top.toInt(),
            rect.right.toInt(),
            rect.bottom.toInt(),
        )
    }
}
