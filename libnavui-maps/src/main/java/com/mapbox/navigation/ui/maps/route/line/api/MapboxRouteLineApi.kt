package com.mapbox.navigation.ui.maps.route.line.api

import android.graphics.Color
import android.util.LruCache
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.QueriedFeature
import com.mapbox.maps.RenderedQueryOptions
import com.mapbox.maps.ScreenBox
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.Layer
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPluginImpl
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.navigation.base.internal.utils.isSameRoute
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.toDirectionsRoutes
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.routealternatives.AlternativeRouteMetadata
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils.getMatchingColors
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils.layerGroup1SourceLayerIds
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils.layerGroup2SourceLayerIds
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils.layerGroup3SourceLayerIds
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants
import com.mapbox.navigation.ui.maps.route.line.model.ClosestRouteValue
import com.mapbox.navigation.ui.maps.route.line.model.ExtractedRouteData
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.NavigationRouteLine
import com.mapbox.navigation.ui.maps.route.line.model.RouteFeatureData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineClearValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineDynamicData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineError
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineExpressionData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineExpressionProvider
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineTrimOffset
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineUpdateValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteNotFound
import com.mapbox.navigation.ui.maps.route.line.model.RouteSetValue
import com.mapbox.navigation.ui.maps.route.line.model.VanishingPointState
import com.mapbox.navigation.ui.maps.route.line.model.toNavigationRouteLines
import com.mapbox.navigation.ui.maps.util.CacheResultUtils
import com.mapbox.navigation.ui.maps.util.CacheResultUtils.cacheResult
import com.mapbox.navigation.ui.utils.internal.ifNonNull
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import com.mapbox.navigation.utils.internal.parallelMap
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfException
import com.mapbox.turf.TurfMisc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.CopyOnWriteArrayList
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
 * view class via [MapboxRouteLineView.renderRouteDrawData] which will draw the route(s) on the map. Note, if
 * passing more than one route to the setRoutes method, the first route in the collection will be
 * considered the primary route.
 *
 * Calls to the [MapboxRouteLineView.renderRouteDrawData] command always take the current [MapboxMap]
 * [Style] object as an argument. It is important to ensure the [Style] object is always current.
 * If the application changes the map style at runtime the new [Style] should be passed as an
 * argument to the render method following the style change.
 *
 * Each [Layer] added to the map by this [MapboxRouteLineView] is a persistent layer - it will survive style changes.
 * This means that if the data has not changed, it does not have to be manually redrawn after a style change.
 * See [Style.addPersistentStyleLayer].
 *
 * In order to display traffic congestion indications on the route line it is necessary to
 * request routes with specific [RouteOptions].  At a minimum the following options are necessary:
 *
 * ```kotlin
 *  val routeOptions = RouteOptions.builder()
 *      .baseUrl(Constants.BASE_API_URL)
 *      .user(Constants.MAPBOX_USER)
 *      .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
 *      .overview(DirectionsCriteria.OVERVIEW_FULL)
 *      .steps(true)
 *      .annotationsList(
 *          listOf(
 *              DirectionsCriteria.ANNOTATION_CONGESTION_NUMERIC,
 *              DirectionsCriteria.ANNOTATION_DURATION,
 *              DirectionsCriteria.ANNOTATION_DISTANCE,
 *          )
 *      )
 *      .requestUuid("")
 *      .accessToken("mapToken")
 *      .coordinatesList(listOf(origin, destination))
 *      .build()
 * ```
 * A good starting point might be RouteOptions.Builder.applyDefaultNavigationOptions() which will
 * include the options above.
 *
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
 * 2. Register an [OnIndicatorPositionChangedListener] with the [LocationComponentPluginImpl]:
 *
 * ```kotlin
 * mapView.getPlugin(LocationComponentPluginImpl.class).addOnIndicatorPositionChangedListener(myIndicatorPositionChangedListener)
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
 * mapboxRouteLineView.renderRouteLineUpdate(mapboxMap.getStyle(), vanishingRouteLineData);
 * }
 * ```
 *
 * 4. Register a [RouteProgressObserver] with [MapboxNavigation] and pass the data to the
 * [MapboxRouteLineApi] (Be sure to unregister this listener appropriately according to the
 * lifecycle of your activity or Fragment in order to prevent resource leaks.)
 *
 * ```kotlin
 * override fun onRouteProgressChanged(routeProgress: RouteProgress) {
 * mapboxRouteLineApi.updateWithRouteProgress(routeProgress) { result ->
 * mapboxRouteLineView.renderRouteLineUpdate(mapboxMap.getStyle(), result)
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
    private val routeLineOptions: MapboxRouteLineOptions
) {
    private var primaryRoute: NavigationRoute? = null
    private var routeHasRestrictions = false
    private val routes: MutableList<NavigationRoute> = mutableListOf()
    private var routeLineExpressionData: List<RouteLineExpressionData> = emptyList()
    private var lastIndexUpdateTimeNano: Long = 0
    private var lastPointUpdateTimeNano: Long = 0
    private val routeFeatureData: MutableList<RouteFeatureData> = mutableListOf()
    private val jobControl = InternalJobControlFactory.createDefaultScopeJobControl()
    private val mutex = Mutex()
    internal var activeLegIndex = INVALID_ACTIVE_LEG_INDEX
        private set
    private val trafficBackfillRoadClasses = CopyOnWriteArrayList<String>()
    private var alternativesDeviationOffset = mapOf<String, Double>()
    private val alternativelyStyleSegmentsNotInLegCache: LruCache<
        CacheResultUtils.CacheResultKey2<
            Int, List<RouteLineExpressionData>,
            List<RouteLineExpressionData>
            >,
        List<RouteLineExpressionData>> by lazy { LruCache(2) }

    companion object {
        private const val INVALID_ACTIVE_LEG_INDEX = -1
    }

    init {
        trafficBackfillRoadClasses.addAll(
            routeLineOptions.resourceProvider.trafficBackfillRoadClasses
        )
    }

    /**
     * Replaces the traffic back fill road classes derived from [RouteLineResources].
     *
     * @param roadClasses the road class collection that should be used in place of those
     * from the [RouteLineResources] as part of the [MapboxRouteLineOptions]
     */
    fun setRoadClasses(roadClasses: List<String>) {
        jobControl.scope.launch(Dispatchers.Main) {
            mutex.withLock {
                trafficBackfillRoadClasses.clear()
                trafficBackfillRoadClasses.addAll(roadClasses)
            }
        }
    }

    /**
     * @return the vanishing point of the route line if the vanishing route line feature was enabled
     * in the [MapboxRouteLineOptions]. If not 0.0 is returned.
     */
    fun getVanishPointOffset(): Double {
        return routeLineOptions.vanishingRouteLine?.vanishPointOffset ?: 0.0
    }

    /**
     * @return the routes being used
     */
    @Deprecated(
        "use #getNavigationRoutes instead",
        ReplaceWith(
            "getNavigationRoutes().toDirectionsRoutes()",
            "com.mapbox.navigation.base.route.toDirectionsRoutes"
        )
    )
    fun getRoutes(): List<DirectionsRoute> = routes.toDirectionsRoutes()

    /**
     * @return the primary route or null if there is none
     */
    @Deprecated(
        "use #getPrimaryNavigationRoute instead",
        ReplaceWith(
            "getPrimaryNavigationRoute()?.directionsRoute",
        )
    )
    fun getPrimaryRoute(): DirectionsRoute? = primaryRoute?.directionsRoute

    /**
     * @return the routes being used
     */
    fun getNavigationRoutes(): List<NavigationRoute> = routes

    /**
     * @return the primary route or null if there is none
     */
    fun getPrimaryNavigationRoute(): NavigationRoute? = primaryRoute

    /**
     * Sets the routes that will be operated on.
     *
     * This can be a long running task with long routes.
     * There is a cancel method which will cancel the background tasks.
     *
     * @param newRoutes one or more routes. The first route in the collection will be considered
     * the primary route and any additional routes will be alternate routes.
     * @param consumer a method that consumes the result of the operation.
     */
    @Deprecated(
        "use #setNavigationRouteLines(List<NavigationRouteLine>) instead",
        ReplaceWith(
            "setNavigationRouteLines(newRoutes.toNavigationRouteLines(), consumer)",
            "com.mapbox.navigation.ui.maps.route.line.model.toNavigationRouteLines"
        )
    )
    fun setRoutes(
        newRoutes: List<RouteLine>,
        consumer: MapboxNavigationConsumer<Expected<RouteLineError, RouteSetValue>>
    ) {
        setNavigationRouteLines(newRoutes.toNavigationRouteLines(), consumer)
    }

    /**
     * Sets the routes that will be operated on.
     *
     * This can be a long running task with long routes.
     * There is a cancel method which will cancel the background tasks.
     *
     * @param newRoutes one or more routes. The first route in the collection will be considered
     * the primary route and any additional routes will be alternate routes.
     * @param consumer a method that consumes the result of the operation.
     */
    fun setNavigationRoutes(
        newRoutes: List<NavigationRoute>,
        consumer: MapboxNavigationConsumer<Expected<RouteLineError, RouteSetValue>>
    ) {
        setNavigationRoutes(newRoutes, 0, consumer)
    }

    /**
     * Sets the routes that will be operated on.
     *
     * This can be a long running task with long routes.
     * There is a cancel method which will cancel the background tasks.
     *
     * @param newRoutes one or more routes. The first route in the collection will be considered
     * the primary route and any additional routes will be alternate routes.
     * @param activeLegIndex the index of the currently active leg of the primary route.
     *  This is used when [MapboxRouteLineOptions.styleInactiveRouteLegsIndependently] is enabled.
     * @param consumer a method that consumes the result of the operation.
     */
    fun setNavigationRoutes(
        newRoutes: List<NavigationRoute>,
        activeLegIndex: Int,
        consumer: MapboxNavigationConsumer<Expected<RouteLineError, RouteSetValue>>
    ) {
        setNavigationRoutes(
            newRoutes = newRoutes,
            activeLegIndex = activeLegIndex,
            alternativeRoutesMetadata = emptyList(),
            consumer = consumer
        )
    }

    /**
     * Sets the routes that will be operated on.
     *
     * This can be a long running task with long routes.
     * There is a cancel method which will cancel the background tasks.
     *
     * @param newRoutes one or more routes. The first route in the collection will be considered
     * the primary route and any additional routes will be alternate routes.
     * @param alternativeRoutesMetadata if available, the update will hide the portions of the alternative routes
     * until the deviation point with the primary route. See [MapboxNavigation.getAlternativeMetadataFor].
     * @param consumer a method that consumes the result of the operation.
     */
    fun setNavigationRoutes(
        newRoutes: List<NavigationRoute>,
        alternativeRoutesMetadata: List<AlternativeRouteMetadata>,
        consumer: MapboxNavigationConsumer<Expected<RouteLineError, RouteSetValue>>
    ) {
        setNavigationRoutes(newRoutes, 0, alternativeRoutesMetadata, consumer)
    }

    /**
     * Sets the routes that will be operated on.
     *
     * This can be a long running task with long routes.
     * There is a cancel method which will cancel the background tasks.
     *
     * @param newRoutes one or more routes. The first route in the collection will be considered
     * the primary route and any additional routes will be alternate routes.
     * @param activeLegIndex the index of the currently active leg of the primary route.
     *  This is used when [MapboxRouteLineOptions.styleInactiveRouteLegsIndependently] is enabled.
     * @param alternativeRoutesMetadata if available, the update will hide the portions of the alternative routes
     * until the deviation point with the primary route. See [MapboxNavigation.getAlternativeMetadataFor].
     * @param consumer a method that consumes the result of the operation.
     */
    fun setNavigationRoutes(
        newRoutes: List<NavigationRoute>,
        activeLegIndex: Int,
        alternativeRoutesMetadata: List<AlternativeRouteMetadata>,
        consumer: MapboxNavigationConsumer<Expected<RouteLineError, RouteSetValue>>
    ) {
        val routeLines = newRoutes.map {
            NavigationRouteLine(it, null)
        }
        setNavigationRouteLines(routeLines, activeLegIndex, alternativeRoutesMetadata, consumer)
    }

    /**
     * Sets the routes that will be operated on.
     *
     * This can be a long running task with long routes.
     * There is a cancel method which will cancel the background tasks.
     *
     * @param newRoutes one or more routes. The first route in the collection will be considered
     * the primary route and any additional routes will be alternate routes.
     * @param consumer a method that consumes the result of the operation.
     */
    fun setNavigationRouteLines(
        newRoutes: List<NavigationRouteLine>,
        consumer: MapboxNavigationConsumer<Expected<RouteLineError, RouteSetValue>>
    ) {
        setNavigationRouteLines(
            newRoutes = newRoutes,
            alternativeRoutesMetadata = emptyList(),
            consumer = consumer
        )
    }

    /**
     * Sets the routes that will be operated on.
     *
     * This can be a long running task with long routes.
     * There is a cancel method which will cancel the background tasks.
     *
     * @param newRoutes one or more routes. The first route in the collection will be considered
     * the primary route and any additional routes will be alternate routes.
     * @param alternativeRoutesMetadata if available, the update will hide the portions of the alternative routes
     * until the deviation point with the primary route. See [MapboxNavigation.getAlternativeMetadataFor].
     * @param consumer a method that consumes the result of the operation.
     */
    fun setNavigationRouteLines(
        newRoutes: List<NavigationRouteLine>,
        alternativeRoutesMetadata: List<AlternativeRouteMetadata>,
        consumer: MapboxNavigationConsumer<Expected<RouteLineError, RouteSetValue>>
    ) {
        setNavigationRouteLines(newRoutes, 0, alternativeRoutesMetadata, consumer)
    }

    /**
     * Sets the routes that will be operated on.
     *
     * This can be a long running task with long routes.
     * There is a cancel method which will cancel the background tasks.
     *
     * @param newRoutes one or more routes. The first route in the collection will be considered
     * the primary route and any additional routes will be alternate routes.
     * @param activeLegIndex the index of the currently active leg of the primary route.
     *  This is used when [MapboxRouteLineOptions.styleInactiveRouteLegsIndependently] is enabled.
     * @param alternativeRoutesMetadata if available, the update will hide the portions of the alternative routes
     * until the deviation point with the primary route. See [MapboxNavigation.getAlternativeMetadataFor].
     * @param consumer a method that consumes the result of the operation.
     */
    fun setNavigationRouteLines(
        newRoutes: List<NavigationRouteLine>,
        activeLegIndex: Int,
        alternativeRoutesMetadata: List<AlternativeRouteMetadata>,
        consumer: MapboxNavigationConsumer<Expected<RouteLineError, RouteSetValue>>
    ) {
        cancel()
        jobControl.scope.launch(Dispatchers.Main) {
            mutex.withLock {
                val featureDataProvider: () -> List<RouteFeatureData> =
                    MapboxRouteLineUtils.getRouteLineFeatureDataProvider(newRoutes)
                val routeData = setNewRouteData(
                    newRoutes.map(NavigationRouteLine::route),
                    featureDataProvider,
                    alternativeRoutesMetadata,
                    activeLegIndex
                )
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
        consumer: MapboxNavigationConsumer<Expected<RouteLineError, RouteSetValue>>
    ) {
        jobControl.scope.launch(Dispatchers.Main) {
            mutex.withLock {
                val featureDataProvider: () -> List<RouteFeatureData> =
                    MapboxRouteLineUtils.getRouteFeatureDataProvider(routes)
                val result = buildDrawRoutesState(featureDataProvider, activeLegIndex)
                consumer.accept(result)
            }
        }
    }

    /**
     * Indicates the point the route line should change from its default color to the vanishing
     * color behind the puck. Calling this method has no effect if the vanishing route line
     * feature was not enabled in the [MapboxRouteLineOptions].
     *
     * @param point representing the current position of the puck
     *
     * @return a value representing the updates to the route line's appearance or an error.
     */
    fun updateTraveledRouteLine(
        point: Point
    ): Expected<RouteLineError, RouteLineUpdateValue> {
        val currentNanoTime = System.nanoTime()
        if (routeLineOptions.vanishingRouteLine?.vanishingPointState ==
            VanishingPointState.DISABLED || currentNanoTime - lastIndexUpdateTimeNano >
            RouteLayerConstants.MAX_ELAPSED_SINCE_INDEX_UPDATE_NANO ||
            currentNanoTime - lastPointUpdateTimeNano <
            routeLineOptions.vanishingRouteLineUpdateIntervalNano
        ) {
            return ExpectedFactory.createError(
                RouteLineError(
                    "Vanishing point state is disabled or the update doesn't fall" +
                        "within the configured interval window.",
                    null
                )
            )
        }

        val workingRouteLineExpressionData =
            if (routeLineOptions.styleInactiveRouteLegsIndependently) {
                alternativelyStyleSegmentsNotInLeg(activeLegIndex, routeLineExpressionData)
            } else {
                routeLineExpressionData
            }

        val stopGap: Double = ifNonNull(primaryRoute?.directionsRoute) { route ->
            RouteLayerConstants.SOFT_GRADIENT_STOP_GAP_METERS / route.distance()
        } ?: .00000000001 // an arbitrarily small value so Expression values are in ascending order

        val restrictedExpressionData: List<ExtractedRouteData>? =
            ifNonNull(primaryRoute?.directionsRoute) { route ->
                if (routeLineOptions.displayRestrictedRoadSections && routeHasRestrictions) {
                    MapboxRouteLineUtils.extractRouteData(
                        route,
                        MapboxRouteLineUtils.getTrafficCongestionAnnotationProvider(
                            route,
                            routeLineOptions.resourceProvider.routeLineColorResources
                        )
                    )
                } else {
                    null
                }
            }

        val routeLineExpressionProviders =
            if (routeLineOptions.styleInactiveRouteLegsIndependently) {
                routeLineOptions.vanishingRouteLine?.getTraveledRouteLineExpressions(
                    point,
                    workingRouteLineExpressionData,
                    restrictedExpressionData,
                    routeLineOptions.resourceProvider,
                    activeLegIndex,
                    stopGap,
                    routeLineOptions.displaySoftGradientForTraffic,
                )
            } else {
                routeLineOptions.vanishingRouteLine?.getTraveledRouteLineExpressions(point)
            }

        lastPointUpdateTimeNano = System.nanoTime()
        return when (routeLineExpressionProviders) {
            null -> {
                ExpectedFactory.createError(
                    RouteLineError(
                        "No expression generated for update.",
                        null
                    )
                )
            }
            else -> {
                val alternativesProvider = {
                    throw UnsupportedOperationException(
                        "alternative routes do not support dynamic updates yet"
                    )
                }
                ExpectedFactory.createValue(
                    RouteLineUpdateValue(
                        primaryRouteLineDynamicData = RouteLineDynamicData(
                            routeLineExpressionProviders.routeLineExpression,
                            routeLineExpressionProviders.routeLineCasingExpression,
                            routeLineExpressionProviders.trafficLineExpression,
                            routeLineExpressionProviders.restrictedRoadExpression
                        ),
                        alternativeRouteLinesDynamicData = listOf(
                            RouteLineDynamicData(
                                alternativesProvider,
                                alternativesProvider,
                                alternativesProvider,
                                alternativesProvider
                            ),
                            RouteLineDynamicData(
                                alternativesProvider,
                                alternativesProvider,
                                alternativesProvider,
                                alternativesProvider
                            )
                        )
                    )
                )
            }
        }
    }

    /**
     * Clears the route line data.
     *
     * This method will execute tasks on a background thread.
     * There is a cancel method which will cancel the background tasks.
     *
     * @param consumer a callback to consume the result
     * @return a state representing the side effects to be rendered on the map. In this case
     * the map should appear without any route lines.
     */
    fun clearRouteLine(
        consumer: MapboxNavigationConsumer<Expected<RouteLineError, RouteLineClearValue>>
    ) {
        jobControl.scope.launch(Dispatchers.Main) {
            mutex.withLock {
                routeLineOptions.vanishingRouteLine?.clear()
                routeLineOptions.vanishingRouteLine?.vanishPointOffset = 0.0
                activeLegIndex = INVALID_ACTIVE_LEG_INDEX
                routes.clear()
                routeFeatureData.clear()
                routeLineExpressionData = emptyList()
                routeHasRestrictions = false
                resetCaches()

                consumer.accept(
                    ExpectedFactory.createValue(
                        RouteLineClearValue(
                            FeatureCollection.fromFeatures(listOf()),
                            listOf(
                                FeatureCollection.fromFeatures(listOf()),
                                FeatureCollection.fromFeatures(listOf())
                            ),
                            FeatureCollection.fromFeatures(listOf())
                        )
                    )
                )
            }
        }
    }

    /**
     * Sets the value of the vanishing point of the route line to the value specified. This is used
     * for the vanishing route line feature and is only applicable only if the feature was enabled
     * in the [MapboxRouteLineOptions].
     *
     * @param offset a value representing the percentage of the distance traveled along the route
     *
     * @return a state representing the side effects to be rendered on the map which will update
     * the appearance of the route line or an error.
     */
    fun setVanishingOffset(
        offset: Double
    ): Expected<RouteLineError, RouteLineUpdateValue> {
        routeLineOptions.vanishingRouteLine?.vanishPointOffset = offset
        return if (offset >= 0) {
            val workingExpressionData = if (routeLineOptions.styleInactiveRouteLegsIndependently) {
                alternativelyStyleSegmentsNotInLeg(activeLegIndex, routeLineExpressionData)
            } else {
                routeLineExpressionData
            }

            val trafficLineExpressionProvider = {
                MapboxRouteLineUtils.getTrafficLineExpression(
                    offset,
                    Color.TRANSPARENT,
                    routeLineOptions
                        .resourceProvider
                        .routeLineColorResources
                        .routeUnknownCongestionColor,
                    workingExpressionData
                )
            }
            val routeLineExpressionProvider = {
                MapboxRouteLineUtils.getRouteLineExpression(
                    offset,
                    routeLineOptions
                        .resourceProvider
                        .routeLineColorResources
                        .routeLineTraveledColor,
                    routeLineOptions.resourceProvider.routeLineColorResources.routeDefaultColor
                )
            }
            val routeLineCasingExpressionProvider = {
                MapboxRouteLineUtils.getRouteLineExpression(
                    offset,
                    routeLineOptions
                        .resourceProvider.routeLineColorResources.routeLineTraveledCasingColor,
                    routeLineOptions.resourceProvider.routeLineColorResources.routeCasingColor
                )
            }

            val restrictedLineExpressionProvider =
                ifNonNull(primaryRoute?.directionsRoute) { route ->
                    if (routeLineOptions.displayRestrictedRoadSections && routeHasRestrictions) {
                        {
                            val routeData = MapboxRouteLineUtils.extractRouteData(
                                route,
                                MapboxRouteLineUtils.getTrafficCongestionAnnotationProvider(
                                    route,
                                    routeLineOptions.resourceProvider.routeLineColorResources
                                )
                            )
                            MapboxRouteLineUtils.getRestrictedLineExpression(
                                offset,
                                activeLegIndex,
                                routeLineOptions
                                    .resourceProvider
                                    .routeLineColorResources
                                    .restrictedRoadColor,
                                routeData
                            )
                        }
                    } else {
                        null
                    }
                }

            val alternativesProvider = {
                throw UnsupportedOperationException(
                    "alternative routes do not support dynamic updates yet"
                )
            }
            ExpectedFactory.createValue(
                RouteLineUpdateValue(
                    primaryRouteLineDynamicData = RouteLineDynamicData(
                        routeLineExpressionProvider,
                        routeLineCasingExpressionProvider,
                        trafficLineExpressionProvider,
                        restrictedLineExpressionProvider
                    ),
                    alternativeRouteLinesDynamicData = listOf(
                        RouteLineDynamicData(
                            alternativesProvider,
                            alternativesProvider,
                            alternativesProvider,
                            alternativesProvider
                        ),
                        RouteLineDynamicData(
                            alternativesProvider,
                            alternativesProvider,
                            alternativesProvider,
                            alternativesProvider
                        )
                    )
                )
            )
        } else {
            ExpectedFactory.createError(
                RouteLineError("Offset value should be greater than or equal to 0", null)
            )
        }
    }

    /**
     * Updates the state of the route line based on data in the [RouteProgress] passing a result
     * to the consumer that should be rendered by the [MapboxRouteLineView].
     *
     * If the vanishing route line feature and style inactive route legs independently
     * features were not enabled in [MapboxRouteLineOptions], this method does not need to
     * be called as it won't produce any updates.
     *
     * This method will execute tasks on a background thread.
     * There is a cancel method which will cancel the background tasks.
     *
     * @param routeProgress a route progress object
     * @param consumer a consumer for the result of this call
     */
    fun updateWithRouteProgress(
        routeProgress: RouteProgress,
        consumer: MapboxNavigationConsumer<Expected<RouteLineError, RouteLineUpdateValue>>
    ) {
        updateUpcomingRoutePointIndex(routeProgress)
        updateVanishingPointState(routeProgress.currentState)

        // If the de-emphasize inactive route legs feature is enabled and the vanishing route line
        // feature is enabled and the active leg index has changed, then calling the
        // alternativelyStyleSegmentsNotInLeg() method here will get the resulting calculation cached so
        // that calls to alternativelyStyleSegmentsNotInLeg() made by updateTraveledRouteLine()
        // won't have to wait for the result. The updateTraveledRouteLine method is much
        // more time sensitive.
        if (routeLineOptions.styleInactiveRouteLegsIndependently) {
            when (routeLineOptions.vanishingRouteLine) {
                // If the styleInactiveRouteLegsIndependently feature is enabled but the
                // vanishingRouteLine feature is not enabled then side effects are generated and
                // need to be rendered.
                null -> highlightActiveLeg(routeProgress, consumer)
                else -> {
                    ifNonNull(routeProgress.currentLegProgress) { routeLegProgress ->
                        if (routeLegProgress.legIndex > activeLegIndex) {
                            jobControl.scope.launch(Dispatchers.Main) {
                                mutex.withLock {
                                    alternativelyStyleSegmentsNotInLeg(
                                        routeLegProgress.legIndex,
                                        routeLineExpressionData
                                    )
                                    activeLegIndex = routeLegProgress.legIndex
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Adjusts the route line visibility so that only the current route leg is visible. This is
     * intended to be used with routes that have multiple waypoints.
     *
     * This method will execute tasks on a background thread.
     * There is a cancel method which will cancel the background tasks.
     *
     * @param routeProgress a [RouteProgress]
     * @param consumer a consumer to receive the method result
     */
    private fun highlightActiveLeg(
        routeProgress: RouteProgress,
        consumer: MapboxNavigationConsumer<Expected<RouteLineError, RouteLineUpdateValue>>
    ) {
        when (routeProgress.currentLegProgress) {
            null -> {
                val expected =
                    ExpectedFactory.createError<RouteLineError, RouteLineUpdateValue>(
                        RouteLineError(
                            "No route set previous to highlighting the leg",
                            null
                        )
                    )
                consumer.accept(expected)
            }
            else -> {
                showRouteWithLegIndexHighlighted(
                    routeProgress.currentLegProgress!!.legIndex,
                    consumer
                )
            }
        }
    }

    /**
     * If successful this method returns a [RouteLineUpdateValue] that when rendered will
     * display the route line with the route leg indicated by the provided leg index highlighted.
     * All the other legs will only show a simple line with
     * [RouteLineColorResources.inActiveRouteLegsColor].
     *
     * This is intended to be used with routes that have multiple waypoints.
     * In addition, calling this method does not change the state of the route line.
     *
     * This method can be useful for showing a route overview with a specific route leg highlighted.
     *
     * This method will execute tasks on a background thread.
     * There is a cancel method which will cancel the background tasks.
     *
     * @param legIndexToHighlight the route leg index that should appear most prominent.
     * @param consumer a consumer to receive the method result
     */
    fun showRouteWithLegIndexHighlighted(
        legIndexToHighlight: Int,
        consumer: MapboxNavigationConsumer<Expected<RouteLineError, RouteLineUpdateValue>>
    ) {
        jobControl.scope.launch(Dispatchers.Main) {
            mutex.withLock {
                val expected = ifNonNull(primaryRoute?.directionsRoute?.legs()) { routeLegs ->
                    if (legIndexToHighlight in 0..routeLegs.lastIndex) {
                        val updatedRouteData = alternativelyStyleSegmentsNotInLeg(
                            legIndexToHighlight,
                            routeLineExpressionData
                        )
                        val routeLineExpressionProvider = {
                            MapboxRouteLineUtils.getRouteLineExpression(
                                0.0,
                                updatedRouteData,
                                routeLineOptions.resourceProvider
                                    .routeLineColorResources
                                    .routeDefaultColor,
                                routeLineOptions.resourceProvider
                                    .routeLineColorResources
                                    .routeDefaultColor,
                                routeLineOptions
                                    .resourceProvider
                                    .routeLineColorResources
                                    .inActiveRouteLegsColor,
                                legIndexToHighlight
                            )
                        }
                        val casingLineExpressionProvider = {
                            MapboxRouteLineUtils.getRouteLineExpression(
                                0.0,
                                updatedRouteData,
                                routeLineOptions.resourceProvider
                                    .routeLineColorResources.routeCasingColor,
                                routeLineOptions.resourceProvider
                                    .routeLineColorResources.routeCasingColor,
                                Color.TRANSPARENT,
                                legIndexToHighlight
                            )
                        }

                        val trafficLineExpressionProvider = {
                            MapboxRouteLineUtils.getTrafficLineExpression(
                                0.0,
                                routeLineOptions
                                    .resourceProvider
                                    .routeLineColorResources
                                    .routeLineTraveledColor,
                                routeLineOptions
                                    .resourceProvider
                                    .routeLineColorResources
                                    .routeUnknownCongestionColor,
                                updatedRouteData
                            )
                        }

                        val restrictedLineExpressionProvider = if (
                            routeLineOptions.displayRestrictedRoadSections && routeHasRestrictions
                        ) {
                            ifNonNull(primaryRoute?.directionsRoute) { route ->
                                {
                                    val expressionData =
                                        MapboxRouteLineUtils.extractRouteData(
                                            route,
                                            MapboxRouteLineUtils
                                                .getTrafficCongestionAnnotationProvider(
                                                    route,
                                                    routeLineOptions
                                                        .resourceProvider
                                                        .routeLineColorResources
                                                )
                                        )
                                    MapboxRouteLineUtils.getRestrictedLineExpression(
                                        0.0,
                                        legIndexToHighlight,
                                        routeLineOptions
                                            .resourceProvider
                                            .routeLineColorResources
                                            .restrictedRoadColor,
                                        expressionData
                                    )
                                }
                            }
                        } else {
                            null
                        }

                        val alternativesProvider = {
                            throw UnsupportedOperationException(
                                "alternative routes do not support dynamic updates yet"
                            )
                        }
                        ExpectedFactory.createValue(
                            RouteLineUpdateValue(
                                primaryRouteLineDynamicData = RouteLineDynamicData(
                                    routeLineExpressionProvider,
                                    casingLineExpressionProvider,
                                    trafficLineExpressionProvider,
                                    restrictedLineExpressionProvider
                                ),
                                alternativeRouteLinesDynamicData = listOf(
                                    RouteLineDynamicData(
                                        alternativesProvider,
                                        alternativesProvider,
                                        alternativesProvider,
                                        alternativesProvider
                                    ),
                                    RouteLineDynamicData(
                                        alternativesProvider,
                                        alternativesProvider,
                                        alternativesProvider,
                                        alternativesProvider
                                    )
                                )
                            )
                        )
                    } else {
                        ExpectedFactory.createError<RouteLineError, RouteLineUpdateValue>(
                            RouteLineError(
                                "Leg index provided is out of range of the primary " +
                                    "route legs collection.",
                                null
                            )
                        )
                    }
                } ?: ExpectedFactory.createError<RouteLineError, RouteLineUpdateValue>(
                    RouteLineError("", null)
                )
                consumer.accept(expected)
            }
        }
    }

    /**
     * The map will be queried for a route line feature at the target point or a bounding box
     * centered at the target point with a padding value determining the box's size. If a route
     * feature is found the index of that route in this class's route collection is returned. The
     * primary route is given precedence if more than one route is found.
     *
     * This method will execute tasks on a background thread.
     * There is a cancel method which will cancel the background tasks.
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
        resultConsumer: MapboxNavigationConsumer<Expected<RouteNotFound, ClosestRouteValue>>
    ) {
        jobControl.scope.launch(Dispatchers.Main) {
            mutex.withLock {
                val state = findClosestRoute(target, mapboxMap, padding)
                resultConsumer.accept(state)
            }
        }
    }

    /**
     * Cancels any/all background tasks that may be running.
     */
    fun cancel() {
        routeLineOptions.vanishingRouteLine?.cancel()
        jobControl.job.cancelChildren()
    }

    private suspend fun findClosestRoute(
        target: Point,
        mapboxMap: MapboxMap,
        padding: Float,
    ): Expected<RouteNotFound, ClosestRouteValue> {
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

        val primaryRouteLineLayers = ifNonNull(mapboxMap.getStyle()) { style ->
            MapboxRouteLineUtils.getLayerIdsForPrimaryRoute(
                style,
                MapboxRouteLineUtils.sourceLayerMap
            )
        } ?: setOf()

        val alternateRouteLayers = layerGroup1SourceLayerIds
            .union(layerGroup2SourceLayerIds)
            .union(layerGroup3SourceLayerIds)
            .subtract(primaryRouteLineLayers)

        val clickPointFeatureIndex = queryMapForFeatureIndex(
            mapboxMap,
            mapClickPoint,
            alternateRouteLayers.toList(),
            features
        )

        return if (clickPointFeatureIndex >= 0) {
            ExpectedFactory.createValue(
                ClosestRouteValue(routesAndFeatures[clickPointFeatureIndex].route)
            )
        } else {
            val clickRectFeatureIndex = queryMapForFeatureIndex(
                mapboxMap,
                clickRect,
                alternateRouteLayers.toList(),
                features
            )
            if (clickRectFeatureIndex >= 0) {
                ExpectedFactory.createValue(
                    ClosestRouteValue(routesAndFeatures[clickRectFeatureIndex].route)
                )
            } else {
                val index = queryMapForFeatureIndex(
                    mapboxMap,
                    mapClickPoint,
                    primaryRouteLineLayers.toList(),
                    features
                )
                if (index >= 0) {
                    ExpectedFactory.createValue(ClosestRouteValue(routesAndFeatures[index].route))
                } else {
                    ExpectedFactory.createError(
                        RouteNotFound("No route found in query area.", null)
                    )
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
                RenderedQueryOptions(layerIds, null)
            ) {
                val index = getIndexOfFirstFeature(it.value ?: listOf(), routeFeatures)
                continuation.resume(index)
            }
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
                RenderedQueryOptions(layerIds, null)
            ) {
                val index = getIndexOfFirstFeature(it.value ?: listOf(), routeFeatures)
                continuation.resume(index)
            }
        }
    }

    private fun getIndexOfFirstFeature(
        features: List<QueriedFeature>,
        routeFeatures: List<FeatureCollection>
    ): Int {
        return features.distinct().run {
            routeFeatures.indexOfFirst {
                (it.features()?.get(0)?.id() ?: 0) == this.firstOrNull()?.feature?.id()
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
            if (currentLegProgress.legIndex < completeRoutePoints.nestedList.size) {
                val currentLegSteps = completeRoutePoints.nestedList[currentLegProgress.legIndex]
                allRemainingPoints += if (currentStepProgress.stepIndex < currentLegSteps.size) {
                    currentLegSteps.slice(
                        currentStepProgress.stepIndex + 1 until currentLegSteps.size - 1
                    ).flatten().size
                } else {
                    0
                }
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
            routeLineOptions.vanishingRouteLine?.primaryRouteRemainingDistancesIndex =
                allPoints - allRemainingPoints - 1
        } ?: run { routeLineOptions.vanishingRouteLine?.primaryRouteRemainingDistancesIndex = null }

        lastIndexUpdateTimeNano = System.nanoTime()
    }

    internal fun updateVanishingPointState(routeProgressState: RouteProgressState) {
        routeLineOptions.vanishingRouteLine?.updateVanishingPointState(routeProgressState)
    }

    private suspend fun setNewRouteData(
        newRoutes: List<NavigationRoute>,
        featureDataProvider: () -> List<RouteFeatureData>,
        alternativeRoutesMetadata: List<AlternativeRouteMetadata>,
        activeLegIndex: Int,
    ): Expected<RouteLineError, RouteSetValue> {
        ifNonNull(newRoutes.firstOrNull()) { primaryRouteCandidate ->
            if (!primaryRouteCandidate.directionsRoute.isSameRoute(primaryRoute?.directionsRoute)) {
                routeLineOptions.vanishingRouteLine?.clear()
                routeLineOptions.vanishingRouteLine?.vanishPointOffset = 0.0
            }
        }

        routes.clear()
        routes.addAll(newRoutes)
        primaryRoute = newRoutes.firstOrNull()
        this.activeLegIndex = INVALID_ACTIVE_LEG_INDEX
        resetCaches()
        alternativesDeviationOffset =
            MapboxRouteLineUtils.getAlternativeRoutesDeviationOffsets(alternativeRoutesMetadata)

        return buildDrawRoutesState(featureDataProvider, activeLegIndex)
    }

    private suspend fun buildDrawRoutesState(
        featureDataProvider: () -> List<RouteFeatureData>,
        legIndex: Int
    ): Expected<RouteLineError, RouteSetValue> {
        val routeFeatureDataDef = jobControl.scope.async {
            featureDataProvider()
        }
        val routeFeatureDataResult = routeFeatureDataDef.await()
        if (routeFeatureDataResult.count { it.lineString.coordinates().size < 2 } > 0) {
            return ExpectedFactory.createError(
                RouteLineError(
                    "The route geometry contained less than two coordinates. " +
                        "At least two coordinates are required to render a route line.",
                    null
                )
            )
        }
        routeFeatureData.clear()
        routeFeatureData.addAll(routeFeatureDataResult)
        val partitionedRoutes = routeFeatureData.partition {
            it.route == routes.first()
        }
        val vanishingPointOffset = routeLineOptions.vanishingRouteLine?.vanishPointOffset ?: 0.0
        val routeLineColorResources = routeLineOptions.resourceProvider.routeLineColorResources
        val primaryRouteTrafficLineExpressionDef = jobControl.scope.async {
            partitionedRoutes.first.firstOrNull()?.route?.run {
                val workingRouteLineExpressionData =
                    MapboxRouteLineUtils.calculateRouteLineSegments(
                        this.directionsRoute,
                        trafficBackfillRoadClasses,
                        isPrimaryRoute = true,
                        routeLineColorResources,
                    ).run {
                        if (routeLineOptions.styleInactiveRouteLegsIndependently) {
                            alternativelyStyleSegmentsNotInLeg(
                                legIndex,
                                this,
                            )
                        } else {
                            this
                        }
                    }

                MapboxRouteLineUtils.getTrafficLineExpression(
                    vanishingPointOffset,
                    Color.TRANSPARENT,
                    routeLineColorResources.routeUnknownCongestionColor,
                    routeLineOptions.softGradientTransition / directionsRoute.distance(),
                    routeLineOptions.displaySoftGradientForTraffic,
                    workingRouteLineExpressionData
                )
            }
        }

        val primaryRouteBaseExpressionDef = jobControl.scope.async {
            if (routeLineOptions.styleInactiveRouteLegsIndependently) {
                MapboxRouteLineUtils.getRouteLineExpression(
                    vanishingPointOffset,
                    routeLineExpressionData,
                    routeLineColorResources.routeLineTraveledColor,
                    routeLineColorResources.routeDefaultColor,
                    routeLineColorResources.inActiveRouteLegsColor,
                    legIndex,
                )
            } else {
                MapboxRouteLineUtils.getRouteLineExpression(
                    vanishingPointOffset,
                    routeLineColorResources.routeLineTraveledColor,
                    routeLineColorResources.routeDefaultColor
                )
            }

            MapboxRouteLineUtils.getRouteLineExpression(
                vanishingPointOffset,
                routeLineColorResources.routeLineTraveledColor,
                routeLineColorResources.routeDefaultColor
            )
        }

        val primaryRouteCasingExpressionDef = jobControl.scope.async {
            if (routeLineOptions.styleInactiveRouteLegsIndependently) {
                MapboxRouteLineUtils.getRouteLineExpression(
                    vanishingPointOffset,
                    routeLineExpressionData,
                    routeLineColorResources.routeLineTraveledCasingColor,
                    routeLineColorResources.routeCasingColor,
                    Color.TRANSPARENT,
                    legIndex
                )
            } else {
                MapboxRouteLineUtils.getRouteLineExpression(
                    vanishingPointOffset,
                    routeLineColorResources.routeLineTraveledCasingColor,
                    routeLineColorResources.routeCasingColor,
                )
            }

            MapboxRouteLineUtils.getRouteLineExpression(
                vanishingPointOffset,
                routeLineColorResources.routeLineTraveledCasingColor,
                routeLineColorResources.routeCasingColor
            )
        }

        val primaryRouteTrailExpressionDef = jobControl.scope.async {
            MapboxRouteLineUtils.getRouteLineExpression(
                vanishingPointOffset,
                routeLineColorResources.routeLineTraveledColor,
                routeLineColorResources.routeLineTraveledColor
            )
        }

        val primaryRouteTrailCasingExpressionDef = jobControl.scope.async {
            MapboxRouteLineUtils.getRouteLineExpression(
                vanishingPointOffset,
                routeLineColorResources.routeLineTraveledCasingColor,
                routeLineColorResources.routeLineTraveledCasingColor
            )
        }

        val alternative1PercentageTraveled = partitionedRoutes.second.firstOrNull()?.route?.run {
            alternativesDeviationOffset[this.id]
        } ?: 0.0

        val alternateRoute1LineColors = getMatchingColors(
            partitionedRoutes.second.firstOrNull()?.featureCollection,
            routeLineOptions.routeStyleDescriptors,
            routeLineColorResources.alternativeRouteDefaultColor,
            routeLineColorResources.alternativeRouteCasingColor
        )

        val alternateRoute1BaseExpressionDef = jobControl.scope.async {
            MapboxRouteLineUtils.getRouteLineExpression(
                alternative1PercentageTraveled,
                Color.TRANSPARENT,
                alternateRoute1LineColors.first
            )
        }

        val alternateRoute1CasingExpressionDef = jobControl.scope.async {
            MapboxRouteLineUtils.getRouteLineExpression(
                alternative1PercentageTraveled,
                Color.TRANSPARENT,
                alternateRoute1LineColors.second
            )
        }

        val alternateRoute1TrailExpressionDef = jobControl.scope.async {
            MapboxRouteLineUtils.getRouteLineExpression(
                alternative1PercentageTraveled,
                Color.TRANSPARENT,
                Color.TRANSPARENT
            )
        }

        val alternateRoute1TrailCasingExpressionDef = jobControl.scope.async {
            MapboxRouteLineUtils.getRouteLineExpression(
                alternative1PercentageTraveled,
                Color.TRANSPARENT,
                Color.TRANSPARENT
            )
        }

        val alternateRoute1RestrictedSectionsExpressionDef = jobControl.scope.async {
            MapboxRouteLineUtils.getRouteLineExpression(
                alternative1PercentageTraveled,
                Color.TRANSPARENT,
                Color.TRANSPARENT
            )
        }

        val alternative2PercentageTraveled = partitionedRoutes.second.getOrNull(1)?.route?.run {
            alternativesDeviationOffset[this.id]
        } ?: 0.0

        val alternateRoute2LineColors = getMatchingColors(
            partitionedRoutes.second.getOrNull(1)?.featureCollection,
            routeLineOptions.routeStyleDescriptors,
            routeLineColorResources.alternativeRouteDefaultColor,
            routeLineColorResources.alternativeRouteCasingColor
        )

        val alternateRoute2BaseExpressionDef = jobControl.scope.async {
            MapboxRouteLineUtils.getRouteLineExpression(
                alternative2PercentageTraveled,
                Color.TRANSPARENT,
                alternateRoute2LineColors.first
            )
        }

        val alternateRoute2CasingExpressionDef = jobControl.scope.async {
            MapboxRouteLineUtils.getRouteLineExpression(
                alternative2PercentageTraveled,
                Color.TRANSPARENT,
                alternateRoute2LineColors.second
            )
        }

        val alternateRoute2TrailExpressionDef = jobControl.scope.async {
            MapboxRouteLineUtils.getRouteLineExpression(
                alternative2PercentageTraveled,
                Color.TRANSPARENT,
                Color.TRANSPARENT
            )
        }

        val alternateRoute2TrailCasingExpressionDef = jobControl.scope.async {
            MapboxRouteLineUtils.getRouteLineExpression(
                alternative2PercentageTraveled,
                Color.TRANSPARENT,
                Color.TRANSPARENT
            )
        }

        val alternateRoute2RestrictedSectionsExpressionDef = jobControl.scope.async {
            MapboxRouteLineUtils.getRouteLineExpression(
                alternative2PercentageTraveled,
                Color.TRANSPARENT,
                Color.TRANSPARENT
            )
        }

        val alternateRoute1TrafficExpressionDef = jobControl.scope.async {
            partitionedRoutes.second.firstOrNull()?.route?.run {
                MapboxRouteLineUtils.getTrafficLineExpressionProducer(
                    this.directionsRoute,
                    routeLineColorResources,
                    trafficBackfillRoadClasses,
                    false,
                    alternativesDeviationOffset[this.id] ?: 0.0,
                    Color.TRANSPARENT,
                    routeLineColorResources.alternativeRouteUnknownCongestionColor,
                    routeLineOptions.displaySoftGradientForTraffic,
                    routeLineOptions.softGradientTransition
                )
            }?.generateExpression()
        }

        val alternateRoute2TrafficExpressionDef = jobControl.scope.async {
            if (partitionedRoutes.second.size > 1) {
                MapboxRouteLineUtils.getTrafficLineExpressionProducer(
                    partitionedRoutes.second[1].route.directionsRoute,
                    routeLineColorResources,
                    trafficBackfillRoadClasses,
                    false,
                    alternativesDeviationOffset[partitionedRoutes.second[1].route.id] ?: 0.0,
                    Color.TRANSPARENT,
                    routeLineColorResources.alternativeRouteUnknownCongestionColor,
                    routeLineOptions.displaySoftGradientForTraffic,
                    routeLineOptions.softGradientTransition
                ).generateExpression()
            } else {
                null
            }
        }

        // If the displayRestrictedRoadSections is true then produce a gradient that is transparent
        // except for the restricted sections. If false produce a gradient for the restricted
        // line layer that is completely transparent.
        val primaryRouteRestrictedSectionsExpressionDef = jobControl.scope.async {
            partitionedRoutes.first.firstOrNull()?.route?.run {
                if (routeLineOptions.displayRestrictedRoadSections) {
                    MapboxRouteLineUtils.getRestrictedLineExpressionProducer(
                        this.directionsRoute,
                        0.0,
                        legIndex,
                        routeLineColorResources
                    )
                } else {
                    MapboxRouteLineUtils.getDisabledRestrictedLineExpressionProducer(
                        0.0,
                        legIndex,
                        routeLineColorResources.restrictedRoadColor
                    )
                }
            }?.generateExpression()
        }

        val wayPointsFeatureCollectionDef = jobControl.scope.async {
            partitionedRoutes.first.firstOrNull()?.route?.run {
                MapboxRouteLineUtils.buildWayPointFeatureCollection(this)
            } ?: FeatureCollection.fromFeatures(listOf())
        }

        val primaryRouteSource = partitionedRoutes.first.firstOrNull()?.featureCollection
            ?: FeatureCollection.fromFeatures(
                listOf()
            )
        val alternativeRoute1FeatureCollection =
            partitionedRoutes.second.firstOrNull()?.featureCollection
                ?: FeatureCollection.fromFeatures(listOf())
        val alternativeRoute2FeatureCollection = if (partitionedRoutes.second.size > 1) {
            partitionedRoutes.second[1].featureCollection
        } else {
            FeatureCollection.fromFeatures(listOf())
        }

        val wayPointsFeatureCollection = wayPointsFeatureCollectionDef.await()

        if (routeLineOptions.displayRestrictedRoadSections) {
            jobControl.scope.launch(Dispatchers.Main) {
                val routeHasRestrictionsDef =
                    jobControl.scope.async {
                        partitionedRoutes.first.firstOrNull()?.route?.run {
                            MapboxRouteLineUtils.routeHasRestrictions(this.directionsRoute)
                        } ?: false
                    }
                routeHasRestrictions = routeHasRestrictionsDef.await()
            }
        }

        // The RouteLineExpressionData is only needed if the vanishing route line feature
        // or styleInactiveRouteLegsIndependently feature are enabled.
        if (
            routeLineOptions.vanishingRouteLine != null ||
            routeLineOptions.styleInactiveRouteLegsIndependently
        ) {
            jobControl.scope.launch(Dispatchers.Main) {
                val segmentsDef = jobControl.scope.async {
                    partitionedRoutes.first.firstOrNull()?.route?.run {
                        MapboxRouteLineUtils.calculateRouteLineSegments(
                            this.directionsRoute,
                            trafficBackfillRoadClasses,
                            true,
                            routeLineColorResources
                        )
                    } ?: listOf()
                }
                routeLineExpressionData = segmentsDef.await()
            }
        }

        // This call is resource intensive so it needs to come last so that
        // it doesn't consume resources used by the calculations above. The results
        // of this call aren't necessary to return to the caller but the calculations above are.
        // Putting this call above will delay the caller receiving the result. There is a check
        // for primaryRouteLineGranularDistances being null because this value is cleared only
        // if the primary route has changed via setRoutes. If there's no change in the primary
        // route there's no need to re-init the vanishing route line which is an expensive
        // operation.
        if (routeLineOptions.vanishingRouteLine?.primaryRouteLineGranularDistances == null) {
            partitionedRoutes.first.firstOrNull()?.let {
                routeLineOptions.vanishingRouteLine?.initWithRoute(it.route)
            }
        }

        val primaryRouteTrafficLineExpressionProducer =
            ifNonNull(primaryRouteTrafficLineExpressionDef.await()) { exp ->
                RouteLineExpressionProvider { exp }
            }

        val alternateRoute1TrafficExpressionProducer =
            ifNonNull(alternateRoute1TrafficExpressionDef.await()) { exp ->
                RouteLineExpressionProvider { exp }
            }

        val alternateRoute2TrafficExpressionProducer =
            ifNonNull(alternateRoute2TrafficExpressionDef.await()) { exp ->
                RouteLineExpressionProvider { exp }
            }

        val primaryRouteBaseExpression = primaryRouteBaseExpressionDef.await()
        val primaryRouteBaseExpressionProducer =
            RouteLineExpressionProvider { primaryRouteBaseExpression }

        val primaryRouteCasingExpression = primaryRouteCasingExpressionDef.await()
        val primaryRouteCasingExpressionProducer =
            RouteLineExpressionProvider { primaryRouteCasingExpression }

        val primaryRouteTrailExpression = primaryRouteTrailExpressionDef.await()
        val primaryRouteTrailExpressionProducer =
            RouteLineExpressionProvider { primaryRouteTrailExpression }

        val primaryRouteTrailCasingExpression = primaryRouteTrailCasingExpressionDef.await()
        val primaryRouteTrailCasingExpressionProducer =
            RouteLineExpressionProvider { primaryRouteTrailCasingExpression }

        val alternateRoute1BaseExpression = alternateRoute1BaseExpressionDef.await()
        val alternateRoute1BaseExpressionProducer =
            RouteLineExpressionProvider { alternateRoute1BaseExpression }

        val alternateRoute1CasingExpression = alternateRoute1CasingExpressionDef.await()
        val alternateRoute1CasingExpressionProducer =
            RouteLineExpressionProvider { alternateRoute1CasingExpression }

        val alternateRoute1TrailExpression = alternateRoute1TrailExpressionDef.await()
        val alternateRoute1TrailExpressionProducer =
            RouteLineExpressionProvider { alternateRoute1TrailExpression }

        val alternateRoute1TrailCasingExpression = alternateRoute1TrailCasingExpressionDef.await()
        val alternateRoute1TrailCasingExpressionProducer =
            RouteLineExpressionProvider { alternateRoute1TrailCasingExpression }

        val alternateRoute1RestrictedSectionsExpression =
            alternateRoute1RestrictedSectionsExpressionDef.await()
        val alternateRoute1RestrictedSectionsExpressionProducer =
            RouteLineExpressionProvider { alternateRoute1RestrictedSectionsExpression }

        val alternateRoute2BaseExpression = alternateRoute2BaseExpressionDef.await()
        val alternateRoute2BaseExpressionProducer =
            RouteLineExpressionProvider { alternateRoute2BaseExpression }

        val alternateRoute2CasingExpression = alternateRoute2CasingExpressionDef.await()
        val alternateRoute2CasingExpressionProducer =
            RouteLineExpressionProvider { alternateRoute2CasingExpression }

        val alternateRoute2TrailExpression = alternateRoute2TrailExpressionDef.await()
        val alternateRoute2TrailExpressionProducer =
            RouteLineExpressionProvider { alternateRoute2TrailExpression }

        val alternateRoute2TrailCasingExpression = alternateRoute2TrailCasingExpressionDef.await()
        val alternateRoute2TrailCasingExpressionProducer =
            RouteLineExpressionProvider { alternateRoute2TrailCasingExpression }

        val alternateRoute2RestrictedSectionsExpression =
            alternateRoute2RestrictedSectionsExpressionDef.await()
        val alternateRoute2RestrictedSectionsExpressionProducer =
            RouteLineExpressionProvider { alternateRoute2RestrictedSectionsExpression }

        val primaryRouteRestrictedSectionsExpressionProducer =
            ifNonNull(primaryRouteRestrictedSectionsExpressionDef.await()) { exp ->
                RouteLineExpressionProvider { exp }
            }

        return ExpectedFactory.createValue(
            RouteSetValue(
                primaryRouteLineData = RouteLineData(
                    primaryRouteSource,
                    RouteLineDynamicData(
                        primaryRouteBaseExpressionProducer,
                        primaryRouteCasingExpressionProducer,
                        primaryRouteTrafficLineExpressionProducer,
                        primaryRouteRestrictedSectionsExpressionProducer,
                        RouteLineTrimOffset(vanishingPointOffset),
                        primaryRouteTrailExpressionProducer,
                        primaryRouteTrailCasingExpressionProducer
                    )
                ),
                alternativeRouteLinesData = listOf(
                    RouteLineData(
                        alternativeRoute1FeatureCollection,
                        RouteLineDynamicData(
                            alternateRoute1BaseExpressionProducer,
                            alternateRoute1CasingExpressionProducer,
                            alternateRoute1TrafficExpressionProducer,
                            alternateRoute1RestrictedSectionsExpressionProducer,
                            RouteLineTrimOffset(alternative1PercentageTraveled),
                            alternateRoute1TrailExpressionProducer,
                            alternateRoute1TrailCasingExpressionProducer
                        )
                    ),
                    RouteLineData(
                        alternativeRoute2FeatureCollection,
                        RouteLineDynamicData(
                            alternateRoute2BaseExpressionProducer,
                            alternateRoute2CasingExpressionProducer,
                            alternateRoute2TrafficExpressionProducer,
                            alternateRoute2RestrictedSectionsExpressionProducer,
                            RouteLineTrimOffset(alternative2PercentageTraveled),
                            alternateRoute2TrailExpressionProducer,
                            alternateRoute2TrailCasingExpressionProducer
                        )
                    )
                ),
                wayPointsFeatureCollection,
            )
        )
    }

    private fun resetCaches() {
        MapboxRouteLineUtils.resetCache()
        alternativelyStyleSegmentsNotInLegCache.evictAll()
    }

    internal val alternativelyStyleSegmentsNotInLeg: (
        activeLegIndex: Int,
        segments: List<RouteLineExpressionData>
    ) -> List<RouteLineExpressionData> =
        { activeLegIndex: Int, segments: List<RouteLineExpressionData> ->
            segments.parallelMap(
                {
                    if (it.legIndex != activeLegIndex) {
                        it.copy(
                            segmentColor = routeLineOptions
                                .resourceProvider
                                .routeLineColorResources
                                .inActiveRouteLegsColor
                        )
                    } else {
                        it
                    }
                },
                jobControl.scope
            )
        }.cacheResult(alternativelyStyleSegmentsNotInLegCache)
}
