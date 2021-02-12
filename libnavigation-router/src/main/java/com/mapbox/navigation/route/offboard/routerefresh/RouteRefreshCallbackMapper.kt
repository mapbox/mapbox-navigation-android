package com.mapbox.navigation.route.offboard.routerefresh

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.LegAnnotation
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.api.directionsrefresh.v1.models.DirectionsRefreshResponse
import com.mapbox.api.directionsrefresh.v1.models.DirectionsRouteRefresh
import com.mapbox.navigation.base.route.RouteRefreshCallback
import com.mapbox.navigation.base.route.RouteRefreshError
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

internal class RouteRefreshCallbackMapper(
    private val originalRoute: DirectionsRoute,
    private val callback: RouteRefreshCallback
) : Callback<DirectionsRefreshResponse> {

    override fun onResponse(
        call: Call<DirectionsRefreshResponse>,
        response: Response<DirectionsRefreshResponse>
    ) {
        val routeAnnotations = response.body()?.route()
        var errorThrowable: Throwable? = null
        val refreshedDirectionsRoute = try {
            mapToDirectionsRoute(routeAnnotations)
        } catch (t: Throwable) {
            errorThrowable = t
            null
        }
        if (refreshedDirectionsRoute != null) {
            callback.onRefresh(refreshedDirectionsRoute)
        } else {
            callback.onError(
                RouteRefreshError(
                    message = "Failed to read refresh response",
                    throwable = errorThrowable
                )
            )
        }
    }

    override fun onFailure(call: Call<DirectionsRefreshResponse>, t: Throwable) {
        callback.onError(RouteRefreshError(throwable = t))
    }

    private fun mapToDirectionsRoute(routeAnnotations: DirectionsRouteRefresh?): DirectionsRoute? {
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
