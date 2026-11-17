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
 *
 * Registers [ComponentCallbacks] on the given [context] when created.
 * Call [release] when the owner is no longer needed, otherwise the callbacks
 * (and this owner) stay registered for the lifetime of the context.
 */
class NavigationConfigOwner(private val context: Context) {

    private val appConfigFlow = MutableStateFlow(context.resources.configuration)

    private val componentCallbacks = object : ComponentCallbacks {

        override fun onConfigurationChanged(newConfig: Configuration) {
            appConfigFlow.value = newConfig
        }

        override fun onLowMemory() = Unit
    }

    init {
        context.registerComponentCallbacks(componentCallbacks)
    }

    /**
     * Unregisters the configuration callbacks from the context.
     * The flows returned by [config] and [language] stop receiving updates afterwards.
     */
    fun release() {
        context.unregisterComponentCallbacks(componentCallbacks)
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
