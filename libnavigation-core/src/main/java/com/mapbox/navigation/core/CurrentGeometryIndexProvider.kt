package com.mapbox.navigation.core

import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.trip.session.RouteProgressObserver

internal class CurrentGeometryIndexProvider : RouteProgressObserver, Function0<Int?> {

    private var currentGeometryIndex: Int? = null
        @Synchronized get
        @Synchronized set

    override fun onRouteProgressChanged(routeProgress: RouteProgress) {
        currentGeometryIndex = routeProgress.currentRouteGeometryIndex
    }

    override fun invoke(): Int? = currentGeometryIndex
}
