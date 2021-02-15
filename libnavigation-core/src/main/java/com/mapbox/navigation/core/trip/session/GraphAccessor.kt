package com.mapbox.navigation.core.trip.session

import com.mapbox.geojson.Point
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.trip.model.eh.EHorizonEdge
import com.mapbox.navigation.core.trip.model.eh.EHorizonEdgeMetadata
import com.mapbox.navigation.core.trip.model.eh.mapToEHorizonEdgeMetadata
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator

/**
 * [MapboxNavigation.graphAccessor] provides methods to get edge (e.g. [EHorizonEdge]) shape and
 * metadata.
 */
class GraphAccessor internal constructor(
    private val navigator: MapboxNativeNavigator,
) {

    /**
     * Returns Graph Edge geometry for the given GraphId of the edge.
     * If edge with given edgeId is not accessible, returns null
     * @param edgeId
     *
     * @return list of Points representing edge shape
     */
    fun getEdgeShape(edgeId: Long): List<Point>? {
        return navigator.graphAccessor?.getEdgeShape(edgeId)
    }

    /**
     * Returns Graph Edge meta-information for the given GraphId of the edge.
     * If edge with given edgeId is not accessible, returns null
     * @param edgeId
     *
     * @return EHorizonEdgeMetadata
     */
    fun getEdgeMetadata(edgeId: Long): EHorizonEdgeMetadata? {
        return navigator.graphAccessor?.getEdgeMetadata(edgeId)?.mapToEHorizonEdgeMetadata()
    }
}
