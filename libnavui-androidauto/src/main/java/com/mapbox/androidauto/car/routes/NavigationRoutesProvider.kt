package com.mapbox.androidauto.car.routes

import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult

internal class NavigationRoutesProvider(private val mapboxNavigation: MapboxNavigation) :
    RoutesProvider {

    override fun registerRoutesListener(listener: RoutesListener) {
        mapboxNavigation.registerRoutesObserver(RoutesObserverAdapter(listener))
    }

    override fun unregisterRoutesListener(listener: RoutesListener) {
        mapboxNavigation.unregisterRoutesObserver(RoutesObserverAdapter(listener))
    }

    private data class RoutesObserverAdapter(val listener: RoutesListener) : RoutesObserver {

        override fun onRoutesChanged(result: RoutesUpdatedResult) {
            listener.onRoutesChanged(result.navigationRoutes.take(1))
        }
    }
}
