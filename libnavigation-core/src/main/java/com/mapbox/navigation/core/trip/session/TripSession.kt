package com.mapbox.navigation.core.trip.session

import android.location.Location
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.trip.service.TripService
import com.mapbox.navigation.core.trip.session.eh.EHorizonObserver
import com.mapbox.navigator.FallbackVersionsObserver

internal interface TripSession {

    val tripService: TripService
    fun setRoute(
        route: DirectionsRoute?,
        legIndex: Int,
        @RoutesExtra.RoutesUpdateReason reason: String
    )

    fun getRawLocation(): Location?
    val locationMatcherResult: LocationMatcherResult?
    fun getRouteProgress(): RouteProgress?
    fun getState(): TripSessionState

    fun start(withTripService: Boolean, withReplayEnabled: Boolean = false)
    fun stop()
    fun isRunningWithForegroundService(): Boolean

    fun registerLocationObserver(locationObserver: LocationObserver)
    fun unregisterLocationObserver(locationObserver: LocationObserver)
    fun unregisterAllLocationObservers()

    fun registerRouteProgressObserver(routeProgressObserver: RouteProgressObserver)
    fun unregisterRouteProgressObserver(routeProgressObserver: RouteProgressObserver)
    fun unregisterAllRouteProgressObservers()

    fun registerOffRouteObserver(offRouteObserver: OffRouteObserver)
    fun unregisterOffRouteObserver(offRouteObserver: OffRouteObserver)
    fun unregisterAllOffRouteObservers()

    fun registerStateObserver(stateObserver: TripSessionStateObserver)
    fun unregisterStateObserver(stateObserver: TripSessionStateObserver)
    fun unregisterAllStateObservers()

    fun registerBannerInstructionsObserver(bannerInstructionsObserver: BannerInstructionsObserver)
    fun unregisterBannerInstructionsObserver(bannerInstructionsObserver: BannerInstructionsObserver)
    fun unregisterAllBannerInstructionsObservers()

    fun registerVoiceInstructionsObserver(voiceInstructionsObserver: VoiceInstructionsObserver)
    fun unregisterVoiceInstructionsObserver(voiceInstructionsObserver: VoiceInstructionsObserver)
    fun unregisterAllVoiceInstructionsObservers()

    fun updateLegIndex(legIndex: Int, callback: LegIndexUpdatedCallback)

    fun registerRoadObjectsOnRouteObserver(
        roadObjectsOnRouteObserver: RoadObjectsOnRouteObserver
    )
    fun unregisterRoadObjectsOnRouteObserver(
        roadObjectsOnRouteObserver: RoadObjectsOnRouteObserver
    )
    fun unregisterAllRoadObjectsOnRouteObservers()

    fun registerEHorizonObserver(eHorizonObserver: EHorizonObserver)
    fun unregisterEHorizonObserver(eHorizonObserver: EHorizonObserver)
    fun unregisterAllEHorizonObservers()

    fun registerFallbackVersionsObserver(fallbackVersionsObserver: FallbackVersionsObserver)
    fun unregisterFallbackVersionsObserver(fallbackVersionsObserver: FallbackVersionsObserver)
    fun unregisterAllFallbackVersionsObservers()
}
