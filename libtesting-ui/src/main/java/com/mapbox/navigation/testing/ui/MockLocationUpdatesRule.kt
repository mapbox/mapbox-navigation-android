package com.mapbox.navigation.testing.ui

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import org.junit.rules.ExternalResource
import java.util.Date

private const val mockProviderName = LocationManager.GPS_PROVIDER

/**
 * Rule that sets up a mock location provider that can inject location samples
 * straight to the device that the test is running on.
 */
class MockLocationUpdatesRule : ExternalResource() {

    private val instrumentation = getInstrumentation()
    private val appContext = (ApplicationProvider.getApplicationContext() as Context)

    private val locationManager: LocationManager by lazy {
        (appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager)
    }

    override fun before() {
        with(instrumentation.uiAutomation) {
            val result = executeShellCommand(
                "appops set " +
                    appContext.packageName +
                    " android:mock_location allow"
            )
            result.close()
            // gives the adb command chance to process
            // and avoids occasional issues with registering the mock provider
            Thread.sleep(1000)
        }

        try {
            locationManager.addTestProvider(
                mockProviderName,
                false,
                false,
                false,
                false,
                true,
                true,
                true,
                3,
                2
            )
        } catch (ex: Exception) {
            // unstable
            Log.w("MockLocationUpdatesRule", "addTestProvider failed")
        }
        locationManager.setTestProviderEnabled(mockProviderName, true)
    }

    override fun after() {
        locationManager.setTestProviderEnabled(mockProviderName, false)
        locationManager.removeTestProvider(mockProviderName)
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
        check(location.provider == mockProviderName) {
            """
              location provider "${location.provider}" is not equal to required "$mockProviderName"
            """.trimIndent()
        }
        locationManager.setTestProviderLocation(mockProviderName, location)
    }

    private fun generateLocationUpdate(modifyFn: (Location.() -> Unit)? = null): Location {
        val location = Location(mockProviderName)
        location.time = Date().time
        location.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        location.accuracy = 5f
        location.altitude = 0.0
        location.bearing = 0f
        location.speed = 5f
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
