package com.mapbox.navigation.ui.maps.route.line.api

import com.mapbox.bindgen.Expected
import com.mapbox.geojson.FeatureCollection
import com.mapbox.maps.LayerPosition
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.dsl.generated.literal
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.Layer
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.getSource
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils.getLayerIdsForPrimaryRoute
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils.getLayerVisibility
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils.layerGroup1SourceLayerIds
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils.layerGroup2SourceLayerIds
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils.layerGroup3SourceLayerIds
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils.sourceLayerMap
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
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineClearValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineDynamicData
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineError
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineExpressionProvider
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineFeatureId
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineSourceKey
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineTrimExpressionProvider
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineUpdateValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteSetValue
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import com.mapbox.navigation.utils.internal.ifNonNull
import com.mapbox.navigation.utils.internal.logE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
 * @param options resource options used rendering the route line on the map
 */
class MapboxRouteLineView(var options: MapboxRouteLineOptions) {

    private companion object {
        private const val TAG = "MbxRouteLineView"
    }
    private val sourceToFeatureMap = mutableMapOf<RouteLineSourceKey, RouteLineFeatureId>(
        Pair(MapboxRouteLineUtils.layerGroup1SourceKey, RouteLineFeatureId(null)),
        Pair(MapboxRouteLineUtils.layerGroup2SourceKey, RouteLineFeatureId(null)),
        Pair(MapboxRouteLineUtils.layerGroup3SourceKey, RouteLineFeatureId(null))
    )
    private val jobControl = InternalJobControlFactory.createDefaultScopeJobControl()
    private val mutex = Mutex()

    private var primaryRouteLineLayerGroup = setOf<String>()
    private val trailCasingLayerIds = setOf(
        LAYER_GROUP_1_TRAIL_CASING,
        LAYER_GROUP_2_TRAIL_CASING,
        LAYER_GROUP_3_TRAIL_CASING
    )
    private val trailLayerIds = setOf(
        LAYER_GROUP_1_TRAIL,
        LAYER_GROUP_2_TRAIL,
        LAYER_GROUP_3_TRAIL
    )
    private val casingLayerIds = setOf(
        LAYER_GROUP_1_CASING,
        LAYER_GROUP_2_CASING,
        LAYER_GROUP_3_CASING
    )
    private val mainLayerIds = setOf(
        LAYER_GROUP_1_MAIN,
        LAYER_GROUP_2_MAIN,
        LAYER_GROUP_3_MAIN
    )
    private val trafficLayerIds = setOf(
        LAYER_GROUP_1_TRAFFIC,
        LAYER_GROUP_2_TRAFFIC,
        LAYER_GROUP_3_TRAFFIC
    )
    private val restrictedLayerIds = setOf(
        LAYER_GROUP_1_RESTRICTED,
        LAYER_GROUP_2_RESTRICTED,
        LAYER_GROUP_3_RESTRICTED
    )

    @TestOnly
    internal fun initPrimaryRouteLineLayerGroup(layerIds: Set<String>) {
        primaryRouteLineLayerGroup = layerIds
    }

    /**
     * Will initialize the route line related layers. Other calls in this class will initialize
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
        MapboxRouteLineUtils.initializeLayers(style, options)
    }

    /**
     * Applies drawing related side effects.
     *
     * @param style a valid [Style] instance
     * @param routeDrawData a [Expected<RouteLineError, RouteSetValue>]
     */
    fun renderRouteDrawData(style: Style, routeDrawData: Expected<RouteLineError, RouteSetValue>) {
        MapboxRouteLineUtils.initializeLayers(style, options)
        jobControl.scope.launch(Dispatchers.Main) {
            mutex.withLock {
                val primaryRouteTrafficVisibility = getTrafficVisibility(style)
                val primaryRouteVisibility = getPrimaryRouteVisibility(style)
                val alternativeRouteVisibility = getAlternativeRoutesVisibility(style)
                routeDrawData.fold({ error ->
                    logE(TAG, error.errorMessage)
                    listOf()
                }, { routeSetValue ->
                    updateSource(
                        style,
                        RouteLayerConstants.WAYPOINT_SOURCE_ID,
                        routeSetValue.waypointsSource
                    )
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

                    sourcesToUpdate.forEach { sourceToUpdate ->
                        val nextFeatureId = featureQueue.removeFirstOrNull()?.id()
                        val nextRouteLineData = routeLineDatas.find {
                            it.featureCollection.features()?.firstOrNull()?.id() == nextFeatureId
                        }
                        val fc: FeatureCollection =
                            nextRouteLineData?.featureCollection
                                ?: FeatureCollection.fromFeatures(listOf())
                        updateSource(style, sourceToUpdate.key.sourceId, fc)
                        sourceToFeatureMap[sourceToUpdate.key] = RouteLineFeatureId(nextFeatureId)
                    }

                    val sourceFeaturePairings = sourceToFeatureMap.toMutableList()
                    routeLineDatas.mapIndexed { index, routeLineData ->
                        val relatedSourceKey = getRelatedSourceKey(
                            routeLineData.featureCollection.features()?.firstOrNull()?.id(),
                            sourceFeaturePairings
                        ).also { sourceFeaturePairings.remove(it) }
                        ifNonNull(relatedSourceKey) { sourceKeyFeaturePair ->
                            val gradientCommands = getGradientUpdateCommands(
                                style,
                                sourceKeyFeaturePair.first,
                                routeLineData,
                                sourceLayerMap
                            ).reversed()
                            // Ignoring the trim offsets if the source was updated with an
                            // empty feature collection. Even though the call to update the
                            // source was made first the trim offset commands seem to get
                            // rendered before the source update completes resulting in an
                            // undesirable flash on the screen.
                            val trimOffsetCommands = when (sourceKeyFeaturePair.second.id()) {
                                null -> listOf()
                                else -> getTrimOffsetCommands(
                                    style,
                                    sourceKeyFeaturePair.first,
                                    routeLineData,
                                    sourceLayerMap
                                )
                            }
                            val layerMoveCommand = if (index == 0) {
                                {
                                    moveLayersUp(
                                        style,
                                        sourceKeyFeaturePair.first,
                                        sourceLayerMap
                                    )
                                }
                            } else { {} }
                            listOf(layerMoveCommand).plus(trimOffsetCommands).plus(gradientCommands)
                        } ?: listOf()
                    }
                }).flatten().forEach { mutationCommand ->
                    mutationCommand()
                }

                primaryRouteLineLayerGroup = getLayerIdsForPrimaryRoute(style, sourceLayerMap)

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
                val trafficLayerIds = setOf(
                    LAYER_GROUP_1_TRAFFIC,
                    LAYER_GROUP_2_TRAFFIC,
                    LAYER_GROUP_3_TRAFFIC
                )
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
                        updateLayerVisibility(style, it.first, visibility)
                    }
                }
            }
        }
    }

    private fun getRelatedSourceKey(
        featureId: String?,
        sourceToFeatureMap: List<Pair<RouteLineSourceKey, RouteLineFeatureId>>
    ): Pair<RouteLineSourceKey, RouteLineFeatureId>? {
        val featureIdTarget = RouteLineFeatureId(featureId)
        return sourceToFeatureMap.firstOrNull { it.second.id() == featureIdTarget.id() }
    }

    /**
     * Applies side effects related to the vanishing route line feature.
     *
     * @param style an instance of the Style
     * @param update an instance of VanishingRouteLineUpdateState
     */
    fun renderRouteLineUpdate(
        style: Style,
        update: Expected<RouteLineError, RouteLineUpdateValue>
    ) {
        MapboxRouteLineUtils.initializeLayers(style, options)
        jobControl.scope.launch(Dispatchers.Main) {
            mutex.withLock {
                update.onValue {
                    primaryRouteLineLayerGroup.map { layerId ->
                        toExpressionUpdateFun(layerId, it.primaryRouteLineDynamicData)
                    }.forEach { updateFun ->
                        updateFun(style)
                    }
                }
            }
        }
    }

    private fun getExpressionUpdateFun(
        layerId: String,
        provider: RouteLineExpressionProvider?
    ): (Style) -> Unit {
        return when (provider) {
            is RouteLineTrimExpressionProvider -> updateTrimOffset(layerId, provider)
            else -> updateLineGradient(layerId, provider)
        }
    }

    /**
     * Applies side effects related to clearing the route(s) from the map.
     *
     * @param style an instance of the Style
     * @param clearRouteLineValue an instance of ClearRouteLineState
     */
    fun renderClearRouteLineValue(
        style: Style,
        clearRouteLineValue: Expected<RouteLineError, RouteLineClearValue>
    ) {
        MapboxRouteLineUtils.initializeLayers(style, options)
        jobControl.scope.launch(Dispatchers.Main) {
            mutex.withLock {
                clearRouteLineValue.onValue { value ->
                    val primarySourceKey = getSourceKeyForPrimaryRoute(style).fold(
                        { routeSourceKey ->
                            routeSourceKey
                        }, { error ->
                        logE(TAG, error.message)
                        null
                    }
                    )?.also {
                        updateSource(
                            style,
                            it.sourceId,
                            value.primaryRouteSource
                        )
                        sourceToFeatureMap[it] =
                            RouteLineFeatureId(
                                value.primaryRouteSource.features()?.firstOrNull()?.id()
                            )
                    }
                    sourceLayerMap.keys.filter { it != primarySourceKey }
                        .forEachIndexed { index, routeLineSourceKey ->
                            if (index < value.alternativeRouteSourceSources.size) {
                                updateSource(
                                    style,
                                    routeLineSourceKey.sourceId,
                                    value.alternativeRouteSourceSources[index]
                                )
                                sourceToFeatureMap[routeLineSourceKey] =
                                    RouteLineFeatureId(
                                        value.alternativeRouteSourceSources[index]
                                            .features()
                                            ?.firstOrNull()
                                            ?.id()
                                    )
                            }
                        }
                    updateSource(
                        style,
                        RouteLayerConstants.WAYPOINT_SOURCE_ID,
                        value.waypointsSource
                    )

                    primaryRouteLineLayerGroup = setOf()
                }
            }
        }
    }

    /**
     * Shows the layers used for the primary route line.
     *
     * @param style an instance of the [Style]
     */
    fun showPrimaryRoute(style: Style) {
        jobControl.scope.launch(Dispatchers.Main) {
            mutex.withLock {
                getLayerIdsForPrimaryRoute(primaryRouteLineLayerGroup, sourceLayerMap, style)
                    .forEach { updateLayerVisibility(style, it, Visibility.VISIBLE) }
            }
        }
    }

    /**
     * Hides the layers used for the primary route line.
     *
     * @param style an instance of the [Style]
     */
    fun hidePrimaryRoute(style: Style) {
        jobControl.scope.launch(Dispatchers.Main) {
            mutex.withLock {
                getLayerIdsForPrimaryRoute(primaryRouteLineLayerGroup, sourceLayerMap, style)
                    .forEach { updateLayerVisibility(style, it, Visibility.NONE) }
            }
        }
    }

    /**
     * Shows the layers used for the alternative route line(s).
     *
     * @param style an instance of the [Style]
     */
    fun showAlternativeRoutes(style: Style) {
        jobControl.scope.launch(Dispatchers.Main) {
            mutex.withLock {
                val primaryRouteLineLayers =
                    getLayerIdsForPrimaryRoute(primaryRouteLineLayerGroup, sourceLayerMap, style)
                layerGroup1SourceLayerIds
                    .union(layerGroup2SourceLayerIds)
                    .union(layerGroup3SourceLayerIds)
                    .subtract(primaryRouteLineLayers).forEach {
                        updateLayerVisibility(style, it, Visibility.VISIBLE)
                    }
            }
        }
    }

    /**
     * Hides the layers used for the alternative route line(s).
     *
     * @param style an instance of the [Style]
     */
    fun hideAlternativeRoutes(style: Style) {
        jobControl.scope.launch(Dispatchers.Main) {
            mutex.withLock {
                val primaryRouteLineLayers =
                    getLayerIdsForPrimaryRoute(primaryRouteLineLayerGroup, sourceLayerMap, style)
                layerGroup1SourceLayerIds
                    .union(layerGroup2SourceLayerIds)
                    .union(layerGroup3SourceLayerIds)
                    .subtract(primaryRouteLineLayers).forEach {
                        updateLayerVisibility(style, it, Visibility.NONE)
                    }
            }
        }
    }

    /**
     * Hides the layers used for the traffic line(s).
     *
     * @param style an instance of the [Style]
     */
    fun hideTraffic(style: Style) {
        jobControl.scope.launch(Dispatchers.Main) {
            mutex.withLock {
                layerGroup1SourceLayerIds
                    .union(layerGroup2SourceLayerIds)
                    .union(layerGroup3SourceLayerIds)
                    .filter { it in trafficLayerIds }
                    .forEach { layerId ->
                        updateLayerVisibility(style, layerId, Visibility.NONE)
                    }
            }
        }
    }

    /**
     * Shows the layers used for the traffic line(s).
     *
     * @param style an instance of the [Style]
     */
    fun showTraffic(style: Style) {
        jobControl.scope.launch(Dispatchers.Main) {
            mutex.withLock {
                layerGroup1SourceLayerIds
                    .union(layerGroup2SourceLayerIds)
                    .union(layerGroup3SourceLayerIds)
                    .filter { it in trafficLayerIds }
                    .forEach { layerId ->
                        updateLayerVisibility(style, layerId, Visibility.VISIBLE)
                    }
            }
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
            style
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
            style
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
        jobControl.scope.launch(Dispatchers.Main) {
            mutex.withLock {
                updateLayerVisibility(
                    style,
                    RouteLayerConstants.WAYPOINT_LAYER_ID,
                    Visibility.VISIBLE
                )
            }
        }
    }

    /**
     * Sets the layer containing the origin and destination icons to not visible.
     *
     * @param style an instance of the Style
     */
    fun hideOriginAndDestinationPoints(style: Style) {
        jobControl.scope.launch(Dispatchers.Main) {
            mutex.withLock {
                updateLayerVisibility(style, RouteLayerConstants.WAYPOINT_LAYER_ID, Visibility.NONE)
            }
        }
    }

    /**
     * Cancels any/all background tasks that may be running.
     */
    fun cancel() {
        jobControl.job.cancelChildren()
    }

    private fun updateLayerVisibility(style: Style, layerId: String, visibility: Visibility) {
        if (style.styleLayerExists(layerId)) {
            style.getLayer(layerId)?.visibility(visibility)
        }
    }

    private fun updateSource(style: Style, sourceId: String, featureCollection: FeatureCollection) {
        style.getSource(sourceId)?.let {
            (it as GeoJsonSource).featureCollection(featureCollection)
        }
    }

    private fun updateTrimOffset(
        layerId: String,
        expressionProvider: RouteLineExpressionProvider?
    ): (Style) -> Unit = { style: Style ->
        ifNonNull(expressionProvider) { provider ->
            if (style.styleLayerExists(layerId)) {
                style.getLayer(layerId)?.let {
                    (it as LineLayer).lineTrimOffset(provider.generateExpression())
                }
            }
        }
    }

    private fun updateLineGradient(
        layerId: String,
        expressionProvider: RouteLineExpressionProvider?
    ): (Style) -> Unit = { style: Style ->
        ifNonNull(expressionProvider) { provider ->
            updateLineGradient(style, provider.generateExpression(), layerId)
        }
    }

    private fun updateLineGradient(style: Style, expression: Expression, vararg layerIds: String) {
        layerIds.forEach { layerId ->
            if (style.styleLayerExists(layerId)) {
                style.getLayer(layerId)?.let {
                    (it as LineLayer).lineGradient(expression)
                }
            }
        }
    }

    private fun moveLayersUp(
        style: Style,
        sourceKey: RouteLineSourceKey,
        sourceToLayerMap: Map<RouteLineSourceKey, Set<String>>
    ) {
        sourceToLayerMap[sourceKey]?.forEach {
            style.moveStyleLayer(
                it,
                LayerPosition(null, RouteLayerConstants.TOP_LEVEL_ROUTE_LINE_LAYER_ID, null)
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

    private fun updateGradientCmd(
        style: Style,
        expressionProvider: RouteLineExpressionProvider?,
        layerId: String
    ): () -> Unit {
        return {
            ifNonNull(expressionProvider) { provider ->
                updateLineGradient(style, provider.generateExpression(), layerId)
            }
        }
    }

    private fun getTrimOffsetCommands(
        style: Style,
        routeLineSourceKey: RouteLineSourceKey?,
        routeLineData: RouteLineData,
        sourceLayerMap: Map<RouteLineSourceKey, Set<String>>
    ): List<() -> Unit> {
        val mutationCommands = mutableListOf<() -> Unit>()
        val trailLayerIds = trailCasingLayerIds.plus(trailLayerIds)
        sourceLayerMap[routeLineSourceKey]?.filter { !trailLayerIds.contains(it) }
            ?.forEach { layerId ->
                val provider = ifNonNull(routeLineData.dynamicData.trimOffset?.offset) { offset ->
                    RouteLineExpressionProvider {
                        literal(listOf(0.0, offset))
                    }
                }

                mutationCommands.add { updateTrimOffset(layerId, provider)(style) }
            }
        return mutationCommands
    }

    private fun getGradientUpdateCommands(
        style: Style,
        routeLineSourceKey: RouteLineSourceKey?,
        routeLineData: RouteLineData,
        sourceLayerMap: Map<RouteLineSourceKey, Set<String>>
    ): List<() -> Unit> {
        val mutationCommands = mutableListOf<() -> Unit>()
        sourceLayerMap[routeLineSourceKey]?.forEach { layerId ->
            when (layerId) {
                in trailCasingLayerIds -> {
                    mutationCommands.add(
                        updateGradientCmd(
                            style,
                            routeLineData.dynamicData.trailCasingExpressionProvider,
                            layerId
                        )
                    )
                }
                in trailLayerIds -> {
                    mutationCommands.add(
                        updateGradientCmd(
                            style,
                            routeLineData.dynamicData.trailExpressionProvider,
                            layerId
                        )
                    )
                }
                in casingLayerIds -> {
                    mutationCommands.add(
                        updateGradientCmd(
                            style,
                            routeLineData.dynamicData.casingExpressionProvider,
                            layerId
                        )
                    )
                }
                in mainLayerIds -> {
                    mutationCommands.add(
                        updateGradientCmd(
                            style,
                            routeLineData.dynamicData.baseExpressionProvider,
                            layerId
                        )
                    )
                }
                in trafficLayerIds -> {
                    mutationCommands.add(
                        updateGradientCmd(
                            style,
                            routeLineData.dynamicData.trafficExpressionProvider,
                            layerId
                        )
                    )
                }
                in restrictedLayerIds -> {
                    mutationCommands.add(
                        updateGradientCmd(
                            style,
                            routeLineData.dynamicData.restrictedSectionExpressionProvider,
                            layerId
                        )
                    )
                }
            }
        }
        return mutationCommands
    }

    private fun toExpressionUpdateFun(
        layerId: String,
        routeLineDynamicData: RouteLineDynamicData
    ): (Style) -> Unit {
        return when (layerId) {
            in trailCasingLayerIds -> {
                routeLineDynamicData.trailCasingExpressionProvider
            }
            in trailLayerIds -> {
                routeLineDynamicData.trailExpressionProvider
            }
            in casingLayerIds -> {
                routeLineDynamicData.casingExpressionProvider
            }
            in mainLayerIds -> {
                routeLineDynamicData.baseExpressionProvider
            }
            in trafficLayerIds -> {
                routeLineDynamicData.trafficExpressionProvider
            }
            in restrictedLayerIds -> {
                routeLineDynamicData.restrictedSectionExpressionProvider
            }
            else -> null
        }.run {
            getExpressionUpdateFun(
                layerId,
                this
            )
        }
    }

    private fun getLayerIdsForPrimaryRoute(
        layerGroup: Set<String>,
        sourceToLayerMap: Map<RouteLineSourceKey, Set<String>>,
        style: Style
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
}
