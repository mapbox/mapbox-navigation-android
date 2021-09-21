package com.mapbox.navigation.core.internal.utils

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.navigation.base.internal.utils.isSameRoute
import com.mapbox.navigation.base.internal.utils.isSameUuid
import com.mapbox.navigation.core.directions.session.RoutesExtra
import com.mapbox.navigation.core.trip.session.OffRouteObserver
import com.mapbox.navigation.core.trip.session.TripSession
import com.mapbox.navigation.utils.internal.ifNonNull
import com.mapbox.navigation.utils.internal.logD

/*
 * ┌───────────────┐      ┌──────────────┐
 * │  LegacyRoutes │      │  NewRoutes   │
 * │               │      │              │
 * │  list         │      │  list        │
 * └─────┬─────────┘      └───────┬──────┘
 *       │                        │
 *       │    ┌──────────────┐    │
 *       │    │ NewRoutes    │    │
 *       └───►│              │◄───┘
 *            │ is empty     │
 *            └─────┬────┬───┘
 *                  │    │      YES
 *               NO │    └───────────► Reason is CLEAN_UP
 *                  │
 *            ┌─────▼────────┐
 *            │ LegacyRoute  │
 *            │ is empty;    │
 *            │ NewRoutes    │
 *            │ is NOT empty.│
 *            └─────┬────┬───┘
 *                  │    │       YES
 *                NO│    └───────────► Reason is NEW_ROUTES
 *                  │
 *            ┌─────▼──────────┐
 *            │ OffRoute event;│
 *            │ Primary routes'│
 *            │ coordinates are│
 *            │ the same.      │
 *            └─────┬────┬─────┘
 *                  │    │
 *                  │    │       YES
 *                NO│    └──────────► Reason is REROUTE
 *                  │
 *            ┌─────▼──────────┐
 *            │Primary routes' │
 *            │UUID's and      │
 *            │geometries are  │
 *            │the same        │
 *            └─────┬─────┬────┘
 *                  │     │      YES
 *               NO │     └────────► Reason is REFRESH
 *                  │
 *            ┌─────▼──────────┐
 *            │Primary routes' │
 *            │coordinates     │
 *            │are the same    │
 *            └──┬─────┬───────┘
 *               │     │
 *               │     │
 *               │     └────────────► Reason is ALTERNATIVE
 *               │
 *               ▼
 *          Reason is NEW_ROUTES
 */
internal class RoutesUpdateReasonHelper(tripSession: TripSession) {

    private var offRoute = false

    private val offRouteObserver = OffRouteObserver { offRoute ->
        this.offRoute = offRoute
    }

    private companion object {
        private val TAG = Tag("MbxRoutesUpdateReasonHelper")
    }

    init {
        tripSession.registerOffRouteObserver(offRouteObserver)
    }

    /**
     * Provides the reason why the routes have been updated.
     *
     * Reason is provided based on the following (priority matches with sequence):
     * - if [newRoutes] is empty -> reason is [RoutesExtra.ROUTES_UPDATE_REASON_CLEAN_UP];
     * - if [legacyRoutes] is empty and [newRoutes] is NOT empty -> reason is [RoutesExtra.ROUTES_UPDATE_REASON_NEW];
     * - if off-route event happened and [RouteOptions.coordinatesList] are same for primary routes of
     * [legacyRoutes] and [newRoutes] (except **origin** coordinate) -> reason is [RoutesExtra.ROUTES_UPDATE_REASON_REROUTE];
     * - if [DirectionsRoute.requestUuid] (see [isSameUuid]) and [DirectionsRoute.geometry]
     * (or **steps' names**, see [isSameRoute]) are the same of primary routes
     * of [legacyRoutes] and [newRoutes] (except **origin** coordinate) -> reason is [RoutesExtra.ROUTES_UPDATE_REASON_REFRESH];
     * - if [RouteOptions.coordinatesList] are same for primary routes of [legacyRoutes] and
     * [newRoutes] (except **origin** coordinate) -> reason is [RoutesExtra.ROUTES_UPDATE_REASON_ALTERNATIVE];
     * - otherwise -> reason is [RoutesExtra.ROUTES_UPDATE_REASON_NEW].
     *
     * Note: function must be invoked before set a new route to NN, because it relies on
     * off-route events.
     */
    @RoutesExtra.RoutesUpdateReason
    internal fun getReason(
        legacyRoutes: List<DirectionsRoute>,
        newRoutes: List<DirectionsRoute>
    ): String {
        when {
            newRoutes.isEmpty() ->
                return RoutesExtra.ROUTES_UPDATE_REASON_CLEAN_UP
            legacyRoutes.isEmpty() && newRoutes.isNotEmpty() ->
                return RoutesExtra.ROUTES_UPDATE_REASON_NEW
        }

        val isSamePrimaryRoutes = sameRoutesForPrimaryRoute(legacyRoutes, newRoutes)
        val isSameUuidPrimaryRoutes = sameUuidForPrimaryRoute(legacyRoutes, newRoutes)
        val isSameRouteOptionsCoordinatesPrimaryRoutes =
            sameRouteOptionsCoordinatesForPrimaryRoute(legacyRoutes, newRoutes)
        return when {
            offRoute && isSameRouteOptionsCoordinatesPrimaryRoutes ->
                RoutesExtra.ROUTES_UPDATE_REASON_REROUTE
            isSameUuidPrimaryRoutes && isSamePrimaryRoutes ->
                RoutesExtra.ROUTES_UPDATE_REASON_REFRESH
            isSameRouteOptionsCoordinatesPrimaryRoutes ->
                RoutesExtra.ROUTES_UPDATE_REASON_ALTERNATIVE
            else -> RoutesExtra.ROUTES_UPDATE_REASON_NEW
        }
    }

    private fun sameUuidForPrimaryRoute(
        legacyRoutes: List<DirectionsRoute>,
        newRoutes: List<DirectionsRoute>
    ): Boolean =
        newRoutes.firstOrNull()?.isSameUuid(legacyRoutes.firstOrNull()) ?: false

    private fun sameRoutesForPrimaryRoute(
        legacyRoutes: List<DirectionsRoute>,
        newRoutes: List<DirectionsRoute>
    ): Boolean =
        newRoutes.firstOrNull()?.isSameRoute(legacyRoutes.firstOrNull()) ?: false

    /**
     * If a primary route has the same route options' coordinates - then **reason** should be
     * re-route or alternative route.
     */
    private fun sameRouteOptionsCoordinatesForPrimaryRoute(
        legacyRoutes: List<DirectionsRoute>,
        newRoutes: List<DirectionsRoute>
    ): Boolean =
        ifNonNull(
            legacyRoutes.firstOrNull()?.routeOptions()?.coordinatesList(),
            newRoutes.firstOrNull()?.routeOptions()?.coordinatesList()
        ) { legacyCoordinates, newCoordinates ->
            val reversedLegacyCoordinates = legacyCoordinates.reversed()
            val reversedNewCoordinates = newCoordinates.reversed()

            for (idx in 0 until reversedNewCoordinates.lastIndex) { // until lastIndex-> skip Origin
                if (reversedNewCoordinates[idx] != reversedLegacyCoordinates.getOrNull(idx)) {
                    return@ifNonNull false
                }
            }
            return@ifNonNull true
        } ?: run {
            logD(
                TAG,
                Message("Cannot compare routes coordinates because they do not exist.")
            )
            false
        }
}
