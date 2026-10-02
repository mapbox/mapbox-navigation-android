package com.mapbox.navigation.ui.androidauto.internal

import android.content.Context

object RendererUtils {
    private const val ANDROID_BASELINE_DPI = 160.0

    /**
     * The car library does not use context.resources.displayMetrics
     * In order to scale correctly on the head unit, use resource.configuration.densityDpi
     */
    fun Context.dpToPx(dp: Int): Int =
        (dp * resources.configuration.densityDpi / ANDROID_BASELINE_DPI).toInt()
}
