package com.mapbox.navigation.route.offboard

import android.content.Context
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.navigation.route.offboard.extension.getUnitTypeForLocale
import com.mapbox.navigation.utils.extensions.inferDeviceLocale

internal object RouteBuilderProvider {

    fun getBuilder(accessToken: String, context: Context): MapboxDirections.Builder =
        MapboxDirections.builder()
            .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
            .language(context.inferDeviceLocale())
            .continueStraight(true)
            .roundaboutExits(true)
            .geometries(DirectionsCriteria.GEOMETRY_POLYLINE6)
            .overview(DirectionsCriteria.OVERVIEW_FULL)
            .steps(true)
            .annotations(
                DirectionsCriteria.ANNOTATION_CONGESTION,
                DirectionsCriteria.ANNOTATION_DISTANCE
            )
            .accessToken(accessToken)
            .voiceInstructions(true)
            .bannerInstructions(true)
            .enableRefresh(false)
            .voiceUnits(context.inferDeviceLocale().getUnitTypeForLocale())
}
