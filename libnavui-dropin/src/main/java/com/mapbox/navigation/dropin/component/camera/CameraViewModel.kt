package com.mapbox.navigation.dropin.component.camera

import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.lifecycle.UIViewModel
import com.mapbox.navigation.ui.maps.camera.NavigationCamera

/**
 * Defines actions responsible to mutate the [CameraState]
 */
@ExperimentalPreviewMapboxNavigationAPI
sealed class CameraAction {
    /**
     * Sets the [NavigationCamera] to Idle
     */
    object ToIdle : CameraAction()
    /**
     * Sets the [NavigationCamera] to overview
     */
    object ToOverview : CameraAction()
    /**
     * Sets the [NavigationCamera] to following
     */
    object ToFollowing : CameraAction()
    /**
     * Updates the padding for camera viewport
     * @param padding camera viewport padding
     */
    data class UpdatePadding(val padding: EdgeInsets) : CameraAction()
    /**
     * Saves the [MapView] camera state to be reused after configuration changes
     * @param mapState camera state for the map view
     */
    data class SaveMapState(val mapState: com.mapbox.maps.CameraState) : CameraAction()
}

@ExperimentalPreviewMapboxNavigationAPI
internal class CameraViewModel : UIViewModel<CameraState, CameraAction>(CameraState()) {

    override fun process(
        mapboxNavigation: MapboxNavigation,
        state: CameraState,
        action: CameraAction
    ): CameraState {

        return when (action) {
            is CameraAction.ToIdle -> {
                state.copy(cameraMode = TargetCameraMode.Idle)
            }
            is CameraAction.ToOverview -> {
                state.copy(cameraMode = TargetCameraMode.Overview)
            }
            is CameraAction.ToFollowing -> {
                state.copy(cameraMode = TargetCameraMode.Following)
            }
            is CameraAction.UpdatePadding -> {
                state.copy(cameraPadding = action.padding)
            }
            is CameraAction.SaveMapState -> {
                state.copy(mapCameraState = action.mapState)
            }
        }
    }
}
