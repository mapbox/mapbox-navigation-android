package com.mapbox.navigation.core.trip.service

import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.TripNotificationState
import com.mapbox.navigation.core.trip.session.TripSession

/**
 * It's a part of [TripSession], interface provides [RouteProgress] to notification bar
 */
internal interface TripService {

    /**
     * Start TripService
     */
    fun startService()

    /**
     * Stop TripService
     */
    fun stopService()

    /**
     * Update the trip's information in the notification bar
     */
    fun updateNotification(tripNotificationState: TripNotificationState)

    /**
     * Return *true* if service is started
     */
    fun hasServiceStarted(): Boolean
}
