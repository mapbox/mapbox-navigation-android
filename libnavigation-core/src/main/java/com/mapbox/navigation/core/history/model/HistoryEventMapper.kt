package com.mapbox.navigation.core.history.model

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.core.navigator.toLocation
import com.mapbox.navigation.navigator.internal.ActiveGuidanceOptionsMapper.mapToActiveGuidanceMode
import com.mapbox.navigation.navigator.internal.ActiveGuidanceOptionsMapper.mapToGeometriesCriteria
import com.mapbox.navigator.GetStatusHistoryRecord
import com.mapbox.navigator.HistoryRecord
import com.mapbox.navigator.HistoryRecordType
import com.mapbox.navigator.PushHistoryRecord
import com.mapbox.navigator.SetRouteHistoryRecord
import com.mapbox.navigator.UpdateLocationHistoryRecord
import com.mapbox.navigator.Waypoint

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
    ) = HistoryEventSetRoute(
        eventTimestamp = eventTimestamp,
        directionsRoute = mapDirectionsRoute(setRoute.routeResponse),
        routeIndex = setRoute.routeIndex,
        legIndex = setRoute.legIndex,
        profile = mapToActiveGuidanceMode(setRoute.options.mode),
        geometries = mapToGeometriesCriteria(setRoute.options.geometryEncoding),
        waypoints = mapToWaypoints(setRoute.options.waypoints)
    )

    private fun mapDirectionsRoute(routeResponse: String?): DirectionsRoute? {
        return if (routeResponse.isNullOrEmpty() || routeResponse == "{}") {
            null
        } else {
            return DirectionsRoute.fromJson(routeResponse)
        }
    }

    private fun mapToWaypoints(
        waypoints: List<Waypoint>
    ): List<HistoryWaypoint> {
        return waypoints.map { waypoint ->
            HistoryWaypoint(
                waypoint.coordinate,
                waypoint.isSilent
            )
        }
    }

    private fun mapPushHistoryRecord(
        eventTimestamp: Double,
        pushHistoryRecord: PushHistoryRecord
    ): HistoryEventPushHistoryRecord = HistoryEventPushHistoryRecord(
        eventTimestamp,
        pushHistoryRecord.type,
        pushHistoryRecord.properties
    )

    private companion object {
        private const val NANOS_PER_SECOND = 1e-9
    }
}
