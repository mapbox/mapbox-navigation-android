package com.mapbox.navigation.base.trip

import android.app.Notification
import com.mapbox.navigation.base.trip.model.RouteProgress

/**
 * Defines notification showing to user during navigation process
 * launched with a [TripSession].
 *
 * @since 1.0.0
 */
interface TripNotification {

    /**
     * Provides a custom [Notification] to launch
     * with the [TripSession], specifically
     * [android.app.Service.startForeground].
     *
     * @return a custom notification
     * @since 1.0.0
     */
    fun getNotification(): Notification

    /**
     * An integer id that will be used to start this notification
     * from [TripSession] with
     * [android.app.Service.startForeground].
     *
     * @return an int id specific to the notification
     * @since 1.0.0
     */
    fun getNotificationId(): Int

    /**
     * If enabled, this method will be called every time a
     * new [RouteProgress] is generated.
     *
     *
     * This method can serve as a cue to update a [Notification]
     * with a specific notification id.
     *
     * @param routeProgress with the latest progress data
     * @since 1.0.0
     */
    fun updateNotification(routeProgress: RouteProgress)

    /**
     * Callback for when trip session is started via [TripSession.start].
     *
     *
     * This callback may be used to perform post start initialization
     * @since 1.0.0
     */
    fun onTripSessionStarted()

    /**
     * Callback for when trip session is stopped via [TripSession.stop].
     *
     *
     * This callback may be used to clean up any listeners or receivers, preventing leaks.
     * @since 1.0.0
     */
    fun onTripSessionStopped()
}
