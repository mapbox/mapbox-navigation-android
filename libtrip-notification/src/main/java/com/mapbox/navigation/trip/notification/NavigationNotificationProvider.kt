package com.mapbox.navigation.trip.notification

import android.app.Notification
import androidx.core.app.NotificationCompat

/**
 * Provides notification for navigation process launched with a trip session
 */
internal object NavigationNotificationProvider {

    /**
     * Builds [Notification] based on [NotificationCompat.Builder] params
     *
     * @param builder is [NotificationCompat.Builder] used for
     * building notification
     * @return [Notification] was built based on [Notification.Builder] params
     */
    fun buildNotification(builder: NotificationCompat.Builder): Notification {
        return builder.build()
    }
}
