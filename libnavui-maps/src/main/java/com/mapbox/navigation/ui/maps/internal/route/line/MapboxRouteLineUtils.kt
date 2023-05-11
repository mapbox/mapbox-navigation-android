package com.mapbox.navigation.ui.maps.internal.route.line

import android.graphics.Color
import android.util.LruCache
import androidx.annotation.ColorInt
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.api.directions.v5.models.StepIntersection
import com.mapbox.bindgen.Value
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.LayerPosition
import com.mapbox.maps.Style
import com.mapbox.maps.StyleObjectInfo
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.extension.style.expressions.dsl.generated.literal
import com.mapbox.maps.extension.style.expressions.dsl.generated.match
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.addPersistentLayer
import com.mapbox.maps.extension.style.layers.generated.BackgroundLayer
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.extension.style.layers.generated.SymbolLayer
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.internal.extensions.isLegWaypoint
import com.mapbox.navigation.base.internal.utils.internalWaypoints
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.utils.DecodeUtils.completeGeometryToLineString
import com.mapbox.navigation.base.utils.DecodeUtils.stepsGeometryToPoints
import com.mapbox.navigation.core.routealternatives.AlternativeRouteMetadata
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants
import com.mapbox.navigation.ui.maps.route.line.model.ExpressionOffsetData
import com.mapbox.navigation.ui.maps.route.line.model.ExtractedRouteData
import com.mapbox.navigation.ui.maps.route.line.model.ExtractedRouteRestrictionData
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.NavigationRouteLine
import com.mapbox.navigation.ui.maps.route.line.model.RouteFeatureData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineDistancesIndex
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineDynamicData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineExpressionData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineExpressionProvider
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineGranularDistances
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineScaleValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineSourceKey
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineTrimExpressionProvider
import com.mapbox.navigation.ui.maps.route.line.model.RouteStyleDescriptor
import com.mapbox.navigation.ui.maps.util.CacheResultUtils
import com.mapbox.navigation.ui.maps.util.CacheResultUtils.cacheResult
import com.mapbox.navigation.ui.maps.util.CacheResultUtils.cacheRouteResult
import com.mapbox.navigation.ui.maps.util.CacheResultUtils.cacheRouteTrafficResult
import com.mapbox.navigation.ui.utils.internal.extensions.getBitmap
import com.mapbox.navigation.ui.utils.internal.ifNonNull
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigation.utils.internal.logW
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMisc
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal object MapboxRouteLineUtils {

    private const val LOG_CATEGORY = "MapboxRouteLineUtils"
    internal const val VANISH_POINT_STOP_GAP = .00000000001
    private const val NUMBER_OF_SUPPORTED_ROUTES = 3
    private const val TOLERANCE = 0.00000001

    internal val extractRouteDataCache: LruCache<
        CacheResultUtils.CacheResultKeyRouteTraffic<
            List<ExtractedRouteData>>, List<ExtractedRouteData>>
        by lazy { LruCache(NUMBER_OF_SUPPORTED_ROUTES) }

    private val granularDistancesCache: LruCache<
        CacheResultUtils.CacheResultKeyRoute<
            RouteLineGranularDistances?>, RouteLineGranularDistances?>
        by lazy { LruCache(NUMBER_OF_SUPPORTED_ROUTES) }

    val layerGroup1SourceKey = RouteLineSourceKey(RouteLayerConstants.LAYER_GROUP_1_SOURCE_ID)
    val layerGroup2SourceKey = RouteLineSourceKey(RouteLayerConstants.LAYER_GROUP_2_SOURCE_ID)
    val layerGroup3SourceKey = RouteLineSourceKey(RouteLayerConstants.LAYER_GROUP_3_SOURCE_ID)

    // ordering is important
    val layerGroup1SourceLayerIds = setOf(
        RouteLayerConstants.LAYER_GROUP_1_TRAIL_CASING,
        RouteLayerConstants.LAYER_GROUP_1_TRAIL,
        RouteLayerConstants.LAYER_GROUP_1_CASING,
        RouteLayerConstants.LAYER_GROUP_1_MAIN,
        RouteLayerConstants.LAYER_GROUP_1_TRAFFIC,
        RouteLayerConstants.LAYER_GROUP_1_RESTRICTED,
        RouteLayerConstants.LAYER_GROUP_1_VIOLATED,
    )

    // ordering is important
    val layerGroup2SourceLayerIds = setOf(
        RouteLayerConstants.LAYER_GROUP_2_TRAIL_CASING,
        RouteLayerConstants.LAYER_GROUP_2_TRAIL,
        RouteLayerConstants.LAYER_GROUP_2_CASING,
        RouteLayerConstants.LAYER_GROUP_2_MAIN,
        RouteLayerConstants.LAYER_GROUP_2_TRAFFIC,
        RouteLayerConstants.LAYER_GROUP_2_RESTRICTED,
        RouteLayerConstants.LAYER_GROUP_2_VIOLATED,
    )

    // ordering is important
    val layerGroup3SourceLayerIds = setOf(
        RouteLayerConstants.LAYER_GROUP_3_TRAIL_CASING,
        RouteLayerConstants.LAYER_GROUP_3_TRAIL,
        RouteLayerConstants.LAYER_GROUP_3_CASING,
        RouteLayerConstants.LAYER_GROUP_3_MAIN,
        RouteLayerConstants.LAYER_GROUP_3_TRAFFIC,
        RouteLayerConstants.LAYER_GROUP_3_RESTRICTED,
        RouteLayerConstants.LAYER_GROUP_3_VIOLATED,
    )

    // ordering is important
    val maskingLayerIds = setOf(
        RouteLayerConstants.MASKING_LAYER_TRAIL_CASING,
        RouteLayerConstants.MASKING_LAYER_TRAIL,
        RouteLayerConstants.MASKING_LAYER_CASING,
        RouteLayerConstants.MASKING_LAYER_MAIN,
        RouteLayerConstants.MASKING_LAYER_TRAFFIC,
        RouteLayerConstants.MASKING_LAYER_RESTRICTED,
        RouteLayerConstants.MASKING_LAYER_VIOLATED,
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
            defaultObjectCreator = {
                RouteLineExpressionData(distanceOffset, lineColor, 0)
            }
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
            defaultObjectCreator = {
                RouteLineExpressionData(distanceOffset, lineColor, 0)
            }
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
                // ignoring offset points that are at the end of a route
                // as that would create a gradient slope towards the last point of the route
                // which is not necessary
                if (expressionData.offset < 1.0) {
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
            defaultObjectCreator = {
                RouteLineExpressionData(distanceOffset, defaultColor, 0)
            }
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

    /**
     * We're removing all stops before the vanishing offset and then we're also removing all duplicate offsets,
     * however, strictly leaving the last valid offset, not the first valid offset.
     * This is needed to workaround a problem where Direction API injects an intersection in the leg arrival step
     * that doesn't have a `restricted` road class assigned, even if it is in a restricted area, or if there are duplicate points in the route.
     * In such cases, if we were to filter the expression data by distinct first offset, we'd have a gap in the line at the point of the duplication.
     */
    internal fun <T : ExpressionOffsetData> getFilteredRouteLineExpressionData(
        distanceOffset: Double,
        routeLineExpressionData: List<T>,
        defaultObjectCreator: () -> T
    ): List<T> {
        val filteredItems = routeLineExpressionData.filterIndexed { index, restrictionData ->
            restrictionData.offset > distanceOffset &&
                restrictionData.offset != routeLineExpressionData.getOrNull(index + 1)?.offset
        }
        return when (filteredItems.isEmpty()) {
            true -> when (routeLineExpressionData.isEmpty()) {
                true -> listOf(defaultObjectCreator())
                false -> listOf(routeLineExpressionData.last().copyWithNewOffset(distanceOffset))
            }
            false -> {
                val firstItemIndex = routeLineExpressionData.indexOf(filteredItems.first())
                val fillerItem = if (firstItemIndex == 0) {
                    routeLineExpressionData[firstItemIndex]
                } else {
                    routeLineExpressionData[firstItemIndex - 1]
                }
                listOf<T>(fillerItem.copyWithNewOffset(distanceOffset)).plus(filteredItems)
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
        route: NavigationRoute,
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
     * on factors such as traffic congestion and/or road class.
     */
    internal val extractRouteDataWithTrafficAndRoadClassDeDuped: (
        route: NavigationRoute,
        trafficCongestionProvider: (RouteLeg) -> List<String>?
    ) -> List<ExtractedRouteData> =
        { route: NavigationRoute, trafficCongestionProvider: (RouteLeg) -> List<String>? ->
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
        }

    /**
     * Extracts restricted road section data from the route. Most routes do not contain restricted
     * sections. This can be an expensive operation.The implementation defers or avoids the most
     * expensive operations as much as possible.
     */
    internal fun extractRouteRestrictionData(
        route: NavigationRoute,
        distancesProvider: (NavigationRoute) -> RouteLineGranularDistances?
    ): List<ExtractedRouteRestrictionData> {
        val itemsToReturn = mutableListOf<ExtractedRouteRestrictionData>()
        route.directionsRoute.legs()?.forEachIndexed { legIndex, leg ->
            val filteredIntersections = filterForRestrictedIntersections(leg)
            ifNonNull(filteredIntersections) { stepIntersections ->
                val legDistancesArray = distancesProvider(route)?.legsDistances
                stepIntersections.forEach { stepIntersectionData ->
                    val geometryIndex = stepIntersectionData.first.geometryIndex()
                    if (
                        geometryIndex != null &&
                        legDistancesArray?.isNotEmpty() == true &&
                        legIndex < legDistancesArray.size &&
                        geometryIndex < legDistancesArray[legIndex].size
                    ) {
                        val distanceRemaining =
                            legDistancesArray[legIndex][geometryIndex].distanceRemaining
                        (1.0 - distanceRemaining / distancesProvider(route)!!.completeDistance)
                            .apply {
                                if (this in 0.0..1.0) {
                                    itemsToReturn.add(
                                        ExtractedRouteRestrictionData(
                                            this,
                                            stepIntersectionData.second,
                                            legIndex
                                        )
                                    )
                                }
                            }
                    }
                }
            }
        }
        return itemsToReturn
    }

    internal fun extractViolatedSectionsData(
        route: NavigationRoute,
        distancesProvider: (NavigationRoute) -> RouteLineGranularDistances?
    ): List<ExtractedRouteRestrictionData> {
        val itemsToReturn = mutableListOf<ExtractedRouteRestrictionData>()
        val granularDistances = distancesProvider(route)
        route.directionsRoute.legs()?.forEachIndexed { legIndex, leg ->
            val legNotificationIndices = extractNotificationIndices(leg)
            val legDistances = granularDistances?.legsDistances?.getOrNull(legIndex)
            val routeDistance = granularDistances?.completeDistance
            legNotificationIndices.forEach { (startIndex, endIndex) ->
                val startRouteLineDistance = legDistances?.getOrNull(startIndex)
                val endRouteLineDistance = legDistances?.getOrNull(endIndex)
                if (startRouteLineDistance != null && endRouteLineDistance != null) {
                    val startOffset = 1.0 - startRouteLineDistance.distanceRemaining / routeDistance!!
                    val endOffset = 1.0 - endRouteLineDistance.distanceRemaining / routeDistance
                    if (startOffset in 0.0..1.0 && endOffset in 0.0..1.0) {
                        if (itemsToReturn.isEmpty() && startIndex >= TOLERANCE) {
                            itemsToReturn.add(ExtractedRouteRestrictionData(0.0, false, 0))
                        }
                        itemsToReturn.add(
                            ExtractedRouteRestrictionData(
                                startOffset,
                                true,
                                legIndex
                            )
                        )
                        itemsToReturn.add(
                            ExtractedRouteRestrictionData(
                                endOffset,
                                false,
                                legIndex
                            )
                        )
                    }
                }
            }
        }
        return itemsToReturn
    }

    private fun extractNotificationIndices(leg: RouteLeg): List<Pair<Int, Int>> {
        val result = mutableListOf<Pair<Int, Int>>()
        leg.notifications()?.forEach { notification ->
            val startIndex = notification.geometryIndexStart()
            val endIndex = notification.geometryIndexEnd()
            if (startIndex != null && endIndex != null) {
                result.add(startIndex to endIndex)
            }
        }
        return result
    }

    /**
     * Filters the [RouteLeg] for intersections that are designated as restricted. If there are
     * no restricted intersections null is returned. If there is at least one restricted intersection
     * data is returned for that intersection plus data for intersections around the restricted
     * intersection.
     */
    private fun filterForRestrictedIntersections(
        leg: RouteLeg
    ): List<Pair<StepIntersection, Boolean>>? {
        val intersections = leg.steps()?.map {
            it.intersections() ?: emptyList()
        }?.flatten()
        return intersections?.mapIndexed { index, stepIntersection ->
            val isRestricted =
                stepIntersection.classes()?.contains("restricted") ?: false
            val previousIsRestricted = index != 0 &&
                intersections[index - 1].classes()?.contains("restricted") ?: false

            if (isRestricted || index == 0 || previousIsRestricted) {
                Pair(stepIntersection, isRestricted)
            } else {
                null
            }
        }?.filterNotNull()?.run {
            if (this.size <= 1) {
                null
            } else {
                this
            }
        }
    }

    /**
     * Extracts data from the [DirectionsRoute] in a format more useful to the route line
     * API. The data returned here is used by several different calculations. The results
     * are cached for performance reasons.
     */
    internal val extractRouteData: (
        route: NavigationRoute,
        trafficCongestionProvider: (RouteLeg) -> List<String>?
    ) -> List<ExtractedRouteData> =
        { route: NavigationRoute, trafficCongestionProvider: (RouteLeg) -> List<String>? ->
            val itemsToReturn = mutableListOf<ExtractedRouteData>()
            val granularDistances = granularDistancesProvider(route)
            if (
                granularDistances != null &&
                granularDistances.legsDistances.size == (route.directionsRoute.legs()?.size ?: 0)
            ) {
                route.directionsRoute.legs()?.forEachIndexed { legIndex, leg ->
                    val closureRanges = getClosureRanges(leg).asSequence()
                    val roadClassArray = getRoadClassArray(leg.steps())
                    val trafficCongestion = trafficCongestionProvider.invoke(leg)

                    granularDistances.legsDistances[legIndex].forEachIndexed { index, distanceObj ->
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

                        val offset =
                            1.0 - distanceObj.distanceRemaining / granularDistances.completeDistance
                        itemsToReturn.add(
                            ExtractedRouteData(
                                offset,
                                congestionValue,
                                roadClass,
                                legIndex,
                                isLegOrigin = index == 0
                            )
                        )
                    }
                }
            } else {
                logE(LOG_CATEGORY) {
                    "Unable to produce route granular distances for '${route.id}'."
                }
            }
            itemsToReturn
        }.cacheRouteTrafficResult(extractRouteDataCache)

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
        }

    /**
     * Decodes the route and produces [RouteLineGranularDistances].
     */
    internal val granularDistancesProvider: (
        route: NavigationRoute,
    ) -> RouteLineGranularDistances? =
        { route: NavigationRoute ->
            val points = route.directionsRoute.stepsGeometryToPoints()
            calculateGranularDistances(points)
        }.cacheRouteResult(granularDistancesCache)

    internal fun getTrafficCongestionAnnotationProvider(
        route: NavigationRoute,
        routeLineColorResources: RouteLineColorResources
    ): (RouteLeg) -> List<String>? {
        return if (
            route.directionsRoute.routeOptions()
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

    internal fun getRoadClassArray(
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
     * @param routeLineColorResources provides color values for the route line
     * @param isPrimaryRoute indicates if the route used is the primary route
     * @param trafficOverrideRoadClasses a collection of road classes for which a color
     * substitution should occur.
     */
    internal fun getRouteLineExpressionDataWithStreetClassOverride(
        annotationExpressionData: List<ExtractedRouteData>,
        routeLineColorResources: RouteLineColorResources,
        isPrimaryRoute: Boolean,
        trafficOverrideRoadClasses: List<String>
    ): List<RouteLineExpressionData> {
        val expressionDataToReturn = mutableListOf<RouteLineExpressionData>()
        annotationExpressionData.forEachIndexed { index, annotationExpData ->
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
                        annotationExpData.offset,
                        trafficColor,
                        annotationExpData.legIndex
                    )
                )
            } else if (trafficColor != expressionDataToReturn.last().segmentColor) {
                expressionDataToReturn.add(
                    RouteLineExpressionData(
                        annotationExpData.offset,
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

    private fun calculateGranularDistances(
        stepsPoints: List<List<List<Point>>>
    ): RouteLineGranularDistances {
        var distance = 0.0

        val stepsArray = stepsPoints.map { pointsPerLeg ->
            pointsPerLeg.map { stepPoints ->
                // there can be a lot of points for each step
                // we're using arrays here to avoid collection resize cycles
                // which significantly improves performance
                arrayOfNulls<RouteLineDistancesIndex>(stepPoints.size)
            }.toTypedArray()
        }.toTypedArray()
        val legsArray = mutableListOf<Array<RouteLineDistancesIndex>>()
        val routeArray = mutableListOf<RouteLineDistancesIndex>()

        // we're iterating from the back of the route
        // gradually building up the distance from end variable
        for (i in stepsPoints.lastIndex downTo 0) {
            val legPoints = stepsPoints[i]
            for (j in legPoints.lastIndex downTo 0) {
                val stepPoints = legPoints[j]
                if (stepPoints.isNotEmpty()) {
                    // the last point in a step has always an equal distance to the end of the route
                    // as the first point from the following step
                    stepsArray[i][j][stepPoints.lastIndex] =
                        RouteLineDistancesIndex(stepPoints.last(), distance)
                }
                for (k in stepPoints.lastIndex downTo 1) {
                    val curr = stepPoints[k]
                    val prev = stepPoints[k - 1]
                    distance += calculateDistance(curr, prev)
                    stepsArray[i][j][k - 1] = RouteLineDistancesIndex(prev, distance)
                }
            }
        }

        stepsArray.forEachIndexed { legIndex, stepDistances ->
            val legArray = mutableListOf<RouteLineDistancesIndex>()
            stepDistances.forEachIndexed { stepIndex, distances ->
                val squashed = if (distances.size == 2) {
                    // step with 2 coordinate might have duplicated values
                    distances.toSet().toTypedArray()
                } else {
                    distances
                }
                legArray.addAll(
                    if (stepIndex != 0) {
                        // removing duplicate points for adjacent steps
                        // Array#copyOfRange is significantly faster than Collection#drop
                        squashed.copyOfRange(1, squashed.size)
                    } else {
                        squashed
                    } as Array<RouteLineDistancesIndex>
                )
            }
            legsArray.add(legIndex, legArray.toTypedArray())
        }

        legsArray.forEachIndexed { legIndex, distances ->
            routeArray.addAll(
                if (legIndex != 0) {
                    // removing duplicate points for adjacent legs
                    distances.copyOfRange(1, distances.size)
                } else {
                    distances
                }
            )
        }

        return RouteLineGranularDistances(
            distance,
            routeDistances = routeArray.toTypedArray(),
            legsDistances = legsArray.toTypedArray(),
            stepsDistances = stepsArray as Array<Array<Array<RouteLineDistancesIndex>>>,
        )
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
        val legWaypoints = route.internalWaypoints().filter { it.isLegWaypoint() }
        val waypointFeatures = legWaypoints.mapIndexed { index, waypoint ->
            waypoint.location.let {
                Feature.fromGeometry(it).apply {
                    val propValue = if (index == 0) {
                        RouteLayerConstants.WAYPOINT_ORIGIN_VALUE
                    } else {
                        RouteLayerConstants.WAYPOINT_DESTINATION_VALUE
                    }
                    addStringProperty(RouteLayerConstants.WAYPOINT_PROPERTY_KEY, propValue)
                }
            }
        }
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
            granularDistances.routeDistances.run {
                val points = mutableListOf<Point>()
                for (i in max(upcomingIndex - 10, 0)..upcomingIndex) {
                    points.add(this[i].point)
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

    /**
     * Compares provided [properties] map with existing values for a source with [sourceId].
     *
     * The primary mechanism to compare the values is [Value.equals],
     * however, there's a special case [Double]s which are compared with a delta.
     * Delta comparison is not supported for collections
     * but it's a case that this class currently doesn't need to take into account.
     */
    private fun Style.sourcePropertiesCompatible(
        sourceId: String,
        properties: HashMap<String, Value>
    ) = properties.all {
        val existing = getStyleSourceProperty(sourceId, it.key).value
        val existingContents = existing.contents
        val expected = it.value
        val expectedContents = expected.contents
        if (existingContents is Double && expectedContents is Double) {
            abs(existingContents - expectedContents) < 0.000001
        } else if (existingContents is Float && expectedContents is Float) {
            abs(existingContents - expectedContents) < 0.000001f
        } else {
            existing == expected
        }
    }

    private fun Style.addNewOrReuseSource(
        id: String,
        tolerance: Double,
        useLineMetrics: Boolean,
        enableSharedCache: Boolean
    ) {
        val source = if (styleSourceExists(id)) {
            getSourceAs<GeoJsonSource>(id)
        } else {
            null
        }

        val expectedProperties = hashMapOf(
            "type" to Value("geojson"),
            "sharedCache" to Value(enableSharedCache),
            "maxzoom" to Value(16),
            "lineMetrics" to Value(useLineMetrics),
            "tolerance" to Value(tolerance),
        )
        val recreateSource = source == null || !sourcePropertiesCompatible(id, expectedProperties)
        if (recreateSource) {
            source?.let {
                removeStyleSource(it.sourceId)
            }
            addStyleSource(
                id,
                Value(
                    expectedProperties.apply {
                        // reuse feature collections when re-adding the source
                        put("data", Value(source?.data ?: ""))
                    }
                )
            )
        }
    }

    fun initializeLayers(style: Style, options: MapboxRouteLineOptions) {
        val belowLayerIdToUse: String? =
            getBelowLayerIdToUse(
                options.routeLineBelowLayerId,
                style
            )

        style.addNewOrReuseSource(
            RouteLayerConstants.WAYPOINT_SOURCE_ID,
            options.tolerance,
            useLineMetrics = false,
            enableSharedCache = false
        )
        style.addNewOrReuseSource(
            RouteLayerConstants.LAYER_GROUP_1_SOURCE_ID,
            options.tolerance,
            useLineMetrics = true,
            enableSharedCache = options.shareLineGeometrySources
        )
        style.addNewOrReuseSource(
            RouteLayerConstants.LAYER_GROUP_2_SOURCE_ID,
            options.tolerance,
            useLineMetrics = true,
            enableSharedCache = options.shareLineGeometrySources
        )
        style.addNewOrReuseSource(
            RouteLayerConstants.LAYER_GROUP_3_SOURCE_ID,
            options.tolerance,
            useLineMetrics = true,
            enableSharedCache = options.shareLineGeometrySources
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
                    style.layerLineDepthOcclusionFactor(layerId, options.lineDepthOcclusionFactor)
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
                    style.layerLineDepthOcclusionFactor(layerId, options.lineDepthOcclusionFactor)
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
                    style.layerLineDepthOcclusionFactor(layerId, options.lineDepthOcclusionFactor)
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
                    style.layerLineDepthOcclusionFactor(layerId, options.lineDepthOcclusionFactor)
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
                    style.layerLineDepthOcclusionFactor(layerId, options.lineDepthOcclusionFactor)
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
                        style.layerLineDepthOcclusionFactor(
                            layerId,
                            options.lineDepthOcclusionFactor
                        )
                    }
            }
        }
        if (options.displayViolatedSections) {
            if (!style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_3_VIOLATED)) {
                LineLayer(
                    RouteLayerConstants.LAYER_GROUP_3_VIOLATED,
                    RouteLayerConstants.LAYER_GROUP_3_SOURCE_ID
                )
                    .lineWidth(options.resourceProvider.violatedSectionLineWidth)
                    .lineJoin(LineJoin.ROUND)
                    .lineOpacity(options.resourceProvider.violatedSectionOpacity)
                    .lineColor(options.resourceProvider.routeLineColorResources.violatedSectionColor)
                    .lineDasharray(options.resourceProvider.violatedSectionDashArray)
                    .lineCap(LineCap.ROUND)
                    .apply {
                        style.addPersistentLayer(this, LayerPosition(null, belowLayerIdToUse, null))
                        style.layerLineDepthOcclusionFactor(
                            layerId,
                            options.lineDepthOcclusionFactor
                        )
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
                    style.layerLineDepthOcclusionFactor(layerId, options.lineDepthOcclusionFactor)
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
                    style.layerLineDepthOcclusionFactor(layerId, options.lineDepthOcclusionFactor)
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
                    style.layerLineDepthOcclusionFactor(layerId, options.lineDepthOcclusionFactor)
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
                    style.layerLineDepthOcclusionFactor(layerId, options.lineDepthOcclusionFactor)
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
                    style.layerLineDepthOcclusionFactor(layerId, options.lineDepthOcclusionFactor)
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
                        style.layerLineDepthOcclusionFactor(
                            layerId,
                            options.lineDepthOcclusionFactor
                        )
                    }
            }
        }
        if (options.displayViolatedSections) {
            if (!style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_2_VIOLATED)) {
                LineLayer(
                    RouteLayerConstants.LAYER_GROUP_2_VIOLATED,
                    RouteLayerConstants.LAYER_GROUP_2_SOURCE_ID
                )
                    .lineWidth(options.resourceProvider.violatedSectionLineWidth)
                    .lineJoin(LineJoin.ROUND)
                    .lineOpacity(options.resourceProvider.violatedSectionOpacity)
                    .lineColor(options.resourceProvider.routeLineColorResources.violatedSectionColor)
                    .lineDasharray(options.resourceProvider.violatedSectionDashArray)
                    .apply {
                        style.addPersistentLayer(this, LayerPosition(null, belowLayerIdToUse, null))
                        style.layerLineDepthOcclusionFactor(
                            layerId,
                            options.lineDepthOcclusionFactor
                        )
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
                    style.layerLineDepthOcclusionFactor(layerId, options.lineDepthOcclusionFactor)
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
                    style.layerLineDepthOcclusionFactor(layerId, options.lineDepthOcclusionFactor)
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
                    style.layerLineDepthOcclusionFactor(layerId, options.lineDepthOcclusionFactor)
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
                    style.layerLineDepthOcclusionFactor(layerId, options.lineDepthOcclusionFactor)
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
                    style.layerLineDepthOcclusionFactor(layerId, options.lineDepthOcclusionFactor)
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
                        style.layerLineDepthOcclusionFactor(
                            layerId,
                            options.lineDepthOcclusionFactor
                        )
                    }
            }
        }
        if (options.displayViolatedSections) {
            if (!style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_1_VIOLATED)) {
                LineLayer(
                    RouteLayerConstants.LAYER_GROUP_1_VIOLATED,
                    RouteLayerConstants.LAYER_GROUP_1_SOURCE_ID
                )
                    .lineWidth(options.resourceProvider.violatedSectionLineWidth)
                    .lineJoin(LineJoin.ROUND)
                    .lineOpacity(options.resourceProvider.violatedSectionOpacity)
                    .lineColor(options.resourceProvider.routeLineColorResources.violatedSectionColor)
                    .lineDasharray(options.resourceProvider.violatedSectionDashArray)
                    .lineCap(LineCap.ROUND)
                    .apply {
                        style.addPersistentLayer(this, LayerPosition(null, belowLayerIdToUse, null))
                        style.layerLineDepthOcclusionFactor(
                            layerId,
                            options.lineDepthOcclusionFactor
                        )
                    }
            }
        }

        if (!style.styleLayerExists(RouteLayerConstants.MASKING_LAYER_TRAIL_CASING)) {
            LineLayer(
                RouteLayerConstants.MASKING_LAYER_TRAIL_CASING,
                RouteLayerConstants.LAYER_GROUP_1_SOURCE_ID
            )
                .lineCap(LineCap.ROUND)
                .lineJoin(LineJoin.ROUND)
                .lineWidth(options.resourceProvider.routeCasingLineScaleExpression)
                .lineColor(Color.GRAY).apply {
                    style.addPersistentLayer(this, LayerPosition(null, belowLayerIdToUse, null))
                    style.layerLineDepthOcclusionFactor(layerId, options.lineDepthOcclusionFactor)
                }
        }
        if (!style.styleLayerExists(RouteLayerConstants.MASKING_LAYER_TRAIL)) {
            LineLayer(
                RouteLayerConstants.MASKING_LAYER_TRAIL,
                RouteLayerConstants.LAYER_GROUP_1_SOURCE_ID
            )
                .lineCap(LineCap.ROUND)
                .lineJoin(LineJoin.ROUND)
                .lineWidth(options.resourceProvider.routeLineScaleExpression)
                .lineColor(Color.GRAY).apply {
                    style.addPersistentLayer(this, LayerPosition(null, belowLayerIdToUse, null))
                    style.layerLineDepthOcclusionFactor(layerId, options.lineDepthOcclusionFactor)
                }
        }
        if (!style.styleLayerExists(RouteLayerConstants.MASKING_LAYER_CASING)) {
            LineLayer(
                RouteLayerConstants.MASKING_LAYER_CASING,
                RouteLayerConstants.LAYER_GROUP_1_SOURCE_ID
            )
                .lineCap(LineCap.ROUND)
                .lineJoin(LineJoin.ROUND)
                .lineWidth(options.resourceProvider.routeCasingLineScaleExpression)
                .lineColor(Color.GRAY).apply {
                    style.addPersistentLayer(this, LayerPosition(null, belowLayerIdToUse, null))
                    style.layerLineDepthOcclusionFactor(layerId, options.lineDepthOcclusionFactor)
                }
        }
        if (!style.styleLayerExists(RouteLayerConstants.MASKING_LAYER_MAIN)) {
            LineLayer(
                RouteLayerConstants.MASKING_LAYER_MAIN,
                RouteLayerConstants.LAYER_GROUP_1_SOURCE_ID
            )
                .lineCap(LineCap.ROUND)
                .lineJoin(LineJoin.ROUND)
                .lineWidth(options.resourceProvider.routeLineScaleExpression)
                .lineColor(Color.GRAY).apply {
                    style.addPersistentLayer(this, LayerPosition(null, belowLayerIdToUse, null))
                    style.layerLineDepthOcclusionFactor(layerId, options.lineDepthOcclusionFactor)
                }
        }
        if (!style.styleLayerExists(RouteLayerConstants.MASKING_LAYER_TRAFFIC)) {
            LineLayer(
                RouteLayerConstants.MASKING_LAYER_TRAFFIC,
                RouteLayerConstants.LAYER_GROUP_1_SOURCE_ID
            )
                .lineCap(LineCap.ROUND)
                .lineJoin(LineJoin.ROUND)
                .lineWidth(options.resourceProvider.routeTrafficLineScaleExpression)
                .lineColor(Color.GRAY).apply {
                    style.addPersistentLayer(this, LayerPosition(null, belowLayerIdToUse, null))
                    style.layerLineDepthOcclusionFactor(layerId, options.lineDepthOcclusionFactor)
                }
        }
        if (options.displayRestrictedRoadSections) {
            if (!style.styleLayerExists(RouteLayerConstants.MASKING_LAYER_RESTRICTED)) {
                LineLayer(
                    RouteLayerConstants.MASKING_LAYER_RESTRICTED,
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
                        style.layerLineDepthOcclusionFactor(
                            layerId,
                            options.lineDepthOcclusionFactor
                        )
                    }
            }
        }
        if (options.displayViolatedSections) {
            if (!style.styleLayerExists(RouteLayerConstants.MASKING_LAYER_VIOLATED)) {
                LineLayer(
                    RouteLayerConstants.MASKING_LAYER_VIOLATED,
                    RouteLayerConstants.LAYER_GROUP_1_SOURCE_ID
                )
                    .lineWidth(options.resourceProvider.violatedSectionLineWidth)
                    .lineJoin(LineJoin.ROUND)
                    .lineOpacity(options.resourceProvider.violatedSectionOpacity)
                    .lineColor(options.resourceProvider.routeLineColorResources.violatedSectionColor)
                    .lineDasharray(options.resourceProvider.violatedSectionDashArray)
                    .lineCap(LineCap.ROUND)
                    .apply {
                        style.addPersistentLayer(this, LayerPosition(null, belowLayerIdToUse, null))
                        style.layerLineDepthOcclusionFactor(
                            layerId,
                            options.lineDepthOcclusionFactor
                        )
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

        if (!style.hasStyleImage(RouteLayerConstants.ORIGIN_MARKER_NAME)) {
            options.originIcon.getBitmap().let {
                style.addImage(RouteLayerConstants.ORIGIN_MARKER_NAME, it)
            }
        }
        if (!style.hasStyleImage(RouteLayerConstants.DESTINATION_MARKER_NAME)) {
            options.destinationIcon.getBitmap().let {
                style.addImage(RouteLayerConstants.DESTINATION_MARKER_NAME, it)
            }
        }
        if (!style.styleLayerExists(RouteLayerConstants.WAYPOINT_LAYER_ID)) {
            style.addPersistentLayer(
                SymbolLayer(
                    RouteLayerConstants.WAYPOINT_LAYER_ID,
                    RouteLayerConstants.WAYPOINT_SOURCE_ID
                )
                    .iconOffset(options.waypointLayerIconOffset)
                    .iconAnchor(options.waypointLayerIconAnchor)
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
                    .iconPitchAlignment(options.iconPitchAlignment)
                    .iconAllowOverlap(true)
                    .iconIgnorePlacement(true)
                    .iconKeepUpright(true),
                LayerPosition(null, belowLayerIdToUse, null)
            )
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
            style.styleLayerExists(RouteLayerConstants.MASKING_LAYER_TRAIL_CASING) &&
            style.styleLayerExists(RouteLayerConstants.MASKING_LAYER_TRAIL) &&
            style.styleLayerExists(RouteLayerConstants.MASKING_LAYER_CASING) &&
            style.styleLayerExists(RouteLayerConstants.MASKING_LAYER_MAIN) &&
            style.styleLayerExists(RouteLayerConstants.MASKING_LAYER_TRAFFIC) &&
            if (options.displayRestrictedRoadSections) {
                style.styleLayerExists(RouteLayerConstants.MASKING_LAYER_RESTRICTED) &&
                    style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_1_RESTRICTED) &&
                    style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_2_RESTRICTED) &&
                    style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_3_RESTRICTED)
            } else {
                true
            } &&
            if (options.displayViolatedSections) {
                style.styleLayerExists(RouteLayerConstants.MASKING_LAYER_VIOLATED) &&
                    style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_1_VIOLATED) &&
                    style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_2_VIOLATED) &&
                    style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_3_VIOLATED)
            } else {
                true
            }
    }

    internal fun getTrafficLineExpressionProducer(
        route: NavigationRoute,
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
        val stopGap = softGradientTransitionDistance / route.directionsRoute.distance()
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
        routeData: List<ExtractedRouteRestrictionData>,
        vanishingPointOffset: Double,
        activeLegIndex: Int,
        @ColorInt color: Int,
    ) = RouteLineExpressionProvider {
        getRestrictedLineExpression(
            vanishingPointOffset,
            activeLegIndex,
            color,
            routeData
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
        routeLineExpressionData: List<ExtractedRouteRestrictionData>
    ): Expression {
        var lastColor = Int.MAX_VALUE
        val expressionBuilder = Expression.ExpressionBuilder("step")
        expressionBuilder.lineProgress()
        expressionBuilder.color(Color.TRANSPARENT)

        getFilteredRouteLineExpressionData(
            vanishingPointOffset,
            routeLineExpressionData,
            defaultObjectCreator = {
                ExtractedRouteRestrictionData(vanishingPointOffset)
            }
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

    internal fun trimRouteDataCacheToSize(size: Int) {
        extractRouteDataCache.trimToSize(size)
        granularDistancesCache.trimToSize(size)
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
                .filter { it.id !in maskingLayerIds }
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

    /**
     * Returns a percentage offset of the fork intersection along the alternative route's geometry.
     */
    internal fun getAlternativeRouteDeviationOffsets(
        metadata: AlternativeRouteMetadata,
        distancesProvider: (NavigationRoute) -> RouteLineGranularDistances? =
            granularDistancesProvider
    ): Double {
        return distancesProvider(metadata.navigationRoute)?.let { distances ->
            if (distances.routeDistances.isEmpty() || distances.completeDistance <= 0) {
                logW(
                    "Remaining distances array size is ${distances.routeDistances.size} " +
                        "and the full distance is ${distances.completeDistance} - " +
                        "unable to calculate the deviation point of the alternative with ID " +
                        "'${metadata.navigationRoute.id}' to hide the portion that overlaps " +
                        "with the primary route.",
                    LOG_CATEGORY
                )
                return@let 0.0
            }
            val index = metadata.forkIntersectionOfAlternativeRoute.geometryIndexInRoute
            val distanceRemaining = distances.routeDistances.getOrElse(index) {
                logW(
                    "Remaining distance at index '$it' requested but there are " +
                        "${distances.routeDistances.size} elements in the distances array - " +
                        "unable to calculate the deviation point of the alternative with ID " +
                        "'${metadata.navigationRoute.id}' to hide the portion that overlaps " +
                        "with the primary route.",
                    LOG_CATEGORY
                )
                return@let 0.0
            }.distanceRemaining
            if (distanceRemaining > distances.completeDistance) {
                logW(
                    "distance remaining > full distance - " +
                        "unable to calculate the deviation point of the alternative with ID " +
                        "'${metadata.navigationRoute.id}' to hide the portion that overlaps " +
                        "with the primary route.",
                    LOG_CATEGORY
                )
                return@let 0.0
            }
            1.0 - distanceRemaining / distances.completeDistance
        } ?: 0.0
    }
    internal fun getMaskingLayerDynamicData(
        route: NavigationRoute?,
        offset: Double
    ): RouteLineDynamicData? {
        return if (
            (route?.directionsRoute?.legs()?.size ?: 1) > 1
        ) {
            val trimmedOffsetExpression = literal(
                listOf(
                    0.0,
                    offset
                )
            )
            RouteLineDynamicData(
                baseExpressionProvider =
                RouteLineTrimExpressionProvider { trimmedOffsetExpression },
                casingExpressionProvider =
                RouteLineTrimExpressionProvider { trimmedOffsetExpression },
                trafficExpressionProvider =
                RouteLineTrimExpressionProvider { trimmedOffsetExpression },
                restrictedSectionExpressionProvider =
                RouteLineTrimExpressionProvider { trimmedOffsetExpression },
                violatedSectionExpressionProvider =
                RouteLineTrimExpressionProvider { trimmedOffsetExpression },
                trailExpressionProvider = { trimmedOffsetExpression },
                trailCasingExpressionProvider = { trimmedOffsetExpression },
            )
        } else {
            null
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

    internal fun removeLayers(style: Style) {
        style.removeStyleLayer(RouteLayerConstants.TOP_LEVEL_ROUTE_LINE_LAYER_ID)
        style.removeStyleLayer(RouteLayerConstants.BOTTOM_LEVEL_ROUTE_LINE_LAYER_ID)
        style.removeStyleLayer(RouteLayerConstants.LAYER_GROUP_1_TRAIL_CASING)
        style.removeStyleLayer(RouteLayerConstants.LAYER_GROUP_1_TRAIL)
        style.removeStyleLayer(RouteLayerConstants.LAYER_GROUP_1_CASING)
        style.removeStyleLayer(RouteLayerConstants.LAYER_GROUP_1_MAIN)
        style.removeStyleLayer(RouteLayerConstants.LAYER_GROUP_1_TRAFFIC)
        style.removeStyleLayer(RouteLayerConstants.LAYER_GROUP_1_RESTRICTED)
        style.removeStyleLayer(RouteLayerConstants.LAYER_GROUP_1_VIOLATED)
        style.removeStyleLayer(RouteLayerConstants.LAYER_GROUP_2_TRAIL_CASING)
        style.removeStyleLayer(RouteLayerConstants.LAYER_GROUP_2_TRAIL)
        style.removeStyleLayer(RouteLayerConstants.LAYER_GROUP_2_CASING)
        style.removeStyleLayer(RouteLayerConstants.LAYER_GROUP_2_MAIN)
        style.removeStyleLayer(RouteLayerConstants.LAYER_GROUP_2_TRAFFIC)
        style.removeStyleLayer(RouteLayerConstants.LAYER_GROUP_2_RESTRICTED)
        style.removeStyleLayer(RouteLayerConstants.LAYER_GROUP_2_VIOLATED)
        style.removeStyleLayer(RouteLayerConstants.LAYER_GROUP_3_TRAIL_CASING)
        style.removeStyleLayer(RouteLayerConstants.LAYER_GROUP_3_TRAIL)
        style.removeStyleLayer(RouteLayerConstants.LAYER_GROUP_3_CASING)
        style.removeStyleLayer(RouteLayerConstants.LAYER_GROUP_3_MAIN)
        style.removeStyleLayer(RouteLayerConstants.LAYER_GROUP_3_TRAFFIC)
        style.removeStyleLayer(RouteLayerConstants.LAYER_GROUP_3_RESTRICTED)
        style.removeStyleLayer(RouteLayerConstants.LAYER_GROUP_3_VIOLATED)
        style.removeStyleLayer(RouteLayerConstants.MASKING_LAYER_TRAIL_CASING)
        style.removeStyleLayer(RouteLayerConstants.MASKING_LAYER_TRAIL)
        style.removeStyleLayer(RouteLayerConstants.MASKING_LAYER_CASING)
        style.removeStyleLayer(RouteLayerConstants.MASKING_LAYER_MAIN)
        style.removeStyleLayer(RouteLayerConstants.MASKING_LAYER_TRAFFIC)
        style.removeStyleLayer(RouteLayerConstants.MASKING_LAYER_RESTRICTED)
        style.removeStyleLayer(RouteLayerConstants.MASKING_LAYER_VIOLATED)
        style.removeStyleLayer(RouteLayerConstants.WAYPOINT_LAYER_ID)
        style.removeStyleImage(RouteLayerConstants.ORIGIN_MARKER_NAME)
        style.removeStyleImage(RouteLayerConstants.DESTINATION_MARKER_NAME)
    }
}

private fun Style.layerLineDepthOcclusionFactor(layerId: String, factor: Double) {
    setStyleLayerProperty(layerId, "line-depth-occlusion-factor", Value(factor))
}
