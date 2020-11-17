package com.mapbox.navigation.ui.maps.route.routeline.internal

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.ui.base.MapboxView
import com.mapbox.navigation.ui.internal.route.RouteConstants.ALTERNATIVE_ROUTE_LAYER_ID
import com.mapbox.navigation.ui.internal.route.RouteConstants.PRIMARY_ROUTE_LAYER_ID
import com.mapbox.navigation.ui.maps.route.routeline.api.RouteLineAPI
import com.mapbox.navigation.ui.maps.route.routeline.api.RouteLineActions
import com.mapbox.navigation.ui.maps.route.routeline.internal.MapboxRouteLineUtils.getLayerVisibility
import com.mapbox.navigation.ui.maps.route.routeline.model.IdentifiableRoute
import com.mapbox.navigation.ui.maps.route.routeline.model.RouteLineState

class MapboxRouteLineAPI(
    private val routeLineActions: RouteLineActions,
    private var routeLineStateConsumer: MapboxView<RouteLineState>
) : RouteLineAPI {

    override fun getAlternativeRoutesVisibility(style: Style): Visibility? {
        return getLayerVisibility(style, ALTERNATIVE_ROUTE_LAYER_ID)
    }

    override fun getPrimaryRouteVisibility(style: Style): Visibility? {
        return getLayerVisibility(style, PRIMARY_ROUTE_LAYER_ID)
    }

    override fun redrawRoute() {
        routeLineActions.redraw().apply { routeLineStateConsumer.render(this) }
    }

    override fun updateViewStyle(style: Style) {
        routeLineActions.getUpdateViewStyleState(style)
            .apply { routeLineStateConsumer.render(this) }
    }

    // todo account for the use case where is a reroute during navigation and the vanishing route line
    // point is set to 0 and renders before the drawing of the new route(s) because drawing the routes
    // on the maps side is asynchronous
    override fun setRoutes(routes: List<DirectionsRoute>) {
        routeLineActions.getDrawRoutesState(routes).apply { routeLineStateConsumer.render(this) }
    }

    // todo account for the use case where is a reroute during navigation and the vanishing route line
    // point is set to 0 and renders before the drawing of the new route(s) because drawing the routes
    // on the maps side is asynchronous
    override fun setIdentifiableRoutes(routes: List<IdentifiableRoute>) {
        routeLineActions.getDrawIdentifiableRoutesState(routes).apply {
            routeLineStateConsumer.render(
                this
            )
        }
    }

    override fun clearRoutes() {
        routeLineActions.clearRouteData().apply { routeLineStateConsumer.render(this) }
    }

    override fun hidePrimaryRoute() {
        routeLineActions.getHidePrimaryRouteState().apply {
            routeLineStateConsumer.render(this)
        }
    }

    override fun showPrimaryRoute() {
        routeLineActions.getShowPrimaryRouteState().apply {
            routeLineStateConsumer.render(this)
        }
    }

    override fun hideAlternativeRoutes() {
        executeHideShow(routeLineActions::getHideAlternativeRoutesState)
    }

    override fun showAlternativeRoutes() {
        executeHideShow(routeLineActions::getShowAlternativeRoutesState)
    }

    override fun hideOriginAndDestinationPoints() {
        executeHideShow(routeLineActions::getHideOriginAndDestinationPointsState)
    }

    override fun showOriginAndDestinationPoints() {
        executeHideShow(routeLineActions::getShowOriginAndDestinationPointsState)
    }

    private fun executeHideShow(
        block: () -> RouteLineState.UpdateLayerVisibilityState
    ): Visibility? {
        block().apply {
            routeLineStateConsumer.render(this)
            this.getLayerVisibilityChanges().firstOrNull()?.apply {
                return this.second
            }
        }
        return null
    }

    override fun updateUpcomingRoutePointIndex(routeProgress: RouteProgress) {
        routeLineActions.updateUpcomingRoutePointIndex(routeProgress).apply {
            routeLineStateConsumer.render(
                this
            )
        }
    }

    override fun updateVanishingPointState(routeProgressState: RouteProgressState) {
        routeLineActions.updateVanishingPointState(routeProgressState).apply {
            routeLineStateConsumer.render(
                this
            )
        }
    }

    override fun updateTraveledRouteLine(point: Point) {
        routeLineActions.getTraveledRouteLineUpdate(point).apply {
            routeLineStateConsumer.render(
                this
            )
        }
    }

    override fun updatePrimaryRouteIndex(route: DirectionsRoute) {
        routeLineActions.getUpdatePrimaryRouteIndexState(route).apply {
            routeLineStateConsumer.render(
                this
            )
        }
    }

    override fun getPrimaryRoute(): DirectionsRoute? = routeLineActions.getPrimaryRoute()
}
