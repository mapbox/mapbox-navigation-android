package com.mapbox.navigation.base.trip.model

import android.app.Notification

/**
 * Defines data needed for displaying notification and passed to [TripNotification]
 *
 * @param notificationId ID of displayed notification
 * @param notification [Notification] object for displayed notification
 */
data class MapboxNotificationData(
    val notificationId: Int,
    val notification: Notification
)
