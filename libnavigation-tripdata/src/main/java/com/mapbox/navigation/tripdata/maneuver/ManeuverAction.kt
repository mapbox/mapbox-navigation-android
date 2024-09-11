package com.mapbox.navigation.tripdata.maneuver

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.formatter.DistanceFormatter
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.tripdata.maneuver.model.ManeuverOptions

internal sealed class ManeuverAction {

    data class GetManeuverList(
        val routeProgress: RouteProgress,
        val maneuverState: ManeuverState,
        val maneuverOption: ManeuverOptions,
        val distanceFormatter: DistanceFormatter,
    ) : ManeuverAction()

    data class GetManeuverListWithRoute(
        val route: DirectionsRoute,
        val routeLegIndex: Int? = null,
        val maneuverState: ManeuverState,
        val maneuverOption: ManeuverOptions,
        val distanceFormatter: DistanceFormatter,
    ) : ManeuverAction()
}
