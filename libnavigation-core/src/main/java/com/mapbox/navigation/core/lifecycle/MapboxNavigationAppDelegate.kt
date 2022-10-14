package com.mapbox.navigation.core.lifecycle

import android.app.Application
import androidx.lifecycle.LifecycleOwner
import com.mapbox.navigation.core.MapboxNavigation
import kotlin.jvm.Throws
import kotlin.reflect.KClass

/**
 * This is a testable version of [MapboxNavigationApp]. Please refer to the singleton
 * for documented functions and expected behaviors.
 */
internal class MapboxNavigationAppDelegate {
    private val mapboxNavigationOwner by lazy { MapboxNavigationOwner() }
    private val carAppLifecycleOwner by lazy { CarAppLifecycleOwner() }

    val lifecycleOwner: LifecycleOwner by lazy { carAppLifecycleOwner }

    var isOptionsChanging = false
        private set

    var isSetup = false
        private set

    fun setup(navigationOptionsProvider: NavigationOptionsProvider) = apply {
        if (carAppLifecycleOwner.isConfigurationChanging()) {
            return this
        }

        if (isSetup) {
            isOptionsChanging = true
            disable()
        }
        mapboxNavigationOwner.setup(navigationOptionsProvider)
        carAppLifecycleOwner.lifecycle.addObserver(mapboxNavigationOwner.carAppLifecycleObserver)
        isOptionsChanging = false
        isSetup = true
    }

    fun isConfigurationChanging(): Boolean {
        return carAppLifecycleOwner.isConfigurationChanging()
    }

    fun attachAllActivities(application: Application) {
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

    @Throws(IllegalStateException::class)
    fun <T : MapboxNavigationObserver> getObserver(clazz: Class<T>): T =
        mapboxNavigationOwner.getObserver(clazz)

    @Throws(IllegalStateException::class)
    fun <T : MapboxNavigationObserver> getObserver(kClass: KClass<T>): T =
        mapboxNavigationOwner.getObserver(kClass)

    fun <T : MapboxNavigationObserver> getObservers(clazz: Class<T>): List<T> =
        mapboxNavigationOwner.getObservers(clazz)

    fun <T : MapboxNavigationObserver> getObservers(kClass: KClass<T>): List<T> =
        mapboxNavigationOwner.getObservers(kClass)
}
