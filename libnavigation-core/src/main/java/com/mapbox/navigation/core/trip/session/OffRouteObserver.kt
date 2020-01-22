package com.mapbox.navigation.core.trip.session

interface OffRouteObserver {
    fun onOffRouteStateChanged(offRoute: Boolean)
}
