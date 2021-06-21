package com.mapbox.navigation.navigator.internal

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigator.ActiveGuidanceGeometryEncoding
import com.mapbox.navigator.ActiveGuidanceMode
import com.mapbox.navigator.ActiveGuidanceOptions
import com.mapbox.navigator.Waypoint

object ActiveGuidanceOptionsMapper {

    private const val GEOJSON = "geojson"

    fun mapFrom(directionsRoute: DirectionsRoute?): ActiveGuidanceOptions {
        val mode = mapToActiveGuidanceMode(directionsRoute?.routeOptions()?.profile())
        val geometry = mapToActiveGuidanceGeometry(directionsRoute?.routeOptions()?.geometries())
        val waypoints = mapToWaypoints(directionsRoute?.routeOptions())
        return ActiveGuidanceOptions(mode, geometry, waypoints)
    }

    private fun mapToActiveGuidanceMode(profile: String?): ActiveGuidanceMode {
        return when (profile) {
            DirectionsCriteria.PROFILE_DRIVING_TRAFFIC, DirectionsCriteria.PROFILE_DRIVING -> {
                ActiveGuidanceMode.DRIVING
            }
            DirectionsCriteria.PROFILE_WALKING ->
                ActiveGuidanceMode.WALKING
            DirectionsCriteria.PROFILE_CYCLING ->
                ActiveGuidanceMode.CYCLING
            else ->
                ActiveGuidanceMode.DRIVING
        }
    }

    fun mapToActiveGuidanceMode(activeGuidanceMode: ActiveGuidanceMode): String {
        return when (activeGuidanceMode) {
            ActiveGuidanceMode.DRIVING -> DirectionsCriteria.PROFILE_DRIVING_TRAFFIC
            ActiveGuidanceMode.WALKING -> DirectionsCriteria.PROFILE_WALKING
            ActiveGuidanceMode.CYCLING -> DirectionsCriteria.PROFILE_CYCLING
        }
    }

    fun mapToActiveGuidanceGeometry(geometry: String?): ActiveGuidanceGeometryEncoding {
        return when (geometry) {
            DirectionsCriteria.GEOMETRY_POLYLINE ->
                ActiveGuidanceGeometryEncoding.POLYLINE5
            DirectionsCriteria.GEOMETRY_POLYLINE6 ->
                ActiveGuidanceGeometryEncoding.POLYLINE6
            GEOJSON ->
                ActiveGuidanceGeometryEncoding.GEO_JSON
            else ->
                ActiveGuidanceGeometryEncoding.POLYLINE6
        }
    }

    fun mapToGeometriesCriteria(encoding: ActiveGuidanceGeometryEncoding): String {
        return when (encoding) {
            ActiveGuidanceGeometryEncoding.POLYLINE5 ->
                DirectionsCriteria.GEOMETRY_POLYLINE
            ActiveGuidanceGeometryEncoding.POLYLINE6 ->
                DirectionsCriteria.GEOMETRY_POLYLINE6
            ActiveGuidanceGeometryEncoding.GEO_JSON -> GEOJSON
        }
    }

    private fun mapToWaypoints(routeOptions: RouteOptions?): List<Waypoint> =
        mutableListOf<Waypoint>().apply {
            routeOptions?.coordinatesList()?.forEachIndexed { index, point ->
                routeOptions.waypointIndicesList()?.let { waypointIndices ->
                    add(Waypoint(point, !waypointIndices.contains(index)))
                } ?: add(Waypoint(point, false))
            }
        }
}
