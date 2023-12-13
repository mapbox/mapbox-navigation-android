package com.mapbox.navigation.utils.internal

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable


fun Drawable.toBitmap(
    width: Int = intrinsicWidth,
    height: Int = intrinsicHeight,
    config: Bitmap.Config? = null
): Bitmap {
    if (this is BitmapDrawable) {
        if (config == null || bitmap.config == config) {
            // Fast-path to return original. Bitmap.createScaledBitmap will do this check, but it
            // involves allocation and two jumps into native code so we perform the check ourselves.
            if (width == intrinsicWidth && height == intrinsicHeight) {
                return bitmap
            }
            return Bitmap.createScaledBitmap(bitmap, width, height, true)
        }
    }
    val oldBounds = bounds
    val bitmap = Bitmap.createBitmap(width, height, config ?: Bitmap.Config.ARGB_8888)
    setBounds(0, 0, width, height)
    draw(Canvas(bitmap))
    setBounds(oldBounds.left, oldBounds.top, oldBounds.right, oldBounds.bottom)
    return bitmap
}
