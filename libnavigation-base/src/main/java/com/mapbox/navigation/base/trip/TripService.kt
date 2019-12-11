package com.mapbox.navigation.base.trip

interface TripService {
    fun startNavigation(tripNotification: TripNotification): Boolean
    fun stopNavigation()
}
