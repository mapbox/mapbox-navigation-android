package com.mapbox.navigation.trip.session

interface OffRouteObserver {
    fun onOffRouteStateChanged(offRoute: Boolean)
}
