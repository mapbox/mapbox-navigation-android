package com.mapbox.navigation.testing.ui

import android.content.Context
import android.location.Location
import com.mapbox.navigation.base.options.LocationOptions

internal interface LocationMocker {

    fun before()

    fun after()

    fun mockLocation(location: Location)

    fun generateDefaultLocation(): Location

    /**
     * Non-null only for mockers that don't rely on the real platform location providers.
     * Pass this into [com.mapbox.navigation.base.options.NavigationOptions.Builder.locationOptions]
     * so a [MapboxNavigation] instance reads from this mocker instead of the real platform.
     */
    val locationOptions: LocationOptions? get() = null
}

internal object LocationMockerProvider {

    fun getLocationMocker(context: Context, useFakeDeviceLocationProvider: Boolean): LocationMocker {
        if (useFakeDeviceLocationProvider) {
            return FakeLocationMocker()
        }

        val fusedLocationMocker = try {
            Class.forName("com.google.android.gms.location.FusedLocationProviderClient")
            FusedLocationMocker(context)
        } catch (ex: Throwable) {
            null
        }
        return DualLocationMocker(
            context,
            SystemLocationMocker(context),
            fusedLocationMocker
        )
    }
}
