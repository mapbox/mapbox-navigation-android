package com.mapbox.navigation.core.trip.session

import com.mapbox.geojson.Point
import com.mapbox.navigation.core.trip.model.eh.EHorizonEdgeMetadata
import com.mapbox.navigation.core.trip.model.eh.mapToEHorizonEdgeMetadata
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator

class EHorizonGraphAccessorImpl(
    private val navigator: MapboxNativeNavigator,
) : EHorizonGraphAccessor {

    override fun getEdgeShape(edgeId: Long): List<Point>? {
        return navigator.graphAccessor?.getEdgeShape(edgeId)
    }

    override fun getEdgeMetadata(edgeId: Long): EHorizonEdgeMetadata? {
        return navigator.graphAccessor?.getEdgeMetadata(edgeId)?.mapToEHorizonEdgeMetadata()
    }
}
