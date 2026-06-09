package com.mapbox.navigation.ui.maps

import com.mapbox.common.Cancelable
import com.mapbox.common.TileStore
import com.mapbox.common.TilesetDescriptor
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.MapboxMapsOptions
import com.mapbox.maps.TilesetDescriptorOptions
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.options.PredictiveCacheLocationOptions
import com.mapbox.navigation.base.options.PredictiveCacheMapsOptions
import com.mapbox.navigation.base.options.PredictiveCacheOptions
import com.mapbox.navigation.base.options.PredictiveCacheSearchOptions
import com.mapbox.navigation.core.internal.PredictiveCache
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import io.mockk.Ordering
import io.mockk.Runs
import io.mockk.clearAllMocks
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(shadows = [ShadowTileStore::class])
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class PredictiveCacheControllerTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    private val predictiveCache = mockk<PredictiveCache>(relaxed = true)
    private val errorHandler = mockk<PredictiveCacheControllerErrorHandler> {
        every { onError(any()) } just Runs
    }

    @Before
    fun setup() {
        mockkObject(OfflineManagerProvider)
        mockkStatic(MapboxMapsOptions::class)
        mockkStatic(TileStore::class)
    }

    @After
    fun teardown() {
        unmockkObject(OfflineManagerProvider)
        unmockkStatic(MapboxMapsOptions::class)
        unmockkStatic(TileStore::class)
    }

    @Test
    fun `sanity primary constructor`() {
        val predictiveCacheOptions = PredictiveCacheOptions.Builder().build()
        val mockedTileStore = mockk<TileStore>()
        every { TileStore.create(any()) } returns mockedTileStore
        every { MapboxMapsOptions.tileStore } returns mockedTileStore

        val predictiveCacheController = PredictiveCacheController(
            predictiveCacheOptions,
            predictiveCache,
        )

        assertNull(predictiveCacheController.predictiveCacheControllerErrorHandler)
        verify(Ordering.SEQUENCE) {
            predictiveCache.createNavigationController(
                predictiveCacheOptions.predictiveCacheNavigationOptions,
            )
        }
    }

    @Test
    fun `check createMapsController without current style`() {
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
                    ),
                )
                .build()

            val locationOptions3 = mockk<PredictiveCacheLocationOptions>(relaxed = true)
            val predictiveCacheMapOptions = PredictiveCacheMapsOptions.Builder()
                .minZoom(70.toByte())
                .maxZoom(80.toByte())
                .predictiveCacheLocationOptions(locationOptions3)
                .build()

            val mockedTileStore = mockk<TileStore>()
            every { TileStore.create(any()) } returns mockedTileStore
            every { MapboxMapsOptions.tileStore } returns mockedTileStore
            val mockedMapboxMap = mockk<MapboxMap>(relaxed = true) {
                every { style } returns null
            }
            val mockTilesetDescriptor1: TilesetDescriptor = mockk()
            val mockTilesetDescriptor2: TilesetDescriptor = mockk()
            val mockTilesetDescriptor3: TilesetDescriptor = mockk()
            val offlineManager = mockk<OfflineManagerProxy> {
                every {
                    createTilesetDescriptor(
                        match { options: TilesetDescriptorOptions ->
                            options.minZoom == 40.toByte()
                        },
                    )
                } returns mockTilesetDescriptor1
                every {
                    createTilesetDescriptor(
                        match { options: TilesetDescriptorOptions ->
                            options.minZoom == 20.toByte()
                        },
                    )
                } returns mockTilesetDescriptor2
                every {
                    createTilesetDescriptor(
                        match { options: TilesetDescriptorOptions ->
                            options.minZoom == 70.toByte()
                        },
                    )
                } returns mockTilesetDescriptor3
            }
            every {
                OfflineManagerProvider.provideOfflineManager()
            } returns offlineManager
            val slotPredictiveCacheControllerKeys1 =
                slot<List<PredictiveCache.PredictiveCacheControllerKey>>()
            every {
                predictiveCache.createMapsControllers(
                    mockedMapboxMap,
                    capture(slotPredictiveCacheControllerKeys1),
                )
            } just Runs

            val predictiveCacheController = PredictiveCacheController(
                predictiveCacheOptions,
                predictiveCache,
            )

            predictiveCacheController.predictiveCacheControllerErrorHandler = errorHandler
            predictiveCacheController.createStyleMapControllers(
                mockedMapboxMap,
                styles = listOf("mapbox://test_test", "non_valid://test_test"),
            )

            val slotPredictiveCacheControllerKeys3 =
                slot<List<PredictiveCache.PredictiveCacheControllerKey>>()
            every {
                predictiveCache.createMapsControllers(
                    mockedMapboxMap,
                    capture(slotPredictiveCacheControllerKeys3),
                )
            } just Runs

            predictiveCacheController.createStyleMapControllers(
                mockedMapboxMap,
                styles = listOf("mapbox://test_explicit_options"),
                predictiveCacheMapOptions = listOf(predictiveCacheMapOptions),
            )

            val slotListTilesetDescriptorOptions = mutableListOf<TilesetDescriptorOptions>()
            verify(exactly = 3) {
                offlineManager.createTilesetDescriptor(
                    capture(slotListTilesetDescriptorOptions),
                )
            }

            assertEquals(40.toByte(), slotListTilesetDescriptorOptions[0].minZoom)
            assertEquals(50.toByte(), slotListTilesetDescriptorOptions[0].maxZoom)
            assertEquals("mapbox://test_test", slotListTilesetDescriptorOptions[0].styleURI)
            assertEquals(20.toByte(), slotListTilesetDescriptorOptions[1].minZoom)
            assertEquals(30.toByte(), slotListTilesetDescriptorOptions[1].maxZoom)
            assertEquals("mapbox://test_test", slotListTilesetDescriptorOptions[1].styleURI)
            assertEquals(70.toByte(), slotListTilesetDescriptorOptions[2].minZoom)
            assertEquals(80.toByte(), slotListTilesetDescriptorOptions[2].maxZoom)
            assertEquals(
                "mapbox://test_explicit_options",
                slotListTilesetDescriptorOptions[2].styleURI,
            )

            val capturedKeys1 = slotPredictiveCacheControllerKeys1.captured
            assertEquals(2, capturedKeys1.size)
            assertEquals("mapbox://test_test", capturedKeys1[0].styleUri)
            assertEquals(mockTilesetDescriptor1, capturedKeys1[0].tilesetDescriptor)
            assertEquals(locationOptions1, capturedKeys1[0].options.predictiveCacheLocationOptions)
            assertEquals(mockedTileStore, capturedKeys1[0].tileStore)
            assertEquals("mapbox://test_test", capturedKeys1[1].styleUri)
            assertEquals(mockTilesetDescriptor2, capturedKeys1[1].tilesetDescriptor)
            assertEquals(locationOptions2, capturedKeys1[1].options.predictiveCacheLocationOptions)
            assertEquals(mockedTileStore, capturedKeys1[1].tileStore)

            val capturedKeys3 = slotPredictiveCacheControllerKeys3.captured
            assertEquals(1, capturedKeys3.size)
            assertEquals("mapbox://test_explicit_options", capturedKeys3[0].styleUri)
            assertEquals(mockTilesetDescriptor3, capturedKeys3[0].tilesetDescriptor)
            assertEquals(locationOptions3, capturedKeys3[0].options.predictiveCacheLocationOptions)
            assertEquals(mockedTileStore, capturedKeys3[0].tileStore)

            // "non_valid://test_test
            verify(exactly = 1) { errorHandler.onError(any()) }
        }
    }

    @Test
    fun `check createMapsController with current style`() {
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
                    ),
                )
                .build()

            val mockedTileStore = mockk<TileStore>()
            every { TileStore.create(any()) } returns mockedTileStore
            every { MapboxMapsOptions.tileStore } returns mockedTileStore
            val mockedMapboxMap = mockk<MapboxMap>(relaxed = true) {
                every { style } returns mockk(relaxed = true) {
                    every { styleURI } returns "mapbox://test_test"
                }
            }
            val mockTilesetDescriptor1: TilesetDescriptor = mockk()
            val mockTilesetDescriptor2: TilesetDescriptor = mockk()
            val offlineManager = mockk<OfflineManagerProxy> {
                every {
                    createTilesetDescriptor(
                        match { options: TilesetDescriptorOptions ->
                            options.minZoom == 40.toByte()
                        },
                    )
                } returns mockTilesetDescriptor1
                every {
                    createTilesetDescriptor(
                        match { options: TilesetDescriptorOptions ->
                            options.minZoom == 20.toByte()
                        },
                    )
                } returns mockTilesetDescriptor2
            }
            every {
                OfflineManagerProvider.provideOfflineManager()
            } returns offlineManager
            val slotPredictiveCacheControllerKeys1 =
                slot<List<PredictiveCache.PredictiveCacheControllerKey>>()
            every {
                predictiveCache.createMapsControllers(
                    mockedMapboxMap,
                    capture(slotPredictiveCacheControllerKeys1),
                )
            } just Runs

            val predictiveCacheController = PredictiveCacheController(
                predictiveCacheOptions,
                predictiveCache,
            )

            predictiveCacheController.predictiveCacheControllerErrorHandler = errorHandler
            predictiveCacheController.createStyleMapControllers(
                mockedMapboxMap,
                styles = emptyList(),
            )

            val slotListTilesetDescriptorOptions = mutableListOf<TilesetDescriptorOptions>()
            verify(exactly = 2) {
                offlineManager.createTilesetDescriptor(
                    capture(slotListTilesetDescriptorOptions),
                )
            }

            assertEquals(40.toByte(), slotListTilesetDescriptorOptions[0].minZoom)
            assertEquals(50.toByte(), slotListTilesetDescriptorOptions[0].maxZoom)
            assertEquals("mapbox://test_test", slotListTilesetDescriptorOptions[0].styleURI)
            assertEquals(20.toByte(), slotListTilesetDescriptorOptions[1].minZoom)
            assertEquals(30.toByte(), slotListTilesetDescriptorOptions[1].maxZoom)
            assertEquals("mapbox://test_test", slotListTilesetDescriptorOptions[1].styleURI)

            val capturedKeys1 = slotPredictiveCacheControllerKeys1.captured
            assertEquals(2, capturedKeys1.size)
            assertEquals("mapbox://test_test", capturedKeys1[0].styleUri)
            assertEquals(mockTilesetDescriptor1, capturedKeys1[0].tilesetDescriptor)
            assertEquals(locationOptions1, capturedKeys1[0].options.predictiveCacheLocationOptions)
            assertEquals(mockedTileStore, capturedKeys1[0].tileStore)
            assertEquals("mapbox://test_test", capturedKeys1[1].styleUri)
            assertEquals(mockTilesetDescriptor2, capturedKeys1[1].tilesetDescriptor)
            assertEquals(locationOptions2, capturedKeys1[1].options.predictiveCacheLocationOptions)
            assertEquals(mockedTileStore, capturedKeys1[1].tileStore)

            verify(exactly = 0) { errorHandler.onError(any()) }
        }
    }

    @Test
    fun `check createSearchControllers`() {
        mockkObject(OfflineManagerProvider) {
            val mockTilesetDescriptor1: TilesetDescriptor = mockk()
            val mockTilesetDescriptor2: TilesetDescriptor = mockk()
            val locationOptions1 = mockk<PredictiveCacheLocationOptions>(relaxed = true)
            val locationOptions2 = mockk<PredictiveCacheLocationOptions>(relaxed = true)
            val predictiveCacheOptions = PredictiveCacheOptions.Builder()
                .predictiveCacheSearchOptionsList(
                    listOf(
                        PredictiveCacheSearchOptions
                            .Builder(mockTilesetDescriptor1)
                            .predictiveCacheLocationOptions(locationOptions1)
                            .build(),
                        PredictiveCacheSearchOptions
                            .Builder(mockTilesetDescriptor2)
                            .predictiveCacheLocationOptions(locationOptions2)
                            .build(),
                    ),
                )
                .build()
            val mockedTileStore = mockk<TileStore>()
            every { TileStore.create(any()) } returns mockedTileStore
            every { MapboxMapsOptions.tileStore } returns mockedTileStore
            val mockedMapboxMap = mockk<MapboxMap>(relaxed = true)
            val slotDescriptorsToOptions =
                slot<List<Pair<TilesetDescriptor, PredictiveCacheLocationOptions>>>()
            every {
                predictiveCache.createSearchControllers(
                    mockedTileStore,
                    capture(slotDescriptorsToOptions),
                )
            } just Runs

            val predictiveCacheController = PredictiveCacheController(
                predictiveCacheOptions,
                predictiveCache,
            )
            predictiveCacheController.predictiveCacheControllerErrorHandler = errorHandler

            assertEquals(2, slotDescriptorsToOptions.captured.size)
            assertEquals(mockTilesetDescriptor1, slotDescriptorsToOptions.captured[0].first)
            assertEquals(locationOptions1, slotDescriptorsToOptions.captured[0].second)
            assertEquals(mockTilesetDescriptor2, slotDescriptorsToOptions.captured[1].first)
            assertEquals(locationOptions2, slotDescriptorsToOptions.captured[1].second)
        }
    }

    @Test
    fun `createMapControllers cancels previous subscriptions for valid map`() {
        val predictiveCacheOptions = PredictiveCacheOptions.Builder().build()
        val mockedTileStore = mockk<TileStore>()
        every { TileStore.create(any()) } returns mockedTileStore
        every { MapboxMapsOptions.tileStore } returns mockedTileStore
        val styleLoadedTask1 = mockk<Cancelable>(relaxed = true)
        val styleLoadedTask2 = mockk<Cancelable>(relaxed = true)
        val mockedMapboxMap1 = mockk<MapboxMap>(relaxed = true) {
            every { subscribeStyleLoaded(any()) } returns styleLoadedTask1
            every { isValid() } returns true
        }
        val mockedMapboxMap2 = mockk<MapboxMap>(relaxed = true) {
            every { subscribeStyleLoaded(any()) } returns styleLoadedTask2
            every { isValid() } returns true
        }
        val offlineManager = mockk<OfflineManagerProxy> {
            every {
                createTilesetDescriptor(any())
            } returns mockk()
        }
        every {
            OfflineManagerProvider.provideOfflineManager()
        } returns offlineManager

        val predictiveCacheController = PredictiveCacheController(
            predictiveCacheOptions,
            predictiveCache,
        )

        predictiveCacheController.createStyleMapControllers(mockedMapboxMap1)
        predictiveCacheController.createStyleMapControllers(mockedMapboxMap2)
        clearAllMocks(answers = false)

        predictiveCacheController.createStyleMapControllers(mockedMapboxMap1)
        verify(exactly = 1) { styleLoadedTask1.cancel() }
        verify(exactly = 0) { styleLoadedTask2.cancel() }
    }

    @Test
    fun `createMapControllers cancels previous subscriptions for invalid map`() {
        val predictiveCacheOptions = PredictiveCacheOptions.Builder().build()
        val mockedTileStore = mockk<TileStore>()
        every { TileStore.create(any()) } returns mockedTileStore
        every { MapboxMapsOptions.tileStore } returns mockedTileStore
        val styleLoadedTask1 = mockk<Cancelable>(relaxed = true)
        val mockedMapboxMap1 = mockk<MapboxMap>(relaxed = true) {
            every { subscribeStyleLoaded(any()) } returns styleLoadedTask1
            every { isValid() } returns false
        }
        val offlineManager = mockk<OfflineManagerProxy> {
            every {
                createTilesetDescriptor(any())
            } returns mockk()
        }
        every {
            OfflineManagerProvider.provideOfflineManager()
        } returns offlineManager

        val predictiveCacheController = PredictiveCacheController(
            predictiveCacheOptions,
            predictiveCache,
        )
        predictiveCacheController.createStyleMapControllers(mockedMapboxMap1)
        clearAllMocks(answers = false)

        predictiveCacheController.createStyleMapControllers(mockedMapboxMap1)
        verify(exactly = 1) { styleLoadedTask1.cancel() }
    }

    @Test
    fun `createTilesetsMapController forwards each option to descriptor with empty styleURI`() {
        val locationOptions1 = mockk<PredictiveCacheLocationOptions>(relaxed = true)
        val locationOptions2 = mockk<PredictiveCacheLocationOptions>(relaxed = true)
        val opts1 = PredictiveCacheMapsOptions.Builder()
            .minZoom(10.toByte()).maxZoom(14.toByte())
            .tilesets(listOf("mapbox://mapbox.mapbox-traffic-v1"))
            .predictiveCacheLocationOptions(locationOptions1)
            .build()
        val opts2 = PredictiveCacheMapsOptions.Builder()
            .minZoom(0.toByte()).maxZoom(8.toByte())
            .predictiveCacheLocationOptions(locationOptions2)
            .build()

        val mockedTileStore = mockk<TileStore>()
        every { TileStore.create(any()) } returns mockedTileStore
        every { MapboxMapsOptions.tileStore } returns mockedTileStore

        val mockedMap = mockk<MapboxMap>(relaxed = true) { every { style } returns null }

        val descriptor1 = mockk<TilesetDescriptor>()
        val descriptor2 = mockk<TilesetDescriptor>()
        val offlineManager = mockk<OfflineManagerProxy> {
            every {
                createTilesetDescriptor(match { it.minZoom == 10.toByte() })
            } returns descriptor1
            every {
                createTilesetDescriptor(match { it.minZoom == 0.toByte() })
            } returns descriptor2
        }
        every { OfflineManagerProvider.provideOfflineManager() } returns offlineManager

        val capturedKeys = slot<List<PredictiveCache.PredictiveCacheControllerKey>>()
        every { predictiveCache.createMapsControllers(mockedMap, capture(capturedKeys)) } just Runs

        val controller = PredictiveCacheController(
            PredictiveCacheOptions.Builder().build(),
            predictiveCache,
        )
        controller.predictiveCacheControllerErrorHandler = errorHandler

        controller.createTilesetsMapController(mockedMap, listOf(opts1, opts2))

        val capturedDescriptorOpts = mutableListOf<TilesetDescriptorOptions>()
        verify(exactly = 2) {
            offlineManager.createTilesetDescriptor(
                capture(capturedDescriptorOpts),
            )
        }
        assertEquals("", capturedDescriptorOpts[0].styleURI)
        assertEquals(10.toByte(), capturedDescriptorOpts[0].minZoom)
        assertEquals(14.toByte(), capturedDescriptorOpts[0].maxZoom)
        assertEquals(
            listOf("mapbox://mapbox.mapbox-traffic-v1"),
            capturedDescriptorOpts[0].tilesets,
        )
        assertEquals("", capturedDescriptorOpts[1].styleURI)

        val keys = capturedKeys.captured
        assertEquals(2, keys.size)
        assertEquals("", keys[0].styleUri)
        assertEquals(descriptor1, keys[0].tilesetDescriptor)
        assertEquals(locationOptions1, keys[0].options.predictiveCacheLocationOptions)
        assertEquals(mockedTileStore, keys[0].tileStore)
        assertEquals("", keys[1].styleUri)
        assertEquals(descriptor2, keys[1].tilesetDescriptor)

        verify(exactly = 0) { mockedMap.subscribeStyleLoaded(any()) }
        verify(exactly = 0) { errorHandler.onError(any()) }
    }

    @Test
    fun `createTilesetsMapController falls back to controller-level options when override is null`() {
        val defaultLocOpts1 = mockk<PredictiveCacheLocationOptions>(relaxed = true)
        val defaultLocOpts2 = mockk<PredictiveCacheLocationOptions>(relaxed = true)
        val predictiveCacheOptions = PredictiveCacheOptions.Builder()
            .predictiveCacheMapsOptionsList(
                listOf(
                    PredictiveCacheMapsOptions.Builder()
                        .minZoom(5.toByte()).maxZoom(9.toByte())
                        .predictiveCacheLocationOptions(defaultLocOpts1)
                        .build(),
                    PredictiveCacheMapsOptions.Builder()
                        .minZoom(11.toByte()).maxZoom(13.toByte())
                        .predictiveCacheLocationOptions(defaultLocOpts2)
                        .build(),
                ),
            )
            .build()

        val mockedTileStore = mockk<TileStore>()
        every { TileStore.create(any()) } returns mockedTileStore
        every { MapboxMapsOptions.tileStore } returns mockedTileStore

        val mockedMap = mockk<MapboxMap>(relaxed = true) { every { style } returns null }
        val descriptor = mockk<TilesetDescriptor>()
        val offlineManager = mockk<OfflineManagerProxy> {
            every { createTilesetDescriptor(any()) } returns descriptor
        }
        every { OfflineManagerProvider.provideOfflineManager() } returns offlineManager

        val capturedKeys = slot<List<PredictiveCache.PredictiveCacheControllerKey>>()
        every { predictiveCache.createMapsControllers(mockedMap, capture(capturedKeys)) } just Runs

        val controller = PredictiveCacheController(predictiveCacheOptions, predictiveCache)

        controller.createTilesetsMapController(mockedMap)

        val captured = mutableListOf<TilesetDescriptorOptions>()
        verify(exactly = 2) { offlineManager.createTilesetDescriptor(capture(captured)) }
        assertEquals(5.toByte(), captured[0].minZoom)
        assertEquals(9.toByte(), captured[0].maxZoom)
        assertEquals(11.toByte(), captured[1].minZoom)
        assertEquals(13.toByte(), captured[1].maxZoom)

        val keys = capturedKeys.captured
        assertEquals(2, keys.size)
        assertEquals(defaultLocOpts1, keys[0].options.predictiveCacheLocationOptions)
        assertEquals(defaultLocOpts2, keys[1].options.predictiveCacheLocationOptions)
    }

    @Test
    fun `createTilesetsMapController reports error and does nothing when TileStore is not configured`() {
        every { MapboxMapsOptions.tileStore } returns null

        val offlineManager = mockk<OfflineManagerProxy>(relaxed = true)
        every { OfflineManagerProvider.provideOfflineManager() } returns offlineManager

        val controller = PredictiveCacheController(
            PredictiveCacheOptions.Builder().build(),
            predictiveCache,
        )
        controller.predictiveCacheControllerErrorHandler = errorHandler

        val mockedMap = mockk<MapboxMap>(relaxed = true)
        val opts = PredictiveCacheMapsOptions.Builder()
            .predictiveCacheLocationOptions(mockk(relaxed = true))
            .build()

        controller.createTilesetsMapController(mockedMap, listOf(opts))

        verify(exactly = 1) {
            errorHandler.onError("TileStore instance not configured for the Map.")
        }
        verify(exactly = 0) { offlineManager.createTilesetDescriptor(any()) }
        verify(exactly = 0) { predictiveCache.createMapsControllers(any(), any()) }
    }

    @Test
    fun `createTilesetsMapController with empty options list calls predictiveCache with empty keys`() {
        val mockedTileStore = mockk<TileStore>()
        every { TileStore.create(any()) } returns mockedTileStore
        every { MapboxMapsOptions.tileStore } returns mockedTileStore

        val mockedMap = mockk<MapboxMap>(relaxed = true) { every { style } returns null }
        val offlineManager = mockk<OfflineManagerProxy>(relaxed = true)
        every { OfflineManagerProvider.provideOfflineManager() } returns offlineManager

        val capturedKeys = slot<List<PredictiveCache.PredictiveCacheControllerKey>>()
        every { predictiveCache.createMapsControllers(mockedMap, capture(capturedKeys)) } just Runs

        val controller = PredictiveCacheController(
            PredictiveCacheOptions.Builder().build(),
            predictiveCache,
        )
        controller.predictiveCacheControllerErrorHandler = errorHandler

        controller.createTilesetsMapController(mockedMap, emptyList())

        verify(exactly = 0) { offlineManager.createTilesetDescriptor(any()) }
        verify(exactly = 1) { predictiveCache.createMapsControllers(mockedMap, any()) }
        assertEquals(
            emptyList<PredictiveCache.PredictiveCacheControllerKey>(),
            capturedKeys.captured,
        )
        verify(exactly = 0) { errorHandler.onError(any()) }
    }

    @Test
    fun `createTilesetsMapController forwards a non-null tilesets list to the descriptor`() {
        val tilesetsList = listOf(
            "mapbox://mapbox.mapbox-streets-v8",
            "mapbox://mapbox.mapbox-terrain-v2",
        )
        val opts = PredictiveCacheMapsOptions.Builder()
            .minZoom(4.toByte()).maxZoom(12.toByte())
            .tilesets(tilesetsList)
            .predictiveCacheLocationOptions(mockk(relaxed = true))
            .build()

        val mockedTileStore = mockk<TileStore>()
        every { TileStore.create(any()) } returns mockedTileStore
        every { MapboxMapsOptions.tileStore } returns mockedTileStore

        val mockedMap = mockk<MapboxMap>(relaxed = true) { every { style } returns null }
        val offlineManager = mockk<OfflineManagerProxy> {
            every { createTilesetDescriptor(any()) } returns mockk()
        }
        every { OfflineManagerProvider.provideOfflineManager() } returns offlineManager
        every { predictiveCache.createMapsControllers(any(), any()) } just Runs

        val controller = PredictiveCacheController(
            PredictiveCacheOptions.Builder().build(),
            predictiveCache,
        )

        controller.createTilesetsMapController(mockedMap, listOf(opts))

        val captured = slot<TilesetDescriptorOptions>()
        verify(exactly = 1) { offlineManager.createTilesetDescriptor(capture(captured)) }
        assertEquals(tilesetsList, captured.captured.tilesets)
        assertEquals("", captured.captured.styleURI)
    }

    @Test
    fun `createTilesetsMapController tolerates null tilesets on options`() {
        val opts = PredictiveCacheMapsOptions.Builder()
            .minZoom(0.toByte()).maxZoom(6.toByte())
            // tilesets() not called -> stays null
            .predictiveCacheLocationOptions(mockk(relaxed = true))
            .build()
        assertNull(opts.tilesets)

        val mockedTileStore = mockk<TileStore>()
        every { TileStore.create(any()) } returns mockedTileStore
        every { MapboxMapsOptions.tileStore } returns mockedTileStore

        val mockedMap = mockk<MapboxMap>(relaxed = true) { every { style } returns null }
        val offlineManager = mockk<OfflineManagerProxy> {
            every { createTilesetDescriptor(any()) } returns mockk()
        }
        every { OfflineManagerProvider.provideOfflineManager() } returns offlineManager
        every { predictiveCache.createMapsControllers(any(), any()) } just Runs

        val controller = PredictiveCacheController(
            PredictiveCacheOptions.Builder().build(),
            predictiveCache,
        )
        controller.predictiveCacheControllerErrorHandler = errorHandler

        controller.createTilesetsMapController(mockedMap, listOf(opts))

        verify(exactly = 1) { offlineManager.createTilesetDescriptor(any()) }
        verify(exactly = 1) { predictiveCache.createMapsControllers(mockedMap, any()) }
        verify(exactly = 0) { errorHandler.onError(any()) }
    }

    @Test
    fun `createTilesetsMapController does not subscribe to style loaded`() {
        val mockedTileStore = mockk<TileStore>()
        every { TileStore.create(any()) } returns mockedTileStore
        every { MapboxMapsOptions.tileStore } returns mockedTileStore

        val styleLoadedTask = mockk<Cancelable>(relaxed = true)
        val mockedMap = mockk<MapboxMap>(relaxed = true) {
            every { subscribeStyleLoaded(any()) } returns styleLoadedTask
            every { style } returns null
            every { isValid() } returns true
        }
        val offlineManager = mockk<OfflineManagerProxy> {
            every { createTilesetDescriptor(any()) } returns mockk()
        }
        every { OfflineManagerProvider.provideOfflineManager() } returns offlineManager
        every { predictiveCache.createMapsControllers(any(), any()) } just Runs

        val opts = PredictiveCacheMapsOptions.Builder()
            .predictiveCacheLocationOptions(mockk(relaxed = true))
            .build()

        val controller = PredictiveCacheController(
            PredictiveCacheOptions.Builder().build(),
            predictiveCache,
        )

        controller.createTilesetsMapController(mockedMap, listOf(opts))

        verify(exactly = 0) { mockedMap.subscribeStyleLoaded(any()) }

        controller.removeMapControllers(mockedMap)
        verify(exactly = 0) { styleLoadedTask.cancel() }
        verify(exactly = 1) { predictiveCache.removeAllMapControllersFromDescriptors(mockedMap) }
    }

    @Test
    fun `createTilesetsMapController does not trigger Mapbox URL prefix validation`() {
        val mockedTileStore = mockk<TileStore>()
        every { TileStore.create(any()) } returns mockedTileStore
        every { MapboxMapsOptions.tileStore } returns mockedTileStore

        val mockedMap = mockk<MapboxMap>(relaxed = true) { every { style } returns null }
        val offlineManager = mockk<OfflineManagerProxy> {
            every { createTilesetDescriptor(any()) } returns mockk()
        }
        every { OfflineManagerProvider.provideOfflineManager() } returns offlineManager
        every { predictiveCache.createMapsControllers(any(), any()) } just Runs

        val opts = PredictiveCacheMapsOptions.Builder()
            .predictiveCacheLocationOptions(mockk(relaxed = true))
            .build()

        val controller = PredictiveCacheController(
            PredictiveCacheOptions.Builder().build(),
            predictiveCache,
        )
        controller.predictiveCacheControllerErrorHandler = errorHandler

        controller.createTilesetsMapController(mockedMap, listOf(opts))

        val captured = slot<TilesetDescriptorOptions>()
        verify(exactly = 1) { offlineManager.createTilesetDescriptor(capture(captured)) }
        assertEquals("", captured.captured.styleURI)
        verify(exactly = 0) { errorHandler.onError(any()) }
    }

    @Test
    fun `createTilesetsMapController invoked twice creates two independent batches`() {
        val mockedTileStore = mockk<TileStore>()
        every { TileStore.create(any()) } returns mockedTileStore
        every { MapboxMapsOptions.tileStore } returns mockedTileStore

        val mockedMap = mockk<MapboxMap>(relaxed = true) { every { style } returns null }
        val offlineManager = mockk<OfflineManagerProxy> {
            every { createTilesetDescriptor(any()) } returns mockk()
        }
        every { OfflineManagerProvider.provideOfflineManager() } returns offlineManager

        val capturedBatches = mutableListOf<List<PredictiveCache.PredictiveCacheControllerKey>>()
        every {
            predictiveCache.createMapsControllers(mockedMap, capture(capturedBatches))
        } just Runs

        val opts1 = PredictiveCacheMapsOptions.Builder()
            .minZoom(2.toByte()).maxZoom(4.toByte())
            .predictiveCacheLocationOptions(mockk(relaxed = true))
            .build()
        val opts2 = PredictiveCacheMapsOptions.Builder()
            .minZoom(6.toByte()).maxZoom(8.toByte())
            .predictiveCacheLocationOptions(mockk(relaxed = true))
            .build()
        val opts3 = PredictiveCacheMapsOptions.Builder()
            .minZoom(10.toByte()).maxZoom(12.toByte())
            .predictiveCacheLocationOptions(mockk(relaxed = true))
            .build()

        val controller = PredictiveCacheController(
            PredictiveCacheOptions.Builder().build(),
            predictiveCache,
        )

        controller.createTilesetsMapController(mockedMap, listOf(opts1))
        controller.createTilesetsMapController(mockedMap, listOf(opts2, opts3))

        verify(exactly = 3) { offlineManager.createTilesetDescriptor(any()) }
        verify(exactly = 2) { predictiveCache.createMapsControllers(mockedMap, any()) }

        assertEquals(2, capturedBatches.size)
        assertEquals(1, capturedBatches[0].size)
        assertEquals(2, capturedBatches[1].size)
        verify(exactly = 0) { mockedMap.subscribeStyleLoaded(any()) }
    }

    @Test
    fun `removeMapControllers cancels subscriptions for valid map`() {
        val predictiveCacheOptions = PredictiveCacheOptions.Builder().build()
        val mockedTileStore = mockk<TileStore>()
        every { TileStore.create(any()) } returns mockedTileStore
        every { MapboxMapsOptions.tileStore } returns mockedTileStore
        val styleLoadedTask1 = mockk<Cancelable>(relaxed = true)
        val styleLoadedTask2 = mockk<Cancelable>(relaxed = true)
        val mockedMapboxMap1 = mockk<MapboxMap>(relaxed = true) {
            every { subscribeStyleLoaded(any()) } returns styleLoadedTask1
            every { isValid() } returns true
        }
        val mockedMapboxMap2 = mockk<MapboxMap>(relaxed = true) {
            every { subscribeStyleLoaded(any()) } returns styleLoadedTask2
        }
        val offlineManager = mockk<OfflineManagerProxy> {
            every {
                createTilesetDescriptor(any())
            } returns mockk()
        }
        every {
            OfflineManagerProvider.provideOfflineManager()
        } returns offlineManager

        val predictiveCacheController = PredictiveCacheController(
            predictiveCacheOptions,
            predictiveCache,
        )

        predictiveCacheController.createStyleMapControllers(mockedMapboxMap1)
        predictiveCacheController.createStyleMapControllers(mockedMapboxMap2)
        clearAllMocks(answers = false)

        predictiveCacheController.removeMapControllers(mockedMapboxMap1)
        verify(exactly = 1) { styleLoadedTask1.cancel() }
        verify(exactly = 0) { styleLoadedTask2.cancel() }
    }

    @Test
    fun `removeMapControllers cancels subscriptions for invalid map`() {
        val predictiveCacheOptions = PredictiveCacheOptions.Builder().build()
        val mockedTileStore = mockk<TileStore>()
        every { TileStore.create(any()) } returns mockedTileStore
        every { MapboxMapsOptions.tileStore } returns mockedTileStore
        val styleLoadedTask1 = mockk<Cancelable>(relaxed = true)
        val mockedMapboxMap1 = mockk<MapboxMap>(relaxed = true) {
            every { subscribeStyleLoaded(any()) } returns styleLoadedTask1
            every { isValid() } returns false
        }
        val offlineManager = mockk<OfflineManagerProxy> {
            every {
                createTilesetDescriptor(any())
            } returns mockk()
        }
        every {
            OfflineManagerProvider.provideOfflineManager()
        } returns offlineManager

        val predictiveCacheController = PredictiveCacheController(
            predictiveCacheOptions,
            predictiveCache,
        )

        predictiveCacheController.createStyleMapControllers(mockedMapboxMap1)
        clearAllMocks(answers = false)

        predictiveCacheController.removeMapControllers(mockedMapboxMap1)
        verify(exactly = 1) { styleLoadedTask1.cancel() }
    }

    @Test
    fun `onDestroy cancels subscriptions for both valid and invalid maps`() {
        val predictiveCacheOptions = PredictiveCacheOptions.Builder().build()
        val mockedTileStore = mockk<TileStore>()
        every { TileStore.create(any()) } returns mockedTileStore
        every { MapboxMapsOptions.tileStore } returns mockedTileStore
        val styleLoadedTask1 = mockk<Cancelable>(relaxed = true)
        val styleLoadedTask2 = mockk<Cancelable>(relaxed = true)
        val mockedMapboxMap1 = mockk<MapboxMap>(relaxed = true) {
            every { subscribeStyleLoaded(any()) } returns styleLoadedTask1
            every { isValid() } returns false
        }
        val mockedMapboxMap2 = mockk<MapboxMap>(relaxed = true) {
            every { subscribeStyleLoaded(any()) } returns styleLoadedTask2
            every { isValid() } returns true
        }
        val offlineManager = mockk<OfflineManagerProxy> {
            every {
                createTilesetDescriptor(any())
            } returns mockk()
        }
        every {
            OfflineManagerProvider.provideOfflineManager()
        } returns offlineManager

        val predictiveCacheController = PredictiveCacheController(
            predictiveCacheOptions,
            predictiveCache,
        )

        predictiveCacheController.createStyleMapControllers(mockedMapboxMap1)
        predictiveCacheController.createStyleMapControllers(mockedMapboxMap2)
        clearAllMocks(answers = false)

        predictiveCacheController.onDestroy()
        verify(exactly = 1) {
            styleLoadedTask1.cancel()
            styleLoadedTask2.cancel()
        }
    }
}
