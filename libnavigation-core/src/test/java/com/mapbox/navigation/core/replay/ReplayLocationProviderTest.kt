package com.mapbox.navigation.core.replay

import android.os.HandlerThread
import android.os.Looper
import android.os.SystemClock
import com.mapbox.common.location.GetLocationCallback
import com.mapbox.common.location.Location
import com.mapbox.common.location.LocationObserver
import com.mapbox.navigation.core.replay.history.ReplayEventGetStatus
import com.mapbox.navigation.core.replay.history.ReplayEventLocation
import com.mapbox.navigation.core.replay.history.ReplayEventUpdateLocation
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CountDownLatch

@RunWith(RobolectricTestRunner::class)
class ReplayLocationProviderTest {

    private val mapboxReplayer: MapboxReplayer = mockk {
        every { registerObserver(any()) } returns Unit
        every { eventRealtimeOffset(any()) } returns 0.0
    }

    private val replayLocationProvider = ReplayLocationProvider(mapboxReplayer)

    @Before
    fun setup() {
        mockkStatic(SystemClock::class)
    }

    @After
    fun teardown() {
        unmockkObject(SystemClock.elapsedRealtimeNanos())
    }

    @Test
    fun `should replay the last location`() {
        val captureSuccess = slot<Location>()
        val engineCallback: GetLocationCallback = mockk {
            every { run(capture(captureSuccess)) } returns Unit
        }

        replayLocationProvider.replayEvents(listOf(testLocation()))
        replayLocationProvider.getLastLocation(engineCallback)

        val lastLocation = captureSuccess.captured
        assertEquals(38.571011, lastLocation.latitude, 0.001)
        assertEquals(-121.452869, lastLocation.longitude, 0.001)
    }

    @Test
    fun `should replay late first location`() {
        val captureSuccess = slot<Location>()
        val engineCallback: GetLocationCallback = mockk {
            every { run(capture(captureSuccess)) } returns Unit
        }

        replayLocationProvider.getLastLocation(engineCallback)
        replayLocationProvider.replayEvents(listOf(testLocation()))

        val lastLocation = captureSuccess.captured
        assertEquals(38.571011, lastLocation.latitude, 0.001)
        assertEquals(-121.452869, lastLocation.longitude, 0.001)
    }

    @Test
    fun `should clear last location`() {
        val engineCallback = mockk<GetLocationCallback>()

        replayLocationProvider.replayEvents(listOf(testLocation()))
        replayLocationProvider.cleanUpLastLocation()

        replayLocationProvider.getLastLocation(engineCallback)

        verify(exactly = 0) { engineCallback.run(any()) }
    }

    @Test
    fun `should not remove last location observers`() {
        val captureSuccess = slot<Location>()
        val engineCallback: GetLocationCallback = mockk {
            every { run(capture(captureSuccess)) } returns Unit
        }

        replayLocationProvider.getLastLocation(engineCallback)
        replayLocationProvider.cleanUpLastLocation()
        clearAllMocks(answers = false)

        replayLocationProvider.replayEvents(listOf(mockk(), testLocation()))

        val lastLocation = captureSuccess.captured
        assertEquals(38.571011, lastLocation.latitude, 0.001)
        assertEquals(-121.452869, lastLocation.longitude, 0.001)
    }

    @Test
    fun `requestLocationUpdates should replay locations`() {
        val captureSuccess = slot<List<Location>>()
        val observer = mockk<LocationObserver> {
            every { onLocationUpdateReceived(capture(captureSuccess)) } returns Unit
        }

        replayLocationProvider.addLocationObserver(observer)
        replayLocationProvider.replayEvents(listOf(mockk(), testLocation()))

        val lastLocation = captureSuccess.captured.last()
        assertEquals(38.571011, lastLocation.latitude, 0.001)
        assertEquals(-121.452869, lastLocation.longitude, 0.001)
    }

    @Test
    fun `requestLocationUpdates should not replay status updates`() {
        val successData = slot<List<Location>>()
        val observer: LocationObserver = mockk {
            every { onLocationUpdateReceived(capture(successData)) } returns Unit
        }

        replayLocationProvider.addLocationObserver(observer)
        replayLocationProvider.replayEvents(
            listOf(
                ReplayEventGetStatus(100.0),
                ReplayEventGetStatus(123.0),
            ),
        )

        assertFalse(successData.isCaptured)
    }

    @Test
    fun `requestLocationUpdates can receive multiple locations`() {
        val successData = mutableListOf<List<Location>>()
        val observer: LocationObserver = mockk {
            every { onLocationUpdateReceived(capture(successData)) } returns Unit
        }

        replayLocationProvider.addLocationObserver(observer)
        replayLocationProvider.replayEvents(listOf(mockk(), testLocation()))
        replayLocationProvider.replayEvents(listOf(mockk(), testLocation()))

        assertEquals(2, successData.size)
    }

    @Test
    fun `removeLocationUpdates should stop capturing locations`() {
        val successData = mutableListOf<List<Location>>()
        val observer: LocationObserver = mockk {
            every { onLocationUpdateReceived(capture(successData)) } returns Unit
        }

        replayLocationProvider.addLocationObserver(observer)
        replayLocationProvider.replayEvents(listOf(mockk(), testLocation()))
        replayLocationProvider.removeLocationObserver(observer)
        replayLocationProvider.replayEvents(listOf(mockk(), testLocation()))

        assertEquals(1, successData.size)
    }

    @Test
    fun `should map all values for a location`() {
        val successData = slot<List<Location>>()
        val observer: LocationObserver = mockk {
            every { onLocationUpdateReceived(capture(successData)) } returns Unit
        }
        replayLocationProvider.addLocationObserver(observer)
        replayLocationProvider.replayEvents(
            listOf(
                ReplayEventUpdateLocation(
                    2234.56,
                    ReplayEventLocation(
                        lat = 48.571011,
                        lon = -151.452869,
                        provider = "ReplayLocationEngineTest",
                        time = 3468.135,
                        altitude = 258.32,
                        accuracyHorizontal = 22.3,
                        bearing = 387.4,
                        speed = 22.2,
                    ),
                ),
                ReplayEventUpdateLocation(
                    1234.56,
                    ReplayEventLocation(
                        lat = 38.571011,
                        lon = -121.452869,
                        provider = "ReplayLocationEngineTest",
                        time = 2468.135,
                        altitude = 158.32,
                        accuracyHorizontal = 12.3,
                        bearing = 287.4,
                        speed = 12.2,
                    ),
                ),
            ),
        )

        val lastLocation = successData.captured.last()
        assertEquals(38.571011, lastLocation.latitude, 0.000001)
        assertEquals(-121.452869, lastLocation.longitude, 0.000001)
        assertEquals("ReplayLocationEngineTest", lastLocation.source)
        assertEquals(158.32, lastLocation.altitude!!, 0.01)
        assertEquals(12.3, lastLocation.horizontalAccuracy!!, 0.01)
        assertEquals(287.4, lastLocation.bearing!!, 0.01)
        assertEquals(12.2, lastLocation.speed!!, 0.01)
    }

    @Test
    fun `should map monotonic time`() {
        val successData = slot<List<Location>>()
        val observer: LocationObserver = mockk {
            every { onLocationUpdateReceived(capture(successData)) } returns Unit
        }
        every { SystemClock.elapsedRealtimeNanos() } returns 135549299280L

        replayLocationProvider.addLocationObserver(observer)
        replayLocationProvider.replayEvents(listOf(mockk(), testLocation()))

        val lastLocation = successData.captured.last()
        assertEquals(135549299280L, lastLocation.monotonicTimestamp)
    }

    @Test
    fun `should add realtimeOffset to the location time`() {
        val successData = mutableListOf<List<Location>>()
        val observer: LocationObserver = mockk {
            every { onLocationUpdateReceived(capture(successData)) } returns Unit
        }
        val testEventFirst = testLocation(2.0)
        val testEventSecond = testLocation(3.0)
        every { mapboxReplayer.eventRealtimeOffset(2.0) } returns -0.3
        every { mapboxReplayer.eventRealtimeOffset(3.0) } returns 0.4
        every { SystemClock.elapsedRealtimeNanos() } returns 5_000_000_000L

        replayLocationProvider.addLocationObserver(observer)
        replayLocationProvider.replayEvents(listOf(testEventFirst, testEventSecond))

        assertEquals(4700000000, successData[0].last().monotonicTimestamp)
        assertEquals(5400000000, successData[1].last().monotonicTimestamp)
    }

    @Test
    fun addLocationObserverMultipleTimes() {
        var actualLooper: Looper? = null
        val latch = CountDownLatch(1)
        val observer: LocationObserver = mockk(relaxed = true) {
            every { onLocationUpdateReceived(any()) } answers {
                actualLooper = Looper.myLooper()
                latch.countDown()
            }
        }
        val looper1 = Looper.getMainLooper()
        val thread = HandlerThread("thread")
        thread.start()
        val looper2 = thread.looper

        replayLocationProvider.addLocationObserver(observer)
        replayLocationProvider.addLocationObserver(observer)
        replayLocationProvider.addLocationObserver(observer, looper1)
        replayLocationProvider.addLocationObserver(observer, looper2)

        replayLocationProvider.replayEvents(listOf(mockk(), testLocation()))

        latch.await()
        verify(exactly = 1) { observer.onLocationUpdateReceived(any()) }
        assertEquals(looper2, actualLooper)

        clearAllMocks(answers = false)

        replayLocationProvider.removeLocationObserver(observer)

        replayLocationProvider.replayEvents(listOf(mockk(), testLocation(1.0)))
        verify(exactly = 0) { observer.onLocationUpdateReceived(any()) }

        thread.quit()
    }

    private fun testLocation(time: Double = 0.0): ReplayEventUpdateLocation {
        return ReplayEventUpdateLocation(
            time,
            ReplayEventLocation(
                lat = 38.571011,
                lon = -121.452869,
                provider = "ReplayLocationEngineTest",
                time = time,
                altitude = null,
                accuracyHorizontal = null,
                bearing = null,
                speed = null,
            ),
        )
    }
}
