package com.mapbox.navigation.ui.maps.route.line.model

import com.mapbox.geojson.FeatureCollection
import com.mapbox.maps.extension.style.expressions.generated.Expression

/**
 * Represents the side effects for drawing routes on a map.
 *
 * @param primaryRouteLineData the data of the primary route line
 * @param alternativeRouteLinesData the data of the alternative route lines
 * @param waypointsSource the feature collection for the origin and destination icons
 */
class RouteSetValue internal constructor(
    val primaryRouteLineData: RouteLineData,
    val alternativeRouteLinesData: List<RouteLineData>,
    val waypointsSource: FeatureCollection
) {

    /**
     * @return a class with mutable values for replacing.
     */
    fun toMutableValue() = MutableRouteSetValue(
        primaryRouteLineData,
        alternativeRouteLinesData,
        waypointsSource
    )

    /**
     * Represents the mutable side effects for drawing routes on a map.
     *
     * @param primaryRouteLineData the data of the primary route line
     * @param alternativeRouteLinesData the data of the alternative route lines
     * @param waypointsSource the feature collection for the origin and destination icons
     */
    class MutableRouteSetValue internal constructor(
        var primaryRouteLineData: RouteLineData,
        var alternativeRouteLinesData: List<RouteLineData>,
        var waypointsSource: FeatureCollection
    ) {

        /**
         * @return a RouteSetValue
         */
        fun toImmutableValue() = RouteSetValue(
            primaryRouteLineData,
            alternativeRouteLinesData,
            waypointsSource
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
