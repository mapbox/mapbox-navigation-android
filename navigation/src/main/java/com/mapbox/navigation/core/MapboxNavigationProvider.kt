package com.mapbox.navigation.core

import androidx.annotation.UiThread
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Singleton responsible for ensuring there is only one [MapboxNavigation] instance.
 * Alternative way of obtaining the instance of the [MapboxNavigation] is [MapboxNavigationApp].
 *
 * Note that [MapboxNavigationProvider] and [MapboxNavigationApp] can't be used together
 * at the same time.
 */
@UiThread
object MapboxNavigationProvider {

    private const val LOG_CATEGORY = "MapboxNavigationProvider"

    @Volatile
    private var mapboxNavigation: MapboxNavigation? = null

    private val observers = CopyOnWriteArrayList<MapboxNavigationObserver>()

    /**
     * Create MapboxNavigation with provided options. Previously created instance
     * of the [MapboxNavigation] will be destroyed. Should be called before [retrieve].
     *
     * @param navigationOptions options used to customize various features of the SDK.
     * @see [MapboxNavigationApp.setup]
     */
    @JvmStatic
    fun create(navigationOptions: NavigationOptions): MapboxNavigation {
        logD("create()")
        mapboxNavigation?.onDestroy()
        mapboxNavigation = MapboxNavigation(
            navigationOptions,
        )

        observers.forEach { it.onAttached(mapboxNavigation!!) }

        return mapboxNavigation!!
    }

    /**
     * Retrieve MapboxNavigation instance. Should be called after [create].
     *
     * @see [isCreated]
     * @see [MapboxNavigationApp.current]
     */
    @JvmStatic
    fun retrieve(): MapboxNavigation {
        if (!isCreated()) {
            throw RuntimeException("Need to create MapboxNavigation before using it.")
        }

        return mapboxNavigation!!
    }

    /**
     * Destroy MapboxNavigation when your process/activity exits.
     */
    @JvmStatic
    fun destroy() {
        logD("destroy()")

        mapboxNavigation?.let { navigation ->
            navigation.onDestroy()
            observers.forEach { it.onDetached(navigation) }
        }
        mapboxNavigation = null
    }

    /**
     * Check if MapboxNavigation is created.
     */
    @JvmStatic
    fun isCreated(): Boolean {
        return mapboxNavigation?.isDestroyed == false
    }

    internal fun registerObserver(observer: MapboxNavigationObserver) {
        observers.add(observer)
        mapboxNavigation?.let { observer.onAttached(it) }
    }

    internal fun unregisterObserver(observer: MapboxNavigationObserver) {
        observers.remove(observer)
        mapboxNavigation?.let { observer.onDetached(it) }
    }

    private fun logD(msg: String) = com.mapbox.navigation.utils.internal.logD(msg, LOG_CATEGORY)
}
