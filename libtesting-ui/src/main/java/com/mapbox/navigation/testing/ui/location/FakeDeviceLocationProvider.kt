package com.mapbox.navigation.testing.ui.location

import android.app.PendingIntent
import android.os.Build
import android.os.Looper
import android.os.SystemClock
import com.mapbox.bindgen.ExpectedFactory
import com.mapbox.bindgen.Value
import com.mapbox.common.Cancelable
import com.mapbox.common.location.DeviceLocationProvider
import com.mapbox.common.location.DeviceLocationProviderFactory
import com.mapbox.common.location.GetLocationCallback
import com.mapbox.common.location.Location
import com.mapbox.common.location.LocationExtraKeys
import com.mapbox.common.location.LocationObserver
import com.mapbox.geojson.Point

/**
 * A fully controllable [DeviceLocationProvider] test double.
 *
 * This provider delivers a location to its registered observers synchronously, on the
 * calling thread, exactly when [emitLocation] is called - so a test can decide precisely when
 * the SDK receives a location, without racing any platform provider start-up.
 */
class FakeDeviceLocationProvider : DeviceLocationProvider {

    private val observers = mutableListOf<LocationObserver>()
    private var lastLocation: Location? = null

    override fun addLocationObserver(observer: LocationObserver) {
        observers.add(observer)
    }

    override fun addLocationObserver(observer: LocationObserver, looper: Looper) {
        observers.add(observer)
    }

    override fun removeLocationObserver(observer: LocationObserver) {
        observers.remove(observer)
    }

    override fun getLastLocation(callback: GetLocationCallback): Cancelable {
        callback.run(lastLocation)
        return Cancelable {}
    }

    override fun requestLocationUpdates(pendingIntent: PendingIntent) {
        throw UnsupportedOperationException()
    }

    override fun removeLocationUpdates(pendingIntent: PendingIntent) {
        throw UnsupportedOperationException()
    }

    /**
     * Synchronously delivers a location at [point] to every currently registered observer.
     */
    fun emitLocation(point: Point) {
        emit(
            Location.Builder()
                .latitude(point.latitude())
                .longitude(point.longitude())
                .timestamp(System.currentTimeMillis())
                .monotonicTimestamp(SystemClock.elapsedRealtimeNanos())
                .extra(Value.valueOf(hashMapOf(LocationExtraKeys.IS_MOCK to Value.valueOf(true))))
                .build(),
        )
    }

    /**
     * Synchronously delivers [location] to every currently registered observer, preserving all
     * the fields [location] carries (accuracy, speed, bearing, etc.) rather than just its
     * coordinates.
     */
    fun emitLocation(location: android.location.Location) {
        val builder = Location.Builder()
            .latitude(location.latitude)
            .longitude(location.longitude)
            .timestamp(location.time)
            .monotonicTimestamp(location.elapsedRealtimeNanos)
            .altitude(location.altitude)
            .horizontalAccuracy(location.accuracy.toDouble())
            .speed(location.speed.toDouble())
            .bearing(location.bearing.toDouble())
            .extra(Value.valueOf(hashMapOf(LocationExtraKeys.IS_MOCK to Value.valueOf(true))))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder
                .verticalAccuracy(location.verticalAccuracyMeters.toDouble())
                .speedAccuracy(location.speedAccuracyMetersPerSecond.toDouble())
                .bearingAccuracy(location.bearingAccuracyDegrees.toDouble())
        }
        emit(builder.build())
    }

    private fun emit(location: Location) {
        lastLocation = location
        observers.forEach { it.onLocationUpdateReceived(listOf(location)) }
    }
}

fun FakeDeviceLocationProvider.asLocationProviderFactory(): DeviceLocationProviderFactory =
    DeviceLocationProviderFactory { ExpectedFactory.createValue(this) }
