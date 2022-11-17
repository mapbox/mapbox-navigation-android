package com.mapbox.navigation.testing.ui

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.util.Log

internal class SystemLocationMocker(
    private val context: Context,
    private val mockProviderName: String
): LocationMocker {

    private val locationManager: LocationManager by lazy {
        (context.getSystemService(Context.LOCATION_SERVICE) as LocationManager)
    }

    override fun before() {
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
            Log.w("SystemLocationMocker", "addTestProvider failed")
        }
        locationManager.setTestProviderEnabled(mockProviderName, true)
    }

    override fun after() {
        locationManager.setTestProviderEnabled(mockProviderName, false)
        locationManager.removeTestProvider(mockProviderName)
    }

    override fun mockLocation(location: Location) {
        check(location.provider == mockProviderName) {
            """
              location provider "${location.provider}" is not equal to required "$mockProviderName"
            """.trimIndent()
        }
        locationManager.setTestProviderLocation(mockProviderName, location)
    }

    override fun generateDefaultLocation(): Location = Location(mockProviderName)
}
