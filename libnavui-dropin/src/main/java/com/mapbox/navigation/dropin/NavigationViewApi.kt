package com.mapbox.navigation.dropin

import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.ui.app.internal.Store
import com.mapbox.navigation.ui.app.internal.destination.Destination
import com.mapbox.navigation.ui.app.internal.destination.DestinationAction
import com.mapbox.navigation.ui.app.internal.routefetch.RoutePreviewAction
import com.mapbox.navigation.ui.app.internal.routefetch.RoutesAction
import com.mapbox.navigation.ui.app.internal.tripsession.TripSessionStarterAction

/**
 * Api that gives you the ability to change the state for navigation apps.
 */
@ExperimentalPreviewMapboxNavigationAPI
class NavigationViewApi internal constructor(
    private val store: Store
) {

    /**
     * Request routes based on [RouteOptions]. Routes obtained from this route request can be
     * obtained via
     * [NavigationViewListener.onRouteFetching] when the route is being fetched
     * [NavigationViewListener.onRouteFetchFailed] if the route request failed
     * [NavigationViewListener.onRouteFetchCanceled] if the route request was canceled
     * [NavigationViewListener.onRouteFetchSuccessful] if the route request was a success
     *
     * @param options [RouteOptions]
     */
    fun fetchRoutes(options: RouteOptions) {
        store.dispatch(RoutePreviewAction.FetchOptions(options))
    }

    /**
     * Request routes based on list of [Point]. Routes obtained from this route request can be
     * obtained via
     * [NavigationViewListener.onRouteFetching] when the route is being fetched
     * [NavigationViewListener.onRouteFetchFailed] if the route request failed
     * [NavigationViewListener.onRouteFetchCanceled] if the route request was canceled
     * [NavigationViewListener.onRouteFetchSuccessful] if the route request was a success
     * The first point in the list should be the origin and last point should be the destination.
     * If you have multi waypoints, you can insert additional waypoints in the list
     * between the first and the last point.
     *
     * @param points list of [Point]
     */
    fun fetchRoutes(points: List<Point>) {
        store.dispatch(RoutePreviewAction.FetchPoints(points))
    }

    /**
     * Sets the [routes] passed to [NavigationView] and not to [MapboxNavigation]. The action
     * does not triggers [RouteProgress], [BannerInstructions] or [VoiceInstructions] updates even
     * if the [TripSessionState] is [TripSessionState.STARTED].
     *
     * @param routes list of [NavigationRoute]
     */
    fun setPreviewRoutes(routes: List<NavigationRoute>) {
        store.dispatch(RoutePreviewAction.Ready(routes))
    }

    /**
     * Sets the [routes] passed to [MapboxNavigation]. The action triggers [RouteProgress],
     * [BannerInstructions] and [VoiceInstructions] updates if the [TripSessionState] is
     * [TripSessionState.STARTED].
     *
     * @param routes list of [NavigationRoute]
     */
    fun setRoutes(routes: List<NavigationRoute>) {
        store.dispatch(RoutesAction.SetRoutes(routes))
    }

    /**
     * Sets a destination to [NavigationView].
     * @property point the destination location
     */
    fun setDestination(point: Point) {
        store.dispatch(DestinationAction.SetDestination(Destination(point)))
    }

    /**
     * Enables trip session based on real gps updates.
     */
    fun enableTripSession() {
        store.dispatch(TripSessionStarterAction.EnableTripSession)
    }

    /**
     * Enables replay trip session based on simulated locations.
     */
    fun enableReplaySession() {
        store.dispatch(TripSessionStarterAction.EnableReplayTripSession)
    }

    /**
     * Checks if the current trip is being simulated.
     */
    fun isReplayEnabled(): Boolean {
        return store.state.value.tripSession.isReplayEnabled
    }
}
