package com.mapbox.navigation.ui.maps.route.routeline.internal

import com.mapbox.geojson.FeatureCollection
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.getSource
import com.mapbox.navigation.ui.base.MapboxView
import com.mapbox.navigation.ui.internal.route.RouteConstants.ALTERNATIVE_ROUTE_SOURCE_ID
import com.mapbox.navigation.ui.internal.route.RouteConstants.PRIMARY_ROUTE_CASING_LAYER_ID
import com.mapbox.navigation.ui.internal.route.RouteConstants.PRIMARY_ROUTE_LAYER_ID
import com.mapbox.navigation.ui.internal.route.RouteConstants.PRIMARY_ROUTE_SOURCE_ID
import com.mapbox.navigation.ui.internal.route.RouteConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID
import com.mapbox.navigation.ui.internal.route.RouteConstants.WAYPOINT_SOURCE_ID
import com.mapbox.navigation.ui.maps.route.routeline.model.RouteLineState

class MapboxRouteLineView() : MapboxView<RouteLineState> {

    private var style: Style? = null

    override fun render(state: RouteLineState) {
        when (state) {
            is RouteLineState.ClearRouteDataState -> renderState(state)
            is RouteLineState.TraveledRouteLineUpdateState.TraveledRouteLineUpdate -> renderState(
                state
            )
            is RouteLineState.TraveledRouteLineUpdateState.TraveledRouteLineNoUpdate -> renderState(
                state
            )
            is RouteLineState.DrawRouteState -> renderState(state)
            is RouteLineState.UpdateLayerVisibilityState -> renderState(state)
            is RouteLineState.PrimaryRouteState -> renderState(state)
            is RouteLineState.UnitState -> renderState(state)
            is RouteLineState.UpdateVanishingPointState -> renderState(state)
            is RouteLineState.RouteProgressChangeState -> renderState(state)
            is RouteLineState.UpdateViewStyleState -> renderState(state)
        }
    }

    private fun renderState(state: RouteLineState.RouteProgressChangeState) {
        when (state) {
            is RouteLineState.RouteProgressChangeState.ReInitializeRouteLineState -> {
                updateSource(PRIMARY_ROUTE_SOURCE_ID, state.getPrimaryRouteSource())
                updateSource(ALTERNATIVE_ROUTE_SOURCE_ID, state.getAlternateRoutesSource())
                updateSource(WAYPOINT_SOURCE_ID, state.getWaypointsSource())
                updateLineGradient(PRIMARY_ROUTE_TRAFFIC_LAYER_ID, state.getTrafficLineExpression())
                updateLineGradient(PRIMARY_ROUTE_LAYER_ID, state.getRouteLineExpression())
                updateLineGradient(PRIMARY_ROUTE_CASING_LAYER_ID, state.getCasingLineExpression())
            }
            is RouteLineState.RouteProgressChangeState.RedrawRouteState -> {
                renderState(state.getDrawRouteState())
            }
            is RouteLineState.RouteProgressChangeState.RouteProgressUpdatedState -> {
                // nothing to do here
            }
        }
    }

    private fun renderState(state: RouteLineState.ClearRouteDataState) {
        updateSource(PRIMARY_ROUTE_SOURCE_ID, state.getPrimaryRouteSource())
        updateSource(ALTERNATIVE_ROUTE_SOURCE_ID, state.getAlternateRoutesSource())
        updateSource(WAYPOINT_SOURCE_ID, state.getWaypointsSource())
    }

    private fun renderState(
        state: RouteLineState.TraveledRouteLineUpdateState.TraveledRouteLineUpdate
    ) {
        updateLineGradient(PRIMARY_ROUTE_TRAFFIC_LAYER_ID, state.getTrafficExpression())
        updateLineGradient(PRIMARY_ROUTE_LAYER_ID, state.getRouteLineExpression())
        updateLineGradient(PRIMARY_ROUTE_CASING_LAYER_ID, state.getCasingLineExpression())
    }

    private fun renderState(
        state: RouteLineState.TraveledRouteLineUpdateState.TraveledRouteLineNoUpdate
    ) {
        // nothing to do here
    }

    private fun renderState(state: RouteLineState.UpdateVanishingPointState) {
        // nothing to do here
    }

    private fun renderState(state: RouteLineState.DrawRouteState) {
        updateLineGradient(PRIMARY_ROUTE_TRAFFIC_LAYER_ID, state.getTrafficLineExpression())
        updateLineGradient(PRIMARY_ROUTE_LAYER_ID, state.getRouteLineExpression())
        updateLineGradient(PRIMARY_ROUTE_CASING_LAYER_ID, state.getCasingLineExpression())
        updateSource(PRIMARY_ROUTE_SOURCE_ID, state.getPrimaryRouteSource())
        updateSource(ALTERNATIVE_ROUTE_SOURCE_ID, state.getAlternateRoutesSource())
        updateSource(WAYPOINT_SOURCE_ID, state.getWaypointsSource())
    }

    private fun renderState(state: RouteLineState.UpdateLayerVisibilityState) {
        state.getLayerVisibilityChanges().forEach {
            updateLayerVisibility(it.first, it.second)
        }
    }

    private fun renderState(state: RouteLineState.PrimaryRouteState) {
        // nothing to do here
    }

    private fun renderState(state: RouteLineState.UnitState) {
        // nothing to do here
    }

    private fun renderState(state: RouteLineState.UpdateViewStyleState) {
        this.style = state.getStyle()
    }

    private fun updateLayerVisibility(layerId: String, visibility: Visibility) {
        if (style?.isFullyLoaded() == true) {
            style?.getLayer(layerId)?.visibility(visibility)
        }
    }

    private fun updateSource(sourceId: String, featureCollection: FeatureCollection) {
        if (style?.isFullyLoaded() == true) {
            style?.getSource(sourceId)?.let {
                (it as GeoJsonSource).featureCollection(featureCollection)
            }
        }
    }

    private fun updateLineGradient(layerId: String, expression: Expression) {
        if (style?.isFullyLoaded() == true) {
            style?.getLayer(layerId)?.let {
                (it as LineLayer).lineGradient(expression)
            }
        }
    }
}
