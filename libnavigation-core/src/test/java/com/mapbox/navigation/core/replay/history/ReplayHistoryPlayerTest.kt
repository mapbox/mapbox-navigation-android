package com.mapbox.navigation.core.replay.history

import android.os.SystemClock
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.Called
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.verify
import java.util.concurrent.TimeUnit
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
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
    private val mockLambda: (List<ReplayEventBase>) -> Unit = mockk(relaxed = true)

    private var deviceElapsedTimeNanos = TimeUnit.HOURS.toNanos(11)

    @Test
    fun `should play start transit and location in order`() = coroutineRule.runBlockingTest {
        val replayHistoryPlayer = ReplayHistoryPlayer()
            .pushEvents(listOf(
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
                        speed = 0.5585000514984131)
                )
            ))
        replayHistoryPlayer.observeReplayEvents(mockLambda)

        val job = replayHistoryPlayer.play(lifecycleOwner)
        advanceTimeSeconds(5)
        replayHistoryPlayer.finish()
        job.cancelAndJoin()

        val replayUpdates = mutableListOf<List<ReplayEventBase>>()
        coVerify { mockLambda(capture(replayUpdates)) }
        val events = replayUpdates.flatten()
        assertEquals(2, events.size)
        assertEquals(1580777612.853, events[0].eventTimestamp)
        assertEquals(1580777612.89, events[1].eventTimestamp)
    }

    @Test
    fun `should play 2 of 3 locations that include time window`() = coroutineRule.runBlockingTest {
        val replayHistoryPlayer = ReplayHistoryPlayer()
            .pushEvents(listOf(
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
            ))
        replayHistoryPlayer.observeReplayEvents(mockLambda)

        val job = replayHistoryPlayer.play(lifecycleOwner)
        advanceTimeSeconds(3)
        replayHistoryPlayer.finish()
        job.cancelAndJoin()

        // Note that it only played 2 of the 3 locations
        val replayUpdates = mutableListOf<List<ReplayEventBase>>()
        coVerify { mockLambda(capture(replayUpdates)) }
        val events = replayUpdates.flatten()
        assertEquals(2, events.size)
        assertEquals(1580777820.952, events[0].eventTimestamp)
        assertEquals(1580777822.959, events[1].eventTimestamp)
    }

    @Test
    fun `should allow custom events`() = coroutineRule.runBlockingTest {
        data class CustomReplayEvent(
            override val eventTimestamp: Double,
            val customValue: String
        ) : ReplayEventBase
        val replayHistoryPlayer = ReplayHistoryPlayer()
            .pushEvents(listOf(
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
            ))
        replayHistoryPlayer.observeReplayEvents(mockLambda)

        val job = replayHistoryPlayer.play(lifecycleOwner)
        advanceTimeSeconds(5)
        replayHistoryPlayer.finish()
        job.cancelAndJoin()

        val replayUpdates = mutableListOf<List<ReplayEventBase>>()
        coVerify { mockLambda(capture(replayUpdates)) }
        val events = replayUpdates.flatten()
        assertEquals(events.size, 2)
        assertTrue(events[0] is CustomReplayEvent)
        assertEquals("custom value", (events[0] as CustomReplayEvent).customValue)
        assertEquals(1580777613.89, events[1].eventTimestamp)
    }

    @Test(expected = Exception::class)
    fun `should crash if history data is empty`() {
        val replayHistoryPlayer = ReplayHistoryPlayer()

        replayHistoryPlayer.play(lifecycleOwner)
        replayHistoryPlayer.finish()
    }

    @Test
    fun `playFirstLocation should ignore events before the first location`() {
        val replayHistoryPlayer = ReplayHistoryPlayer()
            .pushEvents(listOf(
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
            ))
        replayHistoryPlayer.observeReplayEvents(mockLambda)

        replayHistoryPlayer.playFirstLocation()

        val replayUpdates = mutableListOf<List<ReplayEventBase>>()
        verify { mockLambda(capture(replayUpdates)) }
        val events = replayUpdates.flatten()
        assertEquals(1, events.size)
        assertEquals(1580777612.89, events[0].eventTimestamp)
    }

    @Test
    fun `playFirstLocation should handle history events without locations`() {
        val replayHistoryPlayer = ReplayHistoryPlayer()
            .pushEvents(listOf(
                ReplayEventGetStatus(1580777612.853),
                ReplayEventGetStatus(1580777613.452),
                ReplayEventGetStatus(1580777614.085)
            ))
        replayHistoryPlayer.observeReplayEvents(mockLambda)

        replayHistoryPlayer.playFirstLocation()

        verify { mockLambda(any()) wasNot Called }
    }

    @Test
    fun `should seekTo an event`() = coroutineRule.runBlockingTest {
        val seekToEvent = ReplayEventGetStatus(2.452)
        val replayHistoryPlayer = ReplayHistoryPlayer()
            .pushEvents(listOf(
                ReplayEventGetStatus(1.853),
                seekToEvent,
                ReplayEventGetStatus(3.085)
            ))
        replayHistoryPlayer.observeReplayEvents(mockLambda)
        replayHistoryPlayer.seekTo(seekToEvent)

        val job = replayHistoryPlayer.play(lifecycleOwner)
        advanceTimeSeconds(5)
        replayHistoryPlayer.finish()
        job.cancelAndJoin()

        val replayUpdates = mutableListOf<List<ReplayEventBase>>()
        coVerify { mockLambda(capture(replayUpdates)) }
        val events = replayUpdates.flatten()
        assertEquals(events.size, 2)
        assertEquals(2.452, events[0].eventTimestamp)
        assertEquals(3.085, events[1].eventTimestamp)
    }

    @Test(expected = Exception::class)
    fun `should crash when seekTo event is missing`() = coroutineRule.runBlockingTest {
        val seekToEvent = ReplayEventGetStatus(2.452)
        val replayHistoryPlayer = ReplayHistoryPlayer()
            .pushEvents(listOf(
                ReplayEventGetStatus(1.853),
                ReplayEventGetStatus(3.085)
            ))
        replayHistoryPlayer.observeReplayEvents(mockLambda)
        replayHistoryPlayer.seekTo(seekToEvent)
    }

    @Test
    fun `should seekTo an event time`() = coroutineRule.runBlockingTest {
        val replayHistoryPlayer = ReplayHistoryPlayer()
            .pushEvents(listOf(
                ReplayEventGetStatus(0.0),
                ReplayEventGetStatus(2.0),
                ReplayEventGetStatus(4.0)
            ))
        replayHistoryPlayer.observeReplayEvents(mockLambda)
        replayHistoryPlayer.seekTo(1.0)

        val job = replayHistoryPlayer.play(lifecycleOwner)
        advanceTimeSeconds(5)
        replayHistoryPlayer.finish()
        job.cancelAndJoin()

        val replayUpdates = mutableListOf<List<ReplayEventBase>>()
        coVerify { mockLambda(capture(replayUpdates)) }
        val events = replayUpdates.flatten()
        assertEquals(events.size, 2)
        assertEquals(2.0, events[0].eventTimestamp)
        assertEquals(4.0, events[1].eventTimestamp)
    }

    @Test
    fun `should seekTo a time relative to total time`() = coroutineRule.runBlockingTest {
        val replayHistoryPlayer = ReplayHistoryPlayer()
            .pushEvents(listOf(
                ReplayEventGetStatus(1580777611.853),
                ReplayEventGetStatus(1580777613.452),
                ReplayEventGetStatus(1580777614.085)
            ))
        replayHistoryPlayer.observeReplayEvents(mockLambda)
        replayHistoryPlayer.seekTo(1.0)

        val job = replayHistoryPlayer.play(lifecycleOwner)
        advanceTimeSeconds(5)
        replayHistoryPlayer.finish()
        job.cancelAndJoin()

        val replayUpdates = mutableListOf<List<ReplayEventBase>>()
        coVerify { mockLambda(capture(replayUpdates)) }
        val events = replayUpdates.flatten()
        assertEquals(2, events.size)
        assertEquals(1580777613.452, events[0].eventTimestamp)
        assertEquals(1580777614.085, events[1].eventTimestamp)
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

    private fun advanceTimeSeconds(seconds: Int) {
        val advanceSeconds = seconds.toLong()
        deviceElapsedTimeNanos += TimeUnit.SECONDS.toNanos(advanceSeconds)
        every { SystemClock.elapsedRealtimeNanos() } returns deviceElapsedTimeNanos
        coroutineRule.testDispatcher.advanceTimeBy(TimeUnit.SECONDS.toMillis(advanceSeconds))
    }
}
