package com.mapbox.services.android.navigation.v5.navigation

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directionsrefresh.v1.models.DirectionsRefreshResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

internal class RouteRefreshCallback(
    private val routeAnnotationUpdater: RouteAnnotationUpdater,
    private val directionsRoute: DirectionsRoute,
    private val legIndex: Int,
    private val refreshCallback: RefreshCallback
) : Callback<DirectionsRefreshResponse> {

    constructor(directionsRoute: DirectionsRoute, legIndex: Int, refreshCallback: RefreshCallback) : this(RouteAnnotationUpdater(), directionsRoute, legIndex, refreshCallback) {}

    override fun onResponse(call: Call<DirectionsRefreshResponse>, response: Response<DirectionsRefreshResponse>) {
        response.body()?.let { directionsRefreshResponse ->
            directionsRefreshResponse.route()?.let { responseDirectionsRoute ->
                responseDirectionsRoute.legs()?.let {
                    refreshCallback.onRefresh(routeAnnotationUpdater.update(directionsRoute, responseDirectionsRoute, legIndex))
                } ?: refreshCallback.onError(RefreshError(response.message()))
            } ?: refreshCallback.onError(RefreshError(response.message()))
        } ?: refreshCallback.onError(RefreshError(response.message()))
    }

    override fun onFailure(call: Call<DirectionsRefreshResponse>, throwable: Throwable) {
        refreshCallback.onError(RefreshError(throwable.message))
    }
}
