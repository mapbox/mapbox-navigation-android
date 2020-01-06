package com.mapbox.navigation.trip.session

import android.location.Location
import android.os.Looper
import android.util.Log
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.navigation.base.route.model.Route
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.navigator.MapboxNativeNavigator
import com.mapbox.navigation.trip.service.TripService
import com.mapbox.navigation.utils.JobControl
import com.mapbox.navigation.utils.ThreadController
import java.lang.ref.WeakReference
import java.util.Date
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
class MapboxTripSession(
    override val tripService: TripService,
    override val locationEngine: LocationEngine,
    override val locationEngineRequest: LocationEngineRequest,
    private val navigator: MapboxNativeNavigator
) : TripSession {

    override var route: Route? = null
        set(value) {
            field = value
            if (value != null) {
                ioJobController.scope.launch { navigator.setRoute(value) }
            }
        }
    private val ioJobController: JobControl = ThreadController.getScopeAndRootJob()
    private val mainJobController: JobControl =
        ThreadController.getMainScopeAndRootJob()
    private val mainLocationCallback =
        MainLocationCallback(this)

    private val locationObservers = CopyOnWriteArrayList<TripSession.LocationObserver>()
    private val routeProgressObservers = CopyOnWriteArrayList<TripSession.RouteProgressObserver>()

    private var rawLocation: Location? = null
    private var enhancedLocation: Location? = null
    private var routeProgress: RouteProgress? = null

//    private val serviceStateListener = object : TripService.StateListener {
//        override fun onStateChanged(state: Any) {
//            TODO("not implemented")
//        }
//    }

    private var listenLocationUpdatesJob: Job = Job()

    override fun getRawLocation() = rawLocation

    override fun getEnhancedLocation() = enhancedLocation

    override fun getRouteProgress() = routeProgress

    // TODO Remove / integrate as part of start()
    //  Currently temporal for testing purposes
    fun startLocationUpdates() {
        if (!locationChannel.isClosedForSend) {
            locationChannel.cancel()
        }
        locationChannel = Channel(CONFLATED)
        locationEngine.requestLocationUpdates(
            locationEngineRequest,
            locationEngineCallback,
            Looper.getMainLooper()
        )
        listenLocationUpdates()
    }

    override fun start() {
        tripService.startService()
        locationEngine.requestLocationUpdates(
            locationEngineRequest,
            mainLocationCallback,
            Looper.getMainLooper()
        )
        ioJobController.scope.launch {
            while (isActive) {
                navigatorPolling()
                delay(STATUS_POLLING_INTERVAL)
            }
        }
    }

    fun navigatorPolling() {
        val status = navigator.getStatus(Date())
        updateEnhancedLocation(status.enhancedLocation)
        updateRouteProgress(status.routeProgress)
    }

    private fun listenLocationUpdates() {
        listenLocationUpdatesJob = mainJobController.scope.launch {
            while (isActive) {
                when (!locationChannel.isClosedForReceive) {
                    true -> {
                        val location = locationChannel.receive()
                        Log.d("DEBUG", "$location")
                        updateLocation(location)
                    }
                    false -> {
                        Log.d(
                            "DEBUG",
                            "location channel is closed for receive"
                        )
                    }
                }
            }
        }
    }

    override fun stop() {
        tripService.stopService()
        locationEngine.removeLocationUpdates(mainLocationCallback)
        ioJobController.job.cancel()
        mainJobController.job.cancel()
    }

    // TODO Remove / integrate as part of stop()
    //  Currently temporal for testing purposes
    fun stopLocationUpdates() {
        locationEngine.removeLocationUpdates(locationEngineCallback)
        listenLocationUpdatesJob.cancel()
        if (!locationChannel.isClosedForSend) {
            locationChannel.cancel()
        }
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

    private var locationEngineCallback = object : LocationEngineCallback<LocationEngineResult> {
        override fun onSuccess(result: LocationEngineResult?) {
            result?.locations?.firstOrNull()?.let {
                when (!locationChannel.isClosedForSend) {
                    true -> locationChannel.offer(it)
                    false -> Log.d(
                        "DEBUG",
                        "location channel is closed for send"
                    )
                }
            }
        }

        override fun onFailure(exception: Exception) {
            Log.d("DEBUG", "location on failure", exception)
            stopLocationUpdates()
        }
    }

    // TODO Remove, will be replaced by locationEngineCallback
    //  Currently duplicated / not used for testing purposes
    private class MainLocationCallback(tripSession: MapboxTripSession) :
        LocationEngineCallback<LocationEngineResult> {

        private val tripSessionReference = WeakReference(tripSession)

        override fun onSuccess(result: LocationEngineResult?) {
            result?.locations?.firstOrNull()?.let {
                tripSessionReference.get()?.updateRawLocation(it)
            }
        }

        override fun onFailure(exception: Exception) {
            TODO("not implemented")
        }
    }

    // TODO Remove / integrate as part of updateRawLocation()
    //  Currently temporal for testing purposes
    private fun updateLocation(rawLocation: Location) {
        locationObservers.forEach { it.onRawLocationChanged(rawLocation) }
    }

    private fun updateRawLocation(rawLocation: Location) {
        this.rawLocation = rawLocation
        ioJobController.scope.launch { rawLocation.let { navigator.updateLocation(it) } }
        locationObservers.forEach { it.onRawLocationChanged(rawLocation) }
    }

    private fun updateEnhancedLocation(location: Location) {
        ThreadController.getMainScopeAndRootJob().scope.launch {
            enhancedLocation = location
            locationObservers.forEach { it.onEnhancedLocationChanged(location) }
        }
    }

    private fun updateRouteProgress(progress: RouteProgress) {
        ThreadController.getMainScopeAndRootJob().scope.launch {
            routeProgress = progress
            routeProgressObservers.forEach { it.onRouteProgressChanged(progress) }
        }
    }

    companion object {
        private const val STATUS_POLLING_INTERVAL = 1000L
        private var locationChannel = Channel<Location>(CONFLATED)
    }
}
