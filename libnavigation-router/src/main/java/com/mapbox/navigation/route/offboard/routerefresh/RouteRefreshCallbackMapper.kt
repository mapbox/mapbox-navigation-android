package com.mapbox.navigation.route.offboard.routerefresh

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.LegAnnotation
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.api.directionsrefresh.v1.models.DirectionsRouteRefresh

internal object RouteRefreshCallbackMapper {
    fun mapToDirectionsRoute(
        originalRoute: DirectionsRoute,
        routeAnnotations: DirectionsRouteRefresh?
    ): DirectionsRoute? {
        val validRouteAnnotations = routeAnnotations ?: return null
        val updatedLegs = mutableListOf<RouteLeg?>()
        originalRoute.legs()?.let { oldRouteLegsList ->
            oldRouteLegsList.forEachIndexed { index, routeLeg ->
                val newAnnotation = routeLeg.annotation()?.toBuilder()
                    ?.congestion(
                        validRouteAnnotations.annotationOfLeg(index)?.congestion()
                    )
                    ?.distance(
                        validRouteAnnotations.annotationOfLeg(index)?.distance()
                    )
                    ?.duration(
                        validRouteAnnotations.annotationOfLeg(index)?.duration()
                    )
                    ?.maxspeed(
                        validRouteAnnotations.annotationOfLeg(index)?.maxspeed()
                    )
                    ?.speed(
                        validRouteAnnotations.annotationOfLeg(index)?.speed()
                    )
                    ?.build()
                updatedLegs.add(routeLeg.toBuilder().annotation(newAnnotation).build())
            }
        }
        return originalRoute.toBuilder().legs(updatedLegs).build()
    }

    private fun DirectionsRouteRefresh.annotationOfLeg(index: Int): LegAnnotation? =
        this.legs()?.getOrNull(index)?.annotation()
}
