package com.mapbox.navigation.dropin.component.map

import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.extension.observable.eventdata.StyleLoadedEventData
import com.mapbox.maps.plugin.delegates.listeners.OnStyleLoadedListener
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapEventProducerTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @Test
    fun onStyleLoadedListenerTest() = coroutineRule.runBlockingTest {
        val listenerSlot = slot<OnStyleLoadedListener>()
        val mockStyle = mockk<Style>()
        val styleData = mockk<StyleLoadedEventData>()
        val mockMap = mockk<MapboxMap>(relaxed = true) {
            every { getStyle() } returns mockStyle
        }
        val mapView = mockk<MapView>(relaxed = true) {
            every { getMapboxMap() } returns mockMap
            every { location } returns mockk(relaxed = true)
        }
        val mapEventDelegate = MapEventProducer(mapView).also {
            it.onStart(mockk())
        }
        val def = async {
            mapEventDelegate.mapStyleUpdates.first()
        }
        verify { mockMap.addOnStyleLoadedListener(capture(listenerSlot)) }
        listenerSlot.captured.onStyleLoaded(styleData)

        val result = def.await()

        assertEquals(mockStyle, result)
    }

    @Test
    fun onIndicatorPositionChangedListenerTest() = coroutineRule.runBlockingTest {
        val expectedPoint = Point.fromLngLat(-119.27505213820666, 84.85099771823289)
        val listenerSlot = slot<OnIndicatorPositionChangedListener>()
        val mockLocation = mockk<LocationComponentPlugin>(relaxed = true)
        val mockMap = mockk<MapboxMap>(relaxed = true)
        val mapView = mockk<MapView>(relaxed = true) {
            every { getMapboxMap() } returns mockMap
            every { location } returns mockLocation
        }
        val mapEventDelegate = MapEventProducer(mapView).also {
            it.onStart(mockk())
        }
        val def = async {
            mapEventDelegate.positionChanges.first()
        }
        verify { mockLocation.addOnIndicatorPositionChangedListener(capture(listenerSlot)) }
        listenerSlot.captured.onIndicatorPositionChanged(expectedPoint)

        val result = def.await()

        assertEquals(expectedPoint, result)
    }

    @Test
    fun `onStart adds addOnStyleLoadedListener`() {
        val mockLocation = mockk<LocationComponentPlugin>(relaxed = true)
        val mockMap = mockk<MapboxMap>(relaxed = true)
        val mapView = mockk<MapView>(relaxed = true) {
            every { getMapboxMap() } returns mockMap
            every { location } returns mockLocation
        }

        MapEventProducer(mapView).also {
            it.onStart(mockk())
        }

        verify { mockMap.addOnStyleLoadedListener(any()) }
    }

    @Test
    fun `onStart adds addOnIndicatorPositionChangedListener`() {
        val mockLocation = mockk<LocationComponentPlugin>(relaxed = true)
        val mockMap = mockk<MapboxMap>(relaxed = true)
        val mapView = mockk<MapView>(relaxed = true) {
            every { getMapboxMap() } returns mockMap
            every { location } returns mockLocation
        }

        MapEventProducer(mapView).also {
            it.onStart(mockk())
        }

        verify { mockLocation.addOnIndicatorPositionChangedListener(any()) }
    }

    @Test
    fun `onStop removes removeOnStyleLoadedListener`() {
        val mockLocation = mockk<LocationComponentPlugin>(relaxed = true)
        val mockMap = mockk<MapboxMap>(relaxed = true)
        val mapView = mockk<MapView>(relaxed = true) {
            every { getMapboxMap() } returns mockMap
            every { location } returns mockLocation
        }

        MapEventProducer(mapView).also {
            it.onStop(mockk())
        }

        verify { mockMap.removeOnStyleLoadedListener(any()) }
    }

    @Test
    fun `onStop removes removeOnIndicatorPositionChangedListener`() {
        val mockLocation = mockk<LocationComponentPlugin>(relaxed = true)
        val mockMap = mockk<MapboxMap>(relaxed = true)
        val mapView = mockk<MapView>(relaxed = true) {
            every { getMapboxMap() } returns mockMap
            every { location } returns mockLocation
        }

        MapEventProducer(mapView).also {
            it.onStop(mockk())
        }

        verify { mockLocation.removeOnIndicatorPositionChangedListener(any()) }
    }
}
