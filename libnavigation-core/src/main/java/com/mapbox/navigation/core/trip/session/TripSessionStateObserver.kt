package com.mapbox.navigation.core.trip.session

interface TripSessionStateObserver {
    fun onSessionStateChanged(tripSessionState: TripSessionState)
}
