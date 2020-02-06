package com.mapbox.navigation.core.trip.session

import android.location.Location

interface LocationObserver {
    fun onRawLocationChanged(rawLocation: Location)

    /**
     * Provides the best possible location update, snapped to the route or map-matched to the road if possible.
     *
     * @param enhancedLocation the best possible location update
     * @param keyPoints a list (can be empty) of predicted location points leading up to the target update.
     * The last point on the list (if not empty) is always equal to [enhancedLocation].
     */
    fun onEnhancedLocationChanged(enhancedLocation: Location, keyPoints: List<Location>)
}
