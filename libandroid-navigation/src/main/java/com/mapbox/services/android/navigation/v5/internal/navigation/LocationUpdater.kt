package com.mapbox.services.android.navigation.v5.internal.navigation

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.location.LocationEngineResult
import java.lang.ref.WeakReference
import timber.log.Timber

@SuppressLint("MissingPermission")
internal class LocationUpdater(
    private val context: Context,
    private val thread: RouteProcessorBackgroundThread,
    private val dispatcher: NavigationEventDispatcher,
    private var locationEngine: LocationEngine,
    private var request: LocationEngineRequest
) {

    private val callback =
        CurrentLocationEngineCallback(
            this
        )

    init {
        requestInitialLocationUpdates(locationEngine, request)
    }

    fun updateLocationEngine(locationEngine: LocationEngine) {
        requestLocationUpdates(request, locationEngine)
        this.locationEngine = locationEngine
    }

    fun updateLocationEngineRequest(request: LocationEngineRequest) {
        requestLocationUpdates(request, locationEngine)
        this.request = request
    }

    fun onLocationChanged(location: Location?) {
        location?.let { currentLocation ->
            thread.updateLocation(currentLocation)
            dispatcher.onLocationUpdate(currentLocation)
            NavigationTelemetry.updateLocation(context, currentLocation)
        }
    }

    fun removeLocationUpdates() {
        locationEngine.removeLocationUpdates(callback)
    }

    @SuppressLint("MissingPermission")
    private fun requestInitialLocationUpdates(
        locationEngine: LocationEngine,
        request: LocationEngineRequest
    ) {
        locationEngine.requestLocationUpdates(request, callback, null)
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates(
        request: LocationEngineRequest,
        locationEngine: LocationEngine
    ) {
        this.locationEngine.removeLocationUpdates(callback)
        locationEngine.requestLocationUpdates(request, callback, null)
    }

    internal class CurrentLocationEngineCallback(locationUpdater: LocationUpdater) :
        LocationEngineCallback<LocationEngineResult> {

        private val updaterWeakReference:
        WeakReference<LocationUpdater> = WeakReference(locationUpdater)

        override fun onSuccess(result: LocationEngineResult) {
            updaterWeakReference.get()?.let { locationUpdater ->
                val location = result.lastLocation
                locationUpdater.onLocationChanged(location)
            }
        }

        override fun onFailure(exception: Exception) {
            Timber.e(exception)
        }
    }
}
