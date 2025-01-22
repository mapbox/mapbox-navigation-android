package com.mapbox.navigation.ui.maps.camera.data

import com.mapbox.navigation.ui.maps.camera.NavigationCamera

/**
 * Describes an object that provides desired camera positions to [NavigationCamera].
 *
 * Implementation should always store the latest available [ViewportData] and return it via
 * [getViewportData]. Whenever data becomes available,
 * registered [ViewportDataSourceUpdateObserver] should be notified.
 */
interface ViewportDataSource {

    /**
     * Get the latest [ViewportData].
     */
    fun getViewportData(): ViewportData

    /**
     * Register an observer that gets called whenever the available [ViewportData] changes.
     * The observer also gets notified with latest data on registration.
     */
    fun registerUpdateObserver(viewportDataSourceUpdateObserver: ViewportDataSourceUpdateObserver)

    /**
     * Unregister [ViewportDataSourceUpdateObserver].
     */
    fun unregisterUpdateObserver(viewportDataSourceUpdateObserver: ViewportDataSourceUpdateObserver)
}
