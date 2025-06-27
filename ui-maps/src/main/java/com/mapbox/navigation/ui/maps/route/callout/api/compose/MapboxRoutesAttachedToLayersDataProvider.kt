package com.mapbox.navigation.ui.maps.route.callout.api.compose

import com.mapbox.navigation.ui.maps.route.callout.api.RoutesAttachedToLayersDataProvider
import com.mapbox.navigation.ui.maps.route.callout.api.RoutesAttachedToLayersObserver
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import java.util.concurrent.CopyOnWriteArrayList

internal class MapboxRoutesAttachedToLayersDataProvider(
    private val mapboxRouteLineView: MapboxRouteLineView,
) : RoutesAttachedToLayersDataProvider {

    private val observers = CopyOnWriteArrayList<RoutesAttachedToLayersObserver>()

    private val internalObserver = RoutesAttachedToLayersObserver {
        observers.forEach { observer -> observer.onAttached(it) }
    }

    override fun registerRoutesAttachedToLayersObserver(observer: RoutesAttachedToLayersObserver) {
        val firstObserver = observers.isEmpty()
        observers.add(observer)
        if (firstObserver) {
            mapboxRouteLineView.registerRoutesAttachedToLayersObserver(internalObserver)
        }
    }

    override fun unregisterRoutesAttachedToLayersObserver(
        observer: RoutesAttachedToLayersObserver,
    ) {
        observers.remove(observer)
        if (observers.isEmpty()) {
            mapboxRouteLineView.unregisterRoutesAttachedToLayersObserver(internalObserver)
        }
    }
}
