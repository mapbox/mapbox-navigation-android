@file:JvmName("MapboxNavigationObserverEx")

package com.mapbox.navigation.core.internal.extensions

import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * When returning an observer, you can use this extension to return a list of observers. This will
 * attach one to many observers to your view binder.
 */
fun navigationListOf(vararg elements: MapboxNavigationObserver): MapboxNavigationObserver =
    MapboxNavigationObserverChain().apply { addAll(*elements) }

class MapboxNavigationObserverChain : MapboxNavigationObserver {
    private val queue = ConcurrentLinkedQueue<MapboxNavigationObserver>()
    private var attached: MapboxNavigation? = null

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        attached = mapboxNavigation
        queue.forEach { it.onAttached(mapboxNavigation) }
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        queue.reversed().forEach { it.onDetached(mapboxNavigation) }
        attached = null
    }

    fun add(observer: MapboxNavigationObserver) {
        queue.add(observer)
    }

    fun remove(observer: MapboxNavigationObserver) {
        queue.remove(observer)
    }

    fun removeAndDetach(vararg observers: MapboxNavigationObserver) {
        observers.forEach { observer ->
            if (queue.remove(observer)) {
                attached?.also { observer.onDetached(it) }
            }
        }
    }

    fun addAll(vararg observers: MapboxNavigationObserver) {
        queue.addAll(observers)
    }

    fun removeAll(vararg observers: MapboxNavigationObserver) {
        queue.removeAll(observers.toSet())
    }

    fun clear() {
        queue.clear()
    }

    fun toList(): List<MapboxNavigationObserver> = queue.toList()
}
