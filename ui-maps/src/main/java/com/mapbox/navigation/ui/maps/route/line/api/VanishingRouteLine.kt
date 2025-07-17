package com.mapbox.navigation.ui.maps.route.line.api

import com.mapbox.bindgen.Value
import com.mapbox.geojson.Point
import com.mapbox.maps.StylePropertyValue
import com.mapbox.maps.StylePropertyValueKind
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineGranularDistances
import com.mapbox.navigation.ui.maps.route.line.model.VanishingPointState
import com.mapbox.navigation.ui.maps.route.line.model.VanishingRouteLineExpressions
import com.mapbox.navigation.ui.utils.internal.ifNonNull
import com.mapbox.navigation.utils.internal.logD

/**
 * This class implements a feature that can change the appearance of the route line behind the puck.
 * The route line behind the puck can be configured to be transparent or a specified color and
 * will update during navigation.
 *
 * To enable this feature add an instance of this class to the constructor of the MapboxRouteLineApi
 * class. Be sure to send route progress updates and location updates from a
 * OnIndicatorPositionChangedListener to the MapboxRouteLineApi. See the documentation for more
 * information.
 */
internal class VanishingRouteLine() {

    /**
     * The index of the upcoming segment of the road. A segment is an element between two geometry points in the route,
     * see [RouteLineGranularDistances.routeDistances].
     */
    var upcomingRouteGeometrySegmentIndex: Int? = null

    /**
     * a value representing the percentage distance traveled
     */
    var vanishPointOffset: Double = 0.0

    /**
     * the vanishing point state which influences the behavior of the vanishing point
     * calculation
     */
    var vanishingPointState = VanishingPointState.DISABLED
        private set

    /**
     * Updates this instance with a route progress state from a route progress.
     *
     * @param routeProgressState a state from a route progress
     */
    fun updateVanishingPointState(routeProgressState: RouteProgressState) {
        vanishingPointState = when (routeProgressState) {
            RouteProgressState.TRACKING -> VanishingPointState.ENABLED
            RouteProgressState.COMPLETE -> VanishingPointState.ONLY_INCREASE_PROGRESS
            else -> VanishingPointState.DISABLED
        }
    }

    internal fun getOffset(
        point: Point,
        granularDistances: RouteLineGranularDistances,
        upcomingGeoIndex: Int,
    ): Double? {
        val upcomingDistance = granularDistances.routeDistances.getOrNull(
            upcomingGeoIndex,
        )
        if (upcomingDistance == null) {
            logD(
                "Upcoming route line index is null.",
                "VanishingRouteLine",
            )
            return null
        }

        if (upcomingGeoIndex > 0) {
            val distanceToLine = MapboxRouteLineUtils.findDistanceToNearestPointOnCurrentLine(
                point,
                granularDistances,
                upcomingGeoIndex,
            )
            if (
                distanceToLine >
                RouteLayerConstants.ROUTE_LINE_UPDATE_MAX_DISTANCE_THRESHOLD_IN_METERS
            ) {
                return null
            }
        }
        /**
         * Take the remaining distance from the upcoming point on the route and extends it
         * by the exact position of the puck.
         */
        val remainingDistance = upcomingDistance.distanceRemaining +
            MapboxRouteLineUtils.calculateDistance(
                upcomingDistance.point,
                point,
            )

        /**
         * Calculate the percentage of the route traveled and update the expression.
         */
        val offset = if (granularDistances.completeDistance >= remainingDistance) {
            (1.0 - remainingDistance / granularDistances.completeDistance)
        } else {
            0.0
        }

        if (vanishingPointState == VanishingPointState.ONLY_INCREASE_PROGRESS &&
            vanishPointOffset > offset
        ) {
            return null
        }
        return offset
    }

    internal fun getTraveledRouteLineExpressions(
        point: Point,
        granularDistances: RouteLineGranularDistances,
    ): VanishingRouteLineExpressions? {
        return ifNonNull(
            upcomingRouteGeometrySegmentIndex,
        ) { index ->
            ifNonNull(getOffset(point, granularDistances, index)) { offset ->
                getTraveledRouteLineExpressions(offset)
            }
        }
    }

    internal fun getTraveledRouteLineExpressions(
        offset: Double,
    ): VanishingRouteLineExpressions {
        vanishPointOffset = offset
        // Use reversed geometry, that's why 1.0 - offset is used here.
        val value = StylePropertyValue(
            Value(1.0 - offset),
            StylePropertyValueKind.CONSTANT,
        )
        val trafficLineExpressionCommandHolder = RouteLineValueCommandHolder(
            LightRouteLineValueProvider { value },
            LineTrimCommandApplier(),
        )
        val routeLineValueCommandHolder = RouteLineValueCommandHolder(
            LightRouteLineValueProvider { value },
            LineTrimCommandApplier(),
        )
        val routeLineCasingExpressionCommandHolder = RouteLineValueCommandHolder(
            LightRouteLineValueProvider { value },
            LineTrimCommandApplier(),
        )
        val restrictedRoadExpressionCommandHolder = RouteLineValueCommandHolder(
            LightRouteLineValueProvider { value },
            LineTrimCommandApplier(),
        )
        return VanishingRouteLineExpressions(
            trafficLineExpressionCommandHolder,
            routeLineValueCommandHolder,
            routeLineCasingExpressionCommandHolder,
            restrictedRoadExpressionCommandHolder,
        )
    }
}
