package com.mapbox.navigation.ui.maps.internal.camera

import androidx.annotation.RestrictTo
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.ui.maps.camera.data.FollowingFrameOptions
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSourceProcessor

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun isFramingManeuverInternal(
    routeProgress: RouteProgress,
    followingFrameOptions: FollowingFrameOptions,
): Boolean {
    return ViewportDataSourceProcessor.isFramingManeuver(routeProgress, followingFrameOptions)
}
