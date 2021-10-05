package com.mapbox.navigation.ui.maps.util

import android.graphics.Bitmap
import android.view.View

/**
 * A callback that is notified when a [View] screenshot is ready
 */
fun interface OnViewScreenshotReady {

    /**
     * Notifies that a [View] screenshot is ready
     *
     * @param screenshot of the [View] as a [Bitmap]
     */
    fun onViewCaptureReady(screenshot: Bitmap)
}
