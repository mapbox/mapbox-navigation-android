package com.mapbox.navigation.route.offboard

import android.content.Context
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.navigation.base.internal.accounts.UrlSkuTokenProvider
import com.mapbox.navigation.base.internal.extensions.LocaleEx.getUnitTypeForLocale
import com.mapbox.navigation.base.internal.extensions.inferDeviceLocale

internal object RouteBuilderProvider {

    fun getBuilder(
        accessToken: String,
        context: Context,
        urlSkuTokenProvider: UrlSkuTokenProvider
    ): MapboxDirections.Builder =
        MapboxDirections.builder()
            .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
            .language(context.inferDeviceLocale())
            .continueStraight(true)
            .roundaboutExits(true)
            .geometries(DirectionsCriteria.GEOMETRY_POLYLINE6)
            .overview(DirectionsCriteria.OVERVIEW_FULL)
            .steps(true)
            .annotations(
                listOf(
                    DirectionsCriteria.ANNOTATION_CONGESTION,
                    DirectionsCriteria.ANNOTATION_DISTANCE
                )
            )
            .accessToken(accessToken)
            .voiceInstructions(true)
            .bannerInstructions(true)
            .voiceUnits(context.inferDeviceLocale().getUnitTypeForLocale())
            .interceptor {
                val httpUrl = it.request().url()
                val skuUrl =
                    urlSkuTokenProvider.obtainUrlWithSkuToken(httpUrl.toString(), httpUrl.querySize())
                it.proceed(it.request().newBuilder().url(skuUrl).build())
            }
}
