package com.mapbox.navigation.route.onboard.task

import com.mapbox.geojson.Point
import com.mapbox.navigation.navigator.MapboxNativeNavigator
import com.mapbox.navigation.route.onboard.OnOfflineTilesRemovedCallback
import com.mapbox.navigation.utils.tests.MainCoroutineRule
import com.mapbox.navigation.utils.tests.runBlockingTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class RemoveTilesTaskTest {

    private val navigator = mockk<MapboxNativeNavigator>(relaxed = true)
    private val callback = mockk<OnOfflineTilesRemovedCallback>(relaxed = true)
    private val tilePath = "/some/path/version"
    private val southwest = Point.fromLngLat(1.0, 2.0)
    private val northeast = Point.fromLngLat(3.0, 4.0)
    private val theRemoveTilesTask = RemoveTilesTask(
        navigator, tilePath, southwest, northeast, callback
    )

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @Test
    fun checksOnRemoveIsCalledWhenTilesAreRemoved() = coroutineRule.runBlockingTest {
        every { navigator.removeTiles(tilePath, southwest, northeast) } returns 9L

        theRemoveTilesTask.launch()

        verify { callback.onRemoved(9L) }
    }
}
