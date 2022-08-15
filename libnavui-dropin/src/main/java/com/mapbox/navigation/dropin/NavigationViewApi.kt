@file:Suppress("unused")

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

/**
 * Api that gives you the ability to change the state for navigation apps.
 */
@ExperimentalPreviewMapboxNavigationAPI
abstract class NavigationViewApi {

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
    abstract fun fetchRoutes(options: RouteOptions)

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
    abstract fun fetchRoutes(points: List<Point>)

    /**
     * Sets the [routes] passed to [NavigationView] and not to [MapboxNavigation]. The action
     * does not triggers [RouteProgress], [BannerInstructions] or [VoiceInstructions] updates even
     * if the [TripSessionState] is [TripSessionState.STARTED].
     *
     * @param routes list of [NavigationRoute]
     */
    abstract fun setPreviewRoutes(routes: List<NavigationRoute>)

    /**
     * Sets the [routes] passed to [MapboxNavigation]. The action triggers [RouteProgress],
     * [BannerInstructions] and [VoiceInstructions] updates if the [TripSessionState] is
     * [TripSessionState.STARTED].
     *
     * @param routes list of [NavigationRoute]
     */
    abstract fun setRoutes(routes: List<NavigationRoute>)

    /**
     * Sets a destination to [NavigationView].
     * @property point the destination location
     */
    abstract fun setDestination(point: Point)

    /**
     * Enables trip session based on real gps updates.
     */
    abstract fun enableTripSession()

    /**
     * Enables replay trip session based on simulated locations.
     */
    abstract fun enableReplaySession()

    /**
     * Checks if the current trip is being simulated.
     */
    abstract fun isReplayEnabled(): Boolean

    /**
     * Clear Route data and request [NavigationView] to enter Free Drive state.
     *
     * [NavigationViewListener.onFreeDrive] or [NavigationViewListener.onDestinationPreview]
     * (depending if destination has been set) will be called once [NavigationView] enters
     * Free Drive state.
     */
    abstract fun startFreeDrive()

    /**
     * Request [NavigationView] to enter Route Preview state.
     *
     * [NavigationViewListener.onRoutePreview] will be called once [NavigationView] enters
     * Route Preview state.
     *
     * @throws IllegalStateException if either Destination or Preview Routes are not set.
     */
    @Throws(IllegalStateException::class)
    abstract fun startRoutePreview()

    /**
     * Request [NavigationView] to enter Active Navigation state.
     *
     * [NavigationViewListener.onActiveNavigation] will be called once [NavigationView] enters
     * Active Navigation state.
     *
     * @throws IllegalStateException if either Destination or Preview Routes are not set.
     */
    @Throws(IllegalStateException::class)
    abstract fun startNavigation()

    /**
     * Request [NavigationView] to enter Arrival state.
     *
     * [NavigationViewListener.onArrival] will be called once [NavigationView] enters
     * Arrival state.
     */
    abstract fun startArrival()

    /**
     * Clear Destination and Route data, and update [NavigationView] to Free Drive state.
     */
    abstract fun endNavigation()
}

/**
 * Sets a [destination], preview [routes] and request [NavigationView] to enter Route Preview state.
 *
 * It is a shorthand for calling the following with non-empty [routes].
 * ```
 *     setDestination(point)
 *     setPreviewRoutes(routes)
 *     startRoutePreview()
 * ```
 *
 * @throws IllegalArgumentException if the [routes] argument is an empty list.
 */
@ExperimentalPreviewMapboxNavigationAPI
@Throws(IllegalArgumentException::class)
fun NavigationViewApi.startRoutePreview(destination: Point, routes: List<NavigationRoute>) {
    require(routes.isNotEmpty()) { "routes cannot be empty" }

    setDestination(destination)
    setPreviewRoutes(routes)
    startRoutePreview()
}

/**
 * Sets a [destination], preview [routes] and request [NavigationView] to enter Active Navigation
 * state.
 *
 * It is a shorthand for calling the following with non-empty [routes].
 * ```
 *     setDestination(point)
 *     setPreviewRoutes(routes)
 *     startNavigation()
 * ```
 *
 * @throws IllegalArgumentException if the [routes] argument is an empty list.
 */
@ExperimentalPreviewMapboxNavigationAPI
@Throws(IllegalArgumentException::class)
fun NavigationViewApi.startNavigation(destination: Point, routes: List<NavigationRoute>) {
    require(routes.isNotEmpty()) { "routes cannot be empty" }

    setDestination(destination)
    setPreviewRoutes(routes)
    startNavigation()
}
