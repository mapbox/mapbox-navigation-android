package com.mapbox.navigation.ui.androidauto.internal

import android.content.Context
import android.content.res.Resources

object RendererUtils {
    private const val ANDROID_BASELINE_DPI = 160.0

    /**
     * The car library does not use context.resources.displayMetrics
     * In order to scale correctly on the head unit, use resource.configuration.densityDpi
     */
    fun Context.dpToPx(dp: Int): Int =
        (dp * resources.configuration.densityDpi / ANDROID_BASELINE_DPI).toInt()

    /**
     * The number of pixels in one dp on the head unit, see [dpToPx].
     */
    fun Context.dpScale(): Float = resources.dpScale()

    /**
     * The number of pixels in one dp for these [Resources], see [dpToPx].
     */
    fun Resources.dpScale(): Float {
        val densityDpi = configuration.densityDpi
        // An undefined density would size the bitmaps drawn from it to zero.
        return if (densityDpi > 0) (densityDpi / ANDROID_BASELINE_DPI).toFloat() else 1f
    }
}
