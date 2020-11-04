package com.mapbox.navigation.core.trip.session

/**
 * Observer that gets notified whenever a new enhanced location is available.
 *
 * @see [MapMatcherResult]
 */
interface MapMatcherResultObserver {

    /**
     * Called whenever a new enhanced location is available.
     *
     * @param mapMatcherResult details about the status of the enhanced location.
     */
    fun onNewMapMatcherResult(mapMatcherResult: MapMatcherResult)
}
