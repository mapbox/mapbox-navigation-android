package com.mapbox.navigation.core

import com.mapbox.navigation.base.internal.clearCache
import com.mapbox.navigation.base.utils.DecodeUtils
import com.mapbox.navigation.core.internal.LowMemoryManager
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowReachabilityFactory::class])
internal class MapboxNavigationCachesClearingTest : MapboxNavigationBaseTest() {

    private val lowMemoryObserverSlot = slot<LowMemoryManager.Observer>()

    @Before
    override fun setUp() {
        super.setUp()
        every { lowMemoryManager.addObserver(capture(lowMemoryObserverSlot)) } returns Unit
    }

    @Test
    fun `start memory monitoring on creation`() {
        createMapboxNavigation()

        verify(exactly = 1) {
            lowMemoryManager.addObserver(any())
        }
    }

    @Test
    fun `stop memory monitoring on destroy`() {
        createMapboxNavigation()
        mapboxNavigation.onDestroy()

        verify(exactly = 1) {
            lowMemoryManager.removeObserver(any())
        }
    }

    @Test
    fun `clear DecodeUtils cache on onLowMemory event`() {
        createMapboxNavigation()

        lowMemoryObserverSlot.captured.onLowMemory()

        verify(exactly = 1) {
            DecodeUtils.clearCache()
        }
    }

    @Test
    fun `clear DecodeUtils caches on destroy`() {
        createMapboxNavigation()
        mapboxNavigation.onDestroy()

        verify(exactly = 1) {
            DecodeUtils.clearCache()
        }
    }
}
