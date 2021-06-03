package com.mapbox.navigation.core.replay

import android.app.PendingIntent
import android.os.Looper
import android.os.SystemClock
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.navigation.core.replay.history.ReplayEventGetStatus
import com.mapbox.navigation.core.replay.history.ReplayEventLocation
import com.mapbox.navigation.core.replay.history.ReplayEventUpdateLocation
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReplayLocationEngineTest {

    private val mapboxReplayer: MapboxReplayer = mockk {
        every { registerObserver(any()) } returns Unit
    }

    private val replayLocationEngine = ReplayLocationEngine(mapboxReplayer)

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
        val captureSuccess = slot<LocationEngineResult>()
        val engineCallback: LocationEngineCallback<LocationEngineResult> = mockk {
            every { onSuccess(capture(captureSuccess)) } returns Unit
        }

        replayLocationEngine.replayEvents(listOf(testLocation()))
        replayLocationEngine.getLastLocation(engineCallback)

        val lastLocation = captureSuccess.captured.lastLocation!!
        assertEquals(38.571011, lastLocation.latitude, 0.001)
        assertEquals(-121.452869, lastLocation.longitude, 0.001)
    }

    @Test
    fun `should replay late first location`() {
        val captureSuccess = slot<LocationEngineResult>()
        val engineCallback: LocationEngineCallback<LocationEngineResult> = mockk {
            every { onSuccess(capture(captureSuccess)) } returns Unit
        }

        replayLocationEngine.getLastLocation(engineCallback)
        replayLocationEngine.replayEvents(listOf(testLocation()))

        val lastLocation = captureSuccess.captured.lastLocation!!
        assertEquals(38.571011, lastLocation.latitude, 0.001)
        assertEquals(-121.452869, lastLocation.longitude, 0.001)
    }

    @Test
    fun `requestLocationUpdates should replay locations`() {
        val successData = slot<LocationEngineResult>()
        val failureData = slot<Exception>()
        val callback: LocationEngineCallback<LocationEngineResult> = mockk {
            every { onSuccess(capture(successData)) } returns Unit
            every { onFailure(capture(failureData)) } returns Unit
        }
        val request: LocationEngineRequest = mockk()
        val looper: Looper? = mockk()

        replayLocationEngine.requestLocationUpdates(request, callback, looper)
        replayLocationEngine.replayEvents(listOf(testLocation()))

        val lastLocation = successData.captured.lastLocation!!
        assertEquals(38.571011, lastLocation.latitude, 0.001)
        assertEquals(-121.452869, lastLocation.longitude, 0.001)
    }

    @Test
    fun `requestLocationUpdates should not replay status updates`() {
        val successData = slot<LocationEngineResult>()
        val failureData = slot<Exception>()
        val callback: LocationEngineCallback<LocationEngineResult> = mockk {
            every { onSuccess(capture(successData)) } returns Unit
            every { onFailure(capture(failureData)) } returns Unit
        }
        val request: LocationEngineRequest = mockk()
        val looper: Looper? = mockk()

        replayLocationEngine.requestLocationUpdates(request, callback, looper)
        replayLocationEngine.replayEvents(listOf(ReplayEventGetStatus(123.0)))

        assertFalse(successData.isCaptured)
        assertFalse(failureData.isCaptured)
    }

    @Test
    fun `requestLocationUpdates can receive multiple locations`() {
        val successData = mutableListOf<LocationEngineResult>()
        val failureData = slot<Exception>()
        val callback: LocationEngineCallback<LocationEngineResult> = mockk {
            every { onSuccess(capture(successData)) } returns Unit
            every { onFailure(capture(failureData)) } returns Unit
        }

        replayLocationEngine.requestLocationUpdates(mockk(), callback, mockk())
        replayLocationEngine.replayEvents(listOf(testLocation()))
        replayLocationEngine.replayEvents(listOf(testLocation()))

        assertEquals(2, successData.size)
        assertFalse(failureData.isCaptured)
    }

    @Test
    fun `removeLocationUpdates should stop capturing locations`() {
        val successData = mutableListOf<LocationEngineResult>()
        val failureData = slot<Exception>()
        val callback: LocationEngineCallback<LocationEngineResult> = mockk {
            every { onSuccess(capture(successData)) } returns Unit
            every { onFailure(capture(failureData)) } returns Unit
        }

        replayLocationEngine.requestLocationUpdates(mockk(), callback, mockk())
        replayLocationEngine.replayEvents(listOf(testLocation()))
        replayLocationEngine.removeLocationUpdates(callback)
        replayLocationEngine.replayEvents(listOf(testLocation()))

        assertEquals(1, successData.size)
        assertFalse(failureData.isCaptured)
    }

    @Test
    fun `should map all values for a location`() {
        val successData = slot<LocationEngineResult>()
        val callback: LocationEngineCallback<LocationEngineResult> = mockk {
            every { onSuccess(capture(successData)) } returns Unit
        }

        replayLocationEngine.requestLocationUpdates(mockk(), callback, mockk())
        replayLocationEngine.replayEvents(
            listOf(
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
                        speed = 12.2
                    )
                )
            )
        )

        val lastLocation = successData.captured.lastLocation!!
        assertEquals(38.571011, lastLocation.latitude, 0.000001)
        assertEquals(-121.452869, lastLocation.longitude, 0.000001)
        assertEquals("ReplayLocationEngineTest", lastLocation.provider)
        assertEquals(158.32, lastLocation.altitude, 0.01)
        assertEquals(12.3f, lastLocation.accuracy, 0.01f)
        assertEquals(287.4f, lastLocation.bearing, 0.01f)
        assertEquals(12.2f, lastLocation.speed, 0.01f)
    }

    @Test
    fun `should map monotonic time`() {
        val successData = slot<LocationEngineResult>()
        val callback: LocationEngineCallback<LocationEngineResult> = mockk {
            every { onSuccess(capture(successData)) } returns Unit
        }
        every { SystemClock.elapsedRealtimeNanos() } returns 135549299280L

        replayLocationEngine.requestLocationUpdates(mockk(), callback, mockk())
        replayLocationEngine.replayEvents(listOf(testLocation()))

        val lastLocation = successData.captured.lastLocation!!
        assertEquals(135549299280L, lastLocation.elapsedRealtimeNanos)
    }

    @Test(expected = UnsupportedOperationException::class)
    fun `pendingIntents are unsupported for requestLocationUpdates`() {
        val request: LocationEngineRequest = mockk()
        val pendingIntent: PendingIntent? = mockk()

        replayLocationEngine.requestLocationUpdates(request, pendingIntent)
    }

    @Test(expected = UnsupportedOperationException::class)
    fun `pendingIntents are unsupported for removeLocationUpdates`() {
        val pendingIntent: PendingIntent? = mockk()

        replayLocationEngine.removeLocationUpdates(pendingIntent)
    }

    private fun testLocation(): ReplayEventUpdateLocation {
        val time = 0.0
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
                speed = null
            )
        )
    }
}
