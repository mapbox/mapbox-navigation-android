package com.mapbox.navigation.testing.ui

import android.content.Context
import android.location.Location
import android.os.Build
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.mapbox.navigation.testing.ui.utils.executeShellCommandBlocking
import org.junit.rules.ExternalResource
import java.util.Date

/**
 * Rule that sets up a mock location provider that can inject location samples
 * straight to the device that the test is running on.
 */
class MockLocationUpdatesRule : ExternalResource() {

    private val instrumentation = getInstrumentation()
    private val appContext = (ApplicationProvider.getApplicationContext() as Context)

    private val locationMocker: LocationMocker = LocationMockerProvider.getLocationMocker(appContext)

    override fun before() {
        instrumentation.uiAutomation.executeShellCommandBlocking(
            "appops set " +
                appContext.packageName +
                " android:mock_location allow"
        )

        locationMocker.before()
    }

    override fun after() {
        locationMocker.after()
    }

    /**
     * @param modifyFn allows to modify a base location instance
     */
    fun pushLocationUpdate(modifyFn: (Location.() -> Unit)? = null) {
        pushLocationUpdate(generateLocationUpdate(modifyFn))
    }

    fun pushLocationUpdate(location: Location) {
        check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            "MockLocationUpdatesRule is supported only on Android devices " +
                "running version >= Build.VERSION_CODES.M"
        }
        locationMocker.mockLocation(location)
    }

    fun generateLocationUpdate(modifyFn: (Location.() -> Unit)? = null): Location {
        val location = locationMocker.generateDefaultLocation()
        location.time = Date().time
        location.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        location.accuracy = 5f
        location.altitude = 0.0
        location.bearing = 0f
        location.speed = 0f
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            location.verticalAccuracyMeters = 5f
            location.bearingAccuracyDegrees = 5f
            location.speedAccuracyMetersPerSecond = 5f
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            location.elapsedRealtimeUncertaintyNanos = 0.0
        }

        if (modifyFn != null) {
            location.apply(modifyFn)
        }

        return location
    }
}
