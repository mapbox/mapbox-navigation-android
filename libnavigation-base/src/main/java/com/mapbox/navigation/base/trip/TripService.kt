package com.mapbox.navigation.base.trip

interface TripService {

    fun startService()

    fun stopService()

    fun updateNotification(routeProgress: RouteProgress)
}
