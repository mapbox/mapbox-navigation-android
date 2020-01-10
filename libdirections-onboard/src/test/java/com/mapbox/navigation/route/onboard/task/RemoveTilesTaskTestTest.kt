package com.mapbox.navigation.route.onboard.task

import com.mapbox.geojson.Point
import com.mapbox.navigation.route.onboard.OnOfflineTilesRemovedCallback
import com.mapbox.navigator.Navigator
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class RemoveTilesTaskTestTest {

    @Test
    fun checksOnRemoveIsCalledWhenTilesAreRemoved() {
        val mockedNavigator = mockk<Navigator>()
        val aTilePath = "/some/path/version"
        val southwest = Point.fromLngLat(1.0, 2.0)
        val northeast = Point.fromLngLat(3.0, 4.0)
        val mockedCallback = mockk<OnOfflineTilesRemovedCallback>(relaxed = true)
        val theRemoveTilesTask = RemoveTilesTask(
            mockedNavigator, aTilePath, southwest,
            northeast, mockedCallback
        )

        theRemoveTilesTask.onPostExecute(9L)

        verify { mockedCallback.onRemoved(eq(9L)) }
    }
}