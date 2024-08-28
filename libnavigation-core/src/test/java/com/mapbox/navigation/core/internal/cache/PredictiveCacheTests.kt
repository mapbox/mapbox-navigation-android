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
import org.junit.Assert.assertNull
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
    fun `size of deprecated map controllers is correct`() {
        val map1 = mockk<Any>()
        val map2 = mockk<Any>()

        predictiveCache.createMapsController(map1, tileStore, TILE_VARIANT_1, mockk())
        predictiveCache.createMapsController(map1, tileStore, TILE_VARIANT_2, mockk())
        predictiveCache.createMapsController(map1, tileStore, TILE_VARIANT_3, mockk())

        predictiveCache.createMapsController(map2, tileStore, TILE_VARIANT_4, mockk())
        predictiveCache.createMapsController(map2, tileStore, TILE_VARIANT_5, mockk())
        predictiveCache.createMapsController(map2, tileStore, TILE_VARIANT_6, mockk())
        predictiveCache.createMapsController(map2, tileStore, TILE_VARIANT_7, mockk())

        assertEquals(3, predictiveCache.currentMapsPredictiveCacheControllers(map1).size)
        assertEquals(4, predictiveCache.currentMapsPredictiveCacheControllers(map2).size)
        assertNull(predictiveCache.cachedMapsPredictiveCacheControllers[map1])
        assertNull(predictiveCache.cachedMapsPredictiveCacheControllers[map2])
    }

    @Test
    fun `size of map controllers is correct`() {
        val map1 = mockk<Any>()
        val map2 = mockk<Any>()
        val options1 = mockk<PredictiveCacheLocationOptions>(relaxed = true)
        val options2 = mockk<PredictiveCacheLocationOptions>(relaxed = true)
        val tilesetDescriptor1 = mockk<TilesetDescriptor>()
        val tilesetDescriptor2 = mockk<TilesetDescriptor>()

        predictiveCache.createMapsControllers(
            map1,
            tileStore,
            listOf(tilesetDescriptor1 to options1, tilesetDescriptor2 to options2)
        )
        predictiveCache.createMapsControllers(
            map2,
            tileStore,
            listOf(tilesetDescriptor2 to options2)
        )

        assertEquals(2, predictiveCache.cachedMapsPredictiveCacheControllers[map1]!!.size)
        assertEquals(1, predictiveCache.cachedMapsPredictiveCacheControllers[map2]!!.size)
        assertEquals(0, predictiveCache.currentMapsPredictiveCacheControllers(map1).size)
        assertEquals(0, predictiveCache.currentMapsPredictiveCacheControllers(map2).size)
        assertNull(predictiveCache.cachedMapsPredictiveCacheControllersTileVariant[map1])
        assertNull(predictiveCache.cachedMapsPredictiveCacheControllersTileVariant[map2])
        assertNull(predictiveCache.mapsPredictiveCacheLocationOptionsTileVariant[map1])
        assertNull(predictiveCache.mapsPredictiveCacheLocationOptionsTileVariant[map2])

        predictiveCache.createMapsControllers(
            map1,
            tileStore,
            listOf(tilesetDescriptor2 to options2)
        )
        predictiveCache.createMapsControllers(
            map2,
            tileStore,
            listOf(tilesetDescriptor1 to options1, tilesetDescriptor2 to options2)
        )

        assertEquals(1, predictiveCache.cachedMapsPredictiveCacheControllers[map1]!!.size)
        assertEquals(2, predictiveCache.cachedMapsPredictiveCacheControllers[map2]!!.size)
        assertEquals(0, predictiveCache.currentMapsPredictiveCacheControllers(map1).size)
        assertEquals(0, predictiveCache.currentMapsPredictiveCacheControllers(map2).size)
        assertNull(predictiveCache.cachedMapsPredictiveCacheControllersTileVariant[map1])
        assertNull(predictiveCache.cachedMapsPredictiveCacheControllersTileVariant[map2])
        assertNull(predictiveCache.mapsPredictiveCacheLocationOptionsTileVariant[map1])
        assertNull(predictiveCache.mapsPredictiveCacheLocationOptionsTileVariant[map2])
    }

    @Test
    fun `size of deprecated map controllers is correct after removing`() {
        val map = mockk<Any>()

        predictiveCache.createMapsController(map, tileStore, TILE_VARIANT_1, mockk())
        predictiveCache.createMapsController(map, tileStore, TILE_VARIANT_2, mockk())
        predictiveCache.createMapsController(map, tileStore, TILE_VARIANT_3, mockk())
        predictiveCache.createMapsController(map, tileStore, TILE_VARIANT_4, mockk())

        predictiveCache.removeMapControllers(map, TILE_VARIANT_2)
        predictiveCache.removeMapControllers(map, TILE_VARIANT_1)
        predictiveCache.removeMapControllers(map, TILE_VARIANT_4)

        assertEquals(1, predictiveCache.currentMapsPredictiveCacheControllers(map).size)
        assertEquals(1, predictiveCache.cachedMapsPredictiveCacheControllersTileVariant[map]?.size)
        assertEquals(TILE_VARIANT_3, predictiveCache.currentMapsPredictiveCacheControllers(map)[0])
    }

    @Test
    fun `deprecated map controllers are empty after removing all`() {
        val map = mockk<Any>()

        predictiveCache.createMapsController(map, tileStore, TILE_VARIANT_1, mockk())
        predictiveCache.createMapsController(map, tileStore, TILE_VARIANT_2, mockk())
        predictiveCache.createMapsController(map, tileStore, TILE_VARIANT_3, mockk())

        predictiveCache.removeAllMapControllersFromTileVariants(map)

        assertEquals(0, predictiveCache.currentMapsPredictiveCacheControllers(map).size)
        assertEquals(null, predictiveCache.mapsPredictiveCacheLocationOptions[map])
        assertEquals(null, predictiveCache.cachedMapsPredictiveCacheControllers[map])
    }

    @Test
    fun `map controllers are empty after removing all`() {
        val map = mockk<Any>()

        val options1 = mockk<PredictiveCacheLocationOptions>(relaxed = true)
        val options2 = mockk<PredictiveCacheLocationOptions>(relaxed = true)
        val tilesetDescriptor1 = mockk<TilesetDescriptor>()
        val tilesetDescriptor2 = mockk<TilesetDescriptor>()

        predictiveCache.createMapsControllers(
            map,
            tileStore,
            listOf(tilesetDescriptor1 to options1, tilesetDescriptor2 to options2)
        )

        predictiveCache.removeAllMapControllersFromDescriptors(map)

        assertEquals(0, predictiveCache.currentMapsPredictiveCacheControllers(map).size)
        assertEquals(null, predictiveCache.mapsPredictiveCacheLocationOptions[map])
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

        val map1 = mockk<Any>()
        val map2 = mockk<Any>()

        predictiveCache.createMapsController(map1, tileStore, TILE_VARIANT_1, mockk())
        predictiveCache.createMapsController(map1, tileStore, TILE_VARIANT_2, mockk())
        predictiveCache.createMapsController(map1, tileStore, TILE_VARIANT_3, mockk())

        predictiveCache.createMapsController(map2, tileStore, TILE_VARIANT_4, mockk())
        predictiveCache.createMapsController(map2, tileStore, TILE_VARIANT_5, mockk())
        predictiveCache.createMapsController(map2, tileStore, TILE_VARIANT_6, mockk())
        predictiveCache.createMapsController(map2, tileStore, TILE_VARIANT_7, mockk())

        predictiveCache.createMapsControllers(
            map1,
            tileStore,
            listOf(
                tilesetDescriptor1 to navLocationOptions1,
                tilesetDescriptor2 to navLocationOptions2
            )
        )
        predictiveCache.createMapsControllers(
            map2,
            tileStore,
            listOf(tilesetDescriptor3 to navLocationOptions3)
        )

        predictiveCache.createNavigationController(navLocationOptions1)
        predictiveCache.createNavigationController(navLocationOptions2)

        val callback = navigatorRecreationCallbackSlot.captured
        callback.onNativeNavigatorRecreated()

        assertEquals(2, predictiveCache.cachedMapsPredictiveCacheControllersTileVariant.size)
        assertEquals(3, predictiveCache.currentMapsPredictiveCacheControllers(map1).size)
        assertEquals(4, predictiveCache.currentMapsPredictiveCacheControllers(map2).size)

        assertEquals(2, predictiveCache.mapsPredictiveCacheLocationOptionsTileVariant.size)
        assertEquals(3, predictiveCache.mapsPredictiveCacheLocationOptionsTileVariant[map1]?.size)
        assertEquals(4, predictiveCache.mapsPredictiveCacheLocationOptionsTileVariant[map2]?.size)

        assertEquals(2, predictiveCache.cachedMapsPredictiveCacheControllers.size)
        assertEquals(2, predictiveCache.cachedMapsPredictiveCacheControllers[map1]!!.size)
        assertEquals(2, predictiveCache.mapsPredictiveCacheLocationOptions.size)
        assertEquals(1, predictiveCache.cachedMapsPredictiveCacheControllers[map2]!!.size)

        assertEquals(2, predictiveCache.cachedNavigationPredictiveCacheControllers.size)
        assertEquals(2, predictiveCache.navPredictiveCacheLocationOptions.size)

        verify(exactly = 2) {
            navigator.createMapsPredictiveCacheControllerTileVariant(
                tileStore,
                TILE_VARIANT_1,
                any()
            )
        }

        verify(exactly = 2) {
            navigator.createMapsPredictiveCacheControllerTileVariant(
                tileStore,
                TILE_VARIANT_2,
                any()
            )
        }

        verify(exactly = 2) {
            navigator.createMapsPredictiveCacheControllerTileVariant(
                tileStore,
                TILE_VARIANT_3,
                any()
            )
        }

        verify(exactly = 2) {
            navigator.createMapsPredictiveCacheControllerTileVariant(
                tileStore,
                TILE_VARIANT_4,
                any()
            )
        }

        verify(exactly = 2) {
            navigator.createMapsPredictiveCacheControllerTileVariant(
                tileStore,
                TILE_VARIANT_5,
                any()
            )
        }

        verify(exactly = 2) {
            navigator.createMapsPredictiveCacheControllerTileVariant(
                tileStore,
                TILE_VARIANT_6,
                any()
            )
        }

        verify(exactly = 2) {
            navigator.createMapsPredictiveCacheControllerTileVariant(
                tileStore,
                TILE_VARIANT_7,
                any()
            )
        }

        verify(exactly = 2) {
            navigator.createMapsPredictiveCacheController(
                tileStore,
                tilesetDescriptor1,
                navLocationOptions1
            )
        }

        verify(exactly = 2) {
            navigator.createMapsPredictiveCacheController(
                tileStore,
                tilesetDescriptor2,
                navLocationOptions2
            )
        }

        verify(exactly = 2) {
            navigator.createMapsPredictiveCacheController(
                tileStore,
                tilesetDescriptor3,
                navLocationOptions3
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
        predictiveCache.createMapsController(mockk(), tileStore, TILE_VARIANT_1, mockk())
        predictiveCache.createNavigationController(mockk())

        predictiveCache.clean()

        assertEquals(0, predictiveCache.cachedNavigationPredictiveCacheControllers.size)
        assertEquals(0, predictiveCache.cachedMapsPredictiveCacheControllers.size)
        assertEquals(0, predictiveCache.cachedMapsPredictiveCacheControllersTileVariant.size)
        assertEquals(0, predictiveCache.navPredictiveCacheLocationOptions.size)
        assertEquals(0, predictiveCache.mapsPredictiveCacheLocationOptions.size)
        assertEquals(0, predictiveCache.mapsPredictiveCacheLocationOptionsTileVariant.size)
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
