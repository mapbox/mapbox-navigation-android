package com.mapbox.services.android.navigation.v5.navigation.camera

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.services.android.navigation.v5.utils.extensions.ifNonNull
import java.util.ArrayList

/**
 * The default camera used by [com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation].
 */
open class SimpleCamera : Camera() {

    companion object {
        protected const val DEFAULT_TILT = 50
        protected const val DEFAULT_ZOOM = 15.0
    }

    private var routeCoordinates: List<Point> = ArrayList()
    private var initialRoute: DirectionsRoute? = null

    override fun tilt(routeInformation: RouteInformation): Double {
        return DEFAULT_TILT.toDouble()
    }

    override fun zoom(routeInformation: RouteInformation): Double {
        return DEFAULT_ZOOM
    }

    override fun overview(routeInformation: RouteInformation): List<Point> {
        if (routeCoordinates.isEmpty()) {
            buildRouteCoordinatesFromRouteData(routeInformation)
        }
        return routeCoordinates
    }

    private fun buildRouteCoordinatesFromRouteData(routeInformation: RouteInformation) {
        ifNonNull(routeInformation.route) { route ->
            setupLineStringAndBearing(route)
        } ?: ifNonNull(routeInformation.routeProgress?.directionsRoute()) { directionsRoute ->
            setupLineStringAndBearing(directionsRoute)
        }
    }

    private fun setupLineStringAndBearing(route: DirectionsRoute) {
        if (route == initialRoute) {
            return // no need to recalculate these values
        }
        initialRoute = route
        routeCoordinates = generateRouteCoordinates(route)
    }

    private fun generateRouteCoordinates(route: DirectionsRoute?): List<Point> =
        route?.geometry()?.let { geometry ->
            val lineString = LineString.fromPolyline(geometry, Constants.PRECISION_6)
            lineString.coordinates()
        } ?: emptyList()
}
