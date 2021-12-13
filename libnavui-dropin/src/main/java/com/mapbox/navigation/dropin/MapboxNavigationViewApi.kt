package com.mapbox.navigation.dropin

import android.location.Location
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.arrival.ArrivalObserver
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.trip.session.BannerInstructionsObserver
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver
import kotlinx.coroutines.ExperimentalCoroutinesApi

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
    fun getOptions(): NavigationViewOptions
    fun setRoutes(routes: List<DirectionsRoute>)
    fun fetchAndSetRoute(points: List<Point>)
    fun fetchAndSetRoute(routeOptions: RouteOptions)
    fun temporaryStartNavigation()
}

@OptIn(ExperimentalCoroutinesApi::class)
internal class MapboxNavigationViewApiImpl(
    private val navigationView: NavigationView
) : MapboxNavigationViewApi {
    /**
     * Invoke to receive update to [RouteProgress] events.
     */
    override fun addRouteProgressObserver(observer: RouteProgressObserver) {
        navigationView.addRouteProgressObserver(observer)
    }

    /**
     * Invoke to stop receiving updates to [RouteProgress] events.
     */
    override fun removeRouteProgressObserver(observer: RouteProgressObserver) {
        navigationView.removeRouteProgressObserver(observer)
    }

    /**
     * Invoke to receive updates to [Location] events.
     */
    override fun addLocationObserver(observer: LocationObserver) {
        navigationView.addLocationObserver(observer)
    }

    /**
     * Invoke to stop receiving updates to [Location] events.
     */
    override fun removeLocationObserver(observer: LocationObserver) {
        navigationView.removeLocationObserver(observer)
    }

    /**
     * Invoke to receive updates to [DirectionsRoute].
     */
    override fun addRoutesObserver(observer: RoutesObserver) {
        navigationView.addRoutesObserver(observer)
    }

    /**
     * Invoke to stop receiving updates to [DirectionsRoute].
     */
    override fun removeRoutesObserver(observer: RoutesObserver) {
        navigationView.removeRoutesObserver(observer)
    }

    /**
     * Invoke to receive update to arrival events
     */
    override fun addArrivalObserver(observer: ArrivalObserver) {
        navigationView.addArrivalObserver(observer)
    }

    /**
     * Invoke to stop receiving updates to arrival events.
     */
    override fun removeArrivalObserver(observer: ArrivalObserver) {
        navigationView.removeArrivalObserver(observer)
    }

    /**
     * Invoke to receive updates to [BannerInstructions] events.
     */
    override fun addBannerInstructionsObserver(observer: BannerInstructionsObserver) {
        navigationView.addBannerInstructionsObserver(observer)
    }

    /**
     * Invoke to stop receiving updates to [BannerInstructions] events.
     */
    override fun removeBannerInstructionsObserver(observer: BannerInstructionsObserver) {
        navigationView.removeBannerInstructionsObserver(observer)
    }

    /**
     * Invoke to receive updates to [TripSessionState].
     */
    override fun addTripSessionStateObserver(observer: TripSessionStateObserver) {
        navigationView.addTripSessionStateObserver(observer)
    }

    /**
     * Invoke to stop receiving updates to [TripSessionState].
     */
    override fun removeTripSessionStateObserver(observer: TripSessionStateObserver) {
        navigationView.removeTripSessionStateObserver(observer)
    }

    /**
     * Invoke to receive updates to [VoiceInstructions] events.
     */
    override fun addVoiceInstructionsObserver(observer: VoiceInstructionsObserver) {
        navigationView.addVoiceInstructionObserver(observer)
    }

    /**
     * Invoke to stop receiving updates to [VoiceInstructions] events.
     */
    override fun removeVoiceInstructionsObserver(observer: VoiceInstructionsObserver) {
        navigationView.removeVoiceInstructionObserver(observer)
    }

    /**
     * Invoke to update options associated with the [NavigationView].
     */
    override fun update(navigationViewOptions: NavigationViewOptions) {
        navigationView.updateNavigationViewOptions(navigationViewOptions)
    }

    /**
     * Provides access to [MapView].
     */
    override fun getMapView(): MapView {
        return navigationView.retrieveMapView()
    }

    /**
     * Creates references to view components. Views provided via the [viewProvider] will be given preference.
     * If null, default Mapbox designed views will be used.
     */
    override fun configureNavigationView(viewProvider: ViewProvider) {
        navigationView.configure(viewProvider)
    }

    /**
     * A temporary method to start a navigation session.
     */
    override fun temporaryStartNavigation() {
        navigationView.temporaryStartNavigation()
    }

    /**
     * @return the NavigationViewOptions
     */
    override fun getOptions(): NavigationViewOptions {
        return navigationView.navigationViewOptions
    }

    /**
     * Sets the routes for [MapboxNavigation] and supporting components. See the documentation
     * for [MapboxNavigation::setRoutes] for more information.
     *
     * @param routes the routes to use.
     */
    override fun setRoutes(routes: List<DirectionsRoute>) {
        navigationView.setRoutes(routes)
    }

    /**
     * Using the points provided an request will be made to fetch one or more routes. The
     * route(s) returned will be set on [MapboxNavigation]. The first point in the list
     * will be considered the origin and the last point in the list will be considered the
     * destination. Any additional points will be used as waypoints.
     *
     * @param points the points to be used for fetching a route
     */
    override fun fetchAndSetRoute(points: List<Point>) {
        navigationView.fetchAndSetRoute(points)
    }

    /**
     * Using the [RouteOptions] provided an request will be made to fetch one or more routes. The
     * route(s) returned will be set on [MapboxNavigation].
     *
     * @param routeOptions the options to be used when fetching the route(s).
     */
    override fun fetchAndSetRoute(routeOptions: RouteOptions) {
        navigationView.fetchAndSetRoute(routeOptions)
    }
}
