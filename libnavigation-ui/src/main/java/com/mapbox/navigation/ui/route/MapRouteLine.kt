package com.mapbox.navigation.ui.route

import android.content.Context
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.SparseArray
import androidx.annotation.AnyRes
import androidx.annotation.ColorInt
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.geojson.utils.PolylineUtils
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentConstants
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.expressions.Expression
import com.mapbox.mapboxsdk.style.layers.Layer
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineGradient
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonOptions
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.ui.R
import com.mapbox.navigation.ui.internal.route.MapRouteSourceProvider
import com.mapbox.navigation.ui.internal.route.RouteConstants
import com.mapbox.navigation.ui.internal.route.RouteConstants.ALTERNATIVE_ROUTE_CASING_LAYER_ID
import com.mapbox.navigation.ui.internal.route.RouteConstants.ALTERNATIVE_ROUTE_LAYER_ID
import com.mapbox.navigation.ui.internal.route.RouteConstants.ALTERNATIVE_ROUTE_SOURCE_ID
import com.mapbox.navigation.ui.internal.route.RouteConstants.HEAVY_CONGESTION_VALUE
import com.mapbox.navigation.ui.internal.route.RouteConstants.LOW_CONGESTION_VALUE
import com.mapbox.navigation.ui.internal.route.RouteConstants.MAX_ELAPSED_SINCE_INDEX_UPDATE_NANO
import com.mapbox.navigation.ui.internal.route.RouteConstants.MODERATE_CONGESTION_VALUE
import com.mapbox.navigation.ui.internal.route.RouteConstants.PRIMARY_ROUTE_CASING_LAYER_ID
import com.mapbox.navigation.ui.internal.route.RouteConstants.PRIMARY_ROUTE_LAYER_ID
import com.mapbox.navigation.ui.internal.route.RouteConstants.PRIMARY_ROUTE_SOURCE_ID
import com.mapbox.navigation.ui.internal.route.RouteConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID
import com.mapbox.navigation.ui.internal.route.RouteConstants.ROUTE_LINE_UPDATE_MAX_DISTANCE_THRESHOLD_IN_METERS
import com.mapbox.navigation.ui.internal.route.RouteConstants.SEVERE_CONGESTION_VALUE
import com.mapbox.navigation.ui.internal.route.RouteConstants.UNKNOWN_CONGESTION_VALUE
import com.mapbox.navigation.ui.internal.route.RouteConstants.WAYPOINT_DESTINATION_VALUE
import com.mapbox.navigation.ui.internal.route.RouteConstants.WAYPOINT_ORIGIN_VALUE
import com.mapbox.navigation.ui.internal.route.RouteConstants.WAYPOINT_PROPERTY_KEY
import com.mapbox.navigation.ui.internal.route.RouteConstants.WAYPOINT_SOURCE_ID
import com.mapbox.navigation.ui.internal.route.RouteLayerProvider
import com.mapbox.navigation.ui.internal.utils.MapUtils
import com.mapbox.navigation.ui.route.MapRouteLine.MapRouteLineSupport.buildWayPointFeatureCollection
import com.mapbox.navigation.ui.route.MapRouteLine.MapRouteLineSupport.calculateDistance
import com.mapbox.navigation.ui.route.MapRouteLine.MapRouteLineSupport.calculateGranularDistances
import com.mapbox.navigation.ui.route.MapRouteLine.MapRouteLineSupport.calculateRouteLineSegments
import com.mapbox.navigation.ui.route.MapRouteLine.MapRouteLineSupport.generateFeatureCollection
import com.mapbox.navigation.ui.route.MapRouteLine.MapRouteLineSupport.getBelowLayer
import com.mapbox.navigation.ui.route.MapRouteLine.MapRouteLineSupport.getBooleanStyledValue
import com.mapbox.navigation.ui.route.MapRouteLine.MapRouteLineSupport.getResourceStyledValue
import com.mapbox.navigation.ui.route.MapRouteLine.MapRouteLineSupport.getStyledColor
import com.mapbox.navigation.ui.route.MapRouteLine.MapRouteLineSupport.getStyledStringArray
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.navigation.utils.internal.ifNonNull
import com.mapbox.navigation.utils.internal.parallelMap
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfException
import com.mapbox.turf.TurfMisc
import timber.log.Timber
import java.util.UUID
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Responsible for the appearance of the route lines on the map. This class applies styling
 * to the layers that contain the route lines so that for example the primary route appears
 * different than the alternative routes that may be on the map.
 *
 * @param context a context to provide resource values
 * @param style the style reference from the Mapbox map
 * @param styleRes a style resource
 * @param belowLayerId determines the elevation of the route layers
 * @param layerProvider provides the layer configurations for the route layers
 * @param routeFeatureDatas used to restore this class with supplied values
 * @param allRoutesVisible true if all the route layers should be visible
 * @param alternativesVisible true if the alternative route layer is visible
 * @param mapRouteSourceProvider wrapper for creating GeoJsonSource objects
 * @param vanishPoint the percentage of the route line from the origin that should not be visible
 * @param routeLineInitializedCallback called to indicate that the route line layer has been added to the current style
 */
internal class MapRouteLine(
    context: Context,
    private val style: Style,
    @androidx.annotation.StyleRes styleRes: Int,
    belowLayerId: String?,
    layerProvider: RouteLayerProvider,
    routeFeatureDatas: List<RouteFeatureData>,
    routeExpressionData: List<RouteLineExpressionData>,
    allRoutesVisible: Boolean,
    alternativesVisible: Boolean,
    mapRouteSourceProvider: MapRouteSourceProvider,
    vanishPoint: Double,
    routeLineInitializedCallback: MapRouteLineInitializedCallback?
) {

    /**
     * @param context a context to provide resource values
     * @param style the style reference from the Mapbox map
     * @param styleRes a style resource
     * @param belowLayerId determines the elevation of the route layers
     * @param layerProvider provides the layer configurations for the route layers
     * @param routeLineInitializedCallback called to indicate that the route line layer has been added to the current style
     */
    constructor(
        context: Context,
        style: Style,
        @androidx.annotation.StyleRes styleRes: Int,
        belowLayerId: String?,
        layerProvider: RouteLayerProvider,
        mapRouteSourceProvider: MapRouteSourceProvider,
        routeLineInitializedCallback: MapRouteLineInitializedCallback?
    ) : this(
        context,
        style,
        styleRes,
        belowLayerId,
        layerProvider,
        listOf(),
        listOf(),
        true,
        true,
        mapRouteSourceProvider,
        0.0,
        routeLineInitializedCallback
    )

    private var drawnWaypointsFeatureCollection: FeatureCollection =
        FeatureCollection.fromFeatures(arrayOf())
    private var drawnPrimaryRouteFeatureCollection: FeatureCollection =
        FeatureCollection.fromFeatures(arrayOf())
    private var drawnAlternativeRouteFeatureCollection: FeatureCollection =
        FeatureCollection.fromFeatures(arrayOf())
    private val routeLineExpressionData = mutableListOf<RouteLineExpressionData>()

    private var wayPointSource: GeoJsonSource
    private val primaryRouteLineSource: GeoJsonSource
    private val alternativeRouteLineSource: GeoJsonSource
    private val routeLayerIds = mutableSetOf<String>()
    private val directionsRoutes = mutableListOf<DirectionsRoute>()
    private val routeFeatureData = mutableListOf<RouteFeatureData>()
    private var alternativesVisible = true
    private var allLayersAreVisible = true
    private var primaryRoute: DirectionsRoute? = null
    var vanishPointOffset: Double = 0.0
        private set
    private var vanishingPointState = VanishingPointState.DISABLED

    private var primaryRoutePoints: RoutePoints? = null
    private var primaryRouteLineGranularDistances: RouteLineGranularDistances? = null
    private var primaryRouteRemainingDistancesIndex: Int? = null
    private var lastIndexUpdateTimeNano: Long = 0

    @get:ColorInt
    private val routeLineTraveledColor: Int by lazy {
        getStyledColor(
            R.styleable.MapboxStyleNavigationMapRoute_routeLineTraveledColor,
            R.color.mapbox_navigation_route_line_traveled_color,
            context,
            styleRes
        )
    }

    private val routeLineCasingTraveledColor: Int by lazy {
        getStyledColor(
            R.styleable.MapboxStyleNavigationMapRoute_routeLineCasingTraveledColor,
            R.color.mapbox_navigation_route_casing_line_traveled_color,
            context,
            styleRes
        )
    }

    @get:ColorInt
    private val routeUnknownColor: Int by lazy {
        getStyledColor(
            R.styleable.MapboxStyleNavigationMapRoute_routeUnknownCongestionColor,
            R.color.mapbox_navigation_route_layer_congestion_unknown,
            context,
            styleRes
        )
    }

    @get:ColorInt
    private val routeDefaultColor: Int by lazy {
        getStyledColor(
            R.styleable.MapboxStyleNavigationMapRoute_routeColor,
            R.color.mapbox_navigation_route_layer_blue,
            context,
            styleRes
        )
    }

    @get:ColorInt
    private val routeLowCongestionColor: Int by lazy {
        getStyledColor(
            R.styleable.MapboxStyleNavigationMapRoute_routeLowCongestionColor,
            R.color.mapbox_navigation_route_traffic_layer_color,
            context,
            styleRes
        )
    }

    @get:ColorInt
    private val routeModerateColor: Int by lazy {
        getStyledColor(
            R.styleable.MapboxStyleNavigationMapRoute_routeModerateCongestionColor,
            R.color.mapbox_navigation_route_layer_congestion_yellow,
            context,
            styleRes
        )
    }

    @get:ColorInt
    private val routeHeavyColor: Int by lazy {
        getStyledColor(
            R.styleable.MapboxStyleNavigationMapRoute_routeHeavyCongestionColor,
            R.color.mapbox_navigation_route_layer_congestion_heavy,
            context,
            styleRes
        )
    }

    @get:ColorInt
    private val routeSevereColor: Int by lazy {
        getStyledColor(
            R.styleable.MapboxStyleNavigationMapRoute_routeSevereCongestionColor,
            R.color.mapbox_navigation_route_layer_congestion_red,
            context,
            styleRes
        )
    }

    @get:ColorInt
    private val routeCasingColor: Int by lazy {
        getStyledColor(
            R.styleable.MapboxStyleNavigationMapRoute_routeCasingColor,
            R.color.mapbox_navigation_route_casing_layer_color,
            context,
            styleRes
        )
    }

    private val roundedLineCap: Boolean by lazy {
        getBooleanStyledValue(
            R.styleable.MapboxStyleNavigationMapRoute_roundedLineCap,
            true,
            context,
            styleRes
        )
    }

    @get:ColorInt
    private val alternativeRouteUnknownColor: Int by lazy {
        getStyledColor(
            R.styleable.MapboxStyleNavigationMapRoute_alternativeRouteUnknownCongestionColor,
            R.color.mapbox_navigation_route_alternative_congestion_unknown,
            context,
            styleRes
        )
    }

    @get:ColorInt
    private val alternativeRouteDefaultColor: Int by lazy {
        getStyledColor(
            R.styleable.MapboxStyleNavigationMapRoute_alternativeRouteColor,
            R.color.mapbox_navigation_route_alternative_color,
            context,
            styleRes
        )
    }

    @get:ColorInt
    private val alternativeRouteLowColor: Int by lazy {
        getStyledColor(
            R.styleable.MapboxStyleNavigationMapRoute_alternativeRouteLowCongestionColor,
            R.color.mapbox_navigation_route_alternative_color,
            context,
            styleRes
        )
    }

    @get:ColorInt
    private val alternativeRouteModerateColor: Int by lazy {
        getStyledColor(
            R.styleable.MapboxStyleNavigationMapRoute_alternativeRouteSevereCongestionColor,
            R.color.mapbox_navigation_route_alternative_congestion_red,
            context,
            styleRes
        )
    }

    @get:ColorInt
    private val alternativeRouteHeavyColor: Int by lazy {
        getStyledColor(
            R.styleable.MapboxStyleNavigationMapRoute_alternativeRouteHeavyCongestionColor,
            R.color.mapbox_navigation_route_alternative_congestion_heavy,
            context,
            styleRes
        )
    }

    @get:ColorInt
    private val alternativeRouteSevereColor: Int by lazy {
        getStyledColor(
            R.styleable.MapboxStyleNavigationMapRoute_alternativeRouteSevereCongestionColor,
            R.color.mapbox_navigation_route_alternative_congestion_red,
            context,
            styleRes
        )
    }

    @get:ColorInt
    private val alternativeRouteCasingColor: Int by lazy {
        getStyledColor(
            R.styleable.MapboxStyleNavigationMapRoute_alternativeRouteCasingColor,
            R.color.mapbox_navigation_route_alternative_casing_color,
            context,
            styleRes
        )
    }

    private val trafficBackfillRoadClasses: List<String> by lazy {
        getStyledStringArray(
            R.styleable.MapboxStyleNavigationMapRoute_trafficBackFillRoadClasses,
            context,
            styleRes,
            R.styleable.MapboxStyleNavigationMapRoute
        )
    }

    /**
     * Initializes the instance with appropriate default values.
     */
    init {
        this.alternativesVisible = alternativesVisible
        this.allLayersAreVisible = allRoutesVisible
        this.routeFeatureData.addAll(routeFeatureDatas)
        this.routeLineExpressionData.addAll(routeExpressionData)
        this.vanishPointOffset = vanishPoint

        if (routeFeatureData.isNotEmpty()) {
            this.primaryRoute = routeFeatureDatas.first().route
            this.drawnPrimaryRouteFeatureCollection = routeFeatureData.first().featureCollection
            this.drawnAlternativeRouteFeatureCollection = routeFeatureData
                .filter { it.route != primaryRoute }
                .mapNotNull { it.featureCollection.features() }
                .flatten()
                .run { FeatureCollection.fromFeatures(this) }

            this.drawnWaypointsFeatureCollection =
                buildWayPointFeatureCollection(routeFeatureData.first().route)

            initPrimaryRoutePoints(routeFeatureData.first().route)
        }

        val wayPointGeoJsonOptions = GeoJsonOptions().withMaxZoom(16)
        wayPointSource = mapRouteSourceProvider.build(
            WAYPOINT_SOURCE_ID,
            drawnWaypointsFeatureCollection,
            wayPointGeoJsonOptions
        )
        style.addSource(wayPointSource)

        val routeLineGeoJsonOptions = GeoJsonOptions().withMaxZoom(16).withLineMetrics(true)
        primaryRouteLineSource = mapRouteSourceProvider.build(
            PRIMARY_ROUTE_SOURCE_ID,
            drawnPrimaryRouteFeatureCollection,
            routeLineGeoJsonOptions
        )
        style.addSource(primaryRouteLineSource)

        val alternativeRouteLineGeoJsonOptions =
            GeoJsonOptions().withMaxZoom(16).withLineMetrics(true)
        alternativeRouteLineSource = mapRouteSourceProvider.build(
            ALTERNATIVE_ROUTE_SOURCE_ID,
            drawnAlternativeRouteFeatureCollection,
            alternativeRouteLineGeoJsonOptions
        )
        style.addSource(alternativeRouteLineSource)

        val originWaypointIcon = getResourceStyledValue(
            R.styleable.MapboxStyleNavigationMapRoute_originWaypointIcon,
            R.drawable.mapbox_ic_route_origin,
            context,
            styleRes
        )

        val destinationWaypointIcon = getResourceStyledValue(
            R.styleable.MapboxStyleNavigationMapRoute_destinationWaypointIcon,
            R.drawable.mapbox_ic_route_destination,
            context,
            styleRes
        )
        val originIcon = AppCompatResources.getDrawable(context, originWaypointIcon)
        val destinationIcon = AppCompatResources.getDrawable(context, destinationWaypointIcon)
        val belowLayer = getBelowLayer(belowLayerId, style)

        initializeLayers(style, layerProvider, originIcon!!, destinationIcon!!, belowLayer)
        updateAlternativeLayersVisibility(alternativesVisible, routeLayerIds)
        updateAllLayersVisibility(allLayersAreVisible)

        if (style.isFullyLoaded && routeFeatureData.isNotEmpty()) {
            val expression = getExpressionAtOffset(vanishPointOffset)
            style.getLayer(PRIMARY_ROUTE_TRAFFIC_LAYER_ID)?.setProperties(lineGradient(expression))
            hideRouteLineAtOffset(vanishPointOffset)
            hideCasingLineAtOffset(vanishPointOffset)
        }

        routeLineInitializedCallback?.onInitialized(
            RouteLineLayerIds(
                PRIMARY_ROUTE_TRAFFIC_LAYER_ID,
                PRIMARY_ROUTE_LAYER_ID,
                listOf(ALTERNATIVE_ROUTE_LAYER_ID)
            )
        )
    }

    private fun initPrimaryRoutePoints(route: DirectionsRoute) {
        primaryRoutePoints = parseRoutePoints(route)
        primaryRouteLineGranularDistances =
            calculateRouteGranularDistances(primaryRoutePoints?.flatList ?: emptyList())
    }

    /**
     * Decodes the route geometry into nested arrays of legs -> steps -> points.
     *
     * The first and last point of adjacent steps overlap and are duplicated.
     */
    private fun parseRoutePoints(route: DirectionsRoute): RoutePoints? {
        val precision =
            if (route.routeOptions()?.geometries() == DirectionsCriteria.GEOMETRY_POLYLINE) {
                Constants.PRECISION_5
            } else {
                Constants.PRECISION_6
            }

        val nestedList = route.legs()?.map { routeLeg ->
            routeLeg.steps()?.map { legStep ->
                legStep.geometry()?.let { geometry ->
                    PolylineUtils.decode(geometry, precision).toList()
                } ?: return null
            } ?: return null
        } ?: return null

        val flatList = nestedList.flatten().flatten()

        return RoutePoints(nestedList, flatList)
    }

    /**
     * Tries to find and cache the index of the upcoming [RouteLineDistancesIndex].
     */
    fun updateUpcomingRoutePointIndex(routeProgress: RouteProgress) {
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
    }

    fun updateVanishingPointState(routeProgressState: RouteProgressState) {
        vanishingPointState = when (routeProgressState) {
            RouteProgressState.LOCATION_TRACKING -> VanishingPointState.ENABLED
            RouteProgressState.ROUTE_COMPLETE -> VanishingPointState.ONLY_INCREASE_PROGRESS
            else -> VanishingPointState.DISABLED
        }
    }

    /**
     * Creates a route line which is applied to the route layer(s)
     *
     * @param directionsRoute the route object to be represented on the map.
     */
    fun draw(directionsRoute: DirectionsRoute) {
        draw(listOf(directionsRoute))
    }

    /**
     * Creates route lines which is applied to the route layer(s)
     *
     * @param directionsRoutes the routes to be represented on the map.
     */
    fun draw(directionsRoutes: List<DirectionsRoute>) {
        val featureDataProvider: () -> List<RouteFeatureData> =
            getRouteFeatureDataProvider(directionsRoutes)
        reinitializeWithRoutes(directionsRoutes, featureDataProvider)
        drawRoutes(routeFeatureData)
    }

    fun drawIdentifiableRoutes(directionsRoutes: List<IdentifiableRoute>) {
        val routes = directionsRoutes.map { it.route }
        val featureDataProvider: () -> List<RouteFeatureData> =
            getIdentifiableRouteFeatureDataProvider(directionsRoutes)
        reinitializeWithRoutes(routes, featureDataProvider)
        drawRoutes(routeFeatureData)
    }

    fun reinitializeWithRoutes(directionsRoutes: List<DirectionsRoute>) {
        val featureDataProvider: () -> List<RouteFeatureData> =
            getRouteFeatureDataProvider(directionsRoutes)
        reinitializeWithRoutes(directionsRoutes, featureDataProvider)
    }

    fun reinitializePrimaryRoute() {
        this@MapRouteLine.routeFeatureData.firstOrNull { it.route == primaryRoute }?.let {
            updateRouteTrafficSegments(it)
            drawPrimaryRoute(it)
            hideRouteLineAtOffset(vanishPointOffset)
            hideCasingLineAtOffset(vanishPointOffset)
        }
    }

    private fun reinitializeWithRoutes(
        directionsRoutes: List<DirectionsRoute>,
        getRouteFeatureData: () -> List<RouteFeatureData>
    ) {
        if (directionsRoutes.isNotEmpty()) {
            clearRouteData()
            this.directionsRoutes.addAll(directionsRoutes)
            primaryRoute = this.directionsRoutes.first()
            alternativesVisible = directionsRoutes.size > 1
            allLayersAreVisible = true

            val newRouteFeatureData = getRouteFeatureData().also {
                routeFeatureData.addAll(it)
            }

            if (newRouteFeatureData.isNotEmpty()) {
                updateRouteTrafficSegments(newRouteFeatureData.first())
            }
            drawWayPoints()
            updateAlternativeLayersVisibility(alternativesVisible, routeLayerIds)
            updateAllLayersVisibility(allLayersAreVisible)
        }
    }

    /**
     * Updates which route is identified as the primary route.
     *
     * @param route the DirectionsRoute which should be designated as the primary
     */
    fun updatePrimaryRouteIndex(route: DirectionsRoute) {
        this@MapRouteLine.primaryRoute = route
        val partitionedRoutes = routeFeatureData.partition { it.route == primaryRoute }
        routeFeatureData.apply {
            clear()
            addAll(listOf(partitionedRoutes.first, partitionedRoutes.second).flatten())
        }
        updateRouteTrafficSegments(routeFeatureData.first())
        drawRoutes(routeFeatureData)
    }

    /**
     * Returns the top layer ID or the shadow layer if no route layers exist
     */
    fun getTopLayerId(): String {
        return if (routeLayerIds.isEmpty()) {
            LocationComponentConstants.SHADOW_LAYER
        } else {
            routeLayerIds.last()
        }
    }

    /**
     * Turns the alternative routes visible or invisible
     *
     * @param altVisible true if should be visible else false
     */
    fun toggleAlternativeVisibilityWith(altVisible: Boolean) {
        this.alternativesVisible = altVisible
        updateAlternativeLayersVisibility(altVisible, routeLayerIds)
    }

    /**
     * Returns the DirectionsRoutes being used.
     */
    fun retrieveDirectionsRoutes(): List<DirectionsRoute> {
        val itemsToReturn: MutableList<DirectionsRoute> = when (primaryRoute) {
            null -> mutableListOf()
            else -> mutableListOf(primaryRoute!!)
        }
        val filteredItems = routeFeatureData.map { it.route }.filter { it != primaryRoute }
        return itemsToReturn.plus(filteredItems)
    }

    /**
     * Returns true if the alternate routes are visible.
     */
    fun retrieveAlternativesVisible(): Boolean = alternativesVisible

    /**
     * Returns true if all of the route layers are visible.
     */
    fun retrieveVisibility(): Boolean = allLayersAreVisible

    /**
     * Returns the line strings that have been calculated based on the DirectionRoute(s) being used.
     */
    fun retrieveRouteLineStrings(): Map<LineString, DirectionsRoute> {
        return routeFeatureData.map { Pair(it.lineString, it.route) }.toMap()
    }

    /**
     * Returns the RouteFeatureData objects being used.
     */
    fun retrieveRouteFeatureData(): List<RouteFeatureData> {
        return routeFeatureData.toList()
    }

    fun retrieveRouteExpressionData(): List<RouteLineExpressionData> {
        return routeLineExpressionData
    }

    /**
     * Updates the visiblity of the route layers.
     *
     * @param isVisible true indicates all of the route layers will become visible.
     */
    fun updateVisibilityTo(isVisible: Boolean) {
        allLayersAreVisible = isVisible
        updateAllLayersVisibility(isVisible)
    }

    /**
     * @return the primary DirectionsRoute if one exists.
     */
    fun getPrimaryRoute(): DirectionsRoute? {
        return primaryRoute
    }

    /**
     * Returns the LineString associated with the @param route if it exists.
     *
     * @param route the route for which a LineString is sought.
     *
     * @return the associated LineString if it exists.
     */
    fun getLineStringForRoute(route: DirectionsRoute): LineString {
        return routeFeatureData.firstOrNull {
            it.route == route
        }?.lineString ?: LineString.fromPolyline(route.geometry()!!, Constants.PRECISION_6)
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
        target: LatLng,
        mapboxMap: MapboxMap,
        padding: Float
    ): Int {
        val mapClickPointF = mapboxMap.projection.toScreenLocation(target)
        val leftFloat = (mapClickPointF.x - padding)
        val rightFloat = (mapClickPointF.x + padding)
        val topFloat = (mapClickPointF.y - padding)
        val bottomFloat = (mapClickPointF.y + padding)
        val clickRectF = RectF(leftFloat, topFloat, rightFloat, bottomFloat)

        val featureIndex = queryMapForFeatureIndex(
            mapboxMap,
            mapClickPointF,
            clickRectF,
            listOf(PRIMARY_ROUTE_LAYER_ID, PRIMARY_ROUTE_CASING_LAYER_ID)
        )

        if (featureIndex >= 0) {
            return featureIndex
        }

        return queryMapForFeatureIndex(
            mapboxMap,
            mapClickPointF,
            clickRectF,
            listOf(ALTERNATIVE_ROUTE_LAYER_ID, ALTERNATIVE_ROUTE_CASING_LAYER_ID)
        )
    }

    private fun queryMapForFeatureIndex(
        mapboxMap: MapboxMap,
        mapClickPointF: PointF,
        clickRectF: RectF,
        layerIds: List<String>
    ): Int {
        val featureIndex = mapboxMap.queryRenderedFeatures(
            mapClickPointF,
            *layerIds.toTypedArray()
        ).run { getIndexOfFirstFeature(this) }

        return when (featureIndex >= 0) {
            true -> featureIndex
            false -> mapboxMap.queryRenderedFeatures(
                clickRectF,
                *layerIds.toTypedArray()
            ).run { getIndexOfFirstFeature(this) }
        }
    }

    private fun getIndexOfFirstFeature(features: List<Feature>): Int {
        return features.distinct().run {
            routeFeatureData.indexOfFirst {
                it.featureCollection.features()?.get(0) ?.id() ?: 0 == this.firstOrNull()?.id()
            }
        }
    }

    private fun getIdentifiableRouteFeatureDataProvider(directionsRoutes: List<IdentifiableRoute>):
        () -> List<RouteFeatureData> = {
            directionsRoutes.parallelMap(
                ::generateFeatureCollection,
                ThreadController.getMainScopeAndRootJob().scope
            )
        }

    private fun getRouteFeatureDataProvider(directionsRoutes: List<DirectionsRoute>):
        () -> List<RouteFeatureData> = {
            directionsRoutes.parallelMap(
                ::generateFeatureCollection,
                ThreadController.getMainScopeAndRootJob().scope
            )
        }

    /**
     * Initializes the layers used for drawing routes.
     *
     * @param style the style from the Mapbox map
     * @param layerProvider provides the layer configurations for the routes
     * @param originIcon the drawable that represents the route origin
     * @param destinationIcon the drawable that represents the route destination
     * @param belowLayerId the layer ID that indicates what layer elevation the routes should be
     * drawn on
     */
    private fun initializeLayers(
        style: Style,
        layerProvider: RouteLayerProvider,
        originIcon: Drawable,
        destinationIcon: Drawable,
        belowLayerId: String
    ) {

        layerProvider.initializeAlternativeRouteCasingLayer(
            style,
            alternativeRouteCasingColor
        ).apply {
            MapUtils.addLayerToMap(
                style,
                this,
                belowLayerId
            )
            routeLayerIds.add(this.id)
        }

        layerProvider.initializeAlternativeRouteLayer(
            style,
            roundedLineCap,
            alternativeRouteDefaultColor
        ).apply {
            MapUtils.addLayerToMap(
                style,
                this,
                belowLayerId
            )
            routeLayerIds.add(this.id)
        }

        layerProvider.initializePrimaryRouteCasingLayer(
            style,
            routeCasingColor
        ).apply {
            MapUtils.addLayerToMap(
                style,
                this,
                belowLayerId
            )
            routeLayerIds.add(this.id)
        }

        layerProvider.initializePrimaryRouteLayer(
            style,
            roundedLineCap,
            routeDefaultColor
        ).apply {
            MapUtils.addLayerToMap(
                style,
                this,
                belowLayerId
            )
            routeLayerIds.add(this.id)
        }

        layerProvider.initializePrimaryRouteTrafficLayer(
            style,
            roundedLineCap,
            routeDefaultColor
        ).apply {
            MapUtils.addLayerToMap(
                style,
                this,
                belowLayerId
            )
            routeLayerIds.add(this.id)
        }

        layerProvider.initializeWayPointLayer(
            style,
            originIcon,
            destinationIcon
        ).apply {
            MapUtils.addLayerToMap(
                style,
                this,
                belowLayerId
            )
            routeLayerIds.add(this.id)
        }
    }

    private fun drawWayPoints() {
        primaryRoute?.let {
            setWaypointsSource(buildWayPointFeatureCollection(it))
        }
    }

    private fun drawRoutes(routeData: List<RouteFeatureData>) {
        val partitionedRoutes = routeData.partition { it.route == primaryRoute }
        partitionedRoutes.first.firstOrNull()?.let {
            drawPrimaryRoute(it)
            hideRouteLineAtOffset(vanishPointOffset)
            hideCasingLineAtOffset(vanishPointOffset)
        }
        drawAlternativeRoutes(partitionedRoutes.second)
    }

    private fun drawPrimaryRoute(routeData: RouteFeatureData) {
        setPrimaryRoutesSource(routeData.featureCollection)
        if (style.isFullyLoaded) {
            val expression = getExpressionAtOffset(vanishPointOffset)
            style.getLayer(PRIMARY_ROUTE_TRAFFIC_LAYER_ID)?.setProperties(lineGradient(expression))
        }
        initPrimaryRoutePoints(routeData.route)
    }

    private fun updateRouteTrafficSegments(routeData: RouteFeatureData) {
        val segments = calculateRouteLineSegments(
            routeData.route,
            trafficBackfillRoadClasses,
            true,
            ::getRouteColorForCongestion
        )
        routeLineExpressionData.clear()
        routeLineExpressionData.addAll(segments)
    }

    private fun drawAlternativeRoutes(routeData: List<RouteFeatureData>) {
        routeData.mapNotNull {
            it.featureCollection.features()
        }.flatten().let {
            setAlternativeRoutesSource(FeatureCollection.fromFeatures(it))
        }
    }

    /**
     * Creates an [Expression] that can be applied to the layer style changing the appearance of
     * a route line, making the portion of the route line behind the puck invisible.
     *
     * @param distanceOffset the percentage of the distance traveled which will represent
     * the part of the route line that isn't visible
     *
     * @return the Expression that can be used in a Layer's properties.
     */
    fun getExpressionAtOffset(distanceOffset: Double): Expression {
        vanishPointOffset = distanceOffset
        val filteredItems = routeLineExpressionData.filter { it.offset > distanceOffset }
        val trafficExpressions = when (filteredItems.isEmpty()) {
            true -> when (routeLineExpressionData.isEmpty()) {
                true -> listOf(
                    RouteLineExpressionData(
                        distanceOffset,
                        Expression.color(routeUnknownColor)
                    )
                )
                false -> listOf(routeLineExpressionData.last().copy(offset = distanceOffset))
            }
            false -> {
                val firstItemIndex = routeLineExpressionData.indexOf(filteredItems.first())
                val fillerItem = if (firstItemIndex == 0) {
                    routeLineExpressionData[firstItemIndex]
                } else {
                    routeLineExpressionData[firstItemIndex - 1]
                }
                listOf(fillerItem.copy(offset = distanceOffset)).plus(filteredItems)
            }
        }.map {
            Expression.stop(
                it.offset,
                it.segmentColorExpression
            )
        }

        return Expression.step(
            Expression.lineProgress(),
            Expression.rgba(0, 0, 0, 0),
            *trafficExpressions.toTypedArray()
        )
    }

    fun clearRouteData() {
        vanishPointOffset = 0.0
        primaryRoutePoints = null
        primaryRouteLineGranularDistances = null
        primaryRouteRemainingDistancesIndex = null
        vanishingPointState = VanishingPointState.DISABLED
        primaryRoute = null
        directionsRoutes.clear()
        routeFeatureData.clear()
        routeLineExpressionData.clear()
        setPrimaryRoutesSource(FeatureCollection.fromFeatures(arrayOf()))
        setAlternativeRoutesSource(FeatureCollection.fromFeatures(arrayOf()))
        setWaypointsSource(FeatureCollection.fromFeatures(arrayOf()))
    }

    private fun setPrimaryRoutesSource(featureCollection: FeatureCollection) {
        drawnPrimaryRouteFeatureCollection = featureCollection
        primaryRouteLineSource.setGeoJson(drawnPrimaryRouteFeatureCollection)
    }

    private fun calculateRouteGranularDistances(coordinates: List<Point>):
        RouteLineGranularDistances? {
            return if (coordinates.isNotEmpty()) {
                calculateGranularDistances(coordinates)
            } else {
                null
            }
        }

    private fun setAlternativeRoutesSource(featureCollection: FeatureCollection) {
        drawnAlternativeRouteFeatureCollection = featureCollection
        alternativeRouteLineSource.setGeoJson(drawnAlternativeRouteFeatureCollection)
    }

    private fun setWaypointsSource(featureCollection: FeatureCollection) {
        drawnWaypointsFeatureCollection = featureCollection
        wayPointSource.setGeoJson(drawnWaypointsFeatureCollection)
    }

    private fun updateAlternativeLayersVisibility(
        isAlternativeVisible: Boolean,
        routeLayerIds: Set<String>
    ) {
        if (style.isFullyLoaded) {
            routeLayerIds.filter {
                it == RouteConstants.ALTERNATIVE_ROUTE_LAYER_ID ||
                    it == RouteConstants.ALTERNATIVE_ROUTE_CASING_LAYER_ID
            }.mapNotNull { style.getLayer(it) }.forEach {
                (it as LineLayer).setFilter(Expression.literal(isAlternativeVisible))
            }
        }
    }

    private fun updateAllLayersVisibility(areVisible: Boolean) {
        val visPropertyValue = if (areVisible) Property.VISIBLE else Property.NONE

        if (style.isFullyLoaded) {
            routeLayerIds.mapNotNull { style.getLayer(it) }.map {
                it.setProperties(PropertyFactory.visibility(visPropertyValue))
            }
        }
    }

    /**
     * Returns the color that is used to represent traffic congestion.
     *
     * @param congestionValue as string value coming from the DirectionsRoute
     * @param isPrimaryRoute indicates if the congestion value for the primary route should
     * be returned or the color for an alternative route.
     */
    @ColorInt
    fun getRouteColorForCongestion(congestionValue: String, isPrimaryRoute: Boolean): Int {
        return when (isPrimaryRoute) {
            true -> when (congestionValue) {
                MODERATE_CONGESTION_VALUE -> routeModerateColor
                HEAVY_CONGESTION_VALUE -> routeHeavyColor
                SEVERE_CONGESTION_VALUE -> routeSevereColor
                UNKNOWN_CONGESTION_VALUE -> routeUnknownColor
                else -> routeLowCongestionColor
            }
            false -> when (congestionValue) {
                MODERATE_CONGESTION_VALUE -> alternativeRouteModerateColor
                HEAVY_CONGESTION_VALUE -> alternativeRouteHeavyColor
                SEVERE_CONGESTION_VALUE -> alternativeRouteSevereColor
                UNKNOWN_CONGESTION_VALUE -> alternativeRouteUnknownColor
                else -> alternativeRouteDefaultColor
            }
        }
    }

    /**
     * Hides the RouteCasing Layer
     *
     * @param offset the offset of the visibility in the expression
     */
    fun hideCasingLineAtOffset(offset: Double) {
        val expression = Expression.step(
            Expression.lineProgress(),
            Expression.color(routeLineCasingTraveledColor),
            Expression.stop(
                offset,
                Expression.color(routeCasingColor)
            )
        )
        if (style.isFullyLoaded) {
            style.getLayerAs<LineLayer>(RouteConstants.PRIMARY_ROUTE_CASING_LAYER_ID)
                ?.setProperties(lineGradient(expression))
        }
    }

    /**
     * Hides the Route Layer
     *
     * @param offset the offset of the visibility in the expression
     */
    fun hideRouteLineAtOffset(offset: Double) {
        val expression = Expression.step(
            Expression.lineProgress(),
            Expression.color(routeLineTraveledColor),
            Expression.stop(
                offset,
                Expression.color(routeDefaultColor)
            )
        )
        if (style.isFullyLoaded) {
            style.getLayer(PRIMARY_ROUTE_LAYER_ID)?.setProperties(
                lineGradient(
                    expression
                )
            )
        }
    }

    /**
     * Applies an Expression to the route line traffic layer.
     *
     * @param expression the Expression to apply to the layer properties
     */
    fun decorateRouteLine(expression: Expression) {
        if (style.isFullyLoaded) {
            style.getLayer(PRIMARY_ROUTE_TRAFFIC_LAYER_ID)?.setProperties(lineGradient(expression))
        }
    }

    /**
     * Updates the route line appearance from the origin point to the indicated point
     * @param point representing the current position of the puck
     */
    fun updateTraveledRouteLine(point: Point) {
        if (vanishingPointState == VanishingPointState.DISABLED ||
            System.nanoTime() - lastIndexUpdateTimeNano > MAX_ELAPSED_SINCE_INDEX_UPDATE_NANO
        ) {
            return
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
                return
            }
            val upcomingPoint = upcomingIndex.point

            if (index > 0) {
                val distanceToLine = findDistanceToNearestPointOnCurrentLine(
                    point,
                    granularDistances,
                    index
                )
                if (distanceToLine > ROUTE_LINE_UPDATE_MAX_DISTANCE_THRESHOLD_IN_METERS) {
                    return
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
                return
            }
            val expression = getExpressionAtOffset(offset)
            hideCasingLineAtOffset(offset)
            hideRouteLineAtOffset(offset)
            decorateRouteLine(expression)
        }
    }

    /**
     * Creates a line from the upcoming geometry point and the previous 10 points
     * and tries to find the the distance from current point to that line.
     *
     * We need to take more points than <previous - upcoming> because the route progress update
     * can jump by more than 1 geometry point.
     * If this happens, the puck will animate through multiple geometry points,
     * so we need to make a line with a buffer.
     */
    private fun findDistanceToNearestPointOnCurrentLine(
        point: Point,
        granularDistances: RouteLineGranularDistances,
        upcomingIndex: Int
    ): Double {
        return TurfMisc.nearestPointOnLine(
            point,
            granularDistances.distancesArray.run {
                val points = mutableListOf<Point>()
                for (i in max(upcomingIndex - 10, 0)..upcomingIndex) {
                    points.add(this.get(i).point)
                }
                points
            },
            TurfConstants.UNIT_METERS
        ).getNumberProperty("dist")?.toDouble() ?: 0.0
    }

    internal object MapRouteLineSupport {

        fun getRouteLineScalingValues(
            styleRes: Int,
            context: Context,
            stopsResourceId: Int,
            scaleMultiplierResourceId: Int,
            scalesResourceId: Int,
            attributes: IntArray
        ): List<RouteLineScaleValue> {

            val stopsArray = getStyledFloatArray(
                stopsResourceId,
                context,
                styleRes,
                attributes
            )

            val multiplierArray = getStyledFloatArray(
                scaleMultiplierResourceId,
                context,
                styleRes,
                attributes
            )

            val scalesArray = getStyledFloatArray(
                scalesResourceId,
                context,
                styleRes,
                attributes
            )

            return consolidateScalingArrays(stopsArray, multiplierArray, scalesArray)
        }

        private fun consolidateScalingArrays(
            routeScaleStopsArray: List<Float>,
            routeScaleMultiplierArray: List<Float>,
            routeLineScalesArray: List<Float>
        ): List<RouteLineScaleValue> {
            val minCount = minOf(
                routeScaleStopsArray.size,
                routeScaleMultiplierArray.size,
                routeLineScalesArray.size
            )

            val itemsToReturn = mutableListOf<RouteLineScaleValue>()
            for (index in 0 until minCount) {
                itemsToReturn.add(
                    RouteLineScaleValue(
                        routeScaleStopsArray[index],
                        routeScaleMultiplierArray[index],
                        routeLineScalesArray[index]
                    )
                )
            }
            return itemsToReturn
        }

        fun getStyledFloatArray(
            arrayResourceId: Int,
            context: Context,
            styleRes: Int,
            attributes: IntArray
        ): List<Float> {
            return getStyledStringArray(arrayResourceId, context, styleRes, attributes).mapNotNull {
                it.toFloatOrNull()
            }
        }

        fun getStyledStringArray(
            arrayResourceId: Int,
            context: Context,
            styleRes: Int,
            attributes: IntArray
        ): List<String> {
            return try {
                val typedArray = context.obtainStyledAttributes(styleRes, attributes)
                val resourceId = typedArray.getResourceId(arrayResourceId, 0).also {
                    typedArray.recycle()
                }
                context.resources.getStringArray(resourceId).toList()
            } catch (ex: Exception) {
                Timber.e(ex)
                listOf()
            }
        }

        /**
         * Returns a resource value from the style or a default value
         * @param index the index of the item in the styled attributes.
         * @param colorResourceId the default value to use if no value is found
         * @param context the context to obtain the resource from
         * @param styleRes the style resource to look in
         *
         * @return the resource value
         */
        @ColorInt
        fun getStyledColor(index: Int, colorResourceId: Int, context: Context, styleRes: Int): Int {
            val typedArray =
                context.obtainStyledAttributes(styleRes, R.styleable.MapboxStyleNavigationMapRoute)
            return typedArray.getColor(
                index,
                ContextCompat.getColor(
                    context,
                    colorResourceId
                )
            ).also {
                typedArray.recycle()
            }
        }

        /**
         * Returns a resource value from the style or a default value
         * @param index the index of the item in the styled attributes.
         * @param defaultValue the default value to use if no value is found
         * @param context the context to obtain the resource from
         * @param styleRes the style resource to look in
         *
         * @return the resource value
         */
        fun getFloatStyledValue(
            index: Int,
            defaultValue: Float,
            context: Context,
            styleRes: Int
        ): Float {
            val typedArray =
                context.obtainStyledAttributes(styleRes, R.styleable.MapboxStyleNavigationMapRoute)
            return typedArray.getFloat(index, defaultValue).also {
                typedArray.recycle()
            }
        }

        /**
         * Returns a resource value from the style or a default value
         * @param index the index of the item in the styled attributes.
         * @param defaultValue the default value to use if no value is found
         * @param context the context to obtain the resource from
         * @param styleRes the style resource to look in
         *
         * @return the resource value
         */
        fun getBooleanStyledValue(
            index: Int,
            defaultValue: Boolean,
            context: Context,
            styleRes: Int
        ): Boolean {
            val typedArray =
                context.obtainStyledAttributes(styleRes, R.styleable.MapboxStyleNavigationMapRoute)
            return typedArray.getBoolean(index, defaultValue).also {
                typedArray.recycle()
            }
        }

        /**
         * Returns a resource value from the style or a default value
         * @param index the index of the item in the styled attributes.
         * @param defaultValue the default value to use if no value is found
         * @param context the context to obtain the resource from
         * @param styleRes the style resource to look in
         *
         * @return the resource value
         */
        @AnyRes
        fun getResourceStyledValue(
            index: Int,
            defaultValue: Int,
            context: Context,
            styleRes: Int
        ): Int {
            val typedArray =
                context.obtainStyledAttributes(styleRes, R.styleable.MapboxStyleNavigationMapRoute)
            return typedArray.getResourceId(
                index,
                defaultValue
            ).also {
                typedArray.recycle()
            }
        }

        /**
         * Checks if a layer with the given ID exists else returns a default layer ID
         * @param layerId the layer ID to look for
         * @param style the style containing the layers
         *
         * @return either the layer ID if found else a default layer ID
         */
        fun getBelowLayer(layerId: String?, style: Style): String {
            val layers = style.layers
            return when (layerId.isNullOrEmpty()) {
                false -> checkLayerIdPresent(layerId, layers)
                true -> findLayerBelow(layers)
            }
        }

        private fun checkLayerIdPresent(layerId: String, layers: List<Layer>): String {
            val foundId = layers.firstOrNull { it.id == layerId }?.id
            if (foundId == null) {
                Timber.e(
                    """Tried placing route line below "$layerId" which doesn't exist"""
                )
            }
            return foundId ?: LocationComponentConstants.SHADOW_LAYER
        }

        /**
         * Tries to find a reference layer ID that's above a first non-symbol layer from the top
         * of the stack of layers. Additionally, the algorithm always ensures that the reference
         * layer is below the puck layers.
         */
        private fun findLayerBelow(layers: List<Layer>): String {
            val puckLayerIndex = layers.indexOfFirst {
                it.id.contains(RouteConstants.MAPBOX_LOCATION_ID)
            }
            val lastSymbolLayerFromTopIndex = layers.indexOfLast {
                it !is SymbolLayer && !it.id.contains(RouteConstants.MAPBOX_LOCATION_ID)
            } + 1
            val index = if (puckLayerIndex in 0 until lastSymbolLayerFromTopIndex) {
                puckLayerIndex
            } else {
                lastSymbolLayerFromTopIndex
            }
            return layers.getOrNull(index)?.id ?: LocationComponentConstants.SHADOW_LAYER
        }

        /**
         * Generates a FeatureCollection and LineString based on the @param route.
         * @param route the DirectionsRoute to used to derive the result
         *
         * @return a RouteFeatureData containing the original route and a FeatureCollection and
         * LineString
         */
        fun generateFeatureCollection(route: DirectionsRoute): RouteFeatureData =
            generateFeatureCollection(route, null)

        /**
         * Generates a FeatureCollection and LineString based on the @param route.
         * @param route the DirectionsRoute to used to derive the result
         *
         * @return a RouteFeatureData containing the original route and a FeatureCollection and
         * LineString
         */
        fun generateFeatureCollection(routeData: IdentifiableRoute): RouteFeatureData =
            generateFeatureCollection(routeData.route, routeData.routeIdentifier)

        private fun generateFeatureCollection(route: DirectionsRoute, identifier: String?):
            RouteFeatureData {
                val routeGeometry = LineString.fromPolyline(
                    route.geometry() ?: "",
                    Constants.PRECISION_6
                )
                val randomId = UUID.randomUUID().toString()
                val routeFeature = when (identifier) {
                    null -> {
                        Feature.fromGeometry(
                            routeGeometry,
                            null,
                            randomId
                        )
                    }
                    else -> {
                        Feature.fromGeometry(
                            routeGeometry,
                            null,
                            randomId
                        ).also {
                            it.addBooleanProperty(identifier, true)
                        }
                    }
                }

                return RouteFeatureData(
                    route,
                    FeatureCollection.fromFeatures(listOf(routeFeature)),
                    routeGeometry
                )
            }

        /**
         * Calculates line segments based on the legs in the route line and color representation
         * of the traffic congestion. The items returned can be used to create a style expression
         * which can be used to style the route line. The styled route line will be colored
         * according to the traffic conditions indicated in the @param route. For any road class
         * included in the @param trafficBackfillRoadClasses, all route segments with an 'unknown'
         * traffic congestion annotation and a matching road class will be colored with the low
         * traffic color instead of the color configured for unknown traffic congestion.
         *
         * @param route the DirectionsRoute used for the [Expression] calculations
         * @param trafficBackfillRoadClasses a collection of road classes defined in the styles.xml
         * @param isPrimaryRoute indicates if the route used is the primary route
         * @param congestionColorProvider a function that provides the colors used for various
         * traffic congestion values
         *
         * @return a list of items representing the distance offset of each route leg and the color
         * used to represent the traffic congestion.
         */
        fun calculateRouteLineSegments(
            route: DirectionsRoute,
            trafficBackfillRoadClasses: List<String>,
            isPrimaryRoute: Boolean,
            congestionColorProvider: (String, Boolean) -> Int
        ): List<RouteLineExpressionData> {
            val trafficExpressionData = getRouteLineTrafficExpressionData(route)
            return when (trafficExpressionData.isEmpty()) {
                false -> getRouteLineExpressionDataWithStreetClassOverride(
                    trafficExpressionData,
                    route.distance(),
                    congestionColorProvider,
                    isPrimaryRoute,
                    trafficBackfillRoadClasses
                )
                true -> listOf(
                    RouteLineExpressionData(
                        0.0,
                        Expression.color(congestionColorProvider("", isPrimaryRoute))
                    )
                )
            }
        }

        /**
         * Builds a FeatureCollection representing waypoints from a DirectionsRoute
         *
         * @param route the route to use for generating the waypoints FeatureCollection
         * @return a FeatureCollection representing the waypoints derived from the DirectionRoute
         */
        fun buildWayPointFeatureCollection(route: DirectionsRoute): FeatureCollection {
            val wayPointFeatures = mutableListOf<Feature>()
            route.legs()?.forEach {
                buildWayPointFeatureFromLeg(it, 0)?.let { feature ->
                    wayPointFeatures.add(feature)
                }

                it.steps()?.let { steps ->
                    buildWayPointFeatureFromLeg(it, steps.lastIndex)?.let { feature ->
                        wayPointFeatures.add(feature)
                    }
                }
            }
            return FeatureCollection.fromFeatures(wayPointFeatures)
        }

        /**
         * Builds a Feature representing a waypoint for use on a Mapbox Map.
         *
         * @param leg the RouteLeg containing the waypoint info.
         * @param index a value of 0 indicates a property value of origin
         * will be added to the Feature else a value of destination will be used.
         *
         * @return a Feature representing the waypoint from the RouteLog
         */
        fun buildWayPointFeatureFromLeg(leg: RouteLeg, index: Int): Feature? {
            return leg.steps()?.get(index)?.maneuver()?.location()?.run {
                Feature.fromGeometry(Point.fromLngLat(this.longitude(), this.latitude()))
            }?.also {
                val propValue =
                    if (index == 0) WAYPOINT_ORIGIN_VALUE else WAYPOINT_DESTINATION_VALUE
                it.addStringProperty(WAYPOINT_PROPERTY_KEY, propValue)
            }
        }

        fun calculateGranularDistances(points: List<Point>): RouteLineGranularDistances {
            var distance = 0.0
            val indexArray = SparseArray<RouteLineDistancesIndex>(points.size)
            for (i in (points.size - 1) downTo 1) {
                val curr = points[i]
                val prev = points[i - 1]
                distance += calculateDistance(curr, prev)
                indexArray.append(i - 1, RouteLineDistancesIndex(prev, distance))
            }
            indexArray.append(
                points.size - 1,
                RouteLineDistancesIndex(points[points.size - 1], 0.0)
            )
            return RouteLineGranularDistances(distance, indexArray)
        }

        /**
         * Calculates the distance between 2 points using
         * [EPSG:3857 projection](https://epsg.io/3857).
         * Info in [mapbox-gl-js/issues/9998](https://github.com/mapbox/mapbox-gl-js/issues/9998).
         */
        fun calculateDistance(point1: Point, point2: Point): Double {
            val d = doubleArrayOf(
                (projectX(point1.longitude()) - projectX(point2.longitude())),
                (projectY(point1.latitude()) - projectY(point2.latitude()))
            )
            return sqrt(d[0] * d[0] + d[1] * d[1])
        }

        private fun projectX(x: Double): Double {
            return x / 360.0 + 0.5
        }

        private fun projectY(y: Double): Double {
            val sin = sin(y * Math.PI / 180)
            val y2 = 0.5 - 0.25 * ln((1 + sin) / (1 - sin)) / Math.PI
            return when {
                y2 < 0 -> 0.0
                y2 > 1 -> 1.1
                else -> y2
            }
        }

        /**
         * This will extract all of the road classes for the various sections of the route
         * and determine the distance from the origin point to the starting point for each
         * road class section and associate that data with a traffic congestion value.
         *
         * Each item returned represents a distance from the origin point, the road class
         * beginning at that distance and the traffic congestion value for that section. The
         * road class and congestion remain until the next item in the collections.
         *
         * The items returned are ordered from the point closest to the origin to the point
         * farthest from the origin. In other words from the beginning of the route until the end.
         *
         * @param route the the calculations should be performed on.
         */
        fun getRouteLineTrafficExpressionData(route: DirectionsRoute):
            List<RouteLineTrafficExpressionData> {
                var runningDistance = 0.0
                val routeLineTrafficData = mutableListOf<RouteLineTrafficExpressionData>()

                route.legs()?.forEach { leg ->
                    ifNonNull(leg.annotation()?.distance()) { distanceList ->
                        val intersectionsWithGeometryIndex = leg.steps()
                            ?.mapNotNull { it.intersections() }
                            ?.flatten()
                            ?.filter {
                                it.geometryIndex() != null
                            }?.toList() ?: listOf()

                        val roadClassArray = if (intersectionsWithGeometryIndex.isNotEmpty()) {
                            arrayOfNulls<String>(
                                intersectionsWithGeometryIndex.last().geometryIndex()!! + 1
                            ).apply {
                                intersectionsWithGeometryIndex.forEach {
                                    this[it.geometryIndex()!!] = it.mapboxStreetsV8()?.roadClass()
                                        ?: "intersection_without_class_fallback"
                                }
                            }
                        } else {
                            arrayOfNulls(0)
                        }

                        leg.annotation()?.congestion()?.forEachIndexed { index, congestion ->
                            val roadClass = getRoadClassForIndex(roadClassArray, index)
                            if (index == 0) {
                                routeLineTrafficData.add(
                                    RouteLineTrafficExpressionData(
                                        0.0,
                                        congestion,
                                        roadClass
                                    )
                                )
                            } else {
                                runningDistance += distanceList[index - 1]
                                val last = routeLineTrafficData.lastOrNull()
                                if (last?.trafficCongestionIdentifier == congestion &&
                                    last?.roadClass == roadClass
                                ) {
                                    // continue
                                } else if (last?.trafficCongestionIdentifier == congestion &&
                                    roadClass == null
                                ) {
                                    // continue
                                } else {
                                    routeLineTrafficData.add(
                                        RouteLineTrafficExpressionData(
                                            runningDistance,
                                            congestion,
                                            roadClass
                                        )
                                    )
                                }
                            }
                        }

                        runningDistance += distanceList.last()
                    }
                }

                return routeLineTrafficData
            }

        private fun getRoadClassForIndex(roadClassArray: Array<String?>, index: Int): String? {
            return if (roadClassArray.size > index) {
                roadClassArray.slice(0..index).last { it != null }
            } else {
                null
            }
        }

        /**
         * For each item in the trafficExpressionData collection a color substitution will take
         * place that has a road class contained in the trafficOverrideRoadClasses
         * collection. For each of these items the color for 'unknown' traffic congestion
         * will be replaced with the color for 'low' traffic congestion. In addition the
         * percentage of the route distance traveled will be calculated for each item in the
         * trafficExpressionData collection and included in the returned data.
         *
         * @param trafficExpressionData the traffic data to perform the substitution on
         * @param routeDistance the total distance of the route
         * @param congestionColorProvider a function that provides the colors used for various
         * traffic congestion values
         * @param isPrimaryRoute indicates if the route used is the primary route
         * @param trafficOverrideRoadClasses a collection of road classes for which a color
         * substitution should occur.
         */
        fun getRouteLineExpressionDataWithStreetClassOverride(
            trafficExpressionData: List<RouteLineTrafficExpressionData>,
            routeDistance: Double,
            congestionColorProvider: (String, Boolean) -> Int,
            isPrimaryRoute: Boolean,
            trafficOverrideRoadClasses: List<String>
        ): List<RouteLineExpressionData> {
            val colorExpressionMap = mutableMapOf<Int, Expression>()
            val expressionDataToReturn = mutableListOf<RouteLineExpressionData>()
            trafficExpressionData.forEachIndexed { index, trafficExpData ->
                val percentDistanceTraveled = trafficExpData.distanceFromOrigin / routeDistance
                val trafficIdentifier =
                    if (
                        trafficOverrideRoadClasses.contains(trafficExpData.roadClass) &&
                        trafficExpData.trafficCongestionIdentifier == UNKNOWN_CONGESTION_VALUE
                    ) {
                        LOW_CONGESTION_VALUE
                    } else {
                        trafficExpData.trafficCongestionIdentifier
                    }

                val trafficColor = congestionColorProvider(trafficIdentifier, isPrimaryRoute)
                val colorExpression = colorExpressionMap.getOrPut(
                    trafficColor,
                    { Expression.color(trafficColor) }
                )
                if (index == 0) {
                    expressionDataToReturn.add(
                        RouteLineExpressionData(
                            percentDistanceTraveled,
                            colorExpression
                        )
                    )
                } else if (colorExpression !=
                    expressionDataToReturn.last().segmentColorExpression
                ) {
                    expressionDataToReturn.add(
                        RouteLineExpressionData(
                            percentDistanceTraveled,
                            colorExpression
                        )
                    )
                }
            }
            return expressionDataToReturn
        }
    }
}

/**
 * Maintains an association between a DirectionsRoute, FeatureCollection
 * and LineString.
 *
 * @param route a DirectionsRoute
 * @param featureCollection a FeatureCollection created using the route
 * @param lineString a LineString derived from the route's geometry.
 */
internal data class RouteFeatureData(
    val route: DirectionsRoute,
    val featureCollection: FeatureCollection,
    val lineString: LineString
)

internal data class RouteLineExpressionData(
    val offset: Double,
    val segmentColorExpression: Expression
)

internal data class RouteLineTrafficExpressionData(
    val distanceFromOrigin: Double,
    val trafficCongestionIdentifier: String,
    val roadClass: String?
)

/**
 * @param point the upcoming, not yet visited point on the route
 * @param distanceRemaining distance remaining from the upcoming point
 */
internal data class RouteLineDistancesIndex(val point: Point, val distanceRemaining: Double)

/**
 * @param distance full distance of the route
 * @param distancesArray array where index is the index of the upcoming not yet visited point on the route
 */
internal data class RouteLineGranularDistances(
    val distance: Double,
    val distancesArray: SparseArray<RouteLineDistancesIndex>
)

/**
 * @param nestedList nested arrays of legs -> steps -> points
 * @param flatList list of all points on the route.
 * The first and last point of adjacent steps overlap and are duplicated in this list.
 */
internal data class RoutePoints(
    val nestedList: List<List<List<Point>>>,
    val flatList: List<Point>
)

/**
 * Describes the vanishing point update algorithm's state.
 */
internal enum class VanishingPointState {
    /**
     * Always try to take the most recently calculated distance and set the vanishing point.
     */
    ENABLED,

    /**
     * Try to take the most recently calculated distance and set the vanishing point.
     *
     * Accept the value only if the progress is greater than the last update. This avoids
     * the vanishing point from creeping backwards after the destination is passed.
     */
    ONLY_INCREASE_PROGRESS,

    /**
     * Ignore puck position updates and leave the vanishing point in the current position.
     */
    DISABLED
}
