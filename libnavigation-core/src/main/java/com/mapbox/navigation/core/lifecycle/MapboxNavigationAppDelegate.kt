package com.mapbox.navigation.core.lifecycle

import android.app.Application
import androidx.lifecycle.LifecycleOwner
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigation

/**
 * This is a testable version of [MapboxNavigationApp]. Please refer to the singleton
 * for documented functions and expected behaviors.
 */
@ExperimentalPreviewMapboxNavigationAPI
internal class MapboxNavigationAppDelegate {
    private val mapboxNavigationOwner = MapboxNavigationOwner()
    private val carAppLifecycleOwner = CarAppLifecycleOwner()
    private var isSetup = false

    val lifecycleOwner: LifecycleOwner = carAppLifecycleOwner

    fun setup(navigationOptions: NavigationOptions) = apply {
        if (carAppLifecycleOwner.isConfigurationChanging()) {
            return this
        }

        mapboxNavigationOwner.setup(navigationOptions)
        if (isSetup) {
            disable()
        }
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
}
