package com.mapbox.navigation.route.offboard

import android.content.Context
import com.mapbox.annotation.navigation.module.MapboxNavigationModule
import com.mapbox.annotation.navigation.module.MapboxNavigationModuleType
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.route.offboard.router.addInterceptor
import com.mapbox.navigation.route.offboard.router.routeOptions
import com.mapbox.navigation.sku.accounts.SkuInterceptor
import com.mapbox.navigation.utils.exceptions.NavigationException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@MapboxNavigationModule(MapboxNavigationModuleType.OffboardRouter, skipConfiguration = true)
class MapboxOffboardRouter(private val accessToken: String, private val context: Context) : Router {

    companion object {
        const val ERROR_FETCHING_ROUTE = "Error fetching route"
    }

    private var mapboxDirections: MapboxDirections? = null

    override fun getRoute(
        routeOptions: RouteOptions,
        callback: Router.Callback
    ) {
        mapboxDirections = RouteBuilderProvider.getBuilder(accessToken, context)
            .routeOptions(routeOptions)
            .addInterceptor(SkuInterceptor(context))
            .build()
        mapboxDirections?.enqueueCall(object : Callback<DirectionsResponse> {

            override fun onResponse(
                call: Call<DirectionsResponse>,
                response: Response<DirectionsResponse>
            ) {
                val routes = response.body()?.routes()
                if (response.isSuccessful && !routes.isNullOrEmpty()) {
                    callback.onResponse(routes)
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
        mapboxDirections?.cancelCall()
        mapboxDirections = null
    }
}
