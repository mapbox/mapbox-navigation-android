package com.mapbox.services.android.navigation.v5.navigation

import com.mapbox.api.directions.v5.models.DirectionsRoute
import java.util.ArrayList

class RouteAnnotationUpdater {

    fun update(
        oldRoute: DirectionsRoute,
        annotationHolder: DirectionsRoute?,
        currentLegIndex: Int
    ): DirectionsRoute {
        oldRoute.legs()?.let { routeLegList ->
            val legs = ArrayList(routeLegList)
            for (i in currentLegIndex until legs.size) {
                annotationHolder?.legs().let { routeLegs ->
                    routeLegs?.let { routeLegs ->
                        val updatedAnnotation = routeLegs[i - currentLegIndex].annotation()
                        legs[i] = legs[i].toBuilder().annotation(updatedAnnotation).build()
                    }
                }
            }
            return oldRoute.toBuilder()
                    .legs(legs)
                    .build()
        }
        return oldRoute
    }
}
