package com.mapbox.navigation.route.offboard

import android.content.Context
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.base.route.model.Route
import com.mapbox.navigation.base.route.model.mapToRoute
import com.mapbox.navigation.exception.NavigationException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MapboxOffboardRouter(
    private val context: Context,
    private val mapboxToken: String
) : Router {

    private var navigationRoute: NavigationRoute? = null

    override fun getRoute(
        origin: Point,
        waypoints: List<Point>,
        callback: (route: Route) -> Unit
    ) {
    }

    override fun getRoute(
        origin: Point,
        waypoints: List<Point>?,
        destination: Point,
        listener: Router.RouteListener
    ) {
        val builder = NavigationRoute.builder(context).accessToken(mapboxToken).origin(origin)
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
                if (response.isSuccessful) {
                    response.body()?.routes()?.firstOrNull()?.let { listener.onRouteReady(it.mapToRoute()) }
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
