package com.mapbox.navigation.ui.maps.route.line.api

import com.mapbox.geojson.Point
import com.mapbox.maps.extension.style.expressions.dsl.generated.literal
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants
import com.mapbox.navigation.ui.maps.route.line.model.ExtractedRouteRestrictionData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineExpressionData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineGranularDistances
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineTrimExpressionProvider
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
internal class VanishingRouteLine {

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

    private fun getOffset(
        point: Point,
        granularDistances: RouteLineGranularDistances,
        index: Int
    ): Double? {
        val upcomingIndex = granularDistances.routeDistances.getOrNull(index)
        if (upcomingIndex == null) {
            logD(
                "Upcoming route line index is null.",
                "VanishingRouteLine"
            )
            return null
        }
        val upcomingPoint = upcomingIndex.point
        if (index > 0) {
            val distanceToLine = MapboxRouteLineUtils.findDistanceToNearestPointOnCurrentLine(
                point,
                granularDistances,
                index
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
        val remainingDistance =
            upcomingIndex.distanceRemaining + MapboxRouteLineUtils.calculateDistance(
                upcomingPoint,
                point
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
        granularDistances: RouteLineGranularDistances
    ): VanishingRouteLineExpressions? {
        return ifNonNull(upcomingRouteGeometrySegmentIndex) { index ->
            ifNonNull(getOffset(point, granularDistances, index)) { offset ->
                vanishPointOffset = offset
                val trimmedOffsetExpression = literal(listOf(0.0, offset))
                val trafficLineExpressionProvider = RouteLineTrimExpressionProvider {
                    trimmedOffsetExpression
                }
                val routeLineExpressionProvider = RouteLineTrimExpressionProvider {
                    trimmedOffsetExpression
                }
                val routeLineCasingExpressionProvider = RouteLineTrimExpressionProvider {
                    trimmedOffsetExpression
                }
                val restrictedRoadExpressionProvider = RouteLineTrimExpressionProvider {
                    trimmedOffsetExpression
                }
                VanishingRouteLineExpressions(
                    trafficLineExpressionProvider,
                    routeLineExpressionProvider,
                    routeLineCasingExpressionProvider,
                    restrictedRoadExpressionProvider
                )
            }
        }
    }

    internal fun getTraveledRouteLineExpressions(
        point: Point,
        granularDistances: RouteLineGranularDistances,
        routeLineExpressionData: List<RouteLineExpressionData>,
        restrictedLineExpressionData: List<ExtractedRouteRestrictionData>?,
        routeResourceProvider: RouteLineResources,
        activeLegIndex: Int,
        softGradientTransition: Double,
        useSoftGradient: Boolean,
    ): VanishingRouteLineExpressions? {
        return ifNonNull(
            upcomingRouteGeometrySegmentIndex
        ) { index ->
            ifNonNull(getOffset(point, granularDistances, index)) { offset ->
                vanishPointOffset = offset
                val trafficLineExpressionProvider = {
                    MapboxRouteLineUtils.getTrafficLineExpression(
                        offset,
                        routeResourceProvider.routeLineColorResources.routeLineTraveledColor,
                        routeResourceProvider
                            .routeLineColorResources
                            .routeUnknownCongestionColor,
                        softGradientTransition,
                        useSoftGradient,
                        routeLineExpressionData
                    )
                }
                val routeLineExpressionProvider = {
                    MapboxRouteLineUtils.getRouteLineExpression(
                        offset,
                        routeLineExpressionData,
                        routeResourceProvider.routeLineColorResources.routeLineTraveledColor,
                        routeResourceProvider.routeLineColorResources.routeDefaultColor,
                        routeResourceProvider.routeLineColorResources.inActiveRouteLegsColor,
                        activeLegIndex
                    )
                }
                val routeLineCasingExpressionProvider = {
                    MapboxRouteLineUtils.getRouteLineExpression(
                        offset,
                        routeLineExpressionData,
                        routeResourceProvider.routeLineColorResources.routeLineTraveledCasingColor,
                        routeResourceProvider.routeLineColorResources.routeCasingColor,
                        routeResourceProvider.routeLineColorResources.inActiveRouteLegsCasingColor,
                        activeLegIndex
                    )
                }
                val restrictedRoadExpressionProvider =
                    ifNonNull(restrictedLineExpressionData) { expressionData ->
                        {
                            MapboxRouteLineUtils.getRestrictedLineExpression(
                                offset,
                                activeLegIndex,
                                routeResourceProvider.routeLineColorResources.restrictedRoadColor,
                                expressionData
                            )
                        }
                    }

                VanishingRouteLineExpressions(
                    trafficLineExpressionProvider,
                    routeLineExpressionProvider,
                    routeLineCasingExpressionProvider,
                    restrictedRoadExpressionProvider
                )
            }
        }
    }
}
