package com.mapbox.navigation.ui.maps.camera.data

import com.mapbox.maps.CameraOptions
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState.FOLLOWING
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState.OVERVIEW

/**
 * Data object that carries the camera frames that [NavigationCamera] uses for transitions
 * and continuous updates.
 */
data class ViewportData(
    /**
     * Target camera frame to use when transitioning to [FOLLOWING] or for continuous updates when
     * already in [FOLLOWING] state.
     */
    val cameraForFollowing: CameraOptions,

    /**
     * Target camera frame to use when transitioning to [OVERVIEW] or for continuous updates when
     * already in [OVERVIEW] state.
     */
    val cameraForOverview: CameraOptions
)
