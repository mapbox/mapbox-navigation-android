@file:Suppress("NoMockkVerifyImport")

package com.mapbox.androidauto.deeplink

import android.location.Location
import androidx.car.app.CarContext
import com.mapbox.androidauto.MapboxCarApp
import com.mapbox.androidauto.navigation.location.CarAppLocation
import com.mapbox.androidauto.testing.MainCoroutineRule
import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.api.geocoding.v5.models.GeocodingResponse
import com.mapbox.geojson.Point
import com.mapbox.navigation.core.geodeeplink.GeoDeeplink
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class GeoDeeplinkPlacesListOnMapProviderTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Test
    fun cancel() {
        val carContext = mockk<CarContext> {
            every { getString(any()) } returns "test_string"
        }
        val geoDeeplinkGeocoding = mockk<GeoDeeplinkGeocoding>(relaxed = true)
        val geoDeeplink = mockk<GeoDeeplink>()
        GeoDeeplinkPlacesListOnMapProvider(carContext, geoDeeplinkGeocoding, geoDeeplink).cancel()

        verify { geoDeeplinkGeocoding.cancel() }
    }

    @Test
    fun getPlaces() = coroutineRule.runTest {
        mockkObject(MapboxCarApp)
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
        val carAppLocation = mockk<CarAppLocation> {
            coEvery { validLocation() } returns location
        }
        every { MapboxCarApp.carAppLocationService() } returns carAppLocation
        val originSlot = slot<Point>()
        val geoDeeplink = mockk<GeoDeeplink>()
        val geoDeeplinkGeocoding = mockk<GeoDeeplinkGeocoding>(relaxed = true) {
            coEvery { requestPlaces(geoDeeplink, capture(originSlot)) } returns response
        }

        val carContext = mockk<CarContext> {
            every { getString(any()) } returns "test_string"
        }
        val result = GeoDeeplinkPlacesListOnMapProvider(carContext, geoDeeplinkGeocoding, geoDeeplink)
            .getPlaces()
            .value!!

        assertEquals(45.6824467, originSlot.captured.latitude(), 0.0001)
        assertEquals(-121.8544717, originSlot.captured.longitude(), 0.0001)
        assertEquals(2, result.size)
        unmockkObject(MapboxCarApp)
    }
}
