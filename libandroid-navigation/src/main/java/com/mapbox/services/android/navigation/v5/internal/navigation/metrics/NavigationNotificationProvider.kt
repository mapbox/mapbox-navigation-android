package com.mapbox.services.android.navigation.v5.internal.navigation.metrics

import android.content.Context
import com.mapbox.services.android.navigation.v5.internal.navigation.MapboxNavigationNotification
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation
import com.mapbox.services.android.navigation.v5.navigation.notification.NavigationNotification
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress

internal class NavigationNotificationProvider(
    applicationContext: Context,
    mapboxNavigation: MapboxNavigation
) {

    private val navigationNotification: NavigationNotification = buildNotificationFrom(applicationContext, mapboxNavigation)

    private var shouldUpdate: Boolean = true

    fun retrieveNotification(): NavigationNotification {
        return navigationNotification
    }

    fun updateNavigationNotification(routeProgress: RouteProgress) {
        if (shouldUpdate) {
            navigationNotification.updateNotification(routeProgress)
        }
    }

    fun shutdown(applicationContext: Context) {
        navigationNotification.onNavigationStopped(applicationContext)
        shouldUpdate = false
    }

    private fun buildNotificationFrom(
        applicationContext: Context,
        mapboxNavigation: MapboxNavigation
    ): NavigationNotification {
        val navNotification = mapboxNavigation.options().navigationNotification()
        return navNotification ?: MapboxNavigationNotification(applicationContext, mapboxNavigation)
    }
}
