package com.mapbox.navigation.ui.maps.internal.route.arrow

import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.ui.base.MapboxView
import com.mapbox.navigation.ui.base.internal.route.RouteConstants.ARROW_SHAFT_LINE_LAYER_ID
import com.mapbox.navigation.ui.maps.internal.route.line.MapboxRouteLineUtils
import com.mapbox.navigation.ui.maps.route.arrow.api.RouteArrowAPI
import com.mapbox.navigation.ui.maps.route.arrow.api.RouteArrowActions
import com.mapbox.navigation.ui.maps.route.arrow.model.RouteArrowState

class MapboxRouteArrowAPI(
    private val routeArrowActions: RouteArrowActions,
    private var routeArrowStateConsumer: MapboxView<RouteArrowState>
) : RouteArrowAPI {

    override fun updateViewStyle(style: Style) {
        routeArrowActions.getUpdateViewStyleState(style).apply {
            routeArrowStateConsumer.render(
                this
            )
        }
    }

    override fun getRouteArrowVisibility(style: Style): Visibility? {
        return MapboxRouteLineUtils.getLayerVisibility(style, ARROW_SHAFT_LINE_LAYER_ID)
    }

    override fun redrawArrow() {
        routeArrowActions.redraw().apply {
            routeArrowStateConsumer.render(this)
        }
    }

    override fun hideManeuverArrow() {
        executeHideShow(routeArrowActions::hideRouteArrowState)
    }

    override fun showManeuverArrow() {
        executeHideShow(routeArrowActions::showRouteArrowState)
    }

    override fun addUpComingManeuverArrow(routeProgress: RouteProgress) {
        routeArrowActions.getAddUpcomingManeuverArrowState(routeProgress).apply {
            routeArrowStateConsumer.render(this)
        }
    }

    private fun executeHideShow(block: () -> RouteArrowState.UpdateRouteArrowVisibilityState) {
        block().apply {
            routeArrowStateConsumer.render(this)
        }
    }
}
