package com.mapbox.navigation.core.infra.recorders

import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.trip.session.RouteProgressObserver

class RouteProgressObserverRecorder : RouteProgressObserver {

    private val _records = mutableListOf<RouteProgress>()
    val records: List<RouteProgress> get() = _records
    override fun onRouteProgressChanged(routeProgress: RouteProgress) {
        _records.add(routeProgress)
    }
}
