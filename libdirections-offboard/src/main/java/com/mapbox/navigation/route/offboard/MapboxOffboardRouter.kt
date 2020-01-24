package com.mapbox.navigation.route.offboard

import android.content.Context
import com.mapbox.annotation.navigation.module.MapboxNavigationModule
import com.mapbox.annotation.navigation.module.MapboxNavigationModuleType
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.accounts.SkuTokenProvider
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.route.offboard.router.routeOptions
import com.mapbox.navigation.utils.exceptions.NavigationException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * MapboxOffboardRouter provides online route-fetching
 *
 * @param accessToken mapboxAccessToken token
 * @param context application Context
 */
@MapboxNavigationModule(MapboxNavigationModuleType.OffboardRouter, skipConfiguration = true)
class MapboxOffboardRouter(
    private val accessToken: String,
    private val context: Context,
    private val skuTokenProvider: SkuTokenProvider
) : Router {

    companion object {
        const val ERROR_FETCHING_ROUTE = "Error fetching route"
    }

    private var mapboxDirections: MapboxDirections? = null

    override fun getRoute(
        routeOptions: RouteOptions,
        callback: Router.Callback
    ) {
        mapboxDirections = RouteBuilderProvider.getBuilder(accessToken, context, skuTokenProvider)
            .routeOptions(routeOptions)
            .build()
        mapboxDirections?.enqueueCall(object : Callback<DirectionsResponse> {

            override fun onResponse(
                call: Call<DirectionsResponse>,
                response: Response<DirectionsResponse>
            ) {
                val routes = response.body()?.routes()
                when {
                    call.isCanceled -> callback.onCanceled()
                    response.isSuccessful && !routes.isNullOrEmpty() -> callback.onResponse(routes)
                    else -> callback.onFailure(NavigationException(ERROR_FETCHING_ROUTE))
                }
            }

            override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                if (call.isCanceled) {
                    callback.onCanceled()
                } else {
                    callback.onFailure(t)
                }
            }
        })
    }

    override fun cancel() {
        mapboxDirections?.cancelCall()
        mapboxDirections = null
    }
}
