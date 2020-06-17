package com.mapbox.navigation.route.offboard.routerefresh

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directionsrefresh.v1.models.DirectionsRefreshResponse
import com.mapbox.navigation.base.route.RouteRefreshCallback
import com.mapbox.navigation.base.route.RouteRefreshError
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

internal class RouteRefreshCallbackMapper(
    private val originalRoute: DirectionsRoute,
    private val currentLegIndex: Int,
    private val callback: RouteRefreshCallback
) : Callback<DirectionsRefreshResponse> {

    override fun onResponse(call: Call<DirectionsRefreshResponse>, response: Response<DirectionsRefreshResponse>) {
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

    private fun mapToDirectionsRoute(routeAnnotations: DirectionsRoute?): DirectionsRoute? {
        val validRouteAnnotations = routeAnnotations ?: return null
        val refreshedRouteLegs = originalRoute.legs()?.let { oldRouteLegsList ->
            val legs = oldRouteLegsList.toMutableList()
            for (i in currentLegIndex until legs.size) {
                validRouteAnnotations.legs()?.let { annotationHolderRouteLegsList ->
                    val updatedAnnotation = annotationHolderRouteLegsList[i - currentLegIndex].annotation()
                    legs[i] = legs[i].toBuilder().annotation(updatedAnnotation).build()
                }
            }
            legs.toList()
        }
        return originalRoute.toBuilder().legs(refreshedRouteLegs).build()
    }
}
