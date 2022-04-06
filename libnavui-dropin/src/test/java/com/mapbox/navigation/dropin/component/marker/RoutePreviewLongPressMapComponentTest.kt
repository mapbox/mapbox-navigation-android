package com.mapbox.navigation.dropin.component.marker

import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.gestures.GesturesPlugin
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.component.destination.Destination
import com.mapbox.navigation.dropin.component.destination.DestinationAction
import com.mapbox.navigation.dropin.component.destination.DestinationViewModel
import com.mapbox.navigation.dropin.component.location.LocationViewModel
import com.mapbox.navigation.dropin.component.routefetch.RoutesAction
import com.mapbox.navigation.dropin.component.routefetch.RoutesViewModel
import com.mapbox.navigation.dropin.util.HapticFeedback
import com.mapbox.navigation.testing.MockLoggerRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class RoutePreviewLongPressMapComponentTest {

    @get:Rule
    val mockLoggerTestRule = MockLoggerRule()

    private val mockLocationViewModel: LocationViewModel = mockk(relaxed = true)
    private val mockRoutesViewModel: RoutesViewModel = mockk(relaxed = true)
    private val mockDestinationViewModel: DestinationViewModel = mockk(relaxed = true)
    private val mockGesturesPlugin: GesturesPlugin = mockk(relaxed = true)
    private val mockMapView: MapView = mockk {
        every { gestures } returns mockGesturesPlugin
    }
    private val mockMapboxNavigation: MapboxNavigation = mockk(relaxed = true) {
        every { navigationOptions } returns mockk {
            every { applicationContext } returns mockk(relaxed = true)
        }
    }

    lateinit var sut: RoutePreviewLongPressMapComponent

    @Before
    fun setUp() {
        mockkObject(HapticFeedback)
        every { HapticFeedback.create(any()) } returns mockk(relaxed = true)

        sut = RoutePreviewLongPressMapComponent(
            mockMapView,
            mockLocationViewModel,
            mockRoutesViewModel,
            mockDestinationViewModel,
        )
    }

    @After
    fun tearDown() {
        unmockkObject(HapticFeedback)
    }

    @Test
    fun `should register OnMapLongClickListener in onAttached`() {
        sut.onAttached(mockMapboxNavigation)

        verify { mockGesturesPlugin.addOnMapLongClickListener(any()) }
    }

    @Test
    fun `should unregister OnMapLongClickListener in onDetached`() {
        sut.onAttached(mockMapboxNavigation)

        sut.onDetached(mockMapboxNavigation)

        verify { mockGesturesPlugin.removeOnMapLongClickListener(any()) }
    }

    @Test
    fun `onMapLongClick should do nothing if location is unknown`() {
        val slot = slot<OnMapLongClickListener>()
        every { mockGesturesPlugin.addOnMapLongClickListener(capture(slot)) } returns Unit
        every { mockLocationViewModel.lastPoint } returns null
        sut.onAttached(mockMapboxNavigation)

        val point = Point.fromLngLat(11.0, 12.0)
        slot.captured.onMapLongClick(point)

        verify(exactly = 0) {
            mockLocationViewModel.invoke(any())
            mockDestinationViewModel.invoke(any())
            mockRoutesViewModel.invoke(any())
        }
    }

    @Test
    fun `onMapLongClick should update view model state`() {
        val slot = slot<OnMapLongClickListener>()
        every { mockGesturesPlugin.addOnMapLongClickListener(capture(slot)) } returns Unit
        val lastPoint = Point.fromLngLat(21.0, 22.0)
        every { mockLocationViewModel.lastPoint } returns lastPoint
        sut.onAttached(mockMapboxNavigation)

        val point = Point.fromLngLat(11.0, 12.0)
        slot.captured.onMapLongClick(point)

        verifyOrder {
            mockDestinationViewModel.invoke(DestinationAction.SetDestination(Destination(point)))
            mockRoutesViewModel.invoke(RoutesAction.FetchPoints(listOf(lastPoint, point)))
        }
    }
}
