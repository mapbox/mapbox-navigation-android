package com.mapbox.navigation.trip.notification

import android.app.Notification
import androidx.core.app.NotificationCompat

class NavigationNotificationProvider {
    fun buildNotification(builder: NotificationCompat.Builder): Notification {
        return builder.build()
    }
}
