package com.mapbox.navigation.navigator.internal

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigator.NavigationStatus

/**
 * State of a trip at a particular timestamp.
 *
 * @param route
 * @param navigationStatus
 */
data class TripStatus(
    val route: DirectionsRoute?,
    val navigationStatus: NavigationStatus
)
