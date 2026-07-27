package com.mapbox.navigation.ui.maps.internal.camera

import androidx.annotation.RestrictTo
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState

/**
 * Internal counterpart of [NavigationCameraState] that drives the camera state machine.
 *
 * It carries the finer-grained points-overview states ([TRANSITION_TO_POINTS_OVERVIEW],
 * [POINTS_OVERVIEW]) that the public [NavigationCameraState] does not distinguish. The public
 * state is a projection of this one (see [toNavigationCameraState]) and is used purely for public
 * notifications, while this type participates in all the core logic.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
enum class NavigationCameraStateInternal {
    IDLE,
    TRANSITION_TO_FOLLOWING,
    FOLLOWING,
    TRANSITION_TO_ROUTE_OVERVIEW,
    ROUTE_OVERVIEW,
    TRANSITION_TO_POINTS_OVERVIEW,
    POINTS_OVERVIEW,
    ;

    /**
     * Projects this internal state onto the public [NavigationCameraState]. Both overview variants
     * (route and points) collapse onto the single [NavigationCameraState.OVERVIEW] /
     * [NavigationCameraState.TRANSITION_TO_OVERVIEW].
     */
    fun toNavigationCameraState(): NavigationCameraState = when (this) {
        IDLE -> NavigationCameraState.IDLE
        TRANSITION_TO_FOLLOWING -> NavigationCameraState.TRANSITION_TO_FOLLOWING
        FOLLOWING -> NavigationCameraState.FOLLOWING
        TRANSITION_TO_ROUTE_OVERVIEW,
        TRANSITION_TO_POINTS_OVERVIEW,
        -> NavigationCameraState.TRANSITION_TO_OVERVIEW
        ROUTE_OVERVIEW,
        POINTS_OVERVIEW,
        -> NavigationCameraState.OVERVIEW
    }
}
