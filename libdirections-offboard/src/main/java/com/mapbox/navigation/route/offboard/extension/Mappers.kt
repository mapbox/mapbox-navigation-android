package com.mapbox.navigation.route.offboard.extension

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.WalkingOptions
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.core.constants.Constants
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.route.model.Route
import com.mapbox.navigation.base.route.model.RouteLegsNavigation
import com.mapbox.navigation.base.route.model.RouteOptionsNavigation
import com.mapbox.navigation.base.route.model.RoutePointNavigation
import com.mapbox.navigation.base.route.model.WalkingOptionsNavigation
import java.util.Locale

fun DirectionsRoute.mapToRoute() = Route(
    routeIndex = routeIndex(),
    distance = distance()!!,
    duration = duration()?.toLong()!!,
    geometry = geometry(),
    weight = weight(),
    weightName = weightName(),
    voiceLanguage = voiceLanguage(),
    legs = legs()?.let { RouteLegsNavigation(it) },
    routeOptions = routeOptions()?.mapToRouteOptionsNavigation()
)

fun RouteOptionsNavigation.mapToRouteOptions() = RouteOptions
    .builder()
    .baseUrl(baseUrl ?: Constants.BASE_API_URL)
    .user(user ?: Constants.MAPBOX_USER)
    .profile(profile ?: DirectionsCriteria.PROFILE_DRIVING)
    .coordinates(coordinates.map { it.mapToPoint() })
    .alternatives(alternatives)
    .language(language)
    .radiuses(radiuses)
    .continueStraight(continueStraight)
    .roundaboutExits(roundaboutExits)
    .geometries(geometries)
    .overview(overview)
    .steps(steps)
    .annotations(annotations)
    .voiceInstructions(voiceInstructions)
    .bannerInstructions(bannerInstructions)
    .voiceUnits(voiceUnits)
    .accessToken(accessToken ?: "")
    .requestUuid(requestUuid ?: "")
    .exclude(exclude ?: "")
    ?.approaches(approaches)
    ?.waypointIndices(waypointIndices)
    ?.waypointNames(waypointNames)
    ?.waypointTargets(waypointTargets)
    ?.walkingOptions(walkingOptions?.mapToWalkingOptions()!!)
    ?.build()

fun RoutePointNavigation.mapToPoint() =
    Point.fromLngLat(point.longitude(), point.latitude(), point.altitude(), point.bbox())

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
        .geometries(geometries())
        .overview(overview() ?: "")
        .steps(steps() ?: true)
        .annotations(annotations() ?: "")
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
        .walkingOptions(walkingOptions()?.mapToWalkingOptionsNavigation()!!)
        .build()
}

fun WalkingOptionsNavigation.mapToWalkingOptions(): WalkingOptions = WalkingOptions
    .builder()
    .walkingSpeed(walkingSpeed)
    .walkwayBias(walkwayBias)
    .alleyBias(alleyBias)
    .build()

fun WalkingOptions.mapToWalkingOptionsNavigation(): WalkingOptionsNavigation = WalkingOptionsNavigation(
    walkingSpeed = walkingSpeed(),
    walkwayBias = walkwayBias(),
    alleyBias = alleyBias()
)
