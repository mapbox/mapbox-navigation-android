package com.mapbox.navigation.ui.app.internal.camera

import com.mapbox.maps.EdgeInsets
import com.mapbox.navigation.ui.app.internal.Action

/**
 * Defines actions responsible to mutate the [CameraState]
 */
sealed class CameraAction : Action {
    /**
     * Sets the [NavigationCamera] to given [mode]
     */
    data class SetCameraMode(val mode: TargetCameraMode) : CameraAction()

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
