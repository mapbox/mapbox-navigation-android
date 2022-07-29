package com.mapbox.navigation.core

import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.trip.session.RouteProgressObserver

internal class CurrentGeometryIndicesProvider : RouteProgressObserver, Function0<Pair<Int?, Int?>> {

    private var indices: Pair<Int?, Int?> = null to null

    override fun onRouteProgressChanged(routeProgress: RouteProgress) {
        indices = routeProgress.currentRouteGeometryIndex to
            routeProgress.currentLegProgress?.geometryIndex
    }

    override fun invoke(): Pair<Int?, Int?> = indices
}
