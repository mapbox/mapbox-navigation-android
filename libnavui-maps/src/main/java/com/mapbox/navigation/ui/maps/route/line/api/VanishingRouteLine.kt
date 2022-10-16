package com.mapbox.navigation.ui.maps.route.line.api

import android.graphics.Color
import android.util.Log
import android.util.Range
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.style.expressions.dsl.generated.literal
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.ui.maps.internal.route.line.LocationSearchTree
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.ROUTE_LINE_UPDATE_MAX_DISTANCE_THRESHOLD_IN_METERS
import com.mapbox.navigation.ui.maps.route.line.model.ExtractedRouteRestrictionData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineDistancesIndex
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineExpressionData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineGranularDistances
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineTrimExpressionProvider
import com.mapbox.navigation.ui.maps.route.line.model.VanishingRouteLineExpressions
import com.mapbox.navigation.ui.utils.internal.ifNonNull
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList

// todo adjust doc
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
     * a value representing the percentage distance traveled
     */
    var vanishPointOffset: Double = 0.0
        //private set todo

    var primaryRouteRemainingDistancesIndex: Int? = null //todo delme

    var vanishingPointState: RouteProgressState? = null //todo delme

    fun updateVanishingPointState(state: RouteProgressState) {
        //todo  delme
    }

    private var scope: CoroutineScope? = null

    fun setScope(scope: CoroutineScope) {
        this.scope = scope
    }

    fun getTraveledRouteLineExpressions(
        point: Point
    ): VanishingRouteLineExpressions? {
        return ifNonNull(getOffset(point)) { offset ->
            getTraveledRouteLineExpressions(offset)
        }
    }

    fun getTraveledRouteLineExpressions(offset: Double): VanishingRouteLineExpressions {
        this.vanishPointOffset = offset
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
    private val fillerPointsInTree = mutableListOf<List<RouteLineDistancesIndex>>()
    private var indexOfLastStepPointsLoadedInTree = 0
    private val distanceToLastStepPointInMeters = 30.0
    private var stepPointRange: Range<Int>? = null
    private val stepPointRangeSize = 5
    private val maxAllowedFillerPointListsInTree = 3

    fun setGranularDistances(distances: RouteLineGranularDistances) {
        if (distances != granularDistances) {
            granularDistances = distances
            stepsPoints = distances.stepsDistances.flatten()
            locationSearchTree.clear()
            fillerPointsInTree.clear()
            indexOfLastStepPointsLoadedInTree = 0
            vanishPointOffset = 0.0

            // todo remove logging
            Log.e("foobar", "everything got cleared, starting fresh")

            if (distances.flatStepDistances.isNotEmpty()) {
                val endRange = if (distances.flatStepDistances.size > stepPointRangeSize) {
                    stepPointRangeSize
                } else {
                    distances.flatStepDistances.lastIndex
                }
                stepPointRange = Range(0, endRange).also {
                    val fillerPoints = getFillerPointsForRange(it, distances.flatStepDistances)
                    locationSearchTree.addAll(fillerPoints)
                    fillerPointsInTree.add(fillerPoints)
                }
            }
        }
    }

    private fun getFillerPointsForRange(range: Range<Int>, flatStepDistances: Array<RouteLineDistancesIndex>): List<RouteLineDistancesIndex> {
        val fillerSteps = flatStepDistances.copyOfRange(range.lower, range.upper)
        return MapboxRouteLineUtils.getFillerPointsForStepPoints(fillerSteps)
    }


    fun getOffset(point: Point): Double? {
        val offset = ifNonNull(locationSearchTree.getNearestNeighbor(point), granularDistances)
        { closestPoint, distances ->
            val distanceBetweenPoints = TurfMeasurement.distance(point, closestPoint.point)
            if (distanceBetweenPoints <= ROUTE_LINE_UPDATE_MAX_DISTANCE_THRESHOLD_IN_METERS ) {
                (1.0 - closestPoint.distanceRemaining / distances.completeDistance)
            } else {
                // todo remove logging
                Log.e("foobar", "distance of $distanceBetweenPoints beyond distance threshold")
                Log.e("foobar", "incoming point $point nearest neighbor ${closestPoint.point}")
                null
            }
        }
        trimTree(point)
        return offset
    }

    //todo make private
    fun trimTree(point: Point) {
        //if getting close to the last step point, load the points for the next step range
        //and remove the points long since passed.
        scope?.launch(Dispatchers.Main.immediate) {
            val trimStart = System.currentTimeMillis()
            if (fillerPointsInTree.isNotEmpty() && fillerPointsInTree.last().isNotEmpty()) {
                val nearEndStepPoint = fillerPointsInTree.last().last()
                val distanceToNearEndStepPoint =
                    TurfMeasurement.distance(point, nearEndStepPoint.point, TurfConstants.UNIT_METERS)
                if (distanceToNearEndStepPoint <= distanceToLastStepPointInMeters) {
                    stepPointRange = ifNonNull(stepPointRange, granularDistances) { currentStepPointRange, distances ->
                        val endOfRange = if (currentStepPointRange.upper + stepPointRangeSize < distances.flatStepDistances.lastIndex) {
                            currentStepPointRange.upper + stepPointRangeSize
                        } else {
                            distances.flatStepDistances.lastIndex
                        }
                        Range(currentStepPointRange.upper - 1, endOfRange).also {
                            val fillerPoints = getFillerPointsForRange(it, distances.flatStepDistances)
                            if (fillerPoints.isNotEmpty()) {
                                locationSearchTree.addAll(fillerPoints)
                                fillerPointsInTree.add(fillerPoints)
                            }
                        }
                    }
                    if (fillerPointsInTree.size == maxAllowedFillerPointListsInTree) {
                        val pointsToDrop = fillerPointsInTree.removeFirst()
                        locationSearchTree.removeAll(pointsToDrop)
                    }
                }
            }
            val trimTotal = System.currentTimeMillis() - trimStart
            if (trimTotal > 10) {
                // todo remove logging
                Log.e("foobar", "time to trim tree is ${System.currentTimeMillis() - trimStart}")
            }

        }
    }

    fun deleteMeGetTreePoints(): List<Point> {
        val allPoints = mutableListOf<Point>()
        fillerPointsInTree.forEach { distanceIndexes ->
            distanceIndexes.map { it.point }.forEach {
                allPoints.add(it)
            }
        }
        return allPoints
    }
}
