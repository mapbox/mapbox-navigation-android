package com.mapbox.navigation.ui.utils.internal.configuration

import android.content.ComponentCallbacks
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.util.Locale

/**
 * App configuration changes observer.
 */
class NavigationConfigOwner(context: Context) {

    private lateinit var appConfigFlow: MutableStateFlow<Configuration>

    private val componentCallbacks = object : ComponentCallbacks {

        override fun onConfigurationChanged(newConfig: Configuration) {
            appConfigFlow.value = newConfig
        }

        override fun onLowMemory() = Unit
    }

    init {
        appConfigFlow = MutableStateFlow(context.resources.configuration)
        context.registerComponentCallbacks(componentCallbacks)
    }

    fun config(): Flow<Configuration> = appConfigFlow

    fun language(): Flow<String> {
        return config()
            .map { toLocale(it).language }
            .distinctUntilChanged()
    }

    fun toLocale(configuration: Configuration): Locale {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.locales.get(0)
        } else {
            configuration.locale
        }
    }
}
