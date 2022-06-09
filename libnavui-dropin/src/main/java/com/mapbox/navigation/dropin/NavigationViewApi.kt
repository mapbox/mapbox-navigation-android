package com.mapbox.navigation.dropin

import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.ui.app.internal.Store
import com.mapbox.navigation.ui.app.internal.destination.Destination
import com.mapbox.navigation.ui.app.internal.destination.DestinationAction
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
     * The action is used to request and set routes based on [RouteOptions].
     * @param options
     */
    fun setRoutes(options: RouteOptions) {
        store.dispatch(RoutesAction.FetchOptions(options))
    }

    /**
     * The action is used to directly set the [NavigationRoute] supplied to [NavigationView].
     * @param routes list of [NavigationRoute]
     */
    fun setRoutes(routes: List<NavigationRoute>) {
        store.dispatch(RoutesAction.SetRoutes(routes))
    }

    /**
     * The action is used to set the destination for the trip.
     * @property point the destination location
     */
    fun setDestination(point: Point) {
        store.dispatch(DestinationAction.SetDestination(Destination(point)))
    }

    /**
     * The action enables trip session based on real gps updates.
     */
    fun enableTripSession() {
        store.dispatch(TripSessionStarterAction.EnableTripSession)
    }

    /**
     * The action enables replay trip session based on simulated locations.
     */
    fun enableReplaySession() {
        store.dispatch(TripSessionStarterAction.EnableReplayTripSession)
    }

    /**
     * Check if the current trip is being simulated.
     */
    fun isReplayEnabled(): Boolean {
        return store.state.value.tripSession.isReplayEnabled
    }
}
