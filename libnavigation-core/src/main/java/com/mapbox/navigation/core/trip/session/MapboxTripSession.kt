package com.mapbox.navigation.core.trip.session

import android.location.Location
import android.os.Looper
import android.util.Log
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.trip.service.TripService
import com.mapbox.navigation.navigator.MapboxNativeNavigator
import com.mapbox.navigation.navigator.MapboxNativeNavigatorImpl
import com.mapbox.navigation.navigator.TripStatus
import com.mapbox.navigation.utils.extensions.ifNonNull
import com.mapbox.navigation.utils.thread.JobControl
import com.mapbox.navigation.utils.thread.ThreadController
import java.util.Date
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// todo make internal
class MapboxTripSession(
    override val tripService: TripService,
    override val locationEngine: LocationEngine,
    override val locationEngineRequest: LocationEngineRequest,
    private val navigatorPollingDelay: Long,
    private val navigator: MapboxNativeNavigator = MapboxNativeNavigatorImpl,
    threadController: ThreadController = ThreadController
) : TripSession {

    private val STATUS_POLLING_INTERVAL = 1000L
    override var route: DirectionsRoute? = null
        set(value) {
            field = value
            if (value != null) {
                ioJobController.scope.launch {
                    navigator.setRoute(value)
                }
            }
        }
    private val ioJobController: JobControl = threadController.getIOScopeAndRootJob()
    private val mainJobController: JobControl = threadController.getMainScopeAndRootJob()

    private val locationObservers = CopyOnWriteArrayList<LocationObserver>()
    private val routeProgressObservers = CopyOnWriteArrayList<RouteProgressObserver>()
    private val offRouteObservers = CopyOnWriteArrayList<OffRouteObserver>()
    private val stateObservers = CopyOnWriteArrayList<TripSessionStateObserver>()
    private val bannerInstructionsObservers = CopyOnWriteArrayList<BannerInstructionsObserver>()
    private val voiceInstructionsObservers = CopyOnWriteArrayList<VoiceInstructionsObserver>()

    private val bannerInstructionEvent = BannerInstructionEvent()
    private val voiceInstructionEvent = VoiceInstructionEvent()

    private var isStarted: Boolean = false
        set(value) {
            if (field == value) {
                return
            }
            field = value
            when (field) {
                true -> stateObservers.forEach { it.onSessionStarted() }
                false -> stateObservers.forEach { it.onSessionStopped() }
            }
        }

    private var isOffRoute: Boolean = false
        set(value) {
            if (field == value) {
                return
            }
            field = value
            offRouteObservers.forEach { it.onOffRouteStateChanged(value) }
        }

    private var rawLocation: Location? = null
    private var enhancedLocation: Location? = null
    private var routeProgress: RouteProgress? = null

    override fun getRawLocation() = rawLocation

    override fun getEnhancedLocation() = enhancedLocation

    override fun getRouteProgress() = routeProgress

    override fun start() {
        if (isStarted) {
            return
        }
        tripService.startService()
        startLocationUpdates()
        isStarted = true
    }

    private fun startLocationUpdates() {
        locationEngine.requestLocationUpdates(
            locationEngineRequest,
            locationEngineCallback,
            Looper.getMainLooper()
        )
        locationEngine.getLastLocation(locationEngineCallback)
    }

    override fun stop() {
        if (!isStarted) {
            return
        }
        tripService.stopService()
        stopLocationUpdates()
        ioJobController.job.cancelChildren()
        mainJobController.job.cancelChildren()
        reset()
        isStarted = false
    }

    private fun stopLocationUpdates() {
        locationEngine.removeLocationUpdates(locationEngineCallback)
    }

    private fun reset() {
        route = null
        rawLocation = null
        enhancedLocation = null
        routeProgress = null
        isOffRoute = false
    }

    override fun registerLocationObserver(locationObserver: LocationObserver) {
        locationObservers.add(locationObserver)
        rawLocation?.let { locationObserver.onRawLocationChanged(it) }
        enhancedLocation?.let { locationObserver.onEnhancedLocationChanged(it, emptyList()) }
    }

    override fun unregisterLocationObserver(locationObserver: LocationObserver) {
        locationObservers.remove(locationObserver)
    }

    override fun unregisterAllLocationObservers() {
        locationObservers.clear()
    }

    override fun registerRouteProgressObserver(routeProgressObserver: RouteProgressObserver) {
        routeProgressObservers.add(routeProgressObserver)
        routeProgress?.let { routeProgressObserver.onRouteProgressChanged(it) }
    }

    override fun unregisterRouteProgressObserver(routeProgressObserver: RouteProgressObserver) {
        routeProgressObservers.remove(routeProgressObserver)
    }

    override fun unregisterAllRouteProgressObservers() {
        routeProgressObservers.clear()
    }

    override fun registerOffRouteObserver(offRouteObserver: OffRouteObserver) {
        offRouteObservers.add(offRouteObserver)
        offRouteObserver.onOffRouteStateChanged(isOffRoute)
    }

    override fun unregisterOffRouteObserver(offRouteObserver: OffRouteObserver) {
        offRouteObservers.add(offRouteObserver)
    }

    override fun unregisterAllOffRouteObservers() {
        offRouteObservers.clear()
    }

    override fun registerStateObserver(stateObserver: TripSessionStateObserver) {
        stateObservers.add(stateObserver)
        if (isStarted) {
            stateObserver.onSessionStarted()
        } else {
            stateObserver.onSessionStopped()
        }
    }

    override fun unregisterStateObserver(stateObserver: TripSessionStateObserver) {
        stateObservers.remove(stateObserver)
    }

    override fun unregisterAllStateObservers() {
        stateObservers.clear()
    }

    override fun registerBannerInstructionsObserver(bannerInstructionsObserver: BannerInstructionsObserver) {
        bannerInstructionsObservers.add(bannerInstructionsObserver)
        routeProgress?.let {
            checkBannerInstructionEvent(it) { bannerInstruction ->
                bannerInstructionsObserver.onNewBannerInstructions(bannerInstruction)
            }
        }
    }

    override fun unregisterAllBannerInstructionsObservers() {
        bannerInstructionsObservers.clear()
    }

    override fun unregisterBannerInstructionsObserver(bannerInstructionsObserver: BannerInstructionsObserver) {
        bannerInstructionsObservers.remove(bannerInstructionsObserver)
    }

    override fun registerVoiceInstructionsObserver(voiceInstructionsObserver: VoiceInstructionsObserver) {
        voiceInstructionsObservers.add(voiceInstructionsObserver)
        routeProgress?.let {
            checkVoiceInstructionEvent(it) { voiceInstruction ->
                voiceInstructionsObserver.onNewVoiceInstructions(voiceInstruction)
            }
        }
    }

    override fun unregisterVoiceInstructionsObserver(voiceInstructionsObserver: VoiceInstructionsObserver) {
        voiceInstructionsObservers.remove(voiceInstructionsObserver)
    }

    override fun unregisterAllVoiceInstructionsObservers() {
        voiceInstructionsObservers.clear()
    }

    private var locationEngineCallback = object : LocationEngineCallback<LocationEngineResult> {
        override fun onSuccess(result: LocationEngineResult?) {
            result?.locations?.firstOrNull()?.let {
                updateRawLocation(it)
            }
        }

        override fun onFailure(exception: Exception) {
            Log.d("DEBUG", "location on failure", exception)
            stopLocationUpdates()
        }
    }

    private fun updateRawLocation(rawLocation: Location) {
        ioJobController.scope.launch {
            rawLocation.let {
                navigator.updateLocation(it)
            }
        }
        locationObservers.forEach { it.onRawLocationChanged(rawLocation) }
        if (this.rawLocation == null) {
            fireOffStatusPolling()
        }
        this.rawLocation = rawLocation
    }

    private fun fireOffStatusPolling() {
        mainJobController.scope.launch {
            while (isActive) {
                val status = navigatorPolling()
                updateEnhancedLocation(status.enhancedLocation, status.keyPoints)
                updateRouteProgress(status.routeProgress)
                isOffRoute = status.offRoute
                delay(STATUS_POLLING_INTERVAL)
            }
        }
    }

    private suspend fun navigatorPolling(): TripStatus =
        withContext(ioJobController.scope.coroutineContext) {
            val date = Date()
            date.time = date.time + navigatorPollingDelay
            navigator.getStatus(date)
        }

    private fun updateEnhancedLocation(location: Location, keyPoints: List<Location>) {
        enhancedLocation = location
        locationObservers.forEach { it.onEnhancedLocationChanged(location, keyPoints) }
    }

    private fun updateRouteProgress(progress: RouteProgress) {
        routeProgress = progress
        tripService.updateNotification(progress)
        routeProgressObservers.forEach { it.onRouteProgressChanged(progress) }
        checkBannerInstructionEvent(progress) { bannerInstruction ->
            bannerInstructionsObservers.forEach {
                it.onNewBannerInstructions(bannerInstruction)
            }
        }
        checkVoiceInstructionEvent(progress) { voiceInstruction ->
            voiceInstructionsObservers.forEach {
                it.onNewVoiceInstructions(voiceInstruction)
            }
        }
    }

    private fun checkBannerInstructionEvent(
        progress: RouteProgress,
        action: (BannerInstructions) -> Unit
    ) {
        if (bannerInstructionEvent.isOccurring(progress)) {
            ifNonNull(bannerInstructionEvent.bannerInstructions) {
                action(it)
            }
        }
    }

    private fun checkVoiceInstructionEvent(
        progress: RouteProgress,
        action: (VoiceInstructions) -> Unit
    ) {
        if (voiceInstructionEvent.isOccurring(progress)) {
            action(voiceInstructionEvent.voiceInstructions)
        }
    }
}
