@file:JvmName("Mappers")

package com.mapbox.navigation.route.offboard.extension

import android.location.Location
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.api.directions.v5.models.StepManeuver
import com.mapbox.navigation.base.route.model.LegStepNavigation
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.model.Route
import com.mapbox.navigation.base.route.model.RouteLegNavigation
import com.mapbox.navigation.base.route.model.RouteOptionsNavigation
import com.mapbox.navigation.base.route.model.StepManeuverNavigation
import com.mapbox.navigation.base.route.model.WalkingOptionsNavigation
import java.util.Locale

fun StepManeuver.mapToStepManeuver() = StepManeuverNavigation.Builder()
    .type(type())
    .modifier(modifier())
    .build()

fun LegStep.mapToLegStep() = LegStepNavigation.Builder()
    .distance(distance())
    .duration(duration())
    .stepManeuver(maneuver().mapToStepManeuver())
    .build()

fun RouteLeg.mapToRouteLeg() = RouteLegNavigation.Builder()
    .distance(distance())
    .duration(duration())
    .summary(summary())
    .steps(
        steps()?.let { stepList ->
            stepList.map {
                it.mapToLegStep()
            }
        }
    )
    .build()

fun DirectionsRoute.mapToRoute() = Route(
    routeIndex = routeIndex(),
    distance = distance()!!,
    duration = duration()?.toLong()!!,
    geometry = geometry(),
    weight = weight(),
    weightName = weightName(),
    legs = legs()?.let { routeLegs ->
        routeLegs.map {
            it.mapToRouteLeg()
        }
    },
    routeOptions = routeOptions()?.mapToRouteOptionsNavigation(),
    voiceLanguage = voiceLanguage()
)

fun RouteOptions.mapToRouteOptionsNavigation(): RouteOptionsNavigation {
    val routeOptionsNavigationBuilder = RouteOptionsNavigation
        .builder()
        .baseUrl(baseUrl())
        .user(user())
        .profile(profile())
        .origin(coordinates().first())
        .destination(coordinates().last())
    coordinates().drop(1).dropLast(1)
        .forEach { routeOptionsNavigationBuilder.addWaypoint(it) }
    return routeOptionsNavigationBuilder
        .alternatives(alternatives() ?: true)
        .language(language() ?: Locale.getDefault().language)
        .radiuses(radiuses() ?: "")
        .bearings(bearings() ?: "")
        .continueStraight(continueStraight() ?: true)
        .roundaboutExits(roundaboutExits() ?: true)
        .geometries(geometries() ?: DirectionsCriteria.GEOMETRY_POLYLINE6)
        .overview(overview() ?: DirectionsCriteria.OVERVIEW_FULL)
        .steps(steps() ?: true)
        .annotations(
            annotations() ?: arrayOf(
                DirectionsCriteria.ANNOTATION_CONGESTION,
                DirectionsCriteria.ANNOTATION_DISTANCE
            ).joinToString(separator = ",")
        )
        .voiceInstructions(voiceInstructions() ?: true)
        .bannerInstructions(bannerInstructions() ?: true)
        .voiceUnits(voiceUnits() ?: "")
        .accessToken(accessToken())
        .requestUuid(requestUuid())
        .exclude(exclude() ?: "")
        .approaches(approaches() ?: "")
        .waypointIndices(waypointIndices() ?: "")
        .waypointNames(waypointNames() ?: "")
        .waypointTargets(waypointTargets() ?: "")
        .walkingOptions(
            walkingOptions()?.mapToWalkingOptionsNavigation() ?: WalkingOptionsNavigation()
        )
        .build()
}

fun WalkingOptionsNavigation.mapToWalkingOptions(): WalkingOptions = WalkingOptions
    .builder()
    .walkingSpeed(walkingSpeed)
    .walkwayBias(walkwayBias)
    .alleyBias(alleyBias)
    .build()

fun WalkingOptions.mapToWalkingOptionsNavigation(): WalkingOptionsNavigation =
    WalkingOptionsNavigation(
        walkingSpeed = walkingSpeed(),
        walkwayBias = walkwayBias(),
        alleyBias = alleyBias()
    )
