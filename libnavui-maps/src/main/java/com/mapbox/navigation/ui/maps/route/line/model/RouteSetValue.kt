package com.mapbox.navigation.ui.maps.route.line.model

import com.mapbox.geojson.FeatureCollection
import com.mapbox.maps.extension.style.expressions.generated.Expression

/**
 * Represents the side effects for drawing routes on a map.
 *
 * @param primaryRouteLineData the data of the primary route line
 * @param alternativeRouteLinesData the data of the alternative route lines
 * @param waypointsSource the feature collection for the origin and destination icons
 * @param routeLineMaskingLayerDynamicData the data of the masking line
 */
class RouteSetValue internal constructor(
    val primaryRouteLineData: RouteLineData,
    val alternativeRouteLinesData: List<RouteLineData>,
    val waypointsSource: FeatureCollection,
    val routeLineMaskingLayerDynamicData: RouteLineDynamicData? = null
) {

    /**
     * @return a class with mutable values for replacing.
     */
    fun toMutableValue() = MutableRouteSetValue(
        primaryRouteLineData,
        alternativeRouteLinesData,
        waypointsSource,
        routeLineMaskingLayerDynamicData
    )

    /**
     * Represents the mutable side effects for drawing routes on a map.
     *
     * @param primaryRouteLineData the data of the primary route line
     * @param alternativeRouteLinesData the data of the alternative route lines
     * @param waypointsSource the feature collection for the origin and destination icons
     * @param routeLineMaskingLayerDynamicData the data of the masking line
     */
    class MutableRouteSetValue internal constructor(
        var primaryRouteLineData: RouteLineData,
        var alternativeRouteLinesData: List<RouteLineData>,
        var waypointsSource: FeatureCollection,
        var routeLineMaskingLayerDynamicData: RouteLineDynamicData?
    ) {

        /**
         * @return a RouteSetValue
         */
        fun toImmutableValue() = RouteSetValue(
            primaryRouteLineData,
            alternativeRouteLinesData,
            waypointsSource,
            routeLineMaskingLayerDynamicData
        )
    }
}

/**
 * Represents a function that returns an [Expression]
 */
fun interface RouteLineExpressionProvider {
    /**
     * Generates an expression.
     */
    fun generateExpression(): Expression
}

/**
 * Represents a function that returns an [Expression]. The expression this provider is expected
 * to produce is a lineTrimOffset expression. This is a specific type of expression that will
 * make a line transparent between two values representing sections of a line.
 *
 * For example a call like literal(listOf(0.0, 0.5)) would produce a trim offset expression
 * that made a line transparent from the beginning of the line to the midpoint of the line. In other
 * words the first 50% of the line would be transparent.  A call like literal(listOf(0.25, 0.5))
 * would make the line transparent starting at 25% of the line's length to 50% of the line's length.
 * The line's color would be represented in the other sections of the line.  See the Map API documentation
 * regarding lineTrimOffset for more information.
 */
fun interface RouteLineTrimExpressionProvider : RouteLineExpressionProvider
