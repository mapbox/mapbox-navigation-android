package com.mapbox.navigation.ui.androidauto.internal.search

import android.os.Handler
import android.os.Looper
import com.mapbox.common.Cancelable
import com.mapbox.common.location.GetLocationCallback
import com.mapbox.common.location.Location
import com.mapbox.common.location.LocationProvider
import com.mapbox.geojson.Point
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import java.util.concurrent.ConcurrentHashMap

private typealias CommonLocationObserver = com.mapbox.common.location.LocationObserver

class CarSearchLocationProvider : LocationProvider, MapboxNavigationObserver {

    var location: LocationMatcherResult? = null
        private set
    var point: Point? = null
        get() = location?.let {
            Point.fromLngLat(
                it.enhancedLocation.longitude,
                it.enhancedLocation.latitude,
            )
        }
        private set

    private val observers = ConcurrentHashMap<CommonLocationObserver, () -> Looper?>()

    private val locationObserver = object : LocationObserver {
        override fun onNewRawLocation(rawLocation: Location) {
            // no op
        }

        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            location = locationMatcherResult
            observers.forEach { (callback, looper) ->
                notifyCallback(locationMatcherResult, callback, looper())
            }
        }
    }

    private fun notifyCallback(
        locationMatcherResult: LocationMatcherResult,
        observer: CommonLocationObserver,
        looper: Looper?,
    ) {
        val func = {
            observer.onLocationUpdateReceived(locationMatcherResult.keyPoints)
        }

        if (looper != null) {
            Handler(looper).post(func)
        } else {
            func()
        }
    }

    override fun addLocationObserver(observer: CommonLocationObserver) {
        observers[observer] = { null }
    }

    override fun addLocationObserver(
        observer: com.mapbox.common.location.LocationObserver,
        looper: Looper,
    ) {
        observers[observer] = { looper }
    }

    override fun removeLocationObserver(observer: com.mapbox.common.location.LocationObserver) {
        observers.remove(observer)
    }

    override fun getLastLocation(
        callback: GetLocationCallback,
    ): Cancelable {
        callback.run(location?.enhancedLocation)
        return Cancelable {}
    }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.registerLocationObserver(locationObserver)
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.unregisterLocationObserver(locationObserver)
    }
}
