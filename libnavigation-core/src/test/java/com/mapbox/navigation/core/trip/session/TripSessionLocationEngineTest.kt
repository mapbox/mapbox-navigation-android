package com.mapbox.navigation.core.trip.session

import android.content.Context
import android.location.Location
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.navigation.base.options.NavigationOptions
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class TripSessionLocationEngineTest {

    private val locationCallbackSlot = slot<LocationEngineCallback<LocationEngineResult>>()
    private val locationEngine: LocationEngine = mockk(relaxUnitFun = true)
    private val locationEngineResult: LocationEngineResult = mockk(relaxUnitFun = true)
    private val location: Location = mockk(relaxed = true)

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val navigationOptions = NavigationOptions.Builder(context)
        .locationEngine(locationEngine)
        .build()
    private val tripSessionLocationEngine = TripSessionLocationEngine(navigationOptions)

    @Before
    fun setup() {
        every {
            locationEngine.requestLocationUpdates(
                any(),
                capture(locationCallbackSlot),
                any()
            )
        } answers {}
        every { locationEngineResult.locations } returns listOf(location)
    }

    @Test
    fun `should request location updates from navigation options when replay is disabled`() {
        tripSessionLocationEngine.startLocationUpdates(false) {
            // This test is not verifying the callback
        }

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
        tripSessionLocationEngine.startLocationUpdates(false) {
            // This test is not verifying the callback
        }
        tripSessionLocationEngine.stopLocationUpdates()

        verify(exactly = 1) {
            navigationOptions.locationEngine.removeLocationUpdates(
                any<LocationEngineCallback<LocationEngineResult>>()
            )
        }
    }

    @Test
    fun `should not request location updates from navigation options when replay is enabled`() {
        tripSessionLocationEngine.startLocationUpdates(true) {
            // This test is not verifying the callback
        }

        verify(exactly = 0) {
            navigationOptions.locationEngine.requestLocationUpdates(any(), any(), any())
        }
    }
}
