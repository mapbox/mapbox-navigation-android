package com.mapbox.androidauto.car.navigation

import com.mapbox.androidauto.ArrivalState
import com.mapbox.androidauto.MapboxCarApp
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.arrival.ArrivalObserver
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
import org.junit.Test

class CarArrivalTriggerTest {

    private val sut = CarArrivalTrigger()

    @Before
    fun setup() {
        mockkObject(MapboxCarApp)
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

        verify { MapboxCarApp.updateCarAppState(ArrivalState) }
    }

    @Test
    fun `should unregisterArrivalObserver when MapboxNavigation is detached`() {
        val mapboxNavigation: MapboxNavigation = mockk(relaxed = true)

        sut.onAttached(mapboxNavigation)
        sut.onDetached(mapboxNavigation)

        verify { mapboxNavigation.unregisterArrivalObserver(any()) }
    }
}
