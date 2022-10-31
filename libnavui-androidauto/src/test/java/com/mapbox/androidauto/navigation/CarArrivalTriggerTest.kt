package com.mapbox.androidauto.navigation

import com.mapbox.androidauto.screenmanager.MapboxScreen
import com.mapbox.androidauto.screenmanager.MapboxScreenManager
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.arrival.ArrivalObserver
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CarArrivalTriggerTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

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
