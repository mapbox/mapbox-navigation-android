package com.mapbox.navigation.dropin.component.recenter

import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecenterButtonProcessorTest {

    @Test
    fun processVisibilityState_empty() {
        val result = RecenterButtonProcessor.ProcessVisibilityState(
            NavigationState.Empty,
            NavigationCameraState.IDLE
        ).process()

        assertFalse(result.isVisible)
    }

    @Test
    fun processVisibilityState_freeDrive() {
        val result = RecenterButtonProcessor.ProcessVisibilityState(
            NavigationState.FreeDrive,
            NavigationCameraState.IDLE
        ).process()

        assertTrue(result.isVisible)
    }

    @Test
    fun processVisibilityState_routePreview_camera_idle() {
        val result = RecenterButtonProcessor.ProcessVisibilityState(
            NavigationState.RoutePreview,
            NavigationCameraState.IDLE
        ).process()

        assertTrue(result.isVisible)
    }

    @Test
    fun processVisibilityState_routePreview_camera_transitionToFollowing() {
        val result = RecenterButtonProcessor.ProcessVisibilityState(
            NavigationState.RoutePreview,
            NavigationCameraState.TRANSITION_TO_FOLLOWING
        ).process()

        assertFalse(result.isVisible)
    }

    @Test
    fun processVisibilityState_routePreview_camera_following() {
        val result = RecenterButtonProcessor.ProcessVisibilityState(
            NavigationState.RoutePreview,
            NavigationCameraState.FOLLOWING
        ).process()

        assertFalse(result.isVisible)
    }

    @Test
    fun processVisibilityState_routePreview_camera_transitionToOverview() {
        val result = RecenterButtonProcessor.ProcessVisibilityState(
            NavigationState.RoutePreview,
            NavigationCameraState.TRANSITION_TO_OVERVIEW
        ).process()

        assertTrue(result.isVisible)
    }

    @Test
    fun processVisibilityState_routePreview_camera_overview() {
        val result = RecenterButtonProcessor.ProcessVisibilityState(
            NavigationState.RoutePreview,
            NavigationCameraState.OVERVIEW
        ).process()

        assertTrue(result.isVisible)
    }

    @Test
    fun processVisibilityState_activeNavigation_camera_idle() {
        val result = RecenterButtonProcessor.ProcessVisibilityState(
            NavigationState.ActiveNavigation,
            NavigationCameraState.IDLE
        ).process()

        assertTrue(result.isVisible)
    }

    @Test
    fun processVisibilityState_activeNavigation_camera_transitionToFollowing() {
        val result = RecenterButtonProcessor.ProcessVisibilityState(
            NavigationState.ActiveNavigation,
            NavigationCameraState.TRANSITION_TO_FOLLOWING
        ).process()

        assertFalse(result.isVisible)
    }

    @Test
    fun processVisibilityState_activeNavigation_camera_following() {
        val result = RecenterButtonProcessor.ProcessVisibilityState(
            NavigationState.ActiveNavigation,
            NavigationCameraState.FOLLOWING
        ).process()

        assertFalse(result.isVisible)
    }

    @Test
    fun processVisibilityState_activeNavigation_camera_transitionToOverview() {
        val result = RecenterButtonProcessor.ProcessVisibilityState(
            NavigationState.ActiveNavigation,
            NavigationCameraState.TRANSITION_TO_OVERVIEW
        ).process()

        assertTrue(result.isVisible)
    }

    @Test
    fun processVisibilityState_activeNavigation_camera_overview() {
        val result = RecenterButtonProcessor.ProcessVisibilityState(
            NavigationState.ActiveNavigation,
            NavigationCameraState.OVERVIEW
        ).process()

        assertTrue(result.isVisible)
    }

    @Test
    fun processVisibilityState_arrival_camera_idle() {
        val result = RecenterButtonProcessor.ProcessVisibilityState(
            NavigationState.Arrival,
            NavigationCameraState.IDLE
        ).process()

        assertTrue(result.isVisible)
    }

    @Test
    fun processVisibilityState_arrival_camera_transitionToFollowing() {
        val result = RecenterButtonProcessor.ProcessVisibilityState(
            NavigationState.Arrival,
            NavigationCameraState.TRANSITION_TO_FOLLOWING
        ).process()

        assertFalse(result.isVisible)
    }

    @Test
    fun processVisibilityState_arrival_camera_following() {
        val result = RecenterButtonProcessor.ProcessVisibilityState(
            NavigationState.Arrival,
            NavigationCameraState.FOLLOWING
        ).process()

        assertFalse(result.isVisible)
    }

    @Test
    fun processVisibilityState_arrival_camera_transitionToOverview() {
        val result = RecenterButtonProcessor.ProcessVisibilityState(
            NavigationState.Arrival,
            NavigationCameraState.TRANSITION_TO_OVERVIEW
        ).process()

        assertTrue(result.isVisible)
    }

    @Test
    fun processVisibilityState_arrival_camera_overview() {
        val result = RecenterButtonProcessor.ProcessVisibilityState(
            NavigationState.Arrival,
            NavigationCameraState.OVERVIEW
        ).process()

        assertTrue(result.isVisible)
    }
}
