package com.mapbox.navigation.ui.maps.camera.internal.lifecycle

import androidx.annotation.RestrictTo
import com.mapbox.geojson.Point

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun interface UserLocationIndicatorPositionObserver {
    fun onPositionUpdated(point: Point)
}
