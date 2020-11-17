package com.mapbox.navigation.ui.maps.route.routeline.api

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.maps.Style
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.ui.maps.route.routeline.model.IdentifiableRoute
import com.mapbox.navigation.ui.maps.route.routeline.model.RouteLineState

interface RouteLineActions {
    fun clearRouteData(): RouteLineState.ClearRouteDataState
    fun getTraveledRouteLineUpdate(point: Point): RouteLineState.TraveledRouteLineUpdateState
    fun getDrawRoutesState(newRoutes: List<DirectionsRoute>): RouteLineState.DrawRouteState
    fun getDrawIdentifiableRoutesState(
        newRoutes: List<(IdentifiableRoute)>
    ): RouteLineState.DrawRouteState

    fun getHidePrimaryRouteState(): RouteLineState.UpdateLayerVisibilityState
    fun getShowPrimaryRouteState(): RouteLineState.UpdateLayerVisibilityState
    fun getHideAlternativeRoutesState(): RouteLineState.UpdateLayerVisibilityState
    fun getShowAlternativeRoutesState(): RouteLineState.UpdateLayerVisibilityState
    fun getHideOriginAndDestinationPointsState(): RouteLineState.UpdateLayerVisibilityState
    fun getShowOriginAndDestinationPointsState(): RouteLineState.UpdateLayerVisibilityState
    fun getPrimaryRoute(): DirectionsRoute?
    fun getUpdatePrimaryRouteIndexState(route: DirectionsRoute): RouteLineState.DrawRouteState
    fun updateUpcomingRoutePointIndex(routeProgress: RouteProgress): RouteLineState.UnitState
    fun setVanishingOffset(offset: Double): RouteLineState.TraveledRouteLineUpdateState
    fun updateVanishingPointState(
        routeProgressState: RouteProgressState
    ): RouteLineState.UpdateVanishingPointState

    fun redraw(): RouteLineState.DrawRouteState
    fun getUpdateViewStyleState(style: Style): RouteLineState.UpdateViewStyleState
}
