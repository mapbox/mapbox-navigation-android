package com.mapbox.navigation.trip.session

interface TripSessionStateObserver {
    fun onSessionStarted()
    fun onSessionStopped()
}
