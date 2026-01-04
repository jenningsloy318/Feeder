package com.nononsenseapps.feeder.di

import com.nononsenseapps.feeder.archmodel.AndroidSystemStore
import com.nononsenseapps.feeder.ui.compose.settings.MenuDiscoveryService
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.singleton

val androidModule =
    DI.Module(name = "android module") {
        bind<AndroidSystemStore>() with singleton { AndroidSystemStore(di) }
        bind<MenuDiscoveryService>() with singleton { MenuDiscoveryService(di) }
    }
