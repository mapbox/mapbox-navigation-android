package com.mapbox.navigation.ui.maps

import com.mapbox.common.Cancelable
import com.mapbox.common.TileStore
import com.mapbox.common.TilesetDescriptor
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.MapboxMapsOptions
import com.mapbox.maps.TilesetDescriptorOptions
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
                predictiveCacheOptions.predictiveCacheNavigationOptions
                    .predictiveCacheLocationOptions,
            )
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
            val mockedMapboxMap = mockk<MapboxMap>(relaxed = true)
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
            assertEquals(locationOptions1, capturedKeys1[0].locationOptions)
            assertEquals(mockedTileStore, capturedKeys1[0].tileStore)
            assertEquals("mapbox://test_test", capturedKeys1[1].styleUri)
            assertEquals(mockTilesetDescriptor2, capturedKeys1[1].tilesetDescriptor)
            assertEquals(locationOptions2, capturedKeys1[1].locationOptions)
            assertEquals(mockedTileStore, capturedKeys1[1].tileStore)

            val capturedKeys3 = slotPredictiveCacheControllerKeys3.captured
            assertEquals(1, capturedKeys3.size)
            assertEquals("mapbox://test_explicit_options", capturedKeys3[0].styleUri)
            assertEquals(mockTilesetDescriptor3, capturedKeys3[0].tilesetDescriptor)
            assertEquals(locationOptions3, capturedKeys3[0].locationOptions)
            assertEquals(mockedTileStore, capturedKeys3[0].tileStore)

            // "non_valid://test_test
            verify(exactly = 1) { errorHandler.onError(any()) }
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
