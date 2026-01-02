package com.nononsenseapps.feeder.ui.compose.settings

import androidx.lifecycle.viewModelScope
import com.nononsenseapps.feeder.ai.model.TargetLanguage
import com.nononsenseapps.feeder.archmodel.Repository
import com.nononsenseapps.feeder.base.DIAwareViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.instance

/**
 * ViewModel for the Translation Settings screen.
 *
 * Manages the state for:
 * - Whether translation is enabled
 * - The target language for translation
 */
class TranslationSettingsViewModel(
    di: DI,
) : DIAwareViewModel(di) {
    private val repository: Repository by instance()
    val translationEnabled: StateFlow<Boolean> = repository.translationEnabled
    val translationTargetLanguage: StateFlow<TargetLanguage> = repository.translationTargetLanguage

    fun setTranslationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setTranslationEnabled(enabled)
        }
    }

    fun setTranslationTargetLanguage(language: TargetLanguage) {
        viewModelScope.launch {
            repository.setTranslationTargetLanguage(language)
        }
    }
}
