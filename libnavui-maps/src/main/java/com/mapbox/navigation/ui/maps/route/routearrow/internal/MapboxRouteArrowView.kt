package com.mapbox.navigation.ui.maps.route.routearrow.internal

import com.mapbox.geojson.Feature
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.getSource
import com.mapbox.navigation.ui.base.MapboxView
import com.mapbox.navigation.ui.internal.route.RouteConstants.ARROW_HEAD_SOURCE_ID
import com.mapbox.navigation.ui.internal.route.RouteConstants.ARROW_SHAFT_SOURCE_ID
import com.mapbox.navigation.ui.maps.route.routearrow.model.RouteArrowState

class MapboxRouteArrowView(): MapboxView<RouteArrowState> {

    private var style: Style? = null

    // fixme ordering is important but shouldn't be
    override fun render(state: RouteArrowState) {
        when (state) {
            is RouteArrowState.UpdateManeuverArrowState -> renderState(state)
            is RouteArrowState.UpdateRouteArrowVisibilityState -> renderState(state)
            is RouteArrowState.UpdateViewStyleState -> renderState(state)
        }
    }

    private fun renderState(state: RouteArrowState.UpdateViewStyleState) {
        style = state.getStyle()
    }

    private fun renderState(state: RouteArrowState.UpdateRouteArrowVisibilityState) {
        state.getVisibilityChanges().forEach {
            updateLayerVisibility(it.first, it.second)
        }
    }

    private fun renderState(state: RouteArrowState.UpdateManeuverArrowState) {
        state.getVisibilityChanges().forEach {
            updateLayerVisibility(it.first, it.second)
        }

        state.getArrowHeadFeature()?.apply {
            updateSource(ARROW_HEAD_SOURCE_ID, this)
        }

        state.getArrowShaftFeature()?.apply {
            updateSource(ARROW_SHAFT_SOURCE_ID, this)
        }
    }

    private fun updateLayerVisibility(layerId: String, visibility: Visibility) {
        if (style?.isFullyLoaded() == true) {
            style?.getLayer(layerId)?.visibility(visibility)
        }
    }

    private fun updateSource(sourceId: String, feature: Feature) {
        if (style?.isFullyLoaded() == true) {
            style?.getSource(sourceId)?.let {
                (it as GeoJsonSource).feature(feature)
            }
        }
    }
}
