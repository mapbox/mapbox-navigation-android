package com.mapbox.navigation.ui.maps.camera.internal

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
data class InternalViewportDataSourceOptions(
    val ignoreMinZoomWhenFramingManeuver: Boolean,
)
