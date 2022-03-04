package com.mapbox.navigation.dropin.component.camera

import android.location.Location
import androidx.lifecycle.asFlow
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.component.location.LocationBehavior
import com.mapbox.navigation.dropin.lifecycle.UIViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

sealed class CameraAction {
    data class UpdateLocation(val location: Location) : CameraAction()
    data class OnRecenterClicked(val animation: CameraAnimate) : CameraAction()
    data class OnOverviewClicked(val transitionTo: CameraTransition) : CameraAction()
    data class OnFollowingClicked(val transitionTo: CameraTransition) : CameraAction()
    object ToIdle : CameraAction()
    object ToOverview : CameraAction()
    object ToFollowing : CameraAction()
}

class CameraViewModel(
    private val locationBehavior: LocationBehavior,
) : UIViewModel<CameraState, CameraAction>(CameraState.initial()) {

    override fun process(
        mapboxNavigation: MapboxNavigation,
        state: CameraState,
        action: CameraAction
    ): CameraState {

        return when (action) {
            is CameraAction.OnRecenterClicked -> {
                state.location?.let {
                    val options = CameraOptions
                        .Builder()
                        .center(Point.fromLngLat(it.longitude, it.latitude))
                        .build()
                    state.copy(
                        cameraOptions = options,
                        cameraMode = state.recenterTo,
                        cameraAnimation = action.animation,
                    )
                } ?: state
            }
            is CameraAction.OnOverviewClicked -> {
                state.copy(cameraTransition = action.transitionTo)
            }
            is CameraAction.OnFollowingClicked -> {
                state.copy(cameraTransition = action.transitionTo)
            }
            is CameraAction.ToIdle -> {
                state.copy(recenterTo = state.cameraMode, cameraMode = CameraMode.IDLE)
            }
            is CameraAction.ToOverview -> {
                state.copy(cameraMode = CameraMode.OVERVIEW)
            }
            is CameraAction.ToFollowing -> {
                state.copy(cameraMode = CameraMode.FOLLOWING)
            }
            is CameraAction.UpdateLocation -> {
                state.copy(location = action.location)
            }
        }
    }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)

        mainJobControl.scope.launch {
            locationBehavior.locationLiveData.asFlow().collect {
                invoke(CameraAction.UpdateLocation(it))
            }
        }
    }
}
