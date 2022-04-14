package com.mapbox.navigation.dropin.component.navigation

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.arrival.ArrivalObserver
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
internal class NavigationStateViewModelTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    lateinit var sut: NavigationStateViewModel
    lateinit var mockMapboxNavigation: MapboxNavigation

    @Before
    fun setUp() {
        mockkObject(MapboxNavigationApp)
        mockMapboxNavigation = mockk()
        every { MapboxNavigationApp.current() } returns mockMapboxNavigation

        sut = NavigationStateViewModel(NavigationState.FreeDrive)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should set new state on Update action`() = coroutineRule.runBlockingTest {
        sut.onAttached(mockMapboxNavigation)

        sut.invoke(NavigationStateAction.Update(NavigationState.RoutePreview))

        assertEquals(NavigationState.RoutePreview, sut.state.value)
    }

    @Test
    fun `should detect final destination arrival and update NavigationState to Arrival`() {
        val observerSlot = slot<ArrivalObserver>()
        every { mockMapboxNavigation.registerArrivalObserver(capture(observerSlot)) } returns Unit
        sut.onAttached(mockMapboxNavigation)

        observerSlot.captured.onFinalDestinationArrival(mockk())

        assertEquals(NavigationState.Arrival, sut.state.value)
    }
}
