package com.mapbox.navigation.ui.maps.route.line.api

import android.graphics.Color
import android.util.Log
import com.mapbox.bindgen.Expected
import com.mapbox.geojson.FeatureCollection
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.Layer
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.getSource
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils.getLayerVisibility
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils.initializeLayers
import com.mapbox.navigation.ui.maps.route.RouteLayerConstants
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineClearValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineError
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineExpressionProvider
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineUpdateValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteSetValue
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import com.mapbox.navigation.utils.internal.ifNonNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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

    private val jobControl = InternalJobControlFactory.createDefaultScopeJobControl()
    private val mutex = Mutex()

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
        initializeLayers(style, options)
    }

    /**
     * Applies drawing related side effects.
     *
     * @param style a valid [Style] instance
     * @param routeDrawData a [Expected<RouteLineError, RouteSetValue>]
     */
    fun renderRouteDrawData(style: Style, routeDrawData: Expected<RouteLineError, RouteSetValue>) {
        initializeLayers(style, options)
        routeDrawData.fold(
            { error ->
                Log.e(TAG, error.errorMessage)
            },
            { value ->
                val baseExpressionDeferred = jobControl.scope.async {
                    value.primaryRouteLineData
                        .dynamicData
                        .baseExpressionProvider
                        .generateExpression()
                }
                val casingExpressionDeferred = jobControl.scope.async {
                    value.primaryRouteLineData
                        .dynamicData
                        .casingExpressionProvider
                        .generateExpression()
                }
                val trafficExpressionDeferred = jobControl.scope.async {
                    value.primaryRouteLineData
                        .dynamicData
                        .trafficExpressionProvider
                        ?.generateExpression()
                }
                val restrictedSectionExpressionDeferred = jobControl.scope.async {
                    value.primaryRouteLineData
                        .dynamicData
                        .restrictedSectionExpressionProvider
                        ?.generateExpression()
                }
                val alternativeRouteLinesData1Deferred = jobControl.scope.async {
                    value.alternativeRouteLinesData[0]
                        .dynamicData
                        .trafficExpressionProvider
                        ?.generateExpression()
                }
                val alternativeRouteLinesData2Deferred = jobControl.scope.async {
                    value.alternativeRouteLinesData[1]
                        .dynamicData
                        .trafficExpressionProvider
                        ?.generateExpression()
                }

                jobControl.scope.launch(Dispatchers.Main) {
                    mutex.withLock {
                        // The gradients are set to transparent first so that when the route line
                        // layer sources are updated they don't initially reflect the wrong traffic
                        // gradient. The gradients are set on the layers not the feature collections.
                        // The set gradient call is asynchronous in the Maps SDK but the update source
                        // is not.  Setting the gradients to transparent means the traffic isn't
                        // visible with the layer sources are updated. The traffic is calculated and
                        // applied later.
                        updateLineGradient(
                            style,
                            RouteLayerConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID,
                            Expression.color(Color.TRANSPARENT)
                        )

                        updateLineGradient(
                            style,
                            RouteLayerConstants.ALTERNATIVE_ROUTE1_TRAFFIC_LAYER_ID,
                            Expression.color(Color.TRANSPARENT)
                        )

                        updateLineGradient(
                            style,
                            RouteLayerConstants.ALTERNATIVE_ROUTE2_TRAFFIC_LAYER_ID,
                            Expression.color(Color.TRANSPARENT)
                        )

                        updateLineGradient(
                            style,
                            RouteLayerConstants.RESTRICTED_ROAD_LAYER_ID,
                            Expression.color(Color.TRANSPARENT)
                        )

                        updateSource(
                            style,
                            RouteLayerConstants.PRIMARY_ROUTE_SOURCE_ID,
                            value.primaryRouteLineData.featureCollection
                        )

                        updateSource(
                            style,
                            RouteLayerConstants.ALTERNATIVE_ROUTE1_SOURCE_ID,
                            value.alternativeRouteLinesData[0].featureCollection
                        )

                        updateSource(
                            style,
                            RouteLayerConstants.ALTERNATIVE_ROUTE2_SOURCE_ID,
                            value.alternativeRouteLinesData[1].featureCollection
                        )

                        updateLineGradient(
                            style,
                            RouteLayerConstants.PRIMARY_ROUTE_LAYER_ID,
                            baseExpressionDeferred.await()
                        )

                        updateLineGradient(
                            style,
                            RouteLayerConstants.PRIMARY_ROUTE_CASING_LAYER_ID,
                            casingExpressionDeferred.await()
                        )

                        ifNonNull(trafficExpressionDeferred.await()) {
                            updateLineGradient(
                                style,
                                RouteLayerConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID,
                                it
                            )
                        }

                        ifNonNull(restrictedSectionExpressionDeferred.await()) {
                            updateLineGradient(
                                style,
                                RouteLayerConstants.RESTRICTED_ROAD_LAYER_ID,
                                it
                            )
                        }

                        // SBNOTE: In the future let's find a better way to
                        // match the items in the list with the alt. route layer ID's.
                        ifNonNull(alternativeRouteLinesData1Deferred.await()) {
                            updateLineGradient(
                                style,
                                RouteLayerConstants.ALTERNATIVE_ROUTE1_TRAFFIC_LAYER_ID,
                                it
                            )
                        }

                        ifNonNull(alternativeRouteLinesData2Deferred.await()) {
                            updateLineGradient(
                                style,
                                RouteLayerConstants.ALTERNATIVE_ROUTE2_TRAFFIC_LAYER_ID,
                                it
                            )
                        }

                        updateSource(
                            style,
                            RouteLayerConstants.WAYPOINT_SOURCE_ID,
                            value.waypointsSource
                        )
                    }
                }
            }
        )
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
        initializeLayers(style, options)
        jobControl.scope.launch(Dispatchers.Main) {
            mutex.withLock {
                update.onValue {
                    updateLineGradient(
                        style,
                        RouteLayerConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID,
                        it.primaryRouteLineDynamicData.trafficExpressionProvider
                    )
                    updateLineGradient(
                        style,
                        RouteLayerConstants.PRIMARY_ROUTE_LAYER_ID,
                        it.primaryRouteLineDynamicData.baseExpressionProvider
                    )
                    updateLineGradient(
                        style,
                        RouteLayerConstants.PRIMARY_ROUTE_CASING_LAYER_ID,
                        it.primaryRouteLineDynamicData.casingExpressionProvider
                    )
                    updateLineGradient(
                        style,
                        RouteLayerConstants.RESTRICTED_ROAD_LAYER_ID,
                        it.primaryRouteLineDynamicData.restrictedSectionExpressionProvider
                    )
                }
            }
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
        initializeLayers(style, options)
        jobControl.scope.launch(Dispatchers.Main) {
            mutex.withLock {
                clearRouteLineValue.onValue {
                    updateSource(
                        style,
                        RouteLayerConstants.PRIMARY_ROUTE_SOURCE_ID,
                        it.primaryRouteSource
                    )
                    if (it.alternativeRouteSourceSources.isNotEmpty()) {
                        updateSource(
                            style,
                            RouteLayerConstants.ALTERNATIVE_ROUTE1_SOURCE_ID,
                            it.alternativeRouteSourceSources.first()
                        )
                    }
                    if (it.alternativeRouteSourceSources.size > 1) {
                        updateSource(
                            style,
                            RouteLayerConstants.ALTERNATIVE_ROUTE2_SOURCE_ID,
                            it.alternativeRouteSourceSources[1]
                        )
                    }
                    updateSource(
                        style,
                        RouteLayerConstants.WAYPOINT_SOURCE_ID,
                        it.waypointsSource
                    )
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
                updateLayerVisibility(
                    style,
                    RouteLayerConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID, Visibility.VISIBLE
                )
                updateLayerVisibility(
                    style,
                    RouteLayerConstants.PRIMARY_ROUTE_LAYER_ID, Visibility.VISIBLE
                )
                updateLayerVisibility(
                    style,
                    RouteLayerConstants.PRIMARY_ROUTE_CASING_LAYER_ID, Visibility.VISIBLE
                )
                updateLayerVisibility(
                    style,
                    RouteLayerConstants.RESTRICTED_ROAD_LAYER_ID, Visibility.VISIBLE
                )
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
                updateLayerVisibility(
                    style,
                    RouteLayerConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID, Visibility.NONE
                )
                updateLayerVisibility(
                    style,
                    RouteLayerConstants.PRIMARY_ROUTE_LAYER_ID, Visibility.NONE
                )
                updateLayerVisibility(
                    style,
                    RouteLayerConstants.PRIMARY_ROUTE_CASING_LAYER_ID, Visibility.NONE
                )
                updateLayerVisibility(
                    style,
                    RouteLayerConstants.RESTRICTED_ROAD_LAYER_ID, Visibility.NONE
                )
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
                updateLayerVisibility(
                    style,
                    RouteLayerConstants.ALTERNATIVE_ROUTE1_LAYER_ID, Visibility.VISIBLE
                )
                updateLayerVisibility(
                    style,
                    RouteLayerConstants.ALTERNATIVE_ROUTE1_CASING_LAYER_ID, Visibility.VISIBLE
                )
                updateLayerVisibility(
                    style,
                    RouteLayerConstants.ALTERNATIVE_ROUTE2_LAYER_ID, Visibility.VISIBLE
                )
                updateLayerVisibility(
                    style,
                    RouteLayerConstants.ALTERNATIVE_ROUTE2_CASING_LAYER_ID, Visibility.VISIBLE
                )
                updateLayerVisibility(
                    style,
                    RouteLayerConstants.ALTERNATIVE_ROUTE1_TRAFFIC_LAYER_ID, Visibility.VISIBLE
                )
                updateLayerVisibility(
                    style,
                    RouteLayerConstants.ALTERNATIVE_ROUTE2_TRAFFIC_LAYER_ID, Visibility.VISIBLE
                )
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
                updateLayerVisibility(
                    style,
                    RouteLayerConstants.ALTERNATIVE_ROUTE1_LAYER_ID, Visibility.NONE
                )
                updateLayerVisibility(
                    style,
                    RouteLayerConstants.ALTERNATIVE_ROUTE1_CASING_LAYER_ID, Visibility.NONE
                )
                updateLayerVisibility(
                    style,
                    RouteLayerConstants.ALTERNATIVE_ROUTE2_LAYER_ID, Visibility.NONE
                )
                updateLayerVisibility(
                    style,
                    RouteLayerConstants.ALTERNATIVE_ROUTE2_CASING_LAYER_ID, Visibility.NONE
                )
                updateLayerVisibility(
                    style,
                    RouteLayerConstants.ALTERNATIVE_ROUTE1_TRAFFIC_LAYER_ID, Visibility.NONE
                )
                updateLayerVisibility(
                    style,
                    RouteLayerConstants.ALTERNATIVE_ROUTE2_TRAFFIC_LAYER_ID, Visibility.NONE
                )
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
                updateLayerVisibility(
                    style,
                    RouteLayerConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID, Visibility.NONE
                )
                updateLayerVisibility(
                    style,
                    RouteLayerConstants.ALTERNATIVE_ROUTE1_TRAFFIC_LAYER_ID, Visibility.NONE
                )
                updateLayerVisibility(
                    style,
                    RouteLayerConstants.ALTERNATIVE_ROUTE2_TRAFFIC_LAYER_ID, Visibility.NONE
                )
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
                updateLayerVisibility(
                    style,
                    RouteLayerConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID, Visibility.VISIBLE
                )
                updateLayerVisibility(
                    style,
                    RouteLayerConstants.ALTERNATIVE_ROUTE1_TRAFFIC_LAYER_ID, Visibility.VISIBLE
                )
                updateLayerVisibility(
                    style,
                    RouteLayerConstants.ALTERNATIVE_ROUTE2_TRAFFIC_LAYER_ID, Visibility.VISIBLE
                )
            }
        }
    }

    /**
     * Returns the visibility of the primary route map traffic layer.
     *
     * @param style an instance of the Style
     *
     * @return the visibility value returned by the map.
     */
    fun getTrafficVisibility(style: Style): Visibility? {
        return getLayerVisibility(style, RouteLayerConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID)
    }

    /**
     * Returns the visibility of the primary route map layer.
     *
     * @param style an instance of the Style
     *
     * @return the visibility value returned by the map.
     */
    fun getPrimaryRouteVisibility(style: Style): Visibility? {
        return getLayerVisibility(style, RouteLayerConstants.PRIMARY_ROUTE_LAYER_ID)
    }

    /**
     * Returns the visibility of the alternative route(s) map layer.
     *
     * @param style an instance of the Style
     *
     * @return the visibility value returned by the map.
     */
    fun getAlternativeRoutesVisibility(style: Style): Visibility? {
        return getLayerVisibility(style, RouteLayerConstants.ALTERNATIVE_ROUTE1_LAYER_ID)
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
        style.getLayer(layerId)?.visibility(visibility)
    }

    private fun updateSource(style: Style, sourceId: String, featureCollection: FeatureCollection) {
        style.getSource(sourceId)?.let {
            (it as GeoJsonSource).featureCollection(featureCollection)
        }
    }

    private fun updateLineGradient(
        style: Style,
        layerId: String,
        expressionProvider: RouteLineExpressionProvider?
    ) {
        if (expressionProvider != null) {
            updateLineGradient(style, layerId, expressionProvider.generateExpression())
        }
    }

    private fun updateLineGradient(style: Style, layerId: String, expression: Expression) {
        style.getLayer(layerId)?.let {
            (it as LineLayer).lineGradient(expression)
        }
    }
}
