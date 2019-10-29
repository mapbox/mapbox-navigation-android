package com.mapbox.navigation

import android.location.Location
import android.os.Handler
import android.os.Looper
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.navigation.base.route.model.Route
import com.mapbox.navigation.base.trip.RouteProgress
import com.mapbox.navigation.base.trip.TripService
import com.mapbox.navigation.base.trip.TripSession
import com.mapbox.navigation.navigator.MapboxNativeNavigator
import java.lang.ref.WeakReference
import java.util.Date
import java.util.concurrent.CopyOnWriteArrayList

class DefaultTripSession(
    override val tripService: TripService,
    override val locationEngine: LocationEngine,
    override val locationEngineRequest: LocationEngineRequest,
    private val navigator: MapboxNativeNavigator,
    private val mainHandler: Handler,
    private val workerHandler: Handler
) : TripSession {

    override var route: Route? = null
        set(value) {
            field = value
            if (value != null) {
                workerHandler.post { navigator.setRoute(value) }
            }
        }

    private val mainLocationCallback = MainLocationCallback(this)

    private val locationObservers = CopyOnWriteArrayList<TripSession.LocationObserver>()
    private val routeProgressObservers = CopyOnWriteArrayList<TripSession.RouteProgressObserver>()

    private var rawLocation: Location? = null
    private var enhancedLocation: Location? = null
    private var routeProgress: RouteProgress? = null

    private val serviceStateListener = object : TripService.StateListener {
        override fun onStateChanged(state: Any) {
            TODO("not implemented")
        }
    }

    private val navigatorPollingRunnable = object : Runnable {
        override fun run() {
            val status = navigator.getStatus(Date())
            mainHandler.post {
                updateEnhancedLocation(status.enhancedLocation)
                updateRouteProgress(status.routeProgress)
            }
            workerHandler.postDelayed(this, STATUS_POLLING_INTERVAL)
        }
    }

    private val navigatorLocationUpdateRunnable = Runnable {
        rawLocation?.let { navigator.updateLocation(it) }
    }

    override fun getRawLocation() = rawLocation

    override fun getEnhancedLocation() = enhancedLocation

    override fun getRouteProgress() = routeProgress

    override fun start() {
        tripService.startService(serviceStateListener)
        locationEngine.requestLocationUpdates(locationEngineRequest, mainLocationCallback, Looper.getMainLooper())
        workerHandler.postDelayed(navigatorPollingRunnable, STATUS_POLLING_INTERVAL)
    }

    override fun stop() {
        tripService.stopService()
        locationEngine.removeLocationUpdates(mainLocationCallback)
        workerHandler.removeCallbacks(navigatorPollingRunnable)
        workerHandler.removeCallbacks(navigatorLocationUpdateRunnable)
    }

    override fun registerLocationObserver(locationObserver: TripSession.LocationObserver) {
        locationObservers.add(locationObserver)
        rawLocation?.let { locationObserver.onRawLocationChanged(it) }
        enhancedLocation?.let { locationObserver.onEnhancedLocationChanged(it) }
    }

    override fun unregisterLocationObserver(locationObserver: TripSession.LocationObserver) {
        locationObservers.remove(locationObserver)
    }

    override fun registerRouteProgressObserver(routeProgressObserver: TripSession.RouteProgressObserver) {
        routeProgressObservers.add(routeProgressObserver)
        routeProgress?.let { routeProgressObserver.onRouteProgressChanged(it) }
    }

    override fun unregisterRouteProgressObserver(routeProgressObserver: TripSession.RouteProgressObserver) {
        routeProgressObservers.remove(routeProgressObserver)
    }

    private class MainLocationCallback(tripSession: DefaultTripSession) : LocationEngineCallback<LocationEngineResult> {

        private val tripSessionReference = WeakReference(tripSession)

        override fun onSuccess(result: LocationEngineResult?) {
            result?.locations?.firstOrNull()?.let { tripSessionReference.get()?.updateRawLocation(it) }
        }

        override fun onFailure(exception: Exception) {
            TODO("not implemented")
        }
    }

    private fun updateRawLocation(rawLocation: Location) {
        this.rawLocation = rawLocation
        workerHandler.post(navigatorLocationUpdateRunnable)
        locationObservers.forEach { it.onRawLocationChanged(rawLocation) }
    }

    private fun updateEnhancedLocation(enhancedLocation: Location) {
        this.enhancedLocation = enhancedLocation
        locationObservers.forEach { it.onEnhancedLocationChanged(enhancedLocation) }
    }

    private fun updateRouteProgress(routeProgress: RouteProgress) {
        this.routeProgress = routeProgress
        routeProgressObservers.forEach { it.onRouteProgressChanged(routeProgress) }
    }

    companion object {
        private const val STATUS_POLLING_INTERVAL = 1000L
    }
}
