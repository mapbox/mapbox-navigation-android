package com.mapbox.navigation.ui.maps.internal.camera

import androidx.annotation.RestrictTo
import com.mapbox.maps.MapboxMap
import com.mapbox.navigation.ui.maps.camera.data.MapboxNavigationViewportDataSource

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
object MapboxNavigationViewportDataSourceFactory {

    fun create(
        map: MapboxMap,
        followingFramingModeHolder: FollowingFramingModeHolder,
        overviewViewportDataSource: OverviewViewportDataSource,
    ): MapboxNavigationViewportDataSource {
        return MapboxNavigationViewportDataSource(
            map,
            followingFramingModeHolder,
            overviewViewportDataSource,
        )
    }
}
