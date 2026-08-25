package com.nononsenseapps.feeder.ui.compose.settings

import androidx.lifecycle.viewModelScope
import com.nononsenseapps.feeder.ai.model.AISettings
import com.nononsenseapps.feeder.ai.model.TranslationLanguage
import com.nononsenseapps.feeder.archmodel.Repository
import com.nononsenseapps.feeder.base.DIAwareViewModel
import com.nononsenseapps.feeder.localtranslation.BergamotModelManager
import com.nononsenseapps.feeder.localtranslation.LanguagePairInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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

    private val bergamotModelManager: BergamotModelManager by instance()

    private val mutableDownloadedLanguagePairs = MutableStateFlow<List<LanguagePairInfo>>(emptyList())
    val downloadedLanguagePairs: StateFlow<List<LanguagePairInfo>> = mutableDownloadedLanguagePairs.asStateFlow()

    val isOnDeviceProvider: StateFlow<Boolean> =
        repository.aiSettingsFlow
            .map { it is AISettings.OnDevice }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        refreshDownloadedLanguagePairs()
    }

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

    fun deleteLanguagePair(pair: LanguagePairInfo) {
        viewModelScope.launch {
            bergamotModelManager.deleteLanguagePair(pair.sourceLanguage, pair.targetLanguage)
            refreshDownloadedLanguagePairs()
        }
    }

    private fun refreshDownloadedLanguagePairs() {
        viewModelScope.launch {
            mutableDownloadedLanguagePairs.value = bergamotModelManager.getDownloadedLanguagePairs()
        }
    }
}
