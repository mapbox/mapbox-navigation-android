package com.mapbox.navigation.ui.maps

import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.Value
import com.mapbox.common.TileStore
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.StyleObjectInfo
import com.mapbox.maps.TileStoreManager
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.PredictiveCache
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowTileStoreManager::class])
class PredictiveCacheControllerTest {
    @Test
    fun `initialize creates Navigation Predictive Cache Controller`() {
        val mockedMapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        mockkObject(PredictiveCache)
        every {
            PredictiveCache.createNavigationController(any(), any())
        } just Runs

        PredictiveCacheController(
            mockedMapboxNavigation,
        )

        verify {
            PredictiveCache.createNavigationController(
                mockedMapboxNavigation.navigationOptions.onboardRouterOptions,
                mockedMapboxNavigation.navigationOptions.predictiveCacheLocationOptions
            )
        }

        unmockkObject(PredictiveCache)
    }

    @Test
    fun `initialize creates Maps Predictive Cache Controllers`() {
        val mockedMapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        mockkObject(PredictiveCache)
        every {
            PredictiveCache.createNavigationController(any(), any())
        } just Runs
        val mockedMapboxMap = mockk<MapboxMap>(relaxed = true)
        val style = mockk<Style>()
        every {
            mockedMapboxMap.getStyle()
        } returns style
        val mockedIds: List<String> = listOf(
            "composite",
            "mapbox-navigation-waypoint-source",
            "mapbox://mapbox.satellite"
        )
        val styleSources: List<StyleObjectInfo> = listOf(
            StyleObjectInfo(mockedIds[0], "vector"),
            StyleObjectInfo(mockedIds[1], "geojson"),
            StyleObjectInfo(mockedIds[2], "raster")
        )
        every { style.styleSources } returns styleSources

        val mockedPropertiesVector = mockk<Expected<Value, String>>(relaxed = true)
        val contentsVector = mutableMapOf<String, Value>()
        contentsVector["type"] = Value("vector")
        contentsVector["url"] = Value("mapbox://mapbox.mapbox-streets-v8,mapbox.mapbox-terrain-v2")
        every { mockedPropertiesVector.value?.contents } returns contentsVector
        every { style.getStyleSourceProperties(mockedIds[0]) } returns mockedPropertiesVector

        val mockedPropertiesGeojson = mockk<Expected<Value, String>>(relaxed = true)
        val contentsGeojson = mutableMapOf<String, Value>()
        contentsGeojson["type"] = Value("geojson")
        every { mockedPropertiesGeojson.value?.contents } returns contentsGeojson
        every { style.getStyleSourceProperties(mockedIds[1]) } returns mockedPropertiesGeojson

        val mockedPropertiesRaster = mockk<Expected<Value, String>>(relaxed = true)
        val contentsRaster = mutableMapOf<String, Value>()
        contentsRaster["type"] = Value("raster")
        contentsRaster["url"] = Value("mapbox://mapbox.satellite")
        every { mockedPropertiesRaster.value?.contents } returns contentsRaster
        every { style.getStyleSourceProperties(mockedIds[2]) } returns mockedPropertiesRaster

        val predictiveCacheController = PredictiveCacheController(
            mockedMapboxNavigation,
        )
        val expected = mockk<Expected<TileStore, String>>(relaxed = true)
        mockkStatic(TileStoreManager::class)
        every {
            TileStoreManager.getTileStore(any())
        } returns expected
        val mockedTileStore = mockk<TileStore>()
        every { expected.value } returns mockedTileStore
        val slotIds = mutableListOf<String>()
        every {
            PredictiveCache.createMapsController(mockedTileStore, capture(slotIds))
        } just Runs

        predictiveCacheController.setMapInstance(mockedMapboxMap)

        verify(exactly = 1) {
            PredictiveCache.createMapsController(
                mockedTileStore,
                any(),
                any()
            )
        }
        assertEquals(listOf("mapbox.satellite"), slotIds)

        unmockkObject(PredictiveCache)
        unmockkStatic(TileStoreManager::class)
    }
}
