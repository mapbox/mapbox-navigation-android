@file:JvmName("RouteOptionsExtensions")

package com.mapbox.navigation.base.extensions

import android.content.Context
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.core.constants.Constants
import com.mapbox.navigation.base.internal.extensions.LocaleEx.getUnitTypeForLocale
import com.mapbox.navigation.base.internal.extensions.inferDeviceLocale
import java.util.Locale

/**
 * Indicates whether the route options supports route refresh.
 *
 * To qualify for the route refresh feature, the [RouteOptions] need to include:
 * - [DirectionsCriteria.PROFILE_DRIVING_TRAFFIC]
 * - [DirectionsCriteria.OVERVIEW_FULL]
 * and one of:
 * - [DirectionsCriteria.ANNOTATION_CONGESTION]
 * - [DirectionsCriteria.ANNOTATION_MAXSPEED]
 * - [DirectionsCriteria.ANNOTATION_SPEED]
 * - [DirectionsCriteria.ANNOTATION_DURATION]
 * - [DirectionsCriteria.ANNOTATION_DISTANCE]
 * - [DirectionsCriteria.ANNOTATION_CLOSURE]
 *
 * @receiver RouteOptions
 * @return Boolean
 */
fun RouteOptions?.supportsRouteRefresh(): Boolean {
    if (this == null) {
        return false
    }
    val isTrafficProfile = profile() == DirectionsCriteria.PROFILE_DRIVING_TRAFFIC
    val isOverviewFull = overview() == DirectionsCriteria.OVERVIEW_FULL
    val hasCongestionOrMaxSpeed = annotationsList()?.any {
        it == DirectionsCriteria.ANNOTATION_CONGESTION ||
            it == DirectionsCriteria.ANNOTATION_MAXSPEED ||
            it == DirectionsCriteria.ANNOTATION_SPEED ||
            it == DirectionsCriteria.ANNOTATION_DURATION ||
            it == DirectionsCriteria.ANNOTATION_DISTANCE ||
            it == DirectionsCriteria.ANNOTATION_CLOSURE
    } ?: false
    return isTrafficProfile && isOverviewFull && hasCongestionOrMaxSpeed
}

/**
 * Applies the [RouteOptions] that are required for the route request to execute
 * or otherwise recommended for the Navigation SDK and all of its features to provide the best car navigation experience.
 */
fun RouteOptions.Builder.applyDefaultNavigationOptions(): RouteOptions.Builder = apply {
    baseUrl(Constants.BASE_API_URL)
    user(Constants.MAPBOX_USER)
    profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
    geometries(DirectionsCriteria.GEOMETRY_POLYLINE6)
    overview(DirectionsCriteria.OVERVIEW_FULL)
    steps(true)
    continueStraight(true)
    roundaboutExits(true)
    annotationsList(
        listOf(
            DirectionsCriteria.ANNOTATION_CONGESTION,
            DirectionsCriteria.ANNOTATION_MAXSPEED,
            DirectionsCriteria.ANNOTATION_SPEED,
            DirectionsCriteria.ANNOTATION_DURATION,
            DirectionsCriteria.ANNOTATION_DISTANCE,
            DirectionsCriteria.ANNOTATION_CLOSURE
        )
    )
    voiceInstructions(true)
    bannerInstructions(true)
    requestUuid("")
}

/**
 * Applies the [RouteOptions] that adapt the returned instructions' language and voice unit based on the device's [Locale].
 */
fun RouteOptions.Builder.applyLanguageAndVoiceUnitOptions(context: Context): RouteOptions.Builder =
    apply {
        language(context.inferDeviceLocale().language)
        voiceUnits(context.inferDeviceLocale().getUnitTypeForLocale().value)
    }
