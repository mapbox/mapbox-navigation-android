package com.mapbox.navigation.ui.maps.route.line.api

import android.graphics.Color
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.style.expressions.dsl.generated.literal
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.ui.maps.internal.route.line.LocationSearchTree
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants
import com.mapbox.navigation.ui.maps.route.line.model.ExtractedRouteRestrictionData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineDistancesIndex
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineExpressionData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineGranularDistances
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineTrimExpressionProvider
import com.mapbox.navigation.ui.maps.route.line.model.VanishingPointState
import com.mapbox.navigation.ui.maps.route.line.model.VanishingRouteLineExpressions
import com.mapbox.navigation.ui.utils.internal.ifNonNull
import com.mapbox.navigation.utils.internal.logD
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement

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

    fun getTraveledRouteLineExpressions(
        point: Point
    ): VanishingRouteLineExpressions? {
        return ifNonNull(getOffset(point)) { offset ->
            getTraveledRouteLineExpressions(offset)
        }
    }

    fun getTraveledRouteLineExpressions(offset: Double): VanishingRouteLineExpressions {
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
        return VanishingRouteLineExpressions(
            trafficLineExpressionProvider,
            routeLineExpressionProvider,
            routeLineCasingExpressionProvider,
            restrictedRoadExpressionProvider
        )
    }

    internal fun getTraveledRouteLineExpressions(
        point: Point,
        routeLineExpressionData: List<RouteLineExpressionData>,
        restrictedLineExpressionData: List<ExtractedRouteRestrictionData>?,
        routeResourceProvider: RouteLineResources,
        activeLegIndex: Int,
        softGradientTransition: Double,
        useSoftGradient: Boolean,
    ): VanishingRouteLineExpressions? {
        return ifNonNull(getOffset(point)) { offset ->
            vanishPointOffset = offset
            val trafficLineExpressionProvider = if (useSoftGradient) {
                {
                    MapboxRouteLineUtils.getTrafficLineExpressionSoftGradient(
                        offset,
                        routeResourceProvider.routeLineColorResources.routeLineTraveledColor,
                        routeResourceProvider
                            .routeLineColorResources
                            .routeUnknownCongestionColor,
                        softGradientTransition,
                        routeLineExpressionData
                    )
                }
            } else {
                {
                    MapboxRouteLineUtils.getTrafficLineExpression(
                        offset,
                        routeResourceProvider.routeLineColorResources.routeLineTraveledColor,
                        routeResourceProvider
                            .routeLineColorResources
                            .routeUnknownCongestionColor,
                        routeLineExpressionData
                    )
                }
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
                    Color.TRANSPARENT,
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


    ////////////

    private var granularDistances: RouteLineGranularDistances? = null
    private val locationSearchTree = LocationSearchTree<RouteLineDistancesIndex>()
    private var stepsPoints: List<Array<RouteLineDistancesIndex>> = emptyList()
    private val stepPointsInTree = mutableListOf<Array<RouteLineDistancesIndex>>()
    private var indexOfLastStepPointsLoadedInTree = 0

    fun setGranularDistances(distances: RouteLineGranularDistances) {
        granularDistances = distances
        stepsPoints = granularDistances!!.stepsDistances.flatten() // todo test for multileg routes
        locationSearchTree.clear()
        stepPointsInTree.clear()
        indexOfLastStepPointsLoadedInTree = 0

        if (stepsPoints.isNotEmpty()) {
            val firstSteps = stepsPoints.first()
            val fillerPoints = MapboxRouteLineUtils.getFillerPointsForStepPoints(firstSteps)
            locationSearchTree.addAll(fillerPoints)
            stepPointsInTree.add(firstSteps)
        }
    }

    fun getOffset(point: Point): Double? {
        val offset = ifNonNull(locationSearchTree.getNearestNeighbor(point), granularDistances) { closestPoint, distances ->
            //todo check max distance threshold
            (1.0 - closestPoint.distanceRemaining / distances.completeDistance)
        }
        trimTree(point)
        return offset
    }

    // todo put this on background thread
    private fun trimTree(point: Point) {
        //if getting close to the last step point, load the points for the next step
        //and remove the points long since passed.
        if (stepPointsInTree.isNotEmpty()) {
            val nearEndStepPoint = stepPointsInTree.last()[stepPointsInTree.last().lastIndex]
            val distanceToNearEndStepPoint = TurfMeasurement.distance(point, nearEndStepPoint.point, TurfConstants.UNIT_METERS)
            if (distanceToNearEndStepPoint <= 10.0) {
                if (indexOfLastStepPointsLoadedInTree < stepsPoints.size) {
                    val nextSteps = stepsPoints[indexOfLastStepPointsLoadedInTree + 1]
                    val fillerPoints = MapboxRouteLineUtils.getFillerPointsForStepPoints(nextSteps)
                    locationSearchTree.addAll(fillerPoints)
                    stepPointsInTree.add(nextSteps)
                    indexOfLastStepPointsLoadedInTree++
                    if (stepPointsInTree.size == 3) {
                        val pointsToDrop = stepPointsInTree.removeAt(0)
                        locationSearchTree.removeAll(pointsToDrop.toList())
                    }
                }
            }
        }
    }
}
