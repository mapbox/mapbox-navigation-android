package com.mapbox.navigation.ui.maps.internal.route.line

import android.graphics.Color
import android.graphics.drawable.Drawable
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
import com.mapbox.maps.Style
import com.mapbox.maps.StyleObjectInfo
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.extension.style.expressions.dsl.generated.match
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.addPersistentLayer
import com.mapbox.maps.extension.style.layers.generated.BackgroundLayer
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.extension.style.layers.generated.SymbolLayer
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.extension.style.layers.properties.generated.IconPitchAlignment
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.utils.DecodeUtils.completeGeometryToLineString
import com.mapbox.navigation.base.utils.DecodeUtils.stepGeometryToPoints
import com.mapbox.navigation.core.routealternatives.AlternativeRouteMetadata
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
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineSourceKey
import com.mapbox.navigation.ui.maps.route.line.model.RoutePoints
import com.mapbox.navigation.ui.maps.route.line.model.RouteStyleDescriptor
import com.mapbox.navigation.ui.maps.util.CacheResultUtils
import com.mapbox.navigation.ui.maps.util.CacheResultUtils.cacheResult
import com.mapbox.navigation.ui.utils.internal.extensions.getBitmap
import com.mapbox.navigation.ui.utils.internal.ifNonNull
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMisc
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

internal object MapboxRouteLineUtils {

    private const val LOG_CATEGORY = "MapboxRouteLineUtils"
    internal const val VANISH_POINT_STOP_GAP = .00000000001

    private val extractRouteDataCache: LruCache<
        CacheResultUtils.CacheResultKey2<
            DirectionsRoute, (RouteLeg) -> List<String>?,
            List<ExtractedRouteData>
            >,
        List<ExtractedRouteData>> by lazy { LruCache(3) }

    val layerGroup1SourceKey = RouteLineSourceKey(RouteLayerConstants.LAYER_GROUP_1_SOURCE_ID)
    val layerGroup2SourceKey = RouteLineSourceKey(RouteLayerConstants.LAYER_GROUP_2_SOURCE_ID)
    val layerGroup3SourceKey = RouteLineSourceKey(RouteLayerConstants.LAYER_GROUP_3_SOURCE_ID)

    val layerGroup1SourceLayerIds = setOf(
        RouteLayerConstants.LAYER_GROUP_1_TRAIL_CASING,
        RouteLayerConstants.LAYER_GROUP_1_TRAIL,
        RouteLayerConstants.LAYER_GROUP_1_CASING,
        RouteLayerConstants.LAYER_GROUP_1_MAIN,
        RouteLayerConstants.LAYER_GROUP_1_TRAFFIC,
        RouteLayerConstants.LAYER_GROUP_1_RESTRICTED
    )
    val layerGroup2SourceLayerIds = setOf(
        RouteLayerConstants.LAYER_GROUP_2_TRAIL_CASING,
        RouteLayerConstants.LAYER_GROUP_2_TRAIL,
        RouteLayerConstants.LAYER_GROUP_2_CASING,
        RouteLayerConstants.LAYER_GROUP_2_MAIN,
        RouteLayerConstants.LAYER_GROUP_2_TRAFFIC,
        RouteLayerConstants.LAYER_GROUP_2_RESTRICTED
    )
    val layerGroup3SourceLayerIds = setOf(
        RouteLayerConstants.LAYER_GROUP_3_TRAIL_CASING,
        RouteLayerConstants.LAYER_GROUP_3_TRAIL,
        RouteLayerConstants.LAYER_GROUP_3_CASING,
        RouteLayerConstants.LAYER_GROUP_3_MAIN,
        RouteLayerConstants.LAYER_GROUP_3_TRAFFIC,
        RouteLayerConstants.LAYER_GROUP_3_RESTRICTED
    )

    val sourceLayerMap = mapOf<RouteLineSourceKey, Set<String>>(
        Pair(layerGroup1SourceKey, layerGroup1SourceLayerIds),
        Pair(layerGroup2SourceKey, layerGroup2SourceLayerIds),
        Pair(layerGroup3SourceKey, layerGroup3SourceLayerIds)
    )

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
        generateRouteFeatureData(route, null)

    /**
     * Generates a FeatureCollection and LineString based on the @param route.
     * @param route the [NavigationRouteLine] to used to derive the result
     *
     * @return a RouteFeatureData containing the original route and a FeatureCollection and
     * LineString
     */
    private fun generateFeatureCollection(route: NavigationRouteLine): RouteFeatureData =
        generateRouteFeatureData(route.route, route.identifier)

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

    private val generateRouteFeatureData: (
        route: NavigationRoute,
        identifier: String?
    ) -> RouteFeatureData = { route: NavigationRoute, identifier: String? ->
        val routeGeometry = route.directionsRoute.completeGeometryToLineString()
        val routeFeature = when (identifier) {
            null -> Feature.fromGeometry(routeGeometry, null, route.id)
            else -> {
                Feature.fromGeometry(routeGeometry, null, route.id).also {
                    it.addBooleanProperty(identifier, true)
                }
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

    private fun addSource(
        style: Style,
        layerSource: String,
        tolerance: Double,
        useLineMetrics: Boolean
    ) {
        if (!style.styleSourceExists(layerSource)) {
            geoJsonSource(layerSource) {
                maxzoom(16)
                lineMetrics(useLineMetrics)
                tolerance(tolerance)
            }.bindTo(style)
        }
    }

    fun initializeLayers(style: Style, options: MapboxRouteLineOptions) {
        if (layersAreInitialized(style, options)) {
            return
        }

        val belowLayerIdToUse: String? =
            getBelowLayerIdToUse(
                options.routeLineBelowLayerId,
                style
            )

        addSource(
            style,
            RouteLayerConstants.WAYPOINT_SOURCE_ID,
            options.tolerance,
            useLineMetrics = false
        )
        addSource(
            style,
            RouteLayerConstants.LAYER_GROUP_1_SOURCE_ID,
            options.tolerance,
            useLineMetrics = true
        )
        addSource(
            style,
            RouteLayerConstants.LAYER_GROUP_2_SOURCE_ID,
            options.tolerance,
            useLineMetrics = true
        )
        addSource(
            style,
            RouteLayerConstants.LAYER_GROUP_3_SOURCE_ID,
            options.tolerance,
            useLineMetrics = true
        )

        if (!style.styleLayerExists(RouteLayerConstants.BOTTOM_LEVEL_ROUTE_LINE_LAYER_ID)) {
            style.addPersistentLayer(
                BackgroundLayer(
                    RouteLayerConstants.BOTTOM_LEVEL_ROUTE_LINE_LAYER_ID,
                ).apply { this.backgroundOpacity(0.0) },
                LayerPosition(null, belowLayerIdToUse, null)
            )
        }

        if (!style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_3_TRAIL_CASING)) {
            LineLayer(
                RouteLayerConstants.LAYER_GROUP_3_TRAIL_CASING,
                RouteLayerConstants.LAYER_GROUP_3_SOURCE_ID
            )
                .lineCap(LineCap.ROUND)
                .lineJoin(LineJoin.ROUND)
                .lineWidth(options.resourceProvider.routeCasingLineScaleExpression)
                .lineColor(Color.GRAY).apply {
                    style.addPersistentLayer(this, LayerPosition(null, belowLayerIdToUse, null))
                }
        }
        if (!style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_3_TRAIL)) {
            LineLayer(
                RouteLayerConstants.LAYER_GROUP_3_TRAIL,
                RouteLayerConstants.LAYER_GROUP_3_SOURCE_ID
            )
                .lineCap(LineCap.ROUND)
                .lineJoin(LineJoin.ROUND)
                .lineWidth(options.resourceProvider.routeLineScaleExpression)
                .lineColor(Color.GRAY).apply {
                    style.addPersistentLayer(this, LayerPosition(null, belowLayerIdToUse, null))
                }
        }
        if (!style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_3_CASING)) {
            LineLayer(
                RouteLayerConstants.LAYER_GROUP_3_CASING,
                RouteLayerConstants.LAYER_GROUP_3_SOURCE_ID
            )
                .lineCap(LineCap.ROUND)
                .lineJoin(LineJoin.ROUND)
                .lineWidth(options.resourceProvider.routeCasingLineScaleExpression)
                .lineColor(Color.GRAY).apply {
                    style.addPersistentLayer(this, LayerPosition(null, belowLayerIdToUse, null))
                }
        }
        if (!style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_3_MAIN)) {
            LineLayer(
                RouteLayerConstants.LAYER_GROUP_3_MAIN,
                RouteLayerConstants.LAYER_GROUP_3_SOURCE_ID
            )
                .lineCap(LineCap.ROUND)
                .lineJoin(LineJoin.ROUND)
                .lineWidth(options.resourceProvider.routeLineScaleExpression)
                .lineColor(Color.GRAY).apply {
                    style.addPersistentLayer(this, LayerPosition(null, belowLayerIdToUse, null))
                }
        }
        if (!style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_3_TRAFFIC)) {
            LineLayer(
                RouteLayerConstants.LAYER_GROUP_3_TRAFFIC,
                RouteLayerConstants.LAYER_GROUP_3_SOURCE_ID
            )
                .lineCap(LineCap.ROUND)
                .lineJoin(LineJoin.ROUND)
                .lineWidth(options.resourceProvider.routeTrafficLineScaleExpression)
                .lineColor(Color.GRAY).apply {
                    style.addPersistentLayer(this, LayerPosition(null, belowLayerIdToUse, null))
                }
        }
        if (options.displayRestrictedRoadSections) {
            if (!style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_3_RESTRICTED)) {
                LineLayer(
                    RouteLayerConstants.LAYER_GROUP_3_RESTRICTED,
                    RouteLayerConstants.LAYER_GROUP_3_SOURCE_ID
                )
                    .lineWidth(options.resourceProvider.restrictedRoadLineWidth)
                    .lineJoin(LineJoin.ROUND)
                    .lineOpacity(options.resourceProvider.restrictedRoadOpacity)
                    .lineColor(options.resourceProvider.routeLineColorResources.restrictedRoadColor)
                    .lineDasharray(options.resourceProvider.restrictedRoadDashArray)
                    .lineCap(LineCap.ROUND)
                    .apply {
                        style.addPersistentLayer(this, LayerPosition(null, belowLayerIdToUse, null))
                    }
            }
        }

        if (!style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_2_TRAIL_CASING)) {
            LineLayer(
                RouteLayerConstants.LAYER_GROUP_2_TRAIL_CASING,
                RouteLayerConstants.LAYER_GROUP_2_SOURCE_ID
            )
                .lineCap(LineCap.ROUND)
                .lineJoin(LineJoin.ROUND)
                .lineWidth(options.resourceProvider.routeCasingLineScaleExpression)
                .lineColor(Color.GRAY).apply {
                    style.addPersistentLayer(this, LayerPosition(null, belowLayerIdToUse, null))
                }
        }
        if (!style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_2_TRAIL)) {
            LineLayer(
                RouteLayerConstants.LAYER_GROUP_2_TRAIL,
                RouteLayerConstants.LAYER_GROUP_2_SOURCE_ID
            )
                .lineCap(LineCap.ROUND)
                .lineJoin(LineJoin.ROUND)
                .lineWidth(options.resourceProvider.routeLineScaleExpression)
                .lineColor(Color.GRAY).apply {
                    style.addPersistentLayer(this, LayerPosition(null, belowLayerIdToUse, null))
                }
        }
        if (!style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_2_CASING)) {
            LineLayer(
                RouteLayerConstants.LAYER_GROUP_2_CASING,
                RouteLayerConstants.LAYER_GROUP_2_SOURCE_ID
            )
                .lineCap(LineCap.ROUND)
                .lineJoin(LineJoin.ROUND)
                .lineWidth(options.resourceProvider.routeCasingLineScaleExpression)
                .lineColor(Color.GRAY).apply {
                    style.addPersistentLayer(this, LayerPosition(null, belowLayerIdToUse, null))
                }
        }
        if (!style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_2_MAIN)) {
            LineLayer(
                RouteLayerConstants.LAYER_GROUP_2_MAIN,
                RouteLayerConstants.LAYER_GROUP_2_SOURCE_ID
            )
                .lineCap(LineCap.ROUND)
                .lineJoin(LineJoin.ROUND)
                .lineWidth(options.resourceProvider.routeLineScaleExpression)
                .lineColor(Color.GRAY).apply {
                    style.addPersistentLayer(this, LayerPosition(null, belowLayerIdToUse, null))
                }
        }
        if (!style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_2_TRAFFIC)) {
            LineLayer(
                RouteLayerConstants.LAYER_GROUP_2_TRAFFIC,
                RouteLayerConstants.LAYER_GROUP_2_SOURCE_ID
            )
                .lineCap(LineCap.ROUND)
                .lineJoin(LineJoin.ROUND)
                .lineWidth(options.resourceProvider.routeTrafficLineScaleExpression)
                .lineColor(Color.GRAY).apply {
                    style.addPersistentLayer(this, LayerPosition(null, belowLayerIdToUse, null))
                }
        }
        if (options.displayRestrictedRoadSections) {
            if (!style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_2_RESTRICTED)) {
                LineLayer(
                    RouteLayerConstants.LAYER_GROUP_2_RESTRICTED,
                    RouteLayerConstants.LAYER_GROUP_2_SOURCE_ID
                )
                    .lineWidth(options.resourceProvider.restrictedRoadLineWidth)
                    .lineJoin(LineJoin.ROUND)
                    .lineOpacity(options.resourceProvider.restrictedRoadOpacity)
                    .lineColor(options.resourceProvider.routeLineColorResources.restrictedRoadColor)
                    .lineDasharray(options.resourceProvider.restrictedRoadDashArray)
                    .lineCap(LineCap.ROUND)
                    .apply {
                        style.addPersistentLayer(this, LayerPosition(null, belowLayerIdToUse, null))
                    }
            }
        }

        if (!style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_1_TRAIL_CASING)) {
            LineLayer(
                RouteLayerConstants.LAYER_GROUP_1_TRAIL_CASING,
                RouteLayerConstants.LAYER_GROUP_1_SOURCE_ID
            )
                .lineCap(LineCap.ROUND)
                .lineJoin(LineJoin.ROUND)
                .lineWidth(options.resourceProvider.routeCasingLineScaleExpression)
                .lineColor(Color.GRAY).apply {
                    style.addPersistentLayer(this, LayerPosition(null, belowLayerIdToUse, null))
                }
        }
        if (!style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_1_TRAIL)) {
            LineLayer(
                RouteLayerConstants.LAYER_GROUP_1_TRAIL,
                RouteLayerConstants.LAYER_GROUP_1_SOURCE_ID
            )
                .lineCap(LineCap.ROUND)
                .lineJoin(LineJoin.ROUND)
                .lineWidth(options.resourceProvider.routeLineScaleExpression)
                .lineColor(Color.GRAY).apply {
                    style.addPersistentLayer(this, LayerPosition(null, belowLayerIdToUse, null))
                }
        }
        if (!style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_1_CASING)) {
            LineLayer(
                RouteLayerConstants.LAYER_GROUP_1_CASING,
                RouteLayerConstants.LAYER_GROUP_1_SOURCE_ID
            )
                .lineCap(LineCap.ROUND)
                .lineJoin(LineJoin.ROUND)
                .lineWidth(options.resourceProvider.routeCasingLineScaleExpression)
                .lineColor(Color.GRAY).apply {
                    style.addPersistentLayer(this, LayerPosition(null, belowLayerIdToUse, null))
                }
        }
        if (!style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_1_MAIN)) {
            LineLayer(
                RouteLayerConstants.LAYER_GROUP_1_MAIN,
                RouteLayerConstants.LAYER_GROUP_1_SOURCE_ID
            )
                .lineCap(LineCap.ROUND)
                .lineJoin(LineJoin.ROUND)
                .lineWidth(options.resourceProvider.routeLineScaleExpression)
                .lineColor(Color.GRAY).apply {
                    style.addPersistentLayer(this, LayerPosition(null, belowLayerIdToUse, null))
                }
        }
        if (!style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_1_TRAFFIC)) {
            LineLayer(
                RouteLayerConstants.LAYER_GROUP_1_TRAFFIC,
                RouteLayerConstants.LAYER_GROUP_1_SOURCE_ID
            )
                .lineCap(LineCap.ROUND)
                .lineJoin(LineJoin.ROUND)
                .lineWidth(options.resourceProvider.routeTrafficLineScaleExpression)
                .lineColor(Color.GRAY).apply {
                    style.addPersistentLayer(this, LayerPosition(null, belowLayerIdToUse, null))
                }
        }
        if (options.displayRestrictedRoadSections) {
            if (!style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_1_RESTRICTED)) {
                LineLayer(
                    RouteLayerConstants.LAYER_GROUP_1_RESTRICTED,
                    RouteLayerConstants.LAYER_GROUP_1_SOURCE_ID
                )
                    .lineWidth(options.resourceProvider.restrictedRoadLineWidth)
                    .lineJoin(LineJoin.ROUND)
                    .lineOpacity(options.resourceProvider.restrictedRoadOpacity)
                    .lineColor(options.resourceProvider.routeLineColorResources.restrictedRoadColor)
                    .lineDasharray(options.resourceProvider.restrictedRoadDashArray)
                    .lineCap(LineCap.ROUND)
                    .apply {
                        style.addPersistentLayer(this, LayerPosition(null, belowLayerIdToUse, null))
                    }
            }
        }

        if (!style.styleLayerExists(RouteLayerConstants.TOP_LEVEL_ROUTE_LINE_LAYER_ID)) {
            style.addPersistentLayer(
                BackgroundLayer(
                    RouteLayerConstants.TOP_LEVEL_ROUTE_LINE_LAYER_ID
                ).apply { this.backgroundOpacity(0.0) },
                LayerPosition(null, belowLayerIdToUse, null)
            )
        }

        buildWayPointLayer(
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
        return style.styleSourceExists(RouteLayerConstants.LAYER_GROUP_1_SOURCE_ID) &&
            style.styleSourceExists(RouteLayerConstants.LAYER_GROUP_2_SOURCE_ID) &&
            style.styleSourceExists(RouteLayerConstants.LAYER_GROUP_3_SOURCE_ID) &&
            style.styleLayerExists(RouteLayerConstants.TOP_LEVEL_ROUTE_LINE_LAYER_ID) &&
            style.styleLayerExists(RouteLayerConstants.BOTTOM_LEVEL_ROUTE_LINE_LAYER_ID) &&
            style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_1_TRAIL_CASING) &&
            style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_1_TRAIL) &&
            style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_1_CASING) &&
            style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_1_MAIN) &&
            style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_1_TRAFFIC) &&
            style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_2_TRAIL_CASING) &&
            style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_2_TRAIL) &&
            style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_2_CASING) &&
            style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_2_MAIN) &&
            style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_2_TRAFFIC) &&
            style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_3_TRAIL_CASING) &&
            style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_3_TRAIL) &&
            style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_3_CASING) &&
            style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_3_MAIN) &&
            style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_3_TRAFFIC) &&
            if (options.displayRestrictedRoadSections) {
                style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_1_RESTRICTED) &&
                    style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_2_RESTRICTED) &&
                    style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_3_RESTRICTED)
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
        val stopGap = softGradientTransitionDistance / route.distance()
        getTrafficLineExpression(
            vanishingPointOffset,
            lineStartColor,
            lineColor,
            stopGap,
            useSoftGradient,
            segments
        )
    }

    internal fun getTrafficLineExpression(
        vanishingPointOffset: Double,
        lineStartColor: Int,
        lineColor: Int,
        stopGap: Double,
        useSoftGradient: Boolean,
        segments: List<RouteLineExpressionData>,
    ): Expression {
        return if (useSoftGradient) {
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

    internal fun getLayerIdsForPrimaryRoute(
        style: Style,
        sourceLayerMap: Map<RouteLineSourceKey, Set<String>>
    ): Set<String> {
        return getTopRouteLineRelatedLayerId(style)?.run {
            when (this) {
                in layerGroup1SourceLayerIds -> {
                    sourceLayerMap[layerGroup1SourceKey]
                }
                in layerGroup2SourceLayerIds -> {
                    sourceLayerMap[layerGroup2SourceKey]
                }
                in layerGroup3SourceLayerIds -> {
                    sourceLayerMap[layerGroup3SourceKey]
                }
                else -> setOf()
            }
        } ?: setOf()
    }

    internal fun getTopRouteLineRelatedLayerId(style: Style): String? {
        return runCatching {
            val upperRange = style.styleLayers.indexOf(
                StyleObjectInfo(
                    RouteLayerConstants.TOP_LEVEL_ROUTE_LINE_LAYER_ID,
                    "background"
                )
            )
            val lowerRange = style.styleLayers.indexOf(
                StyleObjectInfo(
                    RouteLayerConstants.BOTTOM_LEVEL_ROUTE_LINE_LAYER_ID,
                    "background"
                )
            )

            style.styleLayers.subList(lowerRange, upperRange)
                .mapIndexed { index, styleObjectInfo ->
                    Pair(index, styleObjectInfo.id)
                }.maxByOrNull { it.first }?.second
        }.getOrNull()
    }

    /**
     * Looks for a property in the [FeatureCollection] matching an identifier in the
     * collection of [RouteStyleDescriptor] items and if a match is found the color from
     * the [RouteStyleDescriptor] is returned. If no match is found the fallback color is returned.
     */
    internal fun getMatchingColors(
        featureCollection: FeatureCollection?,
        styleDescriptors: List<RouteStyleDescriptor>,
        fallbackLineColor: Int,
        fallbackLineCasingColor: Int
    ): Pair<Int, Int> {
        return styleDescriptors.firstOrNull {
            featureCollectionHasProperty(featureCollection, 0, it.routeIdentifier)
        }?.run {
            Pair(this.lineColor, this.lineCasingColor)
        } ?: Pair(fallbackLineColor, fallbackLineCasingColor)
    }

    internal tailrec fun featureCollectionHasProperty(
        featureCollection: FeatureCollection?,
        index: Int,
        property: String
    ): Boolean {
        return when {
            featureCollection == null -> false
            featureCollection.features().isNullOrEmpty() -> false
            index >= featureCollection.features()!!.size -> false
            featureCollection
                .features()
                ?.get(index)
                ?.hasNonNullValueForProperty(property) == true -> true
            else -> featureCollectionHasProperty(featureCollection, index + 1, property)
        }
    }

    internal fun getAlternativeRoutesDeviationOffsets(
        alternativeRoutesMetadata: List<AlternativeRouteMetadata>
    ): Map<String, Double> {
        val alternativesDeviationOffset = mutableMapOf<String, Double>()
        alternativeRoutesMetadata.forEach { routeMetadata ->
            var runningDistance = 0.0
            val route = routeMetadata.navigationRoute.directionsRoute
            route.legs()?.forEachIndexed LegLoop@{ legIndex, leg ->
                leg.annotation()?.distance()
                    ?.forEachIndexed AnnotationLoop@{ annotationIndex, distance ->
                        val forkLegIndex = routeMetadata.forkIntersectionOfAlternativeRoute.legIndex
                        val forkGeometryIndexInLeg =
                            routeMetadata.forkIntersectionOfAlternativeRoute.geometryIndexInLeg
                        if (legIndex == forkLegIndex && annotationIndex == forkGeometryIndexInLeg) {
                            val percentageTraveled = runningDistance / route.distance()
                            alternativesDeviationOffset[routeMetadata.navigationRoute.id] =
                                percentageTraveled
                            return@LegLoop
                        }
                        runningDistance += distance
                    }
            }
        }
        return alternativesDeviationOffset
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

    private fun buildWayPointLayer(
        style: Style,
        originIcon: Drawable,
        destinationIcon: Drawable,
        iconOffset: List<Double>,
        iconAnchor: IconAnchor,
        iconPitchAlignment: IconPitchAlignment
    ): SymbolLayer {
        if (style.styleLayerExists(RouteLayerConstants.WAYPOINT_LAYER_ID)) {
            style.removeStyleLayer(RouteLayerConstants.WAYPOINT_LAYER_ID)
        }

        if (style.getStyleImage(RouteLayerConstants.ORIGIN_MARKER_NAME) != null) {
            style.removeStyleImage(RouteLayerConstants.ORIGIN_MARKER_NAME)
        }
        originIcon.getBitmap().let {
            style.addImage(RouteLayerConstants.ORIGIN_MARKER_NAME, it)
        }

        if (style.getStyleImage(RouteLayerConstants.DESTINATION_MARKER_NAME) != null) {
            style.removeStyleImage(RouteLayerConstants.DESTINATION_MARKER_NAME)
        }
        destinationIcon.getBitmap().let {
            style.addImage(RouteLayerConstants.DESTINATION_MARKER_NAME, it)
        }

        return SymbolLayer(
            RouteLayerConstants.WAYPOINT_LAYER_ID,
            RouteLayerConstants.WAYPOINT_SOURCE_ID
        )
            .iconOffset(iconOffset)
            .iconAnchor(iconAnchor)
            .iconImage(
                match {
                    toString {
                        get { literal(RouteLayerConstants.WAYPOINT_PROPERTY_KEY) }
                    }
                    literal(RouteLayerConstants.WAYPOINT_ORIGIN_VALUE)
                    stop {
                        RouteLayerConstants.WAYPOINT_ORIGIN_VALUE
                        literal(RouteLayerConstants.ORIGIN_MARKER_NAME)
                    }
                    stop {
                        RouteLayerConstants.WAYPOINT_DESTINATION_VALUE
                        literal(RouteLayerConstants.DESTINATION_MARKER_NAME)
                    }
                }
            ).iconSize(
                interpolate {
                    exponential { literal(1.5) }
                    zoom()
                    stop {
                        literal(0.0)
                        literal(0.6)
                    }
                    stop {
                        literal(10.0)
                        literal(0.8)
                    }
                    stop {
                        literal(12.0)
                        literal(1.3)
                    }
                    stop {
                        literal(22.0)
                        literal(2.8)
                    }
                }
            )
            .iconPitchAlignment(iconPitchAlignment)
            .iconAllowOverlap(true)
            .iconIgnorePlacement(true)
            .iconKeepUpright(true)
    }
}
