package com.mapbox.navigation.ui.maps.camera.state

import com.mapbox.navigation.ui.maps.camera.NavigationCamera
import com.mapbox.navigation.ui.maps.camera.data.ViewportDataSource

/**
 * Set of possible [NavigationCamera] states.
 */
enum class NavigationCameraState {

    /**
     * Describes state when `NavigationCamera` does not execute any transitions.
     *
     * Set after invoking [NavigationCamera.requestNavigationCameraToIdle]
     */
    IDLE,

    /**
     * Describes state when `NavigationCamera` transitions to the [FOLLOWING] state.
     *
     * Set after invoking [NavigationCamera.requestNavigationCameraToFollowing].
     */
    TRANSITION_TO_FOLLOWING,

    /**
     * Describes state when `NavigationCamera` finished transition to the following state.
     *
     * Preceded by [TRANSITION_TO_FOLLOWING].
     *
     * Set after invoking [NavigationCamera.requestNavigationCameraToFollowing].
     *
     * When in this state, each update to the [ViewportDataSource]
     * will automatically trigger another transition.
     */
    FOLLOWING,

    /**
     * Describes state when `NavigationCamera` transitions to the [OVERVIEW] state.
     *
     * Set after invoking [NavigationCamera.requestNavigationCameraToOverview].
     */
    TRANSITION_TO_OVERVIEW,

    /**
     * Describes state when `NavigationCamera` finished transition to the overview state.
     *
     * Preceded by [TRANSITION_TO_OVERVIEW].
     *
     * Set after invoking [NavigationCamera.requestNavigationCameraToOverview].
     *
     * When in this state, each update to the [ViewportDataSource]
     * will automatically trigger another transition.
     */
    OVERVIEW,
}
