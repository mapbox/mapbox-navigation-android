package com.mapbox.navigation.ui.maps.route.line.api

import android.graphics.Color
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.ui.base.internal.model.route.RouteConstants
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils.parseRoutePoints
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineExpressionData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineGranularDistances
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources
import com.mapbox.navigation.ui.maps.route.line.model.RoutePoints
import com.mapbox.navigation.ui.maps.route.line.model.VanishingPointState
import com.mapbox.navigation.ui.maps.route.line.model.VanishingRouteLineExpressions
import com.mapbox.navigation.ui.utils.internal.ifNonNull
import com.mapbox.navigation.utils.internal.LoggerProvider
import com.mapbox.navigation.utils.internal.ThreadController
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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

    private val jobControl = ThreadController.getMainScopeAndRootJob()

    /**
     * the route points for the indicated primary route
     */
    var primaryRoutePoints: RoutePoints? = null
        private set
    internal var primaryRouteLineGranularDistances: RouteLineGranularDistances? = null

    /**
     * the distance index used for calculating the point at which the primary route line
     * should change its appearance
     */
    var primaryRouteRemainingDistancesIndex: Int? = null

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
     * Initializes this class with the active primary route.
     */
    fun initWithRoute(route: DirectionsRoute) {
        clear()
        jobControl.job.cancelChildren()
        jobControl.scope.launch {
            val parsedRoutePointsDef = async(ThreadController.IODispatcher) {
                parseRoutePoints(route)
            }
            val parsedRoutePoints = parsedRoutePointsDef.await()

            val granularDistancesDef = async(ThreadController.IODispatcher) {
                MapboxRouteLineUtils.calculateRouteGranularDistances(
                    parsedRoutePoints?.flatList
                        ?: emptyList()
                )
            }
            val granularDistances = granularDistancesDef.await()

            if (coroutineContext.isActive) {
                primaryRoutePoints = parsedRoutePoints
                primaryRouteLineGranularDistances = granularDistances
            }
        }
    }

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

    internal fun getTraveledRouteLineExpressions(
        point: Point,
        routeLineExpressionData: List<RouteLineExpressionData>,
        routeResourceProvider: RouteLineResources,
        activeLegIndex: Int,
    ): VanishingRouteLineExpressions? {
        ifNonNull(
            primaryRouteLineGranularDistances,
            primaryRouteRemainingDistancesIndex
        ) { granularDistances, index ->
            val upcomingIndex = granularDistances.distancesArray[index]
            if (upcomingIndex == null) {
                LoggerProvider.logger.e(
                    Tag("MbxVanishingRouteLine"),
                    Message(
                        """
                       Upcoming route line index is null.
                       primaryRouteLineGranularDistances: $primaryRouteLineGranularDistances
                       primaryRouteRemainingDistancesIndex: $primaryRouteRemainingDistancesIndex
                        """.trimIndent()
                    )
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
                    RouteConstants.ROUTE_LINE_UPDATE_MAX_DISTANCE_THRESHOLD_IN_METERS
                ) {
                    return null
                }
            }
            /**
             * Take the remaining distance from the upcoming point on the route and extends it
             * by the exact position of the puck.
             */
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
            /**
             * Calculate the percentage of the route traveled and update the expression.
             */
            val offset = if (granularDistances.distance >= remainingDistance) {
                (1.0 - remainingDistance / granularDistances.distance)
            } else {
                0.0
            }

            if (vanishingPointState == VanishingPointState.ONLY_INCREASE_PROGRESS &&
                vanishPointOffset > offset
            ) {
                return null
            }
            vanishPointOffset = offset

            val trafficLineExpression = MapboxRouteLineUtils.getTrafficLineExpression(
                offset,
                routeResourceProvider.routeLineColorResources.routeLineTraveledColor,
                routeResourceProvider.routeLineColorResources.routeUnknownTrafficColor,
                routeLineExpressionData
            )

            val routeLineExpression = MapboxRouteLineUtils.getRouteLineExpression(
                offset,
                routeLineExpressionData,
                routeResourceProvider.routeLineColorResources.routeLineTraveledColor,
                routeResourceProvider.routeLineColorResources.routeDefaultColor,
                routeResourceProvider.routeLineColorResources.inActiveRouteLegsColor,
                activeLegIndex
            )
            val routeLineCasingExpression = MapboxRouteLineUtils.getRouteLineExpression(
                offset,
                routeLineExpressionData,
                routeResourceProvider.routeLineColorResources.routeLineTraveledCasingColor,
                routeResourceProvider.routeLineColorResources.routeCasingColor,
                Color.TRANSPARENT,
                activeLegIndex
            )

            return VanishingRouteLineExpressions(
                trafficLineExpression,
                routeLineExpression,
                routeLineCasingExpression
            )
        }
        return null
    }

    /**
     * Clears the state stored in this instance.
     */
    fun clear() {
        primaryRoutePoints = null
        primaryRouteLineGranularDistances = null
    }
}
