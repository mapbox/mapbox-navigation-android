package com.mapbox.navigation.base.trip

import android.app.Notification
import android.content.Context
import android.location.Location

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
     *
     * This method can serve as a cue to update a [Notification]
     * with a specific notification id.
     *
     * @param routeProgress with the latest progress data
     */
    fun updateRouteProgress(routeProgress: RouteProgress?)

    /**
     * If enabled, this method will be called every time a
     * new [Location] is available.
     *
     *
     * This method can serve as a cue to update a [Notification]
     * with a specific notification id.
     *
     * @param rawLocation raw location
     * @param enhancedLocation if possible, location snapped to road
     */
    fun updateLocation(rawLocation: Location, enhancedLocation: Location)

    /**
     * Callback for when trip session is stopped via [TripSession.stop].
     *
     *
     * This callback may be used to clean up any listeners or receivers, preventing leaks.
     *
     * @param context to be used if needed for Android-related work
     */
    fun onTripSessionStopped(context: Context)
}
