package com.mapbox.navigation.core.navigator

import com.mapbox.navigation.core.RoadGraphDataUpdateCallback
import com.mapbox.navigator.RoadGraphVersionInfo
import com.mapbox.navigator.TilesManagerHandleInterface
import com.mapbox.navigator.TilesUpdateAvailabilityCallback
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
class TilesManagerHandleWrapperTest {

    private val tilesManagerHandle = mockk<TilesManagerHandleInterface>(relaxed = true)
    private val callback = mockk<RoadGraphDataUpdateCallback>(relaxed = true)

    @Test
    fun `requestRoadGraphDataUpdate invokes callback with filled data`() {
        val expectedIsUpdateAvailable = Random.nextBoolean()
        val expectedDataset = "my dataset"
        val expectedVersion = "my version"
        val versionInfo = RoadGraphVersionInfo(expectedDataset, expectedVersion)

        TilesManagerHandleWrapper.requestRoadGraphDataUpdate(tilesManagerHandle, callback)

        val nativeCallbacks = mutableListOf<TilesUpdateAvailabilityCallback>()
        verify(exactly = 1) {
            tilesManagerHandle.checkUpdateAvailable(capture(nativeCallbacks))
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

        TilesManagerHandleWrapper.requestRoadGraphDataUpdate(tilesManagerHandle, callback)

        val nativeCallbacks = mutableListOf<TilesUpdateAvailabilityCallback>()
        verify(exactly = 1) {
            tilesManagerHandle.checkUpdateAvailable(capture(nativeCallbacks))
        }
        nativeCallbacks[0].run(expectedIsUpdateAvailable, null)
        verify(exactly = 1) {
            callback.onRoadGraphDataUpdateInfoAvailable(expectedIsUpdateAvailable, null)
        }
    }

    @Test
    fun `requestHDGraphDataUpdate invokes callback with filled data`() {
        val expectedIsUpdateAvailable = Random.nextBoolean()
        val expectedDataset = "my dataset"
        val expectedVersion = "my version"
        val versionInfo = RoadGraphVersionInfo(expectedDataset, expectedVersion)

        TilesManagerHandleWrapper.requestHDGraphDataUpdate(tilesManagerHandle, callback)

        val nativeCallbacks = mutableListOf<TilesUpdateAvailabilityCallback>()
        verify(exactly = 1) {
            tilesManagerHandle.checkHDUpdateAvailable(capture(nativeCallbacks))
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
    fun `requestHDGraphDataUpdate invokes callback with null version info`() {
        val expectedIsUpdateAvailable = Random.nextBoolean()

        TilesManagerHandleWrapper.requestHDGraphDataUpdate(tilesManagerHandle, callback)

        val nativeCallbacks = mutableListOf<TilesUpdateAvailabilityCallback>()
        verify(exactly = 1) {
            tilesManagerHandle.checkHDUpdateAvailable(capture(nativeCallbacks))
        }
        nativeCallbacks[0].run(expectedIsUpdateAvailable, null)
        verify(exactly = 1) {
            callback.onRoadGraphDataUpdateInfoAvailable(expectedIsUpdateAvailable, null)
        }
    }
}
