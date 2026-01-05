package com.nononsenseapps.feeder.ui.compose.utils

import android.content.SharedPreferences
import com.nononsenseapps.feeder.ui.compose.settings.MenuConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Store for menu configuration loaded from SharedPreferences.
 *
 * Provides access to the user's menu item configuration (order and visibility)
 * stored in SharedPreferences as JSON. Caches configuration in memory for
 * fast access and provides reactive updates via StateFlow.
 */
interface MenuConfigStore {
    /**
     * Get the current menu configuration.
     *
     * @return Current MenuConfig (cached in memory)
     */
    fun getConfig(): MenuConfig

    /**
     * Get a StateFlow that emits menu configuration updates.
     *
     * @return StateFlow of MenuConfig
     */
    fun getConfigFlow(): StateFlow<MenuConfig>
}

/**
 * Implementation of MenuConfigStore that loads from SharedPreferences.
 *
 * @property sharedPreferences SharedPreferences instance to load config from
 */
class MenuConfigStoreImpl(
    private val sharedPreferences: SharedPreferences,
) : MenuConfigStore {
    private val _configFlow = MutableStateFlow(loadConfig())

    // Cache config in memory for fast access
    private var cachedConfig: MenuConfig = _configFlow.value

    override fun getConfig(): MenuConfig {
        return cachedConfig
    }

    override fun getConfigFlow(): StateFlow<MenuConfig> {
        return _configFlow.asStateFlow()
    }

    /**
     * Load menu configuration from SharedPreferences.
     *
     * @return Loaded MenuConfig or Default if loading fails
     */
    private fun loadConfig(): MenuConfig {
        return try {
            val jsonString = sharedPreferences.getString(PREF_MENU_CONFIG, null)
            if (jsonString != null) {
                MenuConfig.fromJson(jsonString)
            } else {
                MenuConfig.Default
            }
        } catch (e: Exception) {
            // Return default config on any error
            MenuConfig.Default
        }
    }

    companion object {
        private const val PREF_MENU_CONFIG = "menu_config"
    }
}
