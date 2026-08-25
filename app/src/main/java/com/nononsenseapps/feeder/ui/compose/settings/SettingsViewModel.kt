package com.nononsenseapps.feeder.ui.compose.settings

import android.app.Application
import android.os.PowerManager
import androidx.compose.runtime.Immutable
import androidx.core.content.getSystemService
import androidx.lifecycle.viewModelScope
import com.nononsenseapps.feeder.ApplicationCoroutineScope
import com.nononsenseapps.feeder.ai.AIApi
import com.nononsenseapps.feeder.ai.model.AISettings
import com.nononsenseapps.feeder.ai.model.SummaryLanguage
import com.nononsenseapps.feeder.archmodel.DarkThemePreferences
import com.nononsenseapps.feeder.archmodel.FeedItemStyle
import com.nononsenseapps.feeder.archmodel.ItemOpener
import com.nononsenseapps.feeder.archmodel.LinkOpener
import com.nononsenseapps.feeder.archmodel.Repository
import com.nononsenseapps.feeder.archmodel.SortingOptions
import com.nononsenseapps.feeder.archmodel.SwipeAsRead
import com.nononsenseapps.feeder.archmodel.SyncFrequency
import com.nononsenseapps.feeder.archmodel.ThemeOptions
import com.nononsenseapps.feeder.base.DIAwareViewModel
import com.nononsenseapps.feeder.ui.compose.settings.AISettingsEvent
import com.nononsenseapps.feeder.ui.compose.settings.AISettingsState
import com.nononsenseapps.feeder.ui.compose.settings.FontSelection
import com.nononsenseapps.feeder.ui.compose.settings.FontSelection.SystemDefault
import com.nononsenseapps.feeder.ui.compose.settings.ModelsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.instance

class SettingsViewModel(
    di: DI,
) : DIAwareViewModel(di) {
    private val repository: Repository by instance()
    private val context: Application by instance()
    private val applicationCoroutineScope: ApplicationCoroutineScope by instance()
    private val aiApi: AIApi by instance()

    fun setCurrentTheme(value: ThemeOptions) {
        repository.setCurrentTheme(value)
    }

    fun setPreferredDarkTheme(value: DarkThemePreferences) {
        repository.setPreferredDarkTheme(value)
    }

    fun setCurrentSorting(value: SortingOptions) {
        repository.setCurrentSorting(value)
    }

    fun setShowFab(value: Boolean) {
        repository.setShowFab(value)
    }

    fun setSyncOnResume(value: Boolean) {
        repository.setSyncOnResume(value)
    }

    fun setSyncOnlyOnWifi(value: Boolean) =
        applicationCoroutineScope.launch {
            repository.setSyncOnlyOnWifi(value)
        }

    fun setSyncOnlyWhenCharging(value: Boolean) =
        applicationCoroutineScope.launch {
            repository.setSyncOnlyWhenCharging(value)
        }

    fun setAutoFetchFullArticle(value: Boolean) =
        applicationCoroutineScope.launch {
            repository.setAutoFetchFullArticle(value)
        }

    fun setLoadImageOnlyOnWifi(value: Boolean) {
        repository.setLoadImageOnlyOnWifi(value)
    }

    fun setShowThumbnails(value: Boolean) {
        repository.setShowThumbnails(value)
    }

    fun setUseDetectLanguage(value: Boolean) {
        repository.setUseDetectLanguage(value)
    }

    fun setUseDynamicTheme(value: Boolean) {
        repository.setUseDynamicTheme(value)
    }

    fun setMaxCountPerFeed(value: Int) {
        repository.setMaxCountPerFeed(value)
    }

    fun setItemOpener(value: ItemOpener) {
        repository.setItemOpener(value)
    }

    fun setLinkOpener(value: LinkOpener) {
        repository.setLinkOpener(value)
    }

    fun setUseInAppAudioPlayer(value: Boolean) {
        repository.setUseInAppAudioPlayer(value)
    }

    fun setSyncFrequency(value: SyncFrequency) =
        applicationCoroutineScope.launch {
            repository.setSyncFrequency(value)
        }

    fun setFeedItemStyle(value: FeedItemStyle) {
        repository.setFeedItemStyle(value)
    }

    fun addToBlockList(value: String) =
        applicationCoroutineScope.launch {
            repository.addBlocklistPattern(value)
        }

    fun removeFromBlockList(value: String) =
        applicationCoroutineScope.launch {
            repository.removeBlocklistPattern(value)
        }

    fun setApplyBlocklistToSummaries(value: Boolean) =
        applicationCoroutineScope.launch {
            repository.setApplyBlocklistToSummaries(value)
        }

    fun setApplyBlocklistToLinks(value: Boolean) =
        applicationCoroutineScope.launch {
            repository.setApplyBlocklistToLinks(value)
        }

    fun toggleNotifications(
        feedId: Long,
        value: Boolean,
    ) = applicationCoroutineScope.launch {
        repository.toggleNotifications(feedId, value)
    }

    fun setSwipeAsRead(value: SwipeAsRead) {
        repository.setSwipeAsRead(value)
    }

    fun setIsMarkAsReadOnScroll(value: Boolean) {
        repository.setIsMarkAsReadOnScroll(value)
    }

    fun setMaxLines(value: Int) {
        repository.setMaxLines(value)
    }

    fun setShowOnlyTitles(value: Boolean) {
        repository.setShowOnlyTitles(value)
    }

    fun setIsOpenAdjacent(value: Boolean) {
        repository.setOpenAdjacent(value)
    }

    fun setShowReadingTime(value: Boolean) {
        repository.setShowReadingTime(value)
    }

    fun setShowTitleUnreadCount(value: Boolean) {
        repository.setShowTitleUnreadCount(value)
    }

    fun setOpenDrawerOnFab(value: Boolean) {
        repository.setOpenDrawerOnFab(value)
    }

    fun setIsPagingMode(value: Boolean) {
        repository.setIsPagingMode(value)
    }

    fun setForceSingleColumn(value: Boolean) {
        repository.setForceSingleColumn(value)
    }

    fun setIsAnimatedPaging(value: Boolean) {
        repository.setIsAnimatedPaging(value)
    }

    fun onOpenAISettingsEvent(event: AISettingsEvent) {
        when (event) {
            is AISettingsEvent.LoadModels -> loadOpenAIModels(event.settings)
            is AISettingsEvent.UpdateSettings ->
                when (event.settings) {
                    is com.nononsenseapps.feeder.ai.model.AISettings.OpenAI -> {
                        repository.setAIProviderType(com.nononsenseapps.feeder.ai.provider.AIProvider.OPENAI_COMPATIBLE)
                        repository.setOpenAISettings(event.settings.openaiSettings)
                    }
                    is com.nononsenseapps.feeder.ai.model.AISettings.Anthropic -> {
                        repository.setAIProviderType(com.nononsenseapps.feeder.ai.provider.AIProvider.ANTHROPIC)
                        repository.setAnthropicSettings(event.settings.anthropicSettings)
                    }
                }
            is AISettingsEvent.SwitchEditMode -> {
                val current = _viewState.value.openAIState
                _viewState.value = _viewState.value.copy(openAIState = current.copy(isEditMode = event.enabled))
            }
            is AISettingsEvent.ShowModelsError -> {
                val current = _viewState.value.openAIState
                _viewState.value = _viewState.value.copy(openAIState = current.copy(showModelsError = event.show))
            }
            is AISettingsEvent.UpdateSummaryLanguage -> {
                repository.setSummaryLanguage(event.language)
                val current = _viewState.value.openAIState
                _viewState.value = _viewState.value.copy(openAIState = current.copy(summaryLanguage = event.language))
            }
        }
    }

    private val openAIModelsState = MutableStateFlow<ModelsState>(ModelsState.None)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val immutableFeedsSettings =
        repository.feedNotificationSettings
            .mapLatest { values ->
                values.map {
                    UIFeedSettings(
                        feedId = it.id,
                        title = it.title,
                        notify = it.notify,
                    )
                }
            }

    private val batteryOptimizationIgnoredFlow: Flow<Boolean> =
        repository.resumeTime
            .map {
                val powerManager: PowerManager? = context.getSystemService()
                powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
            }.buffer(1)

    private val _viewState = MutableStateFlow(SettingsViewState())
    val viewState: StateFlow<SettingsViewState>
        get() = _viewState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.currentTheme,
                repository.preferredDarkTheme,
                repository.currentSorting,
                repository.showFab,
                repository.syncOnResume,
                repository.syncOnlyOnWifi,
                repository.syncOnlyWhenCharging,
                repository.autoFetchFullArticle,
                repository.loadImageOnlyOnWifi,
                repository.showThumbnails,
                repository.maximumCountPerFeed,
                repository.itemOpener,
                repository.linkOpener,
                repository.syncFrequency,
                batteryOptimizationIgnoredFlow,
                repository.feedItemStyle,
                repository.swipeAsRead,
                repository.blockList,
                repository.applyBlocklistToSummaries,
                repository.useDetectLanguage,
                repository.useDynamicTheme,
                immutableFeedsSettings,
                repository.isMarkAsReadOnScroll,
                repository.maxLines,
                repository.showOnlyTitle,
                repository.isOpenAdjacent,
                repository.showReadingTime,
                repository.showTitleUnreadCount,
                repository.aiSettingsFlow,
                repository.summaryLanguage,
                openAIModelsState,
                repository.isOpenDrawerOnFab,
                repository.font,
                repository.isPagingMode,
                repository.isAnimatedPaging,
                repository.useInAppAudioPlayer,
                repository.forceSingleColumn,
                repository.applyBlocklistToLinks,
            ) { params: Array<Any> ->
                @Suppress("UNCHECKED_CAST")
                SettingsViewState(
                    currentTheme = params[0] as ThemeOptions,
                    darkThemePreference = params[1] as DarkThemePreferences,
                    currentSorting = params[2] as SortingOptions,
                    showFab = params[3] as Boolean,
                    syncOnResume = params[4] as Boolean,
                    syncOnlyOnWifi = params[5] as Boolean,
                    syncOnlyWhenCharging = params[6] as Boolean,
                    autoFetchFullArticle = params[7] as Boolean,
                    loadImageOnlyOnWifi = params[8] as Boolean,
                    showThumbnails = params[9] as Boolean,
                    maximumCountPerFeed = params[10] as Int,
                    itemOpener = params[11] as ItemOpener,
                    linkOpener = params[12] as LinkOpener,
                    syncFrequency = params[13] as SyncFrequency,
                    batteryOptimizationIgnored = params[14] as Boolean,
                    feedItemStyle = params[15] as FeedItemStyle,
                    swipeAsRead = params[16] as SwipeAsRead,
                    blockList = params[17] as List<String>,
                    applyBlocklistToSummaries = params[18] as Boolean,
                    useDetectLanguage = params[19] as Boolean,
                    useDynamicTheme = params[20] as Boolean,
                    feedsSettings = params[21] as List<UIFeedSettings>,
                    isMarkAsReadOnScroll = params[22] as Boolean,
                    maxLines = params[23] as Int,
                    showOnlyTitle = params[24] as Boolean,
                    isOpenAdjacent = params[25] as Boolean,
                    showReadingTime = params[26] as Boolean,
                    showTitleUnreadCount = params[27] as Boolean,
                    openAIState =
                        _viewState.value.openAIState.copy(
                            settings = params[28] as AISettings,
                            summaryLanguage = params[29] as SummaryLanguage,
                            modelsResult = params[30] as ModelsState,
                        ),
                    isOpenDrawerOnFab = params[31] as Boolean,
                    font = params[32] as FontSelection,
                    isPagingMode = params[33] as Boolean,
                    isAnimatedPaging = params[34] as Boolean,
                    useInAppAudioPlayer = params[35] as Boolean,
                    forceSingleColumn = params[36] as Boolean,
                    applyBlocklistToLinks = params[37] as Boolean,
                )
            }.collect {
                _viewState.value = it
            }
        }
    }

    private fun loadOpenAIModels(settings: AISettings) {
        viewModelScope.launch(Dispatchers.IO) {
            openAIModelsState.value = ModelsState.Loading
            openAIModelsState.value =
                try {
                    val result = aiApi.listModelIds(settings)
                    when (result) {
                        is com.nononsenseapps.feeder.ai.AIClient.ModelsResult.Success -> ModelsState.Success(result.ids)
                        is com.nononsenseapps.feeder.ai.AIClient.ModelsResult.Error -> ModelsState.Error(result.message ?: "Unknown error")
                        com.nononsenseapps.feeder.ai.AIClient.ModelsResult.MissingToken -> ModelsState.Error("Missing API key")
                        com.nononsenseapps.feeder.ai.AIClient.ModelsResult.AzureApiVersionRequired -> ModelsState.Error("Azure API version is required")
                        com.nononsenseapps.feeder.ai.AIClient.ModelsResult.AzureDeploymentIdRequired -> ModelsState.Error("Azure deployment ID is required")
                    }
                } catch (e: Exception) {
                    ModelsState.Error(e.message ?: "Unknown error")
                }
        }
    }

    companion object {
        @Suppress("unused")
        private const val LOG_TAG = "FEEDER_SETTINGSVM"
    }
}

@Immutable
data class SettingsViewState(
    val currentTheme: ThemeOptions = ThemeOptions.SYSTEM,
    val darkThemePreference: DarkThemePreferences = DarkThemePreferences.BLACK,
    val currentSorting: SortingOptions = SortingOptions.NEWEST_FIRST,
    val showFab: Boolean = true,
    val feedItemStyle: FeedItemStyle = FeedItemStyle.CARD,
    val blockList: List<String> = emptyList(),
    val applyBlocklistToSummaries: Boolean = false,
    val syncOnResume: Boolean = false,
    val syncOnlyOnWifi: Boolean = false,
    val syncOnlyWhenCharging: Boolean = false,
    val autoFetchFullArticle: Boolean = false,
    val loadImageOnlyOnWifi: Boolean = false,
    val showThumbnails: Boolean = false,
    val maximumCountPerFeed: Int = 100,
    val itemOpener: ItemOpener = ItemOpener.READER,
    val linkOpener: LinkOpener = LinkOpener.CUSTOM_TAB,
    val syncFrequency: SyncFrequency = SyncFrequency.EVERY_1_HOURS,
    val batteryOptimizationIgnored: Boolean = false,
    val swipeAsRead: SwipeAsRead = SwipeAsRead.ONLY_FROM_END,
    val useDetectLanguage: Boolean = true,
    val useDynamicTheme: Boolean = true,
    val feedsSettings: List<UIFeedSettings> = emptyList(),
    val isMarkAsReadOnScroll: Boolean = false,
    val maxLines: Int = 2,
    val showOnlyTitle: Boolean = false,
    val isOpenAdjacent: Boolean = true,
    val openAIState: AISettingsState = AISettingsState(),
    val showReadingTime: Boolean = false,
    val showTitleUnreadCount: Boolean = false,
    val isOpenDrawerOnFab: Boolean = false,
    val font: FontSelection = SystemDefault,
    val isPagingMode: Boolean = false,
    val isAnimatedPaging: Boolean = false,
    val useInAppAudioPlayer: Boolean = true,
    val forceSingleColumn: Boolean = false,
    val applyBlocklistToLinks: Boolean = false,
)

data class UIFeedSettings(
    val feedId: Long,
    val title: String,
    val notify: Boolean,
)
