package com.mapbox.navigation.core.navigator

import com.mapbox.navigation.core.RoadGraphDataUpdateCallback
import com.mapbox.navigator.CacheHandleInterface
import com.mapbox.navigator.RoadGraphUpdateAvailabilityCallback
import com.mapbox.navigator.RoadGraphVersionInfo
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
class CacheHandleWrapperTest {

    private val cache = mockk<CacheHandleInterface>(relaxed = true)
    private val callback = mockk<RoadGraphDataUpdateCallback>(relaxed = true)

    @Test
    fun `requestRoadGraphDataUpdate invokes callback with filled data`() {
        val expectedIsUpdateAvailable = Random.nextBoolean()
        val expectedDataset = "my dataset"
        val expectedVersion = "my version"
        val versionInfo = RoadGraphVersionInfo(expectedDataset, expectedVersion)

        CacheHandleWrapper.requestRoadGraphDataUpdate(cache, callback)

        val nativeCallbacks = mutableListOf<RoadGraphUpdateAvailabilityCallback>()
        verify(exactly = 1) {
            cache.isRoadGraphDataUpdateAvailable(capture(nativeCallbacks))
        }
        nativeCallbacks[0].run(expectedIsUpdateAvailable, versionInfo)
        verify(exactly = 1) {
            callback.onRoadGraphDataUpdateInfoAvailable(
                expectedIsUpdateAvailable,
                com.mapbox.navigation.core.RoadGraphVersionInfo(expectedDataset, expectedVersion),
            )
        }
    }

    @Test
    fun `requestRoadGraphDataUpdate invokes callback with null version info`() {
        val expectedIsUpdateAvailable = Random.nextBoolean()

        CacheHandleWrapper.requestRoadGraphDataUpdate(cache, callback)

        val nativeCallbacks = mutableListOf<RoadGraphUpdateAvailabilityCallback>()
        verify(exactly = 1) {
            cache.isRoadGraphDataUpdateAvailable(capture(nativeCallbacks))
        }
        nativeCallbacks[0].run(expectedIsUpdateAvailable, null)
        verify(exactly = 1) {
            callback.onRoadGraphDataUpdateInfoAvailable(expectedIsUpdateAvailable, null)
        }
    }
}
