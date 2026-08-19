package com.mapbox.navigation.ui.maps.internal.camera

import androidx.annotation.RestrictTo

/**
 * Internal options consumed by `MapboxNavigationViewportDataSource` to control following framing.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
data class InternalFollowingOverviewOptions(
    val ignoreMinZoomWhenFramingManeuver: Boolean,
    val allowCameraFramingForHighZoom: Boolean,
)
