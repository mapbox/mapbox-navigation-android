package com.mapbox.services.android.navigation.v5.navigation

import com.mapbox.api.directions.v5.models.DirectionsRoute
import java.util.ArrayList

class RouteAnnotationUpdater {

    fun update(
        oldRoute: DirectionsRoute,
        annotationHolder: DirectionsRoute,
        currentLegIndex: Int
    ): DirectionsRoute {

        oldRoute.legs()?.let { oldRouteLegsList ->
            val legs = ArrayList(oldRouteLegsList)
            for (i in currentLegIndex until legs.size) {
                annotationHolder.legs()?.let { annotationHolderRouteLegsList ->
                    val updatedAnnotation = annotationHolderRouteLegsList[i - currentLegIndex].annotation()
                    legs[i] = legs[i].toBuilder().annotation(updatedAnnotation).build()
                }
            }
            return oldRoute.toBuilder()
                    .legs(legs)
                    .build()
        }
        return oldRoute
    }
}
