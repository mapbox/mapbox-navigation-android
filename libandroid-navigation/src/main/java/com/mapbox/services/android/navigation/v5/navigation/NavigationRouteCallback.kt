package com.mapbox.services.android.navigation.v5.navigation

import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.navigation.utils.time.ElapsedTime
import com.mapbox.services.android.navigation.v5.internal.navigation.NavigationTelemetry
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NavigationRouteCallback(
    private val listener: NavigationRouteEventListener,
    private val callback: Callback<DirectionsResponse>
) : Callback<DirectionsResponse> {

    override fun onResponse(
        call: Call<DirectionsResponse>,
        response: Response<DirectionsResponse>
    ) {
        callback.onResponse(call, response)
        if (isValid(response)) {
            response.body()?.uuid()?.let { uuid ->
                sendEventWith(listener.time, uuid)
            }
        }
    }

    override fun onFailure(call: Call<DirectionsResponse>, throwable: Throwable) {
        callback.onFailure(call, throwable)
    }

    private fun isValid(response: Response<DirectionsResponse>): Boolean {
        return response.body()?.routes()?.isNotEmpty() ?: false
    }

    private fun sendEventWith(time: ElapsedTime, uuid: String) {
        NavigationTelemetry.routeRetrievalEvent(time, uuid)
    }
}
