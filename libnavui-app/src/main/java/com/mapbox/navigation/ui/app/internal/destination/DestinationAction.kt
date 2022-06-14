package com.mapbox.navigation.ui.app.internal.destination

import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.geojson.Point
import com.mapbox.navigation.ui.app.internal.Action

/**
 * Defines actions responsible to mutate the [Destination].
 */
sealed class DestinationAction : Action {
    /**
     * The action is used to set the destination for the trip.
     * @property destination
     */
    data class SetDestination(val destination: Destination?) : DestinationAction()

    /**
     * The action informs if reverse geocoding was successful for a given [point]
     * @property point
     * @property features
     */
    data class DidReverseGeocode(
        val point: Point,
        val features: List<CarmenFeature>
    ) : DestinationAction()
}
