package com.mapbox.navigation.core.telemetry

import com.mapbox.api.directions.v5.models.DirectionsRoute
import java.util.Date

internal data class RouteAvailable(val route: DirectionsRoute, val date: Date)
