package com.mapbox.navigation.ui.androidauto.navigation.speedlimit

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import com.mapbox.navigation.base.speed.model.SpeedLimitSign
import kotlin.math.ceil

/**
 * Draws speed limit signs into two alternating bitmaps per sign format. The bitmap returned by
 * the previous call may still be shown by the widget, so it is never drawn into; it is reused
 * only by the call after next, when the widget has already switched to the newer bitmap.
 *
 * The drawables are designed in dp; [scale] is the number of pixels in one dp on the surface the
 * sign is shown on.
 */
internal class SpeedLimitBitmapRenderer(private val scale: Float = 1f) {
    private val mutcdDrawable: SpeedLimitDrawable = MutcdSpeedLimitDrawable()
    private val viennaDrawable: SpeedLimitDrawable = ViennaSpeedLimitDrawable()
    private val mutcdBuffers = DoubleBuffer(
        scaled(MutcdSpeedLimitDrawable.WIDTH),
        scaled(MutcdSpeedLimitDrawable.HEIGHT),
    )
    private val viennaBuffers = DoubleBuffer(
        scaled(ViennaSpeedLimitDrawable.WIDTH),
        scaled(ViennaSpeedLimitDrawable.HEIGHT),
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
        drawable.draw(Canvas(bitmap).apply { scale(scale, scale) })
        return bitmap
    }

    private fun scaled(dp: Int): Int = ceil(dp * scale).toInt()

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
