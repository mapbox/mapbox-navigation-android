package com.mapbox.navigation.dropin.component.camera

import com.mapbox.navigation.dropin.component.DropInViewModel

internal class CameraViewModel : DropInViewModel<CameraState, CameraAction>(CameraState()) {

    override suspend fun process(accumulator: CameraState, value: CameraAction): CameraState {
        return when (value) {
            CameraAction.OnRecenterButtonClicked -> accumulator.copy(
                targetCameraState = TargetCameraState.FOLLOWING,
                zoomUpdatesAllowed = true
            )
            CameraAction.OnOverviewButtonClicked -> accumulator.copy(
                targetCameraState = TargetCameraState.OVERVIEW
            )
            is CameraAction.UpdateLocation -> accumulator.copy(
                location = value.locationMatcherResult.enhancedLocation
            )
            is CameraAction.UpdateRoute -> accumulator.copy(
                route = value.routes.firstOrNull()
            )
            is CameraAction.UpdateRouteProgress -> accumulator.copy(
                routeProgress = value.routeProgress
            )
            is CameraAction.UpdateRawLocation -> accumulator.copy(
                location = value.location
            )
            CameraAction.OnZoomGestureWhileTracking -> accumulator.copy(
                zoomUpdatesAllowed = false
            )
            CameraAction.OnTrackingBroken -> accumulator.copy(
                targetCameraState = TargetCameraState.IDLE,
            )
            CameraAction.OnCameraInitialized -> accumulator.copy(
                // todo enable resetting but also clean up the state once rest happens
                // postProcessing method? callback from the consumer?
                // resetFrame = true
            )
        }
    }
}
