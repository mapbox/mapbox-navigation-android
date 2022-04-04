package com.mapbox.navigation.ui.maps.internal.route.line

import android.graphics.Color
import android.util.LruCache
import android.util.SparseArray
import androidx.annotation.ColorInt
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.LayerPosition
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.dsl.generated.color
import com.mapbox.maps.extension.style.expressions.dsl.generated.eq
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.addPersistentLayer
import com.mapbox.maps.extension.style.layers.generated.BackgroundLayer
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.utils.DecodeUtils.completeGeometryToLineString
import com.mapbox.navigation.base.utils.DecodeUtils.stepGeometryToPoints
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants
import com.mapbox.navigation.ui.maps.route.line.model.ExtractedRouteData
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.NavigationRouteLine
import com.mapbox.navigation.ui.maps.route.line.model.RouteFeatureData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineDistancesIndex
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineExpressionData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineExpressionProvider
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineGranularDistances
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineScaleValue
import com.mapbox.navigation.ui.maps.route.line.model.RoutePoints
import com.mapbox.navigation.ui.maps.route.line.model.RouteStyleDescriptor
import com.mapbox.navigation.ui.maps.util.CacheResultUtils
import com.mapbox.navigation.ui.maps.util.CacheResultUtils.cacheResult
import com.mapbox.navigation.ui.utils.internal.ifNonNull
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMisc
import java.util.UUID
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.reflect.KProperty1

internal object MapboxRouteLineUtils {

    private const val LOG_CATEGORY = "MapboxRouteLineUtils"
    internal const val VANISH_POINT_STOP_GAP = .00000000001

    private val extractRouteDataCache: LruCache<
        CacheResultUtils.CacheResultKey2<
            DirectionsRoute, (RouteLeg) -> List<String>?,
            List<ExtractedRouteData>
            >,
        List<ExtractedRouteData>> by lazy { LruCache(3) }

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
     * A linear interpolated gradient will produce a color transition between the stops in the [Expression].
     * The greater the distance between the stops, the longer the gradient appears. For long route
     * lines this can mean a very long gradient between changes in traffic congestion. In order
     * to create a more compact gradient additional [Expression] stops are added. An additional
     * [Expression] stop is added just before a change in color in order to reduce the distance
     * between the stops and compact the gradient. The @param softGradientStopGap is used to
     * represent that distance and should be calculated based on the route distance in order
     * to create a similar looking gradient regardless of the route length.
     */
    internal fun getTrafficLineExpressionSoftGradient(
        distanceOffset: Double,
        lineStartColor: Int,
        lineColor: Int,
        softGradientStopGap: Double,
        routeLineExpressionData: List<RouteLineExpressionData>
    ): Expression {
        val vanishPointStopGap = VANISH_POINT_STOP_GAP
        val expressionBuilder = Expression.InterpolatorBuilder("interpolate")
        expressionBuilder.linear()
        expressionBuilder.lineProgress()

        val filteredItems = getFilteredRouteLineExpressionData(
            distanceOffset,
            routeLineExpressionData,
            lineColor
        )
        filteredItems.forEachIndexed { index, expressionData ->
            if (index == 0) {
                if (expressionData.offset > 0) {
                    expressionBuilder.stop {
                        literal(0.0)
                        color(lineStartColor)
                    }

                    if (expressionData.offset > vanishPointStopGap) {
                        expressionBuilder.stop {
                            literal(expressionData.offset - vanishPointStopGap)
                            color(lineStartColor)
                        }
                    }
                }

                expressionBuilder.stop {
                    literal(expressionData.offset)
                    color(expressionData.segmentColor)
                }
            } else {
                val stopGapOffset = expressionData.offset - softGradientStopGap
                val stopGapOffsetToUse = if (stopGapOffset > filteredItems[index - 1].offset) {
                    stopGapOffset
                } else {
                    filteredItems[index - 1].offset + vanishPointStopGap
                }

                expressionBuilder.stop {
                    literal(stopGapOffsetToUse)
                    color(filteredItems[index - 1].segmentColor)
                }

                expressionBuilder.stop {
                    literal(expressionData.offset)
                    color(expressionData.segmentColor)
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

    // todo this has a lot in common with the method above by the same name
    // find a way to reduce the code duplication
    internal fun getFilteredRouteLineExpressionData(
        distanceOffset: Double,
        routeLineExpressionData: List<ExtractedRouteData>
    ): List<ExtractedRouteData> {
        val filteredItems = routeLineExpressionData
            .filter { it.offset > distanceOffset }.distinctBy { it.offset }
        return when (filteredItems.isEmpty()) {
            true -> when (routeLineExpressionData.isEmpty()) {
                true -> listOf(
                    ExtractedRouteData(
                        -1.1,
                        distanceOffset,
                        trafficCongestionIdentifier = RouteLayerConstants.UNKNOWN_CONGESTION_VALUE
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
        }
    }

    fun getRouteFeatureDataProvider(
        directionsRoutes: List<NavigationRoute>
    ): () -> List<RouteFeatureData> = {
        directionsRoutes.map(::generateFeatureCollection)
    }

    fun getRouteLineFeatureDataProvider(
        directionsRoutes: List<NavigationRouteLine>
    ): () -> List<RouteFeatureData> = {
        directionsRoutes.map(::generateFeatureCollection)
    }

    internal fun resolveNumericToValue(
        congestionValue: Int?,
        routeLineColorResources: RouteLineColorResources
    ): String {
        return when (congestionValue) {
            in routeLineColorResources.lowCongestionRange -> {
                RouteLayerConstants.LOW_CONGESTION_VALUE
            }
            in routeLineColorResources.heavyCongestionRange -> {
                RouteLayerConstants.HEAVY_CONGESTION_VALUE
            }
            in routeLineColorResources.severeCongestionRange -> {
                RouteLayerConstants.SEVERE_CONGESTION_VALUE
            }
            in routeLineColorResources.moderateCongestionRange -> {
                RouteLayerConstants.MODERATE_CONGESTION_VALUE
            }
            else -> {
                RouteLayerConstants.UNKNOWN_CONGESTION_VALUE
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
    fun getRouteColorForCongestion(
        congestionValue: String,
        isPrimaryRoute: Boolean,
        routeLineColorResources: RouteLineColorResources
    ): Int {
        return when (isPrimaryRoute) {
            true -> when (congestionValue) {
                RouteLayerConstants.LOW_CONGESTION_VALUE -> {
                    routeLineColorResources.routeLowCongestionColor
                }
                RouteLayerConstants.MODERATE_CONGESTION_VALUE -> {
                    routeLineColorResources.routeModerateCongestionColor
                }
                RouteLayerConstants.HEAVY_CONGESTION_VALUE -> {
                    routeLineColorResources.routeHeavyCongestionColor
                }
                RouteLayerConstants.SEVERE_CONGESTION_VALUE -> {
                    routeLineColorResources.routeSevereCongestionColor
                }
                RouteLayerConstants.UNKNOWN_CONGESTION_VALUE -> {
                    routeLineColorResources.routeUnknownCongestionColor
                }
                RouteLayerConstants.CLOSURE_CONGESTION_VALUE -> {
                    routeLineColorResources.routeClosureColor
                }
                RouteLayerConstants.RESTRICTED_CONGESTION_VALUE -> {
                    routeLineColorResources.restrictedRoadColor
                }
                else -> routeLineColorResources.routeDefaultColor
            }
            false -> when (congestionValue) {
                RouteLayerConstants.LOW_CONGESTION_VALUE -> {
                    routeLineColorResources.alternativeRouteLowCongestionColor
                }
                RouteLayerConstants.MODERATE_CONGESTION_VALUE -> {
                    routeLineColorResources.alternativeRouteModerateCongestionColor
                }
                RouteLayerConstants.HEAVY_CONGESTION_VALUE -> {
                    routeLineColorResources.alternativeRouteHeavyCongestionColor
                }
                RouteLayerConstants.SEVERE_CONGESTION_VALUE -> {
                    routeLineColorResources.alternativeRouteSevereCongestionColor
                }
                RouteLayerConstants.UNKNOWN_CONGESTION_VALUE -> {
                    routeLineColorResources.alternativeRouteUnknownCongestionColor
                }
                RouteLayerConstants.CLOSURE_CONGESTION_VALUE -> {
                    routeLineColorResources.alternativeRouteClosureColor
                }
                RouteLayerConstants.RESTRICTED_CONGESTION_VALUE -> {
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
     * @return a list of items representing the distance offset of each route leg and the color
     * used to represent the traffic congestion.
     */
    fun calculateRouteLineSegments(
        route: DirectionsRoute,
        trafficBackfillRoadClasses: List<String>,
        isPrimaryRoute: Boolean,
        routeLineColorResources: RouteLineColorResources,
    ): List<RouteLineExpressionData> {
        val congestionProvider =
            getTrafficCongestionAnnotationProvider(route, routeLineColorResources)
        val annotationExpressionData = extractRouteDataWithTrafficAndRoadClassDeDuped(
            route,
            congestionProvider
        )

        return when (annotationExpressionData.isEmpty()) {
            false -> {
                getRouteLineExpressionDataWithStreetClassOverride(
                    annotationExpressionData,
                    route.distance(),
                    routeLineColorResources,
                    isPrimaryRoute,
                    trafficBackfillRoadClasses
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
     * Extracts data from the [DirectionsRoute] and removes items that are deemed duplicates based
     * on factors such as traffic congestion and/or road class. The results are cached for
     * performance reasons.
     */
    internal val extractRouteDataWithTrafficAndRoadClassDeDuped: (
        route: DirectionsRoute,
        trafficCongestionProvider: (RouteLeg) -> List<String>?
    ) -> List<ExtractedRouteData> =
        { route: DirectionsRoute, trafficCongestionProvider: (RouteLeg) -> List<String>? ->
            val extractedRouteDataItems = extractRouteData(
                route,
                trafficCongestionProvider
            )
            extractedRouteDataItems.filterIndexed { index, extractedRouteData ->
                when {
                    index == 0 -> true
                    extractedRouteDataItems[index].isLegOrigin -> true
                    extractedRouteDataItems[index - 1].trafficCongestionIdentifier ==
                        extractedRouteData.trafficCongestionIdentifier &&
                        extractedRouteDataItems[index - 1].roadClass ==
                        extractedRouteData.roadClass -> false
                    extractedRouteDataItems[index - 1].trafficCongestionIdentifier ==
                        extractedRouteData.trafficCongestionIdentifier &&
                        extractedRouteData.roadClass == null -> false
                    else -> true
                }
            }
        }.cacheResult(extractRouteDataCache)

    /**
     * Extracts data from the [DirectionsRoute] in a format more useful to the route line
     * API. The data returned here is used by several different calculations. The results
     * are cached for performance reasons.
     */
    internal val extractRouteData: (
        route: DirectionsRoute,
        trafficCongestionProvider: (RouteLeg) -> List<String>?
    ) -> List<ExtractedRouteData> =
        { route: DirectionsRoute, trafficCongestionProvider: (RouteLeg) -> List<String>? ->
            var runningDistance = 0.0
            val itemsToReturn = mutableListOf<ExtractedRouteData>()
            route.legs()?.forEachIndexed { legIndex, leg ->
                val restrictedRanges = getRestrictedRouteLegRanges(leg).asSequence()
                val closureRanges = getClosureRanges(leg).asSequence()
                val roadClassArray = getRoadClassArray(leg.steps())
                val trafficCongestion = trafficCongestionProvider.invoke(leg)
                var isLegOrigin = true

                leg.annotation()?.distance()?.forEachIndexed { index, distance ->
                    // If the distance is 0 it offers no value to upstream calculations and in fact
                    // causes problems in creating the traffic expression since the expression
                    // values need to be in strictly ascending order. A value of 0 can be caused
                    // by the first point in a route leg being the same as the last point in the
                    // previous route leg. There may be other causes as well.
                    if (distance > 0.0) {
                        val percentDistanceTraveled = runningDistance / route.distance()
                        val isInRestrictedRange = restrictedRanges.any { it.contains(index) }
                        val isInAClosure = closureRanges.any { it.contains(index) }
                        val congestionValue: String = when {
                            isInAClosure -> RouteLayerConstants.CLOSURE_CONGESTION_VALUE
                            trafficCongestion.isNullOrEmpty() ->
                                RouteLayerConstants.UNKNOWN_CONGESTION_VALUE
                            index >= trafficCongestion.size ->
                                RouteLayerConstants.UNKNOWN_CONGESTION_VALUE
                            else -> trafficCongestion[index]
                        }
                        val roadClass = getRoadClassForIndex(roadClassArray, index)

                        itemsToReturn.add(
                            ExtractedRouteData(
                                runningDistance,
                                percentDistanceTraveled,
                                isInRestrictedRange,
                                congestionValue,
                                roadClass,
                                legIndex,
                                isLegOrigin
                            )
                        )
                        isLegOrigin = false
                        runningDistance += distance
                    }
                }
            }

            itemsToReturn
        }.cacheResult(extractRouteDataCache)

    internal val getRouteLegTrafficNumericCongestionProvider: (
        routeLineColorResources: RouteLineColorResources
    ) -> (RouteLeg) -> List<String> = { routeLineColorResources: RouteLineColorResources ->
        { routeLeg: RouteLeg ->
            routeLeg.annotation()?.congestionNumeric()?.map { v ->
                resolveNumericToValue(v, routeLineColorResources)
            } ?: listOf()
        }
    }.cacheResult(1)

    internal val getRouteLegTrafficCongestionProvider: (RouteLeg) -> List<String> =
        { routeLeg: RouteLeg ->
            routeLeg.annotation()?.congestion() ?: listOf()
        }.cacheResult(1)

    internal fun getTrafficCongestionAnnotationProvider(
        route: DirectionsRoute,
        routeLineColorResources: RouteLineColorResources
    ): (RouteLeg) -> List<String>? {
        return if (
            route.routeOptions()
                ?.annotationsList()
                ?.contains(DirectionsCriteria.ANNOTATION_CONGESTION_NUMERIC) == true
        ) {
            getRouteLegTrafficNumericCongestionProvider(routeLineColorResources)
        } else {
            getRouteLegTrafficCongestionProvider
        }
    }

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
                    ifNonNull(
                        geoIndex,
                        stepIntersection.geometryIndex()
                    ) { startGeoIndex, intersectionGeoIndex ->
                        ranges.add(IntRange(startGeoIndex, intersectionGeoIndex - 1))
                        geoIndex = null
                    }
                }
            }
        return ranges
    }

    private fun getRoadClassArray(
        steps: List<LegStep>?
    ): Array<String?> {
        val intersectionsWithGeometryIndex = steps
            ?.mapNotNull { it.intersections() }
            ?.flatten()
            ?.filter {
                it.geometryIndex() != null
            }?.toList() ?: listOf()

        return when (intersectionsWithGeometryIndex.isNotEmpty()) {
            true -> {
                arrayOfNulls<String>(
                    intersectionsWithGeometryIndex.last().geometryIndex()!! + 1
                ).apply {
                    intersectionsWithGeometryIndex.forEach {
                        val roadClass: String =
                            it.mapboxStreetsV8()?.roadClass()
                                ?: "intersection_without_class_fallback"
                        if (it.geometryIndex()!! < this.size) {
                            this[it.geometryIndex()!!] = roadClass
                        } else {
                            logE(
                                "Geometry index for step intersection unexpected or " +
                                    "incorrect. There is a risk of incorrect " +
                                    "road class styling applied to the route line.",
                                LOG_CATEGORY
                            )
                        }
                    }
                }
            }
            false -> {
                arrayOfNulls(0)
            }
        }
    }

    private tailrec fun getRoadClassForIndex(roadClassArray: Array<String?>, index: Int): String? {
        return if (roadClassArray.isNotEmpty() && roadClassArray.size > index && index >= 0) {
            roadClassArray[index] ?: getRoadClassForIndex(roadClassArray, index - 1)
        } else {
            null
        }
    }

    /**
     * For each item in the annotationExpressionData collection a color substitution will take
     * place that has a road class contained in the trafficOverrideRoadClasses
     * collection. For each of these items the color for 'unknown' traffic congestion
     * will be replaced with the color for 'low' traffic congestion. In addition the
     * percentage of the route distance traveled will be calculated for each item in the
     * annotationExpressionData collection and included in the returned data.
     *
     * @param annotationExpressionData the route data to perform the substitution on
     * @param routeDistance the total distance of the route
     * @param routeLineColorResources provides color values for the route line
     * @param isPrimaryRoute indicates if the route used is the primary route
     * @param trafficOverrideRoadClasses a collection of road classes for which a color
     * substitution should occur.
     */
    internal fun getRouteLineExpressionDataWithStreetClassOverride(
        annotationExpressionData: List<ExtractedRouteData>,
        routeDistance: Double,
        routeLineColorResources: RouteLineColorResources,
        isPrimaryRoute: Boolean,
        trafficOverrideRoadClasses: List<String>
    ): List<RouteLineExpressionData> {
        val expressionDataToReturn = mutableListOf<RouteLineExpressionData>()
        annotationExpressionData.forEachIndexed { index, annotationExpData ->
            val percentDistanceTraveled = annotationExpData.distanceFromOrigin / routeDistance
            val trafficIdentifier =
                if (
                    annotationExpData.trafficCongestionIdentifier ==
                    RouteLayerConstants.UNKNOWN_CONGESTION_VALUE &&
                    trafficOverrideRoadClasses.contains(annotationExpData.roadClass)
                ) {
                    RouteLayerConstants.LOW_CONGESTION_VALUE
                } else {
                    annotationExpData.trafficCongestionIdentifier
                }

            val trafficColor = getRouteColorForCongestion(
                trafficIdentifier,
                isPrimaryRoute,
                routeLineColorResources
            )

            if (index == 0 || annotationExpData.isLegOrigin) {
                expressionDataToReturn.add(
                    RouteLineExpressionData(
                        percentDistanceTraveled,
                        trafficColor,
                        annotationExpData.legIndex
                    )
                )
            } else if (trafficColor != expressionDataToReturn.last().segmentColor) {
                expressionDataToReturn.add(
                    RouteLineExpressionData(
                        percentDistanceTraveled,
                        trafficColor,
                        annotationExpData.legIndex
                    )
                )
            }
        }
        return expressionDataToReturn
    }

    /**
     * Generates a FeatureCollection and LineString based on the @param route.
     * @param route the [NavigationRoute] to used to derive the result
     *
     * @return a RouteFeatureData containing the original route and a FeatureCollection and
     * LineString
     */
    private fun generateFeatureCollection(route: NavigationRoute): RouteFeatureData =
        generateFeatureCollection(route, null)

    /**
     * Generates a FeatureCollection and LineString based on the @param route.
     * @param route the [NavigationRouteLine] to used to derive the result
     *
     * @return a RouteFeatureData containing the original route and a FeatureCollection and
     * LineString
     */
    private fun generateFeatureCollection(route: NavigationRouteLine): RouteFeatureData =
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
        route: NavigationRoute,
        identifier: String?
    ) -> RouteFeatureData = { route: NavigationRoute, identifier: String? ->
        val routeGeometry = route.directionsRoute.completeGeometryToLineString()
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
     * Builds a [FeatureCollection] representing waypoints from a [NavigationRoute]
     *
     * @param route the route to use for generating the waypoints [FeatureCollection]
     *
     * @return a [FeatureCollection] representing the waypoints derived from the [NavigationRoute]
     */
    internal fun buildWayPointFeatureCollection(route: NavigationRoute): FeatureCollection {
        val waypointFeatures = route.directionsResponse.waypoints()?.mapIndexed { index, waypoint ->
            waypoint.location().let {
                Feature.fromGeometry(it).apply {
                    val propValue = if (index == 0) {
                        RouteLayerConstants.WAYPOINT_ORIGIN_VALUE
                    } else {
                        RouteLayerConstants.WAYPOINT_DESTINATION_VALUE
                    }
                    addStringProperty(RouteLayerConstants.WAYPOINT_PROPERTY_KEY, propValue)
                }
            }
        } ?: emptyList()
        return FeatureCollection.fromFeatures(waypointFeatures)
    }

    internal fun getLayerVisibility(style: Style, layerId: String): Visibility? {
        return style.getLayer(layerId)?.visibility
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
                    logE(
                        "Layer $belowLayerId not found. Route line related layers will be " +
                            "placed at top of the map stack.",
                        LOG_CATEGORY
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
                get { literal(RouteLayerConstants.DEFAULT_ROUTE_DESCRIPTOR_PLACEHOLDER) }
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

    @OptIn(MapboxExperimental::class)
    internal fun initializeLayers(style: Style, options: MapboxRouteLineOptions) {
        if (layersAreInitialized(style, options)) {
            return
        }
        val belowLayerIdToUse: String? =
            getBelowLayerIdToUse(
                options.routeLineBelowLayerId,
                style
            )

        if (!style.styleSourceExists(RouteLayerConstants.WAYPOINT_SOURCE_ID)) {
            geoJsonSource(RouteLayerConstants.WAYPOINT_SOURCE_ID) {
                maxzoom(16)
                tolerance(options.tolerance)
            }.bindTo(style)
        }

        if (!style.styleSourceExists(RouteLayerConstants.PRIMARY_ROUTE_SOURCE_ID)) {
            geoJsonSource(RouteLayerConstants.PRIMARY_ROUTE_SOURCE_ID) {
                maxzoom(16)
                lineMetrics(true)
                tolerance(options.tolerance)
            }.bindTo(style)
        }

        if (!style.styleSourceExists(RouteLayerConstants.ALTERNATIVE_ROUTE1_SOURCE_ID)) {
            geoJsonSource(RouteLayerConstants.ALTERNATIVE_ROUTE1_SOURCE_ID) {
                maxzoom(16)
                lineMetrics(true)
                tolerance(options.tolerance)
            }.bindTo(style)
        }

        if (!style.styleSourceExists(RouteLayerConstants.ALTERNATIVE_ROUTE2_SOURCE_ID)) {
            geoJsonSource(RouteLayerConstants.ALTERNATIVE_ROUTE2_SOURCE_ID) {
                maxzoom(16)
                lineMetrics(true)
                tolerance(options.tolerance)
            }.bindTo(style)
        }

        style.addPersistentLayer(
            BackgroundLayer(
                RouteLayerConstants.BOTTOM_LEVEL_ROUTE_LINE_LAYER_ID,
            ).apply { this.backgroundOpacity(0.0) },
            LayerPosition(null, belowLayerIdToUse, null)
        )

        options.routeLayerProvider.buildAlternativeRouteCasingLayers(
            style,
            options.resourceProvider.routeLineColorResources.alternativeRouteCasingColor
        ).forEach {
            style.addPersistentLayer(it, LayerPosition(null, belowLayerIdToUse, null))
        }

        options.routeLayerProvider.buildAlternativeRouteLayers(
            style,
            options.resourceProvider.roundedLineCap,
            options.resourceProvider.routeLineColorResources.alternativeRouteDefaultColor
        ).forEach {
            style.addPersistentLayer(it, LayerPosition(null, belowLayerIdToUse, null))
        }

        options.routeLayerProvider.buildAlternativeRouteTrafficLayers(
            style,
            options.resourceProvider.roundedLineCap,
            options.resourceProvider.routeLineColorResources.alternativeRouteDefaultColor
        ).forEach {
            style.addPersistentLayer(it, LayerPosition(null, belowLayerIdToUse, null))
        }

        options.routeLayerProvider.buildPrimaryRouteCasingTrailLayer(
            style,
            options.resourceProvider.routeLineColorResources.routeLineTraveledCasingColor
        ).let {
            style.addPersistentLayer(it, LayerPosition(null, belowLayerIdToUse, null))
        }

        options.routeLayerProvider.buildPrimaryRouteTrailLayer(
            style,
            options.resourceProvider.roundedLineCap,
            options.resourceProvider.routeLineColorResources.routeLineTraveledColor
        ).let {
            style.addPersistentLayer(it, LayerPosition(null, belowLayerIdToUse, null))
        }

        options.routeLayerProvider.buildPrimaryRouteCasingLayer(
            style,
            options.resourceProvider.routeLineColorResources.routeCasingColor
        ).let {
            style.addPersistentLayer(it, LayerPosition(null, belowLayerIdToUse, null))
        }

        options.routeLayerProvider.buildPrimaryRouteLayer(
            style,
            options.resourceProvider.roundedLineCap,
            options.resourceProvider.routeLineColorResources.routeDefaultColor
        ).let {
            style.addPersistentLayer(it, LayerPosition(null, belowLayerIdToUse, null))
        }

        options.routeLayerProvider.buildPrimaryRouteTrafficLayer(
            style,
            options.resourceProvider.roundedLineCap,
            options.resourceProvider.routeLineColorResources.routeDefaultColor
        ).let {
            style.addPersistentLayer(it, LayerPosition(null, belowLayerIdToUse, null))
        }

        if (options.displayRestrictedRoadSections) {
            options.routeLayerProvider.buildAccessRestrictionsLayer(
                options.resourceProvider.restrictedRoadDashArray,
                options.resourceProvider.restrictedRoadOpacity,
                options.resourceProvider.routeLineColorResources.restrictedRoadColor,
                options.resourceProvider.restrictedRoadLineWidth
            ).let {
                style.addPersistentLayer(it, LayerPosition(null, belowLayerIdToUse, null))
            }
        }

        style.addPersistentLayer(
            BackgroundLayer(
                RouteLayerConstants.TOP_LEVEL_ROUTE_LINE_LAYER_ID
            ).apply { this.backgroundOpacity(0.0) },
            LayerPosition(null, belowLayerIdToUse, null)
        )

        options.routeLayerProvider.buildWayPointLayer(
            style,
            options.originIcon,
            options.destinationIcon,
            options.waypointLayerIconOffset,
            options.waypointLayerIconAnchor,
            options.iconPitchAlignment
        ).let {
            style.addPersistentLayer(it, LayerPosition(null, belowLayerIdToUse, null))
        }
    }

    internal fun layersAreInitialized(style: Style, options: MapboxRouteLineOptions): Boolean {
        return style.styleSourceExists(RouteLayerConstants.PRIMARY_ROUTE_SOURCE_ID) &&
            style.styleSourceExists(RouteLayerConstants.ALTERNATIVE_ROUTE1_SOURCE_ID) &&
            style.styleSourceExists(RouteLayerConstants.ALTERNATIVE_ROUTE2_SOURCE_ID) &&
            style.styleLayerExists(RouteLayerConstants.PRIMARY_ROUTE_LAYER_ID) &&
            style.styleLayerExists(RouteLayerConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID) &&
            style.styleLayerExists(RouteLayerConstants.PRIMARY_ROUTE_CASING_LAYER_ID) &&
            style.styleLayerExists(RouteLayerConstants.ALTERNATIVE_ROUTE1_LAYER_ID) &&
            style.styleLayerExists(RouteLayerConstants.ALTERNATIVE_ROUTE2_LAYER_ID) &&
            style.styleLayerExists(RouteLayerConstants.ALTERNATIVE_ROUTE1_CASING_LAYER_ID) &&
            style.styleLayerExists(RouteLayerConstants.ALTERNATIVE_ROUTE2_CASING_LAYER_ID) &&
            style.styleLayerExists(RouteLayerConstants.ALTERNATIVE_ROUTE1_TRAFFIC_LAYER_ID) &&
            style.styleLayerExists(RouteLayerConstants.ALTERNATIVE_ROUTE2_TRAFFIC_LAYER_ID) &&
            style.styleLayerExists(RouteLayerConstants.TOP_LEVEL_ROUTE_LINE_LAYER_ID) &&
            if (options.displayRestrictedRoadSections) {
                style.styleLayerExists(RouteLayerConstants.RESTRICTED_ROAD_LAYER_ID)
            } else {
                true
            }
    }

    /**
     * Decodes the route geometry into nested arrays of legs -> steps -> points.
     *
     * The first and last point of adjacent steps overlap and are duplicated.
     */
    internal fun parseRoutePoints(
        route: DirectionsRoute,
    ): RoutePoints? {
        val nestedList = route.legs()?.map { routeLeg ->
            routeLeg.steps()?.map { legStep ->
                legStep.geometry() ?: return null
                route.stepGeometryToPoints(legStep)
            } ?: return null
        } ?: return null

        val flatList = nestedList.flatten().flatten()

        return RoutePoints(nestedList, flatList)
    }

    internal fun getTrafficLineExpressionProducer(
        route: DirectionsRoute,
        colorResources: RouteLineColorResources,
        trafficBackfillRoadClasses: List<String>,
        isPrimaryRoute: Boolean,
        vanishingPointOffset: Double,
        lineStartColor: Int,
        lineColor: Int,
        useSoftGradient: Boolean,
        softGradientTransitionDistance: Double
    ) = RouteLineExpressionProvider {
        val segments: List<RouteLineExpressionData> = calculateRouteLineSegments(
            route,
            trafficBackfillRoadClasses,
            isPrimaryRoute,
            colorResources
        )
        if (useSoftGradient) {
            val stopGap = softGradientTransitionDistance / route.distance()
            getTrafficLineExpressionSoftGradient(
                vanishingPointOffset,
                lineStartColor,
                lineColor,
                stopGap,
                segments
            )
        } else {
            getTrafficLineExpression(
                vanishingPointOffset,
                lineStartColor,
                lineColor,
                segments
            )
        }
    }

    internal fun getRestrictedLineExpressionProducer(
        route: DirectionsRoute,
        vanishingPointOffset: Double,
        activeLegIndex: Int,
        routeLineColorResources: RouteLineColorResources
    ) = RouteLineExpressionProvider {
        val expData = extractRouteData(
            route,
            getTrafficCongestionAnnotationProvider(route, routeLineColorResources)
        )

        getRestrictedLineExpression(
            vanishingPointOffset,
            activeLegIndex,
            routeLineColorResources.restrictedRoadColor,
            expData
        )
    }

    internal fun getDisabledRestrictedLineExpressionProducer(
        vanishingPointOffset: Double,
        activeLegIndex: Int,
        restrictedSectionColor: Int,
    ) = RouteLineExpressionProvider {
        getRestrictedLineExpression(
            vanishingPointOffset,
            activeLegIndex,
            restrictedSectionColor,
            listOf()
        )
    }

    internal fun getRestrictedLineExpression(
        vanishingPointOffset: Double,
        activeLegIndex: Int,
        restrictedSectionColor: Int,
        routeLineExpressionData: List<ExtractedRouteData>
    ): Expression {
        var lastColor = Int.MAX_VALUE
        val expressionBuilder = Expression.ExpressionBuilder("step")
        expressionBuilder.lineProgress()
        expressionBuilder.color(Color.TRANSPARENT)

        getFilteredRouteLineExpressionData(
            vanishingPointOffset,
            routeLineExpressionData
        ).forEach {
            val colorToUse = if (activeLegIndex >= 0 && it.legIndex != activeLegIndex) {
                Color.TRANSPARENT
            } else if (it.isInRestrictedSection) {
                restrictedSectionColor
            } else {
                Color.TRANSPARENT
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

    internal fun routeHasRestrictions(route: DirectionsRoute): Boolean {
        return route.legs()?.asSequence()?.flatMap { routeLeg ->
            routeLeg.steps()?.asSequence() ?: sequenceOf()
        }?.flatMap { legStep ->
            legStep.intersections()?.asSequence() ?: sequenceOf()
        }?.any { stepIntersection ->
            stepIntersection.classes()?.contains("restricted") ?: false
        } ?: false
    }

    internal fun resetCache() {
        synchronized(extractRouteDataCache) {
            extractRouteDataCache.evictAll()
        }
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
