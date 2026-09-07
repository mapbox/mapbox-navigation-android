package com.mapbox.navigation.ui.androidauto.deeplink

import com.mapbox.common.location.Location
import com.mapbox.geojson.Point
import com.mapbox.navigation.core.geodeeplink.GeoDeeplink
import com.mapbox.navigation.ui.androidauto.location.CarLocationProvider
import com.mapbox.search.result.SearchResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class GeoDeeplinkSearchBoxPlacesListOnMapProviderTest {

    private lateinit var carLocationProvider: CarLocationProvider

    @Before
    fun setUp() {
        carLocationProvider = mockk<CarLocationProvider>(relaxed = true)
        mockkObject(CarLocationProvider)
        every {
            CarLocationProvider.getRegisteredInstance()
        } returns carLocationProvider
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `cancel delegates to geoDeeplinkSearchBox`() {
        val geoDeeplinkSearchBox = mockk<GeoDeeplinkSearchBox>(relaxed = true)
        val geoDeeplink = mockk<GeoDeeplink>()

        @Suppress("DEPRECATION")
        GeoDeeplinkSearchBoxPlacesListOnMapProvider(geoDeeplinkSearchBox, geoDeeplink).cancel()

        verify { geoDeeplinkSearchBox.cancel() }
    }

    @Test
    fun `getPlaces returns error when location is not available`() = runTest {
        val geoDeeplinkSearchBox = mockk<GeoDeeplinkSearchBox>(relaxed = true)
        val geoDeeplink = mockk<GeoDeeplink>()

        val result = GeoDeeplinkSearchBoxPlacesListOnMapProvider(geoDeeplinkSearchBox, geoDeeplink)
            .getPlaces()

        assertNotNull(result.error)
    }

    @Test
    fun `getPlaces returns mapped place records on success`() = runTest {
        mockkLocation()

        val searchResult = mockk<SearchResult> {
            every { id } returns "result-1"
            every { name } returns "Coffee Shop"
            every { coordinate } returns Point.fromLngLat(-121.0, 45.0)
            every { descriptionText } returns "123 Main St"
            every { address } returns null
            every { categories } returns listOf("coffee")
        }
        val geoDeeplink = mockk<GeoDeeplink>()
        val geoDeeplinkSearchBox = mockk<GeoDeeplinkSearchBox> {
            coEvery { requestPlaces(geoDeeplink, any()) } returns listOf(searchResult)
        }

        val result = GeoDeeplinkSearchBoxPlacesListOnMapProvider(geoDeeplinkSearchBox, geoDeeplink)
            .getPlaces()
            .value!!

        assertEquals(1, result.size)
        assertEquals("Coffee Shop", result[0].name)
        assertEquals("result-1", result[0].id)
    }

    @Test
    fun `getPlaces returns error when requestPlaces returns null`() = runTest {
        mockkLocation()

        val geoDeeplink = mockk<GeoDeeplink>()
        val geoDeeplinkSearchBox = mockk<GeoDeeplinkSearchBox> {
            coEvery { requestPlaces(geoDeeplink, any()) } returns null
        }

        val result =
            GeoDeeplinkSearchBoxPlacesListOnMapProvider(geoDeeplinkSearchBox, geoDeeplink)
                .getPlaces()

        assertNotNull(result.error)
    }

    @Test
    fun `getPlaces returns error when requestPlaces returns empty list`() = runTest {
        mockkLocation()

        val geoDeeplink = mockk<GeoDeeplink>()
        val geoDeeplinkSearchBox = mockk<GeoDeeplinkSearchBox> {
            coEvery { requestPlaces(geoDeeplink, any()) } returns emptyList()
        }

        val result =
            GeoDeeplinkSearchBoxPlacesListOnMapProvider(geoDeeplinkSearchBox, geoDeeplink)
                .getPlaces()

        assertNotNull(result.error)
    }

    private fun mockkLocation(
        location: Location = mockk<Location> {
            every { longitude } returns -121.0
            every { latitude } returns 45.0
        },
    ) {
        coEvery { carLocationProvider.validLocation() } returns location
    }
}
