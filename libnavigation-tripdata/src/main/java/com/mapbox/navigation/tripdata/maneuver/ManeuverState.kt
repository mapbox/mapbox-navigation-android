package com.mapbox.navigation.tripdata.maneuver

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.tripdata.maneuver.model.LegIndexToManeuvers
import java.util.concurrent.CopyOnWriteArrayList

internal data class ManeuverState(
    var route: DirectionsRoute? = null,
    val allManeuvers: MutableList<LegIndexToManeuvers> = CopyOnWriteArrayList(),
)
