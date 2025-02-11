package com.mapbox.navigation.trip.notification.internal

import android.content.Context
import androidx.core.app.NotificationCompat

/**
 * This class is used to allow for mocking [NotificationCompat.Builder] in tests
 */
internal object NotificationBuilderProvider {
    fun create(context: Context, channelId: String) =
        NotificationCompat.Builder(context, channelId)
}
