package com.mapbox.navigation.ui.maps.route.line.api

import androidx.annotation.UiThread
import androidx.annotation.VisibleForTesting
import com.mapbox.bindgen.Expected
import com.mapbox.common.toValue
import com.mapbox.geojson.FeatureCollection
import com.mapbox.maps.LayerPosition
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.dsl.generated.literal
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.Layer
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.getSource
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.maps.internal.extensions.getStyleId
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils.getLayerIdsForPrimaryRoute
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils.getLayerVisibility
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils.layerGroup1SourceLayerIds
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils.layerGroup2SourceLayerIds
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils.layerGroup3SourceLayerIds
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils.layersAreInitialized
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils.maskingLayerIds
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils.sourceLayerMap
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLineViewOptionsData
import com.mapbox.navigation.ui.maps.internal.route.line.toData
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_1_CASING
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_1_MAIN
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_1_RESTRICTED
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_1_TRAFFIC
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_1_TRAIL
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_1_TRAIL_CASING
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_2_CASING
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_2_MAIN
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_2_RESTRICTED
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_2_TRAFFIC
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_2_TRAIL
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_2_TRAIL_CASING
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_3_CASING
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_3_MAIN
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_3_RESTRICTED
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_3_TRAFFIC
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_3_TRAIL
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.LAYER_GROUP_3_TRAIL_CASING
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.MASKING_LAYER_CASING
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.MASKING_LAYER_MAIN
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.MASKING_LAYER_RESTRICTED
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.MASKING_LAYER_TRAFFIC
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.MASKING_LAYER_TRAIL
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants.MASKING_LAYER_TRAIL_CASING
import com.mapbox.navigation.ui.maps.route.line.RouteLineHistoryRecordingViewSender
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineViewDynamicOptionsBuilder
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineViewDynamicOptionsBuilderBlock
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineViewOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineClearValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineDynamicData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineError
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineFeatureId
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineSourceKey
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineUpdateValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteSetValue
import com.mapbox.navigation.ui.maps.util.MutexBasedScope
import com.mapbox.navigation.ui.maps.util.toDelayedRoutesRenderedCallback
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import com.mapbox.navigation.utils.internal.ifNonNull
import com.mapbox.navigation.utils.internal.logE
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.jetbrains.annotations.TestOnly

/**
 * Responsible for rendering side effects produced by the [MapboxRouteLineApi]. The [MapboxRouteLineApi]
 * class consumes route data from the Navigation SDK and produces the data necessary to
 * visualize one or more routes on the map. This class renders the data from the [MapboxRouteLineApi]
 * by calling the appropriate map related commands so that the map can have an appearance that is
 * consistent with the state of the navigation SDK and the application.
 *
 * Each [Layer] added to the map by this class is a persistent layer - it will survive style changes.
 * This means that if the data has not changed, it does not have to be manually redrawn after a style change.
 * See [Style.addPersistentStyleLayer].
 *
 * Many of the method calls execute tasks on a background thread. A cancel method is provided
 * in this class which will cancel the background tasks.
 *
 * If you're recreating the [MapboxRouteLineView] instance or changing [MapboxRouteLineViewOptions]
 * in runtime (see [MapboxRouteLineView.updateDynamicOptions]), make sure that your first interaction
 * restores the state and re-applies the options by calling [MapboxRouteLineApi.getRouteDrawData]
 * and passing the result to [MapboxRouteLineView.renderRouteDrawData].
 *
 * @param options Resource options used for rendering the route line on the map
 */
@UiThread
class MapboxRouteLineView @VisibleForTesting internal constructor(
    options: MapboxRouteLineViewOptions,
    private val routesExpector: RoutesExpector,
    private val dataIdHolder: DataIdHolder,
    private val sender: RouteLineHistoryRecordingViewSender,
) {

    private var optionsHolder = OptionsHolder(options)

    private var rebuildLayersOnFirstRender: Boolean = true

    private companion object {
        private const val TAG = "MbxRouteLineView"
    }

    /**
     * Creates [MapboxRouteLineView] instance.
     *
     * @param options [MapboxRouteLineViewOptions]] options used for rendering the route line on the map
     */
    constructor(
        options: MapboxRouteLineViewOptions,
    ) : this(
        options,
        RoutesExpector(),
        DataIdHolder(),
        RouteLineHistoryRecordingViewSender(),
    )

    private var primaryRouteLineLayerGroup = setOf<String>()
    private val trailCasingLayerIds = setOf(
        LAYER_GROUP_1_TRAIL_CASING,
        LAYER_GROUP_2_TRAIL_CASING,
        LAYER_GROUP_3_TRAIL_CASING,
        MASKING_LAYER_TRAIL_CASING,
    )
    private val trailLayerIds = setOf(
        LAYER_GROUP_1_TRAIL,
        LAYER_GROUP_2_TRAIL,
        LAYER_GROUP_3_TRAIL,
        MASKING_LAYER_TRAIL,
    )
    private val casingLayerIds = setOf(
        LAYER_GROUP_1_CASING,
        LAYER_GROUP_2_CASING,
        LAYER_GROUP_3_CASING,
        MASKING_LAYER_CASING,
    )
    private val mainLayerIds = setOf(
        LAYER_GROUP_1_MAIN,
        LAYER_GROUP_2_MAIN,
        LAYER_GROUP_3_MAIN,
        MASKING_LAYER_MAIN,
    )
    private val trafficLayerIds = setOf(
        LAYER_GROUP_1_TRAFFIC,
        LAYER_GROUP_2_TRAFFIC,
        LAYER_GROUP_3_TRAFFIC,
        MASKING_LAYER_TRAFFIC,
    )
    private val restrictedLayerIds = setOf(
        LAYER_GROUP_1_RESTRICTED,
        LAYER_GROUP_2_RESTRICTED,
        LAYER_GROUP_3_RESTRICTED,
        MASKING_LAYER_RESTRICTED,
    )
    private val maskingRouteLineLayerGroup = setOf(
        MASKING_LAYER_MAIN,
        MASKING_LAYER_CASING,
        MASKING_LAYER_TRAIL,
        MASKING_LAYER_TRAFFIC,
        MASKING_LAYER_TRAIL_CASING,
        MASKING_LAYER_RESTRICTED,
    )
    private val sourceToFeatureMap = mutableMapOf<RouteLineSourceKey, RouteLineFeatureId>(
        Pair(MapboxRouteLineUtils.layerGroup1SourceKey, RouteLineFeatureId(null)),
        Pair(MapboxRouteLineUtils.layerGroup2SourceKey, RouteLineFeatureId(null)),
        Pair(MapboxRouteLineUtils.layerGroup3SourceKey, RouteLineFeatureId(null)),
    )
    private val scope = MutexBasedScope(
        InternalJobControlFactory.createImmediateMainScopeJobControl().scope,
    )

    init {
        sender.sendInitialOptionsEvent(optionsHolder.data)
    }

    /**
     * Update a subset of route line options.
     *
     * @param style a style which the new options should be applied to
     * @param dynamicOptionsBlock a lambda that updates the necessary options,
     *  see [MapboxRouteLineViewDynamicOptionsBuilderBlock].
     *
     * Note that updating options doesn't re-render anything automatically.
     * For these options to be applied, you need to do the following:
     * ```
     * mapboxRouteLineView.updateDynamicOptions(style, builderBlock)
     * mapboxRouteLineApi.getRouteDrawData {
     *     mapboxRouteLineView.renderRouteDrawData(style, it)
     * }
     * ```
     */
    fun updateDynamicOptions(
        style: Style,
        dynamicOptionsBlock: MapboxRouteLineViewDynamicOptionsBuilderBlock,
    ) {
        val newHolder = OptionsHolder(
            MapboxRouteLineViewDynamicOptionsBuilder(optionsHolder.options.toBuilder())
                .apply { dynamicOptionsBlock() }
                .build(),
        )
        sender.sendUpdateDynamicOptionsEvent(style.getStyleId(), newHolder.data)
        if (this.optionsHolder != newHolder) {
            this.optionsHolder = newHolder
            MapboxRouteLineUtils.updateLayersStyling(style, newHolder.options)
        }
    }

    @TestOnly
    internal fun initPrimaryRouteLineLayerGroup(layerIds: Set<String>) {
        primaryRouteLineLayerGroup = layerIds
    }

    /**
     * Initializes the route line related layers. Other calls in this class will initialize
     * the layers if they have not yet been initialized. If you have a use case for initializing
     * the layers in advance of any API calls this method may be used.
     *
     * Each [Layer] added to the map by this class is a persistent layer - it will survive style changes.
     * This means that if the data has not changed, it does not have to be manually redrawn after a style change.
     * See [Style.addPersistentStyleLayer].
     *
     * @param style a valid [Style] instance
     */
    fun initializeLayers(style: Style) {
        sender.sendInitializeLayersEvent(style.getStyleId())
        rebuildSourcesAndLayersIfNeeded(style, optionsHolder.options)
    }

    /**
     * Applies drawing related side effects.
     *
     * @param style a valid [Style] instance
     * @param routeDrawData a [Expected<RouteLineError, RouteSetValue>]
     * @param callback [RoutesRenderedCallback] to be invoked when the routes are rendered
     */
    @ExperimentalPreviewMapboxNavigationAPI
    fun renderRouteDrawData(
        style: Style,
        routeDrawData: Expected<RouteLineError, RouteSetValue>,
        map: MapboxMap,
        callback: RoutesRenderedCallback,
    ) {
        renderRouteDrawDataInternal(
            style,
            routeDrawData,
            RoutesRenderedCallbackWrapper(map, callback.toDelayedRoutesRenderedCallback()),
        )
    }

    /**
     * Applies drawing related side effects.
     *
     * @param style a valid [Style] instance
     * @param routeDrawData a [Expected<RouteLineError, RouteSetValue>]
     */
    fun renderRouteDrawData(style: Style, routeDrawData: Expected<RouteLineError, RouteSetValue>) {
        renderRouteDrawDataInternal(style, routeDrawData, null)
    }

    /**
     * Routes expecting works the following way:
     * 1. ExpectedRoutesData#allRenderedRouteIds will have only route ids that we expect to be
     *  actually rendered on the map (as opposed of just switching the source if the route is
     *  already rendered on another source) as a result of this operation.
     * 2. ExpectedRoutesData#allClearedRouteIds will have only route ids that we expect to be
     *  removed from the map as a result of this operation (the ones that have previously been drawn
     *  and will be removed either because new routes will be drawn or, if passed
     *  `RouteSetValue#RouteLineData#FeatureCollection` is empty).
     * 3. However, the callback will be invoked with all passed route ids, not only the ones
     *  that will be actually drawn. For this reason we pass
     *  `(expectedRoutesData.allRenderedRouteIds + incomingFeatureIds).toSet()` as the first
     *  to RoutesExpector#expectRoutes. These are the ids the callback will be invoked with.
     * 4. To understand when the routes rendering/clearing finished, we use
     *  `OnSourceDataLoadedListener` from Maps SDK. To match the listener invocation with a
     *  particular operation, we use dataId: it is a monotonic increasing integer associated with a
     *  particular sourceId. See [RoutesExpector] docs on how it is used.
     */
    private fun renderRouteDrawDataInternal(
        style: Style,
        routeDrawData: Expected<RouteLineError, RouteSetValue>,
        callback: RoutesRenderedCallbackWrapper?,
    ) {
        scope.launchWithMutex {
            val holder = this.optionsHolder
            // To not depend on FreeDrive recording: we want options all the time,
            // but the object is usually created in FreeDrive
            sender.sendInitialOptionsEvent(holder.data)
            sender.sendRenderRouteDrawDataEvent(style.getStyleId(), routeDrawData)
            rebuildSourcesAndLayersIfNeeded(style, holder.options)
            val primaryRouteTrafficVisibility = getTrafficVisibility(style)
            val primaryRouteVisibility = getPrimaryRouteVisibility(style)
            val alternativeRouteVisibility = getAlternativeRoutesVisibility(style)
            val updateSourceCommands = mutableListOf<() -> Unit>()
            val mutationCommands = mutableListOf<() -> Unit>()
            routeDrawData.value?.let { routeSetValue ->
                updateSourceCommands.add {
                    updateSource(
                        style,
                        RouteLayerConstants.WAYPOINT_SOURCE_ID,
                        routeSetValue.waypointsSource,
                    )
                }
                val routeLineDatas = listOf(routeSetValue.primaryRouteLineData)
                    .plus(routeSetValue.alternativeRouteLinesData)
                val incomingFeatureIds = routeLineDatas.mapNotNull {
                    it.featureCollection.features()?.firstOrNull()?.id()
                }
                val featuresNotOnMap = routeLineDatas.map {
                    RouteLineFeatureId(it.featureCollection.features()?.firstOrNull()?.id())
                }.filter { it !in sourceToFeatureMap.values }
                val sourcesToUpdate = sourceToFeatureMap.filter {
                    it.value.id() !in incomingFeatureIds
                }
                val featureQueue = ArrayDeque(featuresNotOnMap)

                val expectedRoutesData = ExpectedRoutesToRenderData()
                sourcesToUpdate.forEach { sourceToUpdate ->
                    val nextFeatureId = featureQueue.removeFirstOrNull()
                    val nextRouteLineData = routeLineDatas.find {
                        it.featureCollection.features()?.firstOrNull()?.id() ==
                            nextFeatureId?.id()
                    }
                    val fc: FeatureCollection =
                        nextRouteLineData?.featureCollection
                            ?: FeatureCollection.fromFeatures(listOf())
                    val sourceId = sourceToUpdate.key.sourceId
                    val routeId = nextFeatureId?.id()
                    val dataId = dataIdHolder.incrementDataId(sourceId)
                    expectedRoutesData.addClearedRoute(
                        sourceId,
                        dataId,
                        sourceToFeatureMap[sourceToUpdate.key]?.id(),
                    )
                    expectedRoutesData.addRenderedRoute(sourceId, dataId, routeId)
                    updateSourceCommands.add {
                        updateSource(style, sourceToUpdate.key.sourceId, fc, dataId)
                    }
                    sourceToFeatureMap[sourceToUpdate.key] = RouteLineFeatureId(routeId)
                }
                if (callback != null) {
                    routesExpector.expectRoutes(
                        (expectedRoutesData.allRenderedRouteIds + incomingFeatureIds).toSet(),
                        expectedRoutesData.allClearedRouteIds,
                        expectedRoutesData,
                        callback,
                    )
                }

                val sourceFeaturePairings = sourceToFeatureMap.toMutableList()
                routeLineDatas.forEachIndexed { index, routeLineData ->
                    val relatedSourceKey = getRelatedSourceKey(
                        routeLineData.featureCollection.features()?.firstOrNull()?.id(),
                        sourceFeaturePairings,
                    ).also { sourceFeaturePairings.remove(it) }
                    ifNonNull(relatedSourceKey) { sourceKeyFeaturePair ->
                        val gradientCommandsDeferreds =
                            ifNonNull(routeLineData.dynamicData) { dynamicData ->
                                getGradientUpdateCommands(
                                    style,
                                    sourceKeyFeaturePair.first,
                                    dynamicData,
                                    sourceLayerMap,
                                    holder.data,
                                ).reversed()
                            } ?: listOf()

                        val maskingGradientCommandsDeferreds = when (index) {
                            0 -> ifNonNull(routeSetValue.routeLineMaskingLayerDynamicData) {
                                getGradientUpdateCommands(
                                    style,
                                    maskingLayerIds,
                                    it,
                                    holder.data,
                                )
                            } ?: listOf()

                            else -> listOf()
                        }
                        val gradientCommands =
                            (gradientCommandsDeferreds + maskingGradientCommandsDeferreds)
                                .awaitAll()

                        // Ignoring the trim offsets if the source was updated with an
                        // empty feature collection. Even though the call to update the
                        // source was made first the trim offset commands seem to get
                        // rendered before the source update completes resulting in an
                        // undesirable flash on the screen.
                        val trimOffsetCommands = when (sourceKeyFeaturePair.second.id()) {
                            null -> listOf()
                            else -> {
                                val maskingTrimCommands = if (index == 0) {
                                    listOf(
                                        createTrimOffsetCommand(
                                            routeSetValue.routeLineMaskingLayerDynamicData,
                                            MASKING_LAYER_CASING,
                                            style,
                                        ),
                                        createTrimOffsetCommand(
                                            routeSetValue.routeLineMaskingLayerDynamicData,
                                            MASKING_LAYER_MAIN,
                                            style,
                                        ),
                                        createTrimOffsetCommand(
                                            routeSetValue.routeLineMaskingLayerDynamicData,
                                            MASKING_LAYER_TRAFFIC,
                                            style,
                                        ),
                                        createTrimOffsetCommand(
                                            routeSetValue.routeLineMaskingLayerDynamicData,
                                            MASKING_LAYER_RESTRICTED,
                                            style,
                                        ),
                                    )
                                } else {
                                    listOf()
                                }
                                getTrimOffsetCommands(
                                    style,
                                    sourceKeyFeaturePair.first,
                                    routeLineData,
                                    sourceLayerMap,
                                ).plus(maskingTrimCommands)
                            }
                        }

                        val layerMoveCommand: () -> Unit = if (index == 0) {
                            {
                                moveLayersUp(
                                    style,
                                    sourceKeyFeaturePair.first,
                                    sourceLayerMap,
                                )
                            }
                        } else {
                            {}
                        }

                        mutationCommands.add(layerMoveCommand)
                        mutationCommands.addAll(trimOffsetCommands)
                        mutationCommands.addAll(gradientCommands)
                    }
                }
            }
            updateSourceCommands.forEach { it() }
            mutationCommands.forEach { mutationCommand ->
                mutationCommand()
            }

            primaryRouteLineLayerGroup = getLayerIdsForPrimaryRoute(style, sourceLayerMap)
            updateLayerScaling(style, optionsHolder.options)

            // Any layer group can host the primary route.  If a call was made to
            // hide the primary our alternative routes, that state needs to be maintained
            // until a call is made to show the line(s).  For example if there are 3
            // route lines showing on the map and a call is made to hide the primary route
            // it's expected only the alternative routes are displayed. If a user then
            // selects an alternative route in order to make it the primary route, that
            // route line should then disappear and the previously hidden primary route line
            // should appear since the state is: primary route line = hidden / alternative
            // routes = showing. Only when an API call to show the primary route is made
            // should the primary route line become visible. The snippet below adjusts
            // the layer visibility to maintain that state.
            adjustLayerVisibility(
                style,
                primaryRouteTrafficVisibility,
                primaryRouteVisibility,
                alternativeRouteVisibility,
            )
            adjustMaskingLayersVisibility(
                style,
                primaryRouteTrafficVisibility,
                primaryRouteVisibility,
            )

            val updateMaskingLayerSourceCommands =
                getSourceKeyForPrimaryRoute(style).getOrNull()?.run {
                    getMaskingLayerSourceSetCommands(style, this.sourceId)
                } ?: listOf()

            val maskingLayerMoveCommands = getMaskingLayerMoveCommands(style)
            updateMaskingLayerSourceCommands.plus(maskingLayerMoveCommands)
                .forEach { mutationCommand ->
                    mutationCommand()
                }
        }
        callback?.callback?.unlock()
    }

    private fun getRelatedSourceKey(
        featureId: String?,
        sourceToFeatureMap: List<Pair<RouteLineSourceKey, RouteLineFeatureId>>,
    ): Pair<RouteLineSourceKey, RouteLineFeatureId>? {
        val featureIdTarget = RouteLineFeatureId(featureId)
        return sourceToFeatureMap.firstOrNull { it.second.id() == featureIdTarget.id() }
    }

    /**
     * Applies side effects related to the vanishing route line feature.
     *
     * @param style an instance of the Style
     * @param update an instance of Expected<RouteLineError, RouteLineUpdateValue> received from [MapboxRouteLineApi]
     */
    fun renderRouteLineUpdate(
        style: Style,
        update: Expected<RouteLineError, RouteLineUpdateValue>,
    ) {
        sender.sendRenderRouteLineUpdateEvent(style.getStyleId(), update)
        update.onValue {
            val optionsHolder = this.optionsHolder
            scope.launchWithMutex {
                val generationCommands = mutableListOf<Deferred<() -> Unit>>()
                ifNonNull(it.primaryRouteLineDynamicData) { primaryRouteLineDynamicData ->
                    primaryRouteLineLayerGroup.forEach { layerId ->
                        val holder = chooseCommandHolder(layerId, primaryRouteLineDynamicData)
                        holder?.let {
                            generationCommands.addLaunched(
                                getGenerateCommand(it, style, layerId, optionsHolder.data),
                            )
                        }
                    }
                }

                ifNonNull(it.routeLineMaskingLayerDynamicData) { overlayData ->
                    overlayData.restrictedSectionExpressionCommandHolder?.let {
                        generationCommands.addLaunched(
                            getGenerateCommand(
                                it,
                                style,
                                MASKING_LAYER_RESTRICTED,
                                optionsHolder.data,
                            ),
                        )
                    }
                    overlayData.trafficExpressionCommandHolder?.let {
                        generationCommands.addLaunched(
                            getGenerateCommand(
                                it,
                                style,
                                MASKING_LAYER_TRAFFIC,
                                optionsHolder.data,
                            ),
                        )
                    }
                    overlayData.baseExpressionCommandHolder.let {
                        generationCommands.addLaunched(
                            getGenerateCommand(it, style, MASKING_LAYER_MAIN, optionsHolder.data),
                        )
                    }
                    overlayData.casingExpressionCommandHolder.let {
                        generationCommands.addLaunched(
                            getGenerateCommand(
                                it,
                                style,
                                MASKING_LAYER_CASING,
                                optionsHolder.data,
                            ),
                        )
                    }
                    overlayData.trailExpressionCommandHolder?.let {
                        generationCommands.addLaunched(
                            getGenerateCommand(it, style, MASKING_LAYER_TRAIL, optionsHolder.data),
                        )
                    }
                    overlayData.trailCasingExpressionCommandHolder?.let {
                        generationCommands.addLaunched(
                            getGenerateCommand(
                                it,
                                style,
                                MASKING_LAYER_TRAIL_CASING,
                                optionsHolder.data,
                            ),
                        )
                    }
                }
                val applyCommands = generationCommands.awaitAll()
                applyCommands.forEach { it() }

                ifNonNull(it.primaryRouteLineDynamicData) { overlayData ->
                    // This method (renderRouteLineUpdate) is called every time the puck movement is updated and
                    // on route progress updates.  It's not necessary to move the layers
                    // on trim offset updates. Checking the kind of update that has come in
                    // saves resources.
                    if (
                        overlayData.baseExpressionCommandHolder.applier !is LineTrimCommandApplier
                    ) {
                        getMaskingLayerMoveCommands(style).forEach { mutationCommand ->
                            mutationCommand()
                        }
                    }
                }
            }
        }
    }

    /**
     * Applies side effects related to clearing the route(s) from the map.
     *
     * @param style an instance of the Style
     * @param clearRouteLineValue an instance of [Expected<RouteLineError, RouteLineClearValue>] received from [MapboxRouteLineApi]
     */
    fun renderClearRouteLineValue(
        style: Style,
        clearRouteLineValue: Expected<RouteLineError, RouteLineClearValue>,
    ) {
        renderClearRouteLineValueInternal(style, clearRouteLineValue, null)
    }

    /**
     * Applies side effects related to the vanishing route line feature.
     *
     * @param style an instance of the Style
     * @param clearRouteLineValue an instance of [Expected<RouteLineError, RouteLineClearValue>] received from [MapboxRouteLineApi]
     * @param callback [RoutesRenderedCallback] to be invoked when the routes are cleared
     */
    @ExperimentalPreviewMapboxNavigationAPI
    fun renderClearRouteLineValue(
        style: Style,
        clearRouteLineValue: Expected<RouteLineError, RouteLineClearValue>,
        map: MapboxMap,
        callback: RoutesRenderedCallback,
    ) {
        renderClearRouteLineValueInternal(
            style,
            clearRouteLineValue,
            RoutesRenderedCallbackWrapper(map, callback.toDelayedRoutesRenderedCallback()),
        )
    }

    /**
     * See [renderRouteDrawDataInternal] on how routes expecting works.
     * Keep in mind that technically `RouteLineClearValue#RouteLineData#FeatureCollection`
     * might be non-empty, which will result in drawing, not clearing the routes.
     */
    private fun renderClearRouteLineValueInternal(
        style: Style,
        clearRouteLineValue: Expected<RouteLineError, RouteLineClearValue>,
        callback: RoutesRenderedCallbackWrapper?,
    ) {
        scope.launchWithMutex {
            val holder = optionsHolder
            sender.sendClearRouteLineValueEvent(style.getStyleId(), clearRouteLineValue)
            rebuildSourcesAndLayersIfNeeded(style, holder.options)
            clearRouteLineValue.onValue { value ->
                val expectedRoutesData = ExpectedRoutesToRenderData()
                val updateSourceCommands = mutableListOf<() -> Unit>()
                val primarySourceKey = getSourceKeyForPrimaryRoute(style).fold(
                    { routeSourceKey ->
                        routeSourceKey
                    },
                    { error ->
                        logE(TAG, error.message)
                        null
                    },
                )?.also {
                    val newDataId = dataIdHolder.incrementDataId(it.sourceId)
                    val routeId = value.primaryRouteSource.features()?.firstOrNull()?.id()
                    expectedRoutesData.addClearedRoute(
                        it.sourceId,
                        newDataId,
                        sourceToFeatureMap[it]?.id(),
                    )
                    expectedRoutesData.addRenderedRoute(it.sourceId, newDataId, routeId)
                    updateSourceCommands.add {
                        updateSource(
                            style,
                            it.sourceId,
                            value.primaryRouteSource,
                            newDataId,
                        )
                    }
                    sourceToFeatureMap[it] = RouteLineFeatureId(routeId)
                }
                sourceLayerMap.keys.filter { it != primarySourceKey }
                    .forEachIndexed { index, routeLineSourceKey ->
                        if (index < value.alternativeRoutesSources.size) {
                            val newDataId = dataIdHolder
                                .incrementDataId(routeLineSourceKey.sourceId)
                            updateSourceCommands.add {
                                updateSource(
                                    style,
                                    routeLineSourceKey.sourceId,
                                    value.alternativeRoutesSources[index],
                                    newDataId,
                                )
                            }
                            val routeId = value.alternativeRoutesSources[index]
                                .features()
                                ?.firstOrNull()
                                ?.id()
                            expectedRoutesData.addClearedRoute(
                                routeLineSourceKey.sourceId,
                                newDataId,
                                sourceToFeatureMap[routeLineSourceKey]?.id(),
                            )
                            expectedRoutesData.addRenderedRoute(
                                routeLineSourceKey.sourceId,
                                newDataId,
                                routeId,
                            )
                            sourceToFeatureMap[routeLineSourceKey] = RouteLineFeatureId(routeId)
                        }
                    }
                if (callback != null) {
                    routesExpector.expectRoutes(
                        expectedRoutesData.allRenderedRouteIds,
                        expectedRoutesData.allClearedRouteIds,
                        expectedRoutesData,
                        callback,
                    )
                }
                updateSourceCommands.forEach { it() }
                updateSource(
                    style,
                    RouteLayerConstants.WAYPOINT_SOURCE_ID,
                    value.waypointsSource,
                )

                primaryRouteLineLayerGroup = setOf()
            }
        }
        callback?.callback?.unlock()
    }

    /**
     * Shows the layers used for the primary route line.
     *
     * @param style an instance of the [Style]
     */
    fun showPrimaryRoute(style: Style) {
        sender.sendShowPrimaryRouteEvent(style.getStyleId())
        getLayerIdsForPrimaryRoute(primaryRouteLineLayerGroup, sourceLayerMap, style)
            .plus(maskingLayerIds)
            .forEach { adjustLayerVisibility(style, it, Visibility.VISIBLE) }
    }

    /**
     * Hides the layers used for the primary route line.
     *
     * @param style an instance of the [Style]
     */
    fun hidePrimaryRoute(style: Style) {
        sender.sendHidePrimaryRouteEvent(style.getStyleId())
        getLayerIdsForPrimaryRoute(
            primaryRouteLineLayerGroup,
            sourceLayerMap,
            style,
        ).plus(maskingLayerIds)
            .forEach {
                adjustLayerVisibility(style, it, Visibility.NONE)
            }
    }

    /**
     * Shows the layers used for the alternative route line(s).
     *
     * @param style an instance of the [Style]
     */
    fun showAlternativeRoutes(style: Style) {
        sender.sendShowAlternativeRoutesEvent(style.getStyleId())
        val primaryRouteLineLayers =
            getLayerIdsForPrimaryRoute(primaryRouteLineLayerGroup, sourceLayerMap, style)
        layerGroup1SourceLayerIds
            .union(layerGroup2SourceLayerIds)
            .union(layerGroup3SourceLayerIds)
            .subtract(primaryRouteLineLayers).forEach {
                adjustLayerVisibility(style, it, Visibility.VISIBLE)
            }
    }

    /**
     * Hides the layers used for the alternative route line(s).
     *
     * @param style an instance of the [Style]
     */
    fun hideAlternativeRoutes(style: Style) {
        sender.sendHideAlternativeRoutesEvent(style.getStyleId())
        val primaryRouteLineLayers =
            getLayerIdsForPrimaryRoute(primaryRouteLineLayerGroup, sourceLayerMap, style)
        layerGroup1SourceLayerIds
            .union(layerGroup2SourceLayerIds)
            .union(layerGroup3SourceLayerIds)
            .subtract(primaryRouteLineLayers).forEach {
                adjustLayerVisibility(style, it, Visibility.NONE)
            }
    }

    /**
     * Hides the layers used for the traffic line(s).
     *
     * @param style an instance of the [Style]
     */
    fun hideTraffic(style: Style) {
        sender.sendHideTrafficEvent(style.getStyleId())
        layerGroup1SourceLayerIds
            .union(layerGroup2SourceLayerIds)
            .union(layerGroup3SourceLayerIds)
            .union(maskingLayerIds)
            .filter { it in trafficLayerIds }
            .forEach { layerId ->
                adjustLayerVisibility(style, layerId, Visibility.NONE)
            }
    }

    /**
     * Shows the layers used for the traffic line(s).
     *
     * @param style an instance of the [Style]
     */
    fun showTraffic(style: Style) {
        sender.sendShowTrafficEvent(style.getStyleId())
        layerGroup1SourceLayerIds
            .union(layerGroup2SourceLayerIds)
            .union(layerGroup3SourceLayerIds)
            .union(maskingLayerIds)
            .filter { it in trafficLayerIds }
            .forEach { layerId ->
                adjustLayerVisibility(style, layerId, Visibility.VISIBLE)
            }
    }

    /**
     * Returns the visibility of the primary route map traffic layer.
     *
     * @param style an instance of the Style
     *
     * @return the visibility value returned by the map for the primary route line.
     */
    fun getTrafficVisibility(style: Style): Visibility? {
        return getLayerIdsForPrimaryRoute(
            primaryRouteLineLayerGroup,
            sourceLayerMap,
            style,
        ).firstOrNull { layerId ->
            layerId in trafficLayerIds
        }?.run {
            getLayerVisibility(style, this)
        }
    }

    /**
     * Returns the visibility of the primary route map layer.
     *
     * @param style an instance of the Style
     *
     * @return the visibility value returned by the map.
     */
    fun getPrimaryRouteVisibility(style: Style): Visibility? {
        return getLayerIdsForPrimaryRoute(
            primaryRouteLineLayerGroup,
            sourceLayerMap,
            style,
        ).firstOrNull { layerId ->
            layerId in mainLayerIds
        }?.run {
            getLayerVisibility(style, this)
        }
    }

    /**
     * Returns the visibility of the alternative route(s) map layer.
     *
     * @param style an instance of the Style
     *
     * @return the visibility value returned by the map.
     */
    fun getAlternativeRoutesVisibility(style: Style): Visibility? {
        val primaryRouteLineLayers =
            getLayerIdsForPrimaryRoute(primaryRouteLineLayerGroup, sourceLayerMap, style)
        return layerGroup1SourceLayerIds
            .union(layerGroup2SourceLayerIds)
            .union(layerGroup3SourceLayerIds)
            .subtract(primaryRouteLineLayers).firstOrNull { layerId ->
                layerId in mainLayerIds
            }?.run {
                getLayerVisibility(style, this)
            }
    }

    /**
     * Sets the layer containing the origin and destination icons to visible.
     *
     * @param style an instance of the Style
     */
    fun showOriginAndDestinationPoints(style: Style) {
        sender.sendShowOriginAndDestinationPointsEvent(style.getStyleId())
        adjustLayerVisibility(
            style,
            RouteLayerConstants.WAYPOINT_LAYER_ID,
            Visibility.VISIBLE,
        )
    }

    /**
     * Sets the layer containing the origin and destination icons to not visible.
     *
     * @param style an instance of the Style
     */
    fun hideOriginAndDestinationPoints(style: Style) {
        sender.sendHideOriginAndDestinationPointsEvent(style.getStyleId())
        adjustLayerVisibility(style, RouteLayerConstants.WAYPOINT_LAYER_ID, Visibility.NONE)
    }

    /**
     * Cancels any/all background tasks that may be running.
     */
    fun cancel() {
        sender.sendCancelEvent()
        scope.cancelChildren()
    }

    private fun adjustLayerVisibility(style: Style, layerId: String, visibility: Visibility) {
        if (style.styleLayerExists(layerId)) {
            style.getLayer(layerId)?.visibility(visibility)
        }
    }

    private fun updateSource(
        style: Style,
        sourceId: String,
        featureCollection: FeatureCollection,
        dataId: Int? = null,
    ) {
        style.getSource(sourceId)?.let {
            val stringDataId = dataId?.let { "$it" }.orEmpty()
            (it as GeoJsonSource).featureCollection(featureCollection, stringDataId)
        }
    }

    private fun updateTrimOffset(
        layerId: String,
        expression: Expression?,
    ): (Style) -> Unit = { style: Style ->
        ifNonNull(expression) { expression ->
            style.setStyleLayerProperty(
                layerId,
                "line-trim-offset",
                expression,
            )
        }
    }

    private fun moveLayersUp(
        style: Style,
        sourceKey: RouteLineSourceKey,
        sourceToLayerMap: Map<RouteLineSourceKey, Set<String>>,
    ) {
        sourceToLayerMap[sourceKey]?.forEach {
            style.moveStyleLayer(
                it,
                LayerPosition(null, RouteLayerConstants.TOP_LEVEL_ROUTE_LINE_LAYER_ID, null),
            )
        }
    }

    private fun getSourceKeyForPrimaryRoute(style: Style): Result<RouteLineSourceKey> {
        return runCatching {
            MapboxRouteLineUtils.getTopRouteLineRelatedLayerId(style)?.run {
                when (this) {
                    in layerGroup1SourceLayerIds -> {
                        MapboxRouteLineUtils.layerGroup1SourceKey
                    }

                    in layerGroup2SourceLayerIds -> {
                        MapboxRouteLineUtils.layerGroup2SourceKey
                    }

                    in layerGroup3SourceLayerIds -> {
                        MapboxRouteLineUtils.layerGroup3SourceKey
                    }

                    else -> throw NoSuchElementException()
                }
            } ?: throw NoSuchElementException()
        }
    }

    private fun getTrimOffsetCommands(
        style: Style,
        routeLineSourceKey: RouteLineSourceKey?,
        routeLineData: RouteLineData,
        sourceLayerMap: Map<RouteLineSourceKey, Set<String>>,
    ): List<() -> Unit> {
        val trailLayerIds = trailCasingLayerIds.plus(trailLayerIds)
        return sourceLayerMap[routeLineSourceKey]?.filter { !trailLayerIds.contains(it) }
            ?.map {
                createTrimOffsetCommand(routeLineData.dynamicData, it, style)
            } ?: listOf()
    }

    private fun createTrimOffsetCommand(
        dynamicData: RouteLineDynamicData?,
        layerId: String,
        style: Style,
    ): () -> Unit {
        val expression = ifNonNull(dynamicData, dynamicData?.trimOffset?.offset) { _, offset ->
            literal(listOf(0.0, offset))
        }

        return { updateTrimOffset(layerId, expression)(style) }
    }

    private suspend fun getGradientUpdateCommands(
        style: Style,
        layerIds: Set<String>,
        routeLineData: RouteLineDynamicData,
        options: RouteLineViewOptionsData,
    ): List<Deferred<() -> Unit>> {
        return layerIds.map {
            when (it) {
                in trailCasingLayerIds -> Pair(it, routeLineData.trailCasingExpressionCommandHolder)
                in trailLayerIds -> Pair(it, routeLineData.trailExpressionCommandHolder)
                in casingLayerIds -> Pair(it, routeLineData.casingExpressionCommandHolder)
                in mainLayerIds -> Pair(it, routeLineData.baseExpressionCommandHolder)
                in trafficLayerIds -> Pair(it, routeLineData.trafficExpressionCommandHolder)
                in restrictedLayerIds -> Pair(
                    it,
                    routeLineData.restrictedSectionExpressionCommandHolder,
                )

                else -> null
            }
        }.filter { it?.second != null }.map {
            coroutineScope {
                async {
                    getGenerateCommand(it!!.second!!, style, it.first, options)()
                }
            }
        }
    }

    private fun getMaskingLayerMoveCommands(style: Style): List<() -> Unit> {
        val commands = mutableListOf<() -> Unit>()
        val belowLayerIdToUse = RouteLayerConstants.TOP_LEVEL_ROUTE_LINE_LAYER_ID
        commands.add {
            style.moveStyleLayer(
                MASKING_LAYER_TRAIL_CASING,
                LayerPosition(null, belowLayerIdToUse, null),
            )
        }
        commands.add {
            style.moveStyleLayer(MASKING_LAYER_TRAIL, LayerPosition(null, belowLayerIdToUse, null))
        }
        commands.add {
            style.moveStyleLayer(MASKING_LAYER_CASING, LayerPosition(null, belowLayerIdToUse, null))
        }
        commands.add {
            style.moveStyleLayer(MASKING_LAYER_MAIN, LayerPosition(null, belowLayerIdToUse, null))
        }
        commands.add {
            style.moveStyleLayer(
                MASKING_LAYER_TRAFFIC,
                LayerPosition(null, belowLayerIdToUse, null),
            )
        }
        commands.add {
            style.moveStyleLayer(
                MASKING_LAYER_RESTRICTED,
                LayerPosition(null, belowLayerIdToUse, null),
            )
        }

        return commands
    }

    private fun getMaskingSourceId(style: Style): String? {
        return try {
            style.getStyleLayerProperty(
                MASKING_LAYER_TRAFFIC,
                "source",
            ).value.contents as String
        } catch (ex: Exception) {
            null
        }
    }

    private fun getMaskingLayerSourceSetCommands(style: Style, sourceId: String): List<() -> Unit> {
        val commands = mutableListOf<() -> Unit>()
        val maskingLayerSourceId = getMaskingSourceId(style)
        if (sourceId != maskingLayerSourceId) {
            commands.add {
                style.setStyleLayerProperty(
                    MASKING_LAYER_TRAFFIC,
                    "source",
                    sourceId.toValue(),
                )
            }
            commands.add {
                style.setStyleLayerProperty(
                    MASKING_LAYER_MAIN,
                    "source",
                    sourceId.toValue(),
                )
            }
            commands.add {
                style.setStyleLayerProperty(
                    MASKING_LAYER_CASING,
                    "source",
                    sourceId.toValue(),
                )
            }
            commands.add {
                style.setStyleLayerProperty(
                    MASKING_LAYER_TRAIL,
                    "source",
                    sourceId.toValue(),
                )
            }
            commands.add {
                style.setStyleLayerProperty(
                    MASKING_LAYER_TRAIL_CASING,
                    "source",
                    sourceId.toValue(),
                )
            }
        }
        return commands
    }

    private suspend fun getGradientUpdateCommands(
        style: Style,
        routeLineSourceKey: RouteLineSourceKey?,
        routeLineData: RouteLineDynamicData,
        sourceLayerMap: Map<RouteLineSourceKey, Set<String>>,
        options: RouteLineViewOptionsData,
    ): List<Deferred<() -> Unit>> {
        return sourceLayerMap[routeLineSourceKey]?.run {
            getGradientUpdateCommands(
                style,
                this,
                routeLineData,
                options,
            )
        } ?: listOf()
    }

    // Any layer group can host the primary route.  If a call was made to
    // hide the primary our alternative routes, that state needs to be maintained
    // until a call is made to show the line(s).  For example if there are 3
    // route lines showing on the map and a call is made to hide the primary route
    // it's expected only the alternative routes are displayed. If a user then
    // selects an alternative route in order to make it the primary route, that
    // route line should then disappear and the previously hidden primary route line
    // should appear since the state is: primary route line = hidden / alternative
    // routes = showing. Only when an API call to show the primary route is made
    // should the primary route line become visible. The snippet below adjusts
    // the layer visibility to maintain that state.
    private fun adjustLayerVisibility(
        style: Style,
        primaryRouteTrafficVisibility: Visibility?,
        primaryRouteVisibility: Visibility?,
        alternativeRouteVisibility: Visibility?,
    ) {
        sourceLayerMap.values.flatten().map { layerID ->
            when (layerID in trafficLayerIds) {
                true -> when (primaryRouteLineLayerGroup.contains(layerID)) {
                    true -> Pair(layerID, primaryRouteTrafficVisibility)
                    false -> Pair(layerID, alternativeRouteVisibility)
                }

                false -> when (primaryRouteLineLayerGroup.contains(layerID)) {
                    true -> Pair(layerID, primaryRouteVisibility)
                    false -> Pair(layerID, alternativeRouteVisibility)
                }
            }
        }.forEach {
            ifNonNull(it.second) { visibility ->
                adjustLayerVisibility(style, it.first, visibility)
            }
        }
    }

    private fun adjustMaskingLayersVisibility(
        style: Style,
        primaryRouteTrafficVisibility: Visibility?,
        primaryRouteVisibility: Visibility?,
    ) {
        primaryRouteTrafficVisibility?.apply {
            adjustLayerVisibility(style, MASKING_LAYER_TRAFFIC, this)
        }
        primaryRouteVisibility?.apply {
            adjustLayerVisibility(style, MASKING_LAYER_MAIN, this)
            adjustLayerVisibility(style, MASKING_LAYER_CASING, this)
            adjustLayerVisibility(style, MASKING_LAYER_TRAIL, this)
            adjustLayerVisibility(style, MASKING_LAYER_TRAIL, this)
            adjustLayerVisibility(style, MASKING_LAYER_TRAIL_CASING, this)
        }
    }

    private fun chooseCommandHolder(
        layerId: String,
        routeLineDynamicData: RouteLineDynamicData,
    ): RouteLineExpressionCommandHolder? {
        return when (layerId) {
            in trailCasingLayerIds -> {
                routeLineDynamicData.trailCasingExpressionCommandHolder
            }

            in trailLayerIds -> {
                routeLineDynamicData.trailExpressionCommandHolder
            }

            in casingLayerIds -> {
                routeLineDynamicData.casingExpressionCommandHolder
            }

            in mainLayerIds -> {
                routeLineDynamicData.baseExpressionCommandHolder
            }

            in trafficLayerIds -> {
                routeLineDynamicData.trafficExpressionCommandHolder
            }

            in restrictedLayerIds -> {
                routeLineDynamicData.restrictedSectionExpressionCommandHolder
            }

            else -> null
        }
    }

    private fun getLayerIdsForPrimaryRoute(
        layerGroup: Set<String>,
        sourceToLayerMap: Map<RouteLineSourceKey, Set<String>>,
        style: Style,
    ): Set<String> {
        return layerGroup.ifEmpty {
            getLayerIdsForPrimaryRoute(style, sourceToLayerMap)
        }
    }

    private fun <K, V> Map<K, V>.toMutableList(): MutableList<Pair<K, V>> {
        val mutableList = mutableListOf<Pair<K, V>>()
        this.map { Pair(it.key, it.value) }.forEach {
            mutableList.add(it)
        }
        return mutableList
    }

    /**
     * The route line layer scaling can be customized differently for various route lines.
     * Since there are no specific layers for the primary route line vs alternatives it's important
     * to adjust the scaling to the layers as different layer groups host the primary route line
     * and alternative routes.
     */
    private fun updateLayerScaling(style: Style, options: MapboxRouteLineViewOptions) {
        trailCasingLayerIds
            .union(trailLayerIds)
            .union(casingLayerIds)
            .union(mainLayerIds)
            .union(trafficLayerIds)
            .forEach {
                when (it) {
                    in primaryRouteLineLayerGroup + maskingRouteLineLayerGroup -> {
                        when (it) {
                            in casingLayerIds -> {
                                options.scaleExpressions.routeCasingLineScaleExpression
                            }

                            in trailCasingLayerIds -> {
                                options.scaleExpressions.routeCasingLineScaleExpression
                            }

                            in mainLayerIds -> {
                                options.scaleExpressions.routeLineScaleExpression
                            }

                            in trailLayerIds -> {
                                options.scaleExpressions.routeLineScaleExpression
                            }

                            in trafficLayerIds -> {
                                options.scaleExpressions.routeTrafficLineScaleExpression
                            }

                            else -> null
                        }
                    }

                    else -> {
                        when (it) {
                            in casingLayerIds -> {
                                options.scaleExpressions.alternativeRouteCasingLineScaleExpression
                            }

                            in trailCasingLayerIds -> {
                                options.scaleExpressions.alternativeRouteCasingLineScaleExpression
                            }

                            in mainLayerIds -> {
                                options.scaleExpressions.alternativeRouteLineScaleExpression
                            }

                            in trailLayerIds -> {
                                options.scaleExpressions.alternativeRouteLineScaleExpression
                            }

                            in trafficLayerIds -> {
                                options.scaleExpressions.alternativeRouteTrafficLineScaleExpression
                            }

                            else -> null
                        }
                    }
                }?.apply {
                    style.setStyleLayerProperty(it, "line-width", this)
                }
            }
    }

    private fun rebuildSourcesAndLayersIfNeeded(style: Style, options: MapboxRouteLineViewOptions) {
        if (rebuildLayersOnFirstRender || !layersAreInitialized(style, options)) {
            rebuildLayersOnFirstRender = false
            resetLayers(style, options)
        }
    }

    private fun resetLayers(style: Style, options: MapboxRouteLineViewOptions) {
        MapboxRouteLineUtils.removeLayers(style)
        MapboxRouteLineUtils.initializeLayers(style, options)
    }

    private suspend fun MutableList<Deferred<() -> Unit>>.addLaunched(
        block: suspend () -> (() -> Unit),
    ) {
        add(coroutineScope { async { block() } })
    }

    private suspend fun getGenerateCommand(
        holder: RouteLineExpressionCommandHolder,
        style: Style,
        layerId: String,
        options: RouteLineViewOptionsData,
    ): suspend () -> (() -> Unit) {
        return {
            val exp = holder.provider.generateCommand(options)
            val result: () -> Unit = {
                holder.applier.applyCommand(
                    style,
                    layerId,
                    exp,
                )
            }
            result
        }
    }

    /**
     * Stores the options for the route line view and its simplified representation
     * [RouteLineViewOptionsData] that does not depend on android context.
     */
    private data class OptionsHolder(val options: MapboxRouteLineViewOptions) {
        val data: RouteLineViewOptionsData = options.toData()
    }
}
