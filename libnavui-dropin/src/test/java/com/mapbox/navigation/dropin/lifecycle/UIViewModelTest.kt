package com.mapbox.navigation.dropin.lifecycle

import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UIViewModelTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @Test
    fun `default value is equal to state`() = runBlockingTest {
        val viewModel = AdderViewModel(10)

        assertEquals(10, viewModel.state.value)
    }

    @Test
    fun `state will change change when view model is attached`() = runBlockingTest {
        val viewModel = AdderViewModel(10)

        viewModel.onAttached(mockk())
        viewModel.invoke(3)
        viewModel.onDetached(mockk())

        assertEquals(13, viewModel.state.value)
    }

    @Test
    fun `state does not change when view model is detached`() = runBlockingTest {
        val viewModel = AdderViewModel(10)

        val mapboxNavigation = mockk<MapboxNavigation>()
        viewModel.invoke(1)
        viewModel.onAttached(mapboxNavigation)
        viewModel.invoke(3)
        viewModel.onDetached(mapboxNavigation)
        viewModel.invoke(5)

        assertEquals(13, viewModel.state.value)
    }

    @Test
    fun `state will survive mapboxNavigation changes`() = runBlockingTest {
        val viewModel = AdderViewModel(10)

        val mapboxNavigationFirst = mockk<MapboxNavigation>()
        viewModel.onAttached(mapboxNavigationFirst)
        viewModel.invoke(3)
        viewModel.onDetached(mapboxNavigationFirst)
        val mapboxNavigationSecond = mockk<MapboxNavigation>()
        viewModel.onAttached(mapboxNavigationSecond)
        viewModel.invoke(4)
        viewModel.onDetached(mapboxNavigationSecond)

        assertEquals(17, viewModel.state.value)
    }
}

private class AdderViewModel(default: Int) : UIViewModel<Int, Int>(default) {
    override fun process(mapboxNavigation: MapboxNavigation, state: Int, action: Int): Int {
        return state + action
    }
}
