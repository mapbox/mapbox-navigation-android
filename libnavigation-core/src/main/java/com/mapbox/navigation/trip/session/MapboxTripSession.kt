package com.mapbox.navigation.trip.session

import android.location.Location
import android.os.Handler
import android.os.Looper
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.base.route.model.Route
import com.mapbox.navigation.base.route.util.redesignRouteOptions
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.directions.session.DirectionsSession
import com.mapbox.navigation.directions.session.MapboxDirectionsSession
import com.mapbox.navigation.navigator.MapboxNativeNavigator
import com.mapbox.navigation.trip.service.TripService
import com.mapbox.navigation.utils.extensions.ifNonNull
import com.mapbox.navigation.utils.thread.WorkThreadHandler
import java.lang.ref.WeakReference
import java.util.Date
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.TimeUnit

internal class MapboxTripSession(
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
    private val fasterRouteListeners = CopyOnWriteArraySet<TripSession.FasterRouteListener>()

    private var rawLocation: Location? = null
    private var enhancedLocation: Location? = null
    private var routeProgress: RouteProgress? = null

    private var fasterRouteExamine: FasterRouteExamine = FasterRouteExamine.Impl()
    private var fasterRouteFetcher: DirectionsSession? = null
    private val fasterRouteThreadHandler = WorkThreadHandler()

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

    override fun getRawLocation() = rawLocation

    override fun getEnhancedLocation() = enhancedLocation

    override fun getRouteProgress() = routeProgress

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

    override fun enableFasterRouteListener(router: Router) {
        fasterRouteFetcher = MapboxDirectionsSession(router, FasterRouteObserver())
        fasterRouteThreadHandler.post { fetchFasterRoute() }
    }

    override fun disableFasterRouteListener() {
        fasterRouteFetcher = null
        fasterRouteThreadHandler.removeAllTasks()
    }

    override fun registerFasterRouteListener(fasterRouteListener: TripSession.FasterRouteListener) {
        fasterRouteListeners.add(fasterRouteListener)
    }

    override fun unregisterFasterRouteListener(fasterRouteListener: TripSession.FasterRouteListener) {
        fasterRouteListeners.remove(fasterRouteListener)
    }

    override fun unregisterAllFasterRouteListeners() {
        fasterRouteListeners.clear()
    }

    private fun fetchFasterRoute() {
        val routeOption = routeProgress?.let { route?.redesignRouteOptions(it) }
        ifNonNull(fasterRouteFetcher, routeOption) { routeFetcher, routeOptions ->
            routeFetcher.requestRoutes(routeOptions)
        }
        fasterRouteThreadHandler.postDelayed(
            { fetchFasterRoute() },
            FASTER_ROUTE_FETCH_PERIOD_MILLS
        )
    }

    private class MainLocationCallback(tripSession: MapboxTripSession) :
        LocationEngineCallback<LocationEngineResult> {

        private val tripSessionReference = WeakReference(tripSession)

        override fun onSuccess(result: LocationEngineResult?) {
            result?.locations?.firstOrNull()
                ?.let { tripSessionReference.get()?.updateRawLocation(it) }
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
        private val FASTER_ROUTE_FETCH_PERIOD_MILLS = TimeUnit.MINUTES.toMillis(10)
    }

    internal inner class FasterRouteObserver : DirectionsSession.RouteObserver {
        override fun onRoutesChanged(routes: List<Route>) {
            ifNonNull(route, routeProgress, routes.firstOrNull()) { oldRoute, routeProgress, newRoute ->
                if (fasterRouteExamine.isRouteFaster(oldRoute, routeProgress, newRoute)) {
                    fasterRouteListeners.forEach { it.onFasterRouteFound(newRoute) }
                }
            }
        }

        override fun onRoutesRequested() = Unit

        override fun onRoutesRequestFailure(throwable: Throwable) = Unit
    }
}
