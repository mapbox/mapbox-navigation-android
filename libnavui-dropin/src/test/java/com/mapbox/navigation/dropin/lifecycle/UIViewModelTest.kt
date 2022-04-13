package com.mapbox.navigation.dropin.lifecycle

import com.mapbox.common.Logger
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.testing.MockLoggerRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
@ExperimentalPreviewMapboxNavigationAPI
class UIViewModelTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @get:Rule
    val mockLoggerTestRule = MockLoggerRule()

    @Before
    fun setup() {
        mockkObject(MapboxNavigationApp)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `default value is equal to state`() = runBlockingTest {
        val viewModel = AdderViewModel(10)

        assertEquals(10, viewModel.state.value)
    }

    @Test
    fun `state will change change when view model is attached`() = runBlockingTest {
        val viewModel = AdderViewModel(10)

        val mapboxNavigation = mockMapboxNavigation()
        viewModel.onAttached(mapboxNavigation)
        viewModel.invoke(3)
        viewModel.onDetached(mapboxNavigation)

        assertEquals(13, viewModel.state.value)
    }

    @Test
    fun `state is updated when view model is detached`() = runBlockingTest {
        val viewModel = AdderViewModel(10)

        val mapboxNavigation = mockMapboxNavigation()
        viewModel.invoke(1)
        viewModel.onAttached(mapboxNavigation)
        viewModel.invoke(3)
        viewModel.onDetached(mapboxNavigation)
        viewModel.invoke(5)

        assertEquals(19, viewModel.state.value)
    }

    @Test
    fun `state will survive mapboxNavigation changes`() = runBlockingTest {
        val viewModel = AdderViewModel(10)

        val mapboxNavigationFirst = mockMapboxNavigation()
        viewModel.onAttached(mapboxNavigationFirst)
        viewModel.invoke(3)
        viewModel.onDetached(mapboxNavigationFirst)
        val mapboxNavigationSecond = mockMapboxNavigation()
        viewModel.onAttached(mapboxNavigationSecond)
        viewModel.invoke(4)
        viewModel.onDetached(mapboxNavigationSecond)

        assertEquals(17, viewModel.state.value)
    }

    @Test
    fun `invoke logs a warning when MapboxNavigationApp is not setup`() = runBlockingTest {
        val viewModel = AdderViewModel(10)
        every { MapboxNavigationApp.current() } returns null

        viewModel.invoke(4)

        assertEquals(10, viewModel.state.value)
        verify { Logger.w(any(), any()) }
    }

    @Test
    fun `Actions can be observed`() = runBlockingTest {
        val viewModel = AdderViewModel(10)
        val actionsSlot = mutableListOf<Int>()
        val mapboxNavigation = mockMapboxNavigation()
        val actions = async { viewModel.action.collect { actionsSlot.add(it) } }

        viewModel.onAttached(mapboxNavigation)
        viewModel.invoke(3)
        viewModel.invoke(4)
        viewModel.onDetached(mapboxNavigation)
        actions.cancelAndJoin()

        assertTrue(actionsSlot.containsAll(listOf(3, 4)))
    }

    private fun mockMapboxNavigation(): MapboxNavigation {
        val mapboxNavigation = mockk<MapboxNavigation>()
        every { MapboxNavigationApp.current() } returns mapboxNavigation
        return mapboxNavigation
    }
}

@ExperimentalPreviewMapboxNavigationAPI
private class AdderViewModel(default: Int) : UIViewModel<Int, Int>(default) {
    override fun process(mapboxNavigation: MapboxNavigation, state: Int, action: Int): Int {
        return state + action
    }
}
