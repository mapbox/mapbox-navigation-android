package com.mapbox.navigation.ui.utils.internal.extensions

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable

/**
 * Converts a [Drawable] to [Bitmap]
 * @receiver Drawable
 * @return Bitmap
 */
fun Drawable.getBitmap(): Bitmap {
    return if (this is BitmapDrawable) {
        this.bitmap
    } else {
        val bitmap = Bitmap.createBitmap(
            intrinsicWidth, intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)
        bitmap
    }
}
