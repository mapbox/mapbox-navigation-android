package com.mapbox.navigation.dropin.component.marker

import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.dropin.component.destination.DestinationAction.DidReverseGeocode
import com.mapbox.navigation.dropin.component.destination.DestinationState
import com.mapbox.navigation.dropin.component.destination.DestinationViewModel
import com.mapbox.navigation.dropin.model.Destination
import com.mapbox.navigation.dropin.util.Geocoder
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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

    @MockK
    lateinit var mockDestinationViewModel: DestinationViewModel
    private lateinit var destinationStateFlow: MutableStateFlow<DestinationState>

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        mockkObject(Geocoder)
        every { Geocoder.create(any()) } returns mockGeocoder
        every { mockNavigation.navigationOptions } returns mockk {
            every { accessToken } returns "ACCESS_TOKEN"
        }
        destinationStateFlow = MutableStateFlow(DestinationState())
        every { mockDestinationViewModel.state } returns destinationStateFlow

        sut = GeocodingComponent(mockDestinationViewModel)
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
        destinationStateFlow.tryEmit(DestinationState(newDestination))

        verify {
            mockDestinationViewModel.invoke(DidReverseGeocode(newDestination.point, features))
        }
    }

    @Test
    fun `should NOT reverse geocode Destinations with already set features list`() {
        coEvery { mockGeocoder.findAddresses(any()) } returns Result.success(listOf(mockk()))
        val destination = Destination(
            point = Point.fromLngLat(22.0, 33.0),
            features = listOf(mockk())
        )
        destinationStateFlow.tryEmit(DestinationState(destination))

        sut.onAttached(mockNavigation)

        coVerify(exactly = 0) { mockGeocoder.findAddresses(destination.point) }
    }
}
