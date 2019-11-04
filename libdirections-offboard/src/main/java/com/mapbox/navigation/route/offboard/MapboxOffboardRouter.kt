package com.mapbox.navigation.route.offboard

import com.mapbox.annotation.navigation.module.MapboxNavigationModule
import com.mapbox.annotation.navigation.module.MapboxNavigationModuleType
import android.content.Context
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.route.common.NavigationRoute
import com.mapbox.navigation.route.common.extension.mapToRoute
import com.mapbox.navigation.utils.exceptions.NavigationException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@MapboxNavigationModule(MapboxNavigationModuleType.OffboardRouter, skipConfiguration = true)
class MapboxOffboardRouter : Router
    private val context: Context,
    private val mapboxToken: String
) : Router {

    override fun getRoute(
        origin: Point,
        waypoints: List<Point>?,
        destination: Point,
        listener: Router.RouteListener
    ) {
        val builder = NavigationRoute
            .builder(context)
            .accessToken(mapboxToken)
            .origin(origin)
            .destination(destination)
        waypoints?.forEach { builder.addWaypoint(it) }
        navigationRoute = builder.build()
        navigationRoute?.getRoute(object : Callback<DirectionsResponse> {
            override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                listener.onFailure(t)
            }

            override fun onResponse(
                call: Call<DirectionsResponse>,
                response: Response<DirectionsResponse>
            ) {
                val route = response.body()?.routes()?.firstOrNull()
                if (response.isSuccessful && route != null) {
                    listener.onRouteReady(route.mapToRoute())
                } else {
                    listener.onFailure(NavigationException("Error fetching route"))
                }
            }
        })
    }

    override fun cancel() {
        navigationRoute?.cancelCall()
        navigationRoute = null
    }
}
