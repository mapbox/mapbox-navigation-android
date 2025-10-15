package com.mapbox.navigation.ui.maps.internal.camera

import android.content.Context
import androidx.annotation.RestrictTo
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.plugin.gestures.GesturesPlugin
import com.mapbox.navigation.ui.maps.camera.lifecycle.NavigationScaleGestureActionListener
import com.mapbox.navigation.ui.maps.camera.lifecycle.NavigationScaleGestureHandler
import com.mapbox.navigation.ui.maps.camera.lifecycle.NavigationScaleGestureHandlerOptions
import com.mapbox.navigation.ui.maps.internal.camera.lifecycle.CameraStateManager
import com.mapbox.navigation.ui.maps.internal.camera.lifecycle.UserLocationIndicatorPositionProvider

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
object NavigationScaleGestureHandlerFactory {

    fun create(
        context: Context,
        cameraStateManager: CameraStateManager,
        mapboxMap: MapboxMap,
        gesturesPlugin: GesturesPlugin,
        userLocationIndicatorPositionProvider: UserLocationIndicatorPositionProvider,
        scaleGestureActionListener: NavigationScaleGestureActionListener?,
        options: NavigationScaleGestureHandlerOptions,
    ): NavigationScaleGestureHandler {
        return NavigationScaleGestureHandler(
            context,
            cameraStateManager,
            mapboxMap,
            gesturesPlugin,
            userLocationIndicatorPositionProvider,
            scaleGestureActionListener,
            options,
        )
    }
}
