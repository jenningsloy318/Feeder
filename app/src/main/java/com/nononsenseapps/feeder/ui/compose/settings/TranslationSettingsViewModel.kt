package com.nononsenseapps.feeder.ui.compose.settings

import androidx.lifecycle.viewModelScope
import com.nononsenseapps.feeder.ai.model.TranslationLanguage
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
 * - The target translation language
 * - The translation timeout duration
 */
class TranslationSettingsViewModel(
    di: DI,
) : DIAwareViewModel(di) {
    private val repository: Repository by instance()
    val translationEnabled: StateFlow<Boolean> = repository.translationEnabled
    val enableTranslation: StateFlow<Boolean> = repository.enableTranslation
    val translationLanguage: StateFlow<TranslationLanguage> = repository.translationLanguage
    val translationTimeout: StateFlow<Int> = repository.translationTimeout
    val translateArticlePreviewsByDefault: StateFlow<Boolean> = repository.translateArticlePreviewsByDefault
    val translateArticlesByDefault: StateFlow<Boolean> = repository.translateArticlesByDefault

    fun setTranslationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setTranslationEnabled(enabled)
        }
    }

    fun setEnableTranslation(enabled: Boolean) {
        viewModelScope.launch {
            repository.setEnableTranslation(enabled)
        }
    }

    fun setTranslationLanguage(language: TranslationLanguage) {
        viewModelScope.launch {
            repository.setTranslationLanguage(language)
        }
    }

    fun setTranslationTimeout(timeoutSeconds: Int) {
        viewModelScope.launch {
            repository.setTranslationTimeout(timeoutSeconds)
        }
    }

    fun setTranslateArticlePreviewsByDefault(enabled: Boolean) {
        viewModelScope.launch {
            repository.setTranslateArticlePreviewsByDefault(enabled)
        }
    }

    fun setTranslateArticlesByDefault(enabled: Boolean) {
        viewModelScope.launch {
            repository.setTranslateArticlesByDefault(enabled)
        }
    }
}
