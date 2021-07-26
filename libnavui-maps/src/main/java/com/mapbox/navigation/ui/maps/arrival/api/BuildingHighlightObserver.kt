package com.mapbox.navigation.ui.maps.arrival.api

import com.mapbox.maps.QueriedFeature

/**
 * Observer used to surface the features queried by [MapboxBuildingHighlightApi].
 * This observer can also be used to observe building highlighted upon arrival
 * from the [MapboxBuildingArrivalApi]
 */
fun interface BuildingHighlightObserver {
    /**
     * Called when buildings have been queried. The list is empty when there
     * were no buildings found.
     */
    fun onBuildingHighlight(features: List<QueriedFeature>)
}
