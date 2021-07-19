package com.mapbox.navigation.core

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.common.BillingServiceError
import com.mapbox.common.BillingServiceErrorCode
import com.mapbox.common.SKUIdentifier
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.utils.internal.LoggerProvider
import com.mapbox.turf.TurfConstants.UNIT_METRES
import com.mapbox.turf.TurfMeasurement
import java.util.concurrent.TimeUnit

internal class BillingController(
    private val accessToken: String,
    private val navigationSession: NavigationSession,
    private val tripSession: TripSession
) {

    private companion object {
        private val tag = Tag("MbxNavBillingController")
        private const val MAX_WAYPOINTS_DISTANCE_DIFF_METERS = 100.0
    }

    private val navigationSessionStateObserver =
        NavigationSessionStateObserver { navigationSessionState ->
            // stop any running sessions
            BillingServiceWrapper.stopBillingSession(SKUIdentifier.NAV2_SES_TRIP)

            if (navigationSessionState != NavigationSession.State.IDLE) {
                // always trigger an MAU event if a session starts
                BillingServiceWrapper.triggerBillingEvent(
                    accessToken,
                    "", // empty string results in default user agent
                    SKUIdentifier.NAV2_SES_MAU
                ) {
                    handlerError(it)
                }
            }

            when (navigationSessionState) {
                NavigationSession.State.IDLE -> {
                    // do nothing
                }
                NavigationSession.State.FREE_DRIVE -> {
                    BillingServiceWrapper.beginBillingSession(
                        SKUIdentifier.NAV2_SES_TRIP,
                        validity = TimeUnit.HOURS.toMillis(1) // validity of 1hr
                    )
                }
                NavigationSession.State.ACTIVE_GUIDANCE -> {
                    BillingServiceWrapper.beginBillingSession(
                        SKUIdentifier.NAV2_SES_TRIP,
                        validity = 0 // default validity, 12hrs
                    )
                }
            }
        }

    init {
        navigationSession.registerNavigationSessionStateObserver(navigationSessionStateObserver)
    }

    fun pauseSession() {
        BillingServiceWrapper.pauseBillingSession(SKUIdentifier.NAV2_SES_TRIP)
    }

    fun resumeSession() {
        BillingServiceWrapper.resumeBillingSession(SKUIdentifier.NAV2_SES_TRIP) {
            handlerError(it)
        }
    }

    /**
     * Has to be called whenever a new route is set by the developer.
     *
     * It also **has to be called** before the state changes in the [DirectionsSession].
     * That's because a route change can also change the state and we first want to evaluate the consequences of the action with the current state,
     * and only then react to a potential state change. When executed in this order, the logic in this block is always mutually exclusive to the [navigationSessionStateObserver].
     * If we already are in active guidance and a route is set, [onExternalRouteSet] will take action while [navigationSessionStateObserver] won't be called.
     * On the other hand if we are not in active guidance and [onExternalRouteSet] is called, it will do nothing, and then [navigationSessionStateObserver] will be called.
     *
     * This block evaluates if the newly provided route has waypoints that are different than the ones in the currently active route.
     * If this is true, it will begin a new billing session.
     *
     * A route is considered the same if the waypoints count is the same and each pair of [`old waypoint` and `new waypoint`] are within [MAX_WAYPOINTS_DISTANCE_DIFF_METERS] of each other.
     * This method is also accounting for progress - if there's a multi-leg route, we'll only compare remaining legs of the current route against the new route.
     */
    fun onExternalRouteSet(directionsRoute: DirectionsRoute) {
        if (navigationSession.state == NavigationSession.State.ACTIVE_GUIDANCE) {
            val currentRemainingWaypoints = getRemainingWaypointsOnRoute(
                tripSession.getRouteProgress()
            )
            val newWaypoints = getWaypointsOnRoute(directionsRoute)

            if (!waypointsWithinRange(currentRemainingWaypoints, newWaypoints)) {
                // stop any running sessions
                BillingServiceWrapper.stopBillingSession(SKUIdentifier.NAV2_SES_TRIP)

                BillingServiceWrapper.beginBillingSession(
                    SKUIdentifier.NAV2_SES_TRIP,
                    validity = 0 // default validity, 12hrs
                )
            }
        }
    }

    private fun BillingServiceWrapper.beginBillingSession(
        identifier: SKUIdentifier,
        validity: Long
    ) {
        this.beginBillingSession(
            accessToken,
            "", // empty string result in default user agent
            identifier,
            {
                handlerError(it)
            },
            validity
        )
    }

    /**
     * Returns a list of remaining [Point]s that mark ends of legs on the route from the current [RouteProgress],
     * ignoring origin and silent waypoints.
     */
    private fun getRemainingWaypointsOnRoute(routeProgress: RouteProgress?): List<Point>? {
        return routeProgress?.route?.let { route ->
            routeProgress.remainingWaypoints.let { remainingWaypointsCount ->
                val coordinates = route.routeOptions()?.coordinatesList()
                coordinates?.mapIndexed { index, point ->
                    Waypoint(index, point)
                }?.filterIndexed { index, _ ->
                    index >= coordinates.size - remainingWaypointsCount.coerceAtMost(
                        // ensures that we drop the origin point
                        coordinates.size - 1
                    )
                }?.filter { waypoint ->
                    val waypointIndices = route.routeOptions()?.waypointIndicesList()
                    waypointIndices?.contains(waypoint.index) ?: true
                }?.map { it.point }
            }
        }
    }

    /**
     * Returns a list of [Point]s that mark ends of legs on the route,
     * ignoring origin and silent waypoints.
     */
    private fun getWaypointsOnRoute(directionsRoute: DirectionsRoute): List<Point>? {
        val waypointIndices = directionsRoute.routeOptions()?.waypointIndicesList()
        return directionsRoute.routeOptions()?.coordinatesList()?.filterIndexed { index, _ ->
            if (index == 0) {
                false
            } else {
                waypointIndices?.contains(index) ?: true
            }
        }
    }

    private fun waypointsWithinRange(
        first: List<Point>?,
        second: List<Point>?
    ): Boolean {
        if (first == null || second == null || first.size != second.size) {
            return false
        }

        first.forEachIndexed { index, firstPoint ->
            val secondPoint = second[index]
            val distance = TurfMeasurement.distance(firstPoint, secondPoint, UNIT_METRES)
            if (distance > MAX_WAYPOINTS_DISTANCE_DIFF_METERS) {
                return false
            }
        }

        return true
    }

    private fun handlerError(error: BillingServiceError) {
        when (error.code) {
            BillingServiceErrorCode.INVALID_SKU_ID,
            BillingServiceErrorCode.RESUME_FAILED,
            null -> {
                throw IllegalArgumentException(error.toString())
            }
            BillingServiceErrorCode.TOKEN_VALIDATION_FAILED -> {
                LoggerProvider.logger.e(
                    tag,
                    Message(error.toString())
                )
            }
        }
    }
}

private data class Waypoint(val index: Int, val point: Point)
