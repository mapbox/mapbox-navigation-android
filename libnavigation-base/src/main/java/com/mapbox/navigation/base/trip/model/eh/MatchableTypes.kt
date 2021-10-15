package com.mapbox.navigation.base.trip.model.eh

import com.mapbox.geojson.Point

/**
 * The record represents a piece of data which is required to match one OpenLR.
 *
 * **NOTE**: The Mapbox Electronic Horizon feature of the Mapbox Navigation SDK is in public beta
 * and is subject to changes, including its pricing. Use of the feature is subject to the beta
 * product restrictions in the Mapbox Terms of Service.
 * Mapbox reserves the right to eliminate any free tier or free evaluation offers at any time and
 * require customers to place an order to purchase the Mapbox Electronic Horizon feature,
 * regardless of the level of use of the feature.
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
 * **NOTE**: The Mapbox Electronic Horizon feature of the Mapbox Navigation SDK is in public beta
 * and is subject to changes, including its pricing. Use of the feature is subject to the beta
 * product restrictions in the Mapbox Terms of Service.
 * Mapbox reserves the right to eliminate any free tier or free evaluation offers at any time and
 * require customers to place an order to purchase the Mapbox Electronic Horizon feature,
 * regardless of the level of use of the feature.
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
 * **NOTE**: The Mapbox Electronic Horizon feature of the Mapbox Navigation SDK is in public beta
 * and is subject to changes, including its pricing. Use of the feature is subject to the beta
 * product restrictions in the Mapbox Terms of Service.
 * Mapbox reserves the right to eliminate any free tier or free evaluation offers at any time and
 * require customers to place an order to purchase the Mapbox Electronic Horizon feature,
 * regardless of the level of use of the feature.
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
