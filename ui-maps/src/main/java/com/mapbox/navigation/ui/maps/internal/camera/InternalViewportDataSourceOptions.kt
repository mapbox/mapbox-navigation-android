package com.mapbox.navigation.ui.maps.internal.camera

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
data class InternalViewportDataSourceOptions(
    val ignoreMinZoomWhenFramingManeuver: Boolean,
    val overviewMode: OverviewMode,
)
