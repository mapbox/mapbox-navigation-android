package com.mapbox.navigation.trip.service

import com.mapbox.navigation.base.trip.model.RouteProgress

interface TripService { // todo make internal

    fun startService()

    fun stopService()

    fun updateNotification(routeProgress: RouteProgress)

    fun hasServiceStarted(): Boolean
}
