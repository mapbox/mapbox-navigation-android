package com.mapbox.navigation.ui.maps.route.line.model

import com.mapbox.geojson.FeatureCollection
import com.mapbox.maps.extension.style.expressions.generated.Expression

/**
 * Represents the side effects for drawing routes on a map.
 *
 * @param primaryRouteSource the feature collection for the primary route line
 * @param trafficLineExpression the expression for the primary route traffic line
 * @param routeLineExpression the expression for the primary route line
 * @param casingLineExpression the expression for the primary route casing line
 * @param altRoute1TrafficExpression the expression for an alternative route traffic line
 * @param altRoute2TrafficExpression the expression for an alternative route traffic line
 * @param alternativeRoute1Source the feature collection for an alternative route line
 * @param alternativeRoute2Source the feature collection for an alternative route line
 * @param waypointsSource the feature collection for the origin and destination icons
 */
class RouteSetValue internal constructor(
    val primaryRouteSource: FeatureCollection,
    val trafficLineExpression: Expression,
    val routeLineExpression: Expression,
    val casingLineExpression: Expression,
    val altRoute1TrafficExpression: Expression,
    val altRoute2TrafficExpression: Expression,
    val alternativeRoute1Source: FeatureCollection,
    val alternativeRoute2Source: FeatureCollection,
    val waypointsSource: FeatureCollection
)
