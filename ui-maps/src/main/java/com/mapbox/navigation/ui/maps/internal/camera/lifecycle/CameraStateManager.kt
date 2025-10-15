package com.mapbox.navigation.ui.maps.internal.camera.lifecycle

import androidx.annotation.RestrictTo
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraStateChangedObserver

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
interface CameraStateManager {

    fun registerStateChangeObserver(observer: NavigationCameraStateChangedObserver)

    fun unregisterStateChangeObserver(observer: NavigationCameraStateChangedObserver)

    fun getCurrentState(): NavigationCameraState

    fun deactivate()
}
