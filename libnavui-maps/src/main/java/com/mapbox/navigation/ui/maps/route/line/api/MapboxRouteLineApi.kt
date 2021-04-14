package com.mapbox.navigation.ui.maps.route.line.api

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.QueriedFeature
import com.mapbox.maps.QueryFeaturesCallback
import com.mapbox.maps.RenderedQueryOptions
import com.mapbox.maps.ScreenBox
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.ui.base.internal.model.route.RouteConstants
import com.mapbox.navigation.ui.base.model.Expected
import com.mapbox.navigation.ui.base.model.route.RouteLayerConstants
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils
import com.mapbox.navigation.ui.maps.route.line.model.ClosestRouteValue
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteFeatureData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineClearValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineError
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineExpressionData
import com.mapbox.navigation.ui.maps.route.line.model.RouteNotFound
import com.mapbox.navigation.ui.maps.route.line.model.RouteSetValue
import com.mapbox.navigation.ui.maps.route.line.model.VanishingPointState
import com.mapbox.navigation.ui.maps.route.line.model.VanishingRouteLineUpdateValue
import com.mapbox.navigation.ui.utils.internal.ifNonNull
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfException
import com.mapbox.turf.TurfMisc
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Responsible for generating route line related data which can be rendered on the map to
 * visualize a line representing a route. The route related data returned should be rendered
 * with the [MapboxRouteLineView] class. In addition to setting route data this class can
 * be used to generate the data necessary to hide and show routes already drawn on the map and
 * generally control the visual aspects of a route line.
 *
 * The two principal classes for the route line are the [MapboxRouteLineApi] and the
 * [MapboxRouteLineView]. The [MapboxRouteLineApi] consumes data produced by the Navigation SDK and
 * produces data that can be used to visualize the data on the map. The [MapboxRouteLineView] consumes
 * the data from the [MapboxRouteLineApi] and calls the appropriate map related commands to produce
 * a line on the map representing one or more routes.
 *
 * A simple example would involve an activity instantiating the [MapboxRouteLineApi] and
 * [MapboxRouteLineView] classes and maintaining a reference to them. Both classes need a reference
 * to an instance of [MapboxRouteLineOptions]. The default options can be used as a starting point
 * so the simplest usage would look like:
 *
 * ```java
 * MapboxRouteLineOptions mapboxRouteLineOptions = new MapboxRouteLineOptions.Builder(context).build();
 * MapboxRouteLineApi mapboxRouteLineApi = new MapboxRouteLineApi(mapboxRouteLineOptions);
 * MapboxRouteLineView mapboxRouteLineView = new MapboxRouteLineView(mapboxRouteLineOptions);
 * ```
 *
 * or
 *
 * ```kotlin
 * val mapboxRouteLineOptions = MapboxRouteLineOptions.Builder(context).build()
 * val mapboxRouteLineApi = MapboxRouteLineApi(mapboxRouteLineOptions)
 * val mapboxRouteLineView = MapboxRouteLineView(mapboxRouteLineOptions)
 * ```
 *
 * When one or more [DirectionsRoute] objects are retrieved from [MapboxNavigation] they can be displayed
 * on the map by calling [mapboxRouteLineApi.setRoutes()] and then passing the object returned to the
 * view class via [mapboxRouteLineView.render()] which will draw the route(s) on the map. Note, if
 * passing more than one route to the setRoutes method, the first route in the collection will be
 * considered the primary route. There is a known bug in the Map SDK that prevents clicking on a
 * drawn alternative route in order to select it as the primary route. This issue will be resolved
 * in an upcoming build.
 *
 * Calls to the [MapboxRouteLineView::render] command always take the current Map Style object as an
 * argument. It is important to ensure the Style object is always current. If the application
 * changes the map style at runtime the new Style should be passed as an argument to the render
 * method following the style change.
 *
 * In almost all cases the [MapboxRouteLineApi] class methods return a state object that should be
 * passed to the [MapboxRouteLineView] render method.
 *
 * Customizing the route line and arrow appearance can be done via the [MapboxRouteLineOptions] and
 * [RouteArrowOptions]. In the 1.x version of the Navigation SDK this customization was done by
 * overriding the Mapbox defaults defined in the styles.xml. In this version of the SDK the
 * customization is done by providing values to the [MapboxRouteLineOptions.Builder] and
 * [RouteArrowOptions.Builder]. Default values are used for any custom values that are not provided.
 *
 * Vanishing Route Line:
 * The "vanishing route line" is a feature which changes the appearance of the route line
 * behind the puck to a specific color or makes it transparent. This creates a visual difference
 * between the section of the route that has been traveled and the section that has yet to be
 * traveled. In order to enable and use this feature do the following:
 *
 * 1. Enable the feature in the [MapboxRouteLineOptions]
 * ```kotlin
 * MapboxRouteLineOptions.Builder(context)
 * .withVanishingRouteLineEnabled(true)
 * .build()
 * ```
 * 2. Register an [OnIndicatorPositionChangedListener] with the [LocationComponent]:
 *
 * ```kotlin
 * locationComponent.addOnIndicatorPositionChangedListener(myIndicatorPositionChangedListener)
 * ```
 * (Be sure to unregister this listener appropriately according to the lifecycle of your activity
 * or Fragment in order to prevent resource leaks.)
 *
 * 3. In your [OnIndicatorPositionChangedListener] implementation update the [MapboxRouteLineApi]
 * with the Point provided by the listener and render the state returned by [MapboxRouteLineApi].
 *
 * ```kotlin
 * val vanishingRouteLineData = mapboxRouteLineApi.updateTraveledRouteLine(point)
 * if (vanishingRouteLineData != null && mapboxMap.getStyle() != null) {
 * mapboxRouteLineView.render(mapboxMap.getStyle(), vanishingRouteLineData);
 * }
 * ```
 *
 * 4. Register a [RouteProgressObserver] with [MapboxNavigation] and pass the data to the
 * [MapboxRouteLineApi] (Be sure to unregister this listener appropriately according to the
 * lifecycle of your activity or Fragment in order to prevent resource leaks.)
 *
 * ```kotlin
 * override fun onRouteProgressChanged(routeProgress: RouteProgress) {
 * mapboxRouteLineApi.updateWithRouteProgress(routeProgress) // nothing to render in this case
 * }
 * ```
 *
 * In order to keep the point on the route line indicating traveled vs not traveled in sync
 * with the puck, data from both [OnIndicatorPositionChangedListener] and the [RouteProgressObserver]
 * are needed.
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
    private val jobControl = ThreadController.getMainScopeAndRootJob()
    private val mutex = Mutex()

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
     * @param route the [DirectionsRoute] which should be designated as the primary
     * @param consumer a consumer of the result of calling this method which should be applied to
     * a render method in [MapboxRouteLineView]
     */
    fun updateToPrimaryRoute(
        route: DirectionsRoute,
        consumer: MapboxNavigationConsumer<Expected<RouteSetValue, RouteLineError>>
    ) {
        jobControl.scope.launch {
            mutex.withLock {
                val newRoutes = directionsRoutes.filter { it != route }.toMutableList().also {
                    it.add(0, route)
                }
                val featureDataProvider: () -> List<RouteFeatureData> =
                    MapboxRouteLineUtils.getRouteFeatureDataProvider(newRoutes)
                val routeData = setNewRouteData(newRoutes, featureDataProvider)
                consumer.accept(routeData)
            }
        }
    }

    /**
     * Sets the routes that will be operated on.
     *
     * @param newRoutes one or more routes. The first route in the collection will be considered
     * the primary route and any additional routes will be alternate routes.
     * @param consumer a method that consumes the result of the operation.
     *
     */
    fun setRoutes(
        newRoutes: List<RouteLine>,
        consumer: MapboxNavigationConsumer<Expected<RouteSetValue, RouteLineError>>
    ) {
        jobControl.scope.launch {
            mutex.withLock {
                val routes = newRoutes.map(RouteLine::route)
                val featureDataProvider: () -> List<RouteFeatureData> =
                    MapboxRouteLineUtils.getRouteLineFeatureDataProvider(newRoutes)
                val routeData = setNewRouteData(routes, featureDataProvider)
                consumer.accept(routeData)
            }
        }
    }

    /**
     * Gathers the data necessary to draw the route line(s) on the map based on the current state.
     *
     * @param consumer a method that consumes the result of the operation. The data received by
     * the consumer should be passed to the render method of the [MapboxRouteLineView]
     */
    fun getRouteDrawData(
        consumer: MapboxNavigationConsumer<Expected<RouteSetValue, RouteLineError>>
    ) {
        jobControl.scope.launch {
            mutex.withLock {
                val featureDataProvider: () -> List<RouteFeatureData> =
                    MapboxRouteLineUtils.getRouteFeatureDataProvider(directionsRoutes)
                val result = buildDrawRoutesState(featureDataProvider)
                consumer.accept(result)
            }
        }
    }

    /**
     * Indicates the point the route line should change from its default color to the vanishing
     * color behind the puck. Calling this method has no effect if null was passed for the
     * VanishingRouteLine parameter in the constructor of this class.
     *
     * @param point representing the current position of the puck
     *
     * @return a value representing the updates to the route line's appearance or an error.
     */
    fun updateTraveledRouteLine(
        point: Point
    ): Expected<VanishingRouteLineUpdateValue, RouteLineError> {
        if (routeLineOptions.vanishingRouteLine?.vanishingPointState ==
            VanishingPointState.DISABLED || System.nanoTime() - lastIndexUpdateTimeNano >
            RouteConstants.MAX_ELAPSED_SINCE_INDEX_UPDATE_NANO
        ) {
            return Expected.Failure(
                RouteLineError(
                    "Vanishing point state is disabled or too much time has " +
                        "elapsed since last update.",
                    null
                )
            )
        }

        val routeLineExpressions =
            routeLineOptions.vanishingRouteLine?.getTraveledRouteLineExpressions(
                point,
                routeLineExpressionData,
                routeLineOptions.resourceProvider
            )

        return when (routeLineExpressions) {
            null -> {
                Expected.Failure(
                    RouteLineError(
                        "No expression generated for update.",
                        null
                    )
                )
            }
            else -> Expected.Success(
                VanishingRouteLineUpdateValue(
                    routeLineExpressions.trafficLineExpression,
                    routeLineExpressions.routeLineExpression,
                    routeLineExpressions.routeLineCasingExpression
                )
            )
        }
    }

    /**
     * Clears the route line data.
     *
     * @param consumer a callback to consume the result
     * @return a state representing the side effects to be rendered on the map. In this case
     * the map should appear without any route lines.
     */
    fun clearRouteLine(
        consumer: MapboxNavigationConsumer<Expected<RouteLineClearValue, RouteLineError>>
    ) {
        jobControl.scope.launch {
            mutex.withLock {
                routeLineOptions.vanishingRouteLine?.vanishPointOffset = 0.0
                directionsRoutes.clear()
                routeFeatureData.clear()
                routeLineExpressionData.clear()
                routeLineOptions.vanishingRouteLine?.clear()

                consumer.accept(
                    Expected.Success(
                        RouteLineClearValue(
                            FeatureCollection.fromFeatures(listOf()),
                            FeatureCollection.fromFeatures(listOf()),
                            FeatureCollection.fromFeatures(listOf()),
                            FeatureCollection.fromFeatures(listOf())
                        )
                    )
                )
            }
        }
    }

    /**
     * Sets the value of the vanishing point of the route line to the value specified. This is used
     * for the vanishing route line feature and is only applicable if an instance of
     * [VanishingRouteLine] was supplied at the time of instantiation.
     *
     * @param offset a value representing the percentage of the distance traveled along the route
     *
     * @return a state representing the side effects to be rendered on the map which will update
     * the appearance of the route line or null if the vanishing route line feature is inactive.
     */
    fun setVanishingOffset(
        offset: Double
    ): Expected<VanishingRouteLineUpdateValue, RouteLineError> {
        routeLineOptions.vanishingRouteLine?.vanishPointOffset = offset
        return if (offset >= 0) {
            val trafficLineExpression = MapboxRouteLineUtils.getTrafficLineExpression(
                offset,
                routeLineExpressionData,
                routeLineOptions
                    .resourceProvider
                    .routeLineColorResources
                    .routeUnknownTrafficColor
            )
            val routeLineExpression = MapboxRouteLineUtils.getVanishingRouteLineExpression(
                offset,
                routeLineOptions
                    .resourceProvider
                    .routeLineColorResources
                    .routeLineTraveledColor,
                routeLineOptions.resourceProvider.routeLineColorResources.routeDefaultColor
            )
            val routeLineCasingExpression =
                MapboxRouteLineUtils.getVanishingRouteLineExpression(
                    offset,
                    routeLineOptions
                        .resourceProvider.routeLineColorResources.routeLineTraveledCasingColor,
                    routeLineOptions.resourceProvider.routeLineColorResources.routeCasingColor
                )

            Expected.Success(
                VanishingRouteLineUpdateValue(
                    trafficLineExpression,
                    routeLineExpression,
                    routeLineCasingExpression
                )
            )
        } else {
            Expected.Failure(
                RouteLineError("Offset value should be greater than or equal to 0", null)
            )
        }
    }

    /**
     * Used for the vanishing route line feature, this method updates the vanishing point
     * calculation point based on the route progress. If null was passed as a parameter for
     * [VanishingRouteLine] during instantiation, this method does not need to be called.
     *
     * @param routeProgress a route progress object
     */
    fun updateWithRouteProgress(routeProgress: RouteProgress) {
        updateUpcomingRoutePointIndex(routeProgress)
        updateVanishingPointState(routeProgress.currentState)
    }

    /**
     * The map will be queried for a route line feature at the target point or a bounding box
     * centered at the target point with a padding value determining the box's size. If a route
     * feature is found the index of that route in this class's route collection is returned. The
     * primary route is given precedence if more than one route is found.
     *
     * @param target a target latitude/longitude serving as the search point
     * @param mapboxMap a reference to the [MapboxMap] that will be queried
     * @param padding a sizing value added to all sides of the target point for creating a bounding
     * box to search in.
     * @param resultConsumer a callback to receive the result
     */
    fun findClosestRoute(
        target: Point,
        mapboxMap: MapboxMap,
        padding: Float,
        resultConsumer: MapboxNavigationConsumer<Expected<ClosestRouteValue, RouteNotFound>>
    ) {
        jobControl.scope.launch {
            mutex.withLock {
                val state = findClosestRoute(target, mapboxMap, padding)
                resultConsumer.accept(state)
            }
        }
    }

    private suspend fun findClosestRoute(
        target: Point,
        mapboxMap: MapboxMap,
        padding: Float,
    ): Expected<ClosestRouteValue, RouteNotFound> {
        val mapClickPoint = mapboxMap.pixelForCoordinate(target)
        val leftFloat = (mapClickPoint.x - padding)
        val rightFloat = (mapClickPoint.x + padding)
        val topFloat = (mapClickPoint.y - padding)
        val bottomFloat = (mapClickPoint.y + padding)
        val clickRect = ScreenBox(
            ScreenCoordinate(leftFloat, topFloat),
            ScreenCoordinate(rightFloat, bottomFloat)
        )
        val routesAndFeatures = routeFeatureData.toList()
        val features = routesAndFeatures.map { it.featureCollection }

        val clickPointFeatureIndex = queryMapForFeatureIndex(
            mapboxMap,
            mapClickPoint,
            listOf(
                RouteLayerConstants.ALTERNATIVE_ROUTE1_LAYER_ID,
                RouteLayerConstants.ALTERNATIVE_ROUTE1_CASING_LAYER_ID,
                RouteLayerConstants.ALTERNATIVE_ROUTE2_LAYER_ID,
                RouteLayerConstants.ALTERNATIVE_ROUTE2_CASING_LAYER_ID
            ),
            features
        )

        return if (clickPointFeatureIndex >= 0) {
            Expected.Success(ClosestRouteValue(routesAndFeatures[clickPointFeatureIndex].route))
        } else {
            val clickRectFeatureIndex = queryMapForFeatureIndex(
                mapboxMap,
                clickRect,
                listOf(
                    RouteLayerConstants.ALTERNATIVE_ROUTE1_LAYER_ID,
                    RouteLayerConstants.ALTERNATIVE_ROUTE1_CASING_LAYER_ID,
                    RouteLayerConstants.ALTERNATIVE_ROUTE2_LAYER_ID,
                    RouteLayerConstants.ALTERNATIVE_ROUTE2_CASING_LAYER_ID
                ),
                features
            )
            if (clickRectFeatureIndex >= 0) {
                Expected.Success(ClosestRouteValue(routesAndFeatures[clickRectFeatureIndex].route))
            } else {
                val index = queryMapForFeatureIndex(
                    mapboxMap,
                    mapClickPoint,
                    listOf(
                        RouteLayerConstants.PRIMARY_ROUTE_LAYER_ID,
                        RouteLayerConstants.PRIMARY_ROUTE_CASING_LAYER_ID,
                    ),
                    features
                )
                if (index >= 0) {
                    Expected.Success(ClosestRouteValue(routesAndFeatures[index].route))
                } else {
                    Expected.Failure(RouteNotFound("No route found in query area.", null))
                }
            }
        }
    }

    private suspend fun queryMapForFeatureIndex(
        mapboxMap: MapboxMap,
        mapClickPoint: ScreenCoordinate,
        layerIds: List<String>,
        routeFeatures: List<FeatureCollection>
    ): Int {
        return suspendCoroutine { continuation ->
            mapboxMap.queryRenderedFeatures(
                mapClickPoint,
                RenderedQueryOptions(layerIds, null),
                QueryFeaturesCallback {
                    val index = getIndexOfFirstFeature(it.value ?: listOf(), routeFeatures)
                    continuation.resume(index)
                }
            )
        }
    }

    private suspend fun queryMapForFeatureIndex(
        mapboxMap: MapboxMap,
        clickRect: ScreenBox,
        layerIds: List<String>,
        routeFeatures: List<FeatureCollection>
    ): Int {
        return suspendCoroutine { continuation ->
            mapboxMap.queryRenderedFeatures(
                clickRect,
                RenderedQueryOptions(layerIds, null),
                QueryFeaturesCallback {
                    val index = getIndexOfFirstFeature(it.value ?: listOf(), routeFeatures)
                    continuation.resume(index)
                }
            )
        }
    }

    private fun getIndexOfFirstFeature(
        features: List<QueriedFeature>,
        routeFeatures: List<FeatureCollection>
    ): Int {
        return features.distinct().run {
            routeFeatures.indexOfFirst {
                it.features()?.get(0)?.id() ?: 0 == this.firstOrNull()?.feature?.id()
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
            for (i in currentLegProgress.legIndex + 1 until completeRoutePoints.nestedList.size) {
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

    private suspend fun setNewRouteData(
        newRoutes: List<DirectionsRoute>,
        featureDataProvider: () -> List<RouteFeatureData>
    ): Expected<RouteSetValue, RouteLineError> {
        directionsRoutes.clear()
        directionsRoutes.addAll(newRoutes)
        primaryRoute = newRoutes.firstOrNull()
        routeLineOptions.vanishingRouteLine?.vanishPointOffset = 0.0
        return buildDrawRoutesState(featureDataProvider)
    }

    private suspend fun buildDrawRoutesState(
        featureDataProvider: () -> List<RouteFeatureData>
    ): Expected<RouteSetValue, RouteLineError> {
        val routeFeatureDataDef = jobControl.scope.async(ThreadController.IODispatcher) {
            featureDataProvider()
        }
        routeFeatureData.clear()
        routeFeatureData.addAll(routeFeatureDataDef.await())
        val partitionedRoutes = routeFeatureData.partition { it.route == directionsRoutes.first() }

        partitionedRoutes.first.firstOrNull()?.let {
            routeLineOptions.vanishingRouteLine?.initWithRoute(it.route)
        }

        val trafficLineExpressionDef = jobControl.scope.async(ThreadController.IODispatcher) {
            val segments: List<RouteLineExpressionData> =
                partitionedRoutes.first.firstOrNull()?.route?.run {
                    MapboxRouteLineUtils.calculateRouteLineSegments(
                        this,
                        routeLineOptions.resourceProvider.trafficBackfillRoadClasses,
                        true,
                        routeLineOptions.resourceProvider.routeLineColorResources
                    )
                } ?: listOf()
            routeLineExpressionData.clear()
            routeLineExpressionData.addAll(segments)
            MapboxRouteLineUtils.getTrafficLineExpression(
                routeLineOptions.vanishingRouteLine?.vanishPointOffset ?: 0.0,
                segments,
                routeLineOptions.resourceProvider.routeLineColorResources.routeUnknownTrafficColor
            )
        }

        val routeLineExpressionDef = jobControl.scope.async(ThreadController.IODispatcher) {
            MapboxRouteLineUtils.getVanishingRouteLineExpression(
                routeLineOptions.vanishingRouteLine?.vanishPointOffset ?: 0.0,
                routeLineOptions.resourceProvider.routeLineColorResources.routeLineTraveledColor,
                routeLineOptions.resourceProvider.routeLineColorResources.routeDefaultColor
            )
        }

        val routeLineCasingExpressionDef = jobControl.scope.async(ThreadController.IODispatcher) {
            MapboxRouteLineUtils.getVanishingRouteLineExpression(
                routeLineOptions.vanishingRouteLine?.vanishPointOffset ?: 0.0,
                routeLineOptions
                    .resourceProvider
                    .routeLineColorResources
                    .routeLineTraveledCasingColor,
                routeLineOptions.resourceProvider.routeLineColorResources.routeCasingColor
            )
        }

        val alternativeRoute1TrafficExpressionDef =
            jobControl.scope.async(ThreadController.IODispatcher) {
                val alternativeRoute1TrafficSegments: List<RouteLineExpressionData> =
                    partitionedRoutes.second.firstOrNull()?.route?.run {
                        MapboxRouteLineUtils.calculateRouteLineSegments(
                            this,
                            routeLineOptions.resourceProvider.trafficBackfillRoadClasses,
                            false,
                            routeLineOptions.resourceProvider.routeLineColorResources
                        )
                    } ?: listOf()
                MapboxRouteLineUtils.getTrafficLineExpression(
                    0.0,
                    alternativeRoute1TrafficSegments,
                    routeLineOptions
                        .resourceProvider
                        .routeLineColorResources
                        .alternativeRouteUnknownTrafficColor
                )
            }

        val alternativeRoute2TrafficExpressionDef =
            jobControl.scope.async(ThreadController.IODispatcher) {
                val alternativeRoute2TrafficSegments: List<RouteLineExpressionData> =
                    if (partitionedRoutes.second.size > 1) {
                        partitionedRoutes.second[1].route.run {
                            MapboxRouteLineUtils.calculateRouteLineSegments(
                                this,
                                routeLineOptions.resourceProvider.trafficBackfillRoadClasses,
                                false,
                                routeLineOptions.resourceProvider.routeLineColorResources
                            )
                        }
                    } else {
                        listOf()
                    }
                MapboxRouteLineUtils.getTrafficLineExpression(
                    0.0,
                    alternativeRoute2TrafficSegments,
                    routeLineOptions
                        .resourceProvider
                        .routeLineColorResources
                        .alternativeRouteUnknownTrafficColor
                )
            }

        val alternativeRoute1FeatureCollectionDef =
            jobControl.scope.async(ThreadController.IODispatcher) {
                partitionedRoutes.second.firstOrNull()?.featureCollection
                    ?: FeatureCollection.fromFeatures(listOf())
            }

        val alternativeRoute2FeatureCollectionDef =
            jobControl.scope.async(ThreadController.IODispatcher) {
                if (partitionedRoutes.second.size > 1) {
                    partitionedRoutes.second[1].featureCollection
                } else {
                    FeatureCollection.fromFeatures(listOf())
                }
            }

        val wayPointsFeatureCollectionDef = jobControl.scope.async(ThreadController.IODispatcher) {
            partitionedRoutes.first.firstOrNull()?.route?.run {
                MapboxRouteLineUtils.buildWayPointFeatureCollection(this)
            } ?: FeatureCollection.fromFeatures(listOf())
        }

        val primaryRouteSourceDef = jobControl.scope.async(ThreadController.IODispatcher) {
            partitionedRoutes.first.firstOrNull()?.featureCollection
                ?: FeatureCollection.fromFeatures(
                    listOf()
                )
        }

        val restrictedSectionFeatureCollectionDef =
            jobControl.scope.async(ThreadController.IODispatcher) {
                if (routeLineOptions.enableRestrictedRoadLayer) {
                    partitionedRoutes.first.firstOrNull()?.route?.run {
                        getRestrictedRoadFeatureCollection(this)
                    } ?: FeatureCollection.fromFeatures(listOf())
                } else {
                    FeatureCollection.fromFeatures(listOf())
                }
            }

        val trafficLineExp = trafficLineExpressionDef.await()
        val routeLineExp = routeLineExpressionDef.await()
        val routeCasingLineExp = routeLineCasingExpressionDef.await()
        val altRoute1Exp = alternativeRoute1TrafficExpressionDef.await()
        val altRoute2Exp = alternativeRoute2TrafficExpressionDef.await()
        val alternativeRoute1FeatureCollection = alternativeRoute1FeatureCollectionDef.await()
        val alternativeRoute2FeatureCollection = alternativeRoute2FeatureCollectionDef.await()
        val wayPointsFeatureCollection = wayPointsFeatureCollectionDef.await()
        val primaryRouteSource = primaryRouteSourceDef.await()
        val restrictedSectionFeatureCollection = restrictedSectionFeatureCollectionDef.await()

        return Expected.Success(
            RouteSetValue(
                primaryRouteSource,
                trafficLineExp,
                routeLineExp,
                routeCasingLineExp,
                altRoute1Exp,
                altRoute2Exp,
                alternativeRoute1FeatureCollection,
                alternativeRoute2FeatureCollection,
                wayPointsFeatureCollection,
                restrictedSectionFeatureCollection
            )
        )
    }

    private fun getRestrictedRoadFeatureCollection(route: DirectionsRoute): FeatureCollection {
        return MapboxRouteLineUtils.getRestrictedRouteSections(route).map {
            Feature.fromGeometry(LineString.fromLngLats(it))
        }.run {
            FeatureCollection.fromFeatures(this)
        }
    }
}
