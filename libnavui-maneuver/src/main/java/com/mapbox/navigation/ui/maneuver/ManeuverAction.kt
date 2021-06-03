package com.mapbox.navigation.ui.maneuver

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.navigation.base.formatter.DistanceFormatter
import com.mapbox.navigation.base.trip.model.RouteProgress

internal sealed class ManeuverAction {

    data class GetManeuverList(
        val routeProgress: RouteProgress,
        val maneuverState: ManeuverState,
        val distanceFormatter: DistanceFormatter
    ) : ManeuverAction()

    data class GetManeuverListWithRoute(
        val route: DirectionsRoute,
        val routeLeg: RouteLeg? = null,
        val maneuverState: ManeuverState,
        val distanceFormatter: DistanceFormatter
    ) : ManeuverAction()
}
