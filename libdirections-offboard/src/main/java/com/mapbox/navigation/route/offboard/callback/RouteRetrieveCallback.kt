package com.mapbox.navigation.route.offboard.callback

import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.route.offboard.extension.mapToRoute
import com.mapbox.navigation.utils.exceptions.NavigationException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

internal class RouteRetrieveCallback(
    private val callback: Router.Callback
) : Callback<DirectionsResponse> {

    companion object {
        const val ERROR_FETCHING_ROUTE = "Error fetching route"
    }

    override fun onResponse(
        call: Call<DirectionsResponse>,
        response: Response<DirectionsResponse>
    ) {
        val routes = response.body()?.routes()
        if (response.isSuccessful && !routes.isNullOrEmpty()) {
            callback.onRouteReady(routes.map { it.mapToRoute() })
        } else {
            callback.onFailure(NavigationException(ERROR_FETCHING_ROUTE))
        }
    }

    override fun onFailure(call: Call<DirectionsResponse>, throwable: Throwable) {
        callback.onFailure(throwable)
    }
}
