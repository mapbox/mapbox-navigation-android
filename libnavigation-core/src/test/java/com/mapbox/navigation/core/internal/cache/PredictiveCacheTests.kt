package com.mapbox.navigation.core.internal.cache

import com.mapbox.common.TileStore
import com.mapbox.common.TilesetDescriptor
import com.mapbox.navigation.base.options.PredictiveCacheLocationOptions
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
        val navLocationOptions1: PredictiveCacheLocationOptions = mockk()
        val navLocationOptions2: PredictiveCacheLocationOptions = mockk()
        val navLocationOptions3: PredictiveCacheLocationOptions = mockk()
        val tilesetDescriptor1 = mockk<TilesetDescriptor>()
        val tilesetDescriptor2 = mockk<TilesetDescriptor>()
        val tilesetDescriptor3 = mockk<TilesetDescriptor>()

        val predictiveCacheControllerKey1 =
            PredictiveCache.PredictiveCacheControllerKey(
                "uri1",
                tileStore,
                tilesetDescriptor1,
                navLocationOptions1,
            )

        val predictiveCacheControllerKey2 =
            PredictiveCache.PredictiveCacheControllerKey(
                "uri2",
                tileStore,
                tilesetDescriptor2,
                navLocationOptions2,
            )

        val predictiveCacheControllerKey3 =
            PredictiveCache.PredictiveCacheControllerKey(
                "uri3",
                tileStore,
                tilesetDescriptor3,
                navLocationOptions3,
            )

        every {
            navigator.createNavigationPredictiveCacheController(navLocationOptions1)
        } returns mockk()

        every {
            navigator.createNavigationPredictiveCacheController(navLocationOptions2)
        } returns mockk()

        every {
            navigator.createNavigationPredictiveCacheController(navLocationOptions3)
        } returns mockk()

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

        predictiveCache.createNavigationController(navLocationOptions1)
        predictiveCache.createNavigationController(navLocationOptions2)

        val callback = navigatorRecreationCallbackSlot.captured
        callback.onNativeNavigatorRecreated()

        assertEquals(2, predictiveCache.cachedMapsPredictiveCacheControllers.size)
        assertEquals(2, predictiveCache.cachedMapsPredictiveCacheControllers[map1]!!.size)
        assertEquals(1, predictiveCache.cachedMapsPredictiveCacheControllers[map2]!!.size)

        assertEquals(2, predictiveCache.cachedNavigationPredictiveCacheControllers.size)
        assertEquals(2, predictiveCache.navPredictiveCacheLocationOptions.size)

        verify(exactly = 2) {
            navigator.createMapsPredictiveCacheController(
                tileStore,
                tilesetDescriptor1,
                navLocationOptions1,
            )
        }

        verify(exactly = 2) {
            navigator.createMapsPredictiveCacheController(
                tileStore,
                tilesetDescriptor2,
                navLocationOptions2,
            )
        }

        verify(exactly = 2) {
            navigator.createMapsPredictiveCacheController(
                tileStore,
                tilesetDescriptor3,
                navLocationOptions3,
            )
        }

        verify(exactly = 2) {
            navigator.createNavigationPredictiveCacheController(navLocationOptions1)
        }

        verify(exactly = 2) {
            navigator.createNavigationPredictiveCacheController(navLocationOptions2)
        }
    }

    @Test
    fun `caches are empty after clean`() {
        val navLocationOptions1: PredictiveCacheLocationOptions = mockk()
        val tilesetDescriptor1 = mockk<TilesetDescriptor>()

        val predictiveCacheControllerKey1 =
            PredictiveCache.PredictiveCacheControllerKey(
                "uri1",
                tileStore,
                tilesetDescriptor1,
                navLocationOptions1,
            )

        predictiveCache.createMapsControllers(
            mockk(),
            listOf(predictiveCacheControllerKey1),
        )
        predictiveCache.createNavigationController(mockk())

        predictiveCache.clean()

        assertEquals(0, predictiveCache.cachedNavigationPredictiveCacheControllers.size)
        assertEquals(0, predictiveCache.cachedMapsPredictiveCacheControllers.size)
        assertEquals(0, predictiveCache.navPredictiveCacheLocationOptions.size)
    }
}
