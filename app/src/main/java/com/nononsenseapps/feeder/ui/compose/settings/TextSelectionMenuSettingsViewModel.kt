package com.nononsenseapps.feeder.ui.compose.settings

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.nononsenseapps.feeder.archmodel.MenuItemConfig
import com.nononsenseapps.feeder.archmodel.MenuItemType
import com.nononsenseapps.feeder.archmodel.Repository
import com.nononsenseapps.feeder.archmodel.TextMenuConfig
import com.nononsenseapps.feeder.base.DIAwareViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.kodein.di.DI
import org.kodein.di.instance

class TextSelectionMenuSettingsViewModel(
    di: DI,
) : DIAwareViewModel(di) {
    private val repository: Repository by instance()

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

    fun moveItemUp(index: Int) {
        if (index <= 0) return

        val currentConfig = repository.textMenuConfig.value
        val items = currentConfig.items.toMutableList()
        val temp = items[index]
        items[index] = items[index - 1]
        items[index - 1] = temp
        repository.setTextMenuConfig(TextMenuConfig(items))
    }

    fun moveItemDown(index: Int) {
        val currentConfig = repository.textMenuConfig.value
        if (index >= currentConfig.items.size - 1) return

        val items = currentConfig.items.toMutableList()
        val temp = items[index]
        items[index] = items[index + 1]
        items[index + 1] = temp
        repository.setTextMenuConfig(TextMenuConfig(items))
    }

    fun moveItem(
        fromIndex: Int,
        toIndex: Int,
    ) {
        val currentConfig = repository.textMenuConfig.value
        val items = currentConfig.items.toMutableList()
        val item = items.removeAt(fromIndex)
        items.add(toIndex, item)
        repository.setTextMenuConfig(TextMenuConfig(items))
    }

    fun resetToDefaults() {
        repository.setTextMenuConfig(TextMenuConfig())
    }
}

@Immutable
data class TextSelectionMenuSettingsViewState(
    val menuItems: List<MenuItemConfig> = TextMenuConfig.defaultMenuItems,
)
