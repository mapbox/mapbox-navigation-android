package com.mapbox.navigation.base.trip

import com.mapbox.navigation.base.trip.model.RouteProgress

interface TripService {

    fun startService()

    fun stopService()

    fun updateNotification(routeProgress: RouteProgress)
}
