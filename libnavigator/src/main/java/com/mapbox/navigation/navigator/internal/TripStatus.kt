package com.mapbox.navigation.navigator.internal

import android.location.Location
import com.mapbox.navigation.base.trip.model.ElectronicHorizon
import com.mapbox.navigation.base.trip.model.RouteProgress

/**
 * State of a trip at a particular timestamp.
 *
 * @param enhancedLocation the user's location
 * @param keyPoints list of predicted locations. Might be empty.
 * @param routeProgress [RouteProgress] is progress information
 * @param offRoute *true* if user is off-route, *false* otherwise
 *
 * @param eHorizon [ElectronicHorizon] object with Electronic Horizon information
 *
 * Electronic Horizon is still **experimental**, which means that the design of the
 * APIs has open issues which may (or may not) lead to their changes in the future.
 * Roughly speaking, there is a chance that those declarations will be deprecated in the near
 * future or the semantics of their behavior may change in some way that may break some code.
 *
 * For now, Electronic Horizon only works in Free Drive.
 *
 * @see [MapboxNativeNavigator.getStatus]
 */
data class TripStatus(
    val enhancedLocation: Location,
    val keyPoints: List<Location>,
    val routeProgress: RouteProgress?,
    val offRoute: Boolean,
    val eHorizon: ElectronicHorizon
)
