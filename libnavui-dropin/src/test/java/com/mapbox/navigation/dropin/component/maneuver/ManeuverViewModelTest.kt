package com.mapbox.navigation.dropin.component.maneuver

import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.ui.maneuver.model.Maneuver
import io.mockk.every
import io.mockk.mockk
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
class ManeuverViewModelTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @Test
    fun consumeAction_ProcessNavigationState() = coroutineRule.runBlockingTest {
        val maneuverApi = mockk<MapboxManeuverApi>()
        val state = ManeuverAction.UpdateNavigationState(NavigationState.ActiveNavigation)
        val viewModel = ManeuverViewModel(maneuverApi)
        val def = async {
            viewModel.maneuverState().drop(1).first()
        }

        viewModel.consumeAction(flowOf(state))
        val viewModelResult = def.await()

        assertFalse(viewModelResult.isVisible)
        assertEquals(NavigationState.ActiveNavigation, viewModelResult.navigationState)
        assertTrue(viewModelResult.maneuver.isError)
    }

    @Test
    fun consumeAction_UpdateRouteProgress() = coroutineRule.runBlockingTest {
        val maneuvers = listOf(mockk<Maneuver>())
        val routeProgress = mockk<RouteProgress>()
        val maneuverApi = mockk<MapboxManeuverApi> {
            every { getManeuvers(routeProgress) } returns ExpectedFactory.createValue(maneuvers)
        }
        val state = ManeuverAction.UpdateRouteProgress(routeProgress)
        val viewModel = ManeuverViewModel(maneuverApi)
        val def = async {
            viewModel.maneuverState().drop(1).first()
        }

        viewModel.consumeAction(flowOf(state))
        val viewModelResult = def.await()

        assertFalse(viewModelResult.isVisible)
        assertEquals(NavigationState.Empty, viewModelResult.navigationState)
        assertEquals(maneuvers, viewModelResult.maneuver.value)
    }
}
