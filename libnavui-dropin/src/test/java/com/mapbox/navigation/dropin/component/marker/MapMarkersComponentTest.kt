package com.mapbox.navigation.dropin.component.marker

import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.dropin.NavigationViewContext
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.component.destination.Destination
import com.mapbox.navigation.dropin.component.destination.DestinationState
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
internal class MapMarkersComponentTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private lateinit var sut: MapMarkersComponent
    private lateinit var destinationStateFlow: MutableStateFlow<DestinationState>

    @MockK
    lateinit var mockAnnotationFactory: MapMarkerFactory

    @MockK
    lateinit var mockAnnotationManager: PointAnnotationManager

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        destinationStateFlow = MutableStateFlow(DestinationState())
        val mapView = mockk<MapView> {
            every { annotations } returns mockk {
                every { createPointAnnotationManager() } returns mockAnnotationManager
            }
        }
        val navContext = mockk<NavigationViewContext> {
            every { destinationState } returns destinationStateFlow
            every { mapAnnotationFactory() } returns mockAnnotationFactory
        }

        sut = MapMarkersComponent(mapView, R.drawable.mapbox_ic_destination_marker, navContext)
    }

    @Test
    fun `should re-create point annotation on destination change`() =
        coroutineRule.runBlockingTest {
            val annotation = mockk<PointAnnotationOptions>()
            val point = Point.fromLngLat(10.0, 11.0)
            destinationStateFlow.value = DestinationState(Destination(point))
            every {
                mockAnnotationFactory.createPin(point, R.drawable.mapbox_ic_destination_marker)
            } returns annotation

            sut.onAttached(mockk())

            verifyOrder {
                mockAnnotationManager.deleteAll()
                mockAnnotationManager.create(annotation)
            }
        }

    @Test
    fun `onDetached should delete all annotations`() =
        coroutineRule.runBlockingTest {
            sut.onAttached(mockk())

            sut.onDetached(mockk())

            verify { mockAnnotationManager.deleteAll() }
        }
}
