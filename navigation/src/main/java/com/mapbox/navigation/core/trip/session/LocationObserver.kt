package com.mapbox.navigation.core.trip.session

import androidx.annotation.UiThread
import com.mapbox.common.location.Location

/**
 * An interface which enables listening to location updates
 *
 * @see [LocationMatcherResult]
 */
@UiThread
interface LocationObserver {

    /**
     * Invoked as soon as a new [Location] has been received.
     *
     * @param rawLocation un-snapped update
     */
    fun onNewRawLocation(rawLocation: Location)

    /**
     * Provides the best possible location update, snapped to the route or map-matched to the road if possible.
     *
     * @param locationMatcherResult details about the status of the enhanced location.
     */
    fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult)
}
