package com.mapbox.navigation.core.trip.session

interface TripSessionStateObserver {
    fun onSessionStarted()
    fun onSessionStopped()
}
