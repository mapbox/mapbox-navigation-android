package com.mapbox.navigation.testing.ui

import android.content.Context
import android.location.Location

internal interface LocationMocker {

    fun before()

    fun after()

    fun mockLocation(location: Location)

    fun generateDefaultLocation(): Location
}

internal object LocationMockerProvider {

    fun getLocationMocker(context: Context): LocationMocker {
        val fusedClientClass = try {
            Class.forName("com.google.android.gms.location.FusedLocationProviderClient")
        } catch (ex: Throwable) {
            null
        }
        return if (fusedClientClass == null) {
            SystemLocationMocker(context, "gps")
        } else {
            FusedLocationMocker(context)
        }
    }
}
