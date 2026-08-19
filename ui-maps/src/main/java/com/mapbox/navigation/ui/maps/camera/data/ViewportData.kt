package com.mapbox.navigation.ui.maps.camera.data

import androidx.annotation.RestrictTo
import com.mapbox.maps.CameraOptions
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState.FOLLOWING
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState.OVERVIEW

/**
 * Data object that carries the camera frames that [NavigationCamera] uses for transitions
 * and continuous updates.
 */
class ViewportData
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
constructor(
    /**
     * Target camera frame to use when transitioning to [FOLLOWING] or for continuous updates when
     * already in [FOLLOWING] state.
     */
    val cameraForFollowing: CameraOptions,

    /**
     * Target camera frame to use for the route overview - when transitioning to [OVERVIEW] or for
     * continuous updates when already in [OVERVIEW] state.
     */
    val cameraForOverview: CameraOptions,

    /**
     * Target camera frame to use for the points overview (framing an arbitrary set of points),
     * which is reported publicly as [OVERVIEW].
     *
     * Equal to [cameraForOverview] for sources that do not distinguish points overview
     */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    val cameraForPointsOverview: CameraOptions = cameraForOverview,
) {

    /**
     * Creates a [ViewportData] for sources that do not distinguish points overview:
     * [cameraForPointsOverview] is set to [cameraForOverview].
     */
    constructor(
        cameraForFollowing: CameraOptions,
        cameraForOverview: CameraOptions,
    ) : this(cameraForFollowing, cameraForOverview, cameraForOverview)

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ViewportData

        if (cameraForFollowing != other.cameraForFollowing) return false
        if (cameraForOverview != other.cameraForOverview) return false
        return cameraForPointsOverview == other.cameraForPointsOverview
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = cameraForFollowing.hashCode()
        result = 31 * result + cameraForOverview.hashCode()
        result = 31 * result + cameraForPointsOverview.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "ViewportData(" +
            "cameraForFollowing=$cameraForFollowing, " +
            "cameraForOverview=$cameraForOverview, " +
            "cameraForPointsOverview=$cameraForPointsOverview" +
            ")"
    }
}
