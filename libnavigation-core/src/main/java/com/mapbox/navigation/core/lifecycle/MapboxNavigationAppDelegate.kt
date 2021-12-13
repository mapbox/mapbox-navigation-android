package com.mapbox.navigation.core.lifecycle

import android.app.Application
import androidx.lifecycle.LifecycleOwner
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.utils.internal.logI

/**
 * This is a testable version of [MapboxNavigationApp]. Please refer to the singleton
 * for documented functions and expected behaviors.
 */
@ExperimentalPreviewMapboxNavigationAPI
internal class MapboxNavigationAppDelegate {
    private val mapboxNavigationOwner by lazy { MapboxNavigationOwner() }
    private val carAppLifecycleOwner by lazy { CarAppLifecycleOwner() }
    private var isSetup = false

    val lifecycleOwner: LifecycleOwner by lazy { carAppLifecycleOwner }

    fun setup(navigationOptions: NavigationOptions) = apply {
        if (carAppLifecycleOwner.isConfigurationChanging()) {
            return this
        }

        if (isSetup) {
            logI(
                TAG,
                Message(
                    """
                MapboxNavigationApp.setup was ignored because it has already been setup.
                If you want to use new NavigationOptions, you must first call
                MapboxNavigationApp.disable() and then call MapboxNavigationApp.setup(..).
                Calling setup multiple times, is harmless otherwise.
                    """.trimIndent()
                )
            )
            return this
        }

        mapboxNavigationOwner.setup(navigationOptions)
        carAppLifecycleOwner.lifecycle.addObserver(mapboxNavigationOwner.carAppLifecycleObserver)
        isSetup = true
    }

    fun attachAllActivities() {
        val application = mapboxNavigationOwner.navigationOptions.applicationContext as Application
        carAppLifecycleOwner.attachAllActivities(application)
    }

    fun disable() {
        isSetup = false
        carAppLifecycleOwner.lifecycle.removeObserver(mapboxNavigationOwner.carAppLifecycleObserver)
        mapboxNavigationOwner.disable()
    }

    fun attach(lifecycleOwner: LifecycleOwner) {
        carAppLifecycleOwner.attach(lifecycleOwner)
    }

    fun detach(lifecycleOwner: LifecycleOwner) {
        carAppLifecycleOwner.detach(lifecycleOwner)
    }

    fun registerObserver(mapboxNavigationObserver: MapboxNavigationObserver) {
        mapboxNavigationOwner.register(mapboxNavigationObserver)
    }

    fun unregisterObserver(mapboxNavigationObserver: MapboxNavigationObserver) {
        mapboxNavigationOwner.unregister(mapboxNavigationObserver)
    }

    fun current(): MapboxNavigation? = mapboxNavigationOwner.current()

    private companion object {
        private val TAG = Tag("MbxMapboxNavigationApp")
    }
}
