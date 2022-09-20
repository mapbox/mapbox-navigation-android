package com.mapbox.navigation.dropin.binder.map

import com.mapbox.geojson.Point
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Test

@ExperimentalCoroutinesApi
class MapClickBehaviorTest {

    @Test
    fun `when map is clicked, event is received`() = runBlockingTest {
        val sut = MapClickBehavior()
        val events = arrayListOf<Point>()
        val job = sut.mapClickBehavior.onEach { events.add(it) }.launchIn(scope = this)

        val point = mockk<Point>()
        sut.onMapClicked(point)
        job.cancelAndJoin()

        assertEquals(listOf(point), events)
    }
}
