package com.mapbox.navigation.ui.maps.camera.internal

import androidx.annotation.RestrictTo
import com.mapbox.maps.MapboxMap
import com.mapbox.navigation.ui.maps.camera.data.FollowingFrameOptions
import kotlin.math.abs

/**
 * This checks if the camera padding is focused on a single point.
 * If so, it indicates that the camera is in following mode, centered on the user location indicator.
 * This is the only state when scale gestures can be executed without disrupting tracking, or when camera and puck can be fully synchronized.
 *
 * This check is needed because there are instances where the camera is in following mode but not focused on a specific pixel,
 * such as when [FollowingFrameOptions.maximizeViewableGeometryWhenPitchZero] is enabled.
 * In these cases, tracking must be interrupted when user zooms in because the camera center is not focused on the user location indicator.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun MapboxMap.isSinglePixelPadding(): Boolean {
    val mapSize = getSize()
    val mapPadding = cameraState.padding
    return abs(mapSize.width - (mapPadding.left + mapPadding.right)) < 1.0 &&
        abs(mapSize.height - (mapPadding.top + mapPadding.bottom)) < 1.0
}
