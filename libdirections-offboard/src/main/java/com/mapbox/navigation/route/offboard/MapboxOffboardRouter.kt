package com.mapbox.navigation.route.offboard

import android.content.Context
import com.mapbox.annotation.navigation.module.MapboxNavigationModule
import com.mapbox.annotation.navigation.module.MapboxNavigationModuleType
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.route.offboard.extension.mapToRoute
import com.mapbox.navigation.route.offboard.router.NavigationRoute
import com.mapbox.navigation.utils.exceptions.NavigationException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@MapboxNavigationModule(MapboxNavigationModuleType.OffboardRouter, skipConfiguration = true)
class MapboxOffboardRouter(
    private val context: Context,
    private val mapboxToken: String
) : Router {

    private var navigationRoute: NavigationRoute? = null

    override fun getRoute(
        origin: Point,
        waypoints: List<Point>?,
        destination: Point,
        callback: Router.RouteCallback
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
                callback.onFailure(t)
            }

            override fun onResponse(
                call: Call<DirectionsResponse>,
                response: Response<DirectionsResponse>
            ) {
                val route = response.body()?.routes()
                if (response.isSuccessful && !route.isNullOrEmpty()) {
                    callback.onRouteReady(route.map { it.mapToRoute() })
                } else {
                    callback.onFailure(NavigationException("Error fetching route"))
                }
            }
        })
    }

    override fun cancel() {
        navigationRoute?.cancelCall()
        navigationRoute = null
    }
}
