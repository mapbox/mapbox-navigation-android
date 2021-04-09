package com.mapbox.navigation.ui.camera

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.utils.internal.ifNonNull
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMisc

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

    /**
     * If the [RouteInformation] does not include a [RouteProgress] instance the points
     * for the entire route will be returned. If a [RouteProgress] is present, the distance
     * traveled will be used to truncate the route points such that the final collection of points
     * returned will contain the point nearest the distance traveled until the end of the route.
     */
    override fun overview(routeInformation: RouteInformation): List<Point> {
        return getRoute(routeInformation)?.run {
            when (routeInformation.routeProgress) {
                null -> generateRouteCoordinates(this)
                else -> getCoordinates(this, routeInformation.routeProgress)
            }
        } ?: emptyList()
    }

    private fun getRoute(routeInformation: RouteInformation): DirectionsRoute? =
        when (routeInformation.route) {
            null -> routeInformation.routeProgress?.route
            else -> routeInformation.route
        }

    private fun getCoordinates(
        route: DirectionsRoute,
        routeProgress: RouteProgress?
    ): List<Point> {
        return ifNonNull(
            routeProgress
        ) { theRouteProgress ->
            getLineString(route)?.run {
                TurfMisc.lineSliceAlong(
                    this,
                    theRouteProgress.distanceTraveled.toDouble(),
                    route.distance(),
                    TurfConstants.UNIT_METERS
                ).coordinates()
            } ?: emptyList()
        } ?: emptyList()
    }

    private fun generateRouteCoordinates(route: DirectionsRoute?): List<Point> {
        return route?.run {
            getLineString(this)?.coordinates()
        } ?: emptyList()
    }

    private fun getLineString(route: DirectionsRoute): LineString? {
        return ifNonNull(route.geometry()) { routeGeometry ->
            val precision =
                if (route.routeOptions()?.geometries() == DirectionsCriteria.GEOMETRY_POLYLINE) {
                    Constants.PRECISION_5
                } else {
                    Constants.PRECISION_6
                }
            LineString.fromPolyline(routeGeometry, precision)
        }
    }
}
