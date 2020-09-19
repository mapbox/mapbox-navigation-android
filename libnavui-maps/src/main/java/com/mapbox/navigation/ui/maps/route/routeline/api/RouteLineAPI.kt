package com.mapbox.navigation.ui.maps.route.routeline.api

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.ui.base.MapboxView
import com.mapbox.navigation.ui.maps.route.routeline.model.IdentifiableRoute
import com.mapbox.navigation.ui.maps.route.routeline.model.RouteLineState

interface RouteLineAPI {
    fun redrawRoute()
    fun setRoutes(routes: List<DirectionsRoute>)
    fun setIdentifiableRoutes(routes: List<IdentifiableRoute>)
    fun showPrimaryRoute()
    fun hidePrimaryRoute()
    fun hideAlternativeRoutes()
    fun showAlternativeRoutes()
    fun hideOriginAndDestinationPoints()
    fun showOriginAndDestinationPoints()
    fun clearRoutes()

    fun updateUpcomingRoutePointIndex(routeProgress: RouteProgress)
    fun updateVanishingPointState(routeProgressState: RouteProgressState)

    fun updateTraveledRouteLine(point: Point)
    fun updatePrimaryRouteIndex(route: DirectionsRoute)
    fun getPrimaryRoute(): DirectionsRoute?
    fun getPrimaryRouteVisibility(style: Style): Visibility?
    fun getAlternativeRoutesVisibility(style: Style): Visibility?
    fun updateViewStyle(style: Style)
}
