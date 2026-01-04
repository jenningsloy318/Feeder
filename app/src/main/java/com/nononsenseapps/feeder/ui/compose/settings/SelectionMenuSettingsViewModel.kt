package com.nononsenseapps.feeder.ui.compose.settings

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.nononsenseapps.feeder.base.DIAwareViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.kodein.di.DI

/**
 * ViewModel for the Selection Menu Configuration screen.
 *
 * Manages state for:
 * - Loading menu items
 * - Displaying menu list
 * - Handling user actions
 */
class SelectionMenuSettingsViewModel(
    di: DI,
) : DIAwareViewModel(di) {
    private val _viewState = MutableStateFlow(SelectionMenuViewState())
    val viewState: StateFlow<SelectionMenuViewState> = _viewState.asStateFlow()

    fun onEvent(event: SelectionMenuEvent) {
        when (event) {
            is SelectionMenuEvent.LoadMenus -> {
                // TODO: Implement in Phase 2 - Load menus from repository
            }
            is SelectionMenuEvent.AddMenu -> {
                // TODO: Implement in Phase 2 - Add menu to repository
            }
            is SelectionMenuEvent.RemoveMenu -> {
                // TODO: Implement in Phase 2 - Remove menu from repository
            }
        }
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
     * Event to add a new menu item.
     * @property item The menu item to add
     */
    data class AddMenu(val item: SelectionMenuItem) : SelectionMenuEvent()

    /**
     * Event to remove a menu item.
     * @property id The ID of the menu item to remove
     */
    data class RemoveMenu(val id: String) : SelectionMenuEvent()
}
