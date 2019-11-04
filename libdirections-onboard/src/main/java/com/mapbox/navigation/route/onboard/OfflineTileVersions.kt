package com.mapbox.navigation.route.onboard

import com.mapbox.api.routetiles.v1.versions.MapboxRouteTileVersions
import com.mapbox.api.routetiles.v1.versions.models.RouteTileVersionsResponse
import com.mapbox.navigation.route.onboard.model.OfflineError
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * This is a wrapper class for the [MapboxRouteTileVersions] class. It returns a list of
 * all available versions of Routing Tiles available via [OfflineTiles]. This class
 * encapsulates the unwrapping of the list from the response.
 */
internal class OfflineTileVersions {

    /**
     * Call to receive all the available versions of Offline Tiles available.
     *
     * @param accessToken for the API call
     * @param callback to be updated with the versions
     */
    fun fetchRouteTileVersions(accessToken: String, callback: OnTileVersionsFoundCallback) {
        val mapboxRouteTileVersions = buildTileVersionsWith(accessToken)
        mapboxRouteTileVersions.enqueueCall(object : Callback<RouteTileVersionsResponse> {
            override fun onResponse(
                call: Call<RouteTileVersionsResponse>,
                response: Response<RouteTileVersionsResponse>
            ) {
                val responseBody = response.body()
                if (response.isSuccessful && responseBody != null) {
                    callback.onVersionsFound(responseBody.availableVersions())
                } else {
                    callback.onError(OfflineError("Tile version response was unsuccessful"))
                }
            }

            override fun onFailure(call: Call<RouteTileVersionsResponse>, throwable: Throwable) {
                val error = OfflineError(throwable.message ?: "Offline Tile Version error")
                callback.onError(error)
            }
        })
    }

    private fun buildTileVersionsWith(accessToken: String): MapboxRouteTileVersions =
        MapboxRouteTileVersions.builder().accessToken(accessToken).build()
}
