package com.mapbox.navigation.ui.maps.internal.camera.lifecycle

import androidx.annotation.RestrictTo
import com.mapbox.navigation.ui.maps.internal.camera.NavigationCameraStateChangedObserverInternal
import com.mapbox.navigation.ui.maps.internal.camera.NavigationCameraStateInternal

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
interface CameraStateManager {

    fun registerStateChangeObserver(observer: NavigationCameraStateChangedObserverInternal)

    fun unregisterStateChangeObserver(observer: NavigationCameraStateChangedObserverInternal)

    fun getCurrentState(): NavigationCameraStateInternal

    fun disable()
}
