package com.nononsenseapps.feeder.ui.compose.settings

import androidx.lifecycle.viewModelScope
import com.nononsenseapps.feeder.ai.model.ProviderConfig
import com.nononsenseapps.feeder.archmodel.Repository
import com.nononsenseapps.feeder.base.DIAwareViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.instance

/**
 * ViewModel for managing the list of AI providers.
 *
 * This ViewModel handles the provider list screen, allowing users to
 * view, activate, and delete providers.
 */
class ProviderListViewModel(
    di: DI,
) : DIAwareViewModel(di) {
    private val repository: Repository by instance()

    /**
     * Stream of all configured providers.
     */
    val providers: StateFlow<List<ProviderConfig>> = repository.providers
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList(),
        )

    /**
     * Activate a provider (set it as the currently active one).
     */
    fun activateProvider(providerId: String) {
        viewModelScope.launch {
            repository.activateProvider(providerId)
        }
    }

    /**
     * Delete a provider from the list.
     */
    fun deleteProvider(providerId: String) {
        viewModelScope.launch {
            repository.deleteProvider(providerId)
        }
    }
}
