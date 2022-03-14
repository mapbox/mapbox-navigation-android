package com.mapbox.navigation.dropin.component.marker

import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.gestures.GesturesPlugin
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.dropin.DropInNavigationViewContext
import com.mapbox.navigation.dropin.component.destination.DestinationAction
import com.mapbox.navigation.dropin.model.Destination
import com.mapbox.navigation.dropin.testutil.DispatchRegistry
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class LongPressMapComponentTest {

    private lateinit var sut: LongPressMapComponent
    private lateinit var dispatchRegistry: DispatchRegistry

    @MockK
    lateinit var mockNavContext: DropInNavigationViewContext

    @MockK
    lateinit var mockMapView: MapView

    @MockK
    lateinit var mockGesturesPlugin: GesturesPlugin

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        dispatchRegistry = DispatchRegistry()
        every { mockNavContext.dispatch } returns { dispatchRegistry(it) }
        every { mockMapView.gestures } returns mockGesturesPlugin

        sut = LongPressMapComponent(mockMapView, mockNavContext)
    }

    @Test
    fun `should register OnMapLongClickListener in onAttached`() {
        sut.onAttached(mockk())

        verify { mockGesturesPlugin.addOnMapLongClickListener(any()) }
    }

    @Test
    fun `should unregister OnMapLongClickListener in onDetached`() {
        sut.onAttached(mockk())

        sut.onDetached(mockk())

        verify { mockGesturesPlugin.removeOnMapLongClickListener(any()) }
    }

    @Test
    fun `on map long click should dispatch SetDestination action`() {
        val slot = slot<OnMapLongClickListener>()
        every { mockGesturesPlugin.addOnMapLongClickListener(capture(slot)) } returns Unit
        val point = Point.fromLngLat(11.0, 12.0)
        sut.onAttached(mockk())

        slot.captured.onMapLongClick(point)

        dispatchRegistry.verifyDispatched(
            DestinationAction.SetDestination(Destination(point))
        )
    }
}
