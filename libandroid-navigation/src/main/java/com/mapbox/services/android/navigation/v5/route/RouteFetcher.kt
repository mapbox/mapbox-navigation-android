package com.mapbox.services.android.navigation.v5.route

import android.content.Context
import android.location.Location
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.core.utils.TextUtils
import com.mapbox.geojson.Point
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import com.mapbox.services.android.navigation.v5.utils.RouteUtils
import java.lang.ref.WeakReference
import java.util.Arrays
import java.util.concurrent.CopyOnWriteArrayList
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber

/**
 * This class can be used to fetch new routes given a [Location] origin and
 * [RouteOptions] provided by a [RouteProgress].
 */
class RouteFetcher {

    companion object {

        private const val BEARING_TOLERANCE = 90.0
        private const val SEMICOLON = ";"
        private const val ORIGIN_APPROACH_THRESHOLD = 1
        private const val ORIGIN_APPROACH = 0
        private const val FIRST_POSITION = 0
        private const val SECOND_POSITION = 1
    }

    private val routeListeners = CopyOnWriteArrayList<RouteListener>()
    private val accessToken: String
    private val contextWeakReference: WeakReference<Context>

    private var navigationRoute: NavigationRoute? = null
    private var routeProgress: RouteProgress? = null
    private val routeUtils: RouteUtils

    private val directionsResponseCallback = object : Callback<DirectionsResponse> {
        override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
            updateListeners(response.body(), routeProgress)
        }

        override fun onFailure(call: Call<DirectionsResponse>, throwable: Throwable) {
            updateListenersWithError(throwable)
        }
    }

    constructor(context: Context, accessToken: String) {
        this.accessToken = accessToken
        contextWeakReference = WeakReference(context)
        routeUtils = RouteUtils()
    }

    // Package private (no modifier) for testing purposes
    internal constructor(context: Context, accessToken: String, navigationRoute: NavigationRoute) {
        this.contextWeakReference = WeakReference(context)
        this.navigationRoute = navigationRoute
        this.accessToken = accessToken
        this.routeUtils = RouteUtils()
    }

    // Package private (no modifier) for testing purposes
    internal constructor(context: Context, accessToken: String, routeUtils: RouteUtils) {
        this.contextWeakReference = WeakReference(context)
        this.accessToken = accessToken
        this.routeUtils = routeUtils
    }

    /**
     * Adds a [RouteListener] to this class to be triggered when a route
     * response has been received.
     *
     * @param listener to be added
     */
    fun addRouteListener(listener: RouteListener) {
        if (!routeListeners.contains(listener)) {
            routeListeners.add(listener)
        }
    }

    /**
     * Clears any listeners that have been added to this class via
     * [RouteFetcher.addRouteListener].
     */
    fun clearListeners() {
        routeListeners.clear()
    }

    /**
     * Calculates a new [com.mapbox.api.directions.v5.models.DirectionsRoute] given
     * the current [Location] and [RouteProgress] along the route.
     *
     *
     * Uses [RouteOptions.coordinates] and [RouteProgress.remainingWaypoints]
     * to determine the amount of remaining waypoints there are along the given route.
     *
     * @param location current location of the device
     * @param routeProgress for remaining waypoints along the route
     * @since 0.13.0
     */
    fun findRouteFromRouteProgress(location: Location, routeProgress: RouteProgress) {
        this.routeProgress = routeProgress
        val builder = buildRequestFrom(location, routeProgress)
        findRouteWith(builder)
    }

    /**
     * Build a route request given the passed [Location] and [RouteProgress].
     *
     *
     * Uses [RouteOptions.coordinates] and [RouteProgress.remainingWaypoints]
     * to determine the amount of remaining waypoints there are along the given route.
     *
     * @param location current location of the device
     * @param routeProgress for remaining waypoints along the route
     * @return request reflecting the current progress
     */
    fun buildRequestFrom(location: Location, routeProgress: RouteProgress): NavigationRoute.Builder? {
        val context = contextWeakReference.get()
        if (invalid(context, location, routeProgress)) {
            return null
        }
        val origin = Point.fromLngLat(location.longitude, location.latitude)
        val bearing = if (location.hasBearing()) java.lang.Float.valueOf(location.bearing).toDouble() else null
        val options = routeProgress.directionsRoute().routeOptions()
        var navigationRouteBuilder: NavigationRoute.Builder? = null
        options?.let { options ->
            navigationRouteBuilder = NavigationRoute.builder(context!!)
                    .accessToken(accessToken)
                    .origin(origin, bearing, BEARING_TOLERANCE)
                    .routeOptions(options)
        }
        val remainingWaypoints = routeUtils.calculateRemainingWaypoints(routeProgress)
        if (remainingWaypoints == null) {
            Timber.e("An error occurred fetching a new route")
            return null
        }
        navigationRouteBuilder?.let { builder ->
            addDestination(remainingWaypoints.toMutableList(), builder)
            addWaypoints(remainingWaypoints, builder)
            addWaypointNames(routeProgress, builder)
            addApproaches(routeProgress, builder)
        }
        return navigationRouteBuilder
    }

    /**
     * Executes the given NavigationRoute builder, eventually triggering
     * any [RouteListener] that has been added via [RouteFetcher.addRouteListener].
     *
     * @param builder to be executed
     */
    fun findRouteWith(builder: NavigationRoute.Builder?) {
        if (builder != null) {
            navigationRoute = builder.build()
            navigationRoute?.getRoute(directionsResponseCallback)
        }
    }

    /**
     * Cancels the Directions API call if it has not been executed yet.
     */
    fun cancelRouteCall() {
        navigationRoute?.cancelCall()
    }

    private fun addDestination(remainingWaypoints: MutableList<Point>, builder: NavigationRoute.Builder) {
        if (remainingWaypoints.isNotEmpty()) {
            builder.destination(retrieveDestinationWaypoint(remainingWaypoints))
        }
    }

    private fun retrieveDestinationWaypoint(remainingWaypoints: MutableList<Point>): Point {
        val lastWaypoint = remainingWaypoints.size - 1
        return remainingWaypoints.removeAt(lastWaypoint)
    }

    private fun addWaypoints(remainingCoordinates: List<Point>, builder: NavigationRoute.Builder) {
        if (remainingCoordinates.isNotEmpty()) {
            for (coordinate in remainingCoordinates) {
                builder.addWaypoint(coordinate)
            }
        }
    }

    private fun addWaypointNames(progress: RouteProgress, builder: NavigationRoute.Builder) {
        val remainingWaypointNames = routeUtils.calculateRemainingWaypointNames(progress)
        if (remainingWaypointNames != null) {
            val result = Array(remainingWaypointNames.size) { "" }
            var index = 0
            for (waypointNames in remainingWaypointNames) {
                if (waypointNames != null) {
                    result[index++] = waypointNames
                }
            }
            builder.addWaypointNames(*result)
        }
    }

    private fun addApproaches(progress: RouteProgress, builder: NavigationRoute.Builder) {
        val remainingApproaches = calculateRemainingApproaches(progress)
        if (remainingApproaches != null) {
            builder.addApproaches(*remainingApproaches)
        }
    }

    private fun calculateRemainingApproaches(routeProgress: RouteProgress): Array<String?>? {
        val routeOptions = routeProgress.directionsRoute().routeOptions()
        if (routeOptions == null || TextUtils.isEmpty(routeOptions.approaches())) {
            return null
        }
        val allApproaches = routeOptions.approaches()
        val splitApproaches = allApproaches!!.split(SEMICOLON.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val coordinatesSize = routeOptions.coordinates().size
        val remainingApproaches = Arrays.copyOfRange(splitApproaches,
                coordinatesSize - routeProgress.remainingWaypoints(), coordinatesSize)
        val approaches = arrayOfNulls<String>(remainingApproaches.size + ORIGIN_APPROACH_THRESHOLD)
        approaches[ORIGIN_APPROACH] = splitApproaches[ORIGIN_APPROACH]
        System.arraycopy(remainingApproaches, FIRST_POSITION, approaches, SECOND_POSITION, remainingApproaches.size)
        return approaches
    }

    private fun invalid(context: Context?, location: Location?, routeProgress: RouteProgress?): Boolean {
        return context == null || location == null || routeProgress == null
    }

    private fun updateListeners(response: DirectionsResponse?, routeProgress: RouteProgress?) {
        for (listener in routeListeners) {
            response?.let { directionsResponse ->
                routeProgress?.let { routeProgress ->
                    listener.onResponseReceived(directionsResponse, routeProgress)
                }
            }
        }
    }

    private fun updateListenersWithError(throwable: Throwable) {
        for (listener in routeListeners) {
            listener.onErrorReceived(throwable)
        }
    }
}
