package com.mapbox.navigation.core.infra.recorders

import com.mapbox.navigation.core.trip.session.OffRouteObserver

class OffRouteObserverRecorder : OffRouteObserver {

    private val _records = mutableListOf<Boolean>()
    val records: List<Boolean> get() = _records

    override fun onOffRouteStateChanged(offRoute: Boolean) {
        _records.add(offRoute)
    }
}
