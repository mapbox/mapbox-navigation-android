package com.mapbox.navigation.dropin.util

import com.mapbox.api.geocoding.v5.GeocodingCriteria
import com.mapbox.api.geocoding.v5.GeocodingCriteria.GeocodingTypeCriteria
import com.mapbox.api.geocoding.v5.MapboxGeocoding
import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.api.geocoding.v5.models.GeocodingResponse
import com.mapbox.geojson.Point
import kotlinx.coroutines.suspendCancellableCoroutine
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Wrapper class for [MapboxGeocoding].
 */
internal class Geocoder(
    private val accessToken: String
) {
    suspend fun findAddresses(point: Point): List<CarmenFeature> =
        reverseGeocode(point, GeocodingCriteria.TYPE_ADDRESS)?.features() ?: emptyList()

    suspend fun reverseGeocode(
        point: Point,
        @GeocodingTypeCriteria criteria: String
    ): GeocodingResponse? =
        suspendCancellableCoroutine { continuation ->
            val reverseGeocode = MapboxGeocoding.builder()
                .accessToken(accessToken)
                .query(point)
                .geocodingTypes(criteria)
                .build()

            reverseGeocode.enqueueCall(object : Callback<GeocodingResponse> {
                override fun onResponse(
                    call: Call<GeocodingResponse>,
                    response: Response<GeocodingResponse>
                ) {
                    continuation.resume(response.body())
                }

                override fun onFailure(call: Call<GeocodingResponse>, throwable: Throwable) {
                    continuation.resumeWithException(throwable)
                }
            })

            continuation.invokeOnCancellation { reverseGeocode.cancelCall() }
        }

    companion object {
        fun create(accessToken: String) = Geocoder(accessToken)
    }
}
