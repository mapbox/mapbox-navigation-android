package com.mapbox.navigation.ui.maps.route.line.api

import android.graphics.Color
import android.util.Log
import com.mapbox.bindgen.Expected
import com.mapbox.geojson.FeatureCollection
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.getSource
import com.mapbox.navigation.ui.base.internal.model.route.RouteConstants
import com.mapbox.navigation.ui.base.model.route.RouteLayerConstants
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils.getLayerVisibility
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils.initializeLayers
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineClearValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineError
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineUpdateValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteSetValue
import com.mapbox.navigation.utils.internal.ThreadController
import kotlinx.coroutines.async
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
 * @param options resource options used rendering the route line on the map
 */
class MapboxRouteLineView(var options: MapboxRouteLineOptions) {

    private companion object {
        private const val TAG = "MbxRouteLineView"
    }

    private val jobControl = ThreadController.getMainScopeAndRootJob()
    private val mutex = Mutex()

    /**
     * Will initialize the route line related layers. Other calls in this class will initialize
     * the layers if they have not yet been initialized. If you have a use case for initializing
     * the layers in advance of any API calls this method may be used.
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
                jobControl.scope.launch {
                    mutex.withLock {
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
                            RouteLayerConstants.PRIMARY_ROUTE_LAYER_ID,
                            value.routeLineExpression
                        )
                        updateLineGradient(
                            style,
                            RouteLayerConstants.PRIMARY_ROUTE_CASING_LAYER_ID,
                            value.casingLineExpression
                        )
                        updateSource(
                            style,
                            RouteConstants.PRIMARY_ROUTE_SOURCE_ID,
                            value.primaryRouteSource
                        )
                        updateSource(
                            style,
                            RouteConstants.ALTERNATIVE_ROUTE1_SOURCE_ID,
                            value.alternativeRoute1Source
                        )
                        updateSource(
                            style,
                            RouteConstants.ALTERNATIVE_ROUTE2_SOURCE_ID,
                            value.alternativeRoute2Source
                        )
                        updateSource(
                            style,
                            RouteConstants.WAYPOINT_SOURCE_ID,
                            value.waypointsSource
                        )
                        value.trafficLineExpressionProvider?.let {
                            val trafficExpressionDef = async(ThreadController.IODispatcher) {
                                it()
                            }
                            trafficExpressionDef.await().apply {
                                updateLineGradient(
                                    style,
                                    RouteLayerConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID,
                                    this
                                )
                            }
                        }
                        value.altRoute1TrafficExpressionProvider?.let {
                            val altRoute1TrafficExpressionDef =
                                async(ThreadController.IODispatcher) {
                                    it()
                                }
                            altRoute1TrafficExpressionDef.await().apply {
                                updateLineGradient(
                                    style,
                                    RouteLayerConstants.ALTERNATIVE_ROUTE1_TRAFFIC_LAYER_ID,
                                    this
                                )
                            }
                        }
                        value.altRoute2TrafficExpressionProvider?.let {
                            val altRoute2TrafficExpressionDef =
                                async(ThreadController.IODispatcher) {
                                    it()
                                }
                            altRoute2TrafficExpressionDef.await().apply {
                                updateLineGradient(
                                    style,
                                    RouteLayerConstants.ALTERNATIVE_ROUTE2_TRAFFIC_LAYER_ID,
                                    this
                                )
                            }
                        }
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
        jobControl.scope.launch {
            mutex.withLock {
                update.onValue {
                    updateLineGradient(
                        style,
                        RouteLayerConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID,
                        it.trafficLineExpression
                    )
                    updateLineGradient(
                        style,
                        RouteLayerConstants.PRIMARY_ROUTE_LAYER_ID,
                        it.routeLineExpression
                    )
                    updateLineGradient(
                        style,
                        RouteLayerConstants.PRIMARY_ROUTE_CASING_LAYER_ID,
                        it.casingLineExpression
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
        jobControl.scope.launch {
            mutex.withLock {
                clearRouteLineValue.onValue {
                    updateSource(
                        style,
                        RouteConstants.PRIMARY_ROUTE_SOURCE_ID,
                        it.primaryRouteSource
                    )
                    updateSource(
                        style,
                        RouteConstants.ALTERNATIVE_ROUTE1_SOURCE_ID,
                        it.altRoute1Source
                    )
                    updateSource(
                        style,
                        RouteConstants.ALTERNATIVE_ROUTE2_SOURCE_ID,
                        it.altRoute2Source
                    )
                    updateSource(
                        style,
                        RouteConstants.WAYPOINT_SOURCE_ID,
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
        jobControl.scope.launch {
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
            }
        }
    }

    /**
     * Hides the layers used for the primary route line.
     *
     * @param style an instance of the [Style]
     */
    fun hidePrimaryRoute(style: Style) {
        jobControl.scope.launch {
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
            }
        }
    }

    /**
     * Shows the layers used for the alternative route line(s).
     *
     * @param style an instance of the [Style]
     */
    fun showAlternativeRoutes(style: Style) {
        jobControl.scope.launch {
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
        jobControl.scope.launch {
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
        jobControl.scope.launch {
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
        jobControl.scope.launch {
            mutex.withLock {
                updateLayerVisibility(style, RouteLayerConstants.WAYPOINT_LAYER_ID, Visibility.NONE)
            }
        }
    }

    private fun updateLayerVisibility(style: Style, layerId: String, visibility: Visibility) {
        if (style.isStyleLoaded) {
            style.getLayer(layerId)?.visibility(visibility)
        }
    }

    private fun updateSource(style: Style, sourceId: String, featureCollection: FeatureCollection) {
        if (style.isStyleLoaded) {
            style.getSource(sourceId)?.let {
                (it as GeoJsonSource).featureCollection(featureCollection)
            }
        }
    }

    private fun updateLineGradient(style: Style, layerId: String, expression: Expression) {
        if (style.isStyleLoaded) {
            style.getLayer(layerId)?.let {
                (it as LineLayer).lineGradient(expression)
            }
        }
    }
}
