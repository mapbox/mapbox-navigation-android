package com.mapbox.navigation.core.trip.session

import android.content.Context
import android.location.Location
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.replay.ReplayLocationEngine
import com.mapbox.navigation.testing.LoggingFrontendTestRule
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class TripSessionLocationEngineTest {

    @get:Rule
    val loggerRule = LoggingFrontendTestRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val deviceLocationEngine = mockk<LocationEngine>(relaxUnitFun = true).also {
        mockLocationEngine(it)
    }
    private val replayLocationEngine = mockk<ReplayLocationEngine>(relaxUnitFun = true).also {
        mockLocationEngine(it)
    }
    private val navigationOptions = NavigationOptions.Builder(context)
        .locationEngine(deviceLocationEngine)
        .build()

    private val sut = TripSessionLocationEngine(navigationOptions) {
        replayLocationEngine
    }

    @Test
    fun `should request location updates from navigation options when replay is disabled`() {
        val onRawLocationUpdate: (Location) -> Unit = mockk()
        sut.startLocationUpdates(false, onRawLocationUpdate)

        verify(exactly = 1) {
            navigationOptions.locationEngine.requestLocationUpdates(
                any(),
                any(),
                Looper.getMainLooper()
            )
        }
    }

    @Test
    fun `should stop location updates from navigation options when replay is disabled`() {
        val onRawLocationUpdate: (Location) -> Unit = mockk()
        sut.startLocationUpdates(false, onRawLocationUpdate)
        sut.stopLocationUpdates()

        verifyOrder {
            navigationOptions.locationEngine.requestLocationUpdates(any(), any(), any())
            navigationOptions.locationEngine.removeLocationUpdates(
                any<LocationEngineCallback<LocationEngineResult>>()
            )
        }
    }

    @Test
    fun `should not request location updates from replay engine when replay is disabled`() {
        val onRawLocationUpdate: (Location) -> Unit = mockk()
        sut.startLocationUpdates(false, onRawLocationUpdate)

        verify(exactly = 0) {
            replayLocationEngine.requestLocationUpdates(any(), any(), any())
        }
    }

    @Test
    fun `should request location updates from replay engine when replay is enable`() {
        val onRawLocationUpdate: (Location) -> Unit = mockk()
        sut.startLocationUpdates(true, onRawLocationUpdate)

        verify(exactly = 1) {
            replayLocationEngine.requestLocationUpdates(
                any(),
                any(),
                Looper.getMainLooper()
            )
        }
    }

    @Test
    fun `should stop location updates from replay engine when replay is enabled`() {
        val onRawLocationUpdate: (Location) -> Unit = mockk()
        sut.startLocationUpdates(true, onRawLocationUpdate)
        sut.stopLocationUpdates()

        verifyOrder {
            replayLocationEngine.requestLocationUpdates(any(), any(), any())
            replayLocationEngine.removeLocationUpdates(
                any<LocationEngineCallback<LocationEngineResult>>()
            )
        }
    }

    @Test
    fun `should clean up last location from replay engine when replay is enabled`() {
        val onRawLocationUpdate: (Location) -> Unit = mockk()
        sut.startLocationUpdates(true, onRawLocationUpdate)
        sut.stopLocationUpdates()

        verify { replayLocationEngine.cleanUpLastLocation() }
    }

    @Test
    fun `should not clean up last location from replay engine when replay is disabled`() {
        val onRawLocationUpdate: (Location) -> Unit = mockk()
        sut.startLocationUpdates(false, onRawLocationUpdate)
        sut.stopLocationUpdates()

        verify(exactly = 0) { replayLocationEngine.cleanUpLastLocation() }
    }

    @Test
    fun `should not request location updates from navigation options when replay is enabled`() {
        val onRawLocationUpdate: (Location) -> Unit = mockk()
        sut.startLocationUpdates(true, onRawLocationUpdate)

        verify(exactly = 0) {
            navigationOptions.locationEngine.requestLocationUpdates(any(), any(), any())
        }
    }

    @Test
    fun `should remove location updates from previous location engine`() {
        val onRawLocationUpdate: (Location) -> Unit = mockk()
        sut.startLocationUpdates(true, onRawLocationUpdate)

        verify(exactly = 0) {
            navigationOptions.locationEngine.requestLocationUpdates(any(), any(), any())
        }
    }

    @Test
    fun `startLocationUpdates should remove updates from previous location engine`() {
        val onRawLocationUpdate: (Location) -> Unit = mockk()
        sut.startLocationUpdates(true, onRawLocationUpdate)
        sut.startLocationUpdates(false, onRawLocationUpdate)
        sut.startLocationUpdates(true, onRawLocationUpdate)

        verifyOrder {
            replayLocationEngine.requestLocationUpdates(any(), any(), any())
            replayLocationEngine.removeLocationUpdates(
                any<LocationEngineCallback<LocationEngineResult>>()
            )
            navigationOptions.locationEngine.requestLocationUpdates(any(), any(), any())
            navigationOptions.locationEngine.removeLocationUpdates(
                any<LocationEngineCallback<LocationEngineResult>>()
            )
            replayLocationEngine.requestLocationUpdates(any(), any(), any())
        }
    }

    @Test
    fun `isReplayEnabled is true after replay is enabled`() {
        sut.startLocationUpdates(true, mockk())

        assertTrue(sut.isReplayEnabled)
    }

    @Test
    fun `isReplayEnabled is false when replay is disabled for location updates`() {
        sut.startLocationUpdates(false, mockk())

        assertFalse(sut.isReplayEnabled)
    }

    @Test
    fun `isReplayEnabled is false after stopLocationUpdates`() {
        sut.startLocationUpdates(true, mockk())
        sut.stopLocationUpdates()

        assertFalse(sut.isReplayEnabled)
    }

    @Test
    fun `should filter out locations from previous session`() {
        val firstCallback = mockk<(Location) -> Unit>(relaxed = true)
        val secondCallback = mockk<(Location) -> Unit>(relaxed = true)
        val firstEngineCallback = slot<LocationEngineCallback<LocationEngineResult>>()
        val secondEngineCallback = slot<LocationEngineCallback<LocationEngineResult>>()
        val location1 = mockk<Location>(relaxed = true)
        val locationResult1 = mockk<LocationEngineResult>(relaxed = true) {
            every { locations } returns listOf(location1)
        }
        val location2 = mockk<Location>(relaxed = true)
        val locationResult2 = mockk<LocationEngineResult>(relaxed = true) {
            every { locations } returns listOf(location2)
        }
        sut.startLocationUpdates(true, firstCallback)
        verify {
            replayLocationEngine.getLastLocation(capture(firstEngineCallback))
        }
        sut.stopLocationUpdates()
        clearAllMocks(answers = false)
        sut.startLocationUpdates(true, secondCallback)
        verify {
            replayLocationEngine.getLastLocation(capture(secondEngineCallback))
        }

        firstEngineCallback.captured.onSuccess(locationResult1)
        secondEngineCallback.captured.onSuccess(locationResult2)

        verify(exactly = 0) {
            firstCallback(any())
            secondCallback(location1)
        }
        verify(exactly = 1) {
            secondCallback(location2)
        }
    }

    private fun mockLocationEngine(locationEngine: LocationEngine) {
        val locationCallbackSlot = slot<LocationEngineCallback<LocationEngineResult>>()
        val locationEngineResult: LocationEngineResult = mockk(relaxUnitFun = true)
        val location: Location = mockk(relaxed = true)
        every {
            locationEngine.requestLocationUpdates(
                any(),
                capture(locationCallbackSlot),
                any()
            )
        } answers {}
        every { locationEngineResult.locations } returns listOf(location)
    }
}
