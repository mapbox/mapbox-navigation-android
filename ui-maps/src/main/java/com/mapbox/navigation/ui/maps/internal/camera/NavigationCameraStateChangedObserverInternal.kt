package com.mapbox.navigation.ui.maps.internal.camera

import androidx.annotation.RestrictTo
import androidx.annotation.UiThread

/**
 * Internal counterpart of
 * [com.mapbox.navigation.ui.maps.camera.state.NavigationCameraStateChangedObserver] that is
 * notified with the fine-grained [NavigationCameraStateInternal] (keeping the points-overview
 * distinction the public observer collapses onto overview).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun interface NavigationCameraStateChangedObserverInternal {

    /**
     * Called whenever [NavigationCameraStateInternal] changes.
     * @param navigationCameraState current state
     */
    @UiThread
    fun onNavigationCameraStateChanged(navigationCameraState: NavigationCameraStateInternal)
}
