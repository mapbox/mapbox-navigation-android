package com.mapbox.navigation.core.trip.session

import android.hardware.SensorEvent
import android.location.Location
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.trip.service.TripService

internal interface TripSession {

    val tripService: TripService
    var route: DirectionsRoute?

    fun getRawLocation(): Location?
    fun getEnhancedLocation(): Location?
    fun getRouteProgress(): RouteProgress?
    fun getState(): TripSessionState

    fun start()
    fun stop()

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

    fun updateSensorEvent(sensorEvent: SensorEvent)
    fun updateLegIndex(legIndex: Int): Boolean

    fun registerRouteAlertsObserver(routeAlertsObserver: RouteAlertsObserver)
    fun unregisterRouteAlertsObserver(routeAlertsObserver: RouteAlertsObserver)
    fun unregisterAllRouteAlertsObservers()

    fun registerEHorizonObserver(eHorizonObserver: EHorizonObserver)
    fun unregisterEHorizonObserver(eHorizonObserver: EHorizonObserver)
    fun unregisterAllEHorizonObservers()

    fun registerMapMatcherResultObserver(mapMatcherResultObserver: MapMatcherResultObserver)
    fun unregisterMapMatcherResultObserver(mapMatcherResultObserver: MapMatcherResultObserver)
    fun unregisterAllMapMatcherResultObservers()
}
