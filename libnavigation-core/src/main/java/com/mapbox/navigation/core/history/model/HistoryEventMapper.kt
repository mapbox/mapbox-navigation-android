package com.mapbox.navigation.core.history.model

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.core.navigator.toLocation
import com.mapbox.navigator.GetStatusHistoryRecord
import com.mapbox.navigator.HistoryRecord
import com.mapbox.navigator.HistoryRecordType
import com.mapbox.navigator.PushHistoryRecord
import com.mapbox.navigator.SetRouteHistoryRecord
import com.mapbox.navigator.UpdateLocationHistoryRecord
import java.net.URL

internal class HistoryEventMapper {

    fun map(historyRecord: HistoryRecord): HistoryEvent {
        val eventTimestamp = historyRecord.timestampNanoseconds * NANOS_PER_SECOND
        return when (historyRecord.type) {
            HistoryRecordType.UPDATE_LOCATION -> mapUpdateLocation(
                eventTimestamp,
                historyRecord.updateLocation!!
            )
            HistoryRecordType.GET_STATUS -> mapGetStatus(
                eventTimestamp,
                historyRecord.getStatus!!
            )
            HistoryRecordType.SET_ROUTE -> mapSetRoute(
                eventTimestamp,
                historyRecord.setRoute!!
            )
            HistoryRecordType.PUSH_HISTORY -> mapPushHistoryRecord(
                eventTimestamp,
                historyRecord.pushHistory!!
            )
        }
    }

    private fun mapUpdateLocation(
        eventTimestamp: Double,
        updateLocation: UpdateLocationHistoryRecord
    ) = HistoryEventUpdateLocation(
        eventTimestamp = eventTimestamp,
        location = updateLocation.location.toLocation()
    )

    private fun mapGetStatus(
        eventTimestamp: Double,
        getStatus: GetStatusHistoryRecord
    ) = HistoryEventGetStatus(
        eventTimestamp = eventTimestamp,
        elapsedRealtimeNanos = getStatus.monotonicTimestampNanoseconds
    )

    private fun mapSetRoute(
        eventTimestamp: Double,
        setRoute: SetRouteHistoryRecord
    ): HistoryEventSetRoute {
        val directionsRoute = mapDirectionsRoute(setRoute.routeResponse)
        val routeOptions = getRouteOptions(
            requestUrlString = setRoute.routeRequest,
            route = directionsRoute
        )
        return HistoryEventSetRoute(
            eventTimestamp = eventTimestamp,
            directionsRoute = directionsRoute,
            routeIndex = setRoute.routeIndex,
            legIndex = setRoute.legIndex,
            profile = mapToProfile(routeOptions),
            geometries = mapToGeometry(routeOptions),
            waypoints = mapToWaypoints(routeOptions)
        )
    }

    private fun mapDirectionsRoute(routeResponse: String?): DirectionsRoute? {
        return if (routeResponse.isNullOrEmpty() || routeResponse == "{}") {
            null
        } else {
            return try {
                // https://github.com/mapbox/mapbox-navigation-native/issues/4296
                // TODO we may create a data object separate from DirectionsResponse
                val directionsResponse = DirectionsResponse.fromJson(routeResponse)
                directionsResponse.routes().firstOrNull()
            } catch (t: Throwable) {
                DirectionsRoute.fromJson(routeResponse)
            }
        }
    }

    private fun mapToWaypoints(routeOptions: RouteOptions?): List<HistoryWaypoint> {
        val coordinatesList = routeOptions?.coordinatesList()
        val waypointIndices = routeOptions?.waypointIndicesList()
        return coordinatesList?.mapIndexed { index, coordinate ->
            HistoryWaypoint(
                point = coordinate,
                isSilent = waypointIndices?.contains(index)?.not() == true
            )
        } ?: emptyList()
    }

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
        pushHistoryRecord: PushHistoryRecord
    ): HistoryEventPushHistoryRecord = HistoryEventPushHistoryRecord(
        eventTimestamp,
        pushHistoryRecord.type,
        pushHistoryRecord.properties
    )

    private fun getRouteOptions(requestUrlString: String?, route: DirectionsRoute?): RouteOptions? {
        return if (route != null) {
            if (requestUrlString == null || requestUrlString.isBlank()) {
                route.routeOptions() ?: throw IllegalArgumentException(
                    "request URL or route options of set route event cannot be null or empty"
                )
            } else {
                RouteOptions.fromUrl(URL(requestUrlString))
            }
        } else {
            null
        }
    }

    private companion object {
        private const val NANOS_PER_SECOND = 1e-9
    }
}
