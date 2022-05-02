package com.mapbox.navigation.qa_test_app

import android.app.PendingIntent
import android.location.Location
import android.os.Handler
import android.os.Looper
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.location.LocationEngineResult
import java.util.concurrent.ConcurrentHashMap

// TODO probably can be removed once https://github.com/mapbox/mapbox-search-sdk/issues/671 fixed
class SearchLocationProvider : LocationEngine {

    private var location: Location? = null
    private val callbacks =
        ConcurrentHashMap<LocationEngineCallback<LocationEngineResult>, Looper?>()

    internal fun setLocation(newValue: Location) {
        location = newValue
        callbacks.forEach { (callback, looper) ->
            notifyCallback(location, callback, looper)
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
}
