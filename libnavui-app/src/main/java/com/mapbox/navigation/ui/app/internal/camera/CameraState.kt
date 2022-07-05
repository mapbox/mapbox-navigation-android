package com.mapbox.navigation.ui.app.internal.camera

import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState

/**
 * Defines the target camera mode desired for navigation camera
 */
sealed class TargetCameraMode {
    /**
     * Represents Idle mode.
     */
    object Idle : TargetCameraMode()

    /**
     * Represents Overview mode.
     */
    object Overview : TargetCameraMode()

    /**
     * Represents Following mode.
     */
    object Following : TargetCameraMode()
}

/**
 * Defines the state for [NavigationCamera]
 * @property cameraMode sets the [TargetCameraMode]
 * @property cameraPadding sets the camera padding for the camera viewport
 * @property mapCameraState sets the [MapView] camera state
 * @property savedCameraMode last [cameraMode] value before switching to [TargetCameraMode.Idle]
 */
data class CameraState internal constructor(
    val cameraMode: TargetCameraMode = TargetCameraMode.Idle,
    val cameraPadding: EdgeInsets = EdgeInsets(0.0, 0.0, 0.0, 0.0),
    val mapCameraState: com.mapbox.maps.CameraState? = null,
    val savedCameraMode: TargetCameraMode = TargetCameraMode.Overview
)

/**
 * Converts this [TargetCameraMode] to [NavigationCameraState].
 */
fun TargetCameraMode.toNavigationCameraState() = when (this) {
    TargetCameraMode.Idle -> NavigationCameraState.IDLE
    TargetCameraMode.Following -> NavigationCameraState.FOLLOWING
    TargetCameraMode.Overview -> NavigationCameraState.OVERVIEW
}

/**
 * Converts this [NavigationCameraState] to [TargetCameraMode]
 */
fun NavigationCameraState.toTargetCameraMode() = when (this) {
    NavigationCameraState.TRANSITION_TO_OVERVIEW,
    NavigationCameraState.OVERVIEW -> TargetCameraMode.Overview
    NavigationCameraState.TRANSITION_TO_FOLLOWING,
    NavigationCameraState.FOLLOWING -> TargetCameraMode.Following
    NavigationCameraState.IDLE -> TargetCameraMode.Idle
}
