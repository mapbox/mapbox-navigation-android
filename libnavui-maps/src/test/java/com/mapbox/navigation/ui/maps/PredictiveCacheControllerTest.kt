package com.mapbox.navigation.ui.maps

import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.Value
import com.mapbox.common.TileStore
import com.mapbox.common.TilesetDescriptor
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.OfflineManagerInterface
import com.mapbox.maps.Style
import com.mapbox.maps.StyleObjectInfo
import com.mapbox.maps.TilesetDescriptorOptions
import com.mapbox.maps.plugin.delegates.listeners.OnStyleLoadedListener
import com.mapbox.navigation.base.options.PredictiveCacheLocationOptions
import com.mapbox.navigation.base.options.PredictiveCacheMapsOptions
import com.mapbox.navigation.base.options.PredictiveCacheNavigationOptions
import com.mapbox.navigation.base.options.PredictiveCacheOptions
import com.mapbox.navigation.core.internal.PredictiveCache
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import com.mapbox.navigation.ui.maps.internal.offline.OfflineManagerProvider
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(shadows = [ShadowTileStore::class])
@RunWith(RobolectricTestRunner::class)
class PredictiveCacheControllerTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    private val predictiveCache = mockk<PredictiveCache>(relaxed = true)

    private val errorHandler = mockk<PredictiveCacheControllerErrorHandler> {
        every { onError(any()) } just Runs
    }

    @Before
    fun setup() {
        mockkStatic(TileStore::class)
    }

    @After
    fun teardown() {
        unmockkStatic(TileStore::class)
    }

    @Test
    fun `sanity primary constructor`() {
        val locationOptions = mockk<PredictiveCacheLocationOptions>()
        val predictiveCacheOptions = mockk<PredictiveCacheOptions> {
            every { predictiveCacheNavigationOptions } returns mockk {
                every { predictiveCacheLocationOptions } returns locationOptions
            }
        }

        val predictiveCacheController = PredictiveCacheController(
            predictiveCacheOptions,
            predictiveCache
        )

        assertNull(predictiveCacheController.predictiveCacheControllerErrorHandler)
        verify(exactly = 1) {
            predictiveCache.createNavigationController(locationOptions)
        }
    }

    @Test
    fun `null tileStore creates error message and does not initialize Predictive Cache Controllers`() {
        val mockedLocationOptions: PredictiveCacheLocationOptions = mockk()
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
            buildOptions(mockedLocationOptions),
            predictiveCache
        ).apply {
            predictiveCacheControllerErrorHandler = errorHandler
        }

        predictiveCacheController.createMapControllers(mockedMapboxMap)

        verify { errorHandler.onError(any()) }
    }

    @Test
    fun `non-null tileStore initializes Maps Predictive Cache Controllers`() {
        val mockedLocationOptions: PredictiveCacheLocationOptions = mockk()
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
            buildOptions(mockedLocationOptions),
            predictiveCache
        ).apply {
            predictiveCacheControllerErrorHandler = errorHandler
        }

        val slotIds = mutableListOf<String>()
        every {
            predictiveCache.createMapsController(
                mockedMapboxMap,
                mockedTileStore,
                capture(slotIds),
                mockedLocationOptions
            )
        } just Runs

        predictiveCacheController.createMapControllers(mockedMapboxMap)

        verify(exactly = 2) {
            predictiveCache.createMapsController(
                mockedMapboxMap,
                mockedTileStore,
                any<String>(),
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
            buildOptions(mockedLocationOptions),
            predictiveCache
        ).apply {
            predictiveCacheControllerErrorHandler = errorHandler
        }

        val slotIds = mutableListOf<String>()
        every {
            predictiveCache.createMapsController(
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
            predictiveCache.createMapsController(
                mockedMapboxMap,
                mockedTileStore,
                any<String>(),
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
            buildOptions(mockedLocationOptions),
            predictiveCache
        ).apply {
            predictiveCacheControllerErrorHandler = errorHandler
        }
        val slotIds = mutableListOf<String>()
        every {
            predictiveCache.createMapsController(
                mockedMapboxMap,
                mockedTileStore,
                capture(slotIds),
                mockedLocationOptions
            )
        } just Runs

        predictiveCacheController.createMapControllers(mockedMapboxMap)

        verify(exactly = 2) {
            predictiveCache.createMapsController(
                mockedMapboxMap,
                mockedTileStore,
                any<String>(),
                any()
            )
        }
        every {
            predictiveCache.currentMapsPredictiveCacheControllers(mockedMapboxMap)
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
            predictiveCache.removeMapControllers(mockedMapboxMap, capture(removeSlotIds))
        } just Runs

        val addSlotIds = mutableListOf<String>()
        every {
            predictiveCache.createMapsController(
                mockedMapboxMap,
                mockedTileStore,
                capture(addSlotIds),
                mockedLocationOptions
            )
        } just Runs

        val mapChangedListenerSlot = slot<OnStyleLoadedListener>()
        verify { mockedMapboxMap.addOnStyleLoadedListener(capture(mapChangedListenerSlot)) }
        mapChangedListenerSlot.captured.onStyleLoaded(mockk())

        assertEquals(listOf("mapbox.satellite"), removeSlotIds)
        assertEquals(listOf("mapbox.mapbox-streets-v9"), addSlotIds)
        verify(exactly = 0) { errorHandler.onError(any()) }
    }

    @Test
    fun `cache controllers are removed when map added twice`() {
        val mockedLocationOptions: PredictiveCacheLocationOptions = mockk()
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
            buildOptions(mockedLocationOptions),
            predictiveCache
        ).apply {
            predictiveCacheControllerErrorHandler = errorHandler
        }

        val slotIds = mutableListOf<String>()
        every {
            predictiveCache.createMapsController(
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
            predictiveCache.createMapsController(
                mockedMapboxMap,
                mockedTileStore,
                any<String>(),
                any()
            )
        }
        verify(exactly = 1) {
            predictiveCache.removeAllMapControllersFromTileVariants(mockedMapboxMap)
        }
        verify(exactly = 0) { errorHandler.onError(any()) }
    }

    @Test
    fun `error style source properties create error message and does not initialize Predictive Cache Controllers`() {
        val mockedLocationOptions: PredictiveCacheLocationOptions = mockk()
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
            buildOptions(mockedLocationOptions),
            predictiveCache
        ).apply {
            predictiveCacheControllerErrorHandler = errorHandler
        }

        predictiveCacheController.createMapControllers(mockedMapboxMap)

        verify(exactly = 1) { errorHandler.onError(error) }
        verify(exactly = 0) {
            predictiveCache.createMapsController(any(), any(), any<String>(), any())
        }
    }

    @Test
    fun `check createMapsController`() {
        mockkObject(OfflineManagerProvider) {
            val locationOptions1 = mockk<PredictiveCacheLocationOptions>(relaxed = true)
            val locationOptions2 = mockk<PredictiveCacheLocationOptions>(relaxed = true)
            val predictiveCacheOptions = PredictiveCacheOptions.Builder()
                .predictiveCacheMapsOptionsList(
                    listOf(
                        PredictiveCacheMapsOptions.Builder()
                            .minZoom(40.toByte())
                            .maxZoom(50.toByte())
                            .predictiveCacheLocationOptions(locationOptions1)
                            .build(),
                        PredictiveCacheMapsOptions.Builder()
                            .minZoom(20.toByte())
                            .maxZoom(30.toByte())
                            .predictiveCacheLocationOptions(locationOptions2)
                            .build(),
                    )
                )
                .build()
            val mockedTileStore = mockk<TileStore>()
            every { TileStore.create(any()) } returns mockedTileStore
            val mockedMapboxMap = mockk<MapboxMap>(relaxed = true) {
                every { getResourceOptions().tileStore } returns mockedTileStore
            }
            val mockTilesetDescriptor1: TilesetDescriptor = mockk()
            val mockTilesetDescriptor2: TilesetDescriptor = mockk()
            val offlineManagerInterface = mockk<OfflineManagerInterface> {
                every {
                    createTilesetDescriptor(
                        match { options: TilesetDescriptorOptions ->
                            options.minZoom == 40.toByte()
                        }
                    )
                } returns mockTilesetDescriptor1
                every {
                    createTilesetDescriptor(
                        match { options: TilesetDescriptorOptions ->
                            options.minZoom == 20.toByte()
                        }
                    )
                } returns mockTilesetDescriptor2
            }
            every {
                OfflineManagerProvider.provideOfflineManager(any())
            } returns offlineManagerInterface
            val slotDescriptorsToOptions =
                slot<List<Pair<TilesetDescriptor, PredictiveCacheLocationOptions>>>()
            every {
                predictiveCache.createMapsControllers(
                    mockedMapboxMap,
                    mockedTileStore,
                    capture(slotDescriptorsToOptions)
                )
            } just Runs

            val predictiveCacheController = PredictiveCacheController(
                predictiveCacheOptions,
                predictiveCache
            )
            predictiveCacheController.predictiveCacheControllerErrorHandler = errorHandler
            predictiveCacheController.createStyleMapControllers(
                mockedMapboxMap,
                styles = listOf("mapbox://test_test", "non_valid://test_test")
            )

            val slotListTilesetDescriptorOptions = mutableListOf<TilesetDescriptorOptions>()
            verify(exactly = 2) {
                offlineManagerInterface.createTilesetDescriptor(
                    capture(slotListTilesetDescriptorOptions)
                )
            }
            assertEquals(40.toByte(), slotListTilesetDescriptorOptions[0].minZoom)
            assertEquals(50.toByte(), slotListTilesetDescriptorOptions[0].maxZoom)
            assertEquals("mapbox://test_test", slotListTilesetDescriptorOptions[0].styleURI)
            assertEquals(20.toByte(), slotListTilesetDescriptorOptions[1].minZoom)
            assertEquals(30.toByte(), slotListTilesetDescriptorOptions[1].maxZoom)
            assertEquals("mapbox://test_test", slotListTilesetDescriptorOptions[1].styleURI)

            assertEquals(2, slotDescriptorsToOptions.captured.size)
            assertEquals(mockTilesetDescriptor1, slotDescriptorsToOptions.captured[0].first)
            assertEquals(locationOptions1, slotDescriptorsToOptions.captured[0].second)
            assertEquals(mockTilesetDescriptor2, slotDescriptorsToOptions.captured[1].first)
            assertEquals(locationOptions2, slotDescriptorsToOptions.captured[1].second)
            // "non_valid://test_test
            verify(exactly = 1) { errorHandler.onError(any()) }
        }
    }

    private fun buildOptions(
        locationOptions: PredictiveCacheLocationOptions,
    ): PredictiveCacheOptions {
        return PredictiveCacheOptions.Builder().apply {
            predictiveCacheNavigationOptions(
                PredictiveCacheNavigationOptions.Builder().apply {
                    predictiveCacheLocationOptions(locationOptions)
                }.build()
            )
            predictiveCacheMapsOptions(
                PredictiveCacheMapsOptions.Builder().apply {
                    predictiveCacheLocationOptions(locationOptions)
                }.build()
            )
        }.build()
    }
}
