package com.mapbox.navigation.ui.maps.internal.route.line

import android.util.Log
import android.util.SparseArray
import androidx.annotation.ColorInt
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
import com.mapbox.maps.extension.style.expressions.dsl.generated.color
import com.mapbox.maps.extension.style.expressions.dsl.generated.eq
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.navigation.ui.base.internal.model.route.RouteConstants
import com.mapbox.navigation.ui.base.model.route.RouteLayerConstants
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteFeatureData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineDistancesIndex
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineExpressionData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineGranularDistances
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineScaleValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineTrafficExpressionData
import com.mapbox.navigation.ui.maps.route.line.model.RoutePoints
import com.mapbox.navigation.ui.maps.route.line.model.RouteStyleDescriptor
import com.mapbox.navigation.ui.utils.internal.ifNonNull
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMisc
import java.util.UUID
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.reflect.KProperty1

object MapboxRouteLineUtils {

    private val TAG = "MbxMapboxRouteLineUtils"

    /**
     * Creates an [Expression] that can be applied to the layer style changing the appearance of
     * a route line, making the portion of the route line behind the puck invisible.
     *
     * @param distanceOffset the percentage of the distance traveled which will represent
     * the part of the route line that isn't visible
     *
     * @return the Expression that can be used in a Layer's properties.
     */
    internal fun getTrafficLineExpression(
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
        val filteredItems = routeLineExpressionData
            .filter { it.offset > distanceOffset }.distinctBy { it.offset }
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

    /**
     * Creates a line gradient applying a color from the beginning of the line to the
     * offset which indicates a percentage of the line. From the offset to the end of the line
     * the base color will be applied.
     *
     * @param offset a value greater than 0 that represents a percentage of the line starting
     * from the beginning.
     * @param traveledColor the color to apply to the section of the line from the beginning
     * to the offset
     * @param lineBaseColor the color to apply to the section from the offset to the end
     *
     * @return an Expression for a gradient line
     */
    internal fun getVanishingRouteLineExpression(
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

    fun getRouteLineFeatureDataProvider(
        directionsRoutes: List<RouteLine>
    ): () -> List<RouteFeatureData> = {
        directionsRoutes.map(::generateFeatureCollection)
    }

    /**
     * Returns the color that is used to represent traffic congestion.
     *
     * @param congestionValue as string value coming from the DirectionsRoute
     * @param isPrimaryRoute indicates if the congestion value for the primary route should
     * be returned or the color for an alternative route.
     */
    @ColorInt
    fun getRouteColorForCongestion(
        congestionValue: String,
        isPrimaryRoute: Boolean,
        routeLineColorResources: RouteLineColorResources
    ): Int {
        return when (isPrimaryRoute) {
            true -> when (congestionValue) {
                RouteConstants.LOW_CONGESTION_VALUE -> {
                    routeLineColorResources.routeLowCongestionColor
                }
                RouteConstants.MODERATE_CONGESTION_VALUE -> {
                    routeLineColorResources.routeModerateColor
                }
                RouteConstants.HEAVY_CONGESTION_VALUE -> {
                    routeLineColorResources.routeHeavyColor
                }
                RouteConstants.SEVERE_CONGESTION_VALUE -> {
                    routeLineColorResources.routeSevereColor
                }
                RouteConstants.UNKNOWN_CONGESTION_VALUE -> {
                    routeLineColorResources.routeUnknownTrafficColor
                }
                else -> routeLineColorResources.routeDefaultColor
            }
            false -> when (congestionValue) {
                RouteConstants.LOW_CONGESTION_VALUE -> {
                    routeLineColorResources.alternativeRouteLowColor
                }
                RouteConstants.MODERATE_CONGESTION_VALUE -> {
                    routeLineColorResources.alternativeRouteModerateColor
                }
                RouteConstants.HEAVY_CONGESTION_VALUE -> {
                    routeLineColorResources.alternativeRouteHeavyColor
                }
                RouteConstants.SEVERE_CONGESTION_VALUE -> {
                    routeLineColorResources.alternativeRouteSevereColor
                }
                RouteConstants.UNKNOWN_CONGESTION_VALUE -> {
                    routeLineColorResources.alternativeRouteUnknownTrafficColor
                }
                else -> {
                    routeLineColorResources.alternativeRouteDefaultColor
                }
            }
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
     * @param trafficBackfillRoadClasses a collection of road classes for overriding the traffic
     * congestion color for unknown traffic conditions
     * @param isPrimaryRoute indicates if the route used is the primary route
     * @param routeLineColorResources provides color values for the route line
     *
     * @return a list of items representing the distance offset of each route leg and the color
     * used to represent the traffic congestion.
     */
    fun calculateRouteLineSegments(
        route: DirectionsRoute,
        trafficBackfillRoadClasses: List<String>,
        isPrimaryRoute: Boolean,
        routeLineColorResources: RouteLineColorResources
    ): List<RouteLineExpressionData> {
        val trafficExpressionData = getRouteLineTrafficExpressionData(route)
        return when (trafficExpressionData.isEmpty()) {
            false -> getRouteLineExpressionDataWithStreetClassOverride(
                trafficExpressionData,
                route.distance(),
                routeLineColorResources,
                isPrimaryRoute,
                trafficBackfillRoadClasses
            )
            true -> listOf(
                RouteLineExpressionData(
                    0.0,
                    getRouteColorForCongestion(
                        "",
                        isPrimaryRoute,
                        routeLineColorResources
                    )
                )
            )
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
    internal fun getRouteLineTrafficExpressionData(
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

    fun getRestrictedRouteSections(route: DirectionsRoute): List<List<Point>> {
        try {
            val coordinates = LineString.fromPolyline(
                route.geometry() ?: "",
                Constants.PRECISION_6
            ).coordinates()
            val restrictedSections = mutableListOf<List<Point>>()
            var geoIndex: Int? = null

            route.legs()
                ?.mapNotNull { it.steps() }
                ?.flatten()
                ?.mapNotNull { it.intersections() }
                ?.flatten()
                ?.forEach { stepIntersection ->
                    if (stepIntersection.classes()?.contains("restricted") == true) {
                        if (geoIndex == null) {
                            geoIndex = stepIntersection.geometryIndex()
                        }
                    } else {
                        if (geoIndex != null && stepIntersection.geometryIndex() != null) {
                            val section = coordinates.subList(
                                geoIndex!!,
                                stepIntersection.geometryIndex()!! + 1
                            )
                            restrictedSections.add(section)
                            geoIndex = null
                        }
                    }
                }
            return restrictedSections
        } catch (ex: Exception) {
            Log.e(
                TAG,
                "Failed to extract route restrictions. " +
                    "This could be caused by missing data in the DirectionsRoute",
                ex
            )
        }
        return listOf()
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
     * @param routeLineColorResources provides color values for the route line
     * @param isPrimaryRoute indicates if the route used is the primary route
     * @param trafficOverrideRoadClasses a collection of road classes for which a color
     * substitution should occur.
     */
    internal fun getRouteLineExpressionDataWithStreetClassOverride(
        trafficExpressionData: List<RouteLineTrafficExpressionData>,
        routeDistance: Double,
        routeLineColorResources: RouteLineColorResources,
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
                    RouteConstants.LOW_CONGESTION_VALUE
                } else {
                    trafficExpData.trafficCongestionIdentifier
                }

            val trafficColor = getRouteColorForCongestion(
                trafficIdentifier,
                isPrimaryRoute,
                routeLineColorResources
            )
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
     * Generates a FeatureCollection and LineString based on the @param route.
     * @param route the DirectionsRoute to used to derive the result
     *
     * @return a RouteFeatureData containing the original route and a FeatureCollection and
     * LineString
     */
    private fun generateFeatureCollection(route: DirectionsRoute): RouteFeatureData =
        generateFeatureCollection(route, null)

    /**
     * Generates a FeatureCollection and LineString based on the @param route.
     * @param route the DirectionsRoute to used to derive the result
     *
     * @return a RouteFeatureData containing the original route and a FeatureCollection and
     * LineString
     */
    private fun generateFeatureCollection(route: RouteLine): RouteFeatureData =
        generateFeatureCollection(route.route, route.identifier)

    internal fun calculateRouteGranularDistances(
        coordinates: List<Point>
    ): RouteLineGranularDistances? {
        return if (coordinates.isNotEmpty()) {
            calculateGranularDistances(coordinates)
        } else {
            null
        }
    }

    private fun calculateGranularDistances(points: List<Point>): RouteLineGranularDistances {
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

    private fun generateFeatureCollection(
        route: DirectionsRoute,
        identifier: String?
    ): RouteFeatureData {
        val routeGeometry = LineString.fromPolyline(
            route.geometry() ?: "",
            Constants.PRECISION_6
        )
        val randomId = UUID.randomUUID().toString()
        val routeFeature = when (identifier) {
            null -> Feature.fromGeometry(routeGeometry, null, randomId)
            else -> Feature.fromGeometry(routeGeometry, null, randomId).also {
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
     * Builds a [FeatureCollection] representing waypoints from a [DirectionsRoute]
     *
     * @param route the route to use for generating the waypoints [FeatureCollection]
     *
     * @return a [FeatureCollection] representing the waypoints derived from the [DirectionsRoute]
     */
    internal fun buildWayPointFeatureCollection(route: DirectionsRoute): FeatureCollection {
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
     * Builds a [Feature] representing a waypoint for use on a Mapbox [Map].
     *
     * @param leg the [RouteLeg] containing the waypoint info.
     * @param index a value of 0 indicates a property value of origin
     * will be added to the [Feature] else a value of destination will be used.
     *
     * @return a [Feature] representing the waypoint from the [RouteLeg]
     */
    private fun buildWayPointFeatureFromLeg(leg: RouteLeg, index: Int): Feature? {
        return leg.steps()?.get(index)?.maneuver()?.location()?.run {
            Feature.fromGeometry(Point.fromLngLat(this.longitude(), this.latitude()))
        }?.also {
            val propValue =
                if (index == 0) RouteConstants.WAYPOINT_ORIGIN_VALUE
                else RouteConstants.WAYPOINT_DESTINATION_VALUE
            it.addStringProperty(RouteConstants.WAYPOINT_PROPERTY_KEY, propValue)
        }
    }

    internal fun getLayerVisibility(style: Style, layerId: String): Visibility? {
        return if (style.isFullyLoaded()) {
            style.getLayer(layerId)?.visibility
        } else {
            null
        }
    }

    /**
     * Checks if a layer with the given ID exists else returns a default layer ID
     * @param belowLayerId the layer ID to look for
     * @param style the [Style] containing the layers
     *
     * @return either the layer ID if found else a default layer ID
     */
    @JvmStatic
    fun getBelowLayerIdToUse(belowLayerId: String?, style: Style): String? {
        return when (belowLayerId) {
            null -> belowLayerId
            else -> when (style.styleLayerExists(belowLayerId)) {
                true -> belowLayerId
                false -> {
                    Log.e(
                        TAG,
                        "Layer $belowLayerId not found. Route line related layers will be " +
                            "placed at top of the map stack."
                    )
                    null
                }
            }
        }
    }

    /**
     * Calculates the distance between 2 points using
     * [EPSG:3857 projection](https://epsg.io/3857).
     * Info in [mapbox-gl-js/issues/9998](https://github.com/mapbox/mapbox-gl-js/issues/9998).
     */
    internal fun calculateDistance(point1: Point, point2: Point): Double {
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
    internal fun findDistanceToNearestPointOnCurrentLine(
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

    internal fun buildScalingExpression(scalingValues: List<RouteLineScaleValue>): Expression {
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

    internal fun getRouteLineColorExpressions(
        defaultColor: Int,
        routeStyleDescriptors: List<RouteStyleDescriptor>,
        routeColorProvider: KProperty1<RouteStyleDescriptor, Int>
    ): List<Expression> {
        val expressions = mutableListOf<Expression>(
            eq {
                get { literal(RouteConstants.DEFAULT_ROUTE_DESCRIPTOR_PLACEHOLDER) }
                literal(true)
            },
            color(defaultColor)
        )
        routeStyleDescriptors.forEach {
            expressions.add(
                eq {
                    get { literal(it.routeIdentifier) }
                    literal(true)
                }
            )
            expressions.add(color(routeColorProvider.get(it)))
        }
        return expressions.plus(color(defaultColor))
    }

    internal fun initializeLayers(style: Style, options: MapboxRouteLineOptions) {
        if (!style.fullyLoaded || layersAreInitialized(style)) {
            return
        }

        val belowLayerIdToUse: String? =
            getBelowLayerIdToUse(
                options.routeLineBelowLayerId,
                style
            )

        if (!style.styleSourceExists(RouteConstants.WAYPOINT_SOURCE_ID)) {
            geoJsonSource(RouteConstants.WAYPOINT_SOURCE_ID) {
                maxzoom(16)
                featureCollection(FeatureCollection.fromFeatures(listOf()))
                tolerance(options.tolerance)
            }.bindTo(style)
        }

        if (!style.styleSourceExists(RouteConstants.PRIMARY_ROUTE_SOURCE_ID)) {
            geoJsonSource(RouteConstants.PRIMARY_ROUTE_SOURCE_ID) {
                maxzoom(16)
                lineMetrics(true)
                featureCollection(FeatureCollection.fromFeatures(listOf<Feature>()))
                tolerance(options.tolerance)
            }.bindTo(style)
        }

        if (!style.styleSourceExists(RouteConstants.ALTERNATIVE_ROUTE1_SOURCE_ID)) {
            geoJsonSource(RouteConstants.ALTERNATIVE_ROUTE1_SOURCE_ID) {
                maxzoom(16)
                lineMetrics(true)
                tolerance(options.tolerance)
                featureCollection(FeatureCollection.fromFeatures(listOf<Feature>()))
            }.bindTo(style)
        }

        if (!style.styleSourceExists(RouteConstants.ALTERNATIVE_ROUTE2_SOURCE_ID)) {
            geoJsonSource(RouteConstants.ALTERNATIVE_ROUTE2_SOURCE_ID) {
                maxzoom(16)
                lineMetrics(true)
                featureCollection(FeatureCollection.fromFeatures(listOf<Feature>()))
                tolerance(options.tolerance)
            }.bindTo(style)
        }

        if (options.enableRestrictedRoadLayer &&
            !style.styleSourceExists(RouteConstants.RESTRICTED_ROAD_SOURCE_ID)
        ) {
            geoJsonSource(RouteConstants.RESTRICTED_ROAD_SOURCE_ID) {
                maxzoom(16)
                lineMetrics(true)
                featureCollection(FeatureCollection.fromFeatures(listOf<Feature>()))
                tolerance(options.tolerance)
            }.bindTo(style)
        }

        options.routeLayerProvider.buildAlternativeRouteCasingLayers(
            style,
            options.resourceProvider.routeLineColorResources.alternativeRouteCasingColor
        ).forEach {
            it.bindTo(style, LayerPosition(null, belowLayerIdToUse, null))
        }

        options.routeLayerProvider.buildAlternativeRouteLayers(
            style,
            options.resourceProvider.roundedLineCap,
            options.resourceProvider.routeLineColorResources.alternativeRouteDefaultColor
        ).forEach {
            it.bindTo(style, LayerPosition(null, belowLayerIdToUse, null))
        }

        options.routeLayerProvider.buildAlternativeRouteTrafficLayers(
            style,
            options.resourceProvider.roundedLineCap,
            options.resourceProvider.routeLineColorResources.alternativeRouteDefaultColor
        ).forEach {
            it.bindTo(style, LayerPosition(null, belowLayerIdToUse, null))
        }

        options.routeLayerProvider.buildPrimaryRouteCasingLayer(
            style,
            options.resourceProvider.routeLineColorResources.routeCasingColor
        ).bindTo(style, LayerPosition(null, belowLayerIdToUse, null))

        options.routeLayerProvider.buildPrimaryRouteLayer(
            style,
            options.resourceProvider.roundedLineCap,
            options.resourceProvider.routeLineColorResources.routeDefaultColor
        ).bindTo(style, LayerPosition(null, belowLayerIdToUse, null))

        options.routeLayerProvider.buildPrimaryRouteTrafficLayer(
            style,
            options.resourceProvider.roundedLineCap,
            options.resourceProvider.routeLineColorResources.routeDefaultColor
        ).bindTo(style, LayerPosition(null, belowLayerIdToUse, null))

        options.routeLayerProvider.buildWayPointLayer(
            style,
            options.originIcon,
            options.destinationIcon
        ).bindTo(style, LayerPosition(null, belowLayerIdToUse, null))

        if (options.enableRestrictedRoadLayer) {
            options.routeLayerProvider.buildAccessRestrictionsLayer(
                options.resourceProvider.restrictedRoadDashArray,
                options.resourceProvider.restrictedRoadOpacity,
                options.resourceProvider.routeLineColorResources.restrictedRoadColor,
                options.resourceProvider.restrictedRoadLineWidth
            ).bindTo(style, LayerPosition(null, belowLayerIdToUse, null))
        }
    }

    internal fun layersAreInitialized(style: Style): Boolean {
        return style.fullyLoaded &&
            style.styleSourceExists(RouteConstants.PRIMARY_ROUTE_SOURCE_ID) &&
            style.styleSourceExists(RouteConstants.ALTERNATIVE_ROUTE1_SOURCE_ID) &&
            style.styleSourceExists(RouteConstants.ALTERNATIVE_ROUTE2_SOURCE_ID) &&
            style.styleLayerExists(RouteLayerConstants.PRIMARY_ROUTE_LAYER_ID) &&
            style.styleLayerExists(RouteLayerConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID) &&
            style.styleLayerExists(RouteLayerConstants.PRIMARY_ROUTE_CASING_LAYER_ID) &&
            style.styleLayerExists(RouteLayerConstants.ALTERNATIVE_ROUTE1_LAYER_ID) &&
            style.styleLayerExists(RouteLayerConstants.ALTERNATIVE_ROUTE2_LAYER_ID) &&
            style.styleLayerExists(RouteLayerConstants.ALTERNATIVE_ROUTE1_CASING_LAYER_ID) &&
            style.styleLayerExists(RouteLayerConstants.ALTERNATIVE_ROUTE2_CASING_LAYER_ID) &&
            style.styleLayerExists(RouteLayerConstants.ALTERNATIVE_ROUTE1_TRAFFIC_LAYER_ID) &&
            style.styleLayerExists(RouteLayerConstants.ALTERNATIVE_ROUTE2_TRAFFIC_LAYER_ID)
    }

    /**
     * Decodes the route geometry into nested arrays of legs -> steps -> points.
     *
     * The first and last point of adjacent steps overlap and are duplicated.
     */
    internal fun parseRoutePoints(
        route: DirectionsRoute,
    ): RoutePoints? {
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
