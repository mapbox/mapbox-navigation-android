package com.mapbox.navigation.core.history.model

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.internal.route.Waypoint
import com.mapbox.navigation.base.internal.route.createNavigationRoutes
import com.mapbox.navigation.base.internal.route.routeOptions
import com.mapbox.navigation.base.internal.utils.internalWaypoints
import com.mapbox.navigation.base.internal.utils.mapToSdkRouteOrigin
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.core.navigator.toLocation
import com.mapbox.navigator.GetStatusHistoryRecord
import com.mapbox.navigator.HistoryRecord
import com.mapbox.navigator.HistoryRecordType
import com.mapbox.navigator.PushHistoryRecord
import com.mapbox.navigator.SetRouteHistoryRecord
import com.mapbox.navigator.UpdateLocationHistoryRecord

internal class HistoryEventMapper {

    private companion object {
        private const val LOG_CATEGORY = "HistoryEventMapper"
        private const val NANOS_PER_SECOND = 1e-9
        private const val WAYPOINTS_JSON_KEY = "waypoints"
        private const val NAME_JSON_KEY = "name"
    }

    fun map(historyRecord: HistoryRecord): HistoryEvent {
        val eventTimestamp = historyRecord.timestampNanoseconds * NANOS_PER_SECOND
        return when (historyRecord.type) {
            HistoryRecordType.UPDATE_LOCATION -> mapUpdateLocation(
                eventTimestamp,
                historyRecord.updateLocation!!,
            )

            HistoryRecordType.GET_STATUS -> mapGetStatus(
                eventTimestamp,
                historyRecord.getStatus!!,
            )

            HistoryRecordType.SET_ROUTE -> mapSetRoute(
                eventTimestamp,
                historyRecord.setRoute!!,
            )

            HistoryRecordType.PUSH_HISTORY -> mapPushHistoryRecord(
                eventTimestamp,
                historyRecord.pushHistory!!,
            )
        }
    }

    private fun mapUpdateLocation(
        eventTimestamp: Double,
        updateLocation: UpdateLocationHistoryRecord,
    ) = HistoryEventUpdateLocation(
        eventTimestamp = eventTimestamp,
        location = updateLocation.location.toLocation(),
    )

    private fun mapGetStatus(
        eventTimestamp: Double,
        getStatus: GetStatusHistoryRecord,
    ) = HistoryEventGetStatus(
        eventTimestamp = eventTimestamp,
        elapsedRealtimeNanos = getStatus.monotonicTimestampNanoseconds,
    )

    private fun mapSetRoute(
        eventTimestamp: Double,
        setRoute: SetRouteHistoryRecord,
    ): HistoryEventSetRoute {
        val navigationRoutes = mapNavigationRoutes(setRoute)
        val navigationRoute = navigationRoutes.getOrNull(setRoute.routeIndex)
        return HistoryEventSetRoute(
            eventTimestamp = eventTimestamp,
            navigationRoute = navigationRoute,
            routeIndex = setRoute.routeIndex,
            legIndex = setRoute.legIndex,
            profile = mapToProfile(navigationRoute?.routeOptions),
            geometries = mapToGeometry(navigationRoute?.routeOptions),
            waypoints = mapToHistoryWaypoints(
                navigationRoute,
            ),
            navigationRoutes,
        )
    }

    /**
     * Function that tries to map the JSON found as route source to an instance of [NavigationRoute].
     *
     * For compatibility reasons, it contains fallbacks to support different formats
     * in which the route was saved over the lifetime of the history recorder.
     */
    private fun mapNavigationRoutes(setRoute: SetRouteHistoryRecord): List<NavigationRoute> {
        val response = setRoute.routeResponse
        return if (response.isNullOrEmpty() || response == "{}") {
            emptyList()
        } else {
            // TODO: support map matched route. NAVAND-1732
            createNavigationRoutes(
                setRoute.routeResponse!!,
                setRoute.routeRequest!!,
                setRoute.origin.mapToSdkRouteOrigin(),
            )
        }
    }

    private fun mapToHistoryWaypoints(
        navigationRoute: NavigationRoute?,
    ): List<HistoryWaypoint> =
        navigationRoute?.internalWaypoints()
            ?.map { HistoryWaypoint(it.location, it.type == Waypoint.SILENT) }
            ?: emptyList()

    @DirectionsCriteria.ProfileCriteria
    private fun mapToProfile(routeOptions: RouteOptions?): String {
        return routeOptions?.profile() ?: DirectionsCriteria.PROFILE_DRIVING_TRAFFIC
    }

    @DirectionsCriteria.GeometriesCriteria
    private fun mapToGeometry(routeOptions: RouteOptions?): String {
        return routeOptions?.geometries() ?: DirectionsCriteria.GEOMETRY_POLYLINE6
    }

    private fun mapPushHistoryRecord(
        eventTimestamp: Double,
        pushHistoryRecord: PushHistoryRecord,
    ): HistoryEventPushHistoryRecord = HistoryEventPushHistoryRecord(
        eventTimestamp,
        pushHistoryRecord.type,
        pushHistoryRecord.properties,
    )
}
