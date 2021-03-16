package com.mapbox.navigation.navigator.internal

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Geometry
import com.mapbox.navigator.NavigationStatus

/**
 * State of a trip at a particular timestamp.
 *
 * @param route
 * @param routeBufferGeoJson
 * @param navigationStatus
 *
 * @see [MapboxNativeNavigator.getStatus]
 */
data class TripStatus(
    val route: DirectionsRoute?,
    val routeBufferGeoJson: Geometry?,
    val navigationStatus: NavigationStatus
)
