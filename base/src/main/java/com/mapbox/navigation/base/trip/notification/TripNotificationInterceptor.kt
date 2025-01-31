package com.mapbox.navigation.base.trip.notification

import androidx.core.app.NotificationCompat

/**
 * This allows you to extend the notification, or change it to suit your needs. You will need to
 * use this in order to extend the notification for Android Auto.
 */
fun interface TripNotificationInterceptor {
    /**
     * Called when the notification is being built.
     *
     * @param builder with values set to have a full navigation experience
     */
    fun intercept(builder: NotificationCompat.Builder): NotificationCompat.Builder
}
