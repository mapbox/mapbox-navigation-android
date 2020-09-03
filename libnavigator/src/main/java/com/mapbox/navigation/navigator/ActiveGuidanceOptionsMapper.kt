package com.mapbox.navigation.navigator

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigator.ActiveGuidanceGeometryEncoding
import com.mapbox.navigator.ActiveGuidanceMode
import com.mapbox.navigator.ActiveGuidanceOptions

internal object ActiveGuidanceOptionsMapper {

    private const val GEOJSON = "geojson"

    fun mapFrom(directionsRoute: DirectionsRoute?): ActiveGuidanceOptions {
        val mode = mapToActiveGuidanceMode(directionsRoute?.routeOptions()?.profile())
        val geometry = mapToActiveGuidanceGeometry(directionsRoute?.routeOptions()?.geometries())
        return ActiveGuidanceOptions(mode, geometry)
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
}
