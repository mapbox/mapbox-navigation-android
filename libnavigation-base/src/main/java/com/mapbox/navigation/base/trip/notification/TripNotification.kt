package com.mapbox.navigation.base.trip.notification

import android.app.Notification
import com.mapbox.navigation.base.trip.model.RouteProgress

/**
 * Defines a contract for [Notification] instance provider and manager.
 * This notification is going to be used in a foreground service managed by a [TripSession]
 */
interface TripNotification {

    /**
     * Provides a custom [Notification] to launch
     * with the [TripSession], specifically
     * [android.app.Service.startForeground].
     *
     * @return a custom notification
     */
    fun getNotification(): Notification

    /**
     * An integer id that will be used to start this notification
     * from [TripSession] with
     * [android.app.Service.startForeground].
     *
     * @return an int id specific to the notification
     */
    fun getNotificationId(): Int

    /**
     * If enabled, this method will be called every time a
     * new [RouteProgress] is generated.
     *
     * This method can serve as a cue to update a [Notification]
     * with a specific notification id.
     *
     * @param routeProgress with the latest progress data
     */
    fun updateNotification(routeProgress: RouteProgress?)

    /**
     * Callback for when trip session is started via [TripSession.start].
     *
     * This callback may be used to perform post start initialization
     */
    fun onTripSessionStarted()

    /**
     * Callback for when trip session is stopped via [TripSession.stop].
     *
     * This callback may be used to clean up any listeners or receivers, preventing leaks.
     */
    fun onTripSessionStopped()
}
