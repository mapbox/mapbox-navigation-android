package com.mapbox.navigation.ui.maps.route.line.api

import androidx.annotation.ColorInt
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.RenderedQueryOptions
import com.mapbox.maps.ScreenBox
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.ui.base.internal.route.RouteConstants
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteFeatureData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineExpressionData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineState
import com.mapbox.navigation.ui.maps.route.line.model.VanishingPointState
import com.mapbox.navigation.ui.utils.internal.ifNonNull
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfException
import com.mapbox.turf.TurfMisc

/**
 * Responsible for generating route line related data which can be rendered on the map to
 * visualize a line representing a route. The route related data returned should be rendered
 * with the MapboxRouteLineView class. In addition to setting route data this class can
 * be used to generate the data necessary to hide and show routes already drawn on the map and
 * generally control the visual aspects of a route line.
 *
 * @param routeLineOptions used for determining the appearance and/or behavior of the route line
 */
class MapboxRouteLineApi(
    private var routeLineOptions: MapboxRouteLineOptions
) {
    private var primaryRoute: DirectionsRoute? = null
    private val directionsRoutes: MutableList<DirectionsRoute> = mutableListOf()
    private val routeLineExpressionData: MutableList<RouteLineExpressionData> = mutableListOf()
    private var lastIndexUpdateTimeNano: Long = 0
    private val routeFeatureData: MutableList<RouteFeatureData> = mutableListOf()

    /**
     * @return the vanishing point of the route line if an instance of VanishingRouteLine
     * was supplied during instantiation. If no instance of VanishingRouteLine was supplied
     * 0.0 is returned.
     */
    fun getVanishPointOffset(): Double {
        return routeLineOptions.vanishingRouteLine?.vanishPointOffset ?: 0.0
    }

    /**
     * @return the routes being used
     */
    fun getRoutes(): List<DirectionsRoute> = directionsRoutes.toList()

    /**
     * @return the primary route or null if there is none
     */
    fun getPrimaryRoute(): DirectionsRoute? = primaryRoute

    /**
     * Updates which route is identified as the primary route.
     *
     * @param route the DirectionsRoute which should be designated as the primary
     *
     * @return a state which contains the side effects to be applied to the map displaying the
     * newly designated route line.
     */
    fun updateToPrimaryRoute(route: DirectionsRoute): RouteLineState.RouteSetState {
        val newRoutes = directionsRoutes.filter { it != route }.toMutableList().also {
            it.add(0, route)
        }
        val featureDataProvider: () -> List<RouteFeatureData> =
            MapboxRouteLineUtils.getRouteFeatureDataProvider(newRoutes)
        return setNewRouteData(newRoutes, featureDataProvider)
    }

    /**
     * Sets the routes that will be operated on.
     *
     * @param newRoutes one or more routes. The first route in the collection will be considered
     * the primary route and any additional routes will be alternate routes.
     *
     * @return a state which contains the side effects to be applied to the map
     */
    fun setRoutes(newRoutes: List<RouteLine>): RouteLineState.RouteSetState {
        val routes = newRoutes.map(RouteLine::route)
        val featureDataProvider: () -> List<RouteFeatureData> =
            MapboxRouteLineUtils.getRouteLineFeatureDataProvider(newRoutes)
        return setNewRouteData(routes, featureDataProvider)
    }

    /**
     * @return a state which contains the side effects to be applied to the map. The data
     * can be used to draw the current route line(s) on the map.
     */
    fun getRouteDrawData(): RouteLineState.RouteSetState {
        val featureDataProvider: () -> List<RouteFeatureData> =
            MapboxRouteLineUtils.getRouteFeatureDataProvider(directionsRoutes)
        return buildDrawRoutesState(featureDataProvider)
    }

    /**
     * Indicates the point the route line should change from its default color to the vanishing
     * color behind the puck. Calling this method has no effect if null was passed for the
     * VanishingRouteLine parameter in the constructor of this class.
     *
     * @param point representing the current position of the puck
     *
     * @return a state representing the updates to the route line's appearance or null if there is
     * no update to render
     */
    fun updateTraveledRouteLine(point: Point): RouteLineState.VanishingRouteLineUpdateState? {
        if (routeLineOptions.vanishingRouteLine?.vanishingPointState ==
            VanishingPointState.DISABLED || System.nanoTime() - lastIndexUpdateTimeNano >
            RouteConstants.MAX_ELAPSED_SINCE_INDEX_UPDATE_NANO
        ) {
            return null
        }

        val routeLineExpressions =
            routeLineOptions.vanishingRouteLine?.getTraveledRouteLineExpressions(
                point,
                routeLineExpressionData,
                routeLineOptions.resourceProvider
            )

        return when (routeLineExpressions) {
            null -> null
            else -> RouteLineState.VanishingRouteLineUpdateState(
                routeLineExpressions.trafficLineExpression,
                routeLineExpressions.routeLineExpression,
                routeLineExpressions.routeLineCasingExpression
            )
        }
    }

    /**
     * Clears the route line data.
     *
     * @return a state representing the side effects to be rendered on the map. In this case
     * the map should appear without any route lines.
     */
    fun clearRouteLine(): RouteLineState.ClearRouteLineState {
        routeLineOptions.vanishingRouteLine?.vanishPointOffset = 0.0
        directionsRoutes.clear()
        routeFeatureData.clear()
        routeLineExpressionData.clear()
        routeLineOptions.vanishingRouteLine?.clear()
        return RouteLineState.ClearRouteLineState(
            FeatureCollection.fromFeatures(listOf()),
            FeatureCollection.fromFeatures(listOf()),
            FeatureCollection.fromFeatures(listOf()),
            FeatureCollection.fromFeatures(listOf())
        )
    }

    /**
     * Sets the value of the vanishing point of the route line to the value specified. This is used
     * for the vanishing route line feature and is only applicable if an instance of
     * VanishingRouteLine was supplied at the time of instantiation.
     *
     * @param offset a value representing the percentage of the distance traveled along the route
     *
     * @return a state representing the side effects to be rendered on the map which will update
     * the appearance of the route line or null if the vanishing route line feature is inactive.
     */
    fun setVanishingOffset(offset: Double): RouteLineState.VanishingRouteLineUpdateState? {
        routeLineOptions.vanishingRouteLine?.vanishPointOffset = offset
        return if (offset >= 0) {
            val trafficLineExpression = MapboxRouteLineUtils.getTrafficLineExpression(
                offset,
                routeLineExpressionData,
                routeLineOptions.resourceProvider.routeUnknownTrafficColor
            )
            val routeLineExpression = MapboxRouteLineUtils.getVanishingRouteLineExpression(
                offset,
                routeLineOptions.resourceProvider.routeLineTraveledColor,
                routeLineOptions.resourceProvider.routeDefaultColor
            )
            val routeLineCasingExpression =
                MapboxRouteLineUtils.getVanishingRouteLineExpression(
                    offset,
                    routeLineOptions.resourceProvider.routeLineTraveledCasingColor,
                    routeLineOptions.resourceProvider.routeCasingColor
                )

            RouteLineState.VanishingRouteLineUpdateState(
                trafficLineExpression,
                routeLineExpression,
                routeLineCasingExpression
            )
        } else {
            null
        }
    }

    /**
     * Used for the vanishing route line feature, this method updates the vanishing point
     * calculation point based on the route progress. If null was passed as a parameter for
     * VanishingRouteLine during instantiation, this method does not need to be called.
     *
     * @param routeProgress a route progress object
     */
    fun updateWithRouteProgress(routeProgress: RouteProgress) {
        updateUpcomingRoutePointIndex(routeProgress)
        updateVanishingPointState(routeProgress.currentState)
    }

    /**
     * @return a state that represents the side effects that when rendered will
     * set the primary route to a state of visible.
     */
    fun showPrimaryRoute(): RouteLineState.UpdateLayerVisibilityState {
        return RouteLineState.UpdateLayerVisibilityState(
            listOf(
                Pair(RouteConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID, Visibility.VISIBLE),
                Pair(RouteConstants.PRIMARY_ROUTE_LAYER_ID, Visibility.VISIBLE),
                Pair(RouteConstants.PRIMARY_ROUTE_CASING_LAYER_ID, Visibility.VISIBLE)
            )
        )
    }

    /**
     * @return a state that represents the side effects that when rendered will
     * set the primary route to a state of not visible.
     */
    fun hidePrimaryRoute(): RouteLineState.UpdateLayerVisibilityState {
        return RouteLineState.UpdateLayerVisibilityState(
            listOf(
                Pair(RouteConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID, Visibility.NONE),
                Pair(RouteConstants.PRIMARY_ROUTE_LAYER_ID, Visibility.NONE),
                Pair(RouteConstants.PRIMARY_ROUTE_CASING_LAYER_ID, Visibility.NONE)
            )
        )
    }

    /**
     * @return a state that represents the side effects that when rendered will
     * set the alternative route(s) to a state of visible.
     */
    fun showAlternativeRoutes(): RouteLineState.UpdateLayerVisibilityState {
        return RouteLineState.UpdateLayerVisibilityState(
            listOf(
                Pair(RouteConstants.ALTERNATIVE_ROUTE1_LAYER_ID, Visibility.VISIBLE),
                Pair(RouteConstants.ALTERNATIVE_ROUTE1_CASING_LAYER_ID, Visibility.VISIBLE),
                Pair(RouteConstants.ALTERNATIVE_ROUTE2_LAYER_ID, Visibility.VISIBLE),
                Pair(RouteConstants.ALTERNATIVE_ROUTE2_CASING_LAYER_ID, Visibility.VISIBLE)
            )
        )
    }

    /**
     * @return a state that represents the side effects that when rendered will
     * set the alternative route(s) to a state of not visible.
     */
    fun hideAlternativeRoutes(): RouteLineState.UpdateLayerVisibilityState {
        return RouteLineState.UpdateLayerVisibilityState(
            listOf(
                Pair(RouteConstants.ALTERNATIVE_ROUTE1_LAYER_ID, Visibility.NONE),
                Pair(RouteConstants.ALTERNATIVE_ROUTE1_CASING_LAYER_ID, Visibility.NONE),
                Pair(RouteConstants.ALTERNATIVE_ROUTE2_LAYER_ID, Visibility.NONE),
                Pair(RouteConstants.ALTERNATIVE_ROUTE2_CASING_LAYER_ID, Visibility.NONE)
            )
        )
    }

    /**
     * The map will be queried for a route line feature at the target point or a bounding box
     * centered at the target point with a padding value determining the box's size. If a route
     * feature is found the index of that route in this class's route collection is returned. The
     * primary route is given precedence if more than one route is found.
     *
     * @param target a target latitude/longitude serving as the search point
     * @param mapboxMap a reference to the MapboxMap that will be queried
     * @param padding a sizing value added to all sides of the target point  for creating a bounding
     * box to search in.
     *
     * @return the index of the route in this class's route collection or -1 if no routes found.
     */
    fun findClosestRoute(
        target: Point,
        mapboxMap: MapboxMap,
        padding: Float,
        resultConsumer: MapboxNavigationConsumer<RouteLineState.ClosestRouteState>
    ) {
        val mapClickPoint = mapboxMap.pixelForCoordinate(target)
        val leftFloat = (mapClickPoint.x - padding)
        val rightFloat = (mapClickPoint.x + padding)
        val topFloat = (mapClickPoint.y - padding)
        val bottomFloat = (mapClickPoint.y + padding)
        val clickRect = ScreenBox(
            ScreenCoordinate(leftFloat, topFloat),
            ScreenCoordinate(rightFloat, bottomFloat)
        )
        val queryWithClickRectConsumer = object : MapboxNavigationConsumer<Int> {
            override fun accept(featureIndex: Int) {
                RouteLineState.ClosestRouteState(featureIndex)
            }
        }
        val queryWithClickPointConsumer = object : MapboxNavigationConsumer<Int> {
            override fun accept(featureIndex: Int) {
                if (featureIndex >= 0) {
                    resultConsumer.accept(RouteLineState.ClosestRouteState(featureIndex))
                } else {
                    queryMapForFeatureIndex(
                        mapboxMap,
                        mapClickPoint,
                        clickRect,
                        listOf(
                            RouteConstants.ALTERNATIVE_ROUTE1_LAYER_ID,
                            RouteConstants.ALTERNATIVE_ROUTE1_CASING_LAYER_ID,
                            RouteConstants.ALTERNATIVE_ROUTE2_LAYER_ID,
                            RouteConstants.ALTERNATIVE_ROUTE2_CASING_LAYER_ID
                        ),
                        routeFeatureData.map { it.featureCollection },
                        queryWithClickRectConsumer
                    )
                }
            }
        }

        queryWithClickPointConsumer.accept(-1)
        queryMapForFeatureIndex(
            mapboxMap,
            mapClickPoint,
            clickRect,
            listOf(
                RouteConstants.PRIMARY_ROUTE_LAYER_ID,
                RouteConstants.PRIMARY_ROUTE_CASING_LAYER_ID
            ),
            routeFeatureData.map { it.featureCollection },
            queryWithClickPointConsumer
        )
    }

    private fun queryMapForFeatureIndex(
        mapboxMap: MapboxMap,
        mapClickPoint: ScreenCoordinate,
        clickRect: ScreenBox,
        layerIds: List<String>,
        routeFeatures: List<FeatureCollection>,
        resultConsumer: MapboxNavigationConsumer<Int>
    ) {

        mapboxMap.queryRenderedFeatures(
            mapClickPoint,
            RenderedQueryOptions(layerIds, null)
        ) { features ->
            var featureIndex = getIndexOfFirstFeature(features.value ?: listOf(), routeFeatures)
            when (featureIndex >= 0) {
                true -> {
                    resultConsumer.accept(featureIndex)
                }
                false -> {
                    mapboxMap.queryRenderedFeatures(
                        clickRect,
                        RenderedQueryOptions(layerIds, null)
                    ) {
                        featureIndex = getIndexOfFirstFeature(
                            features.value ?: listOf(),
                            routeFeatures
                        )
                        resultConsumer.accept(featureIndex)
                    }
                }
            }
        }
    }

    private fun getIndexOfFirstFeature(
        features: List<Feature>,
        routeFeatures: List<FeatureCollection>
    ): Int {
        return features.distinct().run {
            routeFeatures.indexOfFirst {
                it.features()?.get(0) ?.id() ?: 0 == this.firstOrNull()?.id()
            }
        }
    }

    internal fun updateUpcomingRoutePointIndex(routeProgress: RouteProgress) {
        ifNonNull(
            routeProgress.currentLegProgress,
            routeProgress.currentLegProgress?.currentStepProgress,
            routeLineOptions.vanishingRouteLine?.primaryRoutePoints
        ) { currentLegProgress, currentStepProgress, completeRoutePoints ->
            var allRemainingPoints = 0
            /**
             * Finds the count of remaining points in the current step.
             *
             * TurfMisc.lineSliceAlong places an additional point at index 0 to mark the precise
             * cut-off point which we can safely ignore.
             * We'll add the distance from the upcoming point to the current's puck position later.
             */
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

            /**
             * Add to the count of remaining points all of the remaining legs.
             */
            for (
                i in currentLegProgress.legIndex + 1 until
                    completeRoutePoints.nestedList.size
            ) {
                allRemainingPoints += completeRoutePoints.nestedList[i].flatten().size
            }

            /**
             * When we know the number of remaining points and the number of all points,
             * calculate the index of the upcoming point.
             */
            /**
             * When we know the number of remaining points and the number of all points,
             * calculate the index of the upcoming point.
             */
            val allPoints = completeRoutePoints.flatList.size
            routeLineOptions.vanishingRouteLine?.primaryRouteRemainingDistancesIndex =
                allPoints - allRemainingPoints - 1
        } ?: run { routeLineOptions.vanishingRouteLine?.primaryRouteRemainingDistancesIndex = null }

        lastIndexUpdateTimeNano = System.nanoTime()
    }

    internal fun updateVanishingPointState(routeProgressState: RouteProgressState) {
        routeLineOptions.vanishingRouteLine?.updateVanishingPointState(routeProgressState)
    }

    private fun setNewRouteData(
        newRoutes: List<DirectionsRoute>,
        featureDataProvider: () -> List<RouteFeatureData>
    ): RouteLineState.RouteSetState {
        directionsRoutes.clear()
        directionsRoutes.addAll(newRoutes)
        primaryRoute = newRoutes.firstOrNull()
        routeLineOptions.vanishingRouteLine?.vanishPointOffset = 0.0
        return buildDrawRoutesState(featureDataProvider)
    }

    private fun buildDrawRoutesState(
        featureDataProvider: () -> List<RouteFeatureData>
    ): RouteLineState.RouteSetState {
        routeFeatureData.clear()
        routeFeatureData.addAll(featureDataProvider())
        val partitionedRoutes = routeFeatureData.partition { it.route == directionsRoutes.first() }
        val segments: List<RouteLineExpressionData> =
            partitionedRoutes.first.firstOrNull()?.route?.run {
                MapboxRouteLineUtils.calculateRouteLineSegments(
                    this,
                    routeLineOptions.resourceProvider.trafficBackfillRoadClasses,
                    true,
                    ::getRouteColorForCongestion
                )
            } ?: listOf()
        routeLineExpressionData.clear()
        routeLineExpressionData.addAll(segments)
        val trafficLineExpression = MapboxRouteLineUtils.getTrafficLineExpression(
            routeLineOptions.vanishingRouteLine?.vanishPointOffset ?: 0.0,
            segments,
            routeLineOptions.resourceProvider.routeUnknownTrafficColor
        )
        val routeLineExpression = MapboxRouteLineUtils.getVanishingRouteLineExpression(
            routeLineOptions.vanishingRouteLine?.vanishPointOffset ?: 0.0,
            routeLineOptions.resourceProvider.routeLineTraveledColor,
            routeLineOptions.resourceProvider.routeDefaultColor
        )
        val routeLineCasingExpression = MapboxRouteLineUtils.getVanishingRouteLineExpression(
            routeLineOptions.vanishingRouteLine?.vanishPointOffset ?: 0.0,
            routeLineOptions.resourceProvider.routeLineTraveledColor,
            routeLineOptions.resourceProvider.routeCasingColor
        )
        val alternativeRoute1TrafficSegments: List<RouteLineExpressionData> =
            partitionedRoutes.second.firstOrNull()?.route?.run {
                MapboxRouteLineUtils.calculateRouteLineSegments(
                    this,
                    routeLineOptions.resourceProvider.trafficBackfillRoadClasses,
                    false,
                    ::getRouteColorForCongestion
                )
            } ?: listOf()
        val alternativeRoute1TrafficExpression = MapboxRouteLineUtils.getTrafficLineExpression(
            routeLineOptions.vanishingRouteLine?.vanishPointOffset ?: 0.0,
            alternativeRoute1TrafficSegments,
            routeLineOptions.resourceProvider.alternativeRouteUnknownTrafficColor
        )
        val alternativeRoute2TrafficSegments: List<RouteLineExpressionData> =
            if (partitionedRoutes.second.size > 1) {
                partitionedRoutes.second[1].route.run {
                    MapboxRouteLineUtils.calculateRouteLineSegments(
                        this,
                        routeLineOptions.resourceProvider.trafficBackfillRoadClasses,
                        false,
                        ::getRouteColorForCongestion
                    )
                }
            } else {
                listOf()
            }
        val alternativeRoute2TrafficExpression = MapboxRouteLineUtils.getTrafficLineExpression(
            routeLineOptions.vanishingRouteLine?.vanishPointOffset ?: 0.0,
            alternativeRoute2TrafficSegments,
            routeLineOptions.resourceProvider.alternativeRouteUnknownTrafficColor
        )
        val alternativeRoute1FeatureCollection: FeatureCollection =
            partitionedRoutes.second.firstOrNull()?.featureCollection
                ?: FeatureCollection.fromFeatures(listOf())
        val alternativeRoute2FeatureCollection: FeatureCollection =
            if (partitionedRoutes.second.size > 1) {
                partitionedRoutes.second[1].featureCollection
            } else {
                FeatureCollection.fromFeatures(listOf())
            }
        val wayPointsFeatureCollection: FeatureCollection =
            partitionedRoutes.first.firstOrNull()?.route?.run {
                MapboxRouteLineUtils.buildWayPointFeatureCollection(this)
            } ?: FeatureCollection.fromFeatures(listOf())
        partitionedRoutes.first.firstOrNull()?.let {
            routeLineOptions.vanishingRouteLine?.initWithRoute(it.route)
        }
        val primaryRouteSource = partitionedRoutes.first.firstOrNull()?.featureCollection
            ?: FeatureCollection.fromFeatures(
                listOf()
            )

        return RouteLineState.RouteSetState(
            primaryRouteSource,
            trafficLineExpression,
            routeLineExpression,
            routeLineCasingExpression,
            alternativeRoute1TrafficExpression,
            alternativeRoute2TrafficExpression,
            alternativeRoute1FeatureCollection,
            alternativeRoute2FeatureCollection,
            wayPointsFeatureCollection
        )
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
                RouteConstants.LOW_CONGESTION_VALUE -> {
                    routeLineOptions.resourceProvider.routeLowCongestionColor
                }
                RouteConstants.MODERATE_CONGESTION_VALUE -> {
                    routeLineOptions.resourceProvider.routeModerateColor
                }
                RouteConstants.HEAVY_CONGESTION_VALUE -> {
                    routeLineOptions.resourceProvider.routeHeavyColor
                }
                RouteConstants.SEVERE_CONGESTION_VALUE -> {
                    routeLineOptions.resourceProvider.routeSevereColor
                }
                RouteConstants.UNKNOWN_CONGESTION_VALUE -> {
                    routeLineOptions.resourceProvider.routeUnknownTrafficColor
                }
                else -> routeLineOptions.resourceProvider.routeDefaultColor
            }
            false -> when (congestionValue) {
                RouteConstants.LOW_CONGESTION_VALUE -> {
                    routeLineOptions.resourceProvider.alternativeRouteLowColor
                }
                RouteConstants.MODERATE_CONGESTION_VALUE -> {
                    routeLineOptions.resourceProvider.alternativeRouteModerateColor
                }
                RouteConstants.HEAVY_CONGESTION_VALUE -> {
                    routeLineOptions.resourceProvider.alternativeRouteHeavyColor
                }
                RouteConstants.SEVERE_CONGESTION_VALUE -> {
                    routeLineOptions.resourceProvider.alternativeRouteSevereColor
                }
                RouteConstants.UNKNOWN_CONGESTION_VALUE -> {
                    routeLineOptions.resourceProvider.alternativeRouteUnknownTrafficColor
                }
                else -> {
                    routeLineOptions.resourceProvider.alternativeRouteDefaultColor
                }
            }
        }
    }
}
