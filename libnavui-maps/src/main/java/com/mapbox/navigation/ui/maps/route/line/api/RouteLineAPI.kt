package com.mapbox.navigation.ui.maps.route.line.api

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.ui.maps.route.line.model.IdentifiableRoute

/**
 *
 */
interface RouteLineAPI {

    /**
     *
     */
    fun redrawRoute()

    /**
     *
     * @param routes
     */
    fun setRoutes(routes: List<DirectionsRoute>)

    /**
     *
     * @param routes
     */
    fun setIdentifiableRoutes(routes: List<IdentifiableRoute>)

    /**
     *
     */
    fun showPrimaryRoute()

    /**
     *
     */
    fun hidePrimaryRoute()

    /**
     *
     */
    fun hideAlternativeRoutes()

    /**
     *
     */
    fun showAlternativeRoutes()

    /**
     *
     */
    fun hideOriginAndDestinationPoints()

    /**
     *
     */
    fun showOriginAndDestinationPoints()

    /**
     *
     */
    fun clearRoutes()

    /**
     *
     * @param routeProgress
     */
    fun updateUpcomingRoutePointIndex(routeProgress: RouteProgress)

    /**
     *
     * @param routeProgressState
     */
    fun updateVanishingPointState(routeProgressState: RouteProgressState)

    /**
     *
     * @param point
     */
    fun updateTraveledRouteLine(point: Point)

    /**
     *
     * @param route
     */
    fun updatePrimaryRouteIndex(route: DirectionsRoute)

    /**
     *
     */
    fun getPrimaryRoute(): DirectionsRoute?

    /**
     *
     * @param style
     */
    fun getPrimaryRouteVisibility(style: Style): Visibility?

    /**
     *
     * @param style
     */
    fun getAlternativeRoutesVisibility(style: Style): Visibility?

    /**
     *
     * @param style
     */
    fun updateViewStyle(style: Style)
}
