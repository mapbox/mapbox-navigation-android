package com.mapbox.navigation.base.trip.model.roadobject.location

import androidx.annotation.IntDef

/**
 * RoadObjectLocationType defines how the road object is located on the map and how this object is
 * matched to a Mapbox road graph.
 *
 * Available values are:
 * - [RoadObjectLocationType.GANTRY]
 * - [RoadObjectLocationType.OPEN_LR_LINE]
 * - [RoadObjectLocationType.OPEN_LR_POINT]
 * - [RoadObjectLocationType.POINT]
 * - [RoadObjectLocationType.POLYGON]
 * - [RoadObjectLocationType.POLYLINE]
 * - [RoadObjectLocationType.POLYLINE]
 * - [RoadObjectLocationType.ROUTE_ALERT]
 */
object RoadObjectLocationType {

    /**
     * Location defined by a pair of geo-coordinates describing a line typically crossing a road.
     */
    const val GANTRY = 0

    /**
     * An OpenLR line location. For additional information regarding this type of location please
     * consult OpenLR documentation.
     */
    const val OPEN_LR_LINE = 1

    /**
     * An OpenLR point along line location. For additional information regarding this type of
     * location please consult OpenLR documentation.
     */
    const val OPEN_LR_POINT = 2

    /**
     * Location defined by a single geo-coordinate.
     */
    const val POINT = 3

    /**
     * Location represented by a non intersecting shape (polygon) defined by a sequence of
     * geo-coordinates.
     */
    const val POLYGON = 4

    /**
     * Location represented by a polyline defined by a sequence of geo-coordinates and consisting
     * of a line or a number of lines forming a path.
     */
    const val POLYLINE = 5

    /**
     * A location type indicating that this road object is located on the route.
     */
    const val ROUTE_ALERT = 6

    /**
     * Location of an object represented as a subgraph.
     */
    const val SUBGRAPH = 7

    /**
     * Retention policy for the RoadObjectLocationType
     */
    @Retention(AnnotationRetention.BINARY)
    @IntDef(
        GANTRY,
        OPEN_LR_LINE,
        OPEN_LR_POINT,
        POINT,
        POLYGON,
        POLYLINE,
        ROUTE_ALERT,
        SUBGRAPH,
    )
    annotation class Type
}
