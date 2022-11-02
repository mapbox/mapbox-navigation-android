package com.mapbox.navigation.core.internal

import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import com.mapbox.navigation.core.MapboxNavigation
import java.util.concurrent.CopyOnWriteArraySet

fun interface MapboxNavigationCreateObserver {

    fun onCreated(mapboxNavigation: MapboxNavigation)
}

@Deprecated("Used to keep track of MapboxNavigation instances created via deprecated methods")
@UiThread
object LegacyMapboxNavigationInstanceHolder {

    @Volatile
    private var mapboxNavigation: MapboxNavigation? = null

    private val createObservers = CopyOnWriteArraySet<MapboxNavigationCreateObserver>()

    fun onCreated(instance: MapboxNavigation) {
        mapboxNavigation = instance
        createObservers.forEach { it.onCreated(instance) }
    }

    fun onDestroyed() {
        mapboxNavigation = null
    }

    fun peek(): MapboxNavigation? = mapboxNavigation

    fun registerCreateObserver(observer: MapboxNavigationCreateObserver) {
        createObservers.add(observer)
        mapboxNavigation?.let { observer.onCreated(it) }
    }

    fun unregisterCreateObserver(observer: MapboxNavigationCreateObserver) {
        createObservers.remove(observer)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun unregisterAllCreateObservers() {
        createObservers.clear()
    }
}
