package com.mapbox.services.android.navigation.v5.internal.navigation

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.services.android.navigation.v5.navigation.DirectionsRouteType

internal class RouteHandler(private val mapboxNavigator: MapboxNavigator) {

    companion object {
        private const val INDEX_FIRST_ROUTE = 0
        private const val INDEX_FIRST_LEG = 0
    }

    fun updateRoute(route: DirectionsRoute, routeType: DirectionsRouteType) {
        if (routeType == DirectionsRouteType.NEW_ROUTE) {
            val routeJson = route.toJson()
            // TODO route_index (Which route to follow) and leg_index (Which leg to follow) are hardcoded for now
            mapboxNavigator.setRoute(routeJson, INDEX_FIRST_ROUTE, INDEX_FIRST_LEG)
        } else {
            route.legs()?.let { routeLegs ->
                for (i in routeLegs.indices) {
                    routeLegs[i].annotation()?.toJson()?.let { annotationJson ->
                        mapboxNavigator.updateAnnotations(annotationJson, INDEX_FIRST_ROUTE, i)
                    }
                }
            }
        }
    }
}
