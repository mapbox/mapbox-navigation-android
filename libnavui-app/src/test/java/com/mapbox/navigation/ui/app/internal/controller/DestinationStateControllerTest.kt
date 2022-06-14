package com.mapbox.navigation.ui.app.internal.controller

import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.app.internal.State
import com.mapbox.navigation.ui.app.internal.destination.Destination
import com.mapbox.navigation.ui.app.internal.destination.DestinationAction
import com.mapbox.navigation.ui.app.testing.TestStore
import io.mockk.spyk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class DestinationStateControllerTest {

    private lateinit var testStore: TestStore
    private lateinit var sut: DestinationStateController

    @Before
    fun setUp() {
        testStore = spyk(TestStore())
        sut = DestinationStateController(testStore)
    }

    @Test
    fun `on SetDestination action should save destination in the store`() {
        val destination = Destination(Point.fromLngLat(10.0, 20.0))

        testStore.dispatch(DestinationAction.SetDestination(destination))

        assertEquals(destination, testStore.state.value.destination)
    }

    @Test
    fun `on DidReverseGeocode action should save CarmenFeature in the store`() {
        val point = Point.fromLngLat(10.0, 20.0)
        val features = listOf(CarmenFeature.builder().address("1234 some address").build())
        testStore.setState(State(Destination(point)))

        testStore.dispatch(DestinationAction.DidReverseGeocode(point, features))

        assertEquals(features, testStore.state.value.destination?.features)
    }

    @Test
    fun `on DidReverseGeocode action should NOT save CarmenFeature for wrong coordinates`() {
        val point1 = Point.fromLngLat(10.0, 20.0)
        val point2 = Point.fromLngLat(11.0, 21.0)
        val features = listOf(CarmenFeature.builder().address("1234 some address").build())
        testStore.setState(State(Destination(point1)))

        testStore.dispatch(DestinationAction.DidReverseGeocode(point2, features))

        assertNull(testStore.state.value.destination?.features)
    }
}
