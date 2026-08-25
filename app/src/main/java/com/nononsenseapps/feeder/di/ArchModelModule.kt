package com.nononsenseapps.feeder.di

import android.app.Application
import com.nononsenseapps.feeder.ai.AIApi
import com.nononsenseapps.feeder.archmodel.FeedItemStore
import com.nononsenseapps.feeder.archmodel.FeedStore
import com.nononsenseapps.feeder.archmodel.FontStore
import com.nononsenseapps.feeder.archmodel.Repository
import com.nononsenseapps.feeder.archmodel.SessionStore
import com.nononsenseapps.feeder.archmodel.SettingsStore
import com.nononsenseapps.feeder.archmodel.SyncRemoteStore
import com.nononsenseapps.feeder.base.bindWithActivityViewModelScope
import com.nononsenseapps.feeder.base.bindWithComposableViewModelScope
import com.nononsenseapps.feeder.data.suggestions.SuggestedFeedRepository
import com.nononsenseapps.feeder.localtranslation.BergamotModelManager
import com.nononsenseapps.feeder.localtranslation.BergamotWebTranslator
import com.nononsenseapps.feeder.localtranslation.LocalTranslator
import com.nononsenseapps.feeder.model.OPMLParserHandler
import com.nononsenseapps.feeder.model.TranslationManager
import com.nononsenseapps.feeder.model.opml.OPMLImporter
import com.nononsenseapps.feeder.ui.CommonActivityViewModel
import com.nononsenseapps.feeder.ui.MainActivityViewModel
import com.nononsenseapps.feeder.ui.NavigationDeepLinkViewModel
import com.nononsenseapps.feeder.ui.OpenLinkInDefaultActivityViewModel
import com.nononsenseapps.feeder.ui.compose.editfeed.CreateFeedScreenViewModel
import com.nononsenseapps.feeder.ui.compose.editfeed.EditFeedScreenViewModel
import com.nononsenseapps.feeder.ui.compose.feedarticle.ArticleViewModel
import com.nononsenseapps.feeder.ui.compose.feedarticle.FeedViewModel
import com.nononsenseapps.feeder.ui.compose.searchfeed.SearchFeedViewModel
import com.nononsenseapps.feeder.ui.compose.settings.SettingsViewModel
import com.nononsenseapps.feeder.ui.compose.settings.TextSettingsViewModel
import com.nononsenseapps.feeder.ui.compose.settings.TranslationSettingsViewModel
import com.nononsenseapps.feeder.widget.FeedWidgetSettingsActivityViewModel
import kotlinx.serialization.json.Json
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton
import java.util.Locale

val archModelModule =
    DI.Module(name = "arch models") {
        bind<Repository>() with singleton { Repository(di) }
        bind<SessionStore>() with singleton { SessionStore() }
        bind<SettingsStore>() with singleton { SettingsStore(di) }
        bind<FeedStore>() with singleton { FeedStore(di) }
        bind<FontStore>() with singleton { FontStore(di) }
        bind<FeedItemStore>() with singleton { FeedItemStore(di) }
        bind<SyncRemoteStore>() with singleton { SyncRemoteStore(di) }
        bind<OPMLParserHandler>() with singleton { OPMLImporter(di) }
        bind<TranslationManager>() with singleton { TranslationManager(di) }
        bind<BergamotModelManager>() with singleton { BergamotModelManager(di) }
        bind<BergamotWebTranslator>() with singleton { BergamotWebTranslator(di) }
        bind<LocalTranslator>() with singleton { LocalTranslator(di) }
        // AI API with factory pattern for multiple providers
        bind<AIApi>() with singleton { AIApi(instance(), appLang = Locale.getDefault().getISO3Language()) }
        bind<SuggestedFeedRepository>() with
            singleton {
                SuggestedFeedRepository(
                    resources = instance<Application>().resources,
                    json =
                        Json {
                            ignoreUnknownKeys = true
                        },
                )
            }

        bindWithActivityViewModelScope<MainActivityViewModel>()
        bindWithActivityViewModelScope<FeedWidgetSettingsActivityViewModel>()
        bindWithActivityViewModelScope<OpenLinkInDefaultActivityViewModel>()
        bindWithActivityViewModelScope<CommonActivityViewModel>()

        bindWithComposableViewModelScope<SettingsViewModel>()
        bindWithComposableViewModelScope<EditFeedScreenViewModel>()
        bindWithComposableViewModelScope<CreateFeedScreenViewModel>()
        bindWithComposableViewModelScope<SearchFeedViewModel>()
        bindWithComposableViewModelScope<ArticleViewModel>()
        bindWithComposableViewModelScope<FeedViewModel>()
        bindWithComposableViewModelScope<NavigationDeepLinkViewModel>()
        bindWithComposableViewModelScope<TextSettingsViewModel>()
        bindWithComposableViewModelScope<TranslationSettingsViewModel>()
    }
