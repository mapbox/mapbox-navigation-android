package com.mapbox.navigation.navigator

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.navigator.ActiveGuidanceGeometryEncoding
import com.mapbox.navigator.ActiveGuidanceMode
import com.mapbox.navigator.ActiveGuidanceOptions
import com.mapbox.navigator.Waypoint

internal object ActiveGuidanceOptionsMapper {

    private const val GEOJSON = "geojson"

    fun mapFrom(directionsRoute: DirectionsRoute?): ActiveGuidanceOptions {
        val mode = mapToActiveGuidanceMode(directionsRoute?.routeOptions()?.profile())
        val geometry = mapToActiveGuidanceGeometry(directionsRoute?.routeOptions()?.geometries())
        val coordinates = mapToActiveGuidanceCoordinates(
            directionsRoute?.routeOptions()?.coordinates() ?: emptyList()
        )
        return ActiveGuidanceOptions(mode, geometry, coordinates)
    }

    private fun mapToActiveGuidanceMode(profile: String?): ActiveGuidanceMode {
        return when (profile) {
            DirectionsCriteria.PROFILE_DRIVING_TRAFFIC, DirectionsCriteria.PROFILE_DRIVING -> {
                ActiveGuidanceMode.KDRIVING
            }
            DirectionsCriteria.PROFILE_WALKING ->
                ActiveGuidanceMode.KWALKING
            DirectionsCriteria.PROFILE_CYCLING ->
                ActiveGuidanceMode.KCYCLING
            else ->
                ActiveGuidanceMode.KDRIVING
        }
    }

    private fun mapToActiveGuidanceGeometry(geometry: String?): ActiveGuidanceGeometryEncoding {
        return when (geometry) {
            DirectionsCriteria.GEOMETRY_POLYLINE ->
                ActiveGuidanceGeometryEncoding.KPOLYLINE5
            DirectionsCriteria.GEOMETRY_POLYLINE6 ->
                ActiveGuidanceGeometryEncoding.KPOLYLINE6
            GEOJSON ->
                ActiveGuidanceGeometryEncoding.KGEO_JSON
            else ->
                ActiveGuidanceGeometryEncoding.KPOLYLINE6
        }
    }

    private fun mapToActiveGuidanceCoordinates(coordinates: List<Point>): List<Waypoint> {
        val waypoints = mutableListOf<Waypoint>()
        for (coordinate in coordinates) {
            // TODO: Hard-coding isSilent to false, needs to be updated in https://github.com/mapbox/mapbox-navigation-android/pull/3581
            //   Noting that origin and destination must be non-silent i.e. isSilent === false
            val waypoint = Waypoint(Point.fromJson(coordinate.toJson()), false)
            waypoints.add(waypoint)
        }
        return waypoints.toList()
    }
}
