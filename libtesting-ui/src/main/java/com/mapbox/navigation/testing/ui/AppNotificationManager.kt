package com.mapbox.navigation.testing.ui

import android.app.Notification.Builder
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat


class AppNotificationManager internal constructor(
    private val context: Context
) {

    companion object {
        private const val NOTIFICATION_ID = 100
        internal const val NOTIFICATION_TITLE = "Title"
        internal const val NOTIFICATION_MESSAGE = "test message"
        private const val CHANNEL_ID = "channel_id"
        private const val CHANNEL_NAME = "Test channel"
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showTestNotification() {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Builder(context, CHANNEL_ID)
        } else {
            Builder(context)
        }
        builder
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(NOTIFICATION_TITLE)
            .setContentText(NOTIFICATION_MESSAGE)
            .setAutoCancel(true)

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
    }
}
