package com.mapbox.navigation.core.reroute

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.routeoptions.MapboxRouteOptionsUpdater
import com.mapbox.navigation.core.trip.session.OffRouteObserver

/**
 * Reroute controller allows changing the reroute logic externally. Use [MapboxNavigation.rerouteController]
 * to replace it.
 */
interface RerouteController {

    /**
     * Reroute state
     */
    val state: RerouteState

    /**
     * Invoked whenever re-route is needed. For instance when a driver is off-route. Called just after
     * an off-route event.
     *
     * @see [OffRouteObserver]
     */
    fun reroute(routesCallback: RoutesCallback)

    /**
     * Invoked when re-route is not needed anymore (for instance when driver returns to previous route).
     * Might be ignored depending on [RerouteState] e.g. if a route has been fetched it does not make sense to interrupt re-routing
     */
    fun interrupt()

    /**
     * Add a RerouteStateObserver to collection and immediately invoke [rerouteStateObserver] with current
     * re-route state.
     *
     * @return `true` if the element has been added, `false` if the element is already present in the collection.
     */
    fun registerRerouteStateObserver(rerouteStateObserver: RerouteStateObserver): Boolean

    /**
     * Remove [rerouteStateObserver] from collection of observers.
     *
     * @return `true` if the element has been successfully removed; `false` if it was not present in the collection.
     */
    fun unregisterRerouteStateObserver(rerouteStateObserver: RerouteStateObserver): Boolean

    /**
     * Route Callback is useful to set new route(s) on reroute event. Doing the same as
     * [MapboxNavigation.setRoutes].
     */
    interface RoutesCallback {
        /**
         * Called whenever new route(s) has been fetched.
         * @see [MapboxNavigation.setRoutes]
         */
        fun onNewRoutes(routes: List<DirectionsRoute>)
    }

    /**
     * [RerouteState] observer
     */
    interface RerouteStateObserver {

        /**
         * Invoked whenever re-route state has changed.
         */
        fun onRerouteStateChanged(rerouteState: RerouteState)
    }
}

/**
 * Reroute state
 */
sealed class RerouteState {

    /**
     * [RerouteController] is idle.
     */
    object Idle : RerouteState()

    /**
     * Reroute has been interrupted.
     *
     * Might be invoked by:
     * - [RerouteController.interrupt];
     * - [MapboxNavigation.setRoutes];
     * - [MapboxNavigation.requestRoutes];
     * - from the SDK internally if another route request has been requested (only when using the default
     * implementation [MapboxRerouteController]).
     *
     */
    object Interrupted : RerouteState()

    /**
     * Re-route request has failed.
     *
     * You can [MapboxNavigation.requestRoutes] or [MapboxNavigation.setRoutes] with [MapboxRouteOptionsUpdater] to retry the request.
     *
     * @param message describes error
     * @param throwable is optional
     */
    data class Failed(val message: String, val throwable: Throwable? = null) : RerouteState()

    /**
     * Route fetching is in progress.
     */
    object FetchingRoute : RerouteState()

    /**
     * Route has been fetched.
     */
    object RouteFetched : RerouteState()
}
