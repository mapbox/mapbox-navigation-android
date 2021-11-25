package com.mapbox.navigation.dropin.component.camera

import android.location.Location
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.trip.model.RouteProgress

internal data class CameraState(
    val targetCameraState: TargetCameraState = TargetCameraState.IDLE,
    val location: Location? = null,
    val routeProgress: RouteProgress? = null,
    val route: DirectionsRoute? = null,
    val zoomUpdatesAllowed: Boolean = false,
)
