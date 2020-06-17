package com.mapbox.navigation.core.replay.history

import android.os.SystemClock
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.testing.MainCoroutineRule
import io.mockk.Called
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit

@ExperimentalCoroutinesApi
class MapboxReplayerTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val replayEventsObserver: ReplayEventsObserver = mockk(relaxed = true)
    private var deviceElapsedTimeNanos = TimeUnit.HOURS.toNanos(11)

    private val mapboxReplayer = MapboxReplayer()

    @Test
    fun `should play start transit and location in order`() = coroutineRule.runBlockingTest {
        mapboxReplayer.pushEvents(
            listOf(
                ReplayEventGetStatus(1580777612.853),
                ReplayEventUpdateLocation(
                    1580777612.89,
                    ReplayEventLocation(
                        lat = 49.2492411,
                        lon = 8.8512315,
                        provider = "fused",
                        time = 1580777612.892,
                        altitude = 212.4732666015625,
                        accuracyHorizontal = 4.288000106811523,
                        bearing = 243.31265258789063,
                        speed = 0.5585000514984131
                    )
                )
            )
        )
        mapboxReplayer.registerObserver(replayEventsObserver)

        mapboxReplayer.play()
        advanceTimeMillis(5000)
        mapboxReplayer.finish()

        val replayUpdates = mutableListOf<List<ReplayEventBase>>()
        coVerify { replayEventsObserver.replayEvents(capture(replayUpdates)) }
        val events = replayUpdates.flatten()
        assertEquals(2, events.size)
        assertEquals(1580777612.853, events[0].eventTimestamp, 0.001)
        assertEquals(1580777612.89, events[1].eventTimestamp, 0.001)
    }

    @Test
    fun `should play 2 of 3 locations that include time window`() = coroutineRule.runBlockingTest {
        mapboxReplayer.pushEvents(
            listOf(
                ReplayEventUpdateLocation(
                    1580777820.952,
                    ReplayEventLocation(
                        lat = 49.2450478,
                        lon = 8.8682922,
                        time = 1580777820.952,
                        speed = 30.239412307739259,
                        bearing = 108.00135040283203,
                        altitude = 222.47210693359376,
                        accuracyHorizontal = 3.9000000953674318,
                        provider = "fused"
                    )
                ),
                ReplayEventUpdateLocation(
                    1580777822.959,
                    ReplayEventLocation(
                        lat = 49.2448858,
                        lon = 8.8690847,
                        time = 1580777822.958,
                        speed = 29.931121826171876,
                        bearing = 106.001953125,
                        altitude = 221.9241943359375,
                        accuracyHorizontal = 3.9000000953674318,
                        provider = "fused"
                    )
                ),
                ReplayEventUpdateLocation(
                    1580777824.953,
                    ReplayEventLocation(
                        lat = 49.2447354,
                        lon = 8.8698759,
                        time = 1580777824.89,
                        speed = 29.96711540222168,
                        bearing = 106.00138092041016,
                        altitude = 221.253662109375,
                        accuracyHorizontal = 3.9000000953674318,
                        provider = "fused"
                    )
                )
            )
        )
        mapboxReplayer.registerObserver(replayEventsObserver)

        mapboxReplayer.play()
        advanceTimeMillis(3000)
        mapboxReplayer.finish()

        // Note that it only played 2 of the 3 locations
        val replayUpdates = mutableListOf<List<ReplayEventBase>>()
        coVerify { replayEventsObserver.replayEvents(capture(replayUpdates)) }
        val events = replayUpdates.flatten()
        assertEquals(2, events.size)
        assertEquals(1580777820.952, events[0].eventTimestamp, 0.001)
        assertEquals(1580777822.959, events[1].eventTimestamp, 0.001)
    }

    @Test
    fun `should resume playing after completing events`() = coroutineRule.runBlockingTest {
        val testEvents = List(12) { ReplayEventGetStatus(it.toDouble()) }
        mapboxReplayer.pushEvents(testEvents)
        val timeCapture = mutableListOf<Pair<ReplayEventBase, Long>>()
        mapboxReplayer.registerObserver(object : ReplayEventsObserver {
            override fun replayEvents(events: List<ReplayEventBase>) {
                events.forEach { timeCapture.add(Pair(it, currentTime)) }
            }
        })

        mapboxReplayer.play()
        advanceTimeMillis(20000)
        val extraEvents = List(7) { ReplayEventGetStatus(it.toDouble()) }
        mapboxReplayer.pushEvents(extraEvents)
        advanceTimeMillis(20000)
        mapboxReplayer.finish()

        // 12 events at the beginning
        // 7 events later
        assertEquals(12 + 7, timeCapture.size)
        timeCapture.slice(0..11).forEach { assertTrue(it.second < 20000) }
        timeCapture.slice(12..18).forEach { assertTrue(it.second > 20000) }
    }

    @Test
    fun `should not delay player when consumer takes time`() = coroutineRule.runBlockingTest {
        mapboxReplayer.pushEvents(
            listOf(
                ReplayEventGetStatus(1000.000),
                ReplayEventGetStatus(1001.000),
                ReplayEventGetStatus(1003.000)
            )
        )
        val timeCapture = mutableListOf<Long>()
        mapboxReplayer.registerObserver(object : ReplayEventsObserver {
            override fun replayEvents(events: List<ReplayEventBase>) {
                if (events.isNotEmpty()) {
                    timeCapture.add(currentTime)
                    advanceTimeMillis(75)
                }
            }
        })

        mapboxReplayer.play()
        for (i in 0..3000) {
            advanceTimeMillis(1)
        }
        mapboxReplayer.finish()

        assertEquals(3, timeCapture.size)
        assertEquals(0L, timeCapture[0])
        assertEquals(1000L, timeCapture[1])
        assertEquals(3000L, timeCapture[2])
    }

    @Test
    fun `should allow custom events`() = coroutineRule.runBlockingTest {
        data class CustomReplayEvent(
            override val eventTimestamp: Double,
            val customValue: String
        ) : ReplayEventBase
        mapboxReplayer.pushEvents(
            listOf(
                CustomReplayEvent(1580777612.853, "custom value"),
                ReplayEventUpdateLocation(
                    1580777613.89,
                    ReplayEventLocation(
                        lat = 49.2492411,
                        lon = 8.8512315,
                        time = null,
                        provider = null,
                        altitude = null,
                        accuracyHorizontal = null,
                        bearing = null,
                        speed = null
                    )
                )
            )
        )
        mapboxReplayer.registerObserver(replayEventsObserver)

        mapboxReplayer.play()
        advanceTimeMillis(5000)
        mapboxReplayer.finish()

        val replayUpdates = mutableListOf<List<ReplayEventBase>>()
        coVerify { replayEventsObserver.replayEvents(capture(replayUpdates)) }
        val events = replayUpdates.flatten()
        assertEquals(events.size, 2)
        assertTrue(events[0] is CustomReplayEvent)
        assertEquals("custom value", (events[0] as CustomReplayEvent).customValue)
        assertEquals(1580777613.89, events[1].eventTimestamp, 0.001)
    }

    @Test
    fun `should not crash if history data is empty`() {
        mapboxReplayer.play()
        mapboxReplayer.finish()
    }

    @Test
    fun `playFirstLocation should ignore events before the first location`() {
        mapboxReplayer.pushEvents(
            listOf(
                ReplayEventGetStatus(1580777612.853),
                ReplayEventUpdateLocation(
                    1580777612.89,
                    ReplayEventLocation(
                        lat = 49.2492411,
                        lon = 8.8512315,
                        provider = "fused",
                        time = 1580777612.892,
                        altitude = 212.4732666015625,
                        accuracyHorizontal = 4.288000106811523,
                        bearing = 243.31265258789063,
                        speed = 0.5585000514984131
                    )
                )
            )
        )
        mapboxReplayer.registerObserver(replayEventsObserver)

        mapboxReplayer.playFirstLocation()

        val replayUpdates = mutableListOf<List<ReplayEventBase>>()
        verify { replayEventsObserver.replayEvents(capture(replayUpdates)) }
        val events = replayUpdates.flatten()
        assertEquals(1, events.size)
        assertEquals(1580777612.89, events[0].eventTimestamp, 0.001)
    }

    @Test
    fun `playFirstLocation should handle history events without locations`() {
        mapboxReplayer.pushEvents(
            listOf(
                ReplayEventGetStatus(1580777612.853),
                ReplayEventGetStatus(1580777613.452),
                ReplayEventGetStatus(1580777614.085)
            )
        )
        mapboxReplayer.registerObserver(replayEventsObserver)

        mapboxReplayer.playFirstLocation()

        verify { replayEventsObserver wasNot Called }
    }

    @Test
    fun `should seekTo an event`() = coroutineRule.runBlockingTest {
        val seekToEvent = ReplayEventGetStatus(2.452)
        mapboxReplayer.pushEvents(
            listOf(
                ReplayEventGetStatus(1.853),
                seekToEvent,
                ReplayEventGetStatus(3.085)
            )
        )
        mapboxReplayer.registerObserver(replayEventsObserver)
        mapboxReplayer.seekTo(seekToEvent)

        mapboxReplayer.play()
        advanceTimeMillis(5000)
        mapboxReplayer.finish()

        val replayUpdates = mutableListOf<List<ReplayEventBase>>()
        coVerify { replayEventsObserver.replayEvents(capture(replayUpdates)) }
        val events = replayUpdates.flatten()
        assertEquals(events.size, 2)
        assertEquals(2.452, events[0].eventTimestamp, 0.001)
        assertEquals(3.085, events[1].eventTimestamp, 0.001)
    }

    @Test(expected = Exception::class)
    fun `should crash when seekTo event is missing`() = coroutineRule.runBlockingTest {
        val seekToEvent = ReplayEventGetStatus(2.452)
        mapboxReplayer.pushEvents(
            listOf(
                ReplayEventGetStatus(1.853),
                ReplayEventGetStatus(3.085)
            )
        )
        mapboxReplayer.registerObserver(replayEventsObserver)
        mapboxReplayer.seekTo(seekToEvent)
    }

    @Test
    fun `should seekTo an event time`() = coroutineRule.runBlockingTest {
        mapboxReplayer.pushEvents(
            listOf(
                ReplayEventGetStatus(0.0),
                ReplayEventGetStatus(2.0),
                ReplayEventGetStatus(4.0)
            )
        )
        mapboxReplayer.registerObserver(replayEventsObserver)
        mapboxReplayer.seekTo(1.0)

        mapboxReplayer.play()
        advanceTimeMillis(5000)
        mapboxReplayer.finish()

        val replayUpdates = mutableListOf<List<ReplayEventBase>>()
        coVerify { replayEventsObserver.replayEvents(capture(replayUpdates)) }
        val events = replayUpdates.flatten()
        assertEquals(events.size, 2)
        assertEquals(2.0, events[0].eventTimestamp, 0.001)
        assertEquals(4.0, events[1].eventTimestamp, 0.001)
    }

    @Test
    fun `should seekTo a time relative to total time`() = coroutineRule.runBlockingTest {
        mapboxReplayer.pushEvents(
            listOf(
                ReplayEventGetStatus(1580777611.853),
                ReplayEventGetStatus(1580777613.452),
                ReplayEventGetStatus(1580777614.085)
            )
        )
        mapboxReplayer.registerObserver(replayEventsObserver)
        mapboxReplayer.seekTo(1.0)

        mapboxReplayer.play()
        advanceTimeMillis(5000)
        mapboxReplayer.finish()

        val replayUpdates = mutableListOf<List<ReplayEventBase>>()
        coVerify { replayEventsObserver.replayEvents(capture(replayUpdates)) }
        val events = replayUpdates.flatten()
        assertEquals(2, events.size)
        assertEquals(1580777613.452, events[0].eventTimestamp, 0.001)
        assertEquals(1580777614.085, events[1].eventTimestamp, 0.001)
    }

    @Test
    fun `playbackSpeed should play one event per second at 1_0 playbackSpeed`() = coroutineRule.runBlockingTest {
        val testEvents = List(20) { ReplayEventGetStatus(it.toDouble()) }

        mapboxReplayer.pushEvents(testEvents)
        mapboxReplayer.playbackSpeed(1.0)
        mapboxReplayer.play()
        val timeCapture = mutableListOf<Pair<ReplayEventBase, Long>>()
        mapboxReplayer.registerObserver(object : ReplayEventsObserver {
            override fun replayEvents(events: List<ReplayEventBase>) {
                events.forEach { timeCapture.add(Pair(it, currentTime)) }
            }
        })
        advanceTimeMillis(3000)
        mapboxReplayer.finish()

        assertEquals(3, timeCapture.size)
    }

    @Test
    fun `playbackSpeed should play four events per second at 4_0 playbackSpeed`() = coroutineRule.runBlockingTest {
        val testEvents = List(20) { ReplayEventGetStatus(it.toDouble()) }
        mapboxReplayer.pushEvents(testEvents)

        mapboxReplayer.playbackSpeed(4.0)
        mapboxReplayer.play()
        val timeCapture = mutableListOf<Pair<ReplayEventBase, Long>>()
        mapboxReplayer.registerObserver(object : ReplayEventsObserver {
            override fun replayEvents(events: List<ReplayEventBase>) {
                events.forEach { timeCapture.add(Pair(it, currentTime)) }
            }
        })
        advanceTimeMillis(4000)
        mapboxReplayer.finish()

        assertEquals(16, timeCapture.size)
    }

    @Test
    fun `playbackSpeed should play one event every four seconds at 0_25 playbackSpeed`() = coroutineRule.runBlockingTest {
        val testEvents = List(20) { ReplayEventGetStatus(it.toDouble()) }
        mapboxReplayer.pushEvents(testEvents)

        mapboxReplayer.playbackSpeed(0.25)
        mapboxReplayer.play()
        val timeCapture = mutableListOf<Pair<ReplayEventBase, Long>>()
        mapboxReplayer.registerObserver(object : ReplayEventsObserver {
            override fun replayEvents(events: List<ReplayEventBase>) {
                events.forEach { timeCapture.add(Pair(it, currentTime)) }
            }
        })
        advanceTimeMillis(40000)
        mapboxReplayer.finish()

        assertEquals(10, timeCapture.size)
    }

    @Test
    fun `playbackSpeed should update play speed while playing`() = coroutineRule.runBlockingTest {
        val testEvents = List(20) { ReplayEventGetStatus(it.toDouble()) }
        mapboxReplayer.pushEvents(testEvents)

        mapboxReplayer.playbackSpeed(1.0)
        mapboxReplayer.play()
        val timeCapture = mutableListOf<Pair<ReplayEventBase, Long>>()
        mapboxReplayer.registerObserver(object : ReplayEventsObserver {
            override fun replayEvents(events: List<ReplayEventBase>) {
                events.forEach { timeCapture.add(Pair(it, currentTime)) }
            }
        })
        advanceTimeMillis(2000)
        mapboxReplayer.playbackSpeed(3.0)
        advanceTimeMillis(1999) // advance a fraction to remove the equal events
        mapboxReplayer.finish()

        // 2 events over 2 seconds at 1x speed.
        // 6 events over 2 seconds at 3x speed.
        assertEquals(2 + 6, timeCapture.size)
        timeCapture.slice(0..1).forEach { assertTrue(it.second < 2000) }
        timeCapture.slice(2..7).forEach { assertTrue(it.second > 2000) }
    }

    @Test
    fun `playbackSpeed should not crash when events are completed`() = coroutineRule.runBlockingTest {
        val testEvents = List(12) { ReplayEventGetStatus(it.toDouble()) }
        mapboxReplayer.pushEvents(testEvents)
        val timeCapture = mutableListOf<Pair<ReplayEventBase, Long>>()
        mapboxReplayer.registerObserver(object : ReplayEventsObserver {
            override fun replayEvents(events: List<ReplayEventBase>) {
                events.forEach { timeCapture.add(Pair(it, currentTime)) }
            }
        })

        mapboxReplayer.play()
        advanceTimeMillis(20000)
        mapboxReplayer.playbackSpeed(3.0)
        advanceTimeMillis(20000)
        mapboxReplayer.finish()

        // 12 events at the beginning
        assertEquals(12, timeCapture.size)
    }

    @Test
    fun `should register multiple observers`() = coroutineRule.runBlockingTest {
        mapboxReplayer.pushEvents(
            listOf(
                ReplayEventGetStatus(1.0),
                ReplayEventGetStatus(2.0),
                ReplayEventGetStatus(3.0)
            )
        )
        val firstObserver: ReplayEventsObserver = mockk(relaxed = true)
        val secondObserver: ReplayEventsObserver = mockk(relaxed = true)
        mapboxReplayer.registerObserver(firstObserver)
        mapboxReplayer.registerObserver(secondObserver)

        mapboxReplayer.play()
        advanceTimeMillis(5000)
        mapboxReplayer.finish()

        val firstObserverEvents = mutableListOf<List<ReplayEventBase>>()
        coVerify { firstObserver.replayEvents(capture(firstObserverEvents)) }
        val secondObserverEvents = mutableListOf<List<ReplayEventBase>>()
        coVerify { secondObserver.replayEvents(capture(secondObserverEvents)) }
        val firstEvents = firstObserverEvents.flatten()
        val secondEvents = secondObserverEvents.flatten()
        assertEquals(3, firstEvents.size)
        assertEquals(firstEvents, secondEvents)
    }

    @Test
    fun `should unregister single observers`() = coroutineRule.runBlockingTest {
        mapboxReplayer.pushEvents(
            listOf(
                ReplayEventGetStatus(1.0),
                ReplayEventGetStatus(2.0),
                ReplayEventGetStatus(3.0)
            )
        )
        val firstObserver: ReplayEventsObserver = mockk(relaxed = true)
        val secondObserver: ReplayEventsObserver = mockk(relaxed = true)
        mapboxReplayer.registerObserver(firstObserver)
        mapboxReplayer.registerObserver(secondObserver)

        mapboxReplayer.play()
        advanceTimeMillis(1000)
        mapboxReplayer.unregisterObserver(firstObserver)
        advanceTimeMillis(2000)
        mapboxReplayer.finish()

        val firstObserverEvents = mutableListOf<List<ReplayEventBase>>()
        coVerify { firstObserver.replayEvents(capture(firstObserverEvents)) }
        val secondObserverEvents = mutableListOf<List<ReplayEventBase>>()
        coVerify { secondObserver.replayEvents(capture(secondObserverEvents)) }
        assertEquals(2, firstObserverEvents.flatten().size)
        assertEquals(3, secondObserverEvents.flatten().size)
    }

    @Test
    fun `clearEvents should do nothing without events to clear`() = coroutineRule.runBlockingTest {
        mapboxReplayer.clearEvents()

        assertEquals(0.0, mapboxReplayer.durationSeconds(), 0.0)
    }

    @Test
    fun `clearEvents should play second route after clearing first route`() = coroutineRule.runBlockingTest {
        val firstRoute = List(20) { ReplayEventGetStatus(it.toDouble()) }
        val secondRoute = List(10) { ReplayEventGetStatus(100.0 + it.toDouble()) }
        val timeCapture = mutableListOf<Pair<ReplayEventBase, Long>>()
        mapboxReplayer.registerObserver(object : ReplayEventsObserver {
            override fun replayEvents(events: List<ReplayEventBase>) {
                events.forEach { timeCapture.add(Pair(it, currentTime)) }
            }
        })

        mapboxReplayer.pushEvents(firstRoute)
        mapboxReplayer.play()
        advanceTimeMillis(20000)
        mapboxReplayer.clearEvents()
        mapboxReplayer.pushEvents(secondRoute)
        mapboxReplayer.play()
        advanceTimeMillis(10000)
        mapboxReplayer.finish()

        // Captures all 30 events
        assertEquals(30, timeCapture.size)
        assertEquals(105.0, timeCapture[25].first.eventTimestamp, 0.0)
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

    private fun advanceTimeMillis(advanceMillis: Long) {
        deviceElapsedTimeNanos += TimeUnit.MILLISECONDS.toNanos(advanceMillis)
        every { SystemClock.elapsedRealtimeNanos() } returns deviceElapsedTimeNanos
        coroutineRule.testDispatcher.advanceTimeBy(advanceMillis)
    }
}
