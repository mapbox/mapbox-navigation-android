package com.mapbox.navigation.core.history.model

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.internal.route.Waypoint
import com.mapbox.navigation.base.internal.utils.internalWaypoints
import com.mapbox.navigation.base.internal.utils.mapToSdkRouteOrigin
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.toNavigationRoute
import com.mapbox.navigation.core.navigator.toLocation
import com.mapbox.navigator.GetStatusHistoryRecord
import com.mapbox.navigator.HistoryRecord
import com.mapbox.navigator.HistoryRecordType
import com.mapbox.navigator.PushHistoryRecord
import com.mapbox.navigator.SetRouteHistoryRecord
import com.mapbox.navigator.UpdateLocationHistoryRecord
import kotlinx.coroutines.runBlocking
import java.net.URL

internal class HistoryEventMapper {

    private companion object {
        private const val NANOS_PER_SECOND = 1e-9
        private const val WAYPOINTS_JSON_KEY = "waypoints"
        private const val NAME_JSON_KEY = "name"
    }

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
        val navigationRoute = mapNavigationRoute(setRoute)
        return HistoryEventSetRoute(
            eventTimestamp = eventTimestamp,
            navigationRoute = navigationRoute,
            routeIndex = setRoute.routeIndex,
            legIndex = setRoute.legIndex,
            profile = mapToProfile(navigationRoute?.routeOptions),
            geometries = mapToGeometry(navigationRoute?.routeOptions),
            waypoints = mapToHistoryWaypoints(
                navigationRoute
            )
        )
    }

    /**
     * Function that tries to map the JSON found as route source to an instance of [NavigationRoute].
     *
     * For compatibility reasons, it contains fallbacks to support different formats
     * in which the route was saved over the lifetime of the history recorder.
     */
    private fun mapNavigationRoute(setRoute: SetRouteHistoryRecord): NavigationRoute? {
        val response = setRoute.routeResponse
        return if (response.isNullOrEmpty() || response == "{}") {
            null
        } else {
            val noOptionsException = IllegalArgumentException(
                "request URL or route options of set route history event cannot be null or empty"
            )
            try {
                retrieveNavigationRoute(response, setRoute, noOptionsException)
            } catch (t: Throwable) {
                if (t === noOptionsException) {
                    throw t
                } else {
                    try {
                        // Old records may not include waypoint namfes
                        val jsonResponse = addWaypointNames(response)
                        retrieveNavigationRoute(jsonResponse, setRoute, noOptionsException)
                    } catch (t: Throwable) {
                        if (t === noOptionsException) {
                            throw t
                        } else {
                            DirectionsRoute.fromJson(response).toBuilder()
                                .routeIndex("0")
                                .build()
                                .toNavigationRoute(
                                    setRoute.origin.mapToSdkRouteOrigin()
                                )
                        }
                    }
                }
            }
        }
    }

    private fun retrieveNavigationRoute(
        jsonResponse: String,
        setRoute: SetRouteHistoryRecord,
        noOptionsException: IllegalArgumentException
    ): NavigationRoute {
        val directionsResponse = DirectionsResponse.fromJson(jsonResponse)
        val routeOptions = setRoute.routeRequest?.let {
            // Old records may include empty routeRequest
            if (it.isEmpty()) return@let null
            RouteOptions.fromUrl(URL(it))
        } ?: directionsResponse.routes().firstOrNull()?.routeOptions() ?: throw noOptionsException
        return runBlocking {
            NavigationRoute.create(
                directionsResponse,
                routeOptions,
                setRoute.origin.mapToSdkRouteOrigin()
            ).first()
        }
    }

    private fun addWaypointNames(response: String): String {
        val gson = GsonBuilder().create()
        val jsonObject = gson.fromJson(response, JsonObject::class.java)
        val waypoints = jsonObject?.getAsJsonArray(WAYPOINTS_JSON_KEY)?.map { it.asJsonObject }
        waypoints?.forEach { waypoint ->
            if (!waypoint.has(NAME_JSON_KEY)) {
                waypoint.addProperty(NAME_JSON_KEY, "")
            }
        }
        return jsonObject.toString()
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
        pushHistoryRecord: PushHistoryRecord
    ): HistoryEventPushHistoryRecord = HistoryEventPushHistoryRecord(
        eventTimestamp,
        pushHistoryRecord.type,
        pushHistoryRecord.properties
    )
}
