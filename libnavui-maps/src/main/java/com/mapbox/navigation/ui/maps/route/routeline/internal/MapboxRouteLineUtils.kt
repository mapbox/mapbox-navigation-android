package com.mapbox.navigation.ui.maps.route.routeline.internal

import android.content.Context
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
import com.mapbox.maps.LayerPosition
import com.mapbox.maps.Style
import com.mapbox.maps.StyleObjectInfo
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.plugin.location.LocationComponentConstants
import com.mapbox.navigation.ui.internal.route.RouteConstants
import com.mapbox.navigation.ui.internal.route.RouteConstants.LOW_CONGESTION_VALUE
import com.mapbox.navigation.ui.maps.R
import com.mapbox.navigation.ui.maps.route.routeline.api.RouteLineResourceProvider
import com.mapbox.navigation.ui.maps.route.routeline.model.IdentifiableRoute
import com.mapbox.navigation.ui.maps.route.routeline.model.RouteFeatureData
import com.mapbox.navigation.ui.maps.route.routeline.model.RouteLineDistancesIndex
import com.mapbox.navigation.ui.maps.route.routeline.model.RouteLineExpressionData
import com.mapbox.navigation.ui.maps.route.routeline.model.RouteLineGranularDistances
import com.mapbox.navigation.ui.maps.route.routeline.model.RouteLineScaleValue
import com.mapbox.navigation.ui.maps.route.routeline.model.RouteLineTrafficExpressionData
import com.mapbox.navigation.ui.maps.route.routeline.model.RoutePoints
import com.mapbox.navigation.ui.maps.route.routeline.model.RouteStyleDescriptor
import com.mapbox.navigation.util.internal.ifNonNull
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMisc
import timber.log.Timber
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

object MapboxRouteLineUtils {

    /**
     * Creates an [Expression] that can be applied to the layer style changing the appearance of
     * a route line, making the portion of the route line behind the puck invisible.
     *
     * @param distanceOffset the percentage of the distance traveled which will represent
     * the part of the route line that isn't visible
     *
     * @return the Expression that can be used in a Layer's properties.
     */
    fun getTrafficLineExpression(
        distanceOffset: Double,
        routeLineExpressionData: List<RouteLineExpressionData>,
        fallbackRouteColor: Int
    ): Expression {
        val expressionBuilder = Expression.ExpressionBuilder("step")
        expressionBuilder.lineProgress()
        expressionBuilder.stop {
            rgba {
                literal(0.0)
                literal(0.0)
                literal(0.0)
                literal(0.0)
            }
        }
        val filteredItems = routeLineExpressionData.filter { it.offset > distanceOffset }
        when (filteredItems.isEmpty()) {
            true -> when (routeLineExpressionData.isEmpty()) {
                true -> listOf(RouteLineExpressionData(distanceOffset, fallbackRouteColor))
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
        }.forEach {
            expressionBuilder.stop {
                literal(it.offset)
                color(it.segmentColor)
            }
        }

        return expressionBuilder.build()
    }

    fun getVanishingRouteLineExpression(
        offset: Double,
        traveledColor: Int,
        lineBaseColor: Int
    ): Expression {
        val expressionBuilder = Expression.ExpressionBuilder("step")
        expressionBuilder.lineProgress()
        expressionBuilder.color(traveledColor)
        expressionBuilder.stop {
            literal(offset)
            color(lineBaseColor)
        }
        return expressionBuilder.build()
    }

    fun getRouteFeatureDataProvider(
        directionsRoutes: List<DirectionsRoute>
    ): () -> List<RouteFeatureData> = {
        directionsRoutes.map(::generateFeatureCollection)
    }

    fun getIdentifiableRouteFeatureDataProvider(
        directionsRoutes: List<IdentifiableRoute>
    ): () -> List<RouteFeatureData> = {
        directionsRoutes.map(::generateFeatureCollection)
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

    /**
     * Decodes the route geometry into nested arrays of legs -> steps -> points.
     *
     * The first and last point of adjacent steps overlap and are duplicated.
     */
    fun parseRoutePoints(route: DirectionsRoute): RoutePoints? {
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

    fun calculateRouteGranularDistances(coordinates: List<Point>):
        RouteLineGranularDistances? {
            return if (coordinates.isNotEmpty()) {
                calculateGranularDistances(coordinates)
            } else {
                null
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

    private fun generateFeatureCollection(route: DirectionsRoute, identifier: String?):
        RouteFeatureData {
            val routeGeometry = LineString.fromPolyline(
                route.geometry() ?: "",
                Constants.PRECISION_6
            )

            val routeFeature = when (identifier) {
                null -> Feature.fromGeometry(routeGeometry)
                else -> Feature.fromGeometry(routeGeometry).also {
                    it.addBooleanProperty(identifier, true)
                }
            }

            return RouteFeatureData(
                route,
                FeatureCollection.fromFeatures(listOf(routeFeature)),
                routeGeometry
            )
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
    fun getRouteLineTrafficExpressionData(
        route: DirectionsRoute
    ): List<RouteLineTrafficExpressionData> {
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
                    congestionColorProvider("", isPrimaryRoute)
                )
            )
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
        val expressionDataToReturn = mutableListOf<RouteLineExpressionData>()
        trafficExpressionData.forEachIndexed { index, trafficExpData ->
            val percentDistanceTraveled = trafficExpData.distanceFromOrigin / routeDistance
            val trafficIdentifier =
                if (
                    trafficOverrideRoadClasses.contains(trafficExpData.roadClass) &&
                    trafficExpData.trafficCongestionIdentifier ==
                    RouteConstants.UNKNOWN_CONGESTION_VALUE
                ) {
                    LOW_CONGESTION_VALUE
                } else {
                    trafficExpData.trafficCongestionIdentifier
                }

            val trafficColor = congestionColorProvider(trafficIdentifier, isPrimaryRoute)
            if (index == 0) {
                expressionDataToReturn.add(
                    RouteLineExpressionData(
                        percentDistanceTraveled,
                        trafficColor
                    )
                )
            } else if (trafficColor != expressionDataToReturn.last().segmentColor) {
                expressionDataToReturn.add(
                    RouteLineExpressionData(
                        percentDistanceTraveled,
                        trafficColor
                    )
                )
            }
        }
        return expressionDataToReturn
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
                if (index == 0) RouteConstants.WAYPOINT_ORIGIN_VALUE
                else RouteConstants.WAYPOINT_DESTINATION_VALUE
            it.addStringProperty(RouteConstants.WAYPOINT_PROPERTY_KEY, propValue)
        }
    }

    fun buildScalingExpression(scalingValues: List<RouteLineScaleValue>): Expression {
        val expressionBuilder = Expression.ExpressionBuilder("interpolate")
        expressionBuilder.addArgument(Expression.exponential { literal(1.5) })
        expressionBuilder.zoom()
        scalingValues.forEach { routeLineScaleValue ->
            expressionBuilder.stop {
                this.literal(routeLineScaleValue.scaleStop.toDouble())
                product {
                    literal(routeLineScaleValue.scaleMultiplier.toDouble())
                    literal(routeLineScaleValue.scale.toDouble())
                }
            }
        }
        return expressionBuilder.build()
    }

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

    fun initializeRouteLineLayers(
        context: Context,
        style: Style,
        styleRes: Int,
        routeStyleDescriptors: List<RouteStyleDescriptor>,
        belowLayerId: String
    ) {
        val routeLineResourceProvider =
            MapboxRouteLineResourceProviderFactory.getRouteLineResourceProvider(
                context,
                styleRes
            )
        val originIcon = AppCompatResources.getDrawable(
            context,
            routeLineResourceProvider.getOriginWaypointIcon()
        )
        val destinationIcon = AppCompatResources.getDrawable(
            context,
            routeLineResourceProvider.getDestinationWaypointIcon()
        )
        val layerProvider = MapboxRouteLayerProviderFactory.getLayerProvider(
            routeStyleDescriptors,
            context,
            styleRes
        )

        initializeRouteLineLayers(
            style,
            routeLineResourceProvider,
            layerProvider,
            MapboxWayPointIconProvider(originIcon!!, destinationIcon!!),
            belowLayerId
        )
    }

    @JvmStatic
    fun initializeRouteLineLayers(
        style: Style,
        routeLineResourceProvider: RouteLineResourceProvider,
        layerProvider: RouteLayerProvider,
        wayPointIconProvider: WayPointIconProvider,
        belowLayerId: String
    ) {
        if (!style.isFullyLoaded()) {
            return
        }

        val originIcon = wayPointIconProvider.getOriginIconDrawable()
        val destinationIcon = wayPointIconProvider.getOriginIconDrawable()

        if (!style.styleSourceExists(RouteConstants.WAYPOINT_SOURCE_ID)) {
            geoJsonSource(RouteConstants.WAYPOINT_SOURCE_ID) {
                maxzoom(16)
                featureCollection(FeatureCollection.fromFeatures(listOf()))
            }.bindTo(style)
        }

        if (!style.styleSourceExists(RouteConstants.PRIMARY_ROUTE_SOURCE_ID)) {
            val primaryRouteSource = geoJsonSource(RouteConstants.PRIMARY_ROUTE_SOURCE_ID) {
                maxzoom(16)
                lineMetrics(true)
            }
            primaryRouteSource.featureCollection(FeatureCollection.fromFeatures(listOf<Feature>()))
            primaryRouteSource.bindTo(style)
        }

        if (!style.styleSourceExists(RouteConstants.ALTERNATIVE_ROUTE_SOURCE_ID)) {
            val altRouteSource = geoJsonSource(RouteConstants.ALTERNATIVE_ROUTE_SOURCE_ID) {
                maxzoom(16)
                lineMetrics(true)
            }
            altRouteSource.featureCollection(FeatureCollection.fromFeatures(listOf<Feature>()))
            altRouteSource.bindTo(style)
        }

        layerProvider.initializeAlternativeRouteCasingLayer(
            style,
            routeLineResourceProvider.getAlternativeRouteLineCasingColor()
        ).bindTo(style, LayerPosition(null, belowLayerId, null))
        layerProvider.initializeAlternativeRouteLayer(
            style,
            routeLineResourceProvider.getUseRoundedLineCap(),
            routeLineResourceProvider.getAlternativeRouteLineBaseColor()
        ).bindTo(style, LayerPosition(null, belowLayerId, null))
        layerProvider.initializePrimaryRouteCasingLayer(
            style,
            routeLineResourceProvider.getRouteLineCasingColor()
        ).bindTo(style, LayerPosition(null, belowLayerId, null))
        layerProvider.initializePrimaryRouteLayer(
            style,
            routeLineResourceProvider.getUseRoundedLineCap(),
            routeLineResourceProvider.getRouteLineBaseColor()
        ).bindTo(style, LayerPosition(null, belowLayerId, null))
        layerProvider.initializePrimaryRouteTrafficLayer(
            style,
            routeLineResourceProvider.getUseRoundedLineCap(),
            routeLineResourceProvider.getRouteLineBaseColor()
        ).bindTo(style, LayerPosition(null, belowLayerId, null))
        layerProvider.initializeWayPointLayer(style, originIcon, destinationIcon).bindTo(
            style,
            LayerPosition(null, belowLayerId, null)
        )
    }

    fun getLayerVisibility(style: Style, layerId: String): Visibility? {
        return if (style.isFullyLoaded()) {
            style.getLayer(layerId)?.visibility
        } else {
            null
        }
    }

    /**
     * Checks if a layer with the given ID exists else returns a default layer ID
     * @param layerId the layer ID to look for
     * @param style the style containing the layers
     *
     * @return either the layer ID if found else a default layer ID
     */
    @JvmStatic
    fun getDefaultBelowLayer(layerId: String?, style: Style): String {
        val layers = style.styleLayers
        return when (layerId.isNullOrEmpty()) {
            false -> checkLayerIdPresent(layerId, layers)
            true -> findLayerBelow(layers)
        }
    }

    private fun checkLayerIdPresent(layerId: String, layers: List<StyleObjectInfo>): String {
        val foundId = layers.firstOrNull { it.id == layerId }?.id
        if (foundId == null) {
            Timber.e(
                """Tried placing route line below "$layerId" which doesn't exist"""
            )
        }
        return foundId
            ?: LocationComponentConstants.FOREGROUND_LAYER // fixme is this the correct layer???
    }

    /**
     * Tries to find a reference layer ID that's above a first non-symbol layer from the top
     * of the stack of layers. Additionally, the algorithm always ensures that the reference
     * layer is below the puck layers.
     */
    private fun findLayerBelow(layers: List<StyleObjectInfo>): String {
        val puckLayerIndex = layers.indexOfFirst {
            it.id.contains(RouteConstants.MAPBOX_LOCATION_ID)
        }
        val lastSymbolLayerFromTopIndex = layers.indexOfLast {
            it.type != "symbol" && !it.id.contains(RouteConstants.MAPBOX_LOCATION_ID)
        } + 1
        val index = if (puckLayerIndex in 0 until lastSymbolLayerFromTopIndex) {
            puckLayerIndex
        } else {
            lastSymbolLayerFromTopIndex
        }
        return layers.getOrNull(index)?.id ?: LocationComponentConstants.FOREGROUND_LAYER
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

    /**
     * Creates a line from the upcoming geometry point and the previous 10 points
     * and tries to find the the distance from current point to that line.
     *
     * We need to take more points than <previous - upcoming> because the route progress update
     * can jump by more than 1 geometry point.
     * If this happens, the puck will animate through multiple geometry points,
     * so we need to make a line with a buffer.
     */
    fun findDistanceToNearestPointOnCurrentLine(
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
}
