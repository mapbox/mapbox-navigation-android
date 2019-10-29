package com.mapbox.services.android.navigation.v5.navigation

import android.location.Location

/**
 * A listener for getting the best enhanced [Location] updates available at any
 * moment. Either snapped (active guidance), map matched (free drive) or raw.
 */
interface EnhancedLocationListener {

    /**
     * Invoked as soon as a new [Location] has been received.
     *
     * @param enhancedLocation either snapped (active guidance), map matched (free drive) or raw
     */
    fun onEnhancedLocationUpdate(enhancedLocation: Location)
}
