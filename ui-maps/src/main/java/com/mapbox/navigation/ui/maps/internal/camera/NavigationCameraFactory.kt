package com.mapbox.navigation.ui.maps.internal.camera

import androidx.annotation.RestrictTo
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin
import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSource
import com.mapbox.navigation.ui.maps.camera.transition.DefaultSimplifiedUpdateFrameTransitionProvider
import com.mapbox.navigation.ui.maps.camera.transition.NavigationCameraStateTransitionProvider

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
object NavigationCameraFactory {

    fun create(
        mapboxMap: MapboxMap,
        cameraPlugin: CameraAnimationsPlugin,
        viewportDataSource: ViewportDataSource,
        transitionProvider: NavigationCameraStateTransitionProvider,
        simplifiedUpdateFrameTransitionProvider: SimplifiedUpdateFrameTransitionProvider =
            DefaultSimplifiedUpdateFrameTransitionProvider(cameraPlugin),
    ): NavigationCamera {
        return NavigationCamera(
            mapboxMap,
            cameraPlugin,
            viewportDataSource,
            transitionProvider,
            simplifiedUpdateFrameTransitionProvider,
        )
    }
}
