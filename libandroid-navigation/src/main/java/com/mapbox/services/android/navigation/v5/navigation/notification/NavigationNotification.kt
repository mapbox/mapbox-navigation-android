package com.mapbox.services.android.navigation.v5.navigation.notification

import android.app.Notification
import android.content.Context
import com.mapbox.services.android.navigation.v5.internal.navigation.NavigationService
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress

/**
 * Defines a contract in which a custom notification must adhere to when
 * given to [com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions].
 */
interface NavigationNotification {

    /**
     * Provides a custom [Notification] to launch
     * with the [NavigationService], specifically
     * [android.app.Service.startForeground].
     *
     * @return a custom notification
     */
    fun getNotification(): Notification

    /**
     * An integer id that will be used to start this notification
     * from [NavigationService] with
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
    fun updateNotification(routeProgress: RouteProgress)

    /**
     * Callback for when navigation is stopped via [MapboxNavigation.stopNavigation].
     *
     *
     * This callback may be used to clean up any listeners or receivers, preventing leaks.
     *
     * @param context to be used if needed for Android-related work
     */
    fun onNavigationStopped(context: Context)
}
