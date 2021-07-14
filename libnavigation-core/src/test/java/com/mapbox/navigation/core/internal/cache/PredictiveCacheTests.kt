package com.mapbox.navigation.core.internal.cache

import com.mapbox.common.TileStore
import com.mapbox.navigation.base.options.PredictiveCacheLocationOptions
import com.mapbox.navigation.core.internal.PredictiveCache
import com.mapbox.navigation.navigator.internal.MapboxNativeNavigatorImpl
import com.mapbox.navigation.navigator.internal.NativeNavigatorRecreationObserver
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class PredictiveCacheTests {

    private val tileStore: TileStore = mockk()
    private val navigatorRecreationCallbackSlot = slot<NativeNavigatorRecreationObserver>()

    @Before
    fun setUp() {
        mockkObject(MapboxNativeNavigatorImpl)

        every {
            MapboxNativeNavigatorImpl.createNavigationPredictiveCacheController(any())
        } returns mockk()

        every {
            MapboxNativeNavigatorImpl.createMapsPredictiveCacheController(any(), any(), any())
        } returns mockk()

        every {
            MapboxNativeNavigatorImpl.setNativeNavigatorRecreationObserver(
                capture(navigatorRecreationCallbackSlot)
            )
        } just Runs

        PredictiveCache.clean()
    }

    @After
    fun cleanUp() {
        unmockkObject(MapboxNativeNavigatorImpl)
    }

    @Test
    fun `size of map controllers is correct`() {
        val map1 = mockk<Any>()
        val map2 = mockk<Any>()

        PredictiveCache.createMapsController(map1, tileStore, TILE_VARIANT_1, mockk())
        PredictiveCache.createMapsController(map1, tileStore, TILE_VARIANT_2, mockk())
        PredictiveCache.createMapsController(map1, tileStore, TILE_VARIANT_3, mockk())

        PredictiveCache.createMapsController(map2, tileStore, TILE_VARIANT_4, mockk())
        PredictiveCache.createMapsController(map2, tileStore, TILE_VARIANT_5, mockk())
        PredictiveCache.createMapsController(map2, tileStore, TILE_VARIANT_6, mockk())
        PredictiveCache.createMapsController(map2, tileStore, TILE_VARIANT_7, mockk())

        assertEquals(3, PredictiveCache.currentMapsPredictiveCacheControllers(map1).size)
        assertEquals(4, PredictiveCache.currentMapsPredictiveCacheControllers(map2).size)
    }

    @Test
    fun `size of map controllers is correct after removing`() {
        val map = mockk<Any>()

        PredictiveCache.createMapsController(map, tileStore, TILE_VARIANT_1, mockk())
        PredictiveCache.createMapsController(map, tileStore, TILE_VARIANT_2, mockk())
        PredictiveCache.createMapsController(map, tileStore, TILE_VARIANT_3, mockk())
        PredictiveCache.createMapsController(map, tileStore, TILE_VARIANT_4, mockk())

        PredictiveCache.removeMapControllers(map, TILE_VARIANT_2)
        PredictiveCache.removeMapControllers(map, TILE_VARIANT_1)
        PredictiveCache.removeMapControllers(map, TILE_VARIANT_4)

        assertEquals(1, PredictiveCache.currentMapsPredictiveCacheControllers(map).size)
        assertEquals(1, PredictiveCache.cachedMapsPredictiveCacheControllers[map]?.size)
        assertEquals(TILE_VARIANT_3, PredictiveCache.currentMapsPredictiveCacheControllers(map)[0])
    }

    @Test
    fun `map controllers are empty after removing all`() {
        val map = mockk<Any>()

        PredictiveCache.createMapsController(map, tileStore, TILE_VARIANT_1, mockk())
        PredictiveCache.createMapsController(map, tileStore, TILE_VARIANT_2, mockk())
        PredictiveCache.createMapsController(map, tileStore, TILE_VARIANT_3, mockk())

        PredictiveCache.removeAllMapControllers(map)

        assertEquals(0, PredictiveCache.currentMapsPredictiveCacheControllers(map).size)
        assertEquals(null, PredictiveCache.mapsPredictiveCacheLocationOptions[map])
        assertEquals(null, PredictiveCache.cachedMapsPredictiveCacheControllers[map])
    }

    @Test
    fun `controllers are recreated when navigator is recreated`() {
        val navLocationOptions1: PredictiveCacheLocationOptions = mockk()
        val navLocationOptions2: PredictiveCacheLocationOptions = mockk()

        every {
            MapboxNativeNavigatorImpl.createNavigationPredictiveCacheController(navLocationOptions1)
        } returns mockk()

        every {
            MapboxNativeNavigatorImpl.createNavigationPredictiveCacheController(navLocationOptions2)
        } returns mockk()

        val map1 = mockk<Any>()
        val map2 = mockk<Any>()

        PredictiveCache.init()

        PredictiveCache.createMapsController(map1, tileStore, TILE_VARIANT_1, mockk())
        PredictiveCache.createMapsController(map1, tileStore, TILE_VARIANT_2, mockk())
        PredictiveCache.createMapsController(map1, tileStore, TILE_VARIANT_3, mockk())

        PredictiveCache.createMapsController(map2, tileStore, TILE_VARIANT_4, mockk())
        PredictiveCache.createMapsController(map2, tileStore, TILE_VARIANT_5, mockk())
        PredictiveCache.createMapsController(map2, tileStore, TILE_VARIANT_6, mockk())
        PredictiveCache.createMapsController(map2, tileStore, TILE_VARIANT_7, mockk())

        PredictiveCache.createNavigationController(navLocationOptions1)
        PredictiveCache.createNavigationController(navLocationOptions2)

        val callback = navigatorRecreationCallbackSlot.captured
        callback.onNativeNavigatorRecreated()

        assertEquals(2, PredictiveCache.cachedMapsPredictiveCacheControllers.size)
        assertEquals(3, PredictiveCache.currentMapsPredictiveCacheControllers(map1).size)
        assertEquals(4, PredictiveCache.currentMapsPredictiveCacheControllers(map2).size)

        assertEquals(2, PredictiveCache.mapsPredictiveCacheLocationOptions.size)
        assertEquals(3, PredictiveCache.mapsPredictiveCacheLocationOptions[map1]?.size)
        assertEquals(4, PredictiveCache.mapsPredictiveCacheLocationOptions[map2]?.size)

        assertEquals(2, PredictiveCache.cachedNavigationPredictiveCacheControllers.size)
        assertEquals(2, PredictiveCache.navPredictiveCacheLocationOptions.size)

        verify(exactly = 2) {
            MapboxNativeNavigatorImpl.createMapsPredictiveCacheController(
                tileStore, TILE_VARIANT_1, any()
            )
        }

        verify(exactly = 2) {
            MapboxNativeNavigatorImpl.createMapsPredictiveCacheController(
                tileStore, TILE_VARIANT_2, any()
            )
        }

        verify(exactly = 2) {
            MapboxNativeNavigatorImpl.createMapsPredictiveCacheController(
                tileStore, TILE_VARIANT_3, any()
            )
        }

        verify(exactly = 2) {
            MapboxNativeNavigatorImpl.createMapsPredictiveCacheController(
                tileStore, TILE_VARIANT_4, any()
            )
        }

        verify(exactly = 2) {
            MapboxNativeNavigatorImpl.createMapsPredictiveCacheController(
                tileStore, TILE_VARIANT_5, any()
            )
        }

        verify(exactly = 2) {
            MapboxNativeNavigatorImpl.createMapsPredictiveCacheController(
                tileStore, TILE_VARIANT_6, any()
            )
        }

        verify(exactly = 2) {
            MapboxNativeNavigatorImpl.createMapsPredictiveCacheController(
                tileStore, TILE_VARIANT_7, any()
            )
        }

        verify(exactly = 2) {
            MapboxNativeNavigatorImpl.createNavigationPredictiveCacheController(navLocationOptions1)
        }

        verify(exactly = 2) {
            MapboxNativeNavigatorImpl.createNavigationPredictiveCacheController(navLocationOptions2)
        }
    }

    @Test
    fun `caches are empty after clean`() {
        PredictiveCache.createMapsController(mockk(), tileStore, TILE_VARIANT_1, mockk())
        PredictiveCache.createNavigationController(mockk())

        PredictiveCache.clean()

        assertEquals(0, PredictiveCache.cachedNavigationPredictiveCacheControllers.size)
        assertEquals(0, PredictiveCache.cachedMapsPredictiveCacheControllers.size)
        assertEquals(0, PredictiveCache.navPredictiveCacheLocationOptions.size)
        assertEquals(0, PredictiveCache.mapsPredictiveCacheLocationOptions.size)
    }

    private companion object {
        private const val TILE_VARIANT_1 = "tile_variant_1"
        private const val TILE_VARIANT_2 = "tile_variant_2"
        private const val TILE_VARIANT_3 = "tile_variant_3"
        private const val TILE_VARIANT_4 = "tile_variant_4"
        private const val TILE_VARIANT_5 = "tile_variant_5"
        private const val TILE_VARIANT_6 = "tile_variant_6"
        private const val TILE_VARIANT_7 = "tile_variant_7"
    }
}
