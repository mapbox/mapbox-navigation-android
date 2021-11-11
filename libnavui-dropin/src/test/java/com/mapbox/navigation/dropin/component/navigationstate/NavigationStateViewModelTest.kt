package com.mapbox.navigation.dropin.component.navigationstate

import com.mapbox.navigation.testing.MainCoroutineRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NavigationStateViewModelTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()
    private val viewModel = NavigationStateViewModel()

    @Test
    fun navigationStateUpdatedToEmpty() = coroutineRule.runBlockingTest {
        val action = NavigationStateAction.ToEmpty

        val results = mutableListOf<NavigationState>()
        val job = launch {
            viewModel.navigationState().toList(results)
        }
        val expected = listOf(NavigationState.Empty)
        viewModel.processAction(flowOf(action))

        assertEquals(expected, results)
        job.cancel()
    }

    @Test
    fun navigationStateUpdatedToFreeDrive() = coroutineRule.runBlockingTest {
        val action = NavigationStateAction.ToFreeDrive

        val results = mutableListOf<NavigationState>()
        val job = launch {
            viewModel.navigationState().toList(results)
        }
        val expected = listOf(NavigationState.Empty, NavigationState.FreeDrive)
        viewModel.processAction(flowOf(action))

        assertEquals(expected, results)
        job.cancel()
    }

    @Test
    fun navigationStateUpdatedToRoutePreview() = coroutineRule.runBlockingTest {
        val action = NavigationStateAction.ToRoutePreview

        val results = mutableListOf<NavigationState>()
        val job = launch {
            viewModel.navigationState().toList(results)
        }
        val expected = listOf(NavigationState.Empty, NavigationState.RoutePreview)
        viewModel.processAction(flowOf(action))

        assertEquals(expected, results)
        job.cancel()
    }

    @Test
    fun navigationStateUpdatedToActiveNavigation() = coroutineRule.runBlockingTest {
        val action = NavigationStateAction.ToActiveNavigation

        val results = mutableListOf<NavigationState>()
        val job = launch {
            viewModel.navigationState().toList(results)
        }
        val expected = listOf(NavigationState.Empty, NavigationState.ActiveNavigation)
        viewModel.processAction(flowOf(action))

        assertEquals(expected, results)
        job.cancel()
    }

    @Test
    fun navigationStateUpdatedToArrival() = coroutineRule.runBlockingTest {
        val action = NavigationStateAction.ToArrival

        val results = mutableListOf<NavigationState>()
        val job = launch {
            viewModel.navigationState().toList(results)
        }
        val expected = listOf(NavigationState.Empty, NavigationState.Arrival)
        viewModel.processAction(flowOf(action))

        assertEquals(expected, results)
        job.cancel()
    }
}
