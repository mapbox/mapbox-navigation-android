package com.mapbox.navigation.core.trip.session

import com.mapbox.geojson.Point
import com.mapbox.navigation.core.trip.model.eh.EHorizonEdgeMetadata

interface EHorizonGraphAccessor {
    /**
     * Gets the shape of the EHorizon Edge
     * @param edgeId
     *
     * @return list of Points representing edge shape
     */
    fun getEdgeShape(edgeId: Long): List<Point>?

    /**
     * Gets the metadata of the EHorizon Edge
     * @param edgeId
     *
     * @return EdgeMetadata
     */
    fun getEdgeMetadata(edgeId: Long): EHorizonEdgeMetadata?
}
