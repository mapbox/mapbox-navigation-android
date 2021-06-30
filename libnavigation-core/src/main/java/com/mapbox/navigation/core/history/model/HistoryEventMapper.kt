package com.mapbox.navigation.core.history.model

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.core.navigator.toLocation
import com.mapbox.navigation.navigator.internal.ActiveGuidanceOptionsMapper.mapToActiveGuidanceMode
import com.mapbox.navigation.navigator.internal.ActiveGuidanceOptionsMapper.mapToGeometriesCriteria
import com.mapbox.navigator.GetStatusHistoryRecord
import com.mapbox.navigator.HistoryRecord
import com.mapbox.navigator.HistoryRecordType
import com.mapbox.navigator.SetRouteHistoryRecord
import com.mapbox.navigator.UpdateLocationHistoryRecord
import com.mapbox.navigator.Waypoint

internal object HistoryEventMapper {

    fun map(historyRecord: HistoryRecord): HistoryEvent {
        return when (historyRecord.type) {
            HistoryRecordType.UPDATE_LOCATION -> mapUpdateLocation(historyRecord.updateLocation!!)
            HistoryRecordType.GET_STATUS -> mapGetStatus(historyRecord.getStatus!!)
            HistoryRecordType.SET_ROUTE -> mapSetRoute(historyRecord.setRoute!!)
        }
    }

    private fun mapUpdateLocation(
        updateLocation: UpdateLocationHistoryRecord
    ) = HistoryEventUpdateLocation(
        location = updateLocation.location.toLocation()
    )

    private fun mapGetStatus(
        getStatus: GetStatusHistoryRecord
    ) = HistoryEventGetStatus(
        elapsedRealtimeNanos = getStatus.monotonicTimestampNanoseconds
    )

    private fun mapSetRoute(
        setRoute: SetRouteHistoryRecord
    ) = HistoryEventSetRoute(
        directionsRoute = setRoute.routeResponse?.let { DirectionsRoute.fromJson(it) },
        routeIndex = setRoute.routeIndex,
        legIndex = setRoute.legIndex,
        profile = mapToActiveGuidanceMode(setRoute.options.mode),
        geometries = mapToGeometriesCriteria(setRoute.options.geometryEncoding),
        waypoints = mapToWaypoints(setRoute.options.waypoints)
    )

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
}
