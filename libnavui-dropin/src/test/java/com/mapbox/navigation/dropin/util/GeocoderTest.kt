package com.mapbox.navigation.dropin.util

import com.mapbox.api.geocoding.v5.GeocodingCriteria
import com.mapbox.api.geocoding.v5.MapboxGeocoding
import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.api.geocoding.v5.models.GeocodingResponse
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Callback
import retrofit2.Response

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class, ExperimentalCoroutinesApi::class)
internal class GeocoderTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private lateinit var sut: Geocoder

    @MockK
    private lateinit var mockMapboxGeocoding: MapboxGeocoding

    private lateinit var spyMapboxGeocodingBuilder: MapboxGeocoding.Builder

    @Before
    fun setUp() {
        mockkStatic(MapboxGeocoding::class)
        MockKAnnotations.init(this, relaxUnitFun = true)
        spyMapboxGeocodingBuilder = spyk(MapboxGeocoding.builder())

        every { MapboxGeocoding.builder() } returns spyMapboxGeocodingBuilder
        every { spyMapboxGeocodingBuilder.build() } returns mockMapboxGeocoding

        sut = Geocoder("ACCESS_TOKEN")
    }

    @After
    fun tearDown() {
        unmockkStatic(MapboxGeocoding::class)
    }

    @Test
    fun `reverseGeocode should correctly configure MapboxGeocoding`() =
        coroutineRule.runBlockingTest {
            val point = Point.fromLngLat(10.0, 11.0)
            givenGeocodingResponse(response(emptyList()))

            sut.reverseGeocode(point, GeocodingCriteria.TYPE_ADDRESS)

            verify { spyMapboxGeocodingBuilder.accessToken("ACCESS_TOKEN") }
            verify { spyMapboxGeocodingBuilder.query(point) }
            verify { spyMapboxGeocodingBuilder.geocodingTypes(GeocodingCriteria.TYPE_ADDRESS) }
        }

    @Test
    fun `findAddresses should return list of CarmenFeature from MapboxGeocoder response`() =
        coroutineRule.runBlockingTest {
            val point = Point.fromLngLat(10.0, 11.0)
            val features = listOf<CarmenFeature>()
            givenGeocodingResponse(response(features))

            val result = sut.findAddresses(point)

            assertEquals(features, result)
        }

    private fun givenGeocodingResponse(response: Response<GeocodingResponse>) {
        val slot = slot<Callback<GeocodingResponse>>()
        every { mockMapboxGeocoding.enqueueCall(capture(slot)) } answers {
            slot.captured.onResponse(mockk(), response)
        }
    }

    private fun response(features: List<CarmenFeature>): Response<GeocodingResponse> {
        return mockk {
            every { body() } returns mockk {
                every { features() } returns features
            }
        }
    }
}
