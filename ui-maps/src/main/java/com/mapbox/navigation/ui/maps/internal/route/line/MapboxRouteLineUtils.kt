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
import com.mapbox.maps.MapboxExperimental
import com.mapbox.maps.Style
import com.mapbox.maps.StyleObjectInfo
import com.mapbox.maps.StylePropertyValue
import com.mapbox.maps.StylePropertyValueKind
import com.mapbox.maps.extension.style.expressions.dsl.generated.color
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
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
import com.mapbox.navigation.ui.maps.route.line.api.HeavyRouteLineExpressionValueProvider
import com.mapbox.navigation.ui.maps.route.line.api.LightRouteLineExpressionValueProvider
import com.mapbox.navigation.ui.maps.route.line.api.LineGradientCommandApplier
import com.mapbox.navigation.ui.maps.route.line.api.RouteLineValueCommandHolder
import com.mapbox.navigation.ui.maps.route.line.model.ExpressionOffsetData
import com.mapbox.navigation.ui.maps.route.line.model.ExtractedRouteData
import com.mapbox.navigation.ui.maps.route.line.model.ExtractedRouteRestrictionData
import com.mapbox.navigation.ui.maps.route.line.model.InactiveRouteColors
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineApiOptions
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineViewOptions
import com.mapbox.navigation.ui.maps.route.line.model.NavigationRouteLine
import com.mapbox.navigation.ui.maps.route.line.model.RouteFeatureData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineDistancesIndex
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineDynamicData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineExpressionData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineGranularDistances
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineScaleValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineSourceKey
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineTrimOffset
import com.mapbox.navigation.ui.maps.route.line.model.SegmentColorType
import com.mapbox.navigation.ui.maps.route.model.FadingConfig
import com.mapbox.navigation.ui.maps.util.CacheResultUtils
import com.mapbox.navigation.ui.maps.util.CacheResultUtils.cacheResult
import com.mapbox.navigation.ui.maps.util.CacheResultUtils.cacheRouteResult
import com.mapbox.navigation.ui.maps.util.CacheResultUtils.cacheRouteTrafficResult
import com.mapbox.navigation.ui.utils.internal.extensions.getBitmap
import com.mapbox.navigation.ui.utils.internal.ifNonNull
import com.mapbox.navigation.utils.internal.logE
import com.mapbox.navigation.utils.internal.logW
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import com.mapbox.turf.TurfMisc
import kotlinx.coroutines.CoroutineScope
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import com.mapbox.navigation.ui.maps.util.StyleManager as SdkStyleManager

internal object MapboxRouteLineUtils {

    private const val LOG_CATEGORY = "MapboxRouteLineUtils"
    internal const val VANISH_POINT_STOP_GAP = .00000000001
    private const val NUMBER_OF_SUPPORTED_ROUTES = 3

    internal val extractRouteDataCache: LruCache<
        CacheResultUtils.CacheResultKeyRouteTraffic<
            List<ExtractedRouteData>,
            >,
        List<ExtractedRouteData>,
        >
        by lazy { LruCache(NUMBER_OF_SUPPORTED_ROUTES) }

    private val granularDistancesCache: LruCache<
        CacheResultUtils.CacheResultKeyRoute<
            RouteLineGranularDistances?,
            >,
        RouteLineGranularDistances?,
        >
        by lazy { LruCache(NUMBER_OF_SUPPORTED_ROUTES) }

    val layerGroup1SourceKey = RouteLineSourceKey(RouteLayerConstants.LAYER_GROUP_1_SOURCE_ID)
    val layerGroup2SourceKey = RouteLineSourceKey(RouteLayerConstants.LAYER_GROUP_2_SOURCE_ID)
    val layerGroup3SourceKey = RouteLineSourceKey(RouteLayerConstants.LAYER_GROUP_3_SOURCE_ID)

    // ordering is important
    val layerGroup1SourceLayerIds = setOf(
        RouteLayerConstants.LAYER_GROUP_1_TRAIL_CASING,
        RouteLayerConstants.LAYER_GROUP_1_TRAIL,
        RouteLayerConstants.LAYER_GROUP_1_BLUR,
        RouteLayerConstants.LAYER_GROUP_1_CASING,
        RouteLayerConstants.LAYER_GROUP_1_MAIN,
        RouteLayerConstants.LAYER_GROUP_1_TRAFFIC,
        RouteLayerConstants.LAYER_GROUP_1_RESTRICTED,
    )

    // ordering is important
    val layerGroup2SourceLayerIds = setOf(
        RouteLayerConstants.LAYER_GROUP_2_TRAIL_CASING,
        RouteLayerConstants.LAYER_GROUP_2_TRAIL,
        RouteLayerConstants.LAYER_GROUP_2_BLUR,
        RouteLayerConstants.LAYER_GROUP_2_CASING,
        RouteLayerConstants.LAYER_GROUP_2_MAIN,
        RouteLayerConstants.LAYER_GROUP_2_TRAFFIC,
        RouteLayerConstants.LAYER_GROUP_2_RESTRICTED,
    )

    // ordering is important
    val layerGroup3SourceLayerIds = setOf(
        RouteLayerConstants.LAYER_GROUP_3_TRAIL_CASING,
        RouteLayerConstants.LAYER_GROUP_3_TRAIL,
        RouteLayerConstants.LAYER_GROUP_3_BLUR,
        RouteLayerConstants.LAYER_GROUP_3_CASING,
        RouteLayerConstants.LAYER_GROUP_3_MAIN,
        RouteLayerConstants.LAYER_GROUP_3_TRAFFIC,
        RouteLayerConstants.LAYER_GROUP_3_RESTRICTED,
    )

    // ordering is important
    val maskingLayerIds = setOf(
        RouteLayerConstants.MASKING_LAYER_TRAIL_CASING,
        RouteLayerConstants.MASKING_LAYER_TRAIL,
        RouteLayerConstants.MASKING_LAYER_CASING,
        RouteLayerConstants.MASKING_LAYER_MAIN,
        RouteLayerConstants.MASKING_LAYER_TRAFFIC,
        RouteLayerConstants.MASKING_LAYER_RESTRICTED,
    )

    val sourceLayerMap = mapOf<RouteLineSourceKey, Set<String>>(
        Pair(layerGroup1SourceKey, layerGroup1SourceLayerIds),
        Pair(layerGroup2SourceKey, layerGroup2SourceLayerIds),
        Pair(layerGroup3SourceKey, layerGroup3SourceLayerIds),
    )

    /**
     * Creates an [Expression] that can be applied to the layer style changing the appearance of
     * a route line, making the portion of the route line behind the puck invisible.
     *
     * @param distanceOffset the percentage of the distance traveled which will represent
     * the part of the route line that isn't visible
     * @param lineStartColor the starting color for the line gradient. This is usually transparent
     * and steps are added indicating a color change.
     * @param lineColorType a default line color
     * @param routeLineExpressionData a collection of [RouteLineExpressionData]. Generally an
     * [Expression] step should be created for each item in the collection subject to some
     * filtering.
     *
     * @return the Expression that can be used in a Layer's properties.
     */
    private fun getTrafficLineExpression(
        dynamicOptions: RouteLineViewOptionsData,
        distanceOffset: Double,
        lineStartColor: Int,
        lineColorType: SegmentColorType,
        routeLineExpressionData: List<RouteLineExpressionData>,
    ): Expression {
        var lastColor = Int.MAX_VALUE
        val expressionBuilder = Expression.ExpressionBuilder("step")
        expressionBuilder.lineProgress()
        expressionBuilder.color(lineStartColor)

        getFilteredRouteLineExpressionData(
            distanceOffset,
            routeLineExpressionData,
            defaultObjectCreator = {
                RouteLineExpressionData(distanceOffset, congestionValue = "", lineColorType, 0)
            },
        ).forEach {
            // If the color hasn't changed there's no reason to add it to the expression. A smaller
            // expression is less work for the map to process.
            val segmentColor = it.segmentColorType.getColor(dynamicOptions)
            if (segmentColor != lastColor) {
                lastColor = segmentColor
                expressionBuilder.stop {
                    literal(it.offset)
                    color(segmentColor)
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
    private fun getTrafficLineExpressionSoftGradient(
        dynamicOptions: RouteLineViewOptionsData,
        distanceOffset: Double,
        lineStartColor: Int,
        lineColorType: SegmentColorType,
        softGradientStopGap: Double,
        routeLineExpressionData: List<RouteLineExpressionData>,
    ): Expression {
        val vanishPointStopGap = VANISH_POINT_STOP_GAP
        val expressionBuilder = Expression.InterpolatorBuilder("interpolate")
        expressionBuilder.linear()
        expressionBuilder.lineProgress()

        val filteredItems = getFilteredRouteLineExpressionData(
            distanceOffset,
            routeLineExpressionData,
            defaultObjectCreator = {
                RouteLineExpressionData(distanceOffset, congestionValue = "", lineColorType, 0)
            },
        )
        var prevIndex = 0
        for (index in filteredItems.indices) {
            val expressionData = filteredItems[index]
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
                    color(expressionData.segmentColorType.getColor(dynamicOptions))
                }
            } else {
                val curColor = expressionData.segmentColorType.getColor(dynamicOptions)
                val prevColor = filteredItems[prevIndex].segmentColorType.getColor(dynamicOptions)
                if (prevColor != curColor) {
                    // ignoring offset points that are at the end of a route
                    // as that would create a gradient slope towards the last point of the route
                    // which is not necessary
                    if (expressionData.offset < 1.0) {
                        val stopGapOffset = expressionData.offset - softGradientStopGap
                        val stopGapOffsetToUse =
                            if (stopGapOffset > filteredItems[prevIndex].offset) {
                                stopGapOffset
                            } else {
                                filteredItems[prevIndex].offset + vanishPointStopGap
                            }

                        expressionBuilder.stop {
                            literal(stopGapOffsetToUse)
                            color(
                                filteredItems[prevIndex].segmentColorType.getColor(dynamicOptions),
                            )
                        }

                        expressionBuilder.stop {
                            literal(expressionData.offset)
                            color(expressionData.segmentColorType.getColor(dynamicOptions))
                        }
                        prevIndex = index
                    }
                }
            }
        }
        return expressionBuilder.build()
    }

    /**
     * Returns an [Expression] for a gradient line that will use [defaultColor] for the current active leg
     * and all previous legs. All legs after the current leg will use [substitutionColor].
     */
    internal fun getExpressionSubstitutingColorForUpcomingLegs(
        routeLineExpressionData: List<RouteLineExpressionData>,
        defaultColor: Int,
        substitutionColor: Int,
        activeLegIndex: Int,
    ) = getRouteLineExpression(
        distanceOffset = 0.0,
        routeLineExpressionData = routeLineExpressionData,
        lineBaseColor = defaultColor,
        defaultColor = defaultColor,
        substitutionColor = substitutionColor,
        shouldSubstituteColor = { legIndex ->
            legIndex > activeLegIndex
        },
    )

    /**
     * Returns an [Expression] for a gradient line that will start with the [lineBaseColor],
     * creating the first step at the [distanceOffset] with additional steps
     * according to the items in the [routeLineExpressionData].
     *
     * If the [activeLegIndex] is greater than or equal to zero a color substitution will
     * take place in the expression if the underlying data's leg index parameter does not
     * equal the [activeLegIndex]. This was added for the feature allowing for alternate
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
    internal fun getExpressionSubstitutingColorForInactiveLegs(
        distanceOffset: Double,
        routeLineExpressionData: List<RouteLineExpressionData>,
        lineBaseColor: Int,
        defaultColor: Int,
        substitutionColor: Int,
        activeLegIndex: Int,
    ) = getRouteLineExpression(
        distanceOffset,
        routeLineExpressionData,
        lineBaseColor,
        defaultColor,
        substitutionColor,
        shouldSubstituteColor = { legIndex ->
            activeLegIndex >= 0 && legIndex != activeLegIndex
        },
    )

    private fun getRouteLineExpression(
        distanceOffset: Double,
        routeLineExpressionData: List<RouteLineExpressionData>,
        lineBaseColor: Int,
        defaultColor: Int,
        substitutionColor: Int,
        shouldSubstituteColor: (Int) -> Boolean,
    ): StylePropertyValue {
        var lastColor = Int.MAX_VALUE
        val expressionBuilder = Expression.ExpressionBuilder("step")
        expressionBuilder.lineProgress()
        expressionBuilder.color(lineBaseColor)

        getFilteredRouteLineExpressionData(
            distanceOffset,
            routeLineExpressionData,
            defaultObjectCreator = {
                // colors are unused
                RouteLineExpressionData(
                    distanceOffset,
                    congestionValue = "",
                    SegmentColorType.PRIMARY_DEFAULT,
                    0,
                )
            },
        ).forEach {
            val colorToUse = if (shouldSubstituteColor(it.legIndex)) {
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
        return StylePropertyValue(expressionBuilder.build(), StylePropertyValueKind.EXPRESSION)
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
        lineBaseColor: Int,
    ): StylePropertyValue {
        val expressionBuilder = Expression.ExpressionBuilder("step")
        expressionBuilder.lineProgress()
        expressionBuilder.color(traveledColor)
        expressionBuilder.stop {
            literal(offset)
            color(lineBaseColor)
        }
        return StylePropertyValue(expressionBuilder.build(), StylePropertyValueKind.EXPRESSION)
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
        defaultObjectCreator: () -> T,
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
        directionsRoutes: List<NavigationRoute>,
    ): () -> List<RouteFeatureData> = {
        directionsRoutes.map(::generateFeatureCollection)
    }

    fun getRouteLineFeatureDataProvider(
        directionsRoutes: List<NavigationRouteLine>,
    ): () -> List<RouteFeatureData> = {
        directionsRoutes.map(::generateFeatureCollection)
    }

    internal fun resolveNumericToValue(
        congestionValue: Int?,
        staticOptions: MapboxRouteLineApiOptions,
    ): String {
        return when (congestionValue) {
            in staticOptions.lowCongestionRange -> {
                RouteLayerConstants.LOW_CONGESTION_VALUE
            }

            in staticOptions.heavyCongestionRange -> {
                RouteLayerConstants.HEAVY_CONGESTION_VALUE
            }

            in staticOptions.severeCongestionRange -> {
                RouteLayerConstants.SEVERE_CONGESTION_VALUE
            }

            in staticOptions.moderateCongestionRange -> {
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
    fun getRouteColorTypeForCongestion(
        congestionValue: String,
        isPrimaryRoute: Boolean,
    ): SegmentColorType {
        return when (isPrimaryRoute) {
            true -> when (congestionValue) {
                RouteLayerConstants.LOW_CONGESTION_VALUE -> {
                    SegmentColorType.PRIMARY_LOW_CONGESTION
                }

                RouteLayerConstants.MODERATE_CONGESTION_VALUE -> {
                    SegmentColorType.PRIMARY_MODERATE_CONGESTION
                }

                RouteLayerConstants.HEAVY_CONGESTION_VALUE -> {
                    SegmentColorType.PRIMARY_HEAVY_CONGESTION
                }

                RouteLayerConstants.SEVERE_CONGESTION_VALUE -> {
                    SegmentColorType.PRIMARY_SEVERE_CONGESTION
                }

                RouteLayerConstants.UNKNOWN_CONGESTION_VALUE -> {
                    SegmentColorType.PRIMARY_UNKNOWN_CONGESTION
                }

                RouteLayerConstants.CLOSURE_CONGESTION_VALUE -> {
                    SegmentColorType.PRIMARY_CLOSURE
                }

                RouteLayerConstants.RESTRICTED_CONGESTION_VALUE -> {
                    SegmentColorType.PRIMARY_RESTRICTED
                }

                else -> SegmentColorType.PRIMARY_DEFAULT
            }

            false -> when (congestionValue) {
                RouteLayerConstants.LOW_CONGESTION_VALUE -> {
                    SegmentColorType.ALTERNATIVE_LOW_CONGESTION
                }

                RouteLayerConstants.MODERATE_CONGESTION_VALUE -> {
                    SegmentColorType.ALTERNATIVE_MODERATE_CONGESTION
                }

                RouteLayerConstants.HEAVY_CONGESTION_VALUE -> {
                    SegmentColorType.ALTERNATIVE_HEAVY_CONGESTION
                }

                RouteLayerConstants.SEVERE_CONGESTION_VALUE -> {
                    SegmentColorType.ALTERNATIVE_SEVERE_CONGESTION
                }

                RouteLayerConstants.UNKNOWN_CONGESTION_VALUE -> {
                    SegmentColorType.ALTERNATIVE_UNKNOWN_CONGESTION
                }

                RouteLayerConstants.CLOSURE_CONGESTION_VALUE -> {
                    SegmentColorType.ALTERNATIVE_CLOSURE
                }

                RouteLayerConstants.RESTRICTED_CONGESTION_VALUE -> {
                    SegmentColorType.ALTERNATIVE_RESTRICTED
                }

                else -> {
                    SegmentColorType.ALTERNATIVE_DEFAULT
                }
            }
        }
    }

    /**
     * Returns the color that is used to represent traffic congestion on inactive legs of the primary route.
     */
    fun getCongestionColorTypeForInactiveRouteLegs(
        congestionValue: String,
        colors: InactiveRouteColors,
    ): SegmentColorType = when (congestionValue) {
        RouteLayerConstants.LOW_CONGESTION_VALUE -> {
            colors.inactiveRouteLegLowCongestionColorType
        }

        RouteLayerConstants.MODERATE_CONGESTION_VALUE -> {
            colors.inactiveRouteLegModerateCongestionColorType
        }

        RouteLayerConstants.HEAVY_CONGESTION_VALUE -> {
            colors.inactiveRouteLegHeavyCongestionColorType
        }

        RouteLayerConstants.SEVERE_CONGESTION_VALUE -> {
            colors.inactiveRouteLegSevereCongestionColorType
        }

        RouteLayerConstants.UNKNOWN_CONGESTION_VALUE -> {
            colors.inactiveRouteLegUnknownCongestionColorType
        }

        RouteLayerConstants.CLOSURE_CONGESTION_VALUE -> {
            colors.inactiveRouteLegClosureColorType
        }

        RouteLayerConstants.RESTRICTED_CONGESTION_VALUE -> {
            colors.inactiveRouteLegRestrictedRoadColorType
        }

        else -> colors.inactiveRouteLegUnknownCongestionColorType
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
        options: MapboxRouteLineApiOptions,
    ): List<RouteLineExpressionData> {
        val congestionProvider =
            getTrafficCongestionAnnotationProvider(route, options)
        val annotationExpressionData = extractRouteDataWithTrafficAndRoadClassDeDuped(
            route,
            congestionProvider,
        )

        return when (annotationExpressionData.isEmpty()) {
            false -> {
                getRouteLineExpressionDataWithStreetClassOverride(
                    annotationExpressionData,
                    isPrimaryRoute,
                    trafficBackfillRoadClasses,
                )
            }

            true -> listOf(
                RouteLineExpressionData(
                    0.0,
                    "",
                    getRouteColorTypeForCongestion(
                        "",
                        isPrimaryRoute,
                    ),
                    0,
                ),
            )
        }
    }

    /**
     * Extracts data from the [DirectionsRoute] and removes items that are deemed duplicates based
     * on factors such as traffic congestion and/or road class.
     */
    internal val extractRouteDataWithTrafficAndRoadClassDeDuped: (
        route: NavigationRoute,
        trafficCongestionProvider: (RouteLeg) -> List<String>?,
    ) -> List<ExtractedRouteData> =
        { route: NavigationRoute, trafficCongestionProvider: (RouteLeg) -> List<String>? ->
            val extractedRouteDataItems = extractRouteData(
                route,
                trafficCongestionProvider,
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
        distancesProvider: (NavigationRoute) -> RouteLineGranularDistances?,
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
                                            legIndex,
                                        ),
                                    )
                                }
                            }
                    }
                }
            }
        }
        return itemsToReturn
    }

    /**
     * Filters the [RouteLeg] for intersections that are designated as restricted. If there are
     * no restricted intersections null is returned. If there is at least one restricted intersection
     * data is returned for that intersection plus data for intersections around the restricted
     * intersection.
     */
    private fun filterForRestrictedIntersections(
        leg: RouteLeg,
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
        trafficCongestionProvider: (RouteLeg) -> List<String>?,
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
                                isLegOrigin = index == 0,
                            ),
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
        options: MapboxRouteLineApiOptions,
    ) -> (RouteLeg) -> List<String> = { options: MapboxRouteLineApiOptions ->
        { routeLeg: RouteLeg ->
            routeLeg.annotation()?.congestionNumeric()?.map { v ->
                resolveNumericToValue(v, options)
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
        staticOptions: MapboxRouteLineApiOptions,
    ): (RouteLeg) -> List<String>? {
        return if (
            route.directionsRoute.routeOptions()
                ?.annotationsList()
                ?.contains(DirectionsCriteria.ANNOTATION_CONGESTION_NUMERIC) == true
        ) {
            getRouteLegTrafficNumericCongestionProvider(staticOptions)
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
        steps: List<LegStep>?,
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
                    intersectionsWithGeometryIndex.last().geometryIndex()!! + 1,
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
                                LOG_CATEGORY,
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
        isPrimaryRoute: Boolean,
        trafficOverrideRoadClasses: List<String>,
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

            val trafficColorType = getRouteColorTypeForCongestion(
                trafficIdentifier,
                isPrimaryRoute,
            )

            if (index == 0 || annotationExpData.isLegOrigin) {
                expressionDataToReturn.add(
                    RouteLineExpressionData(
                        annotationExpData.offset,
                        congestionValue = trafficIdentifier,
                        trafficColorType,
                        annotationExpData.legIndex,
                    ),
                )
            } else if (trafficColorType != expressionDataToReturn.last().segmentColorType) {
                expressionDataToReturn.add(
                    RouteLineExpressionData(
                        annotationExpData.offset,
                        congestionValue = trafficIdentifier,
                        trafficColorType,
                        annotationExpData.legIndex,
                    ),
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
        stepsPoints: List<List<List<Point>>>,
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
                    } as Array<RouteLineDistancesIndex>,
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
                },
            )
        }

        return RouteLineGranularDistances(
            distance,
            routeDistances = routeArray.toTypedArray(),
            legsDistances = legsArray.toTypedArray(),
            stepsDistances = stepsArray as Array<Array<Array<RouteLineDistancesIndex>>>,
        )
    }

    /* !Note: Feature id is always corresponding to NavigationRoute#id */
    private val generateRouteFeatureData: (
        route: NavigationRoute,
        identifier: String?,
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
            routeGeometry,
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
                        LOG_CATEGORY,
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
            (projectY(point1.latitude()) - projectY(point2.latitude())),
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
        upcomingIndex: Int,
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
            TurfConstants.UNIT_METERS,
        ).getNumberProperty("dist")?.toDouble() ?: 0.0
    }

    /**
     * Searches for the closest [RouteLineDistanceIndex] to the point
     * that the point hasn't passed along the route.
     *
     * For example if point is between index 2 and 3 of [RouteLineGranularDistances]
     * and closest to 2 but has already past the [RouteLineGranularDistances] at index
     * 2 along the route, the [RouteLineGranularDistances] at index 3 will be returned.
     */
    /*
        Prior to this the RouteProgress.currentRouteGeometryIndex was used
        to find the closest RouteLineGranularDistances to a point but there
        are cases where nav. native can emit values in non-consecutive order.
        Meaning values emitted can resemble 7-8-9-8-9-10. This would result
        in trim-offset values that would jump in value then decrement in value.
        The code below should resolve this issue by returning the same RouteLineGranularDistances
        index even in cases where the upcomingIndex parameter has incremented
        "erroneously".

        In order to determine which RouteLineDistanceIndex to return the bearing of the
        two RouteLineDistanceIndexes the point is between are compared to the bearing
        of the point to the preliminary RouteLineDistanceIndex. If the point hasn't passed
        the preliminary RouteLineDistanceIndex it should have a similar bearing.
     */
    internal fun findClosestRouteLineDistanceIndexToPoint(
        point: Point,
        granularDistances: RouteLineGranularDistances,
        upcomingIndex: Int,
    ): Int {
        val closest = getClosestRoutLineDistanceIndex(point, granularDistances, upcomingIndex)
        if (closest.first == 0) {
            return 1
        }

        val baseBearing = TurfMeasurement.bearing(
            granularDistances.routeDistances[closest.first - 1].point,
            granularDistances.routeDistances[closest.first].point,
        )
        val closesPointBearing = TurfMeasurement.bearing(
            point,
            granularDistances.routeDistances[closest.first].point,
        )
        return if (isWithin30Degrees(baseBearing, closesPointBearing)) {
            closest.first
        } else {
            if (closest.first < granularDistances.routeDistances.lastIndex) {
                closest.first + 1
            } else {
                closest.first
            }
        }
    }

    private fun getClosestRoutLineDistanceIndex(
        point: Point,
        granularDistances: RouteLineGranularDistances,
        upcomingIndex: Int,
    ): Pair<Int, Double> {
        val pointIndex = max(upcomingIndex - 10, 0)
        val initialDistance = TurfMeasurement.distance(
            granularDistances.routeDistances[pointIndex].point,
            point,
            TurfConstants.UNIT_METERS,
        )
        var closest = Pair(pointIndex, initialDistance)
        for (i in pointIndex..upcomingIndex) {
            val distance = TurfMeasurement.distance(
                granularDistances.routeDistances[i].point,
                point,
                TurfConstants.UNIT_METERS,
            )
            if (distance < closest.second) {
                closest = Pair(i, distance)
            }
        }
        return closest
    }

    /*
    Determines if two bearings have a range of 30 degrees or less.
     */
    private fun isWithin30Degrees(bearing1: Double, bearing2: Double): Boolean {
        val normalizedBearing1 = bearing1 % 360
        val normalizedBearing2 = bearing2 % 360
        val difference = abs(normalizedBearing1 - normalizedBearing2)

        return difference <= 30
    }

    internal fun buildScalingExpression(scalingValues: List<RouteLineScaleValue>): Expression {
        val expressionBuilder = Expression.ExpressionBuilder("interpolate")
        expressionBuilder.addArgument(Expression.exponential { literal(1.5) })
        expressionBuilder.zoom()
        scalingValues.forEach { routeLineScaleValue ->
            expressionBuilder.stop {
                literal(routeLineScaleValue.scaleStop.toDouble())
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
        properties: HashMap<String, Value>,
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
        enableSharedCache: Boolean,
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
                    },
                ),
            )
        }
    }

    @OptIn(MapboxExperimental::class, ExperimentalPreviewMapboxNavigationAPI::class)
    fun initializeLayers(
        style: Style,
        options: MapboxRouteLineViewOptions,
    ) {
        val styleContainsSlotName = style.styleSlots.contains(options.slotName).also {
            if (!it) {
                logE(LOG_CATEGORY) {
                    "The ${options.slotName} slot is not present in the style."
                }
            }
        }
        val belowLayerIdToUse: String? =
            getBelowLayerIdToUse(
                options.routeLineBelowLayerId,
                style,
            )

        val opacityExpression = options.opacityExpression()
        val restrictedRoadsOpacityExpression = options.restrictedRoadsOpacityExpression()

        style.addNewOrReuseSource(
            RouteLayerConstants.WAYPOINT_SOURCE_ID,
            options.tolerance,
            useLineMetrics = false,
            enableSharedCache = false,
        )
        style.addNewOrReuseSource(
            RouteLayerConstants.LAYER_GROUP_1_SOURCE_ID,
            options.tolerance,
            useLineMetrics = true,
            enableSharedCache = options.shareLineGeometrySources,
        )
        style.addNewOrReuseSource(
            RouteLayerConstants.LAYER_GROUP_2_SOURCE_ID,
            options.tolerance,
            useLineMetrics = true,
            enableSharedCache = options.shareLineGeometrySources,
        )
        style.addNewOrReuseSource(
            RouteLayerConstants.LAYER_GROUP_3_SOURCE_ID,
            options.tolerance,
            useLineMetrics = true,
            enableSharedCache = options.shareLineGeometrySources,
        )

        if (!style.styleLayerExists(RouteLayerConstants.BOTTOM_LEVEL_ROUTE_LINE_LAYER_ID)) {
            style.addPersistentLayer(
                BackgroundLayer(
                    RouteLayerConstants.BOTTOM_LEVEL_ROUTE_LINE_LAYER_ID,
                ).apply {
                    backgroundOpacity(0.0)
                    if (styleContainsSlotName) {
                        this.slot(options.slotName)
                    }
                },
                LayerPosition(null, belowLayerIdToUse, null),
            )
        }

        if (!style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_3_TRAIL_CASING)) {
            LineLayer(
                RouteLayerConstants.LAYER_GROUP_3_TRAIL_CASING,
                RouteLayerConstants.LAYER_GROUP_3_SOURCE_ID,
            )
                .lineCap(LineCap.ROUND)
                .lineJoin(LineJoin.ROUND)
                .lineWidth(options.scaleExpressions.routeCasingLineScaleExpression)
                .lineEmissiveStrength(1.0)
                .apply { opacityExpression?.let { lineOpacity(it) } }
                .lineColor(Color.GRAY).apply {
                    style.addPersistentLayer(this, LayerPosition(null, belowLayerIdToUse, null))
                    style.layerLineDepthOcclusionFactor(
                        layerId,
                        options.lineDepthOcclusionFactor,
                    )
                }
                .also {
                    if (styleContainsSlotName) {
                        it.slot(options.slotName)
                    }
                }
        }
        if (!style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_3_TRAIL)) {
            LineLayer(
                RouteLayerConstants.LAYER_GROUP_3_TRAIL,
                RouteLayerConstants.LAYER_GROUP_3_SOURCE_ID,
            )
                .lineCap(LineCap.ROUND)
                .lineJoin(LineJoin.ROUND)
                .lineWidth(options.scaleExpressions.routeLineScaleExpression)
                .lineEmissiveStrength(1.0)
                .apply { opacityExpression?.let { lineOpacity(it) } }
                .lineColor(Color.GRAY).apply {
                    style.addPersistentLayer(this, LayerPosition(null, belowLayerIdToUse, null))
                    style.layerLineDepthOcclusionFactor(
                        layerId,
                        options.lineDepthOcclusionFactor,
                    )
                }
                .also {
                    if (styleContainsSlotName) {
                        it.slot(options.slotName)
                    }
                }
        }
        if (!style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_3_BLUR)) {
            LineLayer(
                RouteLayerConstants.LAYER_GROUP_3_BLUR,
                RouteLayerConstants.LAYER_GROUP_3_SOURCE_ID,
            )
                .lineOpacity(options.routeLineBlurOpacity)
                .lineBlur(options.routeLineBlurWidth)
                .lineCap(LineCap.ROUND)
                .lineJoin(LineJoin.ROUND)
                .lineWidth(options.scaleExpressions.routeBlurScaleExpression)
                .lineEmissiveStrength(1.0)
                .lineColor(options.routeLineColorResources.blurColor).apply {
                    style.addPersistentLayer(this, LayerPosition(null, belowLayerIdToUse, null))
                    style.layerLineDepthOcclusionFactor(
                        layerId,
                        options.lineDepthOcclusionFactor,
                    )
                }
                .also {
                    if (styleContainsSlotName) {
                        it.slot(options.slotName)
                    }
                }
        }

        if (!style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_3_CASING)) {
            LineLayer(
                RouteLayerConstants.LAYER_GROUP_3_CASING,
                RouteLayerConstants.LAYER_GROUP_3_SOURCE_ID,
            )
                .lineCap(LineCap.ROUND)
                .lineJoin(LineJoin.ROUND)
                .lineWidth(options.scaleExpressions.routeCasingLineScaleExpression)
                .lineEmissiveStrength(1.0)
                .apply { opacityExpression?.let { lineOpacity(it) } }
                .lineColor(Color.GRAY).apply {
                    style.addPersistentLayer(this, LayerPosition(null, belowLayerIdToUse, null))
                    style.layerLineDepthOcclusionFactor(
                        layerId,
                        options.lineDepthOcclusionFactor,
                    )
                }
                .also {
                    if (styleContainsSlotName) {
                        it.slot(options.slotName)
                    }
                }
        }
        if (!style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_3_MAIN)) {
            LineLayer(
                RouteLayerConstants.LAYER_GROUP_3_MAIN,
                RouteLayerConstants.LAYER_GROUP_3_SOURCE_ID,
            )
                .lineCap(LineCap.ROUND)
                .lineJoin(LineJoin.ROUND)
                .lineWidth(options.scaleExpressions.routeLineScaleExpression)
                .lineEmissiveStrength(1.0)
                .apply { opacityExpression?.let { lineOpacity(it) } }
                .lineColor(Color.GRAY).apply {
                    style.addPersistentLayer(this, LayerPosition(null, belowLayerIdToUse, null))
                    style.layerLineDepthOcclusionFactor(
                        layerId,
                        options.lineDepthOcclusionFactor,
                    )
                }
                .also {
                    if (styleContainsSlotName) {
                        it.slot(options.slotName)
                    }
                }
        }
        if (!style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_3_TRAFFIC)) {
            LineLayer(
                RouteLayerConstants.LAYER_GROUP_3_TRAFFIC,
                RouteLayerConstants.LAYER_GROUP_3_SOURCE_ID,
            )
                .lineCap(LineCap.ROUND)
                .lineJoin(LineJoin.ROUND)
                .lineWidth(options.scaleExpressions.routeTrafficLineScaleExpression)
                .lineEmissiveStrength(1.0)
                .apply { opacityExpression?.let { lineOpacity(it) } }
                .lineColor(Color.GRAY).apply {
                    style.addPersistentLayer(this, LayerPosition(null, belowLayerIdToUse, null))
                    style.layerLineDepthOcclusionFactor(
                        layerId,
                        options.lineDepthOcclusionFactor,
                    )
                }
                .also {
                    if (styleContainsSlotName) {
                        it.slot(options.slotName)
                    }
                }
        }
        if (options.displayRestrictedRoadSections) {
            if (!style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_3_RESTRICTED)) {
                LineLayer(
                    RouteLayerConstants.LAYER_GROUP_3_RESTRICTED,
                    RouteLayerConstants.LAYER_GROUP_3_SOURCE_ID,
                )
                    .lineWidth(options.restrictedRoadLineWidth)
                    .lineJoin(LineJoin.ROUND)
                    .lineColor(options.routeLineColorResources.restrictedRoadColor)
                    .lineDasharray(options.restrictedRoadDashArray)
                    .lineCap(LineCap.ROUND)
                    .lineEmissiveStrength(1.0)
                    .lineOpacity(restrictedRoadsOpacityExpression)
                    .apply {
                        style.addPersistentLayer(this, LayerPosition(null, belowLayerIdToUse, null))
                        style.layerLineDepthOcclusionFactor(
                            layerId,
                            options.lineDepthOcclusionFactor,
                        )
                        if (styleContainsSlotName) {
                            this.slot(options.slotName)
                        }
                    }
            }
        }

        if (!style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_2_TRAIL_CASING)) {
            LineLayer(
                RouteLayerConstants.LAYER_GROUP_2_TRAIL_CASING,
                RouteLayerConstants.LAYER_GROUP_2_SOURCE_ID,
            )
                .lineCap(LineCap.ROUND)
                .lineJoin(LineJoin.ROUND)
                .lineWidth(options.scaleExpressions.routeCasingLineScaleExpression)
                .lineEmissiveStrength(1.0)
                .apply { opacityExpression?.let { lineOpacity(it) } }
                .lineColor(Color.GRAY).apply {
                    style.addPersistentLayer(this, LayerPosition(null, belowLayerIdToUse, null))
                    style.layerLineDepthOcclusionFactor(
                        layerId,
                        options.lineDepthOcclusionFactor,
                    )
                }
                .also {
                    if (styleContainsSlotName) {
                        it.slot(options.slotName)
                    }
                }
        }
        if (!style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_2_TRAIL)) {
            LineLayer(
                RouteLayerConstants.LAYER_GROUP_2_TRAIL,
                RouteLayerConstants.LAYER_GROUP_2_SOURCE_ID,
            )
                .lineCap(LineCap.ROUND)
                .lineJoin(LineJoin.ROUND)
                .lineWidth(options.scaleExpressions.routeLineScaleExpression)
                .lineEmissiveStrength(1.0)
                .apply { opacityExpression?.let { lineOpacity(it) } }
                .lineColor(Color.GRAY).apply {
                    style.addPersistentLayer(this, LayerPosition(null, belowLayerIdToUse, null))
                    style.layerLineDepthOcclusionFactor(
                        layerId,
                        options.lineDepthOcclusionFactor,
                    )
                }
                .also {
                    if (styleContainsSlotName) {
                        it.slot(options.slotName)
                    }
                }
        }
        if (!style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_2_BLUR)) {
            LineLayer(
                RouteLayerConstants.LAYER_GROUP_2_BLUR,
                RouteLayerConstants.LAYER_GROUP_2_SOURCE_ID,
            )
                .lineOpacity(options.routeLineBlurOpacity)
                .lineBlur(options.routeLineBlurWidth)
                .lineCap(LineCap.ROUND)
                .lineJoin(LineJoin.ROUND)
                .lineWidth(options.scaleExpressions.routeBlurScaleExpression)
                .lineEmissiveStrength(1.0)
                .lineColor(options.routeLineColorResources.blurColor).apply {
                    style.addPersistentLayer(this, LayerPosition(null, belowLayerIdToUse, null))
                    style.layerLineDepthOcclusionFactor(
                        layerId,
                        options.lineDepthOcclusionFactor,
                    )
                }
                .also {
                    if (styleContainsSlotName) {
                        it.slot(options.slotName)
                    }
                }
        }
        if (!style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_2_CASING)) {
            LineLayer(
                RouteLayerConstants.LAYER_GROUP_2_CASING,
                RouteLayerConstants.LAYER_GROUP_2_SOURCE_ID,
            )
                .lineCap(LineCap.ROUND)
                .lineJoin(LineJoin.ROUND)
                .lineWidth(options.scaleExpressions.routeCasingLineScaleExpression)
                .lineEmissiveStrength(1.0)
                .apply { opacityExpression?.let { lineOpacity(it) } }
                .lineColor(Color.GRAY).apply {
                    style.addPersistentLayer(this, LayerPosition(null, belowLayerIdToUse, null))
                    style.layerLineDepthOcclusionFactor(
                        layerId,
                        options.lineDepthOcclusionFactor,
                    )
                }
                .also {
                    if (styleContainsSlotName) {
                        it.slot(options.slotName)
                    }
                }
        }
        if (!style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_2_MAIN)) {
            LineLayer(
                RouteLayerConstants.LAYER_GROUP_2_MAIN,
                RouteLayerConstants.LAYER_GROUP_2_SOURCE_ID,
            )
                .lineCap(LineCap.ROUND)
                .lineJoin(LineJoin.ROUND)
                .lineWidth(options.scaleExpressions.routeLineScaleExpression)
                .lineEmissiveStrength(1.0)
                .apply { opacityExpression?.let { lineOpacity(it) } }
                .lineColor(Color.GRAY).apply {
                    style.addPersistentLayer(this, LayerPosition(null, belowLayerIdToUse, null))
                    style.layerLineDepthOcclusionFactor(
                        layerId,
                        options.lineDepthOcclusionFactor,
                    )
                }
                .also {
                    if (styleContainsSlotName) {
                        it.slot(options.slotName)
                    }
                }
        }
        if (!style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_2_TRAFFIC)) {
            LineLayer(
                RouteLayerConstants.LAYER_GROUP_2_TRAFFIC,
                RouteLayerConstants.LAYER_GROUP_2_SOURCE_ID,
            )
                .lineCap(LineCap.ROUND)
                .lineJoin(LineJoin.ROUND)
                .lineWidth(options.scaleExpressions.routeTrafficLineScaleExpression)
                .lineEmissiveStrength(1.0)
                .apply { opacityExpression?.let { lineOpacity(it) } }
                .lineColor(Color.GRAY).apply {
                    style.addPersistentLayer(this, LayerPosition(null, belowLayerIdToUse, null))
                    style.layerLineDepthOcclusionFactor(
                        layerId,
                        options.lineDepthOcclusionFactor,
                    )
                }
                .also {
                    if (styleContainsSlotName) {
                        it.slot(options.slotName)
                    }
                }
        }
        if (options.displayRestrictedRoadSections) {
            if (!style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_2_RESTRICTED)) {
                LineLayer(
                    RouteLayerConstants.LAYER_GROUP_2_RESTRICTED,
                    RouteLayerConstants.LAYER_GROUP_2_SOURCE_ID,
                )
                    .lineWidth(options.restrictedRoadLineWidth)
                    .lineJoin(LineJoin.ROUND)
                    .lineColor(options.routeLineColorResources.restrictedRoadColor)
                    .lineDasharray(options.restrictedRoadDashArray)
                    .lineCap(LineCap.ROUND)
                    .lineEmissiveStrength(1.0)
                    .lineOpacity(restrictedRoadsOpacityExpression)
                    .apply {
                        style.addPersistentLayer(this, LayerPosition(null, belowLayerIdToUse, null))
                        style.layerLineDepthOcclusionFactor(
                            layerId,
                            options.lineDepthOcclusionFactor,
                        )
                    }
                    .also {
                        if (styleContainsSlotName) {
                            it.slot(options.slotName)
                        }
                    }
            }
        }

        if (!style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_1_TRAIL_CASING)) {
            LineLayer(
                RouteLayerConstants.LAYER_GROUP_1_TRAIL_CASING,
                RouteLayerConstants.LAYER_GROUP_1_SOURCE_ID,
            )
                .lineCap(LineCap.ROUND)
                .lineJoin(LineJoin.ROUND)
                .lineWidth(options.scaleExpressions.routeCasingLineScaleExpression)
                .lineEmissiveStrength(1.0)
                .apply { opacityExpression?.let { lineOpacity(it) } }
                .lineColor(Color.GRAY).apply {
                    style.addPersistentLayer(this, LayerPosition(null, belowLayerIdToUse, null))
                    style.layerLineDepthOcclusionFactor(
                        layerId,
                        options.lineDepthOcclusionFactor,
                    )
                }
                .also {
                    if (styleContainsSlotName) {
                        it.slot(options.slotName)
                    }
                }
        }
        if (!style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_1_TRAIL)) {
            LineLayer(
                RouteLayerConstants.LAYER_GROUP_1_TRAIL,
                RouteLayerConstants.LAYER_GROUP_1_SOURCE_ID,
            )
                .lineCap(LineCap.ROUND)
                .lineJoin(LineJoin.ROUND)
                .lineWidth(options.scaleExpressions.routeLineScaleExpression)
                .lineEmissiveStrength(1.0)
                .apply { opacityExpression?.let { lineOpacity(it) } }
                .lineColor(Color.GRAY).apply {
                    style.addPersistentLayer(this, LayerPosition(null, belowLayerIdToUse, null))
                    style.layerLineDepthOcclusionFactor(
                        layerId,
                        options.lineDepthOcclusionFactor,
                    )
                }
                .also {
                    if (styleContainsSlotName) {
                        it.slot(options.slotName)
                    }
                }
        }
        if (!style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_1_BLUR)) {
            LineLayer(
                RouteLayerConstants.LAYER_GROUP_1_BLUR,
                RouteLayerConstants.LAYER_GROUP_1_SOURCE_ID,
            )
                .lineOpacity(options.routeLineBlurOpacity)
                .lineBlur(options.routeLineBlurWidth)
                .lineCap(LineCap.ROUND)
                .lineJoin(LineJoin.ROUND)
                .lineWidth(options.scaleExpressions.routeBlurScaleExpression)
                .lineEmissiveStrength(1.0)
                .lineColor(options.routeLineColorResources.blurColor).apply {
                    style.addPersistentLayer(this, LayerPosition(null, belowLayerIdToUse, null))
                    style.layerLineDepthOcclusionFactor(
                        layerId,
                        options.lineDepthOcclusionFactor,
                    )
                }
                .also {
                    if (styleContainsSlotName) {
                        it.slot(options.slotName)
                    }
                }
        }
        if (!style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_1_CASING)) {
            LineLayer(
                RouteLayerConstants.LAYER_GROUP_1_CASING,
                RouteLayerConstants.LAYER_GROUP_1_SOURCE_ID,
            )
                .lineCap(LineCap.ROUND)
                .lineJoin(LineJoin.ROUND)
                .lineWidth(options.scaleExpressions.routeCasingLineScaleExpression)
                .lineEmissiveStrength(1.0)
                .apply { opacityExpression?.let { lineOpacity(it) } }
                .lineColor(Color.GRAY).apply {
                    style.addPersistentLayer(this, LayerPosition(null, belowLayerIdToUse, null))
                    style.layerLineDepthOcclusionFactor(
                        layerId,
                        options.lineDepthOcclusionFactor,
                    )
                }
                .also {
                    if (styleContainsSlotName) {
                        it.slot(options.slotName)
                    }
                }
        }
        if (!style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_1_MAIN)) {
            LineLayer(
                RouteLayerConstants.LAYER_GROUP_1_MAIN,
                RouteLayerConstants.LAYER_GROUP_1_SOURCE_ID,
            )
                .lineCap(LineCap.ROUND)
                .lineJoin(LineJoin.ROUND)
                .lineWidth(options.scaleExpressions.routeLineScaleExpression)
                .lineEmissiveStrength(1.0)
                .apply { opacityExpression?.let { lineOpacity(it) } }
                .lineColor(Color.GRAY).apply {
                    style.addPersistentLayer(this, LayerPosition(null, belowLayerIdToUse, null))
                    style.layerLineDepthOcclusionFactor(
                        layerId,
                        options.lineDepthOcclusionFactor,
                    )
                }
                .also {
                    if (styleContainsSlotName) {
                        it.slot(options.slotName)
                    }
                }
        }
        if (!style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_1_TRAFFIC)) {
            LineLayer(
                RouteLayerConstants.LAYER_GROUP_1_TRAFFIC,
                RouteLayerConstants.LAYER_GROUP_1_SOURCE_ID,
            )
                .lineCap(LineCap.ROUND)
                .lineJoin(LineJoin.ROUND)
                .lineWidth(options.scaleExpressions.routeTrafficLineScaleExpression)
                .lineEmissiveStrength(1.0)
                .apply { opacityExpression?.let { lineOpacity(it) } }
                .lineColor(Color.GRAY).apply {
                    style.addPersistentLayer(this, LayerPosition(null, belowLayerIdToUse, null))
                    style.layerLineDepthOcclusionFactor(
                        layerId,
                        options.lineDepthOcclusionFactor,
                    )
                }
                .also {
                    if (styleContainsSlotName) {
                        it.slot(options.slotName)
                    }
                }
        }
        if (options.displayRestrictedRoadSections) {
            if (!style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_1_RESTRICTED)) {
                LineLayer(
                    RouteLayerConstants.LAYER_GROUP_1_RESTRICTED,
                    RouteLayerConstants.LAYER_GROUP_1_SOURCE_ID,
                )
                    .lineWidth(options.restrictedRoadLineWidth)
                    .lineJoin(LineJoin.ROUND)
                    .lineColor(options.routeLineColorResources.restrictedRoadColor)
                    .lineDasharray(options.restrictedRoadDashArray)
                    .lineCap(LineCap.ROUND)
                    .lineEmissiveStrength(1.0)
                    .lineOpacity(restrictedRoadsOpacityExpression)
                    .apply {
                        style.addPersistentLayer(this, LayerPosition(null, belowLayerIdToUse, null))
                        style.layerLineDepthOcclusionFactor(
                            layerId,
                            options.lineDepthOcclusionFactor,
                        )
                        if (styleContainsSlotName) {
                            this.slot(options.slotName)
                        }
                    }
            }
        }

        if (!style.styleLayerExists(RouteLayerConstants.MASKING_LAYER_TRAIL_CASING)) {
            LineLayer(
                RouteLayerConstants.MASKING_LAYER_TRAIL_CASING,
                RouteLayerConstants.LAYER_GROUP_1_SOURCE_ID,
            )
                .lineCap(LineCap.ROUND)
                .lineJoin(LineJoin.ROUND)
                .lineWidth(options.scaleExpressions.routeCasingLineScaleExpression)
                .lineEmissiveStrength(1.0)
                .apply { opacityExpression?.let { lineOpacity(it) } }
                .lineColor(Color.GRAY).apply {
                    style.addPersistentLayer(this, LayerPosition(null, belowLayerIdToUse, null))
                    style.layerLineDepthOcclusionFactor(
                        layerId,
                        options.lineDepthOcclusionFactor,
                    )
                }
                .also {
                    if (styleContainsSlotName) {
                        it.slot(options.slotName)
                    }
                }
        }
        if (!style.styleLayerExists(RouteLayerConstants.MASKING_LAYER_TRAIL)) {
            LineLayer(
                RouteLayerConstants.MASKING_LAYER_TRAIL,
                RouteLayerConstants.LAYER_GROUP_1_SOURCE_ID,
            )
                .lineCap(LineCap.ROUND)
                .lineJoin(LineJoin.ROUND)
                .lineWidth(options.scaleExpressions.routeLineScaleExpression)
                .lineEmissiveStrength(1.0)
                .apply { opacityExpression?.let { lineOpacity(it) } }
                .lineColor(Color.GRAY).apply {
                    style.addPersistentLayer(this, LayerPosition(null, belowLayerIdToUse, null))
                    style.layerLineDepthOcclusionFactor(
                        layerId,
                        options.lineDepthOcclusionFactor,
                    )
                }
                .also {
                    if (styleContainsSlotName) {
                        it.slot(options.slotName)
                    }
                }
        }
        if (!style.styleLayerExists(RouteLayerConstants.MASKING_LAYER_CASING)) {
            LineLayer(
                RouteLayerConstants.MASKING_LAYER_CASING,
                RouteLayerConstants.LAYER_GROUP_1_SOURCE_ID,
            )
                .lineCap(LineCap.ROUND)
                .lineJoin(LineJoin.ROUND)
                .lineWidth(options.scaleExpressions.routeCasingLineScaleExpression)
                .lineEmissiveStrength(1.0)
                .apply { opacityExpression?.let { lineOpacity(it) } }
                .lineColor(Color.GRAY).apply {
                    style.addPersistentLayer(this, LayerPosition(null, belowLayerIdToUse, null))
                    style.layerLineDepthOcclusionFactor(
                        layerId,
                        options.lineDepthOcclusionFactor,
                    )
                }
                .also {
                    if (styleContainsSlotName) {
                        it.slot(options.slotName)
                    }
                }
        }
        if (!style.styleLayerExists(RouteLayerConstants.MASKING_LAYER_MAIN)) {
            LineLayer(
                RouteLayerConstants.MASKING_LAYER_MAIN,
                RouteLayerConstants.LAYER_GROUP_1_SOURCE_ID,
            )
                .lineCap(LineCap.ROUND)
                .lineJoin(LineJoin.ROUND)
                .lineWidth(options.scaleExpressions.routeLineScaleExpression)
                .lineEmissiveStrength(1.0)
                .apply { opacityExpression?.let { lineOpacity(it) } }
                .lineColor(Color.GRAY).apply {
                    style.addPersistentLayer(this, LayerPosition(null, belowLayerIdToUse, null))
                    style.layerLineDepthOcclusionFactor(
                        layerId,
                        options.lineDepthOcclusionFactor,
                    )
                }
                .also {
                    if (styleContainsSlotName) {
                        it.slot(options.slotName)
                    }
                }
        }
        if (!style.styleLayerExists(RouteLayerConstants.MASKING_LAYER_TRAFFIC)) {
            LineLayer(
                RouteLayerConstants.MASKING_LAYER_TRAFFIC,
                RouteLayerConstants.LAYER_GROUP_1_SOURCE_ID,
            )
                .lineCap(LineCap.ROUND)
                .lineJoin(LineJoin.ROUND)
                .lineWidth(options.scaleExpressions.routeTrafficLineScaleExpression)
                .lineEmissiveStrength(1.0)
                .apply { opacityExpression?.let { lineOpacity(it) } }
                .lineColor(Color.GRAY).apply {
                    style.addPersistentLayer(this, LayerPosition(null, belowLayerIdToUse, null))
                    style.layerLineDepthOcclusionFactor(
                        layerId,
                        options.lineDepthOcclusionFactor,
                    )
                }
                .also {
                    if (styleContainsSlotName) {
                        it.slot(options.slotName)
                    }
                }
        }
        if (options.displayRestrictedRoadSections) {
            if (!style.styleLayerExists(RouteLayerConstants.MASKING_LAYER_RESTRICTED)) {
                LineLayer(
                    RouteLayerConstants.MASKING_LAYER_RESTRICTED,
                    RouteLayerConstants.LAYER_GROUP_1_SOURCE_ID,
                )
                    .lineWidth(options.restrictedRoadLineWidth)
                    .lineJoin(LineJoin.ROUND)
                    .lineColor(options.routeLineColorResources.restrictedRoadColor)
                    .lineDasharray(options.restrictedRoadDashArray)
                    .lineCap(LineCap.ROUND)
                    .lineEmissiveStrength(1.0)
                    .lineOpacity(restrictedRoadsOpacityExpression)
                    .apply {
                        style.addPersistentLayer(this, LayerPosition(null, belowLayerIdToUse, null))
                        style.layerLineDepthOcclusionFactor(
                            layerId,
                            options.lineDepthOcclusionFactor,
                        )
                        if (styleContainsSlotName) {
                            this.slot(options.slotName)
                        }
                    }
            }
        }
        if (!style.styleLayerExists(RouteLayerConstants.TOP_LEVEL_ROUTE_LINE_LAYER_ID)) {
            style.addPersistentLayer(
                BackgroundLayer(
                    RouteLayerConstants.TOP_LEVEL_ROUTE_LINE_LAYER_ID,
                ).apply {
                    backgroundOpacity(0.0)
                    if (styleContainsSlotName) {
                        this.slot(options.slotName)
                    }
                },
                LayerPosition(null, belowLayerIdToUse, null),
            )
        }

        if (!style.hasStyleImage(RouteLayerConstants.ORIGIN_MARKER_NAME)) {
            options.originWaypointIcon.getBitmap().let {
                style.addImage(RouteLayerConstants.ORIGIN_MARKER_NAME, it)
            }
        }
        if (!style.hasStyleImage(RouteLayerConstants.DESTINATION_MARKER_NAME)) {
            options.destinationWaypointIcon.getBitmap().let {
                style.addImage(RouteLayerConstants.DESTINATION_MARKER_NAME, it)
            }
        }
        if (!style.styleLayerExists(RouteLayerConstants.WAYPOINT_LAYER_ID)) {
            style.addPersistentLayer(
                SymbolLayer(
                    RouteLayerConstants.WAYPOINT_LAYER_ID,
                    RouteLayerConstants.WAYPOINT_SOURCE_ID,
                )
                    .iconOffset(options.waypointLayerIconOffset)
                    .iconAnchor(options.waypointLayerIconAnchor)
                    .iconImage(
                        match {
                            toString {
                                get { literal(RouteLayerConstants.WAYPOINT_PROPERTY_KEY) }
                            }
                            stop {
                                literal(RouteLayerConstants.WAYPOINT_ORIGIN_VALUE)
                                literal(RouteLayerConstants.ORIGIN_MARKER_NAME)
                            }
                            stop {
                                literal(RouteLayerConstants.WAYPOINT_DESTINATION_VALUE)
                                literal(RouteLayerConstants.DESTINATION_MARKER_NAME)
                            }
                            literal(RouteLayerConstants.ORIGIN_MARKER_NAME)
                        },
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
                        },
                    ).also {
                        if (styleContainsSlotName) {
                            it.slot(options.slotName)
                        }
                    }
                    .iconPitchAlignment(options.iconPitchAlignment)
                    .iconAllowOverlap(true)
                    .iconIgnorePlacement(true)
                    .iconKeepUpright(true)
                    .apply { opacityExpression?.let { iconOpacity(it) } },
                LayerPosition(null, belowLayerIdToUse, null),
            )
        }
    }

    // for a set of dynamic values, see MapboxRouteLineViewDynamicOptionsBuilder
    // scale expressions and restricted layer colors will be changed when routes are re-rendered.
    @OptIn(MapboxExperimental::class)
    internal fun updateLayersStyling(style: Style, viewOptions: MapboxRouteLineViewOptions) {
        val opacityExpression = viewOptions.opacityExpression()
        val restrictedRoadsOpacityExpression = viewOptions.restrictedRoadsOpacityExpression()
        val styleContainsSlotName = style.styleSlots.contains(viewOptions.slotName).also {
            if (!it) {
                logW(LOG_CATEGORY) {
                    "The ${viewOptions.slotName} slot is not present in the style."
                }
            }
        }
        (style.getLayer(RouteLayerConstants.LAYER_GROUP_3_TRAIL_CASING) as? LineLayer)?.let {
            if (styleContainsSlotName) {
                it.slot(viewOptions.slotName)
            }
            style.layerLineDepthOcclusionFactor(it.layerId, viewOptions.lineDepthOcclusionFactor)
            opacityExpression?.let { expr -> it.lineOpacity(expr) }
        }
        (style.getLayer(RouteLayerConstants.LAYER_GROUP_3_TRAIL) as? LineLayer)?.let {
            if (styleContainsSlotName) {
                it.slot(viewOptions.slotName)
            }
            style.layerLineDepthOcclusionFactor(it.layerId, viewOptions.lineDepthOcclusionFactor)
            opacityExpression?.let { expr -> it.lineOpacity(expr) }
        }
        (style.getLayer(RouteLayerConstants.LAYER_GROUP_3_CASING) as? LineLayer)?.let {
            if (styleContainsSlotName) {
                it.slot(viewOptions.slotName)
            }
            style.layerLineDepthOcclusionFactor(it.layerId, viewOptions.lineDepthOcclusionFactor)
            opacityExpression?.let { expr -> it.lineOpacity(expr) }
        }
        (style.getLayer(RouteLayerConstants.LAYER_GROUP_3_MAIN) as? LineLayer)?.let {
            if (styleContainsSlotName) {
                it.slot(viewOptions.slotName)
            }
            style.layerLineDepthOcclusionFactor(it.layerId, viewOptions.lineDepthOcclusionFactor)
            opacityExpression?.let { expr -> it.lineOpacity(expr) }
        }
        (style.getLayer(RouteLayerConstants.LAYER_GROUP_3_TRAFFIC) as? LineLayer)?.let {
            if (styleContainsSlotName) {
                it.slot(viewOptions.slotName)
            }
            style.layerLineDepthOcclusionFactor(it.layerId, viewOptions.lineDepthOcclusionFactor)
            opacityExpression?.let { expr -> it.lineOpacity(expr) }
        }
        (style.getLayer(RouteLayerConstants.LAYER_GROUP_3_RESTRICTED) as? LineLayer)?.let {
            it.lineWidth(viewOptions.restrictedRoadLineWidth)
            it.lineDasharray(viewOptions.restrictedRoadDashArray)
            if (styleContainsSlotName) {
                it.slot(viewOptions.slotName)
            }
            style.layerLineDepthOcclusionFactor(it.layerId, viewOptions.lineDepthOcclusionFactor)
            it.lineOpacity(restrictedRoadsOpacityExpression)
        }

        (style.getLayer(RouteLayerConstants.LAYER_GROUP_2_TRAIL_CASING) as? LineLayer)?.let {
            if (styleContainsSlotName) {
                it.slot(viewOptions.slotName)
            }
            style.layerLineDepthOcclusionFactor(it.layerId, viewOptions.lineDepthOcclusionFactor)
            opacityExpression?.let { expr -> it.lineOpacity(expr) }
        }
        (style.getLayer(RouteLayerConstants.LAYER_GROUP_2_TRAIL) as? LineLayer)?.let {
            if (styleContainsSlotName) {
                it.slot(viewOptions.slotName)
            }
            style.layerLineDepthOcclusionFactor(it.layerId, viewOptions.lineDepthOcclusionFactor)
            opacityExpression?.let { expr -> it.lineOpacity(expr) }
        }
        (style.getLayer(RouteLayerConstants.LAYER_GROUP_2_CASING) as? LineLayer)?.let {
            if (styleContainsSlotName) {
                it.slot(viewOptions.slotName)
            }
            style.layerLineDepthOcclusionFactor(it.layerId, viewOptions.lineDepthOcclusionFactor)
            opacityExpression?.let { expr -> it.lineOpacity(expr) }
        }
        (style.getLayer(RouteLayerConstants.LAYER_GROUP_2_MAIN) as? LineLayer)?.let {
            if (styleContainsSlotName) {
                it.slot(viewOptions.slotName)
            }
            style.layerLineDepthOcclusionFactor(it.layerId, viewOptions.lineDepthOcclusionFactor)
            opacityExpression?.let { expr -> it.lineOpacity(expr) }
        }
        (style.getLayer(RouteLayerConstants.LAYER_GROUP_2_TRAFFIC) as? LineLayer)?.let {
            if (styleContainsSlotName) {
                it.slot(viewOptions.slotName)
            }
            style.layerLineDepthOcclusionFactor(it.layerId, viewOptions.lineDepthOcclusionFactor)
            opacityExpression?.let { expr -> it.lineOpacity(expr) }
        }
        (style.getLayer(RouteLayerConstants.LAYER_GROUP_2_RESTRICTED) as? LineLayer)?.let {
            it.lineWidth(viewOptions.restrictedRoadLineWidth)
            it.lineDasharray(viewOptions.restrictedRoadDashArray)
            if (styleContainsSlotName) {
                it.slot(viewOptions.slotName)
            }
            style.layerLineDepthOcclusionFactor(it.layerId, viewOptions.lineDepthOcclusionFactor)
            it.lineOpacity(restrictedRoadsOpacityExpression)
        }

        (style.getLayer(RouteLayerConstants.LAYER_GROUP_1_TRAIL_CASING) as? LineLayer)?.let {
            if (styleContainsSlotName) {
                it.slot(viewOptions.slotName)
            }
            style.layerLineDepthOcclusionFactor(it.layerId, viewOptions.lineDepthOcclusionFactor)
            opacityExpression?.let { expr -> it.lineOpacity(expr) }
        }
        (style.getLayer(RouteLayerConstants.LAYER_GROUP_1_TRAIL) as? LineLayer)?.let {
            if (styleContainsSlotName) {
                it.slot(viewOptions.slotName)
            }
            style.layerLineDepthOcclusionFactor(it.layerId, viewOptions.lineDepthOcclusionFactor)
            opacityExpression?.let { expr -> it.lineOpacity(expr) }
        }
        (style.getLayer(RouteLayerConstants.LAYER_GROUP_1_CASING) as? LineLayer)?.let {
            if (styleContainsSlotName) {
                it.slot(viewOptions.slotName)
            }
            style.layerLineDepthOcclusionFactor(it.layerId, viewOptions.lineDepthOcclusionFactor)
            opacityExpression?.let { expr -> it.lineOpacity(expr) }
        }
        (style.getLayer(RouteLayerConstants.LAYER_GROUP_1_MAIN) as? LineLayer)?.let {
            if (styleContainsSlotName) {
                it.slot(viewOptions.slotName)
            }
            style.layerLineDepthOcclusionFactor(it.layerId, viewOptions.lineDepthOcclusionFactor)
            opacityExpression?.let { expr -> it.lineOpacity(expr) }
        }
        (style.getLayer(RouteLayerConstants.LAYER_GROUP_1_TRAFFIC) as? LineLayer)?.let {
            if (styleContainsSlotName) {
                it.slot(viewOptions.slotName)
            }
            style.layerLineDepthOcclusionFactor(it.layerId, viewOptions.lineDepthOcclusionFactor)
            opacityExpression?.let { expr -> it.lineOpacity(expr) }
        }
        (style.getLayer(RouteLayerConstants.LAYER_GROUP_1_RESTRICTED) as? LineLayer)?.let {
            it.lineWidth(viewOptions.restrictedRoadLineWidth)
            it.lineDasharray(viewOptions.restrictedRoadDashArray)
            if (styleContainsSlotName) {
                it.slot(viewOptions.slotName)
            }
            style.layerLineDepthOcclusionFactor(it.layerId, viewOptions.lineDepthOcclusionFactor)
            it.lineOpacity(restrictedRoadsOpacityExpression)
        }

        (style.getLayer(RouteLayerConstants.MASKING_LAYER_TRAIL_CASING) as? LineLayer)?.let {
            if (styleContainsSlotName) {
                it.slot(viewOptions.slotName)
            }
            style.layerLineDepthOcclusionFactor(it.layerId, viewOptions.lineDepthOcclusionFactor)
            opacityExpression?.let { expr -> it.lineOpacity(expr) }
        }
        (style.getLayer(RouteLayerConstants.MASKING_LAYER_TRAIL) as? LineLayer)?.let {
            if (styleContainsSlotName) {
                it.slot(viewOptions.slotName)
            }
            style.layerLineDepthOcclusionFactor(it.layerId, viewOptions.lineDepthOcclusionFactor)
            opacityExpression?.let { expr -> it.lineOpacity(expr) }
        }
        (style.getLayer(RouteLayerConstants.MASKING_LAYER_CASING) as? LineLayer)?.let {
            if (styleContainsSlotName) {
                it.slot(viewOptions.slotName)
            }
            style.layerLineDepthOcclusionFactor(it.layerId, viewOptions.lineDepthOcclusionFactor)
            opacityExpression?.let { expr -> it.lineOpacity(expr) }
        }
        (style.getLayer(RouteLayerConstants.MASKING_LAYER_MAIN) as? LineLayer)?.let {
            if (styleContainsSlotName) {
                it.slot(viewOptions.slotName)
            }
            style.layerLineDepthOcclusionFactor(it.layerId, viewOptions.lineDepthOcclusionFactor)
            opacityExpression?.let { expr -> it.lineOpacity(expr) }
        }
        (style.getLayer(RouteLayerConstants.MASKING_LAYER_TRAFFIC) as? LineLayer)?.let {
            if (styleContainsSlotName) {
                it.slot(viewOptions.slotName)
            }
            style.layerLineDepthOcclusionFactor(it.layerId, viewOptions.lineDepthOcclusionFactor)
            opacityExpression?.let { expr -> it.lineOpacity(expr) }
        }
        (style.getLayer(RouteLayerConstants.MASKING_LAYER_RESTRICTED) as? LineLayer)?.let {
            it.lineWidth(viewOptions.restrictedRoadLineWidth)
            it.lineDasharray(viewOptions.restrictedRoadDashArray)
            if (styleContainsSlotName) {
                it.slot(viewOptions.slotName)
            }
            style.layerLineDepthOcclusionFactor(it.layerId, viewOptions.lineDepthOcclusionFactor)
            it.lineOpacity(restrictedRoadsOpacityExpression)
        }

        viewOptions.originWaypointIcon.getBitmap().let {
            style.addImage(RouteLayerConstants.ORIGIN_MARKER_NAME, it)
        }
        viewOptions.destinationWaypointIcon.getBitmap().let {
            style.addImage(
                RouteLayerConstants.DESTINATION_MARKER_NAME,
                it,
            )
        }
        (style.getLayer(RouteLayerConstants.WAYPOINT_LAYER_ID) as? SymbolLayer)?.let {
            it.iconOffset(viewOptions.waypointLayerIconOffset)
            it.iconAnchor(viewOptions.waypointLayerIconAnchor)
            it.iconImage(
                match {
                    toString {
                        get { literal(RouteLayerConstants.WAYPOINT_PROPERTY_KEY) }
                    }
                    stop {
                        literal(RouteLayerConstants.WAYPOINT_ORIGIN_VALUE)
                        literal(RouteLayerConstants.ORIGIN_MARKER_NAME)
                    }
                    stop {
                        literal(RouteLayerConstants.WAYPOINT_DESTINATION_VALUE)
                        literal(RouteLayerConstants.DESTINATION_MARKER_NAME)
                    }
                    literal(RouteLayerConstants.ORIGIN_MARKER_NAME)
                },
            )
            it.iconPitchAlignment(viewOptions.iconPitchAlignment)
            if (styleContainsSlotName) {
                it.slot(viewOptions.slotName)
            }
            opacityExpression?.let { expr -> it.iconOpacity(expr) }
        }

        (style.getLayer(RouteLayerConstants.LAYER_GROUP_1_BLUR) as? LineLayer)?.let {
            if (styleContainsSlotName) {
                it.slot(viewOptions.slotName)
            }
            it.lineOpacity(viewOptions.routeLineBlurOpacity)
            it.lineBlur(viewOptions.routeLineBlurWidth)
            it.lineWidth(viewOptions.scaleExpressions.routeBlurScaleExpression)
            it.lineColor(viewOptions.routeLineColorResources.blurColor)
            style.layerLineDepthOcclusionFactor(it.layerId, viewOptions.lineDepthOcclusionFactor)
        }

        (style.getLayer(RouteLayerConstants.LAYER_GROUP_2_BLUR) as? LineLayer)?.let {
            if (styleContainsSlotName) {
                it.slot(viewOptions.slotName)
            }
            it.lineOpacity(viewOptions.routeLineBlurOpacity)
            it.lineBlur(viewOptions.routeLineBlurWidth)
            it.lineWidth(viewOptions.scaleExpressions.routeBlurScaleExpression)
            it.lineColor(viewOptions.routeLineColorResources.blurColor)
            style.layerLineDepthOcclusionFactor(it.layerId, viewOptions.lineDepthOcclusionFactor)
        }

        (style.getLayer(RouteLayerConstants.LAYER_GROUP_3_BLUR) as? LineLayer)?.let {
            if (styleContainsSlotName) {
                it.slot(viewOptions.slotName)
            }
            it.lineOpacity(viewOptions.routeLineBlurOpacity)
            it.lineBlur(viewOptions.routeLineBlurWidth)
            it.lineWidth(viewOptions.scaleExpressions.routeBlurScaleExpression)
            it.lineColor(viewOptions.routeLineColorResources.blurColor)
            style.layerLineDepthOcclusionFactor(it.layerId, viewOptions.lineDepthOcclusionFactor)
        }
    }

    internal fun layersAreInitialized(
        style: Style,
        options: MapboxRouteLineViewOptions,
    ): Boolean {
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
            if (options.routeLineBlurEnabled) {
                style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_1_BLUR) &&
                    style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_2_BLUR) &&
                    style.styleLayerExists(RouteLayerConstants.LAYER_GROUP_3_BLUR)
            } else {
                true
            }
    }

    internal fun getTrafficLineExpression(
        route: NavigationRoute,
        staticOptions: MapboxRouteLineApiOptions,
        dynamicData: RouteLineViewOptionsData,
        trafficBackfillRoadClasses: List<String>,
        isPrimaryRoute: Boolean,
        vanishingPointOffset: Double,
        lineStartColor: Int,
        lineColorType: SegmentColorType,
    ): Expression {
        val segments: List<RouteLineExpressionData> = calculateRouteLineSegments(
            route,
            trafficBackfillRoadClasses,
            isPrimaryRoute,
            staticOptions,
        )
        return getTrafficLineExpression(
            dynamicData,
            vanishingPointOffset,
            lineStartColor,
            lineColorType,
            segments,
            route.directionsRoute.distance(),
        )
    }

    internal fun getTrafficLineExpression(
        dynamicData: RouteLineViewOptionsData,
        vanishingPointOffset: Double,
        lineStartColor: Int,
        lineColorType: SegmentColorType,
        segments: List<RouteLineExpressionData>,
        routeDistance: Double,
    ): Expression {
        return if (dynamicData.displaySoftGradientForTraffic) {
            getTrafficLineExpressionSoftGradient(
                dynamicData,
                vanishingPointOffset,
                lineStartColor,
                lineColorType,
                dynamicData.softGradientTransition / routeDistance,
                segments,
            )
        } else {
            getTrafficLineExpression(
                dynamicData,
                vanishingPointOffset,
                lineStartColor,
                lineColorType,
                segments,
            )
        }
    }

    internal fun getNonMaskingRestrictedLineExpressionProducer(
        routeData: List<ExtractedRouteRestrictionData>,
        vanishingPointOffset: Double,
        activeLegIndex: Int,
        staticOptions: MapboxRouteLineApiOptions,
    ): (data: RouteLineViewOptionsData) -> StylePropertyValue {
        return {
            val colorResources = it.routeLineColorResources
            val inactiveColor = if (staticOptions.styleInactiveRouteLegsIndependently) {
                colorResources.inactiveRouteLegRestrictedRoadColor
            } else {
                colorResources.restrictedRoadColor
            }
            getRestrictedLineExpression(
                routeData,
                vanishingPointOffset,
                activeLegIndex,
                staticOptions.calculateRestrictedRoadSections,
                activeColor = colorResources.restrictedRoadColor,
                inactiveColor = inactiveColor,
            ).toStylePropertyValue()
        }
    }

    internal fun getRestrictedLineExpressionProducer(
        staticOptions: MapboxRouteLineApiOptions,
        routeData: List<ExtractedRouteRestrictionData>,
        vanishingPointOffset: Double,
        activeLegIndex: Int,
        inactiveColorType: SegmentColorType,
    ): (data: RouteLineViewOptionsData) -> StylePropertyValue = {
        getRestrictedLineExpression(
            routeData,
            vanishingPointOffset,
            activeLegIndex,
            staticOptions.calculateRestrictedRoadSections,
            it.routeLineColorResources.restrictedRoadColor,
            inactiveColorType.getColor(it),
        ).toStylePropertyValue()
    }

    private fun getRestrictedLineExpression(
        routeData: List<ExtractedRouteRestrictionData>,
        vanishingPointOffset: Double,
        activeLegIndex: Int,
        displayRestrictedEnabled: Boolean,
        activeColor: Int,
        inactiveColor: Int,
    ): Expression = if (displayRestrictedEnabled) {
        getRestrictedLineExpression(
            vanishingPointOffset,
            activeLegIndex,
            restrictedSectionColor = activeColor,
            restrictedSectionInactiveColor = inactiveColor,
            routeData,
        )
    } else {
        color(Color.TRANSPARENT)
    }

    internal fun getRestrictedLineExpression(
        vanishingPointOffset: Double,
        activeLegIndex: Int,
        restrictedSectionColor: Int,
        restrictedSectionInactiveColor: Int,
        routeLineExpressionData: List<ExtractedRouteRestrictionData>,
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
            },
        ).forEach {
            val colorToUse = if (activeLegIndex >= 0 && it.legIndex != activeLegIndex) {
                if (it.isInRestrictedSection) {
                    restrictedSectionInactiveColor
                } else {
                    Color.TRANSPARENT
                }
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

    internal fun trimRouteDataCacheToSize(size: Int) {
        extractRouteDataCache.trimToSize(size)
        granularDistancesCache.trimToSize(size)
    }

    internal fun getLayerIdsForPrimaryRoute(
        style: Style,
        sourceLayerMap: Map<RouteLineSourceKey, Set<String>>,
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
                    "background",
                ),
            )
            val lowerRange = style.styleLayers.indexOf(
                StyleObjectInfo(
                    RouteLayerConstants.BOTTOM_LEVEL_ROUTE_LINE_LAYER_ID,
                    "background",
                ),
            )

            style.styleLayers.subList(lowerRange, upperRange)
                .filter { it.id !in maskingLayerIds }
                .mapIndexed { index, styleObjectInfo ->
                    Pair(index, styleObjectInfo.id)
                }.maxByOrNull { it.first }?.second
        }.getOrNull()
    }

    internal tailrec fun featureCollectionHasProperty(
        featureCollection: FeatureCollection?,
        index: Int,
        property: String,
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
            granularDistancesProvider,
    ): Double {
        return distancesProvider(metadata.navigationRoute)?.let { distances ->
            if (distances.routeDistances.isEmpty() || distances.completeDistance <= 0) {
                logW(
                    "Remaining distances array size is ${distances.routeDistances.size} " +
                        "and the full distance is ${distances.completeDistance} - " +
                        "unable to calculate the deviation point of the alternative with ID " +
                        "'${metadata.navigationRoute.id}' to hide the portion that overlaps " +
                        "with the primary route.",
                    LOG_CATEGORY,
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
                    LOG_CATEGORY,
                )
                return@let 0.0
            }.distanceRemaining
            if (distanceRemaining > distances.completeDistance) {
                logW(
                    "distance remaining > full distance - " +
                        "unable to calculate the deviation point of the alternative with ID " +
                        "'${metadata.navigationRoute.id}' to hide the portion that overlaps " +
                        "with the primary route.",
                    LOG_CATEGORY,
                )
                return@let 0.0
            }
            1.0 - distanceRemaining / distances.completeDistance
        } ?: 0.0
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

    internal fun removeLayers(style: SdkStyleManager) {
        style.removeStyleLayer(RouteLayerConstants.TOP_LEVEL_ROUTE_LINE_LAYER_ID)
        style.removeStyleLayer(RouteLayerConstants.BOTTOM_LEVEL_ROUTE_LINE_LAYER_ID)
        style.removeStyleLayer(RouteLayerConstants.LAYER_GROUP_1_TRAIL_CASING)
        style.removeStyleLayer(RouteLayerConstants.LAYER_GROUP_1_TRAIL)
        style.removeStyleLayer(RouteLayerConstants.LAYER_GROUP_1_CASING)
        style.removeStyleLayer(RouteLayerConstants.LAYER_GROUP_1_MAIN)
        style.removeStyleLayer(RouteLayerConstants.LAYER_GROUP_1_TRAFFIC)
        style.removeStyleLayer(RouteLayerConstants.LAYER_GROUP_1_RESTRICTED)
        style.removeStyleLayer(RouteLayerConstants.LAYER_GROUP_2_TRAIL_CASING)
        style.removeStyleLayer(RouteLayerConstants.LAYER_GROUP_2_TRAIL)
        style.removeStyleLayer(RouteLayerConstants.LAYER_GROUP_2_CASING)
        style.removeStyleLayer(RouteLayerConstants.LAYER_GROUP_2_MAIN)
        style.removeStyleLayer(RouteLayerConstants.LAYER_GROUP_2_TRAFFIC)
        style.removeStyleLayer(RouteLayerConstants.LAYER_GROUP_2_RESTRICTED)
        style.removeStyleLayer(RouteLayerConstants.LAYER_GROUP_3_TRAIL_CASING)
        style.removeStyleLayer(RouteLayerConstants.LAYER_GROUP_3_TRAIL)
        style.removeStyleLayer(RouteLayerConstants.LAYER_GROUP_3_CASING)
        style.removeStyleLayer(RouteLayerConstants.LAYER_GROUP_3_MAIN)
        style.removeStyleLayer(RouteLayerConstants.LAYER_GROUP_3_TRAFFIC)
        style.removeStyleLayer(RouteLayerConstants.LAYER_GROUP_3_RESTRICTED)
        style.removeStyleLayer(RouteLayerConstants.MASKING_LAYER_TRAIL_CASING)
        style.removeStyleLayer(RouteLayerConstants.MASKING_LAYER_TRAIL)
        style.removeStyleLayer(RouteLayerConstants.MASKING_LAYER_CASING)
        style.removeStyleLayer(RouteLayerConstants.MASKING_LAYER_MAIN)
        style.removeStyleLayer(RouteLayerConstants.MASKING_LAYER_TRAFFIC)
        style.removeStyleLayer(RouteLayerConstants.MASKING_LAYER_RESTRICTED)
        style.removeStyleLayer(RouteLayerConstants.WAYPOINT_LAYER_ID)
        style.removeStyleLayer(RouteLayerConstants.LAYER_GROUP_1_BLUR)
        style.removeStyleLayer(RouteLayerConstants.LAYER_GROUP_2_BLUR)
        style.removeStyleLayer(RouteLayerConstants.LAYER_GROUP_3_BLUR)
        style.removeStyleImage(RouteLayerConstants.ORIGIN_MARKER_NAME)
        style.removeStyleImage(RouteLayerConstants.DESTINATION_MARKER_NAME)
    }

    internal fun getPrimaryRouteLineDynamicData(
        calculationsScope: CoroutineScope,
        routeLineOptions: MapboxRouteLineApiOptions,
        routeLineExpressionData: List<RouteLineExpressionData>,
        restrictedExpressionData: List<ExtractedRouteRestrictionData>,
        primaryRouteDistance: Double,
        vanishingPointOffset: Double,
        legIndex: Int,
    ): RouteLineDynamicData {
        val trafficExpressionProvider: (RouteLineViewOptionsData) -> StylePropertyValue =
            { options ->
                getTrafficLineExpression(
                    options,
                    vanishingPointOffset,
                    Color.TRANSPARENT,
                    SegmentColorType.PRIMARY_UNKNOWN_CONGESTION,
                    routeLineExpressionData,
                    primaryRouteDistance,
                ).toStylePropertyValue()
            }
        val primaryRouteTrafficLineExpressionCommandHolder =
            RouteLineValueCommandHolder(
                HeavyRouteLineExpressionValueProvider {
                    trafficExpressionProvider(it)
                },
                LineGradientCommandApplier(),
            )

        val primaryRouteBaseExpressionCommandHolder =
            RouteLineValueCommandHolder(
                LightRouteLineExpressionValueProvider {
                    // TODO why are we changing traveled portion to traveled color instead of
                    //  making it transparent to show trail layers?
                    if (routeLineOptions.styleInactiveRouteLegsIndependently) {
                        getExpressionSubstitutingColorForInactiveLegs(
                            vanishingPointOffset,
                            routeLineExpressionData,
                            it.routeLineColorResources.routeLineTraveledColor,
                            it.routeLineColorResources.routeDefaultColor,
                            it.routeLineColorResources.inActiveRouteLegsColor,
                            legIndex,
                        )
                    } else {
                        getRouteLineExpression(
                            vanishingPointOffset,
                            it.routeLineColorResources.routeLineTraveledColor,
                            it.routeLineColorResources.routeDefaultColor,
                        )
                    }
                },
                LineGradientCommandApplier(),
            )

        val primaryRouteCasingExpressionCommandHolder = RouteLineValueCommandHolder(
            // TODO why are we changing traveled portion to traveled color instead of
            //  making it transparent to show trail layers?
            LightRouteLineExpressionValueProvider {
                if (routeLineOptions.styleInactiveRouteLegsIndependently) {
                    getExpressionSubstitutingColorForInactiveLegs(
                        vanishingPointOffset,
                        routeLineExpressionData,
                        it.routeLineColorResources.routeLineTraveledCasingColor,
                        it.routeLineColorResources.routeCasingColor,
                        it.routeLineColorResources.inactiveRouteLegCasingColor,
                        legIndex,
                    )
                } else {
                    getRouteLineExpression(
                        vanishingPointOffset,
                        it.routeLineColorResources.routeLineTraveledCasingColor,
                        it.routeLineColorResources.routeCasingColor,
                    )
                }
            },
            LineGradientCommandApplier(),
        )

        // TODO if vanishing route line is not used, trail layers should never be visible,
        //  so we shouldn't need to change their colors.
        //  However, all layers are initialized to grey right now,
        //  and they should be initialized as transparent instead.
        val primaryRouteTrailExpressionCommandHolder = RouteLineValueCommandHolder(
            LightRouteLineExpressionValueProvider {
                if (routeLineOptions.styleInactiveRouteLegsIndependently &&
                    routeLineOptions.vanishingRouteLineEnabled
                ) {
                    // if both independent styling and vanishing route line are enabled,
                    // we need to only draw trailing layers for previous and current legs,
                    // upcoming legs should not have trail because they can be transparent
                    getExpressionSubstitutingColorForUpcomingLegs(
                        routeLineExpressionData,
                        it.routeLineColorResources.routeLineTraveledColor,
                        Color.TRANSPARENT,
                        legIndex,
                    )
                } else if (routeLineOptions.styleInactiveRouteLegsIndependently) {
                    // if independent styling is enabled and vanishing route line is not,
                    // we only want to draw trail under current leg,
                    // as without the vanishing route line enabled the traveled portion should not
                    // be emphasized at all and if the inactive legs were transparent,
                    // the trail would show up
                    getExpressionSubstitutingColorForInactiveLegs(
                        distanceOffset = 0.0,
                        routeLineExpressionData = routeLineExpressionData,
                        lineBaseColor = it.routeLineColorResources.routeLineTraveledColor,
                        defaultColor = it.routeLineColorResources.routeLineTraveledColor,
                        substitutionColor = Color.TRANSPARENT,
                        activeLegIndex = legIndex,
                    )
                } else {
                    // if independent styling is not enabled,
                    // we can draw the trail under the whole route,
                    // if vanishing route line is enabled it will show up, if not, it won't
                    getRouteLineExpression(
                        0.0,
                        it.routeLineColorResources.routeLineTraveledColor,
                        it.routeLineColorResources.routeLineTraveledColor,
                    )
                }
            },
            LineGradientCommandApplier(),
        )
        // the same conditions apply for the trail casing as do for the trail layers
        val primaryRouteTrailCasingExpressionCommandHolder = RouteLineValueCommandHolder(
            LightRouteLineExpressionValueProvider {
                if (routeLineOptions.styleInactiveRouteLegsIndependently &&
                    routeLineOptions.vanishingRouteLineEnabled
                ) {
                    getExpressionSubstitutingColorForUpcomingLegs(
                        routeLineExpressionData,
                        it.routeLineColorResources.routeLineTraveledCasingColor,
                        Color.TRANSPARENT,
                        legIndex,
                    )
                } else if (routeLineOptions.styleInactiveRouteLegsIndependently) {
                    getExpressionSubstitutingColorForInactiveLegs(
                        distanceOffset = 0.0,
                        routeLineExpressionData = routeLineExpressionData,
                        lineBaseColor = it.routeLineColorResources.routeLineTraveledCasingColor,
                        defaultColor = it.routeLineColorResources.routeLineTraveledCasingColor,
                        substitutionColor = Color.TRANSPARENT,
                        activeLegIndex = legIndex,
                    )
                } else {
                    getRouteLineExpression(
                        0.0,
                        it.routeLineColorResources.routeLineTraveledCasingColor,
                        it.routeLineColorResources.routeLineTraveledCasingColor,
                    )
                }
            },
            LineGradientCommandApplier(),
        )

        // If the displayRestrictedRoadSections is true AND the route has restricted sections
        // then produce a gradient that is transparent except for the restricted sections.
        // If false produce a gradient for the restricted line layer that is completely transparent.
        val primaryRouteRestrictedSectionsExpressionProducer =
            RouteLineValueCommandHolder(
                HeavyRouteLineExpressionValueProvider(
                    getNonMaskingRestrictedLineExpressionProducer(
                        restrictedExpressionData,
                        0.0,
                        legIndex,
                        routeLineOptions,
                    ),
                ),
                LineGradientCommandApplier(),
            )

        val blurLineHolder = RouteLineValueCommandHolder(
            HeavyRouteLineExpressionValueProvider { options ->
                if (options.routeLineBlurEnabled) {
                    if (options.applyTrafficColorsToRouteLineBlur) {
                        trafficExpressionProvider(options)
                    } else {
                        if (routeLineOptions.styleInactiveRouteLegsIndependently) {
                            getExpressionSubstitutingColorForInactiveLegs(
                                vanishingPointOffset,
                                routeLineExpressionData,
                                options.routeLineColorResources.blurColor,
                                options.routeLineColorResources.blurColor,
                                Color.TRANSPARENT,
                                legIndex,
                            )
                        } else {
                            getSingleColorExpression(options.routeLineColorResources.blurColor)
                        }
                    }
                } else {
                    getSingleColorExpression(Color.TRANSPARENT)
                }
            },
            LineGradientCommandApplier(),
        )

        return RouteLineDynamicData(
            primaryRouteBaseExpressionCommandHolder,
            primaryRouteCasingExpressionCommandHolder,
            primaryRouteTrafficLineExpressionCommandHolder,
            primaryRouteRestrictedSectionsExpressionProducer,
            RouteLineTrimOffset(vanishingPointOffset),
            primaryRouteTrailExpressionCommandHolder,
            primaryRouteTrailCasingExpressionCommandHolder,
            blurLineHolder,
        )
    }

    fun getSingleColorExpression(@ColorInt colorInt: Int): StylePropertyValue {
        return getRouteLineExpression(0.0, colorInt, colorInt)
    }
}

private fun Style.layerLineDepthOcclusionFactor(layerId: String, factor: Double) {
    setStyleLayerProperty(layerId, "line-depth-occlusion-factor", Value(factor))
}

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
private fun MapboxRouteLineViewOptions.opacityExpression(): Expression? =
    fadeOnHighZoomsConfig?.opacityExpression()

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal fun FadingConfig.opacityExpression(): Expression = interpolate {
    linear()
    zoom()
    stop { literal(startFadingZoom); literal(1.0) }
    stop { literal(finishFadingZoom); literal(0.0) }
}

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
private fun MapboxRouteLineViewOptions.restrictedRoadsOpacityExpression(): Expression =
    fadeOnHighZoomsConfig?.let {
        interpolate {
            linear()
            zoom()
            stop { literal(it.startFadingZoom); literal(restrictedRoadOpacity) }
            stop { literal(it.finishFadingZoom); literal(0.0) }
        }
    } ?: Expression.Companion.literal(restrictedRoadOpacity)

internal fun Expression.toStylePropertyValue(): StylePropertyValue =
    StylePropertyValue(this, StylePropertyValueKind.EXPRESSION)
