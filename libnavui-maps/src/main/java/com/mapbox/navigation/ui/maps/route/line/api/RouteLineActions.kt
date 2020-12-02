package com.mapbox.navigation.ui.maps.route.line.api

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.maps.Style
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.ui.maps.route.line.model.IdentifiableRoute
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineState

/**
 *
 */
interface RouteLineActions {

    /**
     *
     */
    fun clearRouteData(): RouteLineState.ClearRouteDataState

    /**
     *
     * @param point
     */
    fun getTraveledRouteLineUpdate(point: Point): RouteLineState.TraveledRouteLineUpdateState

    /**
     *
     * @param newRoutes
     */
    fun getDrawRoutesState(newRoutes: List<DirectionsRoute>): RouteLineState.DrawRouteState

    /**
     *
     * @param newRoutes
     */
    fun getDrawIdentifiableRoutesState(
        newRoutes: List<(IdentifiableRoute)>
    ): RouteLineState.DrawRouteState

    /**
     *
     */
    fun getHidePrimaryRouteState(): RouteLineState.UpdateLayerVisibilityState

    /**
     *
     */
    fun getShowPrimaryRouteState(): RouteLineState.UpdateLayerVisibilityState

    /**
     *
     */
    fun getHideAlternativeRoutesState(): RouteLineState.UpdateLayerVisibilityState

    /**
     *
     */
    fun getShowAlternativeRoutesState(): RouteLineState.UpdateLayerVisibilityState

    /**
     *
     */
    fun getHideOriginAndDestinationPointsState(): RouteLineState.UpdateLayerVisibilityState

    /**
     *
     */
    fun getShowOriginAndDestinationPointsState(): RouteLineState.UpdateLayerVisibilityState

    /**
     *
     */
    fun getPrimaryRoute(): DirectionsRoute?

    /**
     *
     * @param route
     */
    fun getUpdatePrimaryRouteIndexState(route: DirectionsRoute): RouteLineState.DrawRouteState

    /**
     *
     * @param routeProgress
     */
    fun updateUpcomingRoutePointIndex(routeProgress: RouteProgress): RouteLineState.UnitState

    /**
     *
     * @param offset
     */
    fun setVanishingOffset(offset: Double): RouteLineState.TraveledRouteLineUpdateState

    /**
     *
     * @param routeProgressState
     */
    fun updateVanishingPointState(
        routeProgressState: RouteProgressState
    ): RouteLineState.UpdateVanishingPointState

    /**
     *
     */
    fun redraw(): RouteLineState.DrawRouteState

    /**
     *
     * @param style
     */
    fun getUpdateViewStyleState(style: Style): RouteLineState.UpdateViewStyleState
}
