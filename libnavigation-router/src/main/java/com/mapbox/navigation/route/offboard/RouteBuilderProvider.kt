package com.mapbox.navigation.route.offboard

import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directionsrefresh.v1.MapboxDirectionsRefresh
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.navigation.base.internal.accounts.UrlSkuTokenProvider
import com.mapbox.navigation.utils.internal.LoggerProvider
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
                        LoggerProvider.logger.i(
                            Tag("BillingTest"),
                            Message(
                                "requesting route with: ${
                                    it.request().newBuilder().url(skuUrl).build().url
                                }"
                            )
                        )
                        it.proceed(it.request().newBuilder().url(skuUrl).build())
                    }
                }
            }

    fun getRefreshBuilder(): MapboxDirectionsRefresh.Builder =
        MapboxDirectionsRefresh.builder()
}
