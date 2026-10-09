package com.mapbox.navigation.ui.androidauto.location

import com.mapbox.common.location.Location
import com.mapbox.geojson.Point
import com.mapbox.maps.plugin.locationcomponent.LocationConsumer
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.ui.androidauto.testing.CarAppTestRule
import com.mapbox.navigation.ui.androidauto.testing.MapboxRobolectricTestRunner
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test

class CarLocationProviderTest : MapboxRobolectricTestRunner() {

    @get:Rule
    val carAppTestRule = CarAppTestRule()

    private val locationObserver = slot<LocationObserver>()
    private val mapboxNavigation = mockk<MapboxNavigation>(relaxUnitFun = true) {
        every { registerLocationObserver(capture(locationObserver)) } just Runs
    }

    @Test
    fun `location of a finished drive is cleared on detach`() = runBlocking {
        every { MapboxNavigationApp.getObservers(CarLocationProvider::class) } returns emptyList()
        val sut = CarLocationProvider.getRegisteredInstance()
        carAppTestRule.onAttached(mapboxNavigation)
        val location = mockk<Location>(relaxed = true)
        locationObserver.captured.onNewLocationMatcherResult(
            mockk(relaxed = true) {
                every { enhancedLocation } returns location
                every { keyPoints } returns emptyList()
            },
        )
        assertSame(location, sut.lastLocation())
        assertSame(location, sut.waitForLocationOrNull(timeoutMillis = 10))

        carAppTestRule.onDetached(mapboxNavigation)

        assertNull(sut.lastLocation())
        assertNull(sut.waitForLocationOrNull(timeoutMillis = 10))
    }

    @Test
    fun `location of a finished drive is not replayed to a new location consumer`() {
        every { MapboxNavigationApp.getObservers(CarLocationProvider::class) } returns emptyList()
        val sut = CarLocationProvider.getRegisteredInstance()
        val existingConsumer = locationConsumer()
        sut.registerLocationConsumer(existingConsumer.consumer)
        carAppTestRule.onAttached(mapboxNavigation)
        sendLocation()
        assertEquals(1, existingConsumer.updates)

        carAppTestRule.onDetached(mapboxNavigation)
        val newConsumer = locationConsumer()
        sut.registerLocationConsumer(newConsumer.consumer)
        assertEquals(0, newConsumer.updates)

        carAppTestRule.onAttached(mapboxNavigation)
        sendLocation()
        assertEquals(2, existingConsumer.updates)
        assertEquals(1, newConsumer.updates)
    }

    private fun sendLocation() {
        locationObserver.captured.onNewLocationMatcherResult(
            mockk(relaxed = true) {
                every { enhancedLocation } returns mockk(relaxed = true)
                every { keyPoints } returns emptyList()
            },
        )
    }

    private class CountingConsumer(val consumer: LocationConsumer) {
        var updates = 0
    }

    private fun locationConsumer(): CountingConsumer {
        val consumer = mockk<LocationConsumer>(relaxed = true)
        val counting = CountingConsumer(consumer)
        every { consumer.onLocationUpdated(*anyVararg<Point>(), options = any()) } answers {
            counting.updates++
        }
        return counting
    }
}
