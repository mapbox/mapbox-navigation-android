package com.mapbox.navigation.ui.maps.internal.extensions

import com.mapbox.bindgen.Expected
import com.mapbox.bindgen.ExpectedFactory.createValue
import com.mapbox.geojson.Point
import com.mapbox.navigation.testing.MainCoroutineRule
import com.mapbox.navigation.ui.base.util.MapboxNavigationConsumer
import com.mapbox.navigation.ui.maps.building.api.MapboxBuildingsApi
import com.mapbox.navigation.ui.maps.building.model.BuildingError
import com.mapbox.navigation.ui.maps.building.model.BuildingValue
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapboxBuildingsApiExtensionsKtTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @Test
    fun queryBuildingToHighlightCoroutine() = coroutineRule.runBlockingTest {
        val point = Point.fromLngLat(1.0, 2.0)
        val buildingValue = mockk<BuildingValue>()
        val api = mockk<MapboxBuildingsApi>(relaxed = true)
        val callback = slot<MapboxNavigationConsumer<Expected<BuildingError, BuildingValue>>>()
        every {
            api.queryBuildingToHighlight(point, capture(callback))
        } answers {
            callback.captured.accept(createValue(buildingValue))
        }

        val result = api.queryBuildingToHighlight(point)

        assertEquals(buildingValue, result)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `queryBuildingToHighlightCoroutine should cancel the API on coroutine cancellation`() =
        coroutineRule.runBlockingTest {
            val point = Point.fromLngLat(1.0, 2.0)
            val buildingValue = mockk<BuildingValue>()
            val api = mockk<MapboxBuildingsApi>(relaxed = true)
            val callback = slot<MapboxNavigationConsumer<Expected<BuildingError, BuildingValue>>>()
            every {
                api.queryBuildingToHighlight(point, capture(callback))
            } returns Unit

            val job = launch {
                api.queryBuildingToHighlight(point) // waits until captured callback is called
            }
            job.cancelAndJoin()
            // triggering callback after coroutine was cancelled
            callback.captured.accept(createValue(buildingValue))

            verify { api.cancel() }
        }
}
