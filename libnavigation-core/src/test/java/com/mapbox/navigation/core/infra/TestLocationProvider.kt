package com.mapbox.navigation.core.infra

import android.app.PendingIntent
import android.os.Looper
import com.mapbox.common.Cancelable
import com.mapbox.common.location.DeviceLocationProvider
import com.mapbox.common.location.GetLocationCallback
import com.mapbox.common.location.Location
import com.mapbox.common.location.LocationObserver
import com.mapbox.navigation.testing.factories.createLocation

class TestLocationProvider private constructor() : DeviceLocationProvider {

    companion object {

        fun create() = TestLocationProvider()
    }

    private val locationObservers = mutableListOf<LocationObserver>()
    private var currentLocations = listOf(createLocation())

    override fun addLocationObserver(observer: LocationObserver) {
        locationObservers.add(observer)
    }

    override fun addLocationObserver(observer: LocationObserver, looper: Looper) {
        locationObservers.add(observer)
    }

    override fun removeLocationObserver(observer: LocationObserver) {
        locationObservers.remove(observer)
    }

    override fun removeLocationUpdates(pendingIntent: PendingIntent) {
        throw UnsupportedOperationException()
    }

    override fun requestLocationUpdates(pendingIntent: PendingIntent) {
        throw UnsupportedOperationException()
    }

    override fun getLastLocation(callback: GetLocationCallback): Cancelable {
        callback.run(currentLocations.firstOrNull())
        return Cancelable {}
    }

    fun updateLocation(location: Location) {
        currentLocations = listOf(location)
        locationObservers.forEach { it.onLocationUpdateReceived(currentLocations) }
    }
}
