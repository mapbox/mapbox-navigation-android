package com.mapbox.navigation.dropin

import com.mapbox.geojson.Point
import com.mapbox.navigation.ui.speedlimit.model.SpeedInfoValue
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Test

@ExperimentalCoroutinesApi
class ClickBehaviorTest {

    @Test
    fun `when speed info is clicked, event is received`() = runBlockingTest {
        val sut = ClickBehavior<SpeedInfoValue?>()
        val events = arrayListOf<SpeedInfoValue?>()
        val job = sut.onViewClicked.onEach { events.add(it) }.launchIn(scope = this)

        val speedInfo = mockk<SpeedInfoValue>()
        sut.onClicked(speedInfo)
        job.cancelAndJoin()

        assertEquals(listOf(speedInfo), events)
    }

    @Test
    fun `when map is clicked, event is received`() = runBlockingTest {
        val sut = ClickBehavior<Point>()
        val events = arrayListOf<Point>()
        val job = sut.onViewClicked.onEach { events.add(it) }.launchIn(scope = this)

        val point = mockk<Point>()
        sut.onClicked(point)
        job.cancelAndJoin()

        assertEquals(listOf(point), events)
    }
}
