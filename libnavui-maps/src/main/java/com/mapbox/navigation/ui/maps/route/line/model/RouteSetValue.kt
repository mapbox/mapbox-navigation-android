package com.mapbox.navigation.ui.maps.route.line.model

import com.mapbox.geojson.FeatureCollection
import com.mapbox.maps.extension.style.expressions.generated.Expression

/**
 * Represents the side effects for drawing routes on a map.
 *
 * @param primaryRouteSource the feature collection for the primary route line
 * @param trafficLineExpressionProvider the expression for the primary route traffic line
 * @param routeLineExpression the expression for the primary route line
 * @param casingLineExpression the expression for the primary route casing line
 * @param altRoute1TrafficExpressionProvider the expression for an alternative route traffic line
 * @param altRoute2TrafficExpressionProvider the expression for an alternative route traffic line
 * @param alternativeRoute1Source the feature collection for an alternative route line
 * @param alternativeRoute2Source the feature collection for an alternative route line
 * @param waypointsSource the feature collection for the origin and destination icons
 */
class RouteSetValue internal constructor(
    val primaryRouteSource: FeatureCollection,
    val trafficLineExpressionProvider: RouteLineExpressionProvider?,
    val routeLineExpression: Expression,
    val casingLineExpression: Expression,
    val altRoute1TrafficExpressionProvider: RouteLineExpressionProvider?,
    val altRoute2TrafficExpressionProvider: RouteLineExpressionProvider?,
    val alternativeRoute1Source: FeatureCollection,
    val alternativeRoute2Source: FeatureCollection,
    val waypointsSource: FeatureCollection
) {

    /**
     * @return a class with mutable values for replacing.
     */
    fun toMutableValue() = MutableRouteSetValue(
        primaryRouteSource,
        trafficLineExpressionProvider,
        routeLineExpression,
        casingLineExpression,
        altRoute1TrafficExpressionProvider,
        altRoute2TrafficExpressionProvider,
        alternativeRoute1Source,
        alternativeRoute2Source,
        waypointsSource
    )

    /**
     * Represents the mutable side effects for drawing routes on a map.
     *
     * @param primaryRouteSource the feature collection for the primary route line
     * @param trafficLineExpressionProvider the expression for the primary route traffic line
     * @param routeLineExpression the expression for the primary route line
     * @param casingLineExpression the expression for the primary route casing line
     * @param altRoute1TrafficExpression the expression for an alternative route traffic line
     * @param altRoute2TrafficExpression the expression for an alternative route traffic line
     * @param alternativeRoute1Source the feature collection for an alternative route line
     * @param alternativeRoute2Source the feature collection for an alternative route line
     * @param waypointsSource the feature collection for the origin and destination icons
     */
    class MutableRouteSetValue internal constructor (
        var primaryRouteSource: FeatureCollection,
        var trafficLineExpressionProvider: RouteLineExpressionProvider?,
        var routeLineExpression: Expression,
        var casingLineExpression: Expression,
        var altRoute1TrafficExpression: RouteLineExpressionProvider?,
        var altRoute2TrafficExpression: RouteLineExpressionProvider?,
        var alternativeRoute1Source: FeatureCollection,
        var alternativeRoute2Source: FeatureCollection,
        var waypointsSource: FeatureCollection
    ) {

        /**
         * @return a RouteSetValue
         */
        fun toImmutableValue() = RouteSetValue(
            primaryRouteSource,
            trafficLineExpressionProvider,
            routeLineExpression,
            casingLineExpression,
            altRoute1TrafficExpression,
            altRoute2TrafficExpression,
            alternativeRoute1Source,
            alternativeRoute2Source,
            waypointsSource
        )
    }
}

/**
 * Represents a function that returns an [Expression]
 */
typealias RouteLineExpressionProvider = () -> Expression
