package com.nononsenseapps.feeder.ui.compose.settings

import androidx.lifecycle.viewModelScope
import com.nononsenseapps.feeder.ai.model.SummaryLanguage
import com.nononsenseapps.feeder.archmodel.Repository
import com.nononsenseapps.feeder.base.DIAwareViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.instance

/**
 * ViewModel for the Summary Settings screen.
 *
 * Manages the state for:
 * - Whether summaries are enabled
 * - The language for summaries
 * - The timeout for summary generation
 */
class SummarySettingsViewModel(
    di: DI,
) : DIAwareViewModel(di) {
    private val repository: Repository by instance()
    val summaryEnabled: StateFlow<Boolean> = repository.summaryEnabled
    val summaryLanguage: StateFlow<SummaryLanguage> = repository.summaryLanguage
    val summaryTimeout: StateFlow<Int> = repository.summaryTimeout

    fun setSummaryEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setSummaryEnabled(enabled)
        }
    }

    fun setSummaryLanguage(language: SummaryLanguage) {
        viewModelScope.launch {
            repository.setSummaryLanguage(language)
        }
    }

    fun setSummaryTimeout(timeout: Int) {
        viewModelScope.launch {
            repository.setSummaryTimeout(timeout)
        }
    }
}
