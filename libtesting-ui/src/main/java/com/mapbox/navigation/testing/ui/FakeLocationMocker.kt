package com.mapbox.navigation.testing.ui

import android.location.Location
import android.util.Log
import com.mapbox.navigation.base.options.LocationOptions
import com.mapbox.navigation.testing.ui.location.FakeDeviceLocationProvider
import com.mapbox.navigation.testing.ui.location.asLocationProviderFactory

/**
 * [LocationMocker] backed by an in-process [FakeDeviceLocationProvider] instead of the real
 * platform location providers (`LocationManager`/`FusedLocationProviderClient`). Delivers
 * locations to a [MapboxNavigation] instance synchronously, on the calling thread, without racing
 * the real platform provider's asynchronous start-up.
 */
internal class FakeLocationMocker : LocationMocker {

    private val fakeDeviceLocationProvider = FakeDeviceLocationProvider()

    override val locationOptions: LocationOptions = LocationOptions.Builder()
        .locationProviderFactory(
            fakeDeviceLocationProvider.asLocationProviderFactory(),
            LocationOptions.LocationProviderType.MOCKED,
        )
        .build()

    override fun before() {
        Log.d(TAG,"Using FakeDeviceLocationProvider for mock location updates")
        // nothing to set up: there is no real platform provider to authorize/start
    }

    override fun after() {
        // nothing to tear down
    }

    override fun mockLocation(location: Location) {
        fakeDeviceLocationProvider.emitLocation(location)
    }

    override fun generateDefaultLocation(): Location = Location(PROVIDER_NAME)

    private companion object {
        private const val PROVIDER_NAME = "fake"
        private const val TAG = "FakeLocationMocker"
    }
}
