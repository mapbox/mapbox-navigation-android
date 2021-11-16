package com.mapbox.navigation.dropin.viewmodel

import android.location.Location
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.arrival.ArrivalObserver
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.navigation.core.trip.session.BannerInstructionsObserver
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.dropin.DropInUIMapboxNavigationFactory
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

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        mapboxNavigation.registerLocationObserver(locationObserver)
        mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.registerRoutesObserver(routesObserver)
        mapboxNavigation.registerArrivalObserver(arrivalObserver)
        mapboxNavigation.registerBannerInstructionsObserver(bannerInstructionsObserver)
        mapboxNavigation.registerTripSessionStateObserver(tripSessionStateObserver)
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        mapboxNavigation.unregisterLocationObserver(locationObserver)
        mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
        mapboxNavigation.unregisterRoutesObserver(routesObserver)
        mapboxNavigation.unregisterArrivalObserver(arrivalObserver)
        mapboxNavigation.unregisterBannerInstructionsObserver(bannerInstructionsObserver)
        mapboxNavigation.unregisterTripSessionStateObserver(tripSessionStateObserver)
    }

    override fun onCleared() {
        super.onCleared()
        mapboxNavigation.onDestroy()
    }
}
