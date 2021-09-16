package com.mapbox.navigation.base.trip.model.eh

import com.mapbox.geojson.Point

/**
 * The record represents a piece of data which is required to match one OpenLR.
 *
 * @param roadObjectId unique id of the object
 * @param openLRLocation road object location
 * @param openLRStandard standard used to encode openLRLocation
 */
data class MatchableOpenLr(
    val roadObjectId: String,
    val openLRLocation: String,
    @OpenLRStandard.Type val openLRStandard: String
)

/**
 * The record represents the raw data which could be matched to the road graph.
 * Might be used to match:
 * - gantries, with exactly two coordinates
 * - lines, with two or more coordinates
 * - polygons, with three or more coordinates
 *
 * @param roadObjectId unique id of the object
 * @param coordinates list of points representing the geometry
 */
data class MatchableGeometry(
    val roadObjectId: String,
    val coordinates: List<Point>
)

/**
 * The record represents a raw point which could be matched to the road graph.
 *
 * @param roadObjectId unique id of the object
 * @param point point representing the object
 * @param bearing optional bearing in degrees from the North.
 * Describes the direction of riding for the edge where provided point is going to be matched.
 */
data class MatchablePoint(
    val roadObjectId: String,
    val point: Point,
    val bearing: Double? = null
)
