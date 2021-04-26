@file:JvmName("RouteOptionsEx")

package com.mapbox.navigation.base.extensions

import android.content.Context
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.core.constants.Constants
import com.mapbox.navigation.base.internal.extensions.LocaleEx.getUnitTypeForLocale
import com.mapbox.navigation.base.internal.extensions.inferDeviceLocale

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
            it == DirectionsCriteria.ANNOTATION_DISTANCE
    } ?: false
    return isTrafficProfile && isOverviewFull && hasCongestionOrMaxSpeed
}

/**
 * Applies default [RouteOptions] parameters to the RouteOptions builder
 *
 * @receiver RouteOptions.Builder
 * @return RouteOptions.Builder
 */
fun RouteOptions.Builder.applyDefaultOptions(): RouteOptions.Builder = also {
    baseUrl(Constants.BASE_API_URL)
    user(Constants.MAPBOX_USER)
    profile(DirectionsCriteria.PROFILE_DRIVING)
    geometries(DirectionsCriteria.GEOMETRY_POLYLINE6)
    requestUuid("")
}

/**
 * Apply [RouteOptions] location and voice unit based on [Context]
 */
fun RouteOptions.Builder.applyLocationAndVoiceUnit(context: Context): RouteOptions.Builder = also {
    language(context.inferDeviceLocale().language)
    voiceUnits(context.inferDeviceLocale().getUnitTypeForLocale().value)
}

/**
 * Apply recommended options: _continueStraight_, _roundaboutExits_, _overview_, _steps_,
 * _annotationList_ (congestion and distance), _voiceInstruction_, and _bannerInstructions_.
 */
fun RouteOptions.Builder.applyRecommendedOptions(): RouteOptions.Builder = also {
    continueStraight(true)
    roundaboutExits(true)
    overview(DirectionsCriteria.OVERVIEW_FULL)
    steps(true)
    annotationsList(
        listOf(
            DirectionsCriteria.ANNOTATION_CONGESTION,
            DirectionsCriteria.ANNOTATION_DISTANCE
        )
    )
    voiceInstructions(true)
    bannerInstructions(true)
}
