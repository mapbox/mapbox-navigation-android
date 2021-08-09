package com.mapbox.navigation.ui.maps.internal.route.line

import android.graphics.Color
import android.util.SparseArray
import androidx.annotation.ColorInt
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
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
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineExpressionProvider
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineGranularDistances
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineScaleValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineTrafficExpressionData
import com.mapbox.navigation.ui.maps.route.line.model.RoutePoints
import com.mapbox.navigation.ui.maps.route.line.model.RouteStyleDescriptor
import com.mapbox.navigation.ui.maps.util.CacheResultUtils.cacheResult
import com.mapbox.navigation.ui.utils.internal.ifNonNull
import com.mapbox.navigation.utils.internal.LoggerProvider
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
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
     * @param lineStartColor the starting color for the line gradient. This is usually transparent
     * and steps are added indicating a color change.
     * @param lineColor a default line color
     * @param routeLineExpressionData a collection of [RouteLineExpressionData]. Generally an
     * [Expression] step should be created for each item in the collection subject to some
     * filtering.
     *
     * @return the Expression that can be used in a Layer's properties.
     */
    internal fun getTrafficLineExpression(
        distanceOffset: Double,
        lineStartColor: Int,
        lineColor: Int,
        routeLineExpressionData: List<RouteLineExpressionData>
    ): Expression {
        var lastColor = Int.MAX_VALUE
        val expressionBuilder = Expression.ExpressionBuilder("step")
        expressionBuilder.lineProgress()
        expressionBuilder.color(lineStartColor)

        getFilteredRouteLineExpressionData(
            distanceOffset,
            routeLineExpressionData,
            lineColor
        ).forEach {
            // If the color hasn't changed there's no reason to add it to the expression. A smaller
            // expression is less work for the map to process.
            if (it.segmentColor != lastColor) {
                lastColor = it.segmentColor
                expressionBuilder.stop {
                    literal(it.offset)
                    color(it.segmentColor)
                }
            }
        }
        return expressionBuilder.build()
    }

    /**
     * Returns an [Expression] for a gradient line that will start with the @param lineBaseColor,
     * creating the first step at the @param distanceOffset with additional steps
     * according to the items in the @param routeLineExpressionData.
     *
     * If the @param activeLegIndex is greater than or equal to zero a color substitution will
     * take place in the expression if the underlying data's leg index parameter does not
     * equal the @param activeLegIndex. This was added for the feature allowing for alternate
     * styling of inactive route legs. To avoid this substitution altogether pass in an
     * activeLegIndex value less than zero.
     *
     * @param distanceOffset a distance value representing the first step in the [Expression]
     * @param routeLineExpressionData a collection of [RouteLineExpressionData] items
     * @param lineBaseColor the starting color for the line
     * @param defaultColor the default color for the line
     * @param substitutionColor the color to be used in lieu of the default color if active leg
     * index is greater than or equal to 0 and the leg index of a given [RouteLineExpressionData]
     * is not equal to the @param activeLegIndex.
     *
     * @return an [Expression] intended to be used as a gradient line
     */
    internal fun getRouteLineExpression(
        distanceOffset: Double,
        routeLineExpressionData: List<RouteLineExpressionData>,
        lineBaseColor: Int,
        defaultColor: Int,
        substitutionColor: Int,
        activeLegIndex: Int
    ): Expression {
        var lastColor = Int.MAX_VALUE
        val expressionBuilder = Expression.ExpressionBuilder("step")
        expressionBuilder.lineProgress()
        expressionBuilder.color(lineBaseColor)

        getFilteredRouteLineExpressionData(
            distanceOffset,
            routeLineExpressionData,
            defaultColor
        ).forEach {
            val colorToUse = if (activeLegIndex >= 0 && it.legIndex != activeLegIndex) {
                substitutionColor
            } else {
                defaultColor
            }

            // If the color hasn't changed there's no reason to add it to the expression. A smaller
            // expression is less work for the map to process.
            if (colorToUse != lastColor) {
                lastColor = colorToUse
                expressionBuilder.stop {
                    literal(it.offset)
                    color(colorToUse)
                }
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
    internal fun getRouteLineExpression(
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

    internal fun getFilteredRouteLineExpressionData(
        distanceOffset: Double,
        routeLineExpressionData: List<RouteLineExpressionData>,
        lineBaseColor: Int
    ): List<RouteLineExpressionData> {
        val filteredItems = routeLineExpressionData
            .filter { it.offset > distanceOffset }.distinctBy { it.offset }
        return when (filteredItems.isEmpty()) {
            true -> when (routeLineExpressionData.isEmpty()) {
                true -> listOf(RouteLineExpressionData(distanceOffset, lineBaseColor, 0))
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
        }
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
                RouteConstants.CLOSURE_CONGESTION_VALUE -> {
                    routeLineColorResources.routeClosureColor
                }
                RouteConstants.RESTRICTED_CONGESTION_VALUE -> {
                    routeLineColorResources.restrictedRoadColor
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
                RouteConstants.CLOSURE_CONGESTION_VALUE -> {
                    routeLineColorResources.alternativeRouteClosureColor
                }
                RouteConstants.RESTRICTED_CONGESTION_VALUE -> {
                    routeLineColorResources.alternativeRouteRestrictedRoadColor
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
     * @param route the [DirectionsRoute] used for the [Expression] calculations
     * @param trafficBackfillRoadClasses a collection of road classes for overriding the traffic
     * congestion color for unknown traffic conditions
     * @param isPrimaryRoute indicates if the route used is the primary route
     * @param routeLineColorResources provides color values for the route line
     * @param restrictedRoadSectionScale a scaling value for the dashed line representing
     * restricted road sections.
     * @param displayRestrictedRoadSections whether or not restricted road sections should
     * be indicated on the route line.
     *
     * @return a list of items representing the distance offset of each route leg and the color
     * used to represent the traffic congestion.
     */
    fun calculateRouteLineSegments(
        route: DirectionsRoute,
        trafficBackfillRoadClasses: List<String>,
        isPrimaryRoute: Boolean,
        routeLineColorResources: RouteLineColorResources,
        restrictedRoadSectionScale: Double,
        displayRestrictedRoadSections: Boolean
    ): List<RouteLineExpressionData> {
        val trafficExpressionData = getRouteLineTrafficExpressionDataFromCache(route)

        return when (trafficExpressionData.isEmpty()) {
            false -> {
                getRouteLineExpressionDataWithStreetClassOverride(
                    trafficExpressionData,
                    route.distance(),
                    routeLineColorResources,
                    isPrimaryRoute,
                    trafficBackfillRoadClasses,
                    restrictedRoadSectionScale,
                    displayRestrictedRoadSections
                )
            }
            true -> listOf(
                RouteLineExpressionData(
                    0.0,
                    getRouteColorForCongestion(
                        "",
                        isPrimaryRoute,
                        routeLineColorResources
                    ),
                    0
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

        route.legs()?.forEachIndexed { legIndex, leg ->
            ifNonNull(leg.annotation()?.distance()) { distanceList ->
                val closureRanges = getClosureRanges(leg).asSequence()
                val restrictedRanges = getRestrictedRouteLegRanges(leg).asSequence()
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
                    val isInAClosure = closureRanges.any { it.contains(index) }
                    val isInRestrictedRange = restrictedRanges.any { it.contains(index) }
                    val congestionValue: String = when {
                        isInAClosure -> RouteConstants.CLOSURE_CONGESTION_VALUE
                        else -> congestion
                    }
                    val roadClass = getRoadClassForIndex(roadClassArray, index)

                    if (index == 0) {
                        val distanceFromOrigin = if (legIndex > 0) {
                            runningDistance += distanceList[index]
                            runningDistance
                        } else {
                            0.0
                        }

                        val isLegOrigin = if (routeLineTrafficData.isEmpty()) {
                            true
                        } else legIndex != routeLineTrafficData.last().legIndex

                        routeLineTrafficData.add(
                            RouteLineTrafficExpressionData(
                                distanceFromOrigin,
                                congestionValue,
                                roadClass,
                                isInRestrictedRange,
                                legIndex,
                                isLegOrigin
                            )
                        )
                    } else {
                        // The value of distanceList[0] could be 0 for a leg index greater than
                        // 0. Such a condition would not increment the running distance and could
                        // result in two items in the collection returned to have duplicate values
                        // for the distanceFromOrigin. This would be an erroneous condition
                        // since this collection will be used to create a Maps Expression for
                        // a gradient line. The Expression requires strictly ascending values.
                        runningDistance += if (legIndex > 0 && distanceList[index - 1] == 0.0) {
                            val routeGeometry = decodeRoute(route)
                            TurfMeasurement.distance(
                                routeGeometry.coordinates()[routeLineTrafficData.lastIndex],
                                routeGeometry.coordinates()[routeLineTrafficData.lastIndex + 1]
                            )
                        } else {
                            distanceList[index - 1]
                        }

                        val last = routeLineTrafficData.lastOrNull()
                        if (last?.trafficCongestionIdentifier == congestionValue &&
                            last.roadClass == roadClass
                        ) {
                            // continue
                        } else if (last?.trafficCongestionIdentifier == congestionValue &&
                            roadClass == null
                        ) {
                            // continue
                        } else {
                            routeLineTrafficData.add(
                                RouteLineTrafficExpressionData(
                                    runningDistance,
                                    congestionValue,
                                    roadClass,
                                    isInRestrictedRange,
                                    legIndex
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

    private val getRouteLineTrafficExpressionDataFromCache: (route: DirectionsRoute) ->
    List<RouteLineTrafficExpressionData> = { route: DirectionsRoute ->
        getRouteLineTrafficExpressionData(route)
    }.cacheResult(3)

    private fun getClosureRanges(leg: RouteLeg): List<IntRange> {
        return leg.closures()
            ?.map {
                IntRange(it.geometryIndexStart(), it.geometryIndexEnd())
            } ?: listOf()
    }

    internal fun getRestrictedRouteLegRanges(leg: RouteLeg): List<IntRange> {
        var geoIndex: Int? = null
        val ranges = mutableListOf<IntRange>()
        leg.steps()
            ?.mapNotNull { it.intersections() }
            ?.flatten()
            ?.forEach { stepIntersection ->
                if (stepIntersection.classes()?.contains("restricted") == true) {
                    if (geoIndex == null) {
                        geoIndex = stepIntersection.geometryIndex()
                    }
                } else {
                    if (geoIndex != null && stepIntersection.geometryIndex() != null) {
                        ranges.add(IntRange(geoIndex!!, stepIntersection.geometryIndex()!!))
                        geoIndex = null
                    }
                }
            }
        return ranges
    }

    private tailrec fun getRoadClassForIndex(roadClassArray: Array<String?>, index: Int): String? {
        return if (roadClassArray.isNotEmpty() && roadClassArray.size > index && index >= 0) {
            roadClassArray[index] ?: getRoadClassForIndex(roadClassArray, index - 1)
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
        trafficOverrideRoadClasses: List<String>,
        restrictedRoadSectionScale: Double,
        displayRestrictedRoadSections: Boolean
    ): List<RouteLineExpressionData> {
        val expressionDataToReturn = mutableListOf<RouteLineExpressionData>()
        trafficExpressionData.forEachIndexed { index, trafficExpData ->
            val percentDistanceTraveled = trafficExpData.distanceFromOrigin / routeDistance
            val trafficIdentifier =
                if (
                    trafficExpData.trafficCongestionIdentifier ==
                    RouteConstants.UNKNOWN_CONGESTION_VALUE &&
                    trafficOverrideRoadClasses.contains(trafficExpData.roadClass)
                ) {
                    RouteConstants.LOW_CONGESTION_VALUE
                } else if (displayRestrictedRoadSections && trafficExpData.isInRestrictedSection) {
                    RouteConstants.RESTRICTED_CONGESTION_VALUE
                } else {
                    trafficExpData.trafficCongestionIdentifier
                }

            val trafficColor = getRouteColorForCongestion(
                trafficIdentifier,
                isPrimaryRoute,
                routeLineColorResources
            )

            if (index == 0 || trafficExpData.isLegOrigin) {
                expressionDataToReturn.add(
                    RouteLineExpressionData(
                        percentDistanceTraveled,
                        trafficColor,
                        trafficExpData.legIndex
                    )
                )
            } else if (trafficColor != expressionDataToReturn.last().segmentColor) {
                expressionDataToReturn.add(
                    RouteLineExpressionData(
                        percentDistanceTraveled,
                        trafficColor,
                        trafficExpData.legIndex
                    )
                )
            }

            if (trafficIdentifier == RouteConstants.RESTRICTED_CONGESTION_VALUE) {
                val hardStop = if (index < (trafficExpressionData.lastIndex)) {
                    trafficExpressionData[index + 1].distanceFromOrigin
                } else {
                    routeDistance
                }
                val restrictedSegments = getRestrictedSegments(
                    trafficExpData,
                    hardStop,
                    restrictedRoadSectionScale,
                    routeDistance,
                    trafficColor
                )
                expressionDataToReturn.addAll(restrictedSegments)
            }
        }
        return expressionDataToReturn
    }

    private fun getRestrictedSegments(
        trafficExpData: RouteLineTrafficExpressionData,
        hardStop: Double,
        restrictedRoadSectionScale: Double,
        routeDistance: Double,
        filledColorInt: Int
    ): List<RouteLineExpressionData> {
        val expressionDataToReturn = mutableListOf<RouteLineExpressionData>()
        var distOffset = trafficExpData.distanceFromOrigin + restrictedRoadSectionScale
        var nextColor = Color.TRANSPARENT

        while (distOffset < hardStop) {
            val sectionPercentDistance = distOffset / routeDistance
            expressionDataToReturn.add(
                RouteLineExpressionData(
                    sectionPercentDistance,
                    nextColor,
                    trafficExpData.legIndex
                )
            )

            distOffset += restrictedRoadSectionScale
            nextColor = if (nextColor == Color.TRANSPARENT) {
                filledColorInt
            } else {
                Color.TRANSPARENT
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

    private val generateFeatureCollection: (
        route: DirectionsRoute,
        identifier: String?
    ) -> RouteFeatureData = { route: DirectionsRoute, identifier: String? ->
        val routeGeometry = decodeRoute(route)
        val randomId = UUID.randomUUID().toString()
        val routeFeature = when (identifier) {
            null -> Feature.fromGeometry(routeGeometry, null, randomId)
            else -> Feature.fromGeometry(routeGeometry, null, randomId).also {
                it.addBooleanProperty(identifier, true)
            }
        }

        RouteFeatureData(
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
        return if (style.isStyleLoaded) {
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
                    LoggerProvider.logger.e(
                        Tag(TAG),
                        Message(
                            "Layer $belowLayerId not found. Route line related layers will be " +
                                "placed at top of the map stack."
                        )
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
        val expressions = mutableListOf(
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
        if (!style.isStyleLoaded || layersAreInitialized(style)) {
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
                tolerance(options.tolerance)
            }.featureCollection(FeatureCollection.fromFeatures(listOf())).bindTo(style)
        }

        if (!style.styleSourceExists(RouteConstants.PRIMARY_ROUTE_SOURCE_ID)) {
            geoJsonSource(RouteConstants.PRIMARY_ROUTE_SOURCE_ID) {
                maxzoom(16)
                lineMetrics(true)
                tolerance(options.tolerance)
            }.featureCollection(FeatureCollection.fromFeatures(listOf())).bindTo(style)
        }

        if (!style.styleSourceExists(RouteConstants.ALTERNATIVE_ROUTE1_SOURCE_ID)) {
            geoJsonSource(RouteConstants.ALTERNATIVE_ROUTE1_SOURCE_ID) {
                maxzoom(16)
                lineMetrics(true)
                tolerance(options.tolerance)
            }.featureCollection(FeatureCollection.fromFeatures(listOf())).bindTo(style)
        }

        if (!style.styleSourceExists(RouteConstants.ALTERNATIVE_ROUTE2_SOURCE_ID)) {
            geoJsonSource(RouteConstants.ALTERNATIVE_ROUTE2_SOURCE_ID) {
                maxzoom(16)
                lineMetrics(true)
                tolerance(options.tolerance)
            }.featureCollection(FeatureCollection.fromFeatures(listOf())).bindTo(style)
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
    }

    internal fun layersAreInitialized(style: Style): Boolean {
        return style.isStyleLoaded &&
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

    internal fun getTrafficLineExpressionProducer(
        route: DirectionsRoute,
        trafficBackfillRoadClasses: List<String>,
        colorResources: RouteLineColorResources,
        isPrimaryRoute: Boolean,
        vanishingPointOffset: Double,
        lineStartColor: Int,
        lineColor: Int,
        restrictedRoadSectionScale: Double
    ): RouteLineExpressionProvider = {
        val segments: List<RouteLineExpressionData> = calculateRouteLineSegments(
            route,
            trafficBackfillRoadClasses,
            isPrimaryRoute,
            colorResources,
            restrictedRoadSectionScale,
            false
        )
        getTrafficLineExpression(
            vanishingPointOffset,
            lineStartColor,
            lineColor,
            segments
        )
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

    private val decodeRoute: (DirectionsRoute) -> LineString = { route: DirectionsRoute ->
        val precision =
            if (route.routeOptions()?.geometries() == DirectionsCriteria.GEOMETRY_POLYLINE) {
                Constants.PRECISION_5
            } else {
                Constants.PRECISION_6
            }

        LineString.fromPolyline(
            route.geometry() ?: "",
            precision
        )
    }.cacheResult(3)
}
