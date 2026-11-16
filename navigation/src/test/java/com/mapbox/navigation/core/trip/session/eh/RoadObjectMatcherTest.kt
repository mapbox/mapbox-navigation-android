package com.mapbox.navigation.core.trip.session.eh

import com.mapbox.navigation.navigator.internal.MapboxNativeNavigator
import com.mapbox.navigation.navigator.internal.TilesVersionChangedObserver
import com.mapbox.navigator.RoadGraphVersionInfo
import com.mapbox.navigator.RoadObjectMatcherInterface
import com.mapbox.navigator.TilesVersionChangeReason
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoadObjectMatcherTest {

    private val nativeMatcherBeforeSwap = mockk<RoadObjectMatcherInterface>(relaxed = true)
    private val nativeMatcherAfterSwap = mockk<RoadObjectMatcherInterface>(relaxed = true)
    private val tilesVersionChangedObserverSlot = slot<TilesVersionChangedObserver>()
    private val navigator = mockk<MapboxNativeNavigator>(relaxed = true) {
        every { roadObjectMatcher } returns nativeMatcherBeforeSwap
        every {
            addTilesVersionChangedObserver(capture(tilesVersionChangedObserverSlot))
        } returns Unit
    }

    private lateinit var roadObjectMatcher: RoadObjectMatcher

    @Before
    fun setUp() {
        roadObjectMatcher = RoadObjectMatcher(navigator)
    }

    @Test
    fun `listener is attached to the new native matcher after tiles version change`() {
        roadObjectMatcher.registerRoadObjectMatcherObserver(mockk(relaxed = true))

        every { navigator.roadObjectMatcher } returns nativeMatcherAfterSwap
        notifyTilesVersionChanged()

        verify(exactly = 1) { nativeMatcherAfterSwap.setListener(any()) }
    }

    @Test
    fun `listener is not attached after tiles version change when there are no observers`() {
        every { navigator.roadObjectMatcher } returns nativeMatcherAfterSwap
        notifyTilesVersionChanged()

        verify(exactly = 0) { nativeMatcherAfterSwap.setListener(any()) }
    }

    private fun notifyTilesVersionChanged() {
        tilesVersionChangedObserverSlot.captured.onTilesVersionChanged(
            RoadGraphVersionInfo("mapbox/driving-traffic", "2025_08_31-06_57_13"),
            TilesVersionChangeReason.FALLBACK,
        )
    }
}
