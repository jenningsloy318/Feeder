package com.nononsenseapps.feeder.ui.compose.settings

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.nononsenseapps.feeder.archmodel.MenuItemConfig
import com.nononsenseapps.feeder.archmodel.MenuItemType
import com.nononsenseapps.feeder.archmodel.Repository
import com.nononsenseapps.feeder.archmodel.TextMenuConfig
import com.nononsenseapps.feeder.base.DIAwareViewModel
import com.nononsenseapps.feeder.model.TextProcessorDiscoveryService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.instance

class TextSelectionMenuSettingsViewModel(
    di: DI,
) : DIAwareViewModel(di) {
    private val repository: Repository by instance()
    private val appContext: Application by instance()
    private val discoveryService = TextProcessorDiscoveryService(appContext)

    val viewState: StateFlow<TextSelectionMenuSettingsViewState> =
        repository.textMenuConfig
            .map { config ->
                TextSelectionMenuSettingsViewState(
                    menuItems = config.items,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = TextSelectionMenuSettingsViewState(),
            )

    init {
        // Discover text processors and share handlers on initialization
        discoverAndMergeTextProcessors()
    }

    private fun discoverAndMergeTextProcessors() {
        viewModelScope.launch {
            try {
                val discoveredApps = discoveryService.discoverAllTextProcessors()
                val discoveredConfigs = discoveryService.discoveredAppsToMenuConfigs(discoveredApps)

                val currentConfig = repository.textMenuConfig.value
                val currentItems = currentConfig.items.toMutableList()

                // Merge discovered items with existing configuration
                for (discoveredConfig in discoveredConfigs) {
                    val existingIndex = currentItems.indexOfFirst { existing ->
                        existing.specificApp?.packageName == discoveredConfig.specificApp?.packageName
                    }

                    if (existingIndex == -1) {
                        // New discovered app, add to end
                        currentItems.add(discoveredConfig)
                    }
                    // If it exists, keep the existing configuration (enabled state, order)
                }

                // Update the configuration if we found new items
                if (currentItems.size > currentConfig.items.size) {
                    repository.setTextMenuConfig(TextMenuConfig(currentItems))
                }
            } catch (e: Exception) {
                // Log error but don't crash the app
                e.printStackTrace()
            }
        }
    }

    fun toggleItemEnabled(type: MenuItemType) {
        val currentConfig = repository.textMenuConfig.value
        val updatedItems =
            currentConfig.items.map { item ->
                if (item.type == type) {
                    item.copy(enabled = !item.enabled)
                } else {
                    item
                }
            }
        repository.setTextMenuConfig(TextMenuConfig(updatedItems))
    }

    fun toggleSpecificAppEnabled(packageName: String) {
        val currentConfig = repository.textMenuConfig.value
        val updatedItems =
            currentConfig.items.map { item ->
                if (item.specificApp?.packageName == packageName) {
                    item.copy(enabled = !item.enabled)
                } else {
                    item
                }
            }
        repository.setTextMenuConfig(TextMenuConfig(updatedItems))
    }

    fun moveItemUp(index: Int) {
        if (index <= 0) return

        val currentConfig = repository.textMenuConfig.value
        val items = currentConfig.items.toMutableList()
        val temp = items[index]
        items[index] = items[index - 1]
        items[index - 1] = temp

        // Update order values
        val updatedItems = items.mapIndexed { i, item ->
            item.copy(order = i)
        }
        repository.setTextMenuConfig(TextMenuConfig(updatedItems))
    }

    fun moveItemDown(index: Int) {
        val currentConfig = repository.textMenuConfig.value
        if (index >= currentConfig.items.size - 1) return

        val items = currentConfig.items.toMutableList()
        val temp = items[index]
        items[index] = items[index + 1]
        items[index + 1] = temp

        // Update order values
        val updatedItems = items.mapIndexed { i, item ->
            item.copy(order = i)
        }
        repository.setTextMenuConfig(TextMenuConfig(updatedItems))
    }

    fun moveItem(
        fromIndex: Int,
        toIndex: Int,
    ) {
        val currentConfig = repository.textMenuConfig.value
        val items = currentConfig.items.toMutableList()
        val item = items.removeAt(fromIndex)
        items.add(toIndex, item)

        // Update order values
        val updatedItems = items.mapIndexed { i, item ->
            item.copy(order = i)
        }
        repository.setTextMenuConfig(TextMenuConfig(updatedItems))
    }

    fun resetToDefaults() {
        repository.setTextMenuConfig(TextMenuConfig())
        // Re-discover apps after reset
        discoverAndMergeTextProcessors()
    }
}

@Immutable
data class TextSelectionMenuSettingsViewState(
    val menuItems: List<MenuItemConfig> = TextMenuConfig.defaultMenuItems,
)
