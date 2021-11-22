package com.mapbox.navigation.dropin

import com.mapbox.maps.MapView
import com.mapbox.navigation.core.arrival.ArrivalObserver
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.trip.session.BannerInstructionsObserver
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver

interface MapboxNavigationViewApi {
    fun addRouteProgressObserver(observer: RouteProgressObserver)
    fun removeRouteProgressObserver(observer: RouteProgressObserver)
    fun addLocationObserver(observer: LocationObserver)
    fun removeLocationObserver(observer: LocationObserver)
    fun addRoutesObserver(observer: RoutesObserver)
    fun removeRoutesObserver(observer: RoutesObserver)
    fun addArrivalObserver(observer: ArrivalObserver)
    fun removeArrivalObserver(observer: ArrivalObserver)
    fun addBannerInstructionsObserver(observer: BannerInstructionsObserver)
    fun removeBannerInstructionsObserver(observer: BannerInstructionsObserver)
    fun addTripSessionStateObserver(observer: TripSessionStateObserver)
    fun removeTripSessionStateObserver(observer: TripSessionStateObserver)
    fun addVoiceInstructionsObserver(observer: VoiceInstructionsObserver)
    fun removeVoiceInstructionsObserver(observer: VoiceInstructionsObserver)
    fun update(navigationViewOptions: NavigationViewOptions)
    fun getMapView(): MapView
    fun configureNavigationView(viewProvider: ViewProvider)
}

internal class MapboxNavigationViewApiImpl(
    private val navigationView: NavigationView
) : MapboxNavigationViewApi {
    override fun addRouteProgressObserver(observer: RouteProgressObserver) {
        navigationView.addRouteProgressObserver(observer)
    }

    override fun removeRouteProgressObserver(observer: RouteProgressObserver) {
        navigationView.removeRouteProgressObserver(observer)
    }

    override fun addLocationObserver(observer: LocationObserver) {
        navigationView.addLocationObserver(observer)
    }

    override fun removeLocationObserver(observer: LocationObserver) {
        navigationView.removeLocationObserver(observer)
    }

    override fun addRoutesObserver(observer: RoutesObserver) {
        navigationView.addRoutesObserver(observer)
    }

    override fun removeRoutesObserver(observer: RoutesObserver) {
        navigationView.removeRoutesObserver(observer)
    }

    override fun addArrivalObserver(observer: ArrivalObserver) {
        navigationView.addArrivalObserver(observer)
    }

    override fun removeArrivalObserver(observer: ArrivalObserver) {
        navigationView.removeArrivalObserver(observer)
    }

    override fun addBannerInstructionsObserver(observer: BannerInstructionsObserver) {
        navigationView.addBannerInstructionsObserver(observer)
    }

    override fun removeBannerInstructionsObserver(observer: BannerInstructionsObserver) {
        navigationView.removeBannerInstructionsObserver(observer)
    }

    override fun addTripSessionStateObserver(observer: TripSessionStateObserver) {
        navigationView.addTripSessionStateObserver(observer)
    }

    override fun removeTripSessionStateObserver(observer: TripSessionStateObserver) {
        navigationView.removeTripSessionStateObserver(observer)
    }

    override fun addVoiceInstructionsObserver(observer: VoiceInstructionsObserver) {
        navigationView.addVoiceInstructionObserver(observer)
    }

    override fun removeVoiceInstructionsObserver(observer: VoiceInstructionsObserver) {
        navigationView.removeVoiceInstructionObserver(observer)
    }

    override fun update(navigationViewOptions: NavigationViewOptions) {
        navigationView.updateNavigationViewOptions(navigationViewOptions)
    }

    override fun getMapView(): MapView {
        return navigationView.retrieveMapView()
    }

    override fun configureNavigationView(viewProvider: ViewProvider) {
        navigationView.configure(viewProvider)
    }
}
