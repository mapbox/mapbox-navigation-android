package com.mapbox.navigation.ui.maps.route.callout.api

import com.mapbox.navigation.ui.maps.internal.route.callout.api.RoutesSetToRouteLineDataProvider
import com.mapbox.navigation.ui.maps.internal.route.callout.api.RoutesSetToRouteLineObserver
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import java.util.concurrent.CopyOnWriteArrayList

internal class MapboxRoutesSetToRouteLineDataProvider(
    private val mapboxRouteLineApi: MapboxRouteLineApi,
) : RoutesSetToRouteLineDataProvider {

    private val observers = CopyOnWriteArrayList<RoutesSetToRouteLineObserver>()

    private val internalObserver = RoutesSetToRouteLineObserver { routes, metadata ->
        observers.forEach { observer -> observer.onSet(routes, metadata) }
    }

    override fun registerRoutesSetToRouteLineObserver(observer: RoutesSetToRouteLineObserver) {
        val firstObserver = observers.isEmpty()
        observers.add(observer)
        if (firstObserver) {
            mapboxRouteLineApi.registerRoutesSetToRouteLineObserver(internalObserver)
        }
    }

    override fun unregisterRoutesSetToRouteLineObserver(observer: RoutesSetToRouteLineObserver) {
        observers.remove(observer)
        if (observers.isEmpty()) {
            mapboxRouteLineApi.unregisterRoutesSetToRouteLineObserver(internalObserver)
        }
    }
}
