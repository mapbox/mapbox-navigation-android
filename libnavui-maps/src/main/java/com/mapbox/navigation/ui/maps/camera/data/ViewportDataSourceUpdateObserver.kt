package com.mapbox.navigation.ui.maps.camera.data

/**
 * Observer that gets notified whenever [ViewportData] changes.
 */
interface ViewportDataSourceUpdateObserver {

    /**
     * Called whenever [ViewportData] changes.
     * @param viewportData latest data
     */
    fun viewportDataSourceUpdated(viewportData: ViewportData)
}
