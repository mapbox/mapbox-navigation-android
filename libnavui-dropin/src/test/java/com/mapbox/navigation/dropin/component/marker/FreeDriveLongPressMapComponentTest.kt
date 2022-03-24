package com.mapbox.navigation.dropin.component.marker

import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.gestures.GesturesPlugin
import com.mapbox.maps.plugin.gestures.OnMapLongClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.dropin.component.destination.DestinationAction
import com.mapbox.navigation.dropin.component.destination.DestinationViewModel
import com.mapbox.navigation.dropin.component.navigation.NavigationState
import com.mapbox.navigation.dropin.component.navigation.NavigationStateAction
import com.mapbox.navigation.dropin.component.navigation.NavigationStateViewModel
import com.mapbox.navigation.dropin.component.routefetch.RoutesAction
import com.mapbox.navigation.dropin.component.routefetch.RoutesViewModel
import com.mapbox.navigation.dropin.model.Destination
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class FreeDriveLongPressMapComponentTest {

    private val mockNavigationStateViewModel: NavigationStateViewModel = mockk(relaxed = true)
    private val mockRoutesViewModel: RoutesViewModel = mockk(relaxed = true)
    private val mockDestinationViewModel: DestinationViewModel = mockk(relaxed = true)
    private val mockGesturesPlugin: GesturesPlugin = mockk(relaxed = true)
    private val mockMapView: MapView = mockk {
        every { gestures } returns mockGesturesPlugin
    }

    val sut = FreeDriveLongPressMapComponent(
        mockMapView,
        mockNavigationStateViewModel,
        mockRoutesViewModel,
        mockDestinationViewModel,
    )

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
    fun `onMapLongClick should update view model state`() {
        val slot = slot<OnMapLongClickListener>()
        every { mockGesturesPlugin.addOnMapLongClickListener(capture(slot)) } returns Unit
        sut.onAttached(mockk())

        val point = Point.fromLngLat(11.0, 12.0)
        slot.captured.onMapLongClick(point)

        verifyOrder {
            mockDestinationViewModel.invoke(DestinationAction.SetDestination(Destination(point)))
            mockRoutesViewModel.invoke(RoutesAction.SetRoutes(emptyList()))
            mockNavigationStateViewModel.invoke(
                NavigationStateAction.Update(NavigationState.DestinationPreview)
            )
        }
    }
}
