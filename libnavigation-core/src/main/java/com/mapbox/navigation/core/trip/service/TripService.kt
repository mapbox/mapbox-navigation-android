package com.mapbox.navigation.core.trip.service

import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.trip.session.TripSession

// todo make internal
/**
 * It's a part of [TripSession], interface provides [RouteProgress] to notification bar
 */
interface TripService {

    /**
     * Start TripService
     */
    fun startService()

    /**
     * Stop TripSerice
     */
    fun stopService()

    /**
     * Update the trip's information in the notification bar
     */
    fun updateNotification(routeProgress: RouteProgress)

    /**
     * Return *true* if service is started
     */
    fun hasServiceStarted(): Boolean
}
