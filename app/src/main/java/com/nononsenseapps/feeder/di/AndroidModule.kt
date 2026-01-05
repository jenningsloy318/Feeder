package com.nononsenseapps.feeder.di

import com.nononsenseapps.feeder.archmodel.AndroidSystemStore
import com.nononsenseapps.feeder.ui.compose.settings.MenuDiscoveryService
import com.nononsenseapps.feeder.ui.compose.utils.MenuConfigStore
import com.nononsenseapps.feeder.ui.compose.utils.MenuConfigStoreImpl
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton

val androidModule =
    DI.Module(name = "android module") {
        bind<AndroidSystemStore>() with singleton { AndroidSystemStore(di) }
        bind<MenuDiscoveryService>() with singleton { MenuDiscoveryService(di) }
        bind<MenuConfigStore>() with singleton { MenuConfigStoreImpl(instance()) }
    }
