package com.mapbox.navigation.ui.androidauto.deeplink

import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.api.geocoding.v5.models.GeocodingResponse
import com.mapbox.common.location.Location
import com.mapbox.geojson.Point
import com.mapbox.navigation.core.geodeeplink.GeoDeeplink
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.androidauto.location.CarLocationProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class GeoDeeplinkPlacesListOnMapProviderTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Test
    fun cancel() {
        val geoDeeplinkGeocoding = mockk<GeoDeeplinkGeocoding>(relaxed = true)
        val geoDeeplink = mockk<GeoDeeplink>()
        GeoDeeplinkPlacesListOnMapProvider(geoDeeplinkGeocoding, geoDeeplink)
            .cancel()

        verify { geoDeeplinkGeocoding.cancel() }
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun getPlaces() = coroutineRule.runBlockingTest {
        mockkObject(CarLocationProvider)
        val location = mockk<Location> {
            every { longitude } returns -121.8544717
            every { latitude } returns 45.6824467
        }
        val point = Point.fromLngLat(-121.8544717, 45.6824467)
        val carmenFeature1 = mockk<CarmenFeature> {
            every { id() } returns "1"
            every { text() } returns ""
            every { center() } returns point
            every { placeName() } returns "enchanted forrest"
            every { placeType() } returns emptyList()
        }
        val carmenFeature2 = mockk<CarmenFeature> {
            every { id() } returns "1"
            every { text() } returns ""
            every { center() } returns point
            every { placeName() } returns "enchanted forrest"
            every { placeType() } returns emptyList()
        }
        val response = mockk<GeocodingResponse> {
            every { features() } returns listOf(carmenFeature1, carmenFeature2)
        }
        val carLocationProvider = mockk<CarLocationProvider> {
            coEvery { validLocation() } returns location
        }
        every { CarLocationProvider.getRegisteredInstance() } returns carLocationProvider
        val originSlot = slot<Point>()
        val geoDeeplink = mockk<GeoDeeplink>()
        val geoDeeplinkGeocoding = mockk<GeoDeeplinkGeocoding>(relaxed = true) {
            coEvery { requestPlaces(geoDeeplink, capture(originSlot)) } returns response
        }

        val result =
            GeoDeeplinkPlacesListOnMapProvider(geoDeeplinkGeocoding, geoDeeplink)
                .getPlaces()
                .value!!

        assertEquals(45.6824467, originSlot.captured.latitude(), 0.0001)
        assertEquals(-121.8544717, originSlot.captured.longitude(), 0.0001)
        assertEquals(2, result.size)
    }
}
