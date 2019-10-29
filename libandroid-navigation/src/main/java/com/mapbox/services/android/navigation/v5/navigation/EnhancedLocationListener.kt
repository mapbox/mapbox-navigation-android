package com.mapbox.services.android.navigation.v5.navigation

import android.location.Location

/**
 * A listener for getting the best enhanced [Location] updates available at any
 * moment. Either snapped (active guidance), map matched (free drive) or raw.
 * <p>
 * The behavior that causes this listeners callback to get invoked vary depending on whether
 * free drive has been enabled using [MapboxNavigation.enableFreeDrive] or disabled using
 * [MapboxNavigation.disableFreeDrive].
 *
 * @see [MapboxNavigation.enableFreeDrive]
 */
interface EnhancedLocationListener {

    /**
     * Invoked as soon as a new [Location] has been received.
     *
     * @param enhancedLocation either snapped (active guidance), map matched (free drive) or raw
     */
    fun onEnhancedLocationUpdate(enhancedLocation: Location)
}
