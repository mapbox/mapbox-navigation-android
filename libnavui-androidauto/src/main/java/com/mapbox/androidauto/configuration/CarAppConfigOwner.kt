package com.mapbox.androidauto.configuration

import android.app.Application
import android.content.ComponentCallbacks
import android.content.res.Configuration
import android.os.Build
import com.mapbox.androidauto.MapboxCarApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.util.Locale

/**
 * This is a service provided by [MapboxCarApp].
 * The configuration changes in the car or the app and this provides
 * a mechanism to monitor the configuration changes.
 */
class CarAppConfigOwner internal constructor() {

    private lateinit var carAppConfigFlow: MutableStateFlow<Configuration>

    private val componentCallbacks = object : ComponentCallbacks {

        override fun onConfigurationChanged(newConfig: Configuration) {
            carAppConfigFlow.value = newConfig
        }

        override fun onLowMemory() {
            // noop
        }
    }

    internal fun setup(application: Application) {
        carAppConfigFlow = MutableStateFlow(application.resources.configuration)
        application.registerComponentCallbacks(componentCallbacks)
    }

    fun config(): Flow<Configuration> = carAppConfigFlow

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
