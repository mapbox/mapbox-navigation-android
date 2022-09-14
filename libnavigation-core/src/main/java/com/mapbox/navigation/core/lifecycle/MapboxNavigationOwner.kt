package com.mapbox.navigation.core.lifecycle

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.utils.internal.logI
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.jvm.Throws
import kotlin.reflect.KClass

internal class MapboxNavigationOwner {

    private lateinit var navigationOptionsProvider: NavigationOptionsProvider

    private val services = CopyOnWriteArraySet<MapboxNavigationObserver>()
    private var mapboxNavigation: MapboxNavigation? = null
    private var attached = false

    internal val carAppLifecycleObserver = object : DefaultLifecycleObserver {

        override fun onStart(owner: LifecycleOwner) {
            logI("onStart", LOG_CATEGORY)
            check(!MapboxNavigationProvider.isCreated()) {
                "MapboxNavigation should only be created by the MapboxNavigationOwner"
            }
            val navigationOptions = navigationOptionsProvider.createNavigationOptions()
            val mapboxNavigation = MapboxNavigationProvider.create(navigationOptions)
            this@MapboxNavigationOwner.mapboxNavigation = mapboxNavigation
            attached = true
            services.forEach { it.onAttached(mapboxNavigation) }
        }

        override fun onStop(owner: LifecycleOwner) {
            logI("onStop", LOG_CATEGORY)
            attached = false
            services.forEach { it.onDetached(mapboxNavigation!!) }
            MapboxNavigationProvider.destroy()
            mapboxNavigation = null
        }
    }

    fun setup(navigationOptionsProvider: NavigationOptionsProvider) {
        this.navigationOptionsProvider = navigationOptionsProvider
    }

    fun register(mapboxNavigationObserver: MapboxNavigationObserver) = apply {
        if (services.add(mapboxNavigationObserver)) {
            mapboxNavigation?.let { mapboxNavigationObserver.onAttached(it) }
        }
    }

    fun unregister(mapboxNavigationObserver: MapboxNavigationObserver) {
        if (services.remove(mapboxNavigationObserver)) {
            mapboxNavigation?.let { mapboxNavigationObserver.onDetached(it) }
        }
    }

    fun disable() {
        if (attached) {
            attached = false
            services.forEach { it.onDetached(mapboxNavigation!!) }
            MapboxNavigationProvider.destroy()
            mapboxNavigation = null
            logI("disabled ${services.size} observers", LOG_CATEGORY)
        }
    }

    fun current(): MapboxNavigation? = mapboxNavigation

    @Throws(IllegalStateException::class)
    fun <T : MapboxNavigationObserver> getObserver(clazz: KClass<T>): T = getObserver(clazz.java)

    // Java
    @Throws(IllegalStateException::class)
    fun <T : MapboxNavigationObserver> getObserver(clazz: Class<T>): T =
        getObservers(clazz).firstOrNull()
            ?: error("Class ${clazz.simpleName} is not been registered to MapboxNavigationApp")

    fun <T : MapboxNavigationObserver> getObservers(clazz: KClass<T>): List<T> =
        getObservers(clazz.java)

    // Java
    fun <T : MapboxNavigationObserver> getObservers(clazz: Class<T>): List<T> =
        services.filterIsInstance(clazz)

    private companion object {
        private const val LOG_CATEGORY = "MapboxNavigationOwner"
    }
}
