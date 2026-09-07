package com.mapbox.navigation.ui.androidauto.navigation

import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.arrival.ArrivalObserver
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreenManager
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CarArrivalTriggerTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private val sut = CarArrivalTrigger()

    @Before
    fun setup() {
        mockkObject(MapboxScreenManager)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should go to ArrivalState when onFinalDestinationArrival is called`() {
        val observerSlot = slot<ArrivalObserver>()
        val mapboxNavigation: MapboxNavigation = mockk(relaxed = true) {
            every { registerArrivalObserver(capture(observerSlot)) } just runs
        }

        sut.onAttached(mapboxNavigation)
        observerSlot.captured.onFinalDestinationArrival(mockk())

        verify { MapboxScreenManager.replaceTop(MapboxScreen.ARRIVAL) }
    }

    @Test
    fun `should unregisterArrivalObserver when MapboxNavigation is detached`() {
        val mapboxNavigation: MapboxNavigation = mockk(relaxed = true)

        sut.onAttached(mapboxNavigation)
        sut.onDetached(mapboxNavigation)

        verify { mapboxNavigation.unregisterArrivalObserver(any()) }
    }
}
