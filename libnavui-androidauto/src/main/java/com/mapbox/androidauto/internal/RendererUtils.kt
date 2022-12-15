package com.mapbox.androidauto.internal

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color

object RendererUtils {
    private const val ANDROID_BASELINE_DPI = 160.0

    val EMPTY_BITMAP: Bitmap by lazy {
        Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            .apply { eraseColor(Color.TRANSPARENT) }
    }

    /**
     * The car library does not use context.resources.displayMetrics
     * In order to scale correctly on the head unit, use resource.configuration.densityDpi
     */
    fun Context.dpToPx(dp: Int): Int =
        (dp * resources.configuration.densityDpi / ANDROID_BASELINE_DPI).toInt()
}
