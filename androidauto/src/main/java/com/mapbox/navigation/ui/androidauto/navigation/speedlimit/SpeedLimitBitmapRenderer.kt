package com.mapbox.navigation.ui.androidauto.navigation.speedlimit

import android.graphics.Bitmap
import android.graphics.Canvas
import com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool
import com.mapbox.navigation.base.speed.model.SpeedLimitSign

internal class SpeedLimitBitmapRenderer {
    private val mutcdDrawable: SpeedLimitDrawable = MutcdSpeedLimitDrawable()
    private val viennaDrawable: SpeedLimitDrawable = ViennaSpeedLimitDrawable()
    private val bitmapPool: LruBitmapPool = LruBitmapPool(
        MutcdSpeedLimitDrawable.BITMAP_BYTE_SIZE + ViennaSpeedLimitDrawable.BITMAP_BYTE_SIZE,
    )

    fun getBitmap(
        signFormat: SpeedLimitSign,
        speedLimit: Int? = null,
        speed: Int = 0,
        warn: Boolean = false,
    ): Bitmap {
        val drawable = when (signFormat) {
            SpeedLimitSign.MUTCD -> mutcdDrawable
            SpeedLimitSign.VIENNA -> viennaDrawable
        }
        drawable.speedLimit = speedLimit
        drawable.speed = speed
        drawable.warn = warn

        val bitmap = bitmapPool.get(signFormat)
        drawable.draw(Canvas(bitmap))
        bitmapPool.put(bitmap)
        return bitmap
    }

    private fun LruBitmapPool.get(sign: SpeedLimitSign): Bitmap {
        return when (sign) {
            SpeedLimitSign.MUTCD -> get(
                MutcdSpeedLimitDrawable.WIDTH,
                MutcdSpeedLimitDrawable.HEIGHT,
                Bitmap.Config.ARGB_8888,
            )
            SpeedLimitSign.VIENNA -> get(
                ViennaSpeedLimitDrawable.WIDTH,
                ViennaSpeedLimitDrawable.HEIGHT,
                Bitmap.Config.ARGB_8888,
            )
        }
    }
}
