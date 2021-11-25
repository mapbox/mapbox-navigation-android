package com.mapbox.navigation.dropin.component.sound

import com.mapbox.navigation.dropin.component.navigationstate.NavigationState
import com.mapbox.navigation.testing.MainCoroutineRule
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
class SoundButtonViewModelTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @Test
    fun consumeAction_UpdateNavigationState() = coroutineRule.runBlockingTest {
        val state = SoundButtonAction.UpdateNavigationState(NavigationState.ActiveNavigation)
        val viewModel = SoundButtonViewModel()
        val def = async {
            viewModel.state.drop(1).first()
        }

        viewModel.consumeAction(flowOf(state))
        val viewModelResult = def.await()

        assertTrue(viewModelResult.isVisible)
        assertEquals(0f, viewModelResult.volume)
        assertTrue(viewModelResult.isMute)
        assertEquals(NavigationState.ActiveNavigation, viewModelResult.navigationState)
    }

    @Test
    fun consumeAction_UpdateVolume() = coroutineRule.runBlockingTest {
        val state = SoundButtonAction.UpdateVolume(4f)
        val viewModel = SoundButtonViewModel()
        val def = async {
            viewModel.state.drop(1).first()
        }

        viewModel.consumeAction(flowOf(state))
        val viewModelResult = def.await()

        assertFalse(viewModelResult.isVisible)
        assertEquals(4f, viewModelResult.volume)
        assertFalse(viewModelResult.isMute)
        assertEquals(NavigationState.Empty, viewModelResult.navigationState)
    }
}
