/**
 * Tampering with any file that contains billing code is a violation of Mapbox Terms of Service and will result in enforcement of the penalties stipulated in the ToS.
 */

package com.mapbox.navigation.core.accounts

import com.mapbox.common.BillingServiceError
import com.mapbox.common.BillingServiceErrorCode
import com.mapbox.common.BillingSessionStatus
import com.mapbox.common.SessionSKUIdentifier
import com.mapbox.common.UserSKUIdentifier
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.arrival.ArrivalObserver
import com.mapbox.navigation.core.arrival.ArrivalProgressObserver
import com.mapbox.navigation.core.directions.session.DirectionsSession
import com.mapbox.navigation.core.trip.session.NavigationSession
import com.mapbox.navigation.core.trip.session.NavigationSessionState
import com.mapbox.navigation.core.trip.session.NavigationSessionStateObserver
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.utils.internal.logI
import com.mapbox.navigation.utils.internal.logW
import com.mapbox.turf.TurfConstants.UNIT_METRES
import com.mapbox.turf.TurfMeasurement
import java.util.concurrent.TimeUnit

/**
 * The billing controller takes 3 value as an input:
 * - the current [NavigationSessionState]
 * - any new route that is set by the developer **before it has a chance to change the above state**
 * - a notification when a new leg of a route starts
 *
 * Given those 3 inputs, the controller triggers the correct billing events.
 *
 * ### [NavigationSessionState]
 * Below are diagrams for each of the possible [NavigationSessionState]s.
 *
 * [NavigationSessionState.Idle]
 *                                 ┌──────────┐
 *                                 │IDLE STATE│
 *                                 └────┬─────┘
 *                                      │
 *                                      │
 *                                      ▼
 *                      NO  ┌───────────────────────┐  YES
 *                    ┌─────┤IS ANY SESSION RUNNING?├─────┐
 *                    │     └───────────────────────┘     │
 *                    │                                   │
 *                    ▼                                   ▼
 *               ┌──────────┐                       ┌─────────────┐
 *               │DO NOTHING│                       │PAUSE SESSION│
 *               └──────────┘                       └─────────────┘
 *
 * [NavigationSessionState.FreeDrive]
 *                             ┌────────────────┐
 *                             │FREE DRIVE STATE│
 *                             └───────┬────────┘
 *                                     │
 *                                     │
 *                                     ▼
 *                      NO    ┌──────────────────┐     YES
 *                    ┌───────┤  IS ANY SESSION  ├────────┐
 *                    │       │PAUSED OR RUNNING?│        │
 *                    │       └──────────────────┘        │
 *                    ▼                                   ▼
 *            ┌────────────────┐             NO    ┌─────────────┐    YES
 *            │START FREE DRIVE│           ┌───────┤IS FREE DRIVE├───────┐
 *            │BILLING SESSION │           │       │   PAUSED?   │       │
 *            └────────────────┘           │       └─────────────┘       │
 *                    ▲                    ▼                             ▼
 *                    │             ┌───────────────┐               ┌──────────┐
 *                    │             │     STOP      │               │  RESUME  │
 *                    │             │ACTIVE GUIDANCE│               │FREE DRIVE│
 *                    │             └───────┬───────┘               └──────────┘
 *                    │                     │
 *                    └─────────────────────┘
 *
 * [NavigationSessionState.ActiveGuidance]
 *                    ┌─────────────────────┐
 *                    │ACTIVE GUIDANCE STATE│
 *                    └──────────┬──────────┘
 *                               │
 *                               │
 *                               ▼
 *                NO    ┌──────────────────┐     YES
 *              ┌───────┤  IS ANY SESSION  ├────────┐
 *              │       │PAUSED OR RUNNING?│        │
 *              │       └──────────────────┘        │
 *              ▼                                   ▼
 *   ┌─────────────────────┐           NO    ┌──────────────────┐  YES
 *   │START ACTIVE GUIDANCE│         ┌───────┤IS ACTIVE GUIDANCE├─────┐
 *   │   BILLING SESSION   │         │       │      PAUSED?     │     │
 *   └─────────────────────┘         │       └──────────────────┘     │
 *              ▲                    ▼                                ▼
 *              │               ┌──────────┐                   ┌───────────────┐
 *              │               │   STOP   │                   │     RESUME    │
 *              │               │FREE DRIVE│                   │ACTIVE GUIDANCE│
 *              │               └─────┬────┘                   └───────────────┘
 *              │                     │
 *              └─────────────────────┘
 *
 * Additionally, whenever a [NavigationSessionState] is not [NavigationSessionState.Idle], we'll trigger a new MAU event.
 * Triggering this event multiple times within the same calendar month has no effect, the events are later de-duplicated.
 *
 * The controller implements an automatic pausing system - if the state becomes [NavigationSessionState.Idle],
 * we do not stop the billing session immediately, we give an opportunity to resume the same session type without incurring additional costs.
 * If the state changes though, we'll start a new session.
 *
 * ### New route
 * When a new route is set and we're already in active guidance, the [NavigationSessionStateObserver] will not fire again
 * but we still might need to start a new billing session if the route is significantly different than the previous one.
 *               ┌─────────────┐
 *               │NEW ROUTE SET│
 *               └──────┬──────┘
 *                      │
 *                      ▼
 *            ┌──────────────────┐  YES
 *        NO  │IS ACTIVE GUIDANCE├─────┐
 *      ┌─────┤     RUNNING?     │     │
 *      │     └──────────────────┘     │
 *      │                              ▼
 *      ▼                       ┌───────────────────────────────────────┐
 * ┌──────────┐             NO  │        IS EVERY NEW WAYPOINT          │  YES
 * │DO NOTHING│           ┌─────┤WITHIN 100M OF EACH REMAINING WAYPOINT?├─────┐
 * └──────────┘           │     └───────────────────────────────────────┘     │
 *                        │                                                   │
 *                        ▼                                                   ▼
 *                ┌───────────────┐                                      ┌──────────┐
 *                │     STOP      │                                      │DO NOTHING│
 *                │ACTIVE GUIDANCE│                                      └──────────┘
 *                └───────┬───────┘
 *                        │
 *                        ▼
 *             ┌─────────────────────┐
 *             │START ACTIVE GUIDANCE│
 *             │   BILLING SESSION   │
 *             └─────────────────────┘
 *
 * ### New route leg started
 * Whenever a new leg of a route is started, we register a new Active Guidance session.
 *                 ┌───────────────┐
 *                 │NEW LEG STARTED│
 *                 └── ────┬───────┘
 *                         │
 *                         ▼
 *               ┌──────────────────┐
 *           NO  │IS ACTIVE GUIDANCE│  YES
 *         ┌─────┤     RUNNING?     ├─────┐
 *         │     └──────────────────┘     │
 *         │                              │
 *         ▼                              ▼
 * ┌───────────────┐        ┌─────────────────────────┐
 * │THROW EXCEPTION│        │START NEW ACTIVE GUIDANCE│
 * └───────────────┘        │      BILLING SESSION    │
 *                          └─────────────────────────┘
 *
 * ### Summary
 * All of the above interactions gives the below possible cycle of a billing session.
 *                     ┌──────────┐
 *   ┌────────────────►│NO SESSION│◄───────────────┐
 *   │                 └──┬────┬──┘                │
 *   │                    │    │                   │
 *   │         ┌──────────┘    └──────────┐        │
 *   │         ▼                          ▼        │
 *   │  ┌───────────────┐            ┌──────────┐  │
 *   │  │ACTIVE GUIDANCE│◄──────────►│FREE DRIVE│  │
 *   ├──┤    ACTIVE     │            │  ACTIVE  ├──┤
 *   │  │               │◄────┐  ┌──►│          │  │
 *   │  └──────┬────────┘     │  │   └─────┬────┘  │
 *   │         │            ┌─┼──┘         │       │
 *   │         ▼            │ │            ▼       │
 *   │  ┌───────────────┐   │ │      ┌──────────┐  │
 *   │  │ACTIVE GUIDANCE├───┘ └──────┤FREE DRIVE│  │
 *   └──┤    PAUSED     │            │  PAUSED  ├──┘
 *      └───────────────┘            └──────────┘
 *
 * Free drive session is valid for 1 hour and then we trust in the native implementation to start a new session for us automatically.
 *
 * Active guidance session is valid for as long as the server allows (currently up to 12 hours).
 * If we exceed the maximum, we again let the native implementation to start a new session for us automatically.
 */
internal class BillingController(
    private val navigationSession: NavigationSession,
    private val arrivalProgressObserver: ArrivalProgressObserver,
    private val accessToken: String,
    private val tripSession: TripSession,
) {

    private companion object {
        private const val logCategory = "BillingController"
        private const val BILLING_EXPLANATION_CATEGORY = "BillingExplanation"
        private const val MAX_WAYPOINTS_DISTANCE_DIFF_METERS = 100.0
    }

    private val billingService = BillingServiceProvider.getInstance()

    private val navigationSessionStateObserver =
        NavigationSessionStateObserver { navigationSessionState ->
            if (navigationSessionState != NavigationSessionState.Idle) {
                // always trigger an MAU event if a session starts
                billingService.triggerUserBillingEvent(
                    accessToken,
                    "", // empty string results in default user agent
                    UserSKUIdentifier.NAV2_SES_MAU
                ) {
                    handlerError(it)
                }
            }

            when (navigationSessionState) {
                is NavigationSessionState.Idle -> {
                    getRunningOrPausedSessionSkuId()?.let {
                        billingService.pauseBillingSession(it)
                        logI(BILLING_EXPLANATION_CATEGORY) {
                            "${it.publicName} has been paused because Nav SDK is in Idle state"
                        }
                    }
                }
                is NavigationSessionState.FreeDrive -> {
                    resumeOrBeginBillingSession(
                        SessionSKUIdentifier.NAV2_SES_FDTRIP,
                        validity = TimeUnit.HOURS.toMillis(1), // validity of 1hr
                        "Nav SDK is in free drive state"
                    )
                }
                is NavigationSessionState.ActiveGuidance -> {
                    resumeOrBeginBillingSession(
                        SessionSKUIdentifier.NAV2_SES_TRIP,
                        validity = 0, // default validity, 12hrs
                        "Nav SDK is in Active Guidance state"
                    )
                }
            }
        }

    private val arrivalObserver = object : ArrivalObserver {
        override fun onWaypointArrival(routeProgress: RouteProgress) {
            // no-op
        }

        override fun onNextRouteLegStart(routeLegProgress: RouteLegProgress) {
            val runningSessionSkuId = getRunningOrPausedSessionSkuId()
            check(runningSessionSkuId == SessionSKUIdentifier.NAV2_SES_TRIP) {
                """
                    |Next route leg started while an active guidance session is not running.
                    |Actual active SKU: $runningSessionSkuId
                """.trimMargin()
            }
            beginBillingSession(
                SessionSKUIdentifier.NAV2_SES_TRIP,
                validity = 0, // default validity, 12hrs
                "Nav SDK switched to the next route leg"
            )
        }

        override fun onFinalDestinationArrival(routeProgress: RouteProgress) {
            // no-op
        }
    }

    init {
        navigationSession.registerNavigationSessionStateObserver(navigationSessionStateObserver)
        arrivalProgressObserver.registerObserver(arrivalObserver)
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
    fun onExternalRouteSet(navigationRoute: NavigationRoute) {
        val runningSessionSkuId = getRunningOrPausedSessionSkuId()
        if (runningSessionSkuId == SessionSKUIdentifier.NAV2_SES_TRIP) {
            val currentRemainingWaypoints = getRemainingWaypointsOnRoute(
                tripSession.getRouteProgress()
            )
            val newWaypoints = getWaypointsOnRoute(navigationRoute)

            if (!waypointsWithinRange(currentRemainingWaypoints, newWaypoints)) {
                val wasSessionPaused = billingService.getSessionStatus(
                    SessionSKUIdentifier.NAV2_SES_TRIP
                ) == BillingSessionStatus.SESSION_PAUSED
                beginBillingSession(
                    SessionSKUIdentifier.NAV2_SES_TRIP,
                    validity = 0, // default validity, 12hrs
                    "destination has been changed. " +
                        "Old waypoints: $currentRemainingWaypoints," +
                        "new waypoints: $newWaypoints"
                )
                if (wasSessionPaused) {
                    billingService.pauseBillingSession(SessionSKUIdentifier.NAV2_SES_TRIP)
                    logI(BILLING_EXPLANATION_CATEGORY) {
                        "${runningSessionSkuId.publicName} has been paused because " +
                            "it used to be paused before destinations update"
                    }
                }
            }
        }
    }

    fun onDestroy() {
        navigationSession.unregisterNavigationSessionStateObserver(navigationSessionStateObserver)
        arrivalProgressObserver.unregisterObserver(arrivalObserver)
        getRunningOrPausedSessionSkuId()?.let {
            billingService.stopBillingSession(it)
            logI(BILLING_EXPLANATION_CATEGORY) {
                "${it.publicName} has been stopped because Nav SDK is destroyed"
            }
        }
    }

    /**
     * Resumes a paused session if the sku identifiers match, otherwise, starts a new session.
     */
    private fun resumeOrBeginBillingSession(
        skuId: SessionSKUIdentifier,
        validity: Long,
        reason: String
    ) {
        val runningSessionSkuId = getRunningOrPausedSessionSkuId()
        if (runningSessionSkuId == skuId) {
            billingService.resumeBillingSession(runningSessionSkuId) {
                handlerError(it)
                if (it.code == BillingServiceErrorCode.RESUME_FAILED) {
                    logW(
                        "Session resumption failed, starting a new one instead.",
                        logCategory
                    )
                    logI(BILLING_EXPLANATION_CATEGORY) {
                        "Failed to resume ${skuId.publicName}(${it.message})."
                    }
                    beginBillingSession(skuId, validity, reason)
                } else {
                    logI(BILLING_EXPLANATION_CATEGORY) {
                        "${skuId.publicName} has ben resumed because $reason"
                    }
                }
            }
        } else {
            beginBillingSession(skuId, validity, reason)
        }
    }

    /**
     * Stops any running session and starts a new one with provided arguments.
     */
    private fun beginBillingSession(
        skuId: SessionSKUIdentifier,
        validity: Long,
        reason: String
    ) {
        val runningSessionSkuId = getRunningOrPausedSessionSkuId()
        if (runningSessionSkuId != null) {
            billingService.stopBillingSession(runningSessionSkuId)
            logI(BILLING_EXPLANATION_CATEGORY) {
                "${runningSessionSkuId.publicName} has been stopped because $reason"
            }
        }
        billingService.beginBillingSession(
            accessToken,
            "", // empty string result in default user agent
            skuId,
            {
                handlerError(it)
            },
            validity
        )
        logI(BILLING_EXPLANATION_CATEGORY) {
            "${skuId.publicName} has been started because $reason"
        }
    }

    private fun getRunningOrPausedSessionSkuId(): SessionSKUIdentifier? {
        val possibleSessionIds = listOf(
            SessionSKUIdentifier.NAV2_SES_TRIP,
            SessionSKUIdentifier.NAV2_SES_FDTRIP
        )

        data class SkuSessionStatus(
            val skuId: SessionSKUIdentifier,
            val status: BillingSessionStatus
        )

        val sessionStatuses = possibleSessionIds.map { skuId ->
            SkuSessionStatus(skuId, billingService.getSessionStatus(skuId))
        }

        val activeOrPausedSessions = sessionStatuses.filter {
            it.status != BillingSessionStatus.NO_SESSION
        }

        check(
            activeOrPausedSessions.size <= 1
        ) {
            "More than one session is active or paused: $sessionStatuses"
        }

        return activeOrPausedSessions.firstOrNull()?.skuId
    }

    /**
     * Returns a list of remaining [Point]s that mark ends of legs on the route from the current [RouteProgress],
     * ignoring origin.
     */
    private fun getRemainingWaypointsOnRoute(routeProgress: RouteProgress?): List<Point>? {
        return routeProgress?.navigationRoute?.let { route ->
            val waypoints = route.waypoints
            waypoints?.drop(
                (waypoints.size - routeProgress.remainingWaypoints).coerceAtLeast(1)
            )?.map {
                it.location()
            }
        }
    }

    /**
     * Returns a list of [Point]s that mark ends of legs on the route,
     * ignoring origin.
     */
    private fun getWaypointsOnRoute(navigationRoute: NavigationRoute): List<Point>? {
        return navigationRoute.waypoints?.drop(1)?.map {
            it.location()
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
            null -> {
                throw IllegalArgumentException(error.toString())
            }
            BillingServiceErrorCode.RESUME_FAILED,
            BillingServiceErrorCode.TOKEN_VALIDATION_FAILED -> {
                logW(
                    error.toString(),
                    logCategory
                )
            }
        }
    }
}

private data class Waypoint(val index: Int, val point: Point)

private val SessionSKUIdentifier.publicName get() = when (this) {
    SessionSKUIdentifier.NAV2_SES_TRIP -> "Active Guidance Trip Session"
    SessionSKUIdentifier.NAV2_SES_FDTRIP -> "Free Drive Trip Session"
}
