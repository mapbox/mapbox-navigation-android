package com.mapbox.navigation.trip.session

import android.location.Location
import android.os.Handler
import android.os.Looper
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.navigation.base.logger.model.Message
import com.mapbox.navigation.base.logger.model.Tag
import com.mapbox.navigation.base.route.model.Route
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.logger.MapboxLogger
import com.mapbox.navigation.navigator.MapboxNativeNavigator
import com.mapbox.navigation.trip.service.TripService
import java.lang.ref.WeakReference
import java.util.Date
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
class MapboxTripSession(
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

    private val navigatorPollingRunnable = object : Runnable {
        override fun run() {
            val status = navigator.getStatus(Date())
            mainHandler.post {
                updateEnhancedLocation(status.enhancedLocation)
                updateRouteProgress(status.routeProgress)
            }
            workerHandler.postDelayed(
                this,
                STATUS_POLLING_INTERVAL
            )
        }
    }

    private val navigatorLocationUpdateRunnable = Runnable {
        rawLocation?.let { navigator.updateLocation(it) }
    }

    private var listenLocationUpdatesJob: Job = Job()

    override fun getRawLocation() = rawLocation

    override fun getEnhancedLocation() = enhancedLocation

    override fun getRouteProgress() = routeProgress

    // TODO Remove / integrate as part of start()
    //  Currently temporal for testing purposes
    fun startLocationUpdates() {
        if (!locationChannel.isClosedForSend) {
            locationChannel.close()
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
        workerHandler.postDelayed(
            navigatorPollingRunnable,
            STATUS_POLLING_INTERVAL
        )
    }

    private fun listenLocationUpdates() {
        listenLocationUpdatesJob = locationScope.launch {
            while (isActive) {
                when (!locationChannel.isClosedForReceive) {
                    true -> {
                        val location = locationChannel.receive()
                        MapboxLogger.d(Tag("DEBUG"), Message("$location"))
                        updateLocation(location)
                    }
                    false -> {
                        MapboxLogger.d(
                            Tag("DEBUG"),
                            Message("location channel is closed for receive")
                        )
                    }
                }
            }
        }
    }

    override fun stop() {
        tripService.stopService()
        locationEngine.removeLocationUpdates(mainLocationCallback)
        workerHandler.removeCallbacks(navigatorPollingRunnable)
        workerHandler.removeCallbacks(navigatorLocationUpdateRunnable)
    }

    // TODO Remove / integrate as part of stop()
    //  Currently temporal for testing purposes
    fun stopLocationUpdates() {
        locationEngine.removeLocationUpdates(locationEngineCallback)
        listenLocationUpdatesJob.cancel()
        if (!locationChannel.isClosedForSend) {
            locationChannel.close()
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
                    false -> MapboxLogger.d(
                        Tag("DEBUG"),
                        Message("location channel is closed for send")
                    )
                }
            }
        }

        override fun onFailure(exception: Exception) {
            MapboxLogger.d(Tag("DEBUG"), Message("location on failure"), exception)
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
        private var locationChannel = Channel<Location>(CONFLATED)
        private val job = SupervisorJob()
        private val locationScope = CoroutineScope(job + Dispatchers.Main)
    }
}
