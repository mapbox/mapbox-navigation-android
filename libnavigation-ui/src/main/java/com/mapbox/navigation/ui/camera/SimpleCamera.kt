package com.mapbox.navigation.ui.camera

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point

/**
 * The default camera used by [MapboxNavigation].
 */
open class SimpleCamera : Camera() {

    companion object {
        /**
         * Default tilt angle for the Camera
         */
        protected const val DEFAULT_TILT = 50

        /**
         * Default zoom level for the Camera
         */
        protected const val DEFAULT_ZOOM = 15.0
    }

    override fun tilt(routeInformation: RouteInformation): Double {
        return DEFAULT_TILT.toDouble()
    }

    override fun zoom(routeInformation: RouteInformation): Double {
        return DEFAULT_ZOOM
    }

    override fun overview(routeInformation: RouteInformation): List<Point> =
        getRoute(routeInformation)?.run {
            generateRouteCoordinates(this)
        } ?: emptyList()

    private fun getRoute(routeInformation: RouteInformation): DirectionsRoute? =
        when (routeInformation.route) {
            null -> routeInformation.routeProgress?.route
            else -> routeInformation.route
        }

    private fun generateRouteCoordinates(route: DirectionsRoute?): List<Point> =
        route?.geometry()?.let { geometry ->
            val lineString = LineString.fromPolyline(geometry, Constants.PRECISION_6)
            lineString.coordinates()
        } ?: emptyList()
}
