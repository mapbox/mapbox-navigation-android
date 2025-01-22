package com.mapbox.navigation.ui.maps.camera.data

import androidx.annotation.UiThread

/**
 * Observer that gets notified whenever [ViewportData] changes.
 */
fun interface ViewportDataSourceUpdateObserver {

    /**
     * Called whenever [ViewportData] changes.
     * @param viewportData latest data
     */
    @UiThread
    fun viewportDataSourceUpdated(viewportData: ViewportData)
}
