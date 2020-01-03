package com.mapbox.navigation.route.offboard

import android.content.Context
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.model.RouteOptionsNavigation
import com.mapbox.navigation.route.offboard.router.NavigationOffboardRoute

internal object RouteBuilderProvider {
    fun getBuilder(accessToken: String, context: Context): NavigationOffboardRoute.Builder =
        NavigationOffboardRoute.Builder()
            .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
            .language(context)
            .continueStraight(true)
            .roundaboutExits(true)
            .geometries(DirectionsCriteria.GEOMETRY_POLYLINE6)
            .overview(DirectionsCriteria.OVERVIEW_FULL)
            .steps(true)
            .annotations(
                DirectionsCriteria.ANNOTATION_CONGESTION,
                DirectionsCriteria.ANNOTATION_DISTANCE
            )
            .routeOptions(RouteOptionsNavigation.builder()
                .origin(Point.fromLngLat(.0, .0))
                .destination(Point.fromLngLat(.0, .0))
                .accessToken(accessToken)
                .build()
            )
            .voiceInstructions(true)
            .bannerInstructions(true)
            .enableRefresh(false)
            .voiceUnits(context)
}
