package com.mapbox.navigation.dropin.component.destination

import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.geojson.Point
import com.mapbox.navigation.dropin.model.Destination

sealed class DestinationAction {
    data class SetDestination(val destination: Destination?) : DestinationAction()

    data class DidReverseGeocode(
        val point: Point,
        val features: List<CarmenFeature>
    ) : DestinationAction()
}
