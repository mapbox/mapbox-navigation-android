package com.mapbox.navigation.qa_test_app.car.search

import android.app.PendingIntent
import android.location.Location
import android.os.Handler
import android.os.Looper
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import java.util.concurrent.ConcurrentHashMap

// TODO probably can be removed once https://github.com/mapbox/mapbox-search-sdk/issues/671 fixed
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class CarSearchLocationProvider : LocationEngine, MapboxNavigationObserver {

    private var location: Location? = null
    private val callbacks =
        ConcurrentHashMap<LocationEngineCallback<LocationEngineResult>, Looper?>()

    private val locationObserver = object : LocationObserver {
        override fun onNewRawLocation(rawLocation: Location) {
            // no op
        }

        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            location = locationMatcherResult.enhancedLocation
            callbacks.forEach { (callback, looper) ->
                notifyCallback(location, callback, looper)
            }
        }
    }

    private fun notifyCallback(
        location: Location?,
        callback: LocationEngineCallback<LocationEngineResult>,
        looper: Looper?
    ) {
        val callbackRunnable = Runnable {
            callback.onSuccess(LocationEngineResult.create(location))
        }

        if (looper != null) {
            Handler(looper).post(callbackRunnable)
        } else {
            callbackRunnable.run()
        }
    }

    override fun getLastLocation(
        locationEngineCallback: LocationEngineCallback<LocationEngineResult>
    ) {
        locationEngineCallback.onSuccess(LocationEngineResult.create(location))
    }

    override fun requestLocationUpdates(
        locationEngineRequest: LocationEngineRequest,
        locationEngineCallback: LocationEngineCallback<LocationEngineResult>,
        looper: Looper?
    ) {
        callbacks[locationEngineCallback] = looper
        if (location != null) {
            notifyCallback(location, locationEngineCallback, looper)
        }
    }

    override fun requestLocationUpdates(
        locationEngineRequest: LocationEngineRequest,
        pendingIntent: PendingIntent?
    ) {
        throw NotImplementedError()
    }

    override fun removeLocationUpdates(
        locationEngineCallback: LocationEngineCallback<LocationEngineResult>
    ) {
        callbacks.remove(locationEngineCallback)
    }

    override fun removeLocationUpdates(pendingIntent: PendingIntent?) {
        throw NotImplementedError()
    }

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.registerLocationObserver(locationObserver)
    }

    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        mapboxNavigation.unregisterLocationObserver(locationObserver)
    }
}
