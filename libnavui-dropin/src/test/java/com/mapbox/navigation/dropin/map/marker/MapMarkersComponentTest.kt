package com.mapbox.navigation.dropin.map.marker

import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.dropin.util.TestStore
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.app.internal.State
import com.mapbox.navigation.ui.app.internal.destination.Destination
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
internal class MapMarkersComponentTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private lateinit var sut: MapMarkersComponent
    private lateinit var testStore: TestStore
    private var mockAnnotationManager: PointAnnotationManager = mockk(relaxed = true)
    private var annotation: PointAnnotationOptions = mockk(relaxed = true)

    @Before
    fun setUp() {
        testStore = TestStore()
        val mapView = mockk<MapView> {
            every { context } returns mockk(relaxed = true)
            every { annotations } returns mockk {
                every { createPointAnnotationManager() } returns mockAnnotationManager
            }
        }

        sut = MapMarkersComponent(testStore, mapView, annotation)
    }

    @Test
    fun `should re-create point annotation on destination change`() =
        coroutineRule.runBlockingTest {
            val point = Point.fromLngLat(10.0, 11.0)
            testStore.setState(State(destination = Destination(point)))

            sut.onAttached(mockk())

            verifyOrder {
                mockAnnotationManager.deleteAll()
                mockAnnotationManager.create(annotation)
            }
        }

    @Test
    fun `onDetached should delete all annotations`() = coroutineRule.runBlockingTest {
        sut.onAttached(mockk())

        sut.onDetached(mockk())

        verify { mockAnnotationManager.deleteAll() }
    }
}
