package com.mapbox.navigation.dropin.component.routeoverview

import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteOverviewButtonProcessorTest {

    @Test
    fun processVisibilityState_empty() {
        val result = RouteOverviewButtonProcessor.ProcessVisibilityState(
            NavigationState.Empty,
            NavigationCameraState.IDLE
        ).process()

        assertFalse(result.isVisible)
    }

    @Test
    fun processVisibilityState_freeDrive() {
        val result = RouteOverviewButtonProcessor.ProcessVisibilityState(
            NavigationState.FreeDrive,
            NavigationCameraState.IDLE
        ).process()

        assertFalse(result.isVisible)
    }

    @Test
    fun processVisibilityState_routePreview_camera_idle() {
        val result = RouteOverviewButtonProcessor.ProcessVisibilityState(
            NavigationState.RoutePreview,
            NavigationCameraState.IDLE
        ).process()

        assertTrue(result.isVisible)
    }

    @Test
    fun processVisibilityState_routePreview_camera_transitionToFollowing() {
        val result = RouteOverviewButtonProcessor.ProcessVisibilityState(
            NavigationState.RoutePreview,
            NavigationCameraState.TRANSITION_TO_FOLLOWING
        ).process()

        assertTrue(result.isVisible)
    }

    @Test
    fun processVisibilityState_routePreview_camera_following() {
        val result = RouteOverviewButtonProcessor.ProcessVisibilityState(
            NavigationState.RoutePreview,
            NavigationCameraState.FOLLOWING
        ).process()

        assertTrue(result.isVisible)
    }

    @Test
    fun processVisibilityState_routePreview_camera_transitionToOverview() {
        val result = RouteOverviewButtonProcessor.ProcessVisibilityState(
            NavigationState.RoutePreview,
            NavigationCameraState.TRANSITION_TO_OVERVIEW
        ).process()

        assertFalse(result.isVisible)
    }

    @Test
    fun processVisibilityState_routePreview_camera_overview() {
        val result = RouteOverviewButtonProcessor.ProcessVisibilityState(
            NavigationState.RoutePreview,
            NavigationCameraState.OVERVIEW
        ).process()

        assertFalse(result.isVisible)
    }

    @Test
    fun processVisibilityState_activeNavigation_camera_idle() {
        val result = RouteOverviewButtonProcessor.ProcessVisibilityState(
            NavigationState.ActiveNavigation,
            NavigationCameraState.IDLE
        ).process()

        assertTrue(result.isVisible)
    }

    @Test
    fun processVisibilityState_activeNavigation_camera_transitionToFollowing() {
        val result = RouteOverviewButtonProcessor.ProcessVisibilityState(
            NavigationState.ActiveNavigation,
            NavigationCameraState.TRANSITION_TO_FOLLOWING
        ).process()

        assertTrue(result.isVisible)
    }

    @Test
    fun processVisibilityState_activeNavigation_camera_following() {
        val result = RouteOverviewButtonProcessor.ProcessVisibilityState(
            NavigationState.ActiveNavigation,
            NavigationCameraState.FOLLOWING
        ).process()

        assertTrue(result.isVisible)
    }

    @Test
    fun processVisibilityState_activeNavigation_camera_transitionToOverview() {
        val result = RouteOverviewButtonProcessor.ProcessVisibilityState(
            NavigationState.ActiveNavigation,
            NavigationCameraState.TRANSITION_TO_OVERVIEW
        ).process()

        assertFalse(result.isVisible)
    }

    @Test
    fun processVisibilityState_activeNavigation_camera_overview() {
        val result = RouteOverviewButtonProcessor.ProcessVisibilityState(
            NavigationState.ActiveNavigation,
            NavigationCameraState.OVERVIEW
        ).process()

        assertFalse(result.isVisible)
    }

    @Test
    fun processVisibilityState_arrival_camera_idle() {
        val result = RouteOverviewButtonProcessor.ProcessVisibilityState(
            NavigationState.Arrival,
            NavigationCameraState.IDLE
        ).process()

        assertTrue(result.isVisible)
    }

    @Test
    fun processVisibilityState_arrival_camera_transitionToFollowing() {
        val result = RouteOverviewButtonProcessor.ProcessVisibilityState(
            NavigationState.Arrival,
            NavigationCameraState.TRANSITION_TO_FOLLOWING
        ).process()

        assertTrue(result.isVisible)
    }

    @Test
    fun processVisibilityState_arrival_camera_following() {
        val result = RouteOverviewButtonProcessor.ProcessVisibilityState(
            NavigationState.Arrival,
            NavigationCameraState.FOLLOWING
        ).process()

        assertTrue(result.isVisible)
    }

    @Test
    fun processVisibilityState_arrival_camera_transitionToOverview() {
        val result = RouteOverviewButtonProcessor.ProcessVisibilityState(
            NavigationState.Arrival,
            NavigationCameraState.TRANSITION_TO_OVERVIEW
        ).process()

        assertFalse(result.isVisible)
    }

    @Test
    fun processVisibilityState_arrival_camera_overview() {
        val result = RouteOverviewButtonProcessor.ProcessVisibilityState(
            NavigationState.Arrival,
            NavigationCameraState.OVERVIEW
        ).process()

        assertFalse(result.isVisible)
    }
}
