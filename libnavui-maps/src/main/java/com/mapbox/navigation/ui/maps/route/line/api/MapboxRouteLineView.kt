package com.mapbox.navigation.ui.maps.route.line.api

import com.mapbox.geojson.FeatureCollection
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.getSource
import com.mapbox.navigation.ui.base.internal.model.route.RouteConstants
import com.mapbox.navigation.ui.base.model.Expected
import com.mapbox.navigation.ui.base.model.route.RouteLayerConstants
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils.getLayerVisibility
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils.initializeLayers
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineClearValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineError
import com.mapbox.navigation.ui.maps.route.line.model.RouteSetValue
import com.mapbox.navigation.ui.maps.route.line.model.VanishingRouteLineUpdateValue

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
     * @param routeDrawData a [Expected<RouteSetValue, RouteLineError>]
     */
    fun renderRouteDrawData(style: Style, routeDrawData: Expected<RouteSetValue, RouteLineError>) {
        initializeLayers(style, options)

        when (routeDrawData) {
            is Expected.Success -> {
                updateLineGradient(
                    style,
                    RouteLayerConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID,
                    routeDrawData.value.trafficLineExpression
                )
                updateLineGradient(
                    style,
                    RouteLayerConstants.PRIMARY_ROUTE_LAYER_ID,
                    routeDrawData.value.routeLineExpression
                )
                updateLineGradient(
                    style,
                    RouteLayerConstants.PRIMARY_ROUTE_CASING_LAYER_ID,
                    routeDrawData.value.casingLineExpression
                )
                updateSource(
                    style,
                    RouteConstants.PRIMARY_ROUTE_SOURCE_ID,
                    routeDrawData.value.primaryRouteSource
                )
                updateSource(
                    style,
                    RouteConstants.ALTERNATIVE_ROUTE1_SOURCE_ID,
                    routeDrawData.value.alternativeRoute1Source
                )
                updateSource(
                    style,
                    RouteConstants.ALTERNATIVE_ROUTE2_SOURCE_ID,
                    routeDrawData.value.alternativeRoute2Source
                )
                updateLineGradient(
                    style,
                    RouteLayerConstants.ALTERNATIVE_ROUTE1_TRAFFIC_LAYER_ID,
                    routeDrawData.value.altRoute1TrafficExpression
                )
                updateLineGradient(
                    style,
                    RouteLayerConstants.ALTERNATIVE_ROUTE2_TRAFFIC_LAYER_ID,
                    routeDrawData.value.altRoute2TrafficExpression
                )
                updateSource(
                    style,
                    RouteConstants.WAYPOINT_SOURCE_ID,
                    routeDrawData.value.waypointsSource
                )
                updateSource(
                    style,
                    RouteConstants.RESTRICTED_ROAD_SOURCE_ID,
                    routeDrawData.value.restrictedRoadSource
                )
            }
            is Expected.Failure -> { }
        }
    }

    /**
     * Applies side effects related to the vanishing route line feature.
     *
     * @param style an instance of the Style
     * @param update an instance of VanishingRouteLineUpdateState
     */
    fun renderVanishingRouteLineUpdateValue(
        style: Style,
        update: Expected<VanishingRouteLineUpdateValue, RouteLineError>
    ) {
        when (update) {
            is Expected.Failure -> { }
            is Expected.Success -> {
                initializeLayers(style, options)

                updateLineGradient(
                    style,
                    RouteLayerConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID,
                    update.value.trafficLineExpression
                )
                updateLineGradient(
                    style,
                    RouteLayerConstants.PRIMARY_ROUTE_LAYER_ID,
                    update.value.routeLineExpression
                )
                updateLineGradient(
                    style,
                    RouteLayerConstants.PRIMARY_ROUTE_CASING_LAYER_ID,
                    update.value.casingLineExpression
                )
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
        clearRouteLineValue: Expected<RouteLineClearValue, RouteLineError>
    ) {
        when (clearRouteLineValue) {
            is Expected.Failure -> { }
            is Expected.Success -> {
                initializeLayers(style, options)

                updateSource(
                    style,
                    RouteConstants.PRIMARY_ROUTE_SOURCE_ID,
                    clearRouteLineValue.value.primaryRouteSource
                )
                updateSource(
                    style,
                    RouteConstants.ALTERNATIVE_ROUTE1_SOURCE_ID,
                    clearRouteLineValue.value.altRoute1Source
                )
                updateSource(
                    style,
                    RouteConstants.ALTERNATIVE_ROUTE2_SOURCE_ID,
                    clearRouteLineValue.value.altRoute2Source
                )
                updateSource(
                    style,
                    RouteConstants.WAYPOINT_SOURCE_ID,
                    clearRouteLineValue.value.waypointsSource
                )
            }
        }
    }

    /**
     * Shows the layers used for the primary route line.
     *
     * @param style an instance of the [Style]
     */
    fun showPrimaryRoute(style: Style) {
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

    /**
     * Hides the layers used for the primary route line.
     *
     * @param style an instance of the [Style]
     */
    fun hidePrimaryRoute(style: Style) {
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

    /**
     * Shows the layers used for the alternative route line(s).
     *
     * @param style an instance of the [Style]
     */
    fun showAlternativeRoutes(style: Style) {
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

    /**
     * Hides the layers used for the alternative route line(s).
     *
     * @param style an instance of the [Style]
     */
    fun hideAlternativeRoutes(style: Style) {
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
        updateLayerVisibility(style, RouteLayerConstants.WAYPOINT_LAYER_ID, Visibility.VISIBLE)
    }

    /**
     * Sets the layer containing the origin and destination icons to not visible.
     *
     * @param style an instance of the Style
     */
    fun hideOriginAndDestinationPoints(style: Style) {
        updateLayerVisibility(style, RouteLayerConstants.WAYPOINT_LAYER_ID, Visibility.NONE)
    }

    private fun updateLayerVisibility(style: Style, layerId: String, visibility: Visibility) {
        if (style.isFullyLoaded()) {
            style.getLayer(layerId)?.visibility(visibility)
        }
    }

    private fun updateSource(style: Style, sourceId: String, featureCollection: FeatureCollection) {
        if (style.isFullyLoaded()) {
            style.getSource(sourceId)?.let {
                (it as GeoJsonSource).featureCollection(featureCollection)
            }
        }
    }

    private fun updateLineGradient(style: Style, layerId: String, expression: Expression) {
        if (style.isFullyLoaded()) {
            style.getLayer(layerId)?.let {
                (it as LineLayer).lineGradient(expression)
            }
        }
    }
}
