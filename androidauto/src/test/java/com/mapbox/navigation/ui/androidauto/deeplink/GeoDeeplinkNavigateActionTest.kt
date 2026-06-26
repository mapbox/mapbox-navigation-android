package com.mapbox.navigation.ui.androidauto.deeplink

import android.content.Intent
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.ui.androidauto.MapboxCarContext
import com.mapbox.navigation.ui.androidauto.MapboxCarOptions
import com.mapbox.navigation.ui.androidauto.placeslistonmap.PlacesListOnMapProvider
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreenManager
import com.mapbox.navigation.ui.androidauto.search.CarSearchMode
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class GeoDeeplinkNavigateActionTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    private val intent = mockk<Intent> {
        every { dataString } returns "geo:0,0?q=coffee"
    }

    @Before
    fun setUp() {
        mockkObject(MapboxScreenManager)
        every { MapboxScreenManager.push(any()) } just runs
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `legacy mode sets GeoDeeplinkPlacesListOnMapProvider`() {
        val mapboxCarContext = mockMapboxCarContext(CarSearchMode.Legacy)
        val geocodingProvider = mockk<GeoDeeplinkPlacesListOnMapProvider>(relaxed = true)

        mockGeoDeeplinkNavigateAction(
            mapboxCarContext = mapboxCarContext,
            geocodingProvider = geocodingProvider,
        ).onNewIntent(intent)

        verify {
            mapboxCarContext.geoDeeplinkPlacesProvider = geocodingProvider
        }
    }

    @Test
    fun `search box mode sets GeoDeeplinkSearchBoxPlacesListOnMapProvider`() {
        val mapboxCarContext = mockMapboxCarContext(CarSearchMode.SearchBox)
        val searchBoxProvider = mockk<GeoDeeplinkSearchBoxPlacesListOnMapProvider>(relaxed = true)

        mockGeoDeeplinkNavigateAction(
            mapboxCarContext = mapboxCarContext,
            searchBoxProvider = searchBoxProvider,
        ).onNewIntent(intent)

        verify {
            mapboxCarContext.geoDeeplinkPlacesProvider = searchBoxProvider
        }
    }

    @Test
    fun `onNewIntent pushes GEO_DEEPLINK screen`() {
        mockGeoDeeplinkNavigateAction(mockMapboxCarContext()).onNewIntent(intent)
        verify { MapboxScreenManager.push(MapboxScreen.GEO_DEEPLINK) }
    }

    private fun mockGeoDeeplinkNavigateAction(
        mapboxCarContext: MapboxCarContext,
        searchBoxProvider: PlacesListOnMapProvider = mockk(relaxed = true),
        geocodingProvider: PlacesListOnMapProvider = mockk(relaxed = true),
    ): GeoDeeplinkNavigateAction {
        return GeoDeeplinkNavigateAction(
            mapboxCarContext,
            { searchBoxProvider },
            { geocodingProvider },
        )
    }

    private fun mockMapboxCarContext(
        mode: CarSearchMode = CarSearchMode.Legacy,
    ): MapboxCarContext {
        return mockk<MapboxCarContext>(relaxed = true) {
            every { options } returns mockk<MapboxCarOptions> {
                every { searchMode } returns mode
            }
        }
    }
}
