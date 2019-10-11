package com.mapbox.services.android.navigation.v5.navigation

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.services.android.navigation.v5.utils.extensions.ifNonNull
import java.util.ArrayList

class RouteAnnotationUpdater {

    fun update(
        oldRoute: DirectionsRoute,
        annotationHolder: DirectionsRoute,
        currentLegIndex: Int
    ): DirectionsRoute {
        ifNonNull(oldRoute.legs(),
                annotationHolder.legs()
        ) { oldRouteLegsList, annotationHolderRouteLegsList ->
            val legs = ArrayList(oldRouteLegsList)
            for (i in currentLegIndex until legs.size) {
                val updatedAnnotation = annotationHolderRouteLegsList[i - currentLegIndex].annotation()
                legs[i] = legs[i].toBuilder().annotation(updatedAnnotation).build()
            }
            return@ifNonNull oldRoute.toBuilder()
                    .legs(legs)
                    .build()
        }
        return oldRoute
    }
}
