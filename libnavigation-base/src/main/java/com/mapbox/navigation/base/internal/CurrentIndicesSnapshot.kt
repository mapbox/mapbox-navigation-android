package com.mapbox.navigation.base.internal

import com.mapbox.api.directions.v5.models.DirectionsRoute

/**
 * Class holding information about a snapshot of current indices.
 * All the indices are consistent (taken from the same RouteProgress instance).
 *
 * @param legIndex index of a leg the user is currently on.
 * @param routeGeometryIndex route-wise index representing the geometry point
 * right in front of the user (see [DirectionsRoute.geometry]), null if unavailable.
 * @param legGeometryIndex leg-wise index representing the geometry point
 * right in front of the user (see [DirectionsRoute.geometry]), null if unavailable.
 */
data class CurrentIndicesSnapshot(
    val legIndex: Int = 0,
    val routeGeometryIndex: Int? = null,
    val legGeometryIndex: Int? = null,
)
