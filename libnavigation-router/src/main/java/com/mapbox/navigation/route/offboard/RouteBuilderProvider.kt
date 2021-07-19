/**
 * Tampering with any file that contains billing code is a violation of Mapbox Terms of Service and will result in enforcement of the penalties stipulated in the ToS.
 */

package com.mapbox.navigation.route.offboard

import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directionsrefresh.v1.MapboxDirectionsRefresh
import com.mapbox.navigation.base.internal.accounts.UrlSkuTokenProvider

internal object RouteBuilderProvider {

    fun getBuilder(
        urlSkuTokenProvider: UrlSkuTokenProvider?
    ): MapboxDirections.Builder =
        MapboxDirections.builder()
            .also { builder ->
                if (urlSkuTokenProvider != null) {
                    builder.interceptor {
                        val httpUrl = it.request().url
                        val skuUrl = urlSkuTokenProvider.obtainUrlWithSkuToken(httpUrl.toUrl())
                        it.proceed(it.request().newBuilder().url(skuUrl).build())
                    }
                }
            }

    fun getRefreshBuilder(): MapboxDirectionsRefresh.Builder =
        MapboxDirectionsRefresh.builder()
}
