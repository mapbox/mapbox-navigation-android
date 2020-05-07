package com.mapbox.navigation.ui.route

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.libnavigation.ui.R
import com.mapbox.mapboxsdk.location.LocationComponentConstants
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.expressions.Expression
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineGradient
import com.mapbox.mapboxsdk.style.layers.SymbolLayer
import com.mapbox.mapboxsdk.style.sources.GeoJsonOptions
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.navigation.ui.route.MapRouteLine.MapRouteLineSupport.buildRouteLineExpression
import com.mapbox.navigation.ui.route.MapRouteLine.MapRouteLineSupport.buildWayPointFeatureCollection
import com.mapbox.navigation.ui.route.MapRouteLine.MapRouteLineSupport.generateFeatureCollection
import com.mapbox.navigation.ui.route.MapRouteLine.MapRouteLineSupport.getBelowLayer
import com.mapbox.navigation.ui.route.MapRouteLine.MapRouteLineSupport.getBooleanStyledValue
import com.mapbox.navigation.ui.route.MapRouteLine.MapRouteLineSupport.getFloatStyledValue
import com.mapbox.navigation.ui.route.MapRouteLine.MapRouteLineSupport.getResourceStyledValue
import com.mapbox.navigation.ui.route.MapRouteLine.MapRouteLineSupport.getStyledColor
import com.mapbox.navigation.ui.route.RouteConstants.ALTERNATIVE_ROUTE_SOURCE_ID
import com.mapbox.navigation.ui.route.RouteConstants.HEAVY_CONGESTION_VALUE
import com.mapbox.navigation.ui.route.RouteConstants.MODERATE_CONGESTION_VALUE
import com.mapbox.navigation.ui.route.RouteConstants.PRIMARY_ROUTE_LAYER_ID
import com.mapbox.navigation.ui.route.RouteConstants.SEVERE_CONGESTION_VALUE
import com.mapbox.navigation.ui.route.RouteConstants.WAYPOINT_DESTINATION_VALUE
import com.mapbox.navigation.ui.route.RouteConstants.WAYPOINT_ORIGIN_VALUE
import com.mapbox.navigation.ui.route.RouteConstants.WAYPOINT_PROPERTY_KEY
import com.mapbox.navigation.ui.utils.MapUtils
import com.mapbox.navigation.utils.extensions.parallelMap
import com.mapbox.navigation.utils.internal.ThreadController
import com.mapbox.turf.TurfMeasurement

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
 */
internal class MapRouteLine(
    private val context: Context,
    private val style: Style,
    @androidx.annotation.StyleRes styleRes: Int,
    belowLayerId: String?,
    layerProvider: MapRouteLayerProvider,
    mapRouteSourceProvider: MapRouteSourceProvider
) {

    /**
     * @param context a context to provide resource values
     * @param style the style reference from the Mapbox map
     * @param styleRes a style resource
     * @param belowLayerId determines the elevation of the route layers
     * @param layerProvider provides the layer configurations for the route layers
     * @param routeFeatureDatas used to restore this class with supplied values
     * @param allRoutesVisible true if all the route layers should be visible
     * @param alternativesVisible true if the alternative route layer is visible
     */
    constructor(
        context: Context,
        style: Style,
        @androidx.annotation.StyleRes styleRes: Int,
        belowLayerId: String?,
        layerProvider: MapRouteLayerProvider,
        routeFeatureDatas: List<RouteFeatureData>,
        allRoutesVisible: Boolean,
        alternativesVisible: Boolean,
        mapRouteSourceProvider: MapRouteSourceProvider
    ) : this(context, style, styleRes, belowLayerId, layerProvider, mapRouteSourceProvider) {
        this.alternativesVisible = alternativesVisible
        this.allLayersAreVisible = allRoutesVisible
        this.routeFeatureData.addAll(routeFeatureDatas)
    }

    private var drawnWaypointsFeatureCollection: FeatureCollection = FeatureCollection.fromFeatures(arrayOf())
    private var drawnPrimaryRouteFeatureCollection: FeatureCollection = FeatureCollection.fromFeatures(arrayOf())
    private var drawnAlternativeRouteFeatureCollection: FeatureCollection = FeatureCollection.fromFeatures(arrayOf())

    private var wayPointSource: GeoJsonSource
    private val primaryRouteLineSource: GeoJsonSource
    private val alternativeRouteLineSource: GeoJsonSource
    private val routeLayerIds = mutableSetOf<String>()
    private val directionsRoutes = mutableListOf<DirectionsRoute>()
    private val routeFeatureData = mutableListOf<RouteFeatureData>()
    private var alternativesVisible = true
    private var allLayersAreVisible = true
    private var primaryRoute: DirectionsRoute? = null

    private val routeDefaultColor: Int by lazy {
        getStyledColor(
            R.styleable.NavigationMapRoute_routeColor,
            R.color.mapbox_navigation_route_layer_blue,
            context,
            styleRes
        )
    }

    private val routeModerateColor: Int by lazy {
        getStyledColor(
            R.styleable.NavigationMapRoute_routeModerateCongestionColor,
            R.color.mapbox_navigation_route_layer_congestion_yellow,
            context,
            styleRes
        )
    }

    private val routeSevereColor: Int by lazy {
        getStyledColor(
            R.styleable.NavigationMapRoute_routeSevereCongestionColor,
            R.color.mapbox_navigation_route_layer_congestion_red,
            context,
            styleRes
        )
    }

    private val routeShieldColor: Int by lazy {
        getStyledColor(
            R.styleable.NavigationMapRoute_routeShieldColor,
            R.color.mapbox_navigation_route_shield_layer_color,
            context,
            styleRes
        )
    }

    private val routeScale: Float by lazy {
        getFloatStyledValue(
            R.styleable.NavigationMapRoute_routeScale,
            1.0f,
            context,
            styleRes
        )
    }

    private val roundedLineCap: Boolean by lazy {
        getBooleanStyledValue(
            R.styleable.NavigationMapRoute_roundedLineCap,
            true,
            context,
            styleRes
        )
    }

    private val alternativeRouteDefaultColor: Int by lazy {
        getStyledColor(
            R.styleable.NavigationMapRoute_alternativeRouteColor,
            R.color.mapbox_navigation_route_alternative_color,
            context,
            styleRes
        )
    }

    private val alternativeRouteModerateColor: Int by lazy {
        getStyledColor(
            R.styleable.NavigationMapRoute_alternativeRouteSevereCongestionColor,
            R.color.mapbox_navigation_route_alternative_congestion_red,
            context,
            styleRes
        )
    }

    private val alternativeRouteSevereColor: Int by lazy {
        getStyledColor(
            R.styleable.NavigationMapRoute_alternativeRouteSevereCongestionColor,
            R.color.mapbox_navigation_route_alternative_congestion_red,
            context,
            styleRes
        )
    }

    private val alternativeRouteShieldColor: Int by lazy {
        getStyledColor(
            R.styleable.NavigationMapRoute_alternativeRouteShieldColor,
            R.color.mapbox_navigation_route_alternative_shield_color,
            context,
            styleRes
        )
    }

    private val alternativeRouteScale: Float by lazy {
        getFloatStyledValue(
            R.styleable.NavigationMapRoute_alternativeRouteScale,
            1.0f,
            context,
            styleRes
        )
    }

    /**
     * Initializes the instance with appropriate default values.
     */
    init {
        val wayPointGeoJsonOptions = GeoJsonOptions().withMaxZoom(16)
        wayPointSource = mapRouteSourceProvider.build(
            RouteConstants.WAYPOINT_SOURCE_ID,
            drawnWaypointsFeatureCollection,
            wayPointGeoJsonOptions
        )
        style.addSource(wayPointSource)

        val routeLineGeoJsonOptions = GeoJsonOptions().withMaxZoom(16).withLineMetrics(true)
        primaryRouteLineSource = mapRouteSourceProvider.build(
            RouteConstants.PRIMARY_ROUTE_SOURCE_ID,
            drawnPrimaryRouteFeatureCollection,
            routeLineGeoJsonOptions
        )
        style.addSource(primaryRouteLineSource)

        val alternativeRouteLineGeoJsonOptions = GeoJsonOptions().withMaxZoom(16).withLineMetrics(true)
        alternativeRouteLineSource = mapRouteSourceProvider.build(
            ALTERNATIVE_ROUTE_SOURCE_ID,
            drawnAlternativeRouteFeatureCollection,
            alternativeRouteLineGeoJsonOptions
        )
        style.addSource(alternativeRouteLineSource)

        val originWaypointIcon = getResourceStyledValue(
            R.styleable.NavigationMapRoute_originWaypointIcon,
            R.drawable.ic_route_origin,
            context,
            styleRes
        )

        val destinationWaypointIcon = getResourceStyledValue(
            R.styleable.NavigationMapRoute_destinationWaypointIcon,
            R.drawable.ic_route_destination,
            context,
            styleRes
        )
        val originIcon = AppCompatResources.getDrawable(context, originWaypointIcon)
        val destinationIcon = AppCompatResources.getDrawable(context, destinationWaypointIcon)
        val belowLayer = getBelowLayer(belowLayerId, style)

        initializeLayers(style, layerProvider, originIcon!!, destinationIcon!!, belowLayer)
        updateAlternativeLayersVisibility(alternativesVisible, routeLayerIds)
        updateAllLayersVisibility(allLayersAreVisible)
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
        if (directionsRoutes.isNotEmpty()) {
            clearRouteData()
            this.directionsRoutes.addAll(directionsRoutes)
            primaryRoute = this.directionsRoutes.first()
            alternativesVisible = directionsRoutes.size > 1
            allLayersAreVisible = true
            val newRouteFeatureData = directionsRoutes.parallelMap(
                ::generateFeatureCollection,
                ThreadController.getMainScopeAndRootJob().scope
            )
            routeFeatureData.addAll(newRouteFeatureData)
            drawRoutes(newRouteFeatureData)
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
    fun updatePrimaryRouteIndex(route: DirectionsRoute): Boolean {
        return if (route != this.primaryRoute) {
            this.primaryRoute = route
            drawRoutes(routeFeatureData)
            true
        } else {
            false
        }
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

    /**
     * Updates the visiblity of the route layers.
     *
     * @param isVisible true indicates all of the route layers will become visible.
     */
    fun updateVisibilityTo(isVisible: Boolean) {
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
        layerProvider: MapRouteLayerProvider,
        originIcon: Drawable,
        destinationIcon: Drawable,
        belowLayerId: String
    ) {

        layerProvider.initializeAlternativeRouteShieldLayer(
            style,
            alternativeRouteScale,
            alternativeRouteShieldColor
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
            alternativeRouteScale,
            alternativeRouteDefaultColor
        ).apply {
            MapUtils.addLayerToMap(
                style,
                this,
                belowLayerId
            )
            routeLayerIds.add(this.id)
        }

        layerProvider.initializePrimaryRouteShieldLayer(
            style,
            routeScale,
            routeShieldColor
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
            routeScale,
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
            style, originIcon, destinationIcon
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
            setPrimaryRoutesSource(it.featureCollection)
            val lineString: LineString = getLineStringForRoute(it.route)
            val expression = buildRouteLineExpression(
                it.route,
                lineString,
                true,
                0.0,
                ::getRouteColorForCongestion)

            if (style.isFullyLoaded) {
                style.getLayer(PRIMARY_ROUTE_LAYER_ID)?.setProperties(lineGradient(expression))
            }
        }

        partitionedRoutes.second.mapNotNull {
            it.featureCollection.features()
        }.flatten().let {
            setAlternativeRoutesSource(FeatureCollection.fromFeatures(it))
        }
    }

    private fun clearRouteData() {
        directionsRoutes.clear()
        routeFeatureData.clear()
        setPrimaryRoutesSource(FeatureCollection.fromFeatures(arrayOf()))
        setAlternativeRoutesSource(FeatureCollection.fromFeatures(arrayOf()))
        setWaypointsSource(FeatureCollection.fromFeatures(arrayOf()))
    }

    private fun setPrimaryRoutesSource(featureCollection: FeatureCollection) {
        drawnPrimaryRouteFeatureCollection = featureCollection
        primaryRouteLineSource.setGeoJson(drawnPrimaryRouteFeatureCollection)
    }

    private fun setAlternativeRoutesSource(featureCollection: FeatureCollection) {
        drawnAlternativeRouteFeatureCollection = featureCollection
        alternativeRouteLineSource.setGeoJson(drawnAlternativeRouteFeatureCollection)
    }

    private fun setWaypointsSource(featureCollection: FeatureCollection) {
        drawnWaypointsFeatureCollection = featureCollection
        wayPointSource.setGeoJson(drawnWaypointsFeatureCollection)
    }

    private fun updateAlternativeLayersVisibility(isAlternativeVisible: Boolean, routeLayerIds: Set<String>) {
        if (style.isFullyLoaded) {
            routeLayerIds.filter {
                it == RouteConstants.ALTERNATIVE_ROUTE_LAYER_ID ||
                    it == RouteConstants.ALTERNATIVE_ROUTE_SHIELD_LAYER_ID
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
    internal fun getRouteColorForCongestion(congestionValue: String, isPrimaryRoute: Boolean): Int {
        return when (isPrimaryRoute) {
            true -> when (congestionValue) {
                MODERATE_CONGESTION_VALUE -> routeModerateColor
                HEAVY_CONGESTION_VALUE -> routeSevereColor
                SEVERE_CONGESTION_VALUE -> routeSevereColor
                else -> routeDefaultColor
            }
            false -> when (congestionValue) {
                MODERATE_CONGESTION_VALUE -> alternativeRouteModerateColor
                HEAVY_CONGESTION_VALUE -> alternativeRouteSevereColor
                SEVERE_CONGESTION_VALUE -> alternativeRouteSevereColor
                else -> alternativeRouteDefaultColor
            }
        }
    }

    /**
     * Hides the RouteShieldLayer
     *
     * @param offset the offset of the visibility in the expression
     */
    fun hideShieldLineAtOffset(offset: Float) {
        val expression = Expression.step(
            Expression.lineProgress(),
            Expression.rgba(0, 0, 0, 0),
            Expression.stop(
                offset,
                Expression.color(routeShieldColor)
            )
        )
        if (style.isFullyLoaded) {
            style.getLayerAs<LineLayer>(RouteConstants.PRIMARY_ROUTE_SHIELD_LAYER_ID)
                ?.setProperties(lineGradient(expression))
        }
    }

    /**
     * Applies an Expression to the route line layer.
     *
     * @param expression the Expression to apply to the layer properties
     */
    fun decorateRouteLine(expression: Expression) {
        if (style.isFullyLoaded) {
            style.getLayer(PRIMARY_ROUTE_LAYER_ID)?.setProperties(
                lineGradient(
                    expression
                )
            )
        }
    }

    internal object MapRouteLineSupport {

        /**
         * Returns a resource value from the style or a default value
         * @param index the index of the item in the styled attributes.
         * @param defaultValue the default value to use if no value is found
         * @param context the context to obtain the resource from
         * @param styleRes the style resource to look in
         *
         * @return the resource value
         */
        fun getStyledColor(index: Int, colorResourceId: Int, context: Context, styleRes: Int): Int {
            val typedArray = context.obtainStyledAttributes(styleRes, R.styleable.NavigationMapRoute)
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
        fun getFloatStyledValue(index: Int, defaultValue: Float, context: Context, styleRes: Int): Float {
            val typedArray = context.obtainStyledAttributes(styleRes, R.styleable.NavigationMapRoute)
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
        fun getBooleanStyledValue(index: Int, defaultValue: Boolean, context: Context, styleRes: Int): Boolean {
            val typedArray = context.obtainStyledAttributes(styleRes, R.styleable.NavigationMapRoute)
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
        fun getResourceStyledValue(index: Int, defaultValue: Int, context: Context, styleRes: Int): Int {
            val typedArray = context.obtainStyledAttributes(styleRes, R.styleable.NavigationMapRoute)
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
            return when (layerId.isNullOrEmpty()) {
                false -> style.layers.firstOrNull { it.id == layerId }?.id
                true -> style.layers.reversed().filter { it !is SymbolLayer }
                    .firstOrNull { it.id != RouteConstants.MAPBOX_LOCATION_ID }
                    ?.id
            } ?: LocationComponentConstants.SHADOW_LAYER
        }

        /**
         * Generates a FeatureCollection and LineString based on the @param route.
         * @param route the DirectionsRoute to used to derive the result
         *
         * @return a RouteFeatureData containing the original route and a FeatureCollection and
         * LineString
         */
        fun generateFeatureCollection(route: DirectionsRoute): RouteFeatureData {
            val routeGeometry = LineString.fromPolyline(
                route.geometry() ?: "",
                Constants.PRECISION_6
            )
            val routeFeature = Feature.fromGeometry(routeGeometry)

            return RouteFeatureData(route, FeatureCollection.fromFeatures(listOf(routeFeature)), routeGeometry)
        }

        /**
         * Creates an Expression that can be applied to the layer style changing the appearance of
         * a route line, making the portion of the rout line behind the puck invisible.
         *
         * @param route the DirectionsRoute used for the Expression calculations
         * @param routeLineString an optional LineString derived from the DirectionsRoute's geometry
         * @param isPrimaryRoute indicates if the route used is the primary route
         * @param distFractionToVanish indicates what portion of the route line should be invisible,
         * usually based on a previous calculation. Traffic sections before this distance value won't
         * be calculated.
         * @param congestionColorProvider a function that provides the colors used for various
         * traffic congestion values
         *
         * @return the Expression that can be used in a Layer's properties.
         */
        @JvmStatic
        fun buildRouteLineExpression(
            route: DirectionsRoute,
            routeLineString: LineString,
            isPrimaryRoute: Boolean,
            distFractionToVanish: Double,
            congestionColorProvider: (String, Boolean) -> Int
        ): Expression {
            val scrubbedRouteLegs = route.legs()
                ?.filter { it.annotation()?.congestion() != null }

            val stopExpressionsWithTraffic: List<Expression.Stop> = when (scrubbedRouteLegs?.isEmpty()) {
                false -> scrubbedRouteLegs.map {
                    getStopsFromLeg(
                        it,
                        distFractionToVanish,
                        routeLineString,
                        route.distance() ?: 0.0,
                        isPrimaryRoute,
                        congestionColorProvider
                    )
                }.flatten()
                else -> listOf(Expression.stop(
                    distFractionToVanish,
                    Expression.color(congestionColorProvider("", isPrimaryRoute)
                    )))
            }

            return Expression.step(
                Expression.lineProgress(),
                Expression.rgba(0, 0, 0, 0),
                *stopExpressionsWithTraffic.toTypedArray()
            )
        }

        /**
         * Creates a collection of Expression.Stop objects.
         * @param leg the RouteLeg to use in the calculation
         * @param distFractionToVanish the fractional value of the route's distance that shouldn't
         * be visible
         * @param lineString derived from the route's geometry, used for calculating the fractional
         * distance to be used in the Expression.Stop
         * @param routeDistance the total distance of the route
         * @param isPrimary determines the color used for the Expression.Stop
         * @param congestionColorProvider a function that provides the colors used for various
         * traffic congestion values
         *
         *  @return the collection of Expression.Stop objects created
         */
        fun getStopsFromLeg(
            leg: RouteLeg,
            distFractionToVanish: Double,
            lineString: LineString,
            routeDistance: Double,
            isPrimary: Boolean,
            congestionColorProvider: (String, Boolean) -> Int
        ): List<Expression.Stop> {
            val previousDistances = mutableListOf<Double>()
            val expressionStops = mutableListOf<Expression.Stop>()
            val numCongestionPoints: Int = leg.annotation()?.congestion()?.size ?: 0
            var congestionValue = ""
            for (i in 0 until numCongestionPoints) {
                val currentLegCongestionValue = leg.annotation()?.congestion()?.get(i) ?: ""
                if (currentLegCongestionValue == congestionValue) {
                    continue
                }
                congestionValue = currentLegCongestionValue

                if (numCongestionPoints + 1 <= lineString.coordinates().size) {
                    val dist = (TurfMeasurement.distance(
                        lineString.coordinates()[0],
                        lineString.coordinates()[i + 1]
                    ) * 1000)
                    val fractionalDist: Double = dist / routeDistance
                    if (fractionalDist < distFractionToVanish) {
                        continue
                    }

                    if (previousDistances.isNotEmpty() && fractionalDist < previousDistances[previousDistances.size - 1]) {
                        continue
                    }

                    if (expressionStops.isEmpty()) {
                        val vanishStop = Expression.stop(
                            distFractionToVanish,
                            Expression.color(congestionColorProvider(congestionValue, isPrimary))
                        )
                        expressionStops.add(vanishStop)
                    }

                    val routeColor = congestionColorProvider(congestionValue, isPrimary)
                    val stop = Expression.stop(
                        fractionalDist,
                        Expression.color(routeColor)
                    )
                    previousDistances.add(fractionalDist)
                    expressionStops.add(stop)
                }
            }

            if (expressionStops.isEmpty()) {
                val vanishStop = Expression.stop(
                    distFractionToVanish,
                    Expression.color(congestionColorProvider(congestionValue, isPrimary))
                )
                expressionStops.add(vanishStop)
            }

            return expressionStops
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
         * @return a Feature represeting the waypoint from the RouteLog
         */
        fun buildWayPointFeatureFromLeg(leg: RouteLeg, index: Int): Feature? {
            return leg.steps()?.get(index)?.maneuver()?.location()?.run {
                Feature.fromGeometry(Point.fromLngLat(this.longitude(), this.latitude()))
            }?.also {
                val propValue = if (index == 0) WAYPOINT_ORIGIN_VALUE else WAYPOINT_DESTINATION_VALUE
                it.addStringProperty(WAYPOINT_PROPERTY_KEY, propValue)
            }
        }
    }
}

/**
 * Maintains an association between a DirectionsRoute, FeatureCollection
 * and LineString.
 *
 * @param route a Directionsroute
 * @param featureCollection a FeatureCollection created using the route
 * @param lineString a LineString derived from the route's geometry.
 */
data class RouteFeatureData(
    val route: DirectionsRoute,
    val featureCollection: FeatureCollection,
    val lineString: LineString
)
