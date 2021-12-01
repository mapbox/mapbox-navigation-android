package com.mapbox.navigation.trip.notification

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

internal class MapboxTripNotificationView(
    private val context: Context
) {

    internal fun getImageDrawable(@DrawableRes image: Int): Drawable? {
        return ContextCompat.getDrawable(context, image)
    }

    /**
     * Updates the expanded and collapsed views visibility.
     */
    fun setFreeDriveMode(builder: NotificationCompat.Builder) {
        builder.setContentTitle(context.getString(R.string.mapbox_free_drive_session))
        val drawable = getImageDrawable(R.drawable.mapbox_ic_navigation)!!
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        builder.setLargeIcon(bitmap)
    }
}
