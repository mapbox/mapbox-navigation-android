package com.mapbox.navigation.ui.maps.route.line.api

import android.graphics.Color
import android.util.LruCache
import androidx.annotation.VisibleForTesting
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.Layer
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.CoalescingBlockingQueue
import com.mapbox.navigation.base.internal.utils.isSameRoute
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.LowMemoryManager
import com.mapbox.navigation.core.routealternatives.AlternativeRouteMetadata
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.maps.internal.route.callout.api.MapboxRouteCalloutsApi
import com.mapbox.navigation.ui.maps.internal.route.callout.api.RoutesSetToRouteLineObserver
import com.mapbox.navigation.ui.maps.internal.route.callout.model.RouteCalloutData
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils.extractRouteRestrictionData
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils.getCongestionColorTypeForInactiveRouteLegs
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils.getPrimaryRouteLineDynamicData
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils.getRestrictedLineExpressionProducer
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils.getSingleColorExpression
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils.granularDistancesProvider
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils.layerGroup1SourceLayerIds
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils.layerGroup2SourceLayerIds
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils.layerGroup3SourceLayerIds
import com.mapbox.navigation.ui.maps.internal.route.line.toStylePropertyValue
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants
import com.mapbox.navigation.ui.maps.route.line.RouteLineHistoryRecordingApiSender
import com.mapbox.navigation.ui.maps.route.line.model.ClosestRouteValue
import com.mapbox.navigation.ui.maps.route.line.model.ExtractedRouteRestrictionData
import com.mapbox.navigation.ui.maps.route.line.model.InactiveRouteColors
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineApiOptions
import com.mapbox.navigation.ui.maps.route.line.model.NavigationRouteLine
import com.mapbox.navigation.ui.maps.route.line.model.RouteFeatureData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineClearValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineDynamicData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineError
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineExpressionData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineTrimOffset
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineUpdateValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteNotFound
import com.mapbox.navigation.ui.maps.route.line.model.RouteSetValue
import com.mapbox.navigation.ui.maps.route.line.model.SegmentColorType
import com.mapbox.navigation.ui.maps.route.line.model.VanishingPointState
import com.mapbox.navigation.ui.maps.route.line.model.VanishingRouteLineExpressions
import com.mapbox.navigation.ui.maps.util.CacheResultUtils
import com.mapbox.navigation.ui.maps.util.CacheResultUtils.cacheResult
import com.mapbox.navigation.ui.utils.internal.ifNonNull
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import com.mapbox.navigation.utils.internal.logD
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigation.utils.internal.logW
import com.mapbox.navigation.utils.internal.parallelMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.CopyOnWriteArrayList

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
 * [MapboxRouteLineView] classes and maintaining a reference to them. Both classes need options objects to be configured.
 * The default options can be used as a starting point
 * so the simplest usage would look like:
 *
 * ```java
 * MapboxRouteLineApiOptions mapboxRouteLineApiOptions = new MapboxRouteLineApiOptions.Builder().build();
 * MapboxRouteLineViewOptions mapboxRouteLineViewOptions = new MapboxRouteLineViewOptions.Builder(context).build();
 * MapboxRouteLineApi mapboxRouteLineApi = new MapboxRouteLineApi(mapboxRouteLineApiOptions);
 * MapboxRouteLineView mapboxRouteLineView = new MapboxRouteLineView(mapboxRouteLineViewOptions);
 * ```
 *
 * or
 *
 * ```kotlin
 * val mapboxRouteLineApiOptions = MapboxRouteLineApiOptions.Builder().build()
 * val mapboxRouteLineViewOptions = MapboxRouteLineViewOptions.Builder(context).build()
 * val mapboxRouteLineApi = MapboxRouteLineApi(mapboxRouteLineApiOptions)
 * val mapboxRouteLineView = MapboxRouteLineView(mapboxRouteLineViewOptions)
 * ```
 *
 * Note that it's possible to have one instance of [MapboxRouteLineApi] and multiple instances of
 * [MapboxRouteLineView] that will consume the data from the same [MapboxRouteLineApi].
 * Those [MapboxRouteLineView] might have different styling configurations.
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
 * 1. Enable the feature in the [MapboxRouteLineApiOptions]
 * ```kotlin
 * MapboxRouteLineApiOptions.Builder()
 * .vanishingRouteLineEnabled(true)
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
class MapboxRouteLineApi @VisibleForTesting internal constructor(
    private val routeLineOptions: MapboxRouteLineApiOptions,
    private val calculationsScope: CoroutineScope,
    private var vanishingRouteLine: VanishingRouteLine?,
    private val sender: RouteLineHistoryRecordingApiSender,
    private val lowMemoryManager: LowMemoryManager,
) {
    private var primaryRoute: NavigationRoute? = null
    private var routes: List<NavigationRoute> = emptyList()
    private var alternativeRoutesMetadata: List<AlternativeRouteMetadata> = emptyList()
    private var routeLineExpressionData: List<RouteLineExpressionData> = emptyList()
    private var restrictedExpressionData: List<ExtractedRouteRestrictionData> = emptyList()
    private var lastIndexUpdateTimeNano: Long = 0
    private var lastPointUpdateTimeNano: Long = 0
    private val routeFeatureData: MutableList<RouteFeatureData> = mutableListOf()
    private val mutex = Mutex()

    private val lowMemoryObserver = LowMemoryManager.Observer {
        resetCaches()
    }

    private var isMemoryMonitorObserverRegistered = false

    // We had a bug that when styleInactiveRouteLegsIndependently was enabled but we first
    // got route progress update and only then routes update,
    // there was a race that the independent legs might not have been styled independently
    // until we switch to the next leg.
    // The race scheme:
    // 1. Invoke setNavigationRouteLines;
    // 2. Set new routes;
    // 3. Suspend before calculation routeLineExpressionData;
    // 4. Invoke updateWithRouteProgress;
    // 5. Change leg, set activeLegIndex to 0;
    // 6. Invoke provideLegUpdate that styles inactive leg independently based on routeLineExpressionData;
    // 7. Only here actually initialize the routeLineExpressionData in setNavigationRouteLines;
    // Result: we styles independently the inactive legs based on empty data.
    // And the state doesn't restore itself, because on the next updateWithRouteProgress leg didn't change ->
    // we don't invoke provideLegUpdate (that's an optimization we still need).
    // The solution was to use mutex in updateWithRouteProgress, and this JobControl handles the coalescing queue:
    // since we might wait some time for the mutex, we want to cancel the update that waits for the mutex
    // if a newer one arrives.
    private val routeProgressUpdatesJobControl =
        InternalJobControlFactory.createImmediateMainScopeJobControl()
    private val routeProgressUpdatesQueue = CoalescingBlockingQueue(
        routeProgressUpdatesJobControl.scope,
        mutex,
    )
    internal var activeLegIndex = INVALID_ACTIVE_LEG_INDEX
        private set
    private val trafficBackfillRoadClasses = CopyOnWriteArrayList<String>()
    private var alternativesDeviationOffset = mapOf<String, Double>()

    private val alternativelyStyleSegmentsNotInLegCache: LruCache<
        CacheResultUtils.CacheResultKey3<
            Int, List<RouteLineExpressionData>, InactiveRouteColors,
            List<RouteLineExpressionData>,
            >,
        List<RouteLineExpressionData>,> by lazy { LruCache(4) }

    private var lastLocationPoint: Point? = null

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    private val calloutApi: MapboxRouteCalloutsApi?

    private val routesSetToRouteLineObservers = CopyOnWriteArrayList<RoutesSetToRouteLineObserver>()

    companion object {
        private const val INVALID_ACTIVE_LEG_INDEX = -1
        private const val LOG_CATEGORY = "MapboxRouteLineApi"
    }

    /**
     * Creates an instance of [MapboxRouteLineApi].
     *
     * @param options used for determining the appearance and/or behavior of the route line
     */
    constructor(options: MapboxRouteLineApiOptions) : this(
        options,
        InternalJobControlFactory.createDefaultScopeJobControl().scope,
        if (options.vanishingRouteLineEnabled) {
            VanishingRouteLine()
        } else {
            null
        },
        RouteLineHistoryRecordingApiSender(),
        LowMemoryManager.create(),
    )

    init {
        sender.sendOptionsEvent(routeLineOptions)
        trafficBackfillRoadClasses.addAll(
            routeLineOptions.trafficBackfillRoadClasses,
        )
        @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
        calloutApi = if (routeLineOptions.isRouteCalloutsEnabled) {
            MapboxRouteCalloutsApi()
        } else {
            null
        }
    }

    private fun startMemoryMonitoring() {
        synchronized(lowMemoryObserver) {
            if (isMemoryMonitorObserverRegistered) return
            isMemoryMonitorObserverRegistered = true
            lowMemoryManager.addObserver(lowMemoryObserver)
        }
    }

    private fun stopMemoryMonitoring() {
        synchronized(lowMemoryObserver) {
            if (!isMemoryMonitorObserverRegistered) return
            isMemoryMonitorObserverRegistered = false
            lowMemoryManager.removeObserver(lowMemoryObserver)
        }
    }

    internal fun registerRoutesSetToRouteLineObserver(observer: RoutesSetToRouteLineObserver) {
        routesSetToRouteLineObservers.add(observer)
        observer.onSet(routes, alternativeRoutesMetadata)
    }

    internal fun unregisterRoutesSetToRouteLineObserver(observer: RoutesSetToRouteLineObserver) {
        routesSetToRouteLineObservers.remove(observer)
    }

    /**
     * Replaces the traffic back fill road classes derived from [MapboxRouteLineApiOptions].
     *
     * @param roadClasses the road class collection that should be used in place of those
     * from the [MapboxRouteLineApiOptions]
     */
    fun setRoadClasses(roadClasses: List<String>) {
        calculationsScope.launch(Dispatchers.Main) {
            mutex.withLock {
                trafficBackfillRoadClasses.clear()
                trafficBackfillRoadClasses.addAll(roadClasses)
            }
        }
    }

    /**
     * @return the vanishing point of the route line if the vanishing route line feature was enabled
     * in the [MapboxRouteLineApiOptions]. If not 0.0 is returned.
     */
    fun getVanishPointOffset(): Double {
        return vanishingRouteLine?.vanishPointOffset ?: 0.0
    }

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
    fun setNavigationRoutes(
        newRoutes: List<NavigationRoute>,
        consumer: MapboxNavigationConsumer<Expected<RouteLineError, RouteSetValue>>,
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
     *  This is used when [MapboxRouteLineApiOptions.styleInactiveRouteLegsIndependently] is enabled.
     * @param consumer a method that consumes the result of the operation.
     */
    fun setNavigationRoutes(
        newRoutes: List<NavigationRoute>,
        activeLegIndex: Int,
        consumer: MapboxNavigationConsumer<Expected<RouteLineError, RouteSetValue>>,
    ) {
        setNavigationRoutes(
            newRoutes = newRoutes,
            activeLegIndex = activeLegIndex,
            alternativeRoutesMetadata = emptyList(),
            consumer = consumer,
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
        consumer: MapboxNavigationConsumer<Expected<RouteLineError, RouteSetValue>>,
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
     *  This is used when [MapboxRouteLineApiOptions.styleInactiveRouteLegsIndependently] is enabled.
     * @param alternativeRoutesMetadata if available, the update will hide the portions of the alternative routes
     * until the deviation point with the primary route. See [MapboxNavigation.getAlternativeMetadataFor].
     * @param consumer a method that consumes the result of the operation.
     */
    fun setNavigationRoutes(
        newRoutes: List<NavigationRoute>,
        activeLegIndex: Int,
        alternativeRoutesMetadata: List<AlternativeRouteMetadata>,
        consumer: MapboxNavigationConsumer<Expected<RouteLineError, RouteSetValue>>,
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
        consumer: MapboxNavigationConsumer<Expected<RouteLineError, RouteSetValue>>,
    ) {
        setNavigationRouteLines(
            newRoutes = newRoutes,
            alternativeRoutesMetadata = emptyList(),
            consumer = consumer,
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
        consumer: MapboxNavigationConsumer<Expected<RouteLineError, RouteSetValue>>,
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
     *  This is used when [MapboxRouteLineApiOptions.styleInactiveRouteLegsIndependently] is enabled.
     * @param alternativeRoutesMetadata if available, the update will hide the portions of the alternative routes
     * until the deviation point with the primary route. See [MapboxNavigation.getAlternativeMetadataFor].
     * @param consumer a method that consumes the result of the operation.
     */
    fun setNavigationRouteLines(
        newRoutes: List<NavigationRouteLine>,
        activeLegIndex: Int,
        alternativeRoutesMetadata: List<AlternativeRouteMetadata>,
        consumer: MapboxNavigationConsumer<Expected<RouteLineError, RouteSetValue>>,
    ) {
        calculationsScope.coroutineContext.cancelChildren()
        if (newRoutes.isEmpty()) {
            clearRouteLine { clearRouteLineResult ->
                val result = clearRouteLineResult.mapValue { clearValue ->
                    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
                    RouteSetValue(
                        primaryRouteLineData = RouteLineData(
                            featureCollection = clearValue.primaryRouteSource,
                        ),
                        alternativeRouteLinesData = clearValue.alternativeRoutesSources.map {
                            RouteLineData(featureCollection = it)
                        },

                        callouts = clearValue.callouts,
                        waypointsSource = clearValue.waypointsSource,
                    )
                }
                consumer.accept(result)
            }
        } else {
            startMemoryMonitoring()

            calculationsScope.launch(Dispatchers.Main) {
                mutex.withLock {
                    // To not depend on FreeDrive recording: we want options all the time,
                    // but the object is usually created in FreeDrive
                    sender.sendOptionsEvent(routeLineOptions)
                    sender.sendSetRoutesEvent(newRoutes, activeLegIndex)
                    val featureDataProvider: () -> List<RouteFeatureData> =
                        MapboxRouteLineUtils.getRouteLineFeatureDataProvider(newRoutes)
                    val routeData = setNewRouteData(
                        newRoutes.map(NavigationRouteLine::route),
                        featureDataProvider,
                        alternativeRoutesMetadata,
                        activeLegIndex,
                    )
                    consumer.accept(routeData)
                }
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
        consumer: MapboxNavigationConsumer<Expected<RouteLineError, RouteSetValue>>,
    ) {
        calculationsScope.launch(Dispatchers.Main) {
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
     * feature was not enabled in the [MapboxRouteLineApiOptions].
     *
     * @param point representing the current position of the puck
     *
     * @return a value representing the updates to the route line's appearance or an error.
     */
    fun updateTraveledRouteLine(
        point: Point,
    ): Expected<RouteLineError, RouteLineUpdateValue> {
        val currentNanoTime = System.nanoTime()
        if (vanishingRouteLine?.vanishingPointState ==
            VanishingPointState.DISABLED || currentNanoTime - lastIndexUpdateTimeNano >
            RouteLayerConstants.MAX_ELAPSED_SINCE_INDEX_UPDATE_NANO ||
            currentNanoTime - lastPointUpdateTimeNano <
            routeLineOptions.vanishingRouteLineUpdateIntervalNano
        ) {
            return ExpectedFactory.createError(
                RouteLineError(
                    "Vanishing point state is disabled or the update doesn't fall " +
                        "within the configured interval window.",
                    null,
                ),
            )
        }

        if (point == lastLocationPoint) {
            return ExpectedFactory.createError(
                RouteLineError(
                    "Provided point is equal to the last update, skipping recalculation.",
                    null,
                ),
            )
        }

        sender.sendUpdateTraveledRouteLineEvent(point)
        lastLocationPoint = point

        val routeLineExpressionProviders = ifNonNull(primaryRoute) { route ->
            startMemoryMonitoring()

            ifNonNull(granularDistancesProvider(route)) { granularDistances ->
                vanishingRouteLine?.getTraveledRouteLineExpressions(
                    point,
                    granularDistances,
                )
            }
        }

        lastPointUpdateTimeNano = System.nanoTime()
        return getTrimOffsetUpdate(routeLineExpressionProviders)
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
        consumer: MapboxNavigationConsumer<Expected<RouteLineError, RouteLineClearValue>>,
    ) {
        stopMemoryMonitoring()

        calculationsScope.launch(Dispatchers.Main) {
            mutex.withLock {
                sender.sendClearRouteLineEvent()
                lastLocationPoint = null
                vanishingRouteLine?.vanishPointOffset = 0.0
                activeLegIndex = INVALID_ACTIVE_LEG_INDEX
                primaryRoute = null
                routes = emptyList()
                alternativeRoutesMetadata = emptyList()
                routeFeatureData.clear()
                routeLineExpressionData = emptyList()
                resetCaches()
                routesSetToRouteLineObservers.forEach {
                    it.onSet(routes, alternativeRoutesMetadata)
                }

                consumer.accept(
                    ExpectedFactory.createValue(
                        @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
                        RouteLineClearValue(
                            FeatureCollection.fromFeatures(listOf()),
                            listOf(
                                FeatureCollection.fromFeatures(listOf()),
                                FeatureCollection.fromFeatures(listOf()),
                            ),
                            FeatureCollection.fromFeatures(listOf()),
                            RouteCalloutData(listOf()),
                        ),
                    ),
                )
            }
        }
    }

    /**
     * Sets the value of the vanishing point of the route line to the value specified. This is used
     * for the vanishing route line feature and is only applicable only if the feature was enabled
     * in the [MapboxRouteLineApiOptions].
     *
     * @param offset a value representing the percentage of the distance traveled along the route
     *
     * @return a state representing the side effects to be rendered on the map which will update
     * the appearance of the route line or an error. Note that an error will be returned in case
     * no routes had been set to [MapboxRouteLineApi] prior to this invocation.
     */
    fun setVanishingOffset(
        offset: Double,
    ): Expected<RouteLineError, RouteLineUpdateValue> {
        sender.sendSetVanishingOffsetEvent(offset)
        return ifNonNull(primaryRoute, vanishingRouteLine) { _, vanishingRouteLine ->
            startMemoryMonitoring()

            val expression = vanishingRouteLine.getTraveledRouteLineExpressions(offset)
            getTrimOffsetUpdate(expression)
        } ?: ExpectedFactory.createError(
            RouteLineError(
                "there's no route set or vanishing route line is disabled",
                null,
            ),
        )
    }

    /**
     * Updates the state of the route line based on data in the [RouteProgress] passing a result
     * to the consumer that should be rendered by the [MapboxRouteLineView].
     *
     * Calling this method and rendering the result is required in order to use the vanishing
     * route line feature and/or to style inactive route legs independently and/or display multi-leg
     * routes with the active leg appearing to overlap the inactive leg(s).
     *
     * This method will execute tasks on a background thread.
     * There is a cancel method which will cancel the background tasks.
     *
     * @param routeProgress a route progress object
     * @param consumer a consumer for the result of this call
     */
    fun updateWithRouteProgress(
        routeProgress: RouteProgress,
        consumer: MapboxNavigationConsumer<Expected<RouteLineError, RouteLineUpdateValue>>,
    ) {
        routeProgressUpdatesQueue.addJob(
            CoalescingBlockingQueue.Item(
                {
                    sender.sendUpdateWithRouteProgressEvent(routeProgress)
                    val currentPrimaryRoute = primaryRoute
                    val currentLegProgress = routeProgress.currentLegProgress
                    val currentLegIndex = routeProgress.currentLegProgress?.legIndex
                    if (currentPrimaryRoute == null) {
                        val msg =
                            "You're calling #updateWithRouteProgress without any routes being set."
                        consumer.accept(
                            ExpectedFactory.createError(RouteLineError(msg, throwable = null)),
                        )
                        logW(msg, LOG_CATEGORY)
                    } else if (currentPrimaryRoute.id != routeProgress.navigationRoute.id) {
                        val msg = "Provided primary route (#setNavigationRoutes, ID: " +
                            "${currentPrimaryRoute.id}) and navigated route " +
                            "(#updateWithRouteProgress, ID: ${routeProgress.navigationRoute.id}) " +
                            "are not the same. Aborting the update."
                        consumer.accept(
                            ExpectedFactory.createError(RouteLineError(msg, throwable = null)),
                        )
                        logE(msg, LOG_CATEGORY)
                    } else if (currentLegProgress == null || currentLegIndex == null) {
                        val msg = "Provided route progress has invalid leg progress."
                        consumer.accept(
                            ExpectedFactory.createError(RouteLineError(msg, throwable = null)),
                        )
                        logE(msg, LOG_CATEGORY)
                    } else {
                        startMemoryMonitoring()

                        updateUpcomingRoutePointIndex(routeProgress)
                        updateVanishingPointState(routeProgress.currentState)

                        val legChange = currentLegIndex != activeLegIndex
                        activeLegIndex = currentLegIndex

                        val (maskingLayerData, routeLineData) = if (legChange) {
                            val vanishingOffset = vanishingRouteLine?.vanishPointOffset ?: 0.0
                            val maskingLayerData = getRouteLineDynamicDataForMaskingLayers(
                                currentPrimaryRoute,
                                vanishingOffset,
                                currentLegProgress,
                            )
                            val routeLineData =
                                if (routeLineOptions.styleInactiveRouteLegsIndependently) {
                                    getPrimaryRouteLineDynamicData(
                                        routeLineOptions = routeLineOptions,
                                        routeLineExpressionData =
                                        routeLineActiveLegExpressionData(activeLegIndex),
                                        restrictedExpressionData = restrictedExpressionData,
                                        primaryRouteDistance =
                                        currentPrimaryRoute.directionsRoute.distance(),
                                        vanishingPointOffset =
                                        vanishingOffset,
                                        legIndex = activeLegIndex,
                                    )
                                } else {
                                    null
                                }
                            maskingLayerData to routeLineData
                        } else {
                            null to null
                        }
                        val alternativesCommandHolder = unsupportedRouteLineCommandHolder()
                        consumer.accept(
                            ExpectedFactory.createValue(
                                RouteLineUpdateValue(
                                    primaryRouteLineDynamicData = routeLineData,
                                    alternativeRouteLinesDynamicData = listOf(
                                        RouteLineDynamicData(
                                            alternativesCommandHolder,
                                            alternativesCommandHolder,
                                            alternativesCommandHolder,
                                            alternativesCommandHolder,
                                        ),
                                        RouteLineDynamicData(
                                            alternativesCommandHolder,
                                            alternativesCommandHolder,
                                            alternativesCommandHolder,
                                            alternativesCommandHolder,
                                        ),
                                    ),
                                    routeLineMaskingLayerDynamicData = maskingLayerData,
                                ),
                            ),
                        )
                    }
                },
                {
                    val msg = "Skipping #updateWithRouteProgress because a newer one is available."
                    consumer.accept(
                        ExpectedFactory.createError(RouteLineError(msg, throwable = null)),
                    )
                    logW(msg, LOG_CATEGORY)
                },
            ),
        )
    }

    internal fun getRouteLineDynamicDataForMaskingLayers(
        segments: List<RouteLineExpressionData>,
        vanishingOffset: Double?,
        distance: Double,
        legIndex: Int,
    ): RouteLineDynamicData {
        val alteredSegments = alternativelyStyleSegmentsNotInLeg(
            legIndex,
            segments,
            InactiveRouteColors(SegmentColorType.TRANSPARENT),
        )
        val trafficExpProvider = RouteLineValueCommandHolder(
            // TODO why is this a "light" expression instead of "heavy"?
            //  Congestion generation for initial draw uses "heavy"
            LightRouteLineValueProvider {
                MapboxRouteLineUtils.getTrafficLineExpression(
                    it,
                    0.0,
                    Color.TRANSPARENT,
                    SegmentColorType.PRIMARY_UNKNOWN_CONGESTION,
                    alteredSegments,
                    distance,
                ).toStylePropertyValue()
            },
            LineGradientCommandApplier(),
        )
        val mainExpCommandHolder = RouteLineValueCommandHolder(
            LightRouteLineValueProvider {
                MapboxRouteLineUtils.getExpressionSubstitutingColorForInactiveLegs(
                    0.0,
                    segments,
                    Color.TRANSPARENT,
                    it.routeLineColorResources.routeDefaultColor,
                    Color.TRANSPARENT,
                    legIndex,
                )
            },
            LineGradientCommandApplier(),
        )
        val casingExpApplier = RouteLineValueCommandHolder(
            LightRouteLineValueProvider {
                MapboxRouteLineUtils.getExpressionSubstitutingColorForInactiveLegs(
                    0.0,
                    segments,
                    Color.TRANSPARENT,
                    it.routeLineColorResources.routeCasingColor,
                    Color.TRANSPARENT,
                    legIndex,
                )
            },
            LineGradientCommandApplier(),
        )
        val trailExpCommandHolder = RouteLineValueCommandHolder(
            LightRouteLineValueProvider {
                MapboxRouteLineUtils.getExpressionSubstitutingColorForInactiveLegs(
                    0.0,
                    segments,
                    Color.TRANSPARENT,
                    it.routeLineColorResources.routeLineTraveledColor,
                    Color.TRANSPARENT,
                    legIndex,
                )
            },
            LineGradientCommandApplier(),
        )
        val trailCasingExpCommandHolder = RouteLineValueCommandHolder(
            LightRouteLineValueProvider {
                MapboxRouteLineUtils.getExpressionSubstitutingColorForInactiveLegs(
                    0.0,
                    segments,
                    Color.TRANSPARENT,
                    it
                        .routeLineColorResources
                        .routeLineTraveledCasingColor,
                    Color.TRANSPARENT,
                    legIndex,
                )
            },
            LineGradientCommandApplier(),
        )
        val restrictedExpCommandHolder = RouteLineValueCommandHolder(
            // TODO why is this a "light" expression instead of "heavy"?
            //  Congestion generation for initial draw uses "heavy"
            LightRouteLineValueProvider(
                getRestrictedLineExpressionProducer(
                    routeLineOptions,
                    restrictedExpressionData,
                    0.0,
                    legIndex,
                    inactiveColorType = SegmentColorType.TRANSPARENT,
                ),
            ),
            LineGradientCommandApplier(),
        )

        return RouteLineDynamicData(
            mainExpCommandHolder,
            casingExpApplier,
            trafficExpProvider,
            trimOffset = vanishingOffset?.let { RouteLineTrimOffset(it) },
            restrictedSectionExpressionCommandHolder = restrictedExpCommandHolder,
            trailExpressionCommandHolder = trailExpCommandHolder,
            trailCasingExpressionCommandHolder = trailCasingExpCommandHolder,
        )
    }

    /*
    December 2022 there was a feature request that the active leg of a route visually appear
    above the inactive legs.  The default behavior of the Map SDK at this time was that when
    a line overlapped itself the later part of the line appears above the earlier part of the line.
    A customer requested the opposite of this behavior. Specifically the active route leg should
    appear above the inactive route leg(s). To achieve this result additional layers were added
    to mask the primary route line. The section of the route line representing the inactive route
    legs is made transparent revealing the primary route line.  The section of the route line
    representing the active leg is left opaque masking that section of the primary route line.
    These masking layers are kept above the layers used for the primary route line, sharing the
    same source data.  The route only has one leg the additional gradient calculations aren't
    performed and the masking layers are transparent.
     */
    internal fun getRouteLineDynamicDataForMaskingLayers(
        route: NavigationRoute,
        vanishingOffset: Double?,
        currentLegProgress: RouteLegProgress,
    ): RouteLineDynamicData? {
        val numLegs = route.directionsRoute.legs()?.size ?: 0
        val legIndex = currentLegProgress.legIndex
        return if (route.isMultiLeg() && legIndex < numLegs) {
            getRouteLineDynamicDataForMaskingLayers(
                routeLineExpressionData,
                vanishingOffset,
                route.directionsRoute.distance(),
                legIndex,
            )
        } else {
            null
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
        resultConsumer: MapboxNavigationConsumer<Expected<RouteNotFound, ClosestRouteValue>>,
    ) {
        calculationsScope.launch(Dispatchers.Main) {
            if (!mapboxMap.isValid()) {
                resultConsumer.accept(
                    ExpectedFactory.createError(
                        RouteNotFound("MapboxMap instance is invalid", null),
                    ),
                )
            } else {
                val featuresDataCopy: List<RouteFeatureData>
                mutex.withLock {
                    featuresDataCopy = routeFeatureData.toList()
                }
                val state = findClosestRoute(target, mapboxMap, padding, featuresDataCopy)
                mutex.withLock {
                    if (
                        featuresDataCopy.map { it.route.id } == routeFeatureData.map { it.route.id }
                    ) {
                        resultConsumer.accept(state)
                    } else {
                        resultConsumer.accept(
                            ExpectedFactory.createError(
                                RouteNotFound("Routes have changed", null),
                            ),
                        )
                    }
                }
            }
        }
    }

    /**
     * Cancels any/all background tasks that may be running.
     */
    fun cancel() {
        sender.sendCancelEvent()
        calculationsScope.coroutineContext.cancelChildren()
        routeProgressUpdatesJobControl.job.cancelChildren()

        stopMemoryMonitoring()
        resetCaches()
    }

    private suspend fun findClosestRoute(
        target: Point,
        mapboxMap: MapboxMap,
        padding: Float,
        featuresData: List<RouteFeatureData>,
    ): Expected<RouteNotFound, ClosestRouteValue> {
        val routesAndFeatures = featuresData.toList()
        val features = routesAndFeatures.map { it.reversedFeatureCollection }

        val primaryRouteLineLayers = ifNonNull(mapboxMap.getStyle()) { style ->
            MapboxRouteLineUtils.getLayerIdsForPrimaryRoute(
                style,
                MapboxRouteLineUtils.sourceLayerMap,
            )
        } ?: setOf()

        val alternateRouteLayers = layerGroup1SourceLayerIds
            .union(layerGroup2SourceLayerIds)
            .union(layerGroup3SourceLayerIds)
            .subtract(primaryRouteLineLayers)
            .toList()
        val primaryRouteLineLayersList = primaryRouteLineLayers.toList()
        val mapClickPoint = mapboxMap.pixelForCoordinate(target)

        val closestRouteHandler = CompositeClosestRouteHandlerProvider.createHandler(
            listOf(
                SinglePointClosestRouteHandler(primaryRouteLineLayersList),
                RectClosestRouteHandler(primaryRouteLineLayersList, padding),
                SinglePointClosestRouteHandler(alternateRouteLayers),
                RectClosestRouteHandler(alternateRouteLayers, padding),
            ),
        )

        val result = closestRouteHandler.handle(mapboxMap, mapClickPoint, features)
        return result.fold(
            {
                ExpectedFactory.createError(
                    RouteNotFound("No route found in query area.", null),
                )
            },
            {
                ExpectedFactory.createValue(
                    ClosestRouteValue(routesAndFeatures[it].route),
                )
            },
        )
    }

    internal fun updateUpcomingRoutePointIndex(routeProgress: RouteProgress) {
        vanishingRouteLine?.upcomingRouteGeometrySegmentIndex =
            routeProgress.currentRouteGeometryIndex + 1

        lastIndexUpdateTimeNano = System.nanoTime()
    }

    internal fun updateVanishingPointState(routeProgressState: RouteProgressState) {
        vanishingRouteLine?.updateVanishingPointState(routeProgressState)
    }

    private suspend fun setNewRouteData(
        newRoutes: List<NavigationRoute>,
        featureDataProvider: () -> List<RouteFeatureData>,
        alternativeRoutesMetadata: List<AlternativeRouteMetadata>,
        activeLegIndex: Int,
    ): Expected<RouteLineError, RouteSetValue> {
        val distinctNewRoutes = newRoutes.distinctBy { it.id }
        if (distinctNewRoutes.size < newRoutes.size) {
            logW(
                "Routes provided to MapboxRouteLineApi contain duplicates " +
                    "(based on NavigationRoute#id) - using only distinct instances",
                LOG_CATEGORY,
            )
        }
        logD(LOG_CATEGORY) {
            "setNewRouteData: distinct routes ids are: " +
                distinctNewRoutes.joinToString(", ") { it.id }
        }
        val distinctAlternativeRouteMetadata = alternativeRoutesMetadata.filter { metadata ->
            distinctNewRoutes.find { it.id == metadata.navigationRoute.id } != null
        }

        ifNonNull(distinctNewRoutes.firstOrNull()) { primaryRouteCandidate ->
            if (!primaryRouteCandidate.directionsRoute.isSameRoute(primaryRoute?.directionsRoute)) {
                vanishingRouteLine?.vanishPointOffset = 0.0
            }
        }

        this.alternativeRoutesMetadata = distinctAlternativeRouteMetadata

        routes = distinctNewRoutes
        routesSetToRouteLineObservers.forEach { it.onSet(routes, alternativeRoutesMetadata) }

        primaryRoute = distinctNewRoutes.firstOrNull()
        logD(LOG_CATEGORY) {
            "trimming route data caches to size ${distinctNewRoutes.size}"
        }
        MapboxRouteLineUtils.trimRouteDataCacheToSize(size = distinctNewRoutes.size)
        this.activeLegIndex = activeLegIndex

        preWarmRouteCaches(
            distinctNewRoutes,
            vanishingRouteLineEnabled = vanishingRouteLine != null,
            alternativeRouteMetadataAvailable = distinctAlternativeRouteMetadata.isNotEmpty(),
        )

        alternativesDeviationOffset = distinctAlternativeRouteMetadata.associate {
            it.navigationRoute.id to MapboxRouteLineUtils.getAlternativeRouteDeviationOffsets(it)
        }

        return buildDrawRoutesState(featureDataProvider, activeLegIndex)
    }

    private suspend fun preWarmRouteCaches(
        routes: List<NavigationRoute>,
        vanishingRouteLineEnabled: Boolean,
        alternativeRouteMetadataAvailable: Boolean,
    ) {
        if (routes.isEmpty()) return
        withContext(calculationsScope.coroutineContext) {
            if (vanishingRouteLineEnabled) {
                granularDistancesProvider(routes.first())
            }
            if (alternativeRouteMetadataAvailable) {
                routes.drop(1).forEach {
                    granularDistancesProvider(it)
                }
            }
        }

        restrictedExpressionData = if (routeLineOptions.calculateRestrictedRoadSections) {
            extractRouteRestrictionData(routes.first(), granularDistancesProvider)
        } else {
            listOf()
        }
    }

    private suspend fun buildDrawRoutesState(
        featureDataProvider: () -> List<RouteFeatureData>,
        legIndex: Int,
    ): Expected<RouteLineError, RouteSetValue> {
        lastLocationPoint = null

        val routeFeatureDataDef = calculationsScope.async {
            featureDataProvider()
        }
        val routeFeatureDataResult = routeFeatureDataDef.await()
        if (routeFeatureDataResult.count { it.coordinatesCount < 2 } > 0) {
            return ExpectedFactory.createError(
                RouteLineError(
                    "The route geometry contained less than two coordinates. " +
                        "At least two coordinates are required to render a route line.",
                    null,
                ),
            )
        }
        routeFeatureData.clear()
        routeFeatureData.addAll(routeFeatureDataResult)
        val partitionedRoutes = routeFeatureData.partition {
            it.route == routes.first()
        }

        val primaryRoute = partitionedRoutes.first.firstOrNull()
            ?: return ExpectedFactory.createError(
                RouteLineError(
                    "There's no primary route to be drawn.",
                    null,
                ),
            )
        val alternativeRoute1 = partitionedRoutes.second.firstOrNull()
        val alternativeRoute2 = partitionedRoutes.second.getOrNull(1)

        val vanishingPointOffset = vanishingRouteLine?.vanishPointOffset ?: 0.0

        val routeLineExpressionDataDef = calculationsScope.async {
            primaryRoute.route.run {
                MapboxRouteLineUtils.calculateRouteLineSegments(
                    this,
                    trafficBackfillRoadClasses,
                    isPrimaryRoute = true,
                    routeLineOptions,
                )
            }
        }
        val wayPointsFeatureCollectionDef = calculationsScope.async {
            primaryRoute.route.run {
                MapboxRouteLineUtils.buildWayPointFeatureCollection(this)
            }
        }

        routeLineExpressionData = routeLineExpressionDataDef.await()

        val alternative1PercentageTraveled = alternativeRoute1?.route?.run {
            alternativesDeviationOffset[this.id]
        } ?: 0.0

        val alternative2PercentageTraveled =
            alternativeRoute2?.route?.run {
                alternativesDeviationOffset[this.id]
            } ?: 0.0

        val alternateRoute1TrafficExpressionCommandHolder =
            alternativeRoute1?.route?.let { route ->
                RouteLineValueCommandHolder(
                    HeavyRouteLineValueProvider {
                        MapboxRouteLineUtils.getTrafficLineExpression(
                            route,
                            routeLineOptions,
                            it,
                            trafficBackfillRoadClasses,
                            isPrimaryRoute = false,
                            vanishingPointOffset = alternativesDeviationOffset[route.id] ?: 0.0,
                            Color.TRANSPARENT,
                            SegmentColorType.ALTERNATIVE_UNKNOWN_CONGESTION,
                        ).toStylePropertyValue()
                    },
                    LineGradientCommandApplier(),
                )
            }

        val alternateRoute2TrafficExpressionCommandHolder =
            if (partitionedRoutes.second.size > 1) {
                RouteLineValueCommandHolder(
                    HeavyRouteLineValueProvider {
                        MapboxRouteLineUtils.getTrafficLineExpression(
                            alternativeRoute2!!.route,
                            routeLineOptions,
                            it,
                            trafficBackfillRoadClasses,
                            false,
                            alternativesDeviationOffset[alternativeRoute2.route.id] ?: 0.0,
                            Color.TRANSPARENT,
                            SegmentColorType.ALTERNATIVE_UNKNOWN_CONGESTION,
                        ).toStylePropertyValue()
                    },
                    LineGradientCommandApplier(),
                )
            } else {
                null
            }

        val primaryRouteSource = primaryRoute.reversedFeatureCollection
        val alternativeRoute1FeatureCollection =
            alternativeRoute1?.reversedFeatureCollection
                ?: FeatureCollection.fromFeatures(listOf())
        val alternativeRoute2FeatureCollection = alternativeRoute2?.reversedFeatureCollection
            ?: FeatureCollection.fromFeatures(listOf())
        val wayPointsFeatureCollection = wayPointsFeatureCollectionDef.await()

        val alternateRoute1BaseExpressionCommandHolder = RouteLineValueCommandHolder(
            LightRouteLineValueProvider {
                MapboxRouteLineUtils.getRouteLineExpression(
                    alternative1PercentageTraveled,
                    Color.TRANSPARENT,
                    it.routeLineColorResources.alternativeRouteDefaultColor,
                )
            },
            LineGradientCommandApplier(),
        )

        val alternateRoute1CasingExpressionCommandHolder = RouteLineValueCommandHolder(
            LightRouteLineValueProvider {
                MapboxRouteLineUtils.getRouteLineExpression(
                    alternative1PercentageTraveled,
                    Color.TRANSPARENT,
                    it.routeLineColorResources.alternativeRouteCasingColor,
                )
            },
            LineGradientCommandApplier(),
        )

        val alternateRoute1TrailExpressionCommandHolder = RouteLineValueCommandHolder(
            LightRouteLineValueProvider {
                getSingleColorExpression(Color.TRANSPARENT)
            },
            LineGradientCommandApplier(),
        )

        val alternateRoute1TrailCasingExpressionCommandHolder = RouteLineValueCommandHolder(
            LightRouteLineValueProvider {
                getSingleColorExpression(Color.TRANSPARENT)
            },
            LineGradientCommandApplier(),
        )

        val alternateRoute1RestrictedSectionsExpressionCommandHolder =
            RouteLineValueCommandHolder(
                LightRouteLineValueProvider {
                    getSingleColorExpression(Color.TRANSPARENT)
                },
                LineGradientCommandApplier(),
            )

        val alternateRoute1BlurExpressionHolder = RouteLineValueCommandHolder(
            LightRouteLineValueProvider {
                getSingleColorExpression(Color.TRANSPARENT)
            },
            LineGradientCommandApplier(),
        )

        val alternateRoute2BaseExpressionCommandHolder = RouteLineValueCommandHolder(
            LightRouteLineValueProvider {
                MapboxRouteLineUtils.getRouteLineExpression(
                    alternative2PercentageTraveled,
                    Color.TRANSPARENT,
                    it.routeLineColorResources.alternativeRouteDefaultColor,
                )
            },
            LineGradientCommandApplier(),
        )

        val alternateRoute2CasingExpressionCommandHolder = RouteLineValueCommandHolder(
            LightRouteLineValueProvider {
                MapboxRouteLineUtils.getRouteLineExpression(
                    alternative2PercentageTraveled,
                    Color.TRANSPARENT,
                    it.routeLineColorResources.alternativeRouteCasingColor,
                )
            },
            LineGradientCommandApplier(),
        )

        val alternateRoute2TrailExpressionCommandHolder = RouteLineValueCommandHolder(
            LightRouteLineValueProvider {
                getSingleColorExpression(Color.TRANSPARENT)
            },
            LineGradientCommandApplier(),
        )

        val alternateRoute2TrailCasingExpressionCommandHolder = RouteLineValueCommandHolder(
            LightRouteLineValueProvider {
                getSingleColorExpression(Color.TRANSPARENT)
            },
            LineGradientCommandApplier(),
        )

        val alternateRoute2RestrictedSectionsExpressionCommandHolder =
            RouteLineValueCommandHolder(
                LightRouteLineValueProvider {
                    getSingleColorExpression(Color.TRANSPARENT)
                },
                LineGradientCommandApplier(),
            )

        val alternateRoute2BlurExpressionHolder = RouteLineValueCommandHolder(
            LightRouteLineValueProvider {
                getSingleColorExpression(Color.TRANSPARENT)
            },
            LineGradientCommandApplier(),
        )

        val navigationRoute = primaryRoute.route
        val maskingLayerData = if (navigationRoute.isMultiLeg()) {
            getRouteLineDynamicDataForMaskingLayers(
                routeLineExpressionData,
                vanishingPointOffset,
                navigationRoute.directionsRoute.distance(),
                legIndex,
            )
        } else {
            val exp = getSingleColorExpression(Color.TRANSPARENT)
            RouteLineDynamicData(
                baseExpressionCommandHolder = RouteLineValueCommandHolder(
                    LightRouteLineValueProvider { exp },
                    LineGradientCommandApplier(),
                ),
                casingExpressionCommandHolder = RouteLineValueCommandHolder(
                    LightRouteLineValueProvider { exp },
                    LineGradientCommandApplier(),
                ),
                trafficExpressionCommandHolder = RouteLineValueCommandHolder(
                    LightRouteLineValueProvider { exp },
                    LineGradientCommandApplier(),
                ),
                restrictedSectionExpressionCommandHolder = RouteLineValueCommandHolder(
                    LightRouteLineValueProvider { exp },
                    LineGradientCommandApplier(),
                ),
                trimOffset = RouteLineTrimOffset(vanishingPointOffset),
                trailExpressionCommandHolder = RouteLineValueCommandHolder(
                    LightRouteLineValueProvider { exp },
                    LineGradientCommandApplier(),
                ),
                trailCasingExpressionCommandHolder = RouteLineValueCommandHolder(
                    LightRouteLineValueProvider { exp },
                    LineGradientCommandApplier(),
                ),
            )
        }

        @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
        val routeCalloutData = calloutApi?.setNavigationRoutes(routes, alternativeRoutesMetadata)
            ?: RouteCalloutData(emptyList())

        return ExpectedFactory.createValue(
            @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
            RouteSetValue(
                primaryRouteLineData = RouteLineData(
                    primaryRouteSource,
                    getPrimaryRouteLineDynamicData(
                        routeLineOptions = routeLineOptions,
                        routeLineExpressionData = routeLineActiveLegExpressionData(legIndex),
                        restrictedExpressionData = restrictedExpressionData,
                        primaryRouteDistance = primaryRoute.route.directionsRoute.distance(),
                        vanishingPointOffset = vanishingPointOffset,
                        legIndex = legIndex,
                    ),
                ),
                alternativeRouteLinesData = listOf(
                    RouteLineData(
                        alternativeRoute1FeatureCollection,
                        RouteLineDynamicData(
                            alternateRoute1BaseExpressionCommandHolder,
                            alternateRoute1CasingExpressionCommandHolder,
                            alternateRoute1TrafficExpressionCommandHolder,
                            alternateRoute1RestrictedSectionsExpressionCommandHolder,
                            RouteLineTrimOffset(alternative1PercentageTraveled),
                            alternateRoute1TrailExpressionCommandHolder,
                            alternateRoute1TrailCasingExpressionCommandHolder,
                            alternateRoute1BlurExpressionHolder,
                        ),
                    ),
                    RouteLineData(
                        alternativeRoute2FeatureCollection,
                        RouteLineDynamicData(
                            alternateRoute2BaseExpressionCommandHolder,
                            alternateRoute2CasingExpressionCommandHolder,
                            alternateRoute2TrafficExpressionCommandHolder,
                            alternateRoute2RestrictedSectionsExpressionCommandHolder,
                            RouteLineTrimOffset(alternative2PercentageTraveled),
                            alternateRoute2TrailExpressionCommandHolder,
                            alternateRoute2TrailCasingExpressionCommandHolder,
                            alternateRoute2BlurExpressionHolder,
                        ),
                    ),
                ),
                wayPointsFeatureCollection,
                routeCalloutData,
                maskingLayerData,
            ),
        )
    }

    private fun resetCaches() {
        MapboxRouteLineUtils.trimRouteDataCacheToSize(size = 0)
        alternativelyStyleSegmentsNotInLegCache.evictAll()
    }

    internal val alternativelyStyleSegmentsNotInLeg: (
        Int,
        List<RouteLineExpressionData>,
        InactiveRouteColors,
    ) -> List<RouteLineExpressionData> = {
            activeLegIndex: Int,
            segments: List<RouteLineExpressionData>,
            colors: InactiveRouteColors, ->
        segments.parallelMap(
            {
                if (it.legIndex != activeLegIndex) {
                    it.copyWithNewSegmentColorType(
                        newSegmentColorType = getCongestionColorTypeForInactiveRouteLegs(
                            congestionValue = it.congestionValue,
                            colors,
                        ),
                    )
                } else {
                    it
                }
            },
            calculationsScope,
        )
    }.cacheResult(alternativelyStyleSegmentsNotInLegCache)

    private suspend fun routeLineActiveLegExpressionData(
        legIndex: Int,
    ): List<RouteLineExpressionData> {
        return if (routeLineOptions.styleInactiveRouteLegsIndependently) {
            calculationsScope.async {
                alternativelyStyleSegmentsNotInLeg(
                    legIndex,
                    routeLineExpressionData,
                    InactiveRouteColors(),
                )
            }.await()
        } else {
            routeLineExpressionData
        }
    }

    private fun getTrimOffsetUpdate(
        vanishingRouteLineExpressions: VanishingRouteLineExpressions?,
    ): Expected<RouteLineError, RouteLineUpdateValue> {
        return when (vanishingRouteLineExpressions) {
            null -> {
                ExpectedFactory.createError(
                    RouteLineError(
                        "No expression generated for update.",
                        null,
                    ),
                )
            }

            else -> {
                val alternativesHolder = unsupportedRouteLineCommandHolder()
                val primaryLayerDynamicData = vanishingRouteLineExpressions.let {
                    RouteLineDynamicData(
                        it.routeLineValueCommandHolder,
                        it.routeLineCasingExpressionCommandHolder,
                        it.trafficLineExpressionCommandHolder,
                        it.restrictedRoadExpressionCommandHolder,
                        blurExpressionCommandHolder = it.routeLineValueCommandHolder,
                    )
                }
                val maskingLayerDynamicData = if (primaryRoute.isMultiLeg()) {
                    primaryLayerDynamicData
                } else {
                    null
                }

                ExpectedFactory.createValue(
                    RouteLineUpdateValue(
                        primaryRouteLineDynamicData = primaryLayerDynamicData,
                        alternativeRouteLinesDynamicData = listOf(
                            RouteLineDynamicData(
                                alternativesHolder,
                                alternativesHolder,
                                alternativesHolder,
                                alternativesHolder,
                            ),
                            RouteLineDynamicData(
                                alternativesHolder,
                                alternativesHolder,
                                alternativesHolder,
                                alternativesHolder,
                            ),
                        ),
                        routeLineMaskingLayerDynamicData = maskingLayerDynamicData,
                    ),
                )
            }
        }
    }

    private fun NavigationRoute?.isMultiLeg(): Boolean {
        return (this?.directionsRoute?.legs()?.size ?: 0) >= 2
    }
}
