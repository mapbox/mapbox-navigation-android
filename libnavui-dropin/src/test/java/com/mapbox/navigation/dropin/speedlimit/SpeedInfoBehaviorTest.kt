package com.mapbox.navigation.dropin.speedlimit

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
class SpeedInfoBehaviorTest {

    @Test
    fun `when speed info is clicked, event is received`() = runBlockingTest {
        val sut = SpeedInfoBehavior()
        val events = arrayListOf<SpeedInfoValue?>()
        val job = sut.speedInfoClickBehavior.onEach { events.add(it) }.launchIn(scope = this)

        val speedInfo = mockk<SpeedInfoValue>()
        sut.onSpeedInfoClicked(speedInfo)
        job.cancelAndJoin()

        assertEquals(listOf(speedInfo), events)
    }
}
