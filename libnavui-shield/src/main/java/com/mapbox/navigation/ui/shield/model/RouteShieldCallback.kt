package com.mapbox.navigation.ui.shield.model

import android.graphics.Bitmap
import com.mapbox.bindgen.Expected
import com.mapbox.navigation.ui.shield.api.toBitmap

/**
 * An interface that is triggered when road shields are available.
 */
fun interface RouteShieldCallback {

    /**
     * The callback is invoked when road shields are ready.
     *
     * This returns a list of possible [RouteShieldResult] or [RouteShieldError] that can be used
     * to render the shield on a UI.
     *
     * To convert the returned SVG [ByteArray] to a [Bitmap] use [toBitmap].
     *
     * @param shields list of [Expected] wither containing [RouteShieldError] or [RouteShieldResult]
     */
    fun onRoadShields(
        shields: List<Expected<RouteShieldError, RouteShieldResult>>
    )
}
