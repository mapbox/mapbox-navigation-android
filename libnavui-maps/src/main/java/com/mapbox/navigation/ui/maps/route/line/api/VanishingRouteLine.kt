package com.mapbox.navigation.ui.maps.route.line.api

import android.graphics.Color
import android.util.Log
import android.util.Range
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.style.expressions.dsl.generated.literal
import com.mapbox.navigation.ui.maps.util.LocationSearchTree
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

/**
 * This class implements a feature that can change the appearance of the route line behind the puck.
 * The route line behind the puck can be configured to be transparent or a specified color and
 * will update during navigation.
 *
 * Enable this feature with the [MapboxRouteLineOptions] class.  Be sure to create a
 * [OnIndicatorPositionChangedListener] and pass the values generated to the [MapboxRouteLineApi] instance.
 *
 * See the documentation for more information.
 */
internal class VanishingRouteLine {

    /**
     * a value representing the percentage distance traveled
     */
    var vanishPointOffset: Double = 0.0
        private set

    private var scope: CoroutineScope? = null
    private var granularDistances: RouteLineGranularDistances? = null
    private val locationSearchTree = LocationSearchTree<RouteLineDistancesIndex>()
    private val fillerPointsInTree = CopyOnWriteArrayList<RouteLineDistancesIndex>()
    private var indexOfLastStepPointsLoadedInTree = 0
    private val distanceToLastStepPointInMeters = 20.0
    private var stepPointRange: Range<Int>? = null
    private val stepPointRangeSize = 2
    //private val maxAllowedFillerPointListsInTree = 3
    private val maxTrailingFillerPoints = 10
    private val treeAdjustmentFrequency = 10
    private var treeAdjustmentCounter = 0

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

    /**
     * When the granular distances are received the flatStepDistances are used to generate
     * a range of very granular points along the route.  These granular points are added
     * to a search tree. When a call is made to get the offset for a specific point a
     * nearest neighbor search of the granular points generated is performed. If a neighbor is
     * found within the distance threshold it is used to determine the offset.
     *
     * The range of granular points adjusts at runtime according to the point coming in for the
     * offset calculation. The range begins with the first step points in flatStepDistances.
     * As the route is navigated the range is adjusted to include upcoming step points and
     * the points that have been passed are removed. This constantly adjusting range keeps the
     * number of points to search low to optimize performance.
     */
    fun setGranularDistances(distances: RouteLineGranularDistances) {
        scope?.launch(Dispatchers.Main.immediate) {
            if (distances != granularDistances) {
                granularDistances = distances
                locationSearchTree.clear()
                fillerPointsInTree.clear()
                indexOfLastStepPointsLoadedInTree = 0
                vanishPointOffset = 0.0
                treeAdjustmentCounter = 0

                if (distances.flatStepDistances.isNotEmpty()) {
                    val endRange = if (distances.flatStepDistances.size > stepPointRangeSize) {
                        stepPointRangeSize
                    } else {
                        distances.flatStepDistances.lastIndex
                    }
                    stepPointRange = Range(0, endRange).also {
                        val fillerPoints = getFillerPointsForRange(it, distances.flatStepDistances)
                        locationSearchTree.addAll(fillerPoints)
                        fillerPointsInTree.addAll(fillerPoints)
                    }
                }
            }
        }
    }

    fun getOffset(point: Point): Double? {
        val nearestNeighbor = locationSearchTree.getNearestNeighbor(point)
        val offset = ifNonNull(nearestNeighbor, granularDistances)
        { closestPoint, distances ->
            val distanceBetweenPoints = TurfMeasurement.distance(
                point,
                closestPoint.point,
                TurfConstants.UNIT_METERS
            )
            if (distanceBetweenPoints <= ROUTE_LINE_UPDATE_MAX_DISTANCE_THRESHOLD_IN_METERS ) {
                (1.0 - closestPoint.distanceRemaining / distances.completeDistance)
            } else {
                if (distanceBetweenPoints >= distanceToLastStepPointInMeters) {
                    recalculateRange(point)
                }
                null
            }
        }
        adjustTree(point, nearestNeighbor)
        return offset
    }

    /**
     * It's possible the incoming point is not at the beginning of the route. This method
     * searches the route for the closest step point and creates a point range around it
     * so that the correct offset can be determined. Any/All points in the search tree
     * are removed and the points falling withing the range defined here are added.
     */
    private fun recalculateRange(point: Point) {
        scope?.launch(Dispatchers.Main.immediate) {
            ifNonNull(granularDistances) { distances ->
                val indexOfClosestStepPoint =
                    distances.flatStepDistances.mapIndexed { index, routeLineDistancesIndex ->
                        val dist = TurfMeasurement.distance(
                            point, routeLineDistancesIndex.point,
                            TurfConstants.UNIT_METERS
                        )
                    Pair(index, dist)
                }.filter { it.first >= indexOfLastStepPointsLoadedInTree }.minByOrNull { it.second }
                ifNonNull(indexOfClosestStepPoint) {
                    val endOfRange =
                        if (it.first + stepPointRangeSize < distances.flatStepDistances.lastIndex) {
                            it.first + stepPointRangeSize
                        } else {
                            distances.flatStepDistances.lastIndex
                        }
                    stepPointRange = Range(it.first, endOfRange).also { range ->
                        val fillerPoints = getFillerPointsForRange(
                            range,
                            distances.flatStepDistances
                        )
                        if (fillerPoints.isNotEmpty()) {
                            locationSearchTree.clear()
                            fillerPointsInTree.clear()
                            locationSearchTree.addAll(fillerPoints)
                            fillerPointsInTree.addAll(fillerPoints)
                        }
                        indexOfLastStepPointsLoadedInTree = range.lower
                    }
                }
            }
        }
    }

    /**
     * Gets the generated points between the points in the range so they can be added to the
     * search tree.
     */
    private fun getFillerPointsForRange(
        range: Range<Int>,
        flatStepDistances: Array<RouteLineDistancesIndex>
    ): List<RouteLineDistancesIndex> {
        val fillerSteps = flatStepDistances.copyOfRange(range.lower, range.upper)
        return MapboxRouteLineUtils.getFillerPointsForStepPoints(fillerSteps)
    }

    /**
     * When the incoming point gets close to the last point in the currently defined range
     * the range is redefined with the upcoming points.  The points in the newly defined
     * range are added to the search tree and the points passed are removed.
     */
    private fun adjustTree(point: Point, nearestNeighbor: RouteLineDistancesIndex?) {
        treeAdjustmentCounter++
        if (treeAdjustmentCounter >= treeAdjustmentFrequency) {
            treeAdjustmentCounter = 0
            scope?.launch(Dispatchers.Main.immediate) {
                loadNextRange(point)
                trimTree(nearestNeighbor)
            }
        }
    }

    private fun loadNextRange(point: Point) {
        if (fillerPointsInTree.isNotEmpty()) {
            val nearEndStepPoint = fillerPointsInTree.last()
            val distanceToNearEndStepPoint = TurfMeasurement.distance(
                point,
                nearEndStepPoint.point,
                TurfConstants.UNIT_METERS
            )
            if (distanceToNearEndStepPoint <= distanceToLastStepPointInMeters) {
                stepPointRange = ifNonNull(stepPointRange, granularDistances)
                { currentStepPointRange, distances ->
                    val endOfRange =
                        if (
                            currentStepPointRange.upper + stepPointRangeSize
                            < distances.flatStepDistances.lastIndex
                        ) {
                            currentStepPointRange.upper + stepPointRangeSize
                        } else {
                            distances.flatStepDistances.lastIndex
                        }
                    Range(currentStepPointRange.upper - 1, endOfRange).also {
                        val fillerPoints = getFillerPointsForRange(
                            it,
                            distances.flatStepDistances
                        )
                        if (fillerPoints.isNotEmpty()) {
                            locationSearchTree.addAll(fillerPoints)
                            fillerPointsInTree.addAll(fillerPoints)
                        }
                        indexOfLastStepPointsLoadedInTree = endOfRange
                    }
                }
            }
        }
    }

    private fun trimTree(nearestNeighbor: RouteLineDistancesIndex?) {
        //Log.e("foobar", "trimming tree")
        ifNonNull(nearestNeighbor) {
            val index = fillerPointsInTree.indexOf(it)
            if (index > maxTrailingFillerPoints) {
                fillerPointsInTree.take(index).apply {
                    fillerPointsInTree.removeAll(this)
                    locationSearchTree.removeAll(this)
                }
            }
        }
    }

    //fixme remove this
    fun getFillerPointsInTree(): List<Point> {
        return fillerPointsInTree.map { it.point }
    }
}
