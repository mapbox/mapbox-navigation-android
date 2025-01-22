package com.mapbox.navigation.tripdata.shield.model

import android.graphics.Bitmap
import androidx.annotation.UiThread
import com.mapbox.bindgen.Expected

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
     * To convert the returned SVG [ByteArray] to a [Bitmap] use [RouteShield.toBitmap].
     *
     * @param shields list of [Expected] wither containing [RouteShieldError] or [RouteShieldResult]
     */
    @UiThread
    fun onRoadShields(
        shields: List<Expected<RouteShieldError, RouteShieldResult>>,
    )
}
