package com.mapbox.navigation.ui.tripprogress

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteProgress

internal sealed class TripProgressAction {
    data class CalculateTripDetails(val route: NavigationRoute) : TripProgressAction()
    data class CalculateTripProgress(val routeProgress: RouteProgress) : TripProgressAction()
}
