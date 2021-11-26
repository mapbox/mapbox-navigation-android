package com.mapbox.navigation.dropin.component.camera

import android.location.Location
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.trip.session.LocationMatcherResult

internal sealed class CameraAction {
    data class UpdateRoute(val routes: List<DirectionsRoute>) : CameraAction()
    data class UpdateRouteProgress(val routeProgress: RouteProgress) : CameraAction()
    data class UpdateLocation(val locationMatcherResult: LocationMatcherResult) : CameraAction()
    data class UpdateRawLocation(val location: Location) : CameraAction()
    object OnRecenterButtonClicked : CameraAction()
    object OnOverviewButtonClicked : CameraAction()
    object OnTrackingBroken : CameraAction()
    object OnZoomGestureWhileTracking : CameraAction()
    object OnCameraInitialized : CameraAction()
}
