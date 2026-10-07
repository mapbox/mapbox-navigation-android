package com.mapbox.navigation.ui.androidauto.navigation.speedlimit

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import com.mapbox.navigation.base.speed.model.SpeedLimitSign

/**
 * Draws speed limit signs into two alternating bitmaps per sign format. The bitmap returned by
 * the previous call may still be shown by the widget, so it is never drawn into; it is reused
 * only by the call after next, when the widget has already switched to the newer bitmap.
 */
internal class SpeedLimitBitmapRenderer {
    private val mutcdDrawable: SpeedLimitDrawable = MutcdSpeedLimitDrawable()
    private val viennaDrawable: SpeedLimitDrawable = ViennaSpeedLimitDrawable()
    private val mutcdBuffers = DoubleBuffer(
        MutcdSpeedLimitDrawable.WIDTH,
        MutcdSpeedLimitDrawable.HEIGHT,
    )
    private val viennaBuffers = DoubleBuffer(
        ViennaSpeedLimitDrawable.WIDTH,
        ViennaSpeedLimitDrawable.HEIGHT,
    )

    fun getBitmap(
        signFormat: SpeedLimitSign,
        speedLimit: Int? = null,
        speed: Int = 0,
        warn: Boolean = false,
    ): Bitmap {
        val (drawable, buffers) = when (signFormat) {
            SpeedLimitSign.MUTCD -> mutcdDrawable to mutcdBuffers
            SpeedLimitSign.VIENNA -> viennaDrawable to viennaBuffers
        }
        drawable.speedLimit = speedLimit
        drawable.speed = speed
        drawable.warn = warn

        val bitmap = buffers.next()
        bitmap.eraseColor(Color.TRANSPARENT)
        drawable.draw(Canvas(bitmap))
        return bitmap
    }

    private class DoubleBuffer(private val width: Int, private val height: Int) {
        private val bitmaps = arrayOfNulls<Bitmap>(2)
        private var frontIndex = 1

        fun next(): Bitmap {
            frontIndex = 1 - frontIndex
            return bitmaps[frontIndex]
                ?: Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    .also { bitmaps[frontIndex] = it }
        }
    }
}
