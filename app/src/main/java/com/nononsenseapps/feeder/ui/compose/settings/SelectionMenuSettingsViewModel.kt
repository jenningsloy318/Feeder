package com.nononsenseapps.feeder.ui.compose.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.nononsenseapps.feeder.base.DIAwareViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.instance

/**
 * ViewModel for the Selection Menu Configuration screen.
 *
 * Manages state for:
 * - Loading menu items
 * - Displaying menu list
 * - Handling user actions (reorder, toggle visibility)
 * - Persisting configuration
 */
class SelectionMenuSettingsViewModel(
    di: DI,
) : DIAwareViewModel(di) {
    private val menuDiscoveryService: MenuDiscoveryService by instance()
    private val context: Context by instance()
    private val sharedPreferences: SharedPreferences by instance()

    private val _viewState = MutableStateFlow(SelectionMenuViewState())
    val viewState: StateFlow<SelectionMenuViewState> = _viewState.asStateFlow()

    private var saveJob: Job? = null

    init {
        loadMenus()
    }

    fun onEvent(event: SelectionMenuEvent) {
        when (event) {
            is SelectionMenuEvent.LoadMenus -> {
                loadMenus()
            }
            is SelectionMenuEvent.ReorderMenu -> {
                handleReorder(event.fromIndex, event.toIndex)
            }
            is SelectionMenuEvent.ToggleItem -> {
                handleToggle(event.itemId)
            }
        }
    }

    private fun loadMenus() {
        viewModelScope.launch {
            _viewState.value = _viewState.value.copy(isLoading = true, error = null)

            try {
                // Discover all menu items
                val discoveredItems = menuDiscoveryService.discoverAll()

                // Load saved configuration
                val savedConfig = loadMenuConfig()

                // Merge discovered items with saved configuration
                val mergedItems = mergeWithConfig(discoveredItems, savedConfig)

                _viewState.value =
                    _viewState.value.copy(
                        isLoading = false,
                        items = mergedItems,
                    )
            } catch (e: Exception) {
                _viewState.value =
                    _viewState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Unknown error occurred",
                    )
            }
        }
    }

    private fun loadMenuConfig(): MenuConfig =
        try {
            val jsonString = sharedPreferences.getString(PREF_MENU_CONFIG, null)
            if (jsonString != null) {
                MenuConfig.fromJson(jsonString)
            } else {
                MenuConfig.Default
            }
        } catch (e: Exception) {
            MenuConfig.Default
        }

    private fun saveMenuConfig(items: List<SelectionMenuItem>) {
        val config =
            MenuConfig(
                order = items.map { it.id },
                visibility = items.associate { it.id to it.visible },
            )

        try {
            sharedPreferences
                .edit()
                .putString(PREF_MENU_CONFIG, config.toJson())
                .apply()
        } catch (e: Exception) {
            // Log error but don't crash
        }
    }

    private fun saveMenuConfigDebounced(items: List<SelectionMenuItem>) {
        saveJob?.cancel()
        saveJob =
            viewModelScope.launch {
                kotlinx.coroutines.delay(500) // 500ms debounce
                saveMenuConfig(items)
            }
    }

    private fun mergeWithConfig(
        discoveredItems: List<SelectionMenuItem>,
        savedConfig: MenuConfig,
    ): List<SelectionMenuItem> {
        // Create map of discovered items
        val itemsMap = discoveredItems.associateBy { it.id }

        // Determine final order
        val finalOrder =
            if (savedConfig.isEmpty()) {
                // First time: use discovery order
                discoveredItems.mapIndexed { index, item -> item.id to index }
            } else {
                // Use saved order, append new items at the end
                val savedItems = savedConfig.order.mapNotNull { id -> id to itemsMap[id] }
                val newItems = itemsMap.values.filter { it.id !in savedConfig.order }

                savedItems.mapIndexed { index, pair -> pair.first to index } +
                    newItems.mapIndexed { index, item ->
                        item.id to (savedConfig.order.size + index)
                    }
            }

        // Build final list with order and visibility
        return finalOrder.map { (id, order) ->
            val item = itemsMap[id]!!
            item.copy(
                order = order,
                visible = savedConfig.isVisible(id),
            )
        }
    }

    private fun handleReorder(
        fromIndex: Int,
        toIndex: Int,
    ) {
        val currentItems = _viewState.value.items.toMutableList()
        if (fromIndex < 0 ||
            fromIndex >= currentItems.size ||
            toIndex < 0 ||
            toIndex >= currentItems.size
        ) {
            return
        }

        // Move item
        val item = currentItems.removeAt(fromIndex)
        currentItems.add(toIndex, item)

        // Update order property
        val reorderedItems =
            currentItems.mapIndexed { index, menuItem ->
                menuItem.copy(order = index)
            }

        _viewState.value = _viewState.value.copy(items = reorderedItems)
        saveMenuConfigDebounced(reorderedItems)
    }

    private fun handleToggle(itemId: String) {
        val currentItems = _viewState.value.items
        val updatedItems =
            currentItems.map { item ->
                if (item.id == itemId) {
                    item.copy(visible = !item.visible)
                } else {
                    item
                }
            }

        _viewState.value = _viewState.value.copy(items = updatedItems)
        saveMenuConfigDebounced(updatedItems)
    }

    companion object {
        private const val PREF_MENU_CONFIG = "menu_config"
    }
}

/**
 * ViewState for the Selection Menu Configuration screen.
 *
 * @property isLoading Whether menu items are being loaded
 * @property items List of menu items to display
 * @property error Optional error message
 */
@Immutable
data class SelectionMenuViewState(
    val isLoading: Boolean = false,
    val items: List<SelectionMenuItem> = emptyList(),
    val error: String? = null,
)

/**
 * Events that can be triggered in the Selection Menu Configuration screen.
 */
sealed class SelectionMenuEvent {
    /**
     * Event to load menu items.
     */
    data object LoadMenus : SelectionMenuEvent()

    /**
     * Event to reorder menu items.
     * @property fromIndex The current index of the item to move
     * @property toIndex The new index for the item
     */
    data class ReorderMenu(
        val fromIndex: Int,
        val toIndex: Int,
    ) : SelectionMenuEvent()

    /**
     * Event to toggle item visibility.
     * @property itemId The ID of the menu item to toggle
     */
    data class ToggleItem(
        val itemId: String,
    ) : SelectionMenuEvent()
}
