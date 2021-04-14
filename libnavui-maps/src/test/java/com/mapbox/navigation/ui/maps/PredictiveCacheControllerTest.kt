package com.mapbox.navigation.ui.maps

import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.Value
import com.mapbox.common.TileStore
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.StyleObjectInfo
import com.mapbox.maps.plugin.delegates.listeners.OnStyleLoadedListener
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.PredictiveCache
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(shadows = [ShadowTileStore::class])
@RunWith(RobolectricTestRunner::class)
class PredictiveCacheControllerTest {
    @Test
    fun `initialize creates Navigation Predictive Cache Controller`() {
        val mockedMapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        mockkObject(PredictiveCache)
        every {
            PredictiveCache.createNavigationController(any())
        } just Runs

        PredictiveCacheController(
            mockedMapboxNavigation,
        )

        verify {
            PredictiveCache.createNavigationController(
                mockedMapboxNavigation.navigationOptions.predictiveCacheLocationOptions
            )
        }

        unmockkObject(PredictiveCache)
    }

    @Test
    fun `initialize creates Maps Predictive Cache Controllers - null tileStorePath`() {
        val mockedMapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        mockkObject(PredictiveCache)
        every {
            PredictiveCache.createNavigationController(any())
        } just Runs
        val mockedMapboxMap = mockk<MapboxMap>(relaxed = true)
        every {
            mockedMapboxMap.getResourceOptions().tileStorePath
        } returns null
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
        val mockedTileStore = mockk<TileStore>()
        mockkStatic(TileStore::class)
        every {
            TileStore.getInstance()
        } returns mockedTileStore
        val slotIds = mutableListOf<String>()
        every {
            PredictiveCache.createMapsController(mockedTileStore, capture(slotIds))
        } just Runs

        predictiveCacheController.setMapInstance(mockedMapboxMap)

        verify(exactly = 2) {
            PredictiveCache.createMapsController(
                mockedTileStore,
                any(),
                any()
            )
        }
        assertEquals(
            listOf(
                "mapbox.mapbox-streets-v8,mapbox.mapbox-terrain-v2",
                "mapbox.satellite"
            ),
            slotIds
        )

        unmockkObject(PredictiveCache)
        unmockkStatic(TileStore::class)
    }

    @Test
    fun `initialize creates Maps Predictive Cache Controllers - non-null tileStorePath`() {
        val mockedMapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        mockkObject(PredictiveCache)
        every {
            PredictiveCache.createNavigationController(any())
        } just Runs
        val mockedMapboxMap = mockk<MapboxMap>(relaxed = true)
        every {
            mockedMapboxMap.getResourceOptions().tileStorePath
        } returns "/test/path"
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
        val mockedTileStore = mockk<TileStore>()
        mockkStatic(TileStore::class)
        every {
            TileStore.getInstance(any())
        } returns mockedTileStore
        val slotIds = mutableListOf<String>()
        every {
            PredictiveCache.createMapsController(mockedTileStore, capture(slotIds))
        } just Runs

        predictiveCacheController.setMapInstance(mockedMapboxMap)

        verify(exactly = 2) {
            PredictiveCache.createMapsController(
                mockedTileStore,
                any(),
                any()
            )
        }
        assertEquals(
            listOf(
                "mapbox.mapbox-streets-v8,mapbox.mapbox-terrain-v2",
                "mapbox.satellite"
            ),
            slotIds
        )

        unmockkObject(PredictiveCache)
        unmockkStatic(TileStore::class)
    }

    @Test
    fun `style change triggers Maps Predictive Cache Controllers update`() {
        val mockedMapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        mockkObject(PredictiveCache)
        every {
            PredictiveCache.createNavigationController(any())
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
        val mockedTileStore = mockk<TileStore>()
        mockkStatic(TileStore::class)
        every {
            TileStore.getInstance(any())
        } returns mockedTileStore
        val slotIds = mutableListOf<String>()
        every {
            PredictiveCache.createMapsController(mockedTileStore, capture(slotIds))
        } just Runs

        predictiveCacheController.setMapInstance(mockedMapboxMap)

        verify(exactly = 2) {
            PredictiveCache.createMapsController(
                mockedTileStore,
                any(),
                any()
            )
        }
        every {
            PredictiveCache.currentMapsPredictiveCacheControllers()
        } returns listOf("mapbox.satellite")

        val newStyle = mockk<Style>()
        every {
            mockedMapboxMap.getStyle()
        } returns newStyle
        val newMockedIds: List<String> = listOf(
            "mapbox://mapbox.mapbox-streets-v9"
        )
        val newStyleSources: List<StyleObjectInfo> = listOf(
            StyleObjectInfo(newMockedIds[0], "vector")
        )
        every { newStyle.styleSources } returns newStyleSources

        val newMockedPropertiesVector = mockk<Expected<Value, String>>(relaxed = true)
        val newContentsVector = mutableMapOf<String, Value>()
        newContentsVector["type"] = Value("vector")
        newContentsVector["url"] = Value("mapbox://mapbox.mapbox-streets-v9")
        every { newMockedPropertiesVector.value?.contents } returns newContentsVector
        every {
            newStyle.getStyleSourceProperties(newMockedIds[0])
        } returns newMockedPropertiesVector

        val removeSlotIds = mutableListOf<String>()
        every {
            PredictiveCache.removeMapsController(capture(removeSlotIds))
        } just Runs

        val addSlotIds = mutableListOf<String>()
        every {
            PredictiveCache.createMapsController(mockedTileStore, capture(addSlotIds))
        } just Runs

        val mapChangedListenerSlot = slot<OnStyleLoadedListener>()
        verify { mockedMapboxMap.addOnStyleLoadedListener(capture(mapChangedListenerSlot)) }
        mapChangedListenerSlot.captured.onStyleLoaded()

        assertEquals(listOf("mapbox.satellite"), removeSlotIds)
        assertEquals(listOf("mapbox.mapbox-streets-v9"), addSlotIds)

        unmockkObject(PredictiveCache)
        unmockkStatic(TileStore::class)
    }
}
