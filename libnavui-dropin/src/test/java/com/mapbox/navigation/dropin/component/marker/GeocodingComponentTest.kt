package com.mapbox.navigation.dropin.component.marker

import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.component.destination.Destination
import com.mapbox.navigation.dropin.component.destination.DestinationAction.DidReverseGeocode
import com.mapbox.navigation.dropin.model.State
import com.mapbox.navigation.dropin.util.Geocoder
import com.mapbox.navigation.dropin.util.TestStore
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
internal class GeocodingComponentTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    lateinit var sut: GeocodingComponent

    @MockK
    lateinit var mockGeocoder: Geocoder

    @MockK
    lateinit var mockNavigation: MapboxNavigation

    private lateinit var testStore: TestStore

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        mockkObject(Geocoder)
        every { Geocoder.create(any()) } returns mockGeocoder
        testStore = spyk(TestStore())
        every { mockNavigation.navigationOptions } returns mockk {
            every { accessToken } returns "ACCESS_TOKEN"
        }

        sut = GeocodingComponent(testStore)
    }

    @After
    fun tearDown() {
        unmockkObject(Geocoder)
    }

    @Test
    fun `should reverse geocode new Destination`() = coroutineRule.runBlockingTest {
        val newDestination = Destination(Point.fromLngLat(11.0, 22.0))
        val features: List<CarmenFeature> = listOf(mockk())
        coEvery {
            mockGeocoder.findAddresses(newDestination.point)
        } returns Result.success(features)

        sut.onAttached(mockNavigation)
        testStore.setState(State(destination = newDestination))

        verify {
            testStore.dispatch(DidReverseGeocode(newDestination.point, features))
        }
    }

    @Test
    fun `should NOT reverse geocode Destinations with already set features list`() {
        coEvery { mockGeocoder.findAddresses(any()) } returns Result.success(listOf(mockk()))
        val destination = Destination(
            point = Point.fromLngLat(22.0, 33.0),
            features = listOf(mockk())
        )
        testStore.setState(State(destination = destination))

        sut.onAttached(mockNavigation)

        coVerify(exactly = 0) { mockGeocoder.findAddresses(destination.point) }
    }
}
