package com.mapbox.navigation.ui.maps.route.routeline.internal

import androidx.annotation.ColorInt
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.ui.internal.route.RouteConstants.ALTERNATIVE_ROUTE_CASING_LAYER_ID
import com.mapbox.navigation.ui.internal.route.RouteConstants.ALTERNATIVE_ROUTE_LAYER_ID
import com.mapbox.navigation.ui.internal.route.RouteConstants.HEAVY_CONGESTION_VALUE
import com.mapbox.navigation.ui.internal.route.RouteConstants.MAX_ELAPSED_SINCE_INDEX_UPDATE_NANO
import com.mapbox.navigation.ui.internal.route.RouteConstants.MODERATE_CONGESTION_VALUE
import com.mapbox.navigation.ui.internal.route.RouteConstants.PRIMARY_ROUTE_CASING_LAYER_ID
import com.mapbox.navigation.ui.internal.route.RouteConstants.PRIMARY_ROUTE_LAYER_ID
import com.mapbox.navigation.ui.internal.route.RouteConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID
import com.mapbox.navigation.ui.internal.route.RouteConstants.ROUTE_LINE_UPDATE_MAX_DISTANCE_THRESHOLD_IN_METERS
import com.mapbox.navigation.ui.internal.route.RouteConstants.SEVERE_CONGESTION_VALUE
import com.mapbox.navigation.ui.internal.route.RouteConstants.UNKNOWN_CONGESTION_VALUE
import com.mapbox.navigation.ui.internal.route.RouteConstants.WAYPOINT_LAYER_ID
import com.mapbox.navigation.ui.maps.route.routeline.api.RouteLineActions
import com.mapbox.navigation.ui.maps.route.routeline.api.RouteLineResourceProvider
import com.mapbox.navigation.ui.maps.route.routeline.internal.MapboxRouteLineUtils.calculateDistance
import com.mapbox.navigation.ui.maps.route.routeline.internal.MapboxRouteLineUtils.calculateRouteGranularDistances
import com.mapbox.navigation.ui.maps.route.routeline.internal.MapboxRouteLineUtils.calculateRouteLineSegments
import com.mapbox.navigation.ui.maps.route.routeline.internal.MapboxRouteLineUtils.findDistanceToNearestPointOnCurrentLine
import com.mapbox.navigation.ui.maps.route.routeline.internal.MapboxRouteLineUtils.getIdentifiableRouteFeatureDataProvider
import com.mapbox.navigation.ui.maps.route.routeline.internal.MapboxRouteLineUtils.getRouteFeatureDataProvider
import com.mapbox.navigation.ui.maps.route.routeline.internal.MapboxRouteLineUtils.getTrafficLineExpression
import com.mapbox.navigation.ui.maps.route.routeline.internal.MapboxRouteLineUtils.getVanishingRouteLineExpression
import com.mapbox.navigation.ui.maps.route.routeline.internal.MapboxRouteLineUtils.parseRoutePoints
import com.mapbox.navigation.ui.maps.route.routeline.model.IdentifiableRoute
import com.mapbox.navigation.ui.maps.route.routeline.model.RouteFeatureData
import com.mapbox.navigation.ui.maps.route.routeline.model.RouteLineExpressionData
import com.mapbox.navigation.ui.maps.route.routeline.model.RouteLineGranularDistances
import com.mapbox.navigation.ui.maps.route.routeline.model.RouteLineState
import com.mapbox.navigation.ui.maps.route.routeline.model.RoutePoints
import com.mapbox.navigation.ui.maps.route.routeline.model.VanishingPointState
import com.mapbox.navigation.utils.internal.ifNonNull
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfException
import com.mapbox.turf.TurfMisc
import timber.log.Timber

class MapboxRouteLineActions(
    private val routeResourceProvider: RouteLineResourceProvider
) : RouteLineActions {
    private val routeLineExpressionData: MutableList<RouteLineExpressionData> =
        mutableListOf<RouteLineExpressionData>()
    private val directionsRoutes: MutableList<DirectionsRoute> = mutableListOf()
    private var primaryRoute: DirectionsRoute? = null
    private var lastIndexUpdateTimeNano: Long = 0
    private var primaryRouteLineGranularDistances: RouteLineGranularDistances? = null
    private var primaryRouteRemainingDistancesIndex: Int? = null
    private var primaryRoutePoints: RoutePoints? = null
    private var vanishingPointState = VanishingPointState.DISABLED
    private var vanishPointOffset: Double = 0.0

    internal fun getVanishPointOffset() = vanishPointOffset

    override fun getPrimaryRoute() = primaryRoute

    override fun getUpdateViewStyleState(style: Style): RouteLineState.UpdateViewStyleState {
        return RouteLineState.UpdateViewStyleState(style)
    }

    override fun updateVanishingPointState(
        routeProgressState: RouteProgressState
    ): RouteLineState.UpdateVanishingPointState {
        vanishingPointState = when (routeProgressState) {
            RouteProgressState.LOCATION_TRACKING -> VanishingPointState.ENABLED
            RouteProgressState.ROUTE_COMPLETE -> VanishingPointState.ONLY_INCREASE_PROGRESS
            else -> VanishingPointState.DISABLED
        }
        return RouteLineState.UpdateVanishingPointState(vanishingPointState)
    }

    override fun getHidePrimaryRouteState(): RouteLineState.UpdateLayerVisibilityState {
        val layerModifications = getShowPrimaryRouteLayerModifications(Visibility.NONE)
        return RouteLineState.UpdateLayerVisibilityState(layerModifications)
    }

    override fun getShowPrimaryRouteState(): RouteLineState.UpdateLayerVisibilityState {
        val layerModifications = getShowPrimaryRouteLayerModifications(Visibility.VISIBLE)
        return RouteLineState.UpdateLayerVisibilityState(layerModifications)
    }

    private fun getShowPrimaryRouteLayerModifications(visibility: Visibility):
        List<Pair<String, Visibility>> {
            return listOf(
                Pair(PRIMARY_ROUTE_TRAFFIC_LAYER_ID, visibility),
                Pair(PRIMARY_ROUTE_LAYER_ID, visibility),
                Pair(PRIMARY_ROUTE_CASING_LAYER_ID, visibility)
            )
        }

    override fun getHideAlternativeRoutesState(): RouteLineState.UpdateLayerVisibilityState {
        val layerModifications = getShowAlternateRoutesLayerModifications(Visibility.NONE)
        return RouteLineState.UpdateLayerVisibilityState(layerModifications)
    }

    override fun getShowAlternativeRoutesState(): RouteLineState.UpdateLayerVisibilityState {
        val layerModifications = getShowAlternateRoutesLayerModifications(Visibility.VISIBLE)
        return RouteLineState.UpdateLayerVisibilityState(layerModifications)
    }

    private fun getShowAlternateRoutesLayerModifications(
        visibility: Visibility
    ): List<Pair<String, Visibility>> {
        return listOf(
            Pair(ALTERNATIVE_ROUTE_LAYER_ID, visibility),
            Pair(ALTERNATIVE_ROUTE_CASING_LAYER_ID, visibility)
        )
    }

    override fun getHideOriginAndDestinationPointsState():
        RouteLineState.UpdateLayerVisibilityState {
            val layerModifications = listOf(Pair(WAYPOINT_LAYER_ID, Visibility.NONE))
            return RouteLineState.UpdateLayerVisibilityState(layerModifications)
        }

    override fun getShowOriginAndDestinationPointsState():
        RouteLineState.UpdateLayerVisibilityState {
            val layerModifications = listOf(Pair(WAYPOINT_LAYER_ID, Visibility.VISIBLE))
            return RouteLineState.UpdateLayerVisibilityState(layerModifications)
        }

    override fun getUpdatePrimaryRouteIndexState(
        route: DirectionsRoute
    ): RouteLineState.DrawRouteState {
        primaryRoute = route
        directionsRoutes.remove(route)
        directionsRoutes.add(0, route)
        val featureDataProvider: () -> List<RouteFeatureData> =
            getRouteFeatureDataProvider(directionsRoutes)
        return buildDrawRoutesState(featureDataProvider)
    }

    override fun getDrawRoutesState(
        newRoutes: List<DirectionsRoute>
    ): RouteLineState.DrawRouteState {
        directionsRoutes.clear()
        directionsRoutes.addAll(newRoutes)
        primaryRoute = newRoutes.firstOrNull()
        vanishPointOffset = 0.0
        val featureDataProvider: () -> List<RouteFeatureData> =
            getRouteFeatureDataProvider(directionsRoutes)
        return buildDrawRoutesState(featureDataProvider)
    }

    override fun getDrawIdentifiableRoutesState(
        newRoutes: List<IdentifiableRoute>
    ): RouteLineState.DrawRouteState {
        val routes = newRoutes.map { it.route }
        directionsRoutes.clear()
        directionsRoutes.addAll(routes)
        primaryRoute = routes.firstOrNull()
        vanishPointOffset = 0.0
        val featureDataProvider: () -> List<RouteFeatureData> =
            getIdentifiableRouteFeatureDataProvider(newRoutes)
        return buildDrawRoutesState(featureDataProvider)
    }

    override fun redraw(): RouteLineState.DrawRouteState {
        val featureDataProvider: () -> List<RouteFeatureData> =
            getRouteFeatureDataProvider(directionsRoutes)
        return buildDrawRoutesState(featureDataProvider)
    }

    private fun buildDrawRoutesState(
        featureDataProvider: () -> List<RouteFeatureData>
    ): RouteLineState.DrawRouteState {
        val routeData = featureDataProvider()
        val partitionedRoutes = routeData.partition { it.route == directionsRoutes.first() }
        val segments: List<RouteLineExpressionData> =
            partitionedRoutes.first.firstOrNull()?.route?.run {
                calculateRouteLineSegments(
                    this,
                    routeResourceProvider.getTrafficBackfillRoadClasses(),
                    true,
                    ::getRouteColorForCongestion
                )
            } ?: listOf()
        routeLineExpressionData.clear()
        routeLineExpressionData.addAll(segments)
        val trafficLineExpression = getTrafficLineExpression(
            vanishPointOffset,
            segments,
            routeResourceProvider.getRouteUnknownTrafficColor()
        )
        val routeLineExpression = getVanishingRouteLineExpression(
            vanishPointOffset,
            routeResourceProvider.getRouteLineTraveledColor(),
            routeResourceProvider.getRouteLineBaseColor()
        )
        val routeLineCasingExpression = getVanishingRouteLineExpression(
            vanishPointOffset,
            routeResourceProvider.getRouteLineTraveledColor(),
            routeResourceProvider.getRouteLineCasingColor()
        )

        val alternativeRouteFeatures = partitionedRoutes.second.mapNotNull {
            it.featureCollection.features()
        }.flatten()
        val alternativeRouteFeatureCollection = FeatureCollection.fromFeatures(
            alternativeRouteFeatures
        )
        val wayPointsFeatureCollection: FeatureCollection =
            partitionedRoutes.first.firstOrNull()?.route?.run {
                MapboxRouteLineUtils.buildWayPointFeatureCollection(this)
            } ?: FeatureCollection.fromFeatures(listOf())
        partitionedRoutes.first.firstOrNull()?.let {
            initPrimaryRoutePoints(it.route)
        }

        return RouteLineState.DrawRouteState(
            partitionedRoutes.first.firstOrNull()?.featureCollection
                ?: FeatureCollection.fromFeatures(
                    listOf()
                ),
            trafficLineExpression,
            routeLineExpression,
            routeLineCasingExpression,
            alternativeRouteFeatureCollection,
            wayPointsFeatureCollection
        )
    }

    override fun getTraveledRouteLineUpdate(
        point: Point
    ): RouteLineState.TraveledRouteLineUpdateState {
        if (vanishingPointState == VanishingPointState.DISABLED ||
            System.nanoTime() - lastIndexUpdateTimeNano > MAX_ELAPSED_SINCE_INDEX_UPDATE_NANO
        ) {
            return RouteLineState.TraveledRouteLineUpdateState.TraveledRouteLineNoUpdate()
        }

        ifNonNull(
            primaryRouteLineGranularDistances,
            primaryRouteRemainingDistancesIndex
        ) { granularDistances, index ->
            val upcomingIndex = granularDistances.distancesArray[index]
            if (upcomingIndex == null) {
                Timber.e(
                    """
                       Upcoming route line index is null.
                       primaryRouteLineGranularDistances: $primaryRouteLineGranularDistances
                       primaryRouteRemainingDistancesIndex: $primaryRouteRemainingDistancesIndex
                    """.trimIndent()
                )
                return RouteLineState.TraveledRouteLineUpdateState.TraveledRouteLineNoUpdate()
            }
            val upcomingPoint = upcomingIndex.point
            if (index > 0) {
                val distanceToLine = findDistanceToNearestPointOnCurrentLine(
                    point,
                    granularDistances,
                    index
                )
                if (distanceToLine > ROUTE_LINE_UPDATE_MAX_DISTANCE_THRESHOLD_IN_METERS) {
                    return RouteLineState.TraveledRouteLineUpdateState.TraveledRouteLineNoUpdate()
                }
            }
            /**
             * Take the remaining distance from the upcoming point on the route and extends it
             * by the exact position of the puck.
             */
            val remainingDistance =
                upcomingIndex.distanceRemaining + calculateDistance(upcomingPoint, point)

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
                return RouteLineState.TraveledRouteLineUpdateState.TraveledRouteLineNoUpdate()
            }
            vanishPointOffset = offset
            val trafficLineExpression = getTrafficLineExpression(
                offset,
                routeLineExpressionData,
                routeResourceProvider.getRouteUnknownTrafficColor()
            )
            val routeLineExpression = getVanishingRouteLineExpression(
                offset,
                routeResourceProvider.getRouteLineTraveledColor(),
                routeResourceProvider.getRouteLineBaseColor()
            )
            val routeLineCasingExpression = getVanishingRouteLineExpression(
                offset,
                routeResourceProvider.getRouteLineTraveledColor(),
                routeResourceProvider.getRouteLineCasingColor()
            )
            return RouteLineState.TraveledRouteLineUpdateState.TraveledRouteLineUpdate(
                trafficLineExpression,
                routeLineExpression,
                routeLineCasingExpression
            )
        }
        return RouteLineState.TraveledRouteLineUpdateState.TraveledRouteLineNoUpdate()
    }

    override fun clearRouteData(): RouteLineState.ClearRouteDataState {
        vanishPointOffset = 0.0
        directionsRoutes.clear()
        routeLineExpressionData.clear()
        primaryRoutePoints = null
        primaryRouteLineGranularDistances = null
        return RouteLineState.ClearRouteDataState(
            FeatureCollection.fromFeatures(listOf()),
            FeatureCollection.fromFeatures(listOf()),
            FeatureCollection.fromFeatures(listOf())
        )
    }

    override fun setVanishingOffset(offset: Double): RouteLineState.TraveledRouteLineUpdateState {
        vanishPointOffset = offset
        return if (offset >= 0) {
            val trafficLineExpression = getTrafficLineExpression(
                offset,
                routeLineExpressionData,
                routeResourceProvider.getRouteUnknownTrafficColor()
            )
            val routeLineExpression = getVanishingRouteLineExpression(
                offset,
                routeResourceProvider.getRouteLineTraveledColor(),
                routeResourceProvider.getRouteLineBaseColor()
            )
            val routeLineCasingExpression = getVanishingRouteLineExpression(
                offset,
                routeResourceProvider.getRouteLineTraveledColor(),
                routeResourceProvider.getRouteLineCasingColor()
            )
            RouteLineState.TraveledRouteLineUpdateState.TraveledRouteLineUpdate(
                trafficLineExpression,
                routeLineExpression,
                routeLineCasingExpression
            )
        } else {
            RouteLineState.TraveledRouteLineUpdateState.TraveledRouteLineNoUpdate()
        }
    }

    /**
     * Tries to find and cache the index of the upcoming [RouteLineDistancesIndex].
     */
    override fun updateUpcomingRoutePointIndex(
        routeProgress: RouteProgress
    ): RouteLineState.UnitState {
        ifNonNull(
            routeProgress.currentLegProgress,
            routeProgress.currentLegProgress?.currentStepProgress,
            primaryRoutePoints
        ) { currentLegProgress, currentStepProgress, completeRoutePoints ->
            var allRemainingPoints = 0
            /**
             * Finds the count of remaining points in the current step.
             *
             * TurfMisc.lineSliceAlong places an additional point at index 0 to mark the precise
             * cut-off point which we can safely ignore.
             * We'll add the distance from the upcoming point to the current's puck position later.
             */
            allRemainingPoints += try {
                TurfMisc.lineSliceAlong(
                    LineString.fromLngLats(currentStepProgress.stepPoints ?: emptyList()),
                    currentStepProgress.distanceTraveled.toDouble(),
                    currentStepProgress.step?.distance() ?: 0.0,
                    TurfConstants.UNIT_METERS
                ).coordinates().drop(1).size
            } catch (e: TurfException) {
                0
            }

            /**
             * Add to the count of remaining points all of the remaining points on the current leg,
             * after the current step.
             */
            val currentLegSteps = completeRoutePoints.nestedList[currentLegProgress.legIndex]
            allRemainingPoints += if (currentStepProgress.stepIndex < currentLegSteps.size) {
                currentLegSteps.slice(
                    currentStepProgress.stepIndex + 1 until currentLegSteps.size - 1
                ).flatten().size
            } else {
                0
            }

            /**
             * Add to the count of remaining points all of the remaining legs.
             */
            for (i in currentLegProgress.legIndex + 1 until completeRoutePoints.nestedList.size) {
                allRemainingPoints += completeRoutePoints.nestedList[i].flatten().size
            }

            /**
             * When we know the number of remaining points and the number of all points,
             * calculate the index of the upcoming point.
             */
            val allPoints = completeRoutePoints.flatList.size
            primaryRouteRemainingDistancesIndex = allPoints - allRemainingPoints - 1
        } ?: run { primaryRouteRemainingDistancesIndex = null }

        lastIndexUpdateTimeNano = System.nanoTime()
        return RouteLineState.UnitState()
    }

    /**
     * Returns the color that is used to represent traffic congestion.
     *
     * @param congestionValue as string value coming from the DirectionsRoute
     * @param isPrimaryRoute indicates if the congestion value for the primary route should
     * be returned or the color for an alternative route.
     */
    @ColorInt
    private fun getRouteColorForCongestion(congestionValue: String, isPrimaryRoute: Boolean): Int {
        return when (isPrimaryRoute) {
            true -> when (congestionValue) {
                MODERATE_CONGESTION_VALUE -> routeResourceProvider.getRouteModerateTrafficColor()
                HEAVY_CONGESTION_VALUE -> routeResourceProvider.getRouteHeavyTrafficColor()
                SEVERE_CONGESTION_VALUE -> routeResourceProvider.getRouteSevereTrafficColor()
                UNKNOWN_CONGESTION_VALUE -> routeResourceProvider.getRouteUnknownTrafficColor()
                else -> routeResourceProvider.getRouteLowTrafficColor()
            }
            false -> when (congestionValue) {
                MODERATE_CONGESTION_VALUE ->
                    routeResourceProvider.getAlternativeRouteModerateTrafficColor()
                HEAVY_CONGESTION_VALUE ->
                    routeResourceProvider.getAlternativeRouteHeavyTrafficColor()
                SEVERE_CONGESTION_VALUE ->
                    routeResourceProvider.getAlternativeRouteSevereTrafficColor()
                UNKNOWN_CONGESTION_VALUE ->
                    routeResourceProvider.getAlternativeRouteUnknownTrafficColor()
                else -> routeResourceProvider.getAlternativeRouteLineBaseColor()
            }
        }
    }

    private fun initPrimaryRoutePoints(route: DirectionsRoute) {
        primaryRoutePoints = parseRoutePoints(route)
        primaryRouteLineGranularDistances =
            calculateRouteGranularDistances(primaryRoutePoints?.flatList ?: emptyList())
    }
}
