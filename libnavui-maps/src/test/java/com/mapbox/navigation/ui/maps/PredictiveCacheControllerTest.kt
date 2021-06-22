package com.mapbox.navigation.ui.maps

import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.Value
import com.mapbox.common.TileStore
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.StyleObjectInfo
import com.mapbox.maps.plugin.delegates.listeners.OnStyleLoadedListener
import com.mapbox.navigation.base.options.PredictiveCacheLocationOptions
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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(shadows = [ShadowTileStore::class])
@RunWith(RobolectricTestRunner::class)
class PredictiveCacheControllerTest {

    private val errorHandler = mockk<PredictiveCacheControllerErrorHandler>() {
        every { onError(any()) } just Runs
    }

    @Before
    fun setup() {
        mockkObject(PredictiveCache)
        mockkStatic(TileStore::class)
    }

    @After
    fun teardown() {
        unmockkObject(PredictiveCache)
        unmockkStatic(TileStore::class)
    }

    @Test
    fun `initialize creates Navigation Predictive Cache Controller`() {
        val mockedLocationOptions: PredictiveCacheLocationOptions = mockk()
        every {
            PredictiveCache.createNavigationController(any())
        } just Runs

        PredictiveCacheController(mockedLocationOptions, errorHandler)

        verify {
            PredictiveCache.createNavigationController(mockedLocationOptions)
        }

        verify(exactly = 0) { errorHandler.onError(any()) }
    }

    @Test
    fun `null tileStore creates error message and does not initialize Predictive Cache Controllers`() {
        val mockedLocationOptions: PredictiveCacheLocationOptions = mockk()
        every {
            PredictiveCache.createNavigationController(any())
        } just Runs
        val mockedMapboxMap = mockk<MapboxMap>(relaxed = true)
        every {
            mockedMapboxMap.getResourceOptions().tileStore
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

        val predictiveCacheController = PredictiveCacheController(
            mockedLocationOptions,
            errorHandler
        )

        predictiveCacheController.createMapControllers(mockedMapboxMap)

        verify { errorHandler.onError(any()) }
    }

    @Test
    fun `non-null tileStore initializes Maps Predictive Cache Controllers`() {
        val mockedLocationOptions: PredictiveCacheLocationOptions = mockk()
        every {
            PredictiveCache.createNavigationController(any())
        } just Runs
        val mockedTileStore = mockk<TileStore>()
        every { TileStore.create(any()) } returns mockedTileStore
        val mockedMapboxMap = mockk<MapboxMap>(relaxed = true) {
            every { getResourceOptions().tileStore } returns mockedTileStore
        }
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

        val mockedPropertiesVector = mockk<Expected<String, Value>>(relaxed = true)
        val contentsVector = mutableMapOf<String, Value>()
        contentsVector["type"] = Value("vector")
        contentsVector["url"] = Value("mapbox://mapbox.mapbox-streets-v8,mapbox.mapbox-terrain-v2")
        every { mockedPropertiesVector.value?.contents } returns contentsVector
        every { style.getStyleSourceProperties(mockedIds[0]) } returns mockedPropertiesVector

        val mockedPropertiesGeojson = mockk<Expected<String, Value>>(relaxed = true)
        val contentsGeojson = mutableMapOf<String, Value>()
        contentsGeojson["type"] = Value("geojson")
        every { mockedPropertiesGeojson.value?.contents } returns contentsGeojson
        every { style.getStyleSourceProperties(mockedIds[1]) } returns mockedPropertiesGeojson

        val mockedPropertiesRaster = mockk<Expected<String, Value>>(relaxed = true)
        val contentsRaster = mutableMapOf<String, Value>()
        contentsRaster["type"] = Value("raster")
        contentsRaster["url"] = Value("mapbox://mapbox.satellite")
        every { mockedPropertiesRaster.value?.contents } returns contentsRaster
        every { style.getStyleSourceProperties(mockedIds[2]) } returns mockedPropertiesRaster

        val predictiveCacheController = PredictiveCacheController(
            mockedLocationOptions,
            errorHandler
        )

        val slotIds = mutableListOf<String>()
        every {
            PredictiveCache.createMapsController(
                mockedMapboxMap,
                mockedTileStore,
                capture(slotIds),
                mockedLocationOptions
            )
        } just Runs

        predictiveCacheController.createMapControllers(mockedMapboxMap)

        verify(exactly = 2) {
            PredictiveCache.createMapsController(
                mockedMapboxMap,
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
        verify(exactly = 0) { errorHandler.onError(any()) }
    }

    @Test
    fun `Maps Predictive Cache Controllers initialized for passed sources`() {
        val mockedLocationOptions: PredictiveCacheLocationOptions = mockk()
        every {
            PredictiveCache.createNavigationController(any())
        } just Runs
        val mockedTileStore = mockk<TileStore>()
        every { TileStore.create(any()) } returns mockedTileStore
        val mockedMapboxMap = mockk<MapboxMap>(relaxed = true) {
            every { getResourceOptions().tileStore } returns mockedTileStore
        }
        val style = mockk<Style>()
        every {
            mockedMapboxMap.getStyle()
        } returns style
        val mockedIds: List<String> = listOf(
            "composite",
            "mapbox-navigation-waypoint-source",
            "mapbox://mapbox.satellite",
            "mapbox://mapbox.mapbox-terrain-v2",
            "mapbox://mapbox.transit-v2",
        )
        val styleSources: List<StyleObjectInfo> = listOf(
            StyleObjectInfo(mockedIds[0], "vector"),
            StyleObjectInfo(mockedIds[1], "geojson"),
            StyleObjectInfo(mockedIds[2], "raster"),
            StyleObjectInfo(mockedIds[3], "vector"),
            StyleObjectInfo(mockedIds[4], "vector"),
        )
        every { style.styleSources } returns styleSources

        val mockedPropertiesVector = mockk<Expected<String, Value>>(relaxed = true)
        val contentsVector = mutableMapOf<String, Value>()
        contentsVector["type"] = Value("vector")
        contentsVector["url"] = Value("mapbox://mapbox.mapbox-streets-v8,mapbox.mapbox-terrain-v2")
        every { mockedPropertiesVector.value?.contents } returns contentsVector
        every { style.getStyleSourceProperties(mockedIds[0]) } returns mockedPropertiesVector

        val mockedPropertiesGeojson = mockk<Expected<String, Value>>(relaxed = true)
        val contentsGeojson = mutableMapOf<String, Value>()
        contentsGeojson["type"] = Value("geojson")
        every { mockedPropertiesGeojson.value?.contents } returns contentsGeojson
        every { style.getStyleSourceProperties(mockedIds[1]) } returns mockedPropertiesGeojson

        val mockedPropertiesRaster = mockk<Expected<String, Value>>(relaxed = true)
        val contentsRaster = mutableMapOf<String, Value>()
        contentsRaster["type"] = Value("raster")
        contentsRaster["url"] = Value("mapbox://mapbox.satellite")
        every { mockedPropertiesRaster.value?.contents } returns contentsRaster
        every { style.getStyleSourceProperties(mockedIds[2]) } returns mockedPropertiesRaster

        val mockedPropertiesVectorSecond = mockk<Expected<String, Value>>(relaxed = true)
        val contentsVectorSecond = mutableMapOf<String, Value>()
        contentsVectorSecond["type"] = Value("vector")
        contentsVectorSecond["url"] = Value("mapbox://mapbox.mapbox-terrain-v2")
        every { mockedPropertiesVectorSecond.value?.contents } returns contentsVectorSecond
        every { style.getStyleSourceProperties(mockedIds[3]) } returns mockedPropertiesVectorSecond

        val mockedPropertiesVectorThird = mockk<Expected<String, Value>>(relaxed = true)
        val contentsVectorThird = mutableMapOf<String, Value>()
        contentsVectorThird["type"] = Value("vector")
        contentsVectorThird["url"] = Value("mapbox://mapbox.transit-v2")
        every { mockedPropertiesVectorThird.value?.contents } returns contentsVectorThird
        every { style.getStyleSourceProperties(mockedIds[4]) } returns mockedPropertiesVectorThird

        val predictiveCacheController = PredictiveCacheController(
            mockedLocationOptions,
            errorHandler
        )

        val slotIds = mutableListOf<String>()
        every {
            PredictiveCache.createMapsController(
                mockedMapboxMap,
                mockedTileStore,
                capture(slotIds),
                mockedLocationOptions
            )
        } just Runs

        predictiveCacheController.createMapControllers(
            mockedMapboxMap,
            listOf(
                "mapbox://mapbox.satellite",
                "mapbox://mapbox.transit-v2",
            )
        )

        verify(exactly = 2) {
            PredictiveCache.createMapsController(
                mockedMapboxMap,
                mockedTileStore,
                any(),
                any()
            )
        }
        assertEquals(
            listOf("mapbox.satellite", "mapbox.transit-v2"),
            slotIds
        )
        verify(exactly = 0) { errorHandler.onError(any()) }
    }

    @Test
    fun `style change triggers Maps Predictive Cache Controllers update`() {
        val mockedLocationOptions: PredictiveCacheLocationOptions = mockk()
        every {
            PredictiveCache.createNavigationController(any())
        } just Runs
        val mockedTileStore = mockk<TileStore>()
        every { TileStore.create(any()) } returns mockedTileStore
        val mockedMapboxMap = mockk<MapboxMap>(relaxed = true) {
            every { getResourceOptions().tileStore } returns mockedTileStore
        }
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

        val mockedPropertiesVector = mockk<Expected<String, Value>>(relaxed = true)
        val contentsVector = mutableMapOf<String, Value>()
        contentsVector["type"] = Value("vector")
        contentsVector["url"] = Value("mapbox://mapbox.mapbox-streets-v8,mapbox.mapbox-terrain-v2")
        every { mockedPropertiesVector.value?.contents } returns contentsVector
        every { style.getStyleSourceProperties(mockedIds[0]) } returns mockedPropertiesVector

        val mockedPropertiesGeojson = mockk<Expected<String, Value>>(relaxed = true)
        val contentsGeojson = mutableMapOf<String, Value>()
        contentsGeojson["type"] = Value("geojson")
        every { mockedPropertiesGeojson.value?.contents } returns contentsGeojson
        every { style.getStyleSourceProperties(mockedIds[1]) } returns mockedPropertiesGeojson

        val mockedPropertiesRaster = mockk<Expected<String, Value>>(relaxed = true)
        val contentsRaster = mutableMapOf<String, Value>()
        contentsRaster["type"] = Value("raster")
        contentsRaster["url"] = Value("mapbox://mapbox.satellite")
        every { mockedPropertiesRaster.value?.contents } returns contentsRaster
        every { style.getStyleSourceProperties(mockedIds[2]) } returns mockedPropertiesRaster

        val predictiveCacheController = PredictiveCacheController(
            mockedLocationOptions,
            errorHandler
        )
        val slotIds = mutableListOf<String>()
        every {
            PredictiveCache.createMapsController(
                mockedMapboxMap,
                mockedTileStore,
                capture(slotIds),
                mockedLocationOptions
            )
        } just Runs

        predictiveCacheController.createMapControllers(mockedMapboxMap)

        verify(exactly = 2) {
            PredictiveCache.createMapsController(
                mockedMapboxMap,
                mockedTileStore,
                any(),
                any()
            )
        }
        every {
            PredictiveCache.currentMapsPredictiveCacheControllers(mockedMapboxMap)
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

        val newMockedPropertiesVector = mockk<Expected<String, Value>>(relaxed = true)
        val newContentsVector = mutableMapOf<String, Value>()
        newContentsVector["type"] = Value("vector")
        newContentsVector["url"] = Value("mapbox://mapbox.mapbox-streets-v9")
        every { newMockedPropertiesVector.value?.contents } returns newContentsVector
        every {
            newStyle.getStyleSourceProperties(newMockedIds[0])
        } returns newMockedPropertiesVector

        val removeSlotIds = mutableListOf<String>()
        every {
            PredictiveCache.removeMapControllers(mockedMapboxMap, capture(removeSlotIds))
        } just Runs

        val addSlotIds = mutableListOf<String>()
        every {
            PredictiveCache.createMapsController(
                mockedMapboxMap,
                mockedTileStore,
                capture(addSlotIds),
                mockedLocationOptions
            )
        } just Runs

        val mapChangedListenerSlot = slot<OnStyleLoadedListener>()
        verify { mockedMapboxMap.addOnStyleLoadedListener(capture(mapChangedListenerSlot)) }
        mapChangedListenerSlot.captured.onStyleLoaded()

        assertEquals(listOf("mapbox.satellite"), removeSlotIds)
        assertEquals(listOf("mapbox.mapbox-streets-v9"), addSlotIds)
        verify(exactly = 0) { errorHandler.onError(any()) }
    }

    @Test
    fun `cache controllers are removed when map added twice`() {
        val mockedLocationOptions: PredictiveCacheLocationOptions = mockk()
        every {
            PredictiveCache.createNavigationController(any())
        } just Runs
        val mockedTileStore = mockk<TileStore>()
        every { TileStore.create(any()) } returns mockedTileStore
        val mockedMapboxMap = mockk<MapboxMap>(relaxed = true) {
            every { getResourceOptions().tileStore } returns mockedTileStore
        }
        val style = mockk<Style>()
        every {
            mockedMapboxMap.getStyle()
        } returns style
        val mockedIds: List<String> = listOf("mapbox://mapbox.satellite")
        val styleSources: List<StyleObjectInfo> = listOf(StyleObjectInfo(mockedIds[0], "raster"))
        every { style.styleSources } returns styleSources

        val mockedPropertiesRaster = mockk<Expected<String, Value>>(relaxed = true)
        val contentsRaster = mutableMapOf<String, Value>()
        contentsRaster["type"] = Value("raster")
        contentsRaster["url"] = Value("mapbox://mapbox.satellite")
        every { mockedPropertiesRaster.value?.contents } returns contentsRaster
        every { style.getStyleSourceProperties(mockedIds[0]) } returns mockedPropertiesRaster

        val predictiveCacheController = PredictiveCacheController(
            mockedLocationOptions,
            errorHandler
        )

        val slotIds = mutableListOf<String>()
        every {
            PredictiveCache.createMapsController(
                mockedMapboxMap,
                mockedTileStore,
                capture(slotIds),
                mockedLocationOptions
            )
        } just Runs

        predictiveCacheController.createMapControllers(mockedMapboxMap)
        predictiveCacheController.createMapControllers(mockedMapboxMap)

        assertEquals(listOf("mapbox.satellite", "mapbox.satellite"), slotIds)

        verify(exactly = 2) {
            PredictiveCache.createMapsController(
                mockedMapboxMap,
                mockedTileStore,
                any(),
                any()
            )
        }
        verify(exactly = 1) { PredictiveCache.removeAllMapControllers(mockedMapboxMap) }
        verify(exactly = 0) { errorHandler.onError(any()) }
    }

    @Test
    fun `error style source properties create error message and does not initialize Predictive Cache Controllers`() {
        val mockedLocationOptions: PredictiveCacheLocationOptions = mockk()
        every {
            PredictiveCache.createNavigationController(any())
        } just Runs
        val mockedTileStore = mockk<TileStore>()
        every { TileStore.create(any()) } returns mockedTileStore
        val mockedMapboxMap = mockk<MapboxMap>(relaxed = true) {
            every { getResourceOptions().tileStore } returns mockedTileStore
        }
        val style = mockk<Style>()
        every {
            mockedMapboxMap.getStyle()
        } returns style
        val mockedIds: List<String> = listOf("mapbox://mapbox.mapbox-terrain-v2")
        val styleSources: List<StyleObjectInfo> = listOf(StyleObjectInfo(mockedIds[0], "vector"))
        every { style.styleSources } returns styleSources

        val mockedPropertiesVector = mockk<Expected<String, Value>>(relaxed = true)
        every { mockedPropertiesVector.isError } returns true
        val error = "style source property error"
        every { mockedPropertiesVector.error } returns error
        every { style.getStyleSourceProperties(mockedIds[0]) } returns mockedPropertiesVector

        val predictiveCacheController = PredictiveCacheController(
            mockedLocationOptions,
            errorHandler
        )

        predictiveCacheController.createMapControllers(mockedMapboxMap)

        verify(exactly = 1) { errorHandler.onError(error) }
        verify(exactly = 0) {
            PredictiveCache.createMapsController(any(), any(), any(), any())
        }
    }
}
