package com.mapbox.navigation.tripdata.maneuver

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.tripdata.maneuver.model.LegIndexToManeuvers

internal data class ManeuverState(
    @Volatile var routeWithManeuvers: Pair<DirectionsRoute?, List<LegIndexToManeuvers>> =
        null to emptyList(),
)
