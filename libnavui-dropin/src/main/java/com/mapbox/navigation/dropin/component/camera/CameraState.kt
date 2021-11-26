package com.mapbox.navigation.dropin.component.camera

import android.location.Location
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.maps.EdgeInsets
import com.mapbox.navigation.base.trip.model.RouteProgress

internal data class CameraState(
    val targetCameraState: TargetCameraState = TargetCameraState.IDLE,
    val location: Location? = null,
    val routeProgress: RouteProgress? = null,
    val route: DirectionsRoute? = null,
    val zoomUpdatesAllowed: Boolean = false,
    val resetFrame: Boolean = false,

    // todo make padding based on screen orientation and pixel density
    val followingPadding: EdgeInsets = EdgeInsets(300.0, 300.0, 300.0, 300.0),
    val overviewPadding: EdgeInsets = EdgeInsets(300.0, 300.0, 300.0, 300.0),
)

internal fun CameraState.hasUpdatesInhibited(): Boolean {
    return !zoomUpdatesAllowed
}
