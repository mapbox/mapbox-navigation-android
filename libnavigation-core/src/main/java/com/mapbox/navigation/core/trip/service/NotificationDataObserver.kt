package com.mapbox.navigation.core.trip.service

internal fun interface NotificationDataObserver {
    fun onNotificationUpdated(notificationData: MapboxNotificationData)
}
