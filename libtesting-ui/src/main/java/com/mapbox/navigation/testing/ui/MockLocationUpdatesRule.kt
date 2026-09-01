package com.mapbox.navigation.testing.ui

import android.content.Context
import android.location.Location
import android.os.Build
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import com.mapbox.navigation.base.options.LocationOptions
import org.junit.rules.ExternalResource
import java.util.Date

/**
 * Rule that sets up a mock location provider that can inject location samples
 * straight to the device that the test is running on.
 *
 * @param useFakeDeviceLocationProvider when `true`, this rule pushes location updates
 * synchronously, in-process, through an internal fake device location provider instead of the
 * real platform providers (`LocationManager`/`FusedLocationProviderClient`), avoiding the real
 * platform provider's asynchronous, racy start-up.
 * `withMapboxNavigation` (see `MapboxNavigationCreator.kt`) already wires [locationOptions] in
 * automatically when this flag is `true`, so no extra setup is needed at the call site; pass it
 * into [com.mapbox.navigation.base.options.NavigationOptions.Builder.locationOptions] yourself
 * only if you're building a [MapboxNavigation] instance without that helper.
 */
class MockLocationUpdatesRule @JvmOverloads constructor(
    private val useFakeDeviceLocationProvider: Boolean = true,
) : ExternalResource() {

    private val appContext = (ApplicationProvider.getApplicationContext() as Context)

    private val locationMocker: LocationMocker =
        LocationMockerProvider.getLocationMocker(appContext, useFakeDeviceLocationProvider)

    /**
     * Non-null only when this rule was constructed with `useFakeDeviceLocationProvider = true`.
     * `withMapboxNavigation` (see `MapboxNavigationCreator.kt`) already defaults to this value, so
     * most tests don't need to reference it directly.
     */
    val locationOptions: LocationOptions? = locationMocker.locationOptions

    override fun before() {
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
