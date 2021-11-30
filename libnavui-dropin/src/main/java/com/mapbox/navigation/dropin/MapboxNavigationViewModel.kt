package com.mapbox.navigation.dropin

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.route.RouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.arrival.ArrivalObserver
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper
import com.mapbox.navigation.core.trip.session.BannerInstructionsObserver
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

internal class MapboxNavigationViewModel(
    private val dropInUIMapboxNavigationFactory: DropInUIMapboxNavigationFactory
) : ViewModel(), DefaultLifecycleObserver {

    private val mapboxNavigation: MapboxNavigation by lazy {
        dropInUIMapboxNavigationFactory.getMapboxNavigation()
    }

    private val _rawLocationUpdates: MutableSharedFlow<Location> = MutableSharedFlow()
    fun rawLocationUpdates(): Flow<Location> = _rawLocationUpdates

    private val _newLocationMatcherResult: MutableSharedFlow<LocationMatcherResult> =
        MutableSharedFlow()
    val newLocationMatcherResults: Flow<LocationMatcherResult> = _newLocationMatcherResult

    private val _routeProgressUpdates: MutableSharedFlow<RouteProgress> = MutableSharedFlow()
    val routeProgressUpdates: Flow<RouteProgress> = _routeProgressUpdates

    private val _routesUpdatedResults: MutableSharedFlow<RoutesUpdatedResult> = MutableSharedFlow()
    val routesUpdatedResults: Flow<RoutesUpdatedResult> = _routesUpdatedResults

    private val _finalDestinationArrivals: MutableSharedFlow<RouteProgress> = MutableSharedFlow()
    val finalDestinationArrivals: Flow<RouteProgress> = _finalDestinationArrivals

    private val _nextRouteLegStartUpdates: MutableSharedFlow<RouteLegProgress> =
        MutableSharedFlow()
    val nextRouteLegStartUpdates: Flow<RouteLegProgress> = _nextRouteLegStartUpdates

    private val _waypointArrivals: MutableSharedFlow<RouteProgress> = MutableSharedFlow()
    val wayPointArrivals: Flow<RouteProgress> = _waypointArrivals

    private val _bannerInstructions: MutableSharedFlow<BannerInstructions> = MutableSharedFlow()
    val bannerInstructions: Flow<BannerInstructions> = _bannerInstructions

    private val _tripSessionStateUpdates: MutableSharedFlow<TripSessionState> = MutableSharedFlow()
    val tripSessionStateUpdates: Flow<TripSessionState> = _tripSessionStateUpdates

    private val _routeRequestFailures: MutableSharedFlow<List<RouterFailure>> = MutableSharedFlow()
    val routeRequestFailures: Flow<List<RouterFailure>> = _routeRequestFailures

    private val _voiceInstructions: MutableSharedFlow<VoiceInstructions> = MutableSharedFlow()
    val voiceInstructions: Flow<VoiceInstructions> = _voiceInstructions

    // This may be temporary. We need some way to start a trip session to further development.
    // This is here because this class has a reference to MapboxNavigation
    @SuppressLint("MissingPermission")
    fun startTripSession() {
        mapboxNavigation.startTripSession()
    }

    // This may be temporary. We need some way to start a trip session to further development.
    // This is here because this class has a reference to MapboxNavigation
    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    fun startSimulatedTripSession(location: Location) {
        stopTripSession()
        mapboxNavigation.mapboxReplayer.clearEvents()

        val events = if (mapboxNavigation.getRoutes().isEmpty()) {
            listOf(ReplayRouteMapper.mapToUpdateLocation(0.0, location))
        } else {
            ReplayRouteMapper().mapDirectionsRouteGeometry(mapboxNavigation.getRoutes().first())
        }

        mapboxNavigation.mapboxReplayer.pushEvents(events)
        mapboxNavigation.startReplayTripSession()
        mapboxNavigation.mapboxReplayer.play()
    }

    // This may be temporary. We need some way to stop a trip session to further development.
    // This is here because this class has a reference to MapboxNavigation
    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    fun stopTripSession() {
        mapboxNavigation.stopTripSession()
        mapboxNavigation.mapboxReplayer.stop()
    }

    // This was added to facilitate getting a route into mapbox navigation so work could go forward.
    // It may be temporary.
    fun findRoute(origin: Point, destination: Point, context: Context) {
        val routeOptions = RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .applyLanguageAndVoiceUnitOptions(context)
            .coordinatesList(listOf(origin, destination))
            .layersList(listOf(mapboxNavigation.getZLevel(), null))
            .alternatives(true)
            .build()
        mapboxNavigation.requestRoutes(
            routeOptions,
            object : RouterCallback {
                override fun onRoutesReady(
                    routes: List<DirectionsRoute>,
                    routerOrigin: RouterOrigin
                ) {
                    mapboxNavigation.setRoutes(routes)
                }

                override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {
                    viewModelScope.launch {
                        _routeRequestFailures.emit(reasons)
                    }
                }

                override fun onCanceled(routeOptions: RouteOptions, routerOrigin: RouterOrigin) {
                    //
                }
            }
        )
    }

    @VisibleForTesting
    private val locationObserver = object : LocationObserver {
        override fun onNewRawLocation(rawLocation: Location) {
            viewModelScope.launch {
                _rawLocationUpdates.emit(rawLocation)
            }
        }
        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            viewModelScope.launch {
                _newLocationMatcherResult.emit(locationMatcherResult)
            }
        }
    }

    private val routeProgressObserver = RouteProgressObserver {
        viewModelScope.launch {
            _routeProgressUpdates.emit(it)
        }
    }

    private val routesObserver = RoutesObserver {
        viewModelScope.launch {
            _routesUpdatedResults.emit(it)
        }
    }

    private val arrivalObserver = object : ArrivalObserver {
        override fun onFinalDestinationArrival(routeProgress: RouteProgress) {
            viewModelScope.launch {
                _finalDestinationArrivals.emit(routeProgress)
            }
        }

        override fun onNextRouteLegStart(routeLegProgress: RouteLegProgress) {
            viewModelScope.launch {
                _nextRouteLegStartUpdates.emit(routeLegProgress)
            }
        }

        override fun onWaypointArrival(routeProgress: RouteProgress) {
            viewModelScope.launch {
                _waypointArrivals.emit(routeProgress)
            }
        }
    }

    private val bannerInstructionsObserver = BannerInstructionsObserver { bannerInstructions ->
        viewModelScope.launch {
            _bannerInstructions.emit(bannerInstructions)
        }
    }

    private val tripSessionStateObserver = TripSessionStateObserver { tripSessionState ->
        viewModelScope.launch {
            _tripSessionStateUpdates.emit(tripSessionState)
        }
    }

    private val voiceInstructionsObserver = VoiceInstructionsObserver { voiceInstructions ->
        viewModelScope.launch {
            _voiceInstructions.emit(voiceInstructions)
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        mapboxNavigation.registerLocationObserver(locationObserver)
        mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.registerRoutesObserver(routesObserver)
        mapboxNavigation.registerArrivalObserver(arrivalObserver)
        mapboxNavigation.registerBannerInstructionsObserver(bannerInstructionsObserver)
        mapboxNavigation.registerTripSessionStateObserver(tripSessionStateObserver)
        mapboxNavigation.registerVoiceInstructionsObserver(voiceInstructionsObserver)
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        mapboxNavigation.unregisterLocationObserver(locationObserver)
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.unregisterRoutesObserver(routesObserver)
        mapboxNavigation.unregisterArrivalObserver(arrivalObserver)
        mapboxNavigation.unregisterBannerInstructionsObserver(bannerInstructionsObserver)
        mapboxNavigation.unregisterTripSessionStateObserver(tripSessionStateObserver)
        mapboxNavigation.unregisterVoiceInstructionsObserver(voiceInstructionsObserver)
    }

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    override fun onCleared() {
        super.onCleared()
        mapboxNavigation.mapboxReplayer.stop()
        mapboxNavigation.mapboxReplayer.finish()
        mapboxNavigation.stopTripSession()
        mapboxNavigation.onDestroy()
    }
}
