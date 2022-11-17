package com.mapbox.navigation.testing.ui

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient

internal class FusedLocationMocker(
    appContext: Context
) : LocationMocker {

    private val fusedLocationProviderClient = FusedLocationProviderClient(appContext)

    @SuppressLint("MissingPermission")
    override fun before() {
        try {
            fusedLocationProviderClient.setMockMode(true)
        } catch (ex: Throwable) {
            // unstable
            Log.w("FusedLocationMocker", "could not set mock mode to true")
        }
    }

    @SuppressLint("MissingPermission")
    override fun after() {
        try {
            fusedLocationProviderClient.setMockMode(false)
        } catch (ex: Throwable) {
            Log.w("FusedLocationMocker", "could not set mock mode to false")
        }
    }

    @SuppressLint("MissingPermission")
    override fun mockLocation(location: Location) {
        fusedLocationProviderClient.setMockLocation(location)
    }

    override fun generateDefaultLocation(): Location = Location("fused")
}
