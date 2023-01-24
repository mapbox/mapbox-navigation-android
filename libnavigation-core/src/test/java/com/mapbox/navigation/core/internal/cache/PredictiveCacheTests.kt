package com.mapbox.navigation.core.internal.cache

import com.mapbox.common.TileStore
import com.mapbox.common.TilesetDescriptor
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
import org.junit.Assert.assertNull
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
            MapboxNativeNavigatorImpl.createMapsPredictiveCacheControllerTileVariant(
                any(),
                any(),
                any()
            )
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
    fun `size of deprecated map controllers is correct`() {
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
        assertNull(PredictiveCache.cachedMapsPredictiveCacheControllers[map1])
        assertNull(PredictiveCache.cachedMapsPredictiveCacheControllers[map2])
    }

    @Test
    fun `size of map controllers is correct`() {
        val map1 = mockk<Any>()
        val map2 = mockk<Any>()
        val options1 = mockk<PredictiveCacheLocationOptions>(relaxed = true)
        val options2 = mockk<PredictiveCacheLocationOptions>(relaxed = true)
        val tilesetDescriptor1 = mockk<TilesetDescriptor>()
        val tilesetDescriptor2 = mockk<TilesetDescriptor>()

        PredictiveCache.createMapsControllers(
            map1,
            tileStore,
            listOf(tilesetDescriptor1 to options1, tilesetDescriptor2 to options2)
        )
        PredictiveCache.createMapsControllers(
            map2,
            tileStore,
            listOf(tilesetDescriptor2 to options2)
        )

        assertEquals(2, PredictiveCache.cachedMapsPredictiveCacheControllers[map1]!!.size)
        assertEquals(1, PredictiveCache.cachedMapsPredictiveCacheControllers[map2]!!.size)
        assertEquals(0, PredictiveCache.currentMapsPredictiveCacheControllers(map1).size)
        assertEquals(0, PredictiveCache.currentMapsPredictiveCacheControllers(map2).size)
        assertNull(PredictiveCache.cachedMapsPredictiveCacheControllersTileVariant[map1])
        assertNull(PredictiveCache.cachedMapsPredictiveCacheControllersTileVariant[map2])
        assertNull(PredictiveCache.mapsPredictiveCacheLocationOptionsTileVariant[map1])
        assertNull(PredictiveCache.mapsPredictiveCacheLocationOptionsTileVariant[map2])

        PredictiveCache.createMapsControllers(
            map1,
            tileStore,
            listOf(tilesetDescriptor2 to options2)
        )
        PredictiveCache.createMapsControllers(
            map2,
            tileStore,
            listOf(tilesetDescriptor1 to options1, tilesetDescriptor2 to options2)
        )

        assertEquals(1, PredictiveCache.cachedMapsPredictiveCacheControllers[map1]!!.size)
        assertEquals(2, PredictiveCache.cachedMapsPredictiveCacheControllers[map2]!!.size)
        assertEquals(0, PredictiveCache.currentMapsPredictiveCacheControllers(map1).size)
        assertEquals(0, PredictiveCache.currentMapsPredictiveCacheControllers(map2).size)
        assertNull(PredictiveCache.cachedMapsPredictiveCacheControllersTileVariant[map1])
        assertNull(PredictiveCache.cachedMapsPredictiveCacheControllersTileVariant[map2])
        assertNull(PredictiveCache.mapsPredictiveCacheLocationOptionsTileVariant[map1])
        assertNull(PredictiveCache.mapsPredictiveCacheLocationOptionsTileVariant[map2])
    }

    @Test
    fun `size of deprecated map controllers is correct after removing`() {
        val map = mockk<Any>()

        PredictiveCache.createMapsController(map, tileStore, TILE_VARIANT_1, mockk())
        PredictiveCache.createMapsController(map, tileStore, TILE_VARIANT_2, mockk())
        PredictiveCache.createMapsController(map, tileStore, TILE_VARIANT_3, mockk())
        PredictiveCache.createMapsController(map, tileStore, TILE_VARIANT_4, mockk())

        PredictiveCache.removeMapControllers(map, TILE_VARIANT_2)
        PredictiveCache.removeMapControllers(map, TILE_VARIANT_1)
        PredictiveCache.removeMapControllers(map, TILE_VARIANT_4)

        assertEquals(1, PredictiveCache.currentMapsPredictiveCacheControllers(map).size)
        assertEquals(1, PredictiveCache.cachedMapsPredictiveCacheControllersTileVariant[map]?.size)
        assertEquals(TILE_VARIANT_3, PredictiveCache.currentMapsPredictiveCacheControllers(map)[0])
    }

    @Test
    fun `deprecated map controllers are empty after removing all`() {
        val map = mockk<Any>()

        PredictiveCache.createMapsController(map, tileStore, TILE_VARIANT_1, mockk())
        PredictiveCache.createMapsController(map, tileStore, TILE_VARIANT_2, mockk())
        PredictiveCache.createMapsController(map, tileStore, TILE_VARIANT_3, mockk())

        PredictiveCache.removeAllMapControllersFromTileVariants(map)

        assertEquals(0, PredictiveCache.currentMapsPredictiveCacheControllers(map).size)
        assertEquals(null, PredictiveCache.mapsPredictiveCacheLocationOptions[map])
        assertEquals(null, PredictiveCache.cachedMapsPredictiveCacheControllers[map])
    }

    @Test
    fun `map controllers are empty after removing all`() {
        val map = mockk<Any>()

        val options1 = mockk<PredictiveCacheLocationOptions>(relaxed = true)
        val options2 = mockk<PredictiveCacheLocationOptions>(relaxed = true)
        val tilesetDescriptor1 = mockk<TilesetDescriptor>()
        val tilesetDescriptor2 = mockk<TilesetDescriptor>()

        PredictiveCache.createMapsControllers(
            map,
            tileStore,
            listOf(tilesetDescriptor1 to options1, tilesetDescriptor2 to options2)
        )

        PredictiveCache.removeAllMapControllersFromDescriptors(map)

        assertEquals(0, PredictiveCache.currentMapsPredictiveCacheControllers(map).size)
        assertEquals(null, PredictiveCache.mapsPredictiveCacheLocationOptions[map])
        assertEquals(null, PredictiveCache.cachedMapsPredictiveCacheControllers[map])
    }

    @Test
    fun `controllers are recreated when navigator is recreated`() {
        val navLocationOptions1: PredictiveCacheLocationOptions = mockk()
        val navLocationOptions2: PredictiveCacheLocationOptions = mockk()
        val navLocationOptions3: PredictiveCacheLocationOptions = mockk()
        val tilesetDescriptor1 = mockk<TilesetDescriptor>()
        val tilesetDescriptor2 = mockk<TilesetDescriptor>()
        val tilesetDescriptor3 = mockk<TilesetDescriptor>()

        every {
            MapboxNativeNavigatorImpl.createNavigationPredictiveCacheController(navLocationOptions1)
        } returns mockk()

        every {
            MapboxNativeNavigatorImpl.createNavigationPredictiveCacheController(navLocationOptions2)
        } returns mockk()

        every {
            MapboxNativeNavigatorImpl.createNavigationPredictiveCacheController(navLocationOptions3)
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

        PredictiveCache.createMapsControllers(
            map1,
            tileStore,
            listOf(
                tilesetDescriptor1 to navLocationOptions1,
                tilesetDescriptor2 to navLocationOptions2
            )
        )
        PredictiveCache.createMapsControllers(
            map2,
            tileStore,
            listOf(tilesetDescriptor3 to navLocationOptions3)
        )

        PredictiveCache.createNavigationController(navLocationOptions1)
        PredictiveCache.createNavigationController(navLocationOptions2)

        val callback = navigatorRecreationCallbackSlot.captured
        callback.onNativeNavigatorRecreated()

        assertEquals(2, PredictiveCache.cachedMapsPredictiveCacheControllersTileVariant.size)
        assertEquals(3, PredictiveCache.currentMapsPredictiveCacheControllers(map1).size)
        assertEquals(4, PredictiveCache.currentMapsPredictiveCacheControllers(map2).size)

        assertEquals(2, PredictiveCache.mapsPredictiveCacheLocationOptionsTileVariant.size)
        assertEquals(3, PredictiveCache.mapsPredictiveCacheLocationOptionsTileVariant[map1]?.size)
        assertEquals(4, PredictiveCache.mapsPredictiveCacheLocationOptionsTileVariant[map2]?.size)

        assertEquals(2, PredictiveCache.cachedMapsPredictiveCacheControllers.size)
        assertEquals(2, PredictiveCache.cachedMapsPredictiveCacheControllers[map1]!!.size)
        assertEquals(2, PredictiveCache.mapsPredictiveCacheLocationOptions.size)
        assertEquals(1, PredictiveCache.cachedMapsPredictiveCacheControllers[map2]!!.size)

        assertEquals(2, PredictiveCache.cachedNavigationPredictiveCacheControllers.size)
        assertEquals(2, PredictiveCache.navPredictiveCacheLocationOptions.size)

        verify(exactly = 2) {
            MapboxNativeNavigatorImpl.createMapsPredictiveCacheControllerTileVariant(
                tileStore,
                TILE_VARIANT_1,
                any()
            )
        }

        verify(exactly = 2) {
            MapboxNativeNavigatorImpl.createMapsPredictiveCacheControllerTileVariant(
                tileStore,
                TILE_VARIANT_2,
                any()
            )
        }

        verify(exactly = 2) {
            MapboxNativeNavigatorImpl.createMapsPredictiveCacheControllerTileVariant(
                tileStore,
                TILE_VARIANT_3,
                any()
            )
        }

        verify(exactly = 2) {
            MapboxNativeNavigatorImpl.createMapsPredictiveCacheControllerTileVariant(
                tileStore,
                TILE_VARIANT_4,
                any()
            )
        }

        verify(exactly = 2) {
            MapboxNativeNavigatorImpl.createMapsPredictiveCacheControllerTileVariant(
                tileStore,
                TILE_VARIANT_5,
                any()
            )
        }

        verify(exactly = 2) {
            MapboxNativeNavigatorImpl.createMapsPredictiveCacheControllerTileVariant(
                tileStore,
                TILE_VARIANT_6,
                any()
            )
        }

        verify(exactly = 2) {
            MapboxNativeNavigatorImpl.createMapsPredictiveCacheControllerTileVariant(
                tileStore,
                TILE_VARIANT_7,
                any()
            )
        }

        verify(exactly = 2) {
            MapboxNativeNavigatorImpl.createMapsPredictiveCacheController(
                tileStore,
                tilesetDescriptor1,
                navLocationOptions1
            )
        }

        verify(exactly = 2) {
            MapboxNativeNavigatorImpl.createMapsPredictiveCacheController(
                tileStore,
                tilesetDescriptor2,
                navLocationOptions2
            )
        }

        verify(exactly = 2) {
            MapboxNativeNavigatorImpl.createMapsPredictiveCacheController(
                tileStore,
                tilesetDescriptor3,
                navLocationOptions3
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
        assertEquals(0, PredictiveCache.cachedMapsPredictiveCacheControllersTileVariant.size)
        assertEquals(0, PredictiveCache.navPredictiveCacheLocationOptions.size)
        assertEquals(0, PredictiveCache.mapsPredictiveCacheLocationOptions.size)
        assertEquals(0, PredictiveCache.mapsPredictiveCacheLocationOptionsTileVariant.size)
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
