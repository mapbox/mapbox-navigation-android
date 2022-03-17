package com.mapbox.navigation.dropin.component.routeoverview

import com.mapbox.navigation.dropin.component.navigation.NavigationState
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.maps.camera.state.NavigationCameraState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RouteOverviewViewModelTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @Test
    fun consumeAction_UpdateNavigationState() = coroutineRule.runBlockingTest {
        val state =
            RouteOverviewButtonAction.UpdateNavigationState(NavigationState.ActiveNavigation)
        val viewModel = RouteOverviewViewModel()
        val def = async {
            viewModel.state.drop(1).first()
        }

        viewModel.consumeAction(flowOf(state))
        val viewModelResult = def.await()

        assertTrue(viewModelResult.isVisible)
        assertEquals(NavigationState.ActiveNavigation, viewModelResult.navigationState)
        assertEquals(NavigationCameraState.IDLE, viewModelResult.cameraState)
    }

    @Test
    fun consumeAction_UpdateCameraState() = coroutineRule.runBlockingTest {
        val state =
            RouteOverviewButtonAction.UpdateCameraState(NavigationCameraState.FOLLOWING)
        val viewModel = RouteOverviewViewModel()
        val def = async {
            viewModel.state.drop(1).first()
        }

        viewModel.consumeAction(flowOf(state))
        val viewModelResult = def.await()

        assertFalse(viewModelResult.isVisible)
        assertEquals(NavigationState.FreeDrive, viewModelResult.navigationState)
        assertEquals(NavigationCameraState.FOLLOWING, viewModelResult.cameraState)
    }
}
