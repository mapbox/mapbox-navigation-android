package com.mapbox.navigation.navigator.internal

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigator.NavigationStatus

/**
 * State of a trip at a particular timestamp.
 *
 * @param route
 * @param navigationStatus
 */
data class TripStatus(
    val route: NavigationRoute?,
    val navigationStatus: NavigationStatus,
)
