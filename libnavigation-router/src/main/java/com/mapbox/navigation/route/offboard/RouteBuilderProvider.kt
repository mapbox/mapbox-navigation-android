package com.mapbox.navigation.route.offboard

import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directionsrefresh.v1.MapboxDirectionsRefresh
import com.mapbox.navigation.base.internal.accounts.UrlSkuTokenProvider
import okhttp3.Request

internal object RouteBuilderProvider {

    fun getBuilder(
        urlSkuTokenProvider: UrlSkuTokenProvider?
    ): MapboxDirections.Builder =
        MapboxDirections.builder()
            .also { builder ->
                if (urlSkuTokenProvider != null) {
                    builder.interceptor {
                        val httpUrl = (it.request() as Request).url
                        val skuUrl = urlSkuTokenProvider.obtainUrlWithSkuToken(httpUrl.toUrl())
                        it.proceed(it.request().newBuilder().url(skuUrl).build())
                    }
                }
            }

    fun getRefreshBuilder(): MapboxDirectionsRefresh.Builder =
        MapboxDirectionsRefresh.builder()
}
