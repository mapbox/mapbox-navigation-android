package com.mapbox.navigation.base.trip

import android.app.Notification
import android.location.Location
const val NAVIGATION_NOTIFICATION_ID = 5678

data class NavigationTripDescriptor(val routeProgress: RouteProgress, val rawLocation: Location, val enhancedLocation: Location)
data class MapboxNotificationData(val notificationID: Int, val notification: Notification)
interface TripService {

    fun startService()
    fun stopService()
}
