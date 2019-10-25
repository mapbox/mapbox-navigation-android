package com.mapbox.services.android.navigation.v5.route

import android.content.Context
import android.location.Location
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.utils.extensions.ifNonNull
import com.mapbox.navigation.route.offboard.NavigationRoute
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress
import com.mapbox.services.android.navigation.v5.utils.RouteUtils
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber

/**
 * This class can be used to fetch new routes given a [Location] origin and
 * [RouteOptions] provided by a [RouteProgress].
 */
class RouteFetcher
@JvmOverloads constructor(
    context: Context,
    private val accessToken: String,
    private val routeUtils: RouteUtils = RouteUtils(),
    private var navigationRoute: com.mapbox.navigation.route.offboard.NavigationRoute? = null
) {

    companion object {
        private const val BEARING_TOLERANCE = 90.0
    }

    private val routeListeners = CopyOnWriteArrayList<RouteListener>()
    private val contextWeakReference: WeakReference<Context> = WeakReference(context)

    private var routeProgress: RouteProgress? = null

    private val directionsResponseCallback = object : Callback<DirectionsResponse> {
        override fun onResponse(
            call: Call<DirectionsResponse>,
            response: Response<DirectionsResponse>
        ) {
            updateListeners(response.body(), routeProgress)
        }

        override fun onFailure(call: Call<DirectionsResponse>, throwable: Throwable) {
            updateListenersWithError(throwable)
        }
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
     * Uses [RouteOptions.coordinates] and [RouteProgress.remainingWaypoints]
     * to determine the amount of remaining waypoints there are along the given route.
     *
     * @param location current location of the device
     * @param routeProgress for remaining waypoints along the route
     * @return request reflecting the current progress
     */
    fun buildRequestFrom(
        location: Location,
        routeProgress: RouteProgress
    ): com.mapbox.navigation.route.offboard.NavigationRoute.Builder? {
        val context = contextWeakReference.get()
            ?: return null
        val origin = Point.fromLngLat(location.longitude, location.latitude)
        val bearing = if (location.hasBearing()) location.bearing.toDouble() else null
        val routeOptions = routeProgress.directionsRoute()?.routeOptions()
        val navigationRouteBuilder = com.mapbox.navigation.route.offboard.NavigationRoute.builder(context)
            .accessToken(accessToken)
            .origin(origin, bearing, BEARING_TOLERANCE)
        routeOptions?.let { options ->
            navigationRouteBuilder.routeOptions(options)
        }
        val remainingWaypoints = routeUtils.calculateRemainingWaypoints(routeProgress)?.toMutableList()
        if (remainingWaypoints == null) {
            Timber.e("An error occurred fetching a new route")
            return null
        }
        addDestination(remainingWaypoints, navigationRouteBuilder)
        addWaypoints(remainingWaypoints, navigationRouteBuilder)
        addWaypointIndices(routeProgress, navigationRouteBuilder)
        addWaypointNames(routeProgress, navigationRouteBuilder)
        addApproaches(routeProgress, navigationRouteBuilder)
        return navigationRouteBuilder
    }

    /**
     * Executes the given NavigationRoute builder, eventually triggering
     * any [RouteListener] that has been added via [RouteFetcher.addRouteListener].
     *
     * @param builder to be executed
     */
    fun findRouteWith(builder: com.mapbox.navigation.route.offboard.NavigationRoute.Builder?) {
        builder?.let { navigationRouteBuilder ->
            navigationRoute = navigationRouteBuilder.build()
            navigationRoute?.getRoute(directionsResponseCallback)
        }
    }

    /**
     * Cancels the Directions API call if it has not been executed yet.
     */
    fun cancelRouteCall() {
        navigationRoute?.cancelCall()
    }

    private fun addDestination(
        remainingWaypoints: MutableList<Point>,
        builder: com.mapbox.navigation.route.offboard.NavigationRoute.Builder
    ) {
        if (remainingWaypoints.isNotEmpty()) {
            builder.destination(retrieveDestinationWaypoint(remainingWaypoints))
        }
    }

    private fun retrieveDestinationWaypoint(
        remainingWaypoints: MutableList<Point>
    ): Point =
        remainingWaypoints.removeAt(remainingWaypoints.size - 1)

    private fun addWaypoints(
        remainingCoordinates: List<Point>,
        builder: com.mapbox.navigation.route.offboard.NavigationRoute.Builder
    ) {
        if (remainingCoordinates.isNotEmpty()) {
            for (coordinate in remainingCoordinates) {
                builder.addWaypoint(coordinate)
            }
        }
    }

    private fun addWaypointIndices(
        routeProgress: RouteProgress,
        builder: com.mapbox.navigation.route.offboard.NavigationRoute.Builder
    ) {
        val remainingWaypointIndices: IntArray? = routeUtils.calculateRemainingWaypointIndices(routeProgress)
        if (remainingWaypointIndices != null && remainingWaypointIndices.isNotEmpty()) {
            builder.addWaypointIndices(*remainingWaypointIndices)
        }
    }

    private fun addWaypointNames(
        progress: RouteProgress,
        builder: com.mapbox.navigation.route.offboard.NavigationRoute.Builder
    ) {
        val remainingWaypointNames: Array<String>? = routeUtils.calculateRemainingWaypointNames(progress)
        remainingWaypointNames?.let {
            builder.addWaypointNames(*it)
        }
    }

    private fun addApproaches(
        progress: RouteProgress,
        builder: com.mapbox.navigation.route.offboard.NavigationRoute.Builder
    ) {
        val remainingApproaches: Array<String>? = routeUtils.calculateRemainingApproaches(progress)
        remainingApproaches?.let {
            builder.addApproaches(*it)
        }
    }

    private fun updateListeners(
        response: DirectionsResponse?,
        routeProgress: RouteProgress?
    ) {
        ifNonNull(response) { directionsResponse ->
            for (listener in routeListeners) {
                listener.onResponseReceived(directionsResponse, routeProgress)
            }
        }
    }

    private fun updateListenersWithError(throwable: Throwable) {
        for (listener in routeListeners) {
            listener.onErrorReceived(throwable)
        }
    }
}
