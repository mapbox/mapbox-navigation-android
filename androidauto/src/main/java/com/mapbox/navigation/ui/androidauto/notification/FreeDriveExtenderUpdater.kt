package com.mapbox.navigation.ui.androidauto.notification

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.car.app.notification.CarAppExtender
import androidx.core.content.ContextCompat
import com.mapbox.navigation.ui.androidauto.R

internal class FreeDriveExtenderUpdater(private val context: Context) {

    fun update(extenderBuilder: CarAppExtender.Builder) {
        extenderBuilder.setContentTitle(context.getString(R.string.mapbox_free_drive_session))
        val drawable = ContextCompat.getDrawable(context, R.drawable.mapbox_ic_navigation)!!
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        extenderBuilder.setLargeIcon(bitmap)
    }
}
