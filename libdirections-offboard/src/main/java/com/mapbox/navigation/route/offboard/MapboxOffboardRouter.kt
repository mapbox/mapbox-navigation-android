package com.mapbox.navigation.route.offboard

import android.content.Context
import com.mapbox.annotation.navigation.module.MapboxNavigationModule
import com.mapbox.annotation.navigation.module.MapboxNavigationModuleType
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.base.route.model.RouteOptionsNavigation
import com.mapbox.navigation.route.offboard.extension.mapToRoute
import com.mapbox.navigation.route.offboard.router.NavigationRoute
import com.mapbox.navigation.utils.exceptions.NavigationException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@MapboxNavigationModule(MapboxNavigationModuleType.OffboardRouter, skipConfiguration = true)
class MapboxOffboardRouter : Router {

    companion object {
        const val ERROR_FETCHING_ROUTE = "Error fetching route"
    }

    private val context: Context
    private val mapboxToken: String
    private val routeBuilderProvider: NavigationRoute.Builder
    private var navigationRoute: NavigationRoute? = null

    constructor(
        context: Context,
        mapboxToken: String
    ) : this(
        context,
        mapboxToken,
        NavigationRoute.builder(context)
    )

    internal constructor(
        context: Context,
        mapboxToken: String,
        routeBuilderProvider: NavigationRoute.Builder
    ) {
        this.context = context
        this.mapboxToken = mapboxToken
        this.routeBuilderProvider = routeBuilderProvider
    }

    override fun getRoute(
        routeOptions: RouteOptionsNavigation,
        callback: Router.Callback
    ) {
        navigationRoute = routeBuilderProvider.routeOptions(routeOptions).build()
        navigationRoute?.getRoute(object : Callback<DirectionsResponse> {

            override fun onResponse(
                call: Call<DirectionsResponse>,
                response: Response<DirectionsResponse>
            ) {
                val routes = response.body()?.routes()
                if (response.isSuccessful && !routes.isNullOrEmpty()) {
                    callback.onResponse(routes.map { it.mapToRoute() })
                } else {
                    callback.onFailure(NavigationException(ERROR_FETCHING_ROUTE))
                }
            }

            override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                callback.onFailure(t)
            }
        })
    }

    override fun cancel() {
        navigationRoute?.cancelCall()
        navigationRoute = null
    }
}
