package com.mapbox.navigation.ui.androidauto.deeplink

import com.mapbox.api.geocoding.v5.GeocodingCriteria
import com.mapbox.api.geocoding.v5.MapboxGeocoding
import com.mapbox.api.geocoding.v5.models.GeocodingResponse
import com.mapbox.geojson.Point
import com.mapbox.navigation.core.geodeeplink.GeoDeeplink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class GeoDeeplinkGeocoding(
    private val accessToken: String,
) {
    var currentMapboxGeocoding: MapboxGeocoding? = null

    suspend fun requestPlaces(
        geoDeeplink: GeoDeeplink,
        origin: Point,
    ): GeocodingResponse? {
        currentMapboxGeocoding?.cancelCall()
        val point = geoDeeplink.point
        val placeQuery = geoDeeplink.placeQuery
        currentMapboxGeocoding = when {
            point != null -> {
                MapboxGeocoding.builder()
                    .accessToken(accessToken)
                    .query(point)
                    .proximity(origin)
                    .geocodingTypes(GeocodingCriteria.TYPE_ADDRESS)
                    .build()
            }
            placeQuery != null -> {
                MapboxGeocoding.builder()
                    .accessToken(accessToken)
                    .query(placeQuery)
                    .proximity(origin)
                    .build()
            }
            else -> {
                error("GeoDeepLink must have a point or query")
            }
        }
        return withContext(Dispatchers.IO) {
            currentMapboxGeocoding?.asFlow()?.first()
        }
    }

    fun cancel() {
        currentMapboxGeocoding?.cancelCall()
    }

    private fun MapboxGeocoding.asFlow(): Flow<GeocodingResponse?> = callbackFlow {
        enqueueCall(
            object : Callback<GeocodingResponse> {
                override fun onResponse(
                    call: Call<GeocodingResponse>,
                    response: Response<GeocodingResponse>,
                ) {
                    trySend(response.body())
                    close()
                }

                override fun onFailure(call: Call<GeocodingResponse>, t: Throwable) {
                    trySend(null)
                    close()
                }
            },
        )
        awaitClose {
            cancelCall()
        }
    }
}
