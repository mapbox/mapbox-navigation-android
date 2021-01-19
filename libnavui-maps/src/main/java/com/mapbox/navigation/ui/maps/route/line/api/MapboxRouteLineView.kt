package com.mapbox.navigation.ui.maps.route.line.api

import com.mapbox.geojson.FeatureCollection
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.switchCase
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.layers.getLayerAs
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.getSource
import com.mapbox.navigation.ui.base.internal.route.RouteConstants
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils.getLayerVisibility
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils.initializeLayers
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineState

/**
 * Responsible for rendering side effects produced by the MapboxRouteLineApi. The MapboxRouteLineApi
 * class consumes route data from the Navigation SDK and produces the data necessary to
 * visualize one or more routes on the map. This class renders the data from the MapboxRouteLineApi
 * by calling the appropriate map related commands so that the map can have an appearance that is
 * consistent with the state of the navigation SDK and the application.
 *
 * @param options resource options used rendering the route line on the map
 */
class MapboxRouteLineView(var options: MapboxRouteLineOptions) {

    /**
     * Applies drawing related side effects.
     *
     * @param style a valid Style instance
     * @param routeDrawData a RouteSetState object
     */
    fun render(style: Style, routeDrawData: RouteLineState.RouteSetState) {
        initializeLayers(style, options, routeDrawData.getUIMode())

        updateLineGradient(
            style,
            RouteConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID,
            routeDrawData.getTrafficLineExpression()
        )
        updateLineGradient(
            style,
            RouteConstants.PRIMARY_ROUTE_LAYER_ID,
            routeDrawData.getRouteLineExpression()
        )
        updateLineGradient(
            style,
            RouteConstants.PRIMARY_ROUTE_CASING_LAYER_ID,
            routeDrawData.getCasingLineExpression()
        )
        updateSource(
            style,
            RouteConstants.PRIMARY_ROUTE_SOURCE_ID,
            routeDrawData.getPrimaryRouteSource()
        )
        updateSource(
            style,
            RouteConstants.ALTERNATIVE_ROUTE1_SOURCE_ID,
            routeDrawData.getAlternativeRoute1Source()
        )
        updateSource(
            style,
            RouteConstants.ALTERNATIVE_ROUTE2_SOURCE_ID,
            routeDrawData.getAlternativeRoute2Source()
        )
        updateLineGradient(
            style,
            RouteConstants.ALTERNATIVE_ROUTE1_TRAFFIC_LAYER_ID,
            routeDrawData.getAlternativeRoute1TrafficExpression()
        )
        updateLineGradient(
            style,
            RouteConstants.ALTERNATIVE_ROUTE2_TRAFFIC_LAYER_ID,
            routeDrawData.getAlternativeRoute2TrafficExpression()
        )
        updateSource(
            style,
            RouteConstants.WAYPOINT_SOURCE_ID,
            routeDrawData.getOriginAndDestinationPointsSource()
        )
    }

    /**
     * Applies side effects related to the vanishing route line feature.
     *
     * @param style an instance of the Style
     * @param vanishingRouteLineState an instance of VanishingRouteLineUpdateState
     */
    fun render(
        style: Style,
        vanishingRouteLineState: RouteLineState.VanishingRouteLineUpdateState
    ) {
        initializeLayers(style, options, vanishingRouteLineState.getUIMode())

        updateLineGradient(
            style,
            RouteConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID,
            vanishingRouteLineState.getTrafficLineExpression()
        )
        updateLineGradient(
            style,
            RouteConstants.PRIMARY_ROUTE_LAYER_ID,
            vanishingRouteLineState.getRouteLineExpression()
        )
        updateLineGradient(
            style,
            RouteConstants.PRIMARY_ROUTE_CASING_LAYER_ID,
            vanishingRouteLineState.getCasingLineExpression()
        )
    }

    /**
     * Applies side effects related to clearing the route(s) from the map.
     *
     * @param style an instance of the Style
     * @param clearRouteLineData an instance of ClearRouteLineState
     */
    fun render(style: Style, clearRouteLineData: RouteLineState.ClearRouteLineState) {
        initializeLayers(style, options, clearRouteLineData.getUIMode())

        updateSource(
            style,
            RouteConstants.PRIMARY_ROUTE_SOURCE_ID,
            clearRouteLineData.getPrimaryRouteSource()
        )
        updateSource(
            style,
            RouteConstants.ALTERNATIVE_ROUTE1_SOURCE_ID,
            clearRouteLineData.getAlternativeRoute1Source()
        )
        updateSource(
            style,
            RouteConstants.ALTERNATIVE_ROUTE2_SOURCE_ID,
            clearRouteLineData.getAlternativeRoute2Source()
        )
        updateSource(
            style,
            RouteConstants.WAYPOINT_SOURCE_ID,
            clearRouteLineData.getOriginAndDestinationPointsSource()
        )
    }

    /**
     * Applies side effects related to updating the visibility of the route line(s)
     *
     * @param style an instance of the Style
     * @param state an instance of UpdateLayerVisibilityState
     */
    fun render(style: Style, state: RouteLineState.UpdateLayerVisibilityState) {
        initializeLayers(style, options, state.getUIMode())

        state.getLayerVisibilityChanges().forEach {
            updateLayerVisibility(style, it.first, it.second)
        }
    }

    fun render(style: Style, state: RouteLineState.UpdateColorPropertiesState) {
        initializeLayers(style, options, state.getUIMode())
        state.getColorUpdates().forEach {
            updateLayerColor(style, it.first, it.second)
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
        return getLayerVisibility(style, RouteConstants.PRIMARY_ROUTE_LAYER_ID)
    }

    /**
     * Returns the visibility of the alternative route(s) map layer.
     *
     * @param style an instance of the Style
     *
     * @return the visibility value returned by the map.
     */
    fun getAlternativeRoutesVisibility(style: Style): Visibility? {
        return getLayerVisibility(style, RouteConstants.ALTERNATIVE_ROUTE1_LAYER_ID)
    }

    /**
     * Sets the layer containing the origin and destination icons to visible.
     *
     * @param style an instance of the Style
     */
    @Deprecated("This needs to be moved to its own API")
    fun showOriginAndDestinationPoints(style: Style) {
        updateLayerVisibility(style, RouteConstants.WAYPOINT_LAYER_ID, Visibility.VISIBLE)
    }

    /**
     * Sets the layer containing the origin and destination icons to not visible.
     *
     * @param style an instance of the Style
     */
    @Deprecated("This needs to be moved to its own API")
    fun hideOriginAndDestinationPoints(style: Style) {
        updateLayerVisibility(style, RouteConstants.WAYPOINT_LAYER_ID, Visibility.NONE)
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

    private fun updateLayerColor(style: Style, layerId: String, colorExpressions: List<Expression>) {
        if (style.isFullyLoaded()) {
            style.getLayerAs<LineLayer>(layerId).lineColor(
                switchCase(*colorExpressions.toTypedArray())
            )
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
