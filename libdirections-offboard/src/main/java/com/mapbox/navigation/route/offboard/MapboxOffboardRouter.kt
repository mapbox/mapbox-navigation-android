package com.mapbox.navigation.route.offboard

import android.content.Context
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.base.route.model.PointNavigation
import com.mapbox.navigation.base.route.model.Route
import com.mapbox.navigation.exception.NavigationException
import com.mapbox.navigation.route.common.NavigationRoute
import com.mapbox.navigation.route.common.extension.mapToRoute
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MapboxOffboardRouter(
    private val context: Context,
    private val mapboxToken: String
) : Router {

    private var navigationRoute: NavigationRoute? = null

    override fun getRoute(
        origin: PointNavigation,
        waypoints: List<PointNavigation>,
        callback: (route: Route) -> Unit
    ) {
    }

    override fun getRoute(
        origin: PointNavigation,
        waypoints: List<PointNavigation>?,
        destination: PointNavigation,
        listener: Router.RouteListener
    ) {
        val builder = NavigationRoute.builder(context).accessToken(mapboxToken)
            .origin(Point.fromLngLat(origin.longitude, origin.latitude))
            .destination(Point.fromLngLat(destination.longitude, destination.latitude))
        waypoints?.forEach { builder.addWaypoint(Point.fromLngLat(it.longitude, it.latitude)) }
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
