package com.mapbox.navigation.core.replay.history

import android.os.SystemClock
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import java.util.concurrent.TimeUnit
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class ReplayHistoryPlayerTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val lifecycleOwner: LifecycleOwner = mockk {
        every { lifecycle } returns mockk {
            every { currentState } returns Lifecycle.State.STARTED
        }
    }
    private val mockLambda: (ReplayEvents) -> Unit = mockk(relaxed = true)

    private var deviceElapsedTimeNanos = TimeUnit.HOURS.toNanos(11)

    @Test
    fun `should play start transit and location in order`() = coroutineRule.runBlockingTest {
        val replayHistoryData = ReplayEvents(
            listOf(
                ReplayEventGetStatus(1580777612.853),
                ReplayEventUpdateLocation(1580777612.89,
                    ReplayEventLocation(
                        lat = 49.2492411,
                        lon = 8.8512315,
                        provider = "fused",
                        time = 1580777612.892,
                        altitude = 212.4732666015625,
                        accuracyHorizontal = 4.288000106811523,
                        bearing = 243.31265258789063,
                        speed = 0.5585000514984131))
            )
        )
        val replayHistoryPlayer = ReplayHistoryPlayer(replayHistoryData)
        replayHistoryPlayer.observeReplayEvents(mockLambda)

        val job = replayHistoryPlayer.play(lifecycleOwner)
        coAdvanceTimeSeconds(5)
        replayHistoryPlayer.finish()
        job.cancelAndJoin()

        val replayUpdates = mutableListOf<ReplayEvents>()
        coVerify { mockLambda(capture(replayUpdates)) }
        val events = replayUpdates.flatMap { it.events }
        assertEquals(events.size, 2)
        assertEquals(events[0].eventTimestamp, 1580777612.853)
        assertEquals(events[1].eventTimestamp, 1580777612.89)
    }

    @Test
    fun `should play 2 of 3 locations that include time window`() = coroutineRule.runBlockingTest {
        val replayHistoryData = ReplayEvents(
            listOf(
                ReplayEventUpdateLocation(1580777820.952,
                    ReplayEventLocation(
                        lat = 49.2450478,
                        lon = 8.8682922,
                        time = 1580777820.952,
                        speed = 30.239412307739259,
                        bearing = 108.00135040283203,
                        altitude = 222.47210693359376,
                        accuracyHorizontal = 3.9000000953674318,
                        provider = "fused")),
                ReplayEventUpdateLocation(1580777822.959,
                    ReplayEventLocation(
                        lat = 49.2448858,
                        lon = 8.8690847,
                        time = 1580777822.958,
                        speed = 29.931121826171876,
                        bearing = 106.001953125,
                        altitude = 221.9241943359375,
                        accuracyHorizontal = 3.9000000953674318,
                        provider = "fused")),
                ReplayEventUpdateLocation(1580777824.953,
                    ReplayEventLocation(
                        lat = 49.2447354,
                        lon = 8.8698759,
                        time = 1580777824.89,
                        speed = 29.96711540222168,
                        bearing = 106.00138092041016,
                        altitude = 221.253662109375,
                        accuracyHorizontal = 3.9000000953674318,
                        provider = "fused"))
            )
        )
        val replayHistoryPlayer = ReplayHistoryPlayer(replayHistoryData)
        replayHistoryPlayer.observeReplayEvents(mockLambda)

        val job = replayHistoryPlayer.play(lifecycleOwner)
        coAdvanceTimeSeconds(3)
        replayHistoryPlayer.finish()
        job.cancelAndJoin()

        // Note that it only played 2 of the 3 locations
        val replayUpdates = mutableListOf<ReplayEvents>()
        coVerify { mockLambda(capture(replayUpdates)) }
        val events = replayUpdates.flatMap { it.events }
        assertEquals(events.size, 2)
        assertEquals(events[0].eventTimestamp, 1580777820.952)
        assertEquals(events[1].eventTimestamp, 1580777822.959)
    }

    @Test
    fun `should allow custom events`() = coroutineRule.runBlockingTest {
        data class CustomReplayEvent(
            override val eventTimestamp: Double,
            val customValue: String
        ) : ReplayEventBase
        val replayHistoryData = ReplayEvents(
            listOf(
                CustomReplayEvent(1580777612.853, "custom value"),
                ReplayEventUpdateLocation(1580777613.89,
                    ReplayEventLocation(
                        lat = 49.2492411,
                        lon = 8.8512315,
                        time = null,
                        provider = null,
                        altitude = null,
                        accuracyHorizontal = null,
                        bearing = null,
                        speed = null))
            )
        )
        val replayHistoryPlayer = ReplayHistoryPlayer(replayHistoryData)
        replayHistoryPlayer.observeReplayEvents(mockLambda)

        val job = replayHistoryPlayer.play(lifecycleOwner)
        coAdvanceTimeSeconds(5)
        replayHistoryPlayer.finish()
        job.cancelAndJoin()

        val replayUpdates = mutableListOf<ReplayEvents>()
        coVerify { mockLambda(capture(replayUpdates)) }
        val events = replayUpdates.flatMap { it.events }
        assertEquals(events.size, 2)
        assertTrue(events[0] is CustomReplayEvent)
        assertEquals((events[0] as CustomReplayEvent).customValue, "custom value")
        assertEquals(events[1].eventTimestamp, 1580777613.89)
    }

    @Test(expected = Exception::class)
    fun `should crash if history data is empty`() {
        val replayHistoryData = ReplayEvents(
            listOf())
        val replayHistoryPlayer = ReplayHistoryPlayer(replayHistoryData)

        replayHistoryPlayer.play(lifecycleOwner)
        replayHistoryPlayer.finish()
    }

    /**
     * Helpers for moving the simulation clock
     */

    @Before
    fun setup() {
        mockkStatic(SystemClock::class)
        every { SystemClock.elapsedRealtimeNanos() } returns deviceElapsedTimeNanos
    }

    @After
    fun teardown() {
        unmockkObject(SystemClock.elapsedRealtimeNanos())
    }

    private suspend fun coAdvanceTimeSeconds(seconds: Int) {
        val advanceSeconds = seconds.toLong()
        deviceElapsedTimeNanos += TimeUnit.SECONDS.toNanos(advanceSeconds)
        every { SystemClock.elapsedRealtimeNanos() } returns deviceElapsedTimeNanos
        coroutineRule.testDispatcher.advanceTimeBy(TimeUnit.SECONDS.toMillis(advanceSeconds))
        delay(TimeUnit.SECONDS.toMillis(advanceSeconds))
    }
}
