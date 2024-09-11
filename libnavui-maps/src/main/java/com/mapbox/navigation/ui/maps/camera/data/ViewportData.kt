package com.mapbox.navigation.ui.maps.camera.data

import com.mapbox.maps.CameraOptions
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState.FOLLOWING
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState.OVERVIEW

/**
 * Data object that carries the camera frames that [NavigationCamera] uses for transitions
 * and continuous updates.
 */
class ViewportData(
    /**
     * Target camera frame to use when transitioning to [FOLLOWING] or for continuous updates when
     * already in [FOLLOWING] state.
     */
    val cameraForFollowing: CameraOptions,

    /**
     * Target camera frame to use when transitioning to [OVERVIEW] or for continuous updates when
     * already in [OVERVIEW] state.
     */
    val cameraForOverview: CameraOptions,
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ViewportData

        if (cameraForFollowing != other.cameraForFollowing) return false
        return cameraForOverview == other.cameraForOverview
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = cameraForFollowing.hashCode()
        result = 31 * result + cameraForOverview.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "ViewportData(" +
            "cameraForFollowing=$cameraForFollowing, " +
            "cameraForOverview=$cameraForOverview" +
            ")"
    }
}
