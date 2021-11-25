package com.mapbox.navigation.dropin.component.tripprogress

import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.tripprogress.api.MapboxTripProgressApi
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TripProgressViewModelTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @Test
    fun consumeAction_UpdateNavigationState() = coroutineRule.runBlockingTest {
        val state = TripProgressAction.UpdateNavigationState(NavigationState.ActiveNavigation)
        val tripProgressApi = mockk<MapboxTripProgressApi>()
        val viewModel = TripProgressViewModel(tripProgressApi)
        val def = async {
            viewModel.state.drop(1).first()
        }

        viewModel.consumeAction(flowOf(state))
        val viewModelResult = def.await()

        assertTrue(viewModelResult.isVisible)
        assertEquals(NavigationState.ActiveNavigation, viewModelResult.navigationState)
    }
}
