package com.mapbox.navigation.core.internal.cache

import com.mapbox.common.TileStore
import com.mapbox.common.TilesetDescriptor
import com.mapbox.navigation.base.options.PredictiveCacheLocationOptions
import com.mapbox.navigation.base.options.PredictiveCacheNavigationOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.PredictiveCache
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigation.navigator.internal.NativeNavigatorRecreationObserver
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class PredictiveCacheTests {

    private val tileStore: TileStore = mockk()
    private val navigatorRecreationCallbackSlot = slot<NativeNavigatorRecreationObserver>()
    private val navigator: MapboxNativeNavigator = mockk(relaxed = true)
    private lateinit var predictiveCache: PredictiveCache

    @Before
    fun setUp() {
        every {
            navigator.setNativeNavigatorRecreationObserver(
                capture(navigatorRecreationCallbackSlot),
            )
        } just Runs

        val mapboxNavigation = mockk<MapboxNavigation>(relaxed = true)
        every { mapboxNavigation.navigator } returns navigator
        predictiveCache = PredictiveCache(mapboxNavigation)
    }

    @Test
    fun `size of map controllers is correct`() {
        val map1 = mockk<Any>()
        val map2 = mockk<Any>()
        val options1 = mockk<PredictiveCacheLocationOptions>(relaxed = true)
        val options2 = mockk<PredictiveCacheLocationOptions>(relaxed = true)
        val tilesetDescriptor1 = mockk<TilesetDescriptor>()
        val tilesetDescriptor2 = mockk<TilesetDescriptor>()

        val predictiveCacheControllerKey1 =
            PredictiveCache.PredictiveCacheControllerKey(
                "uri1",
                tileStore,
                tilesetDescriptor1,
                options1,
            )

        val predictiveCacheControllerKey2 =
            PredictiveCache.PredictiveCacheControllerKey(
                "uri2",
                tileStore,
                tilesetDescriptor2,
                options2,
            )

        predictiveCache.createMapsControllers(
            map1,
            listOf(
                predictiveCacheControllerKey1,
                predictiveCacheControllerKey2,
            ),
        )
        predictiveCache.createMapsControllers(
            map2,
            listOf(predictiveCacheControllerKey2),
        )

        assertEquals(2, predictiveCache.cachedMapsPredictiveCacheControllers[map1]!!.size)
        assertEquals(1, predictiveCache.cachedMapsPredictiveCacheControllers[map2]!!.size)

        predictiveCache.createMapsControllers(
            map1,
            listOf(predictiveCacheControllerKey1),
        )
        predictiveCache.createMapsControllers(
            map2,
            listOf(predictiveCacheControllerKey2, predictiveCacheControllerKey1),
        )

        assertEquals(2, predictiveCache.cachedMapsPredictiveCacheControllers[map1]!!.size)
        assertEquals(2, predictiveCache.cachedMapsPredictiveCacheControllers[map2]!!.size)
    }

    @Test
    fun `map controllers are empty after removing all`() {
        val map = mockk<Any>()

        val options1 = mockk<PredictiveCacheLocationOptions>(relaxed = true)
        val options2 = mockk<PredictiveCacheLocationOptions>(relaxed = true)
        val tilesetDescriptor1 = mockk<TilesetDescriptor>()
        val tilesetDescriptor2 = mockk<TilesetDescriptor>()

        val predictiveCacheControllerKey1 =
            PredictiveCache.PredictiveCacheControllerKey(
                "uri1",
                tileStore,
                tilesetDescriptor1,
                options1,
            )

        val predictiveCacheControllerKey2 =
            PredictiveCache.PredictiveCacheControllerKey(
                "uri2",
                tileStore,
                tilesetDescriptor2,
                options2,
            )

        predictiveCache.createMapsControllers(
            map,
            listOf(predictiveCacheControllerKey1, predictiveCacheControllerKey2),
        )

        predictiveCache.removeAllMapControllersFromDescriptors(map)

        assertEquals(null, predictiveCache.cachedMapsPredictiveCacheControllers[map])
    }

    @Test
    fun `controllers are recreated when navigator is recreated`() {
        val navOptions1: PredictiveCacheNavigationOptions = mockk()
        val navOptions2: PredictiveCacheNavigationOptions = mockk()
        val navOptions3: PredictiveCacheNavigationOptions = mockk()

        val predictiveCacheControllerKey1 = createPredictiveCacheControllerKey("uri1")
        val predictiveCacheControllerKey2 = createPredictiveCacheControllerKey("uri2")
        val predictiveCacheControllerKey3 = createPredictiveCacheControllerKey("uri3")

        every {
            navigator.createNavigationPredictiveCacheController(navOptions1)
        } returns listOf(mockk())

        every {
            navigator.createNavigationPredictiveCacheController(navOptions2)
        } returns listOf(mockk(), mockk())

        every {
            navigator.createNavigationPredictiveCacheController(navOptions3)
        } returns listOf(mockk())

        val map1 = mockk<Any>()
        val map2 = mockk<Any>()

        predictiveCache.createMapsControllers(
            map1,
            listOf(predictiveCacheControllerKey1, predictiveCacheControllerKey2),
        )
        predictiveCache.createMapsControllers(
            map2,
            listOf(predictiveCacheControllerKey3),
        )

        predictiveCache.createNavigationController(navOptions1)
        predictiveCache.createNavigationController(navOptions2)

        val callback = navigatorRecreationCallbackSlot.captured
        callback.onNativeNavigatorRecreated()

        assertEquals(2, predictiveCache.cachedMapsPredictiveCacheControllers.size)
        assertEquals(2, predictiveCache.cachedMapsPredictiveCacheControllers[map1]!!.size)
        assertEquals(1, predictiveCache.cachedMapsPredictiveCacheControllers[map2]!!.size)

        assertEquals(3, predictiveCache.cachedNavigationPredictiveCacheControllers.size)
        assertEquals(2, predictiveCache.navPredictiveCacheOptions.size)

        verify(exactly = 2) {
            navigator.createMapsPredictiveCacheController(
                tileStore,
                predictiveCacheControllerKey1.tilesetDescriptor,
                predictiveCacheControllerKey1.locationOptions,
            )
        }

        verify(exactly = 2) {
            navigator.createMapsPredictiveCacheController(
                tileStore,
                predictiveCacheControllerKey2.tilesetDescriptor,
                predictiveCacheControllerKey2.locationOptions,
            )
        }

        verify(exactly = 2) {
            navigator.createMapsPredictiveCacheController(
                tileStore,
                predictiveCacheControllerKey3.tilesetDescriptor,
                predictiveCacheControllerKey3.locationOptions,
            )
        }

        verify(exactly = 2) {
            navigator.createNavigationPredictiveCacheController(navOptions1)
        }

        verify(exactly = 2) {
            navigator.createNavigationPredictiveCacheController(navOptions2)
        }
    }

    @Test
    fun `caches are empty after clean`() {
        predictiveCache.createMapsControllers(
            mockk(),
            listOf(createPredictiveCacheControllerKey("uri1")),
        )

        predictiveCache.createNavigationController(mockk())

        predictiveCache.createSearchControllers(
            mockk(),
            listOf(
                mockk<TilesetDescriptor>(relaxed = true) to mockk(relaxed = true),
                mockk<TilesetDescriptor>(relaxed = true) to mockk(relaxed = true),
            ),
        )

        predictiveCache.clean()

        assertEquals(0, predictiveCache.cachedNavigationPredictiveCacheControllers.size)
        assertEquals(0, predictiveCache.cachedMapsPredictiveCacheControllers.size)
        assertEquals(0, predictiveCache.navPredictiveCacheOptions.size)
        assertEquals(0, predictiveCache.searchPredictiveCacheLocationOptions.size)
    }

    private fun createPredictiveCacheControllerKey(
        styleUri: String,
        tileStore: TileStore = this.tileStore,
        tilesetDescriptor: TilesetDescriptor = mockk(),
        locationOptions: PredictiveCacheLocationOptions = mockk(),
    ) = PredictiveCache.PredictiveCacheControllerKey(
        styleUri,
        tileStore,
        tilesetDescriptor,
        locationOptions,
    )
}
