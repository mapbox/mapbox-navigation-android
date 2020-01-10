@file:JvmName("Mappers")

package com.mapbox.navigation.route.offboard.extension

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.WalkingOptions
import com.mapbox.api.directions.v5.models.BannerComponents
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.BannerText
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.IntersectionLanes
import com.mapbox.api.directions.v5.models.LegAnnotation
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.api.directions.v5.models.MaxSpeed
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.api.directions.v5.models.StepIntersection
import com.mapbox.api.directions.v5.models.StepManeuver
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.navigation.base.route.model.BannerComponentsNavigation
import com.mapbox.navigation.base.route.model.BannerInstructionsNavigation
import com.mapbox.navigation.base.route.model.BannerTextNavigation
import com.mapbox.navigation.base.route.model.IntersectionLanesNavigation
import com.mapbox.navigation.base.route.model.LegAnnotationNavigation
import com.mapbox.navigation.base.route.model.LegStepNavigation
import com.mapbox.navigation.base.route.model.MaxSpeedNavigation
import com.mapbox.navigation.base.route.model.Route
import com.mapbox.navigation.base.route.model.RouteLegNavigation
import com.mapbox.navigation.base.route.model.RouteOptionsNavigation
import com.mapbox.navigation.base.route.model.StepIntersectionNavigation
import com.mapbox.navigation.base.route.model.StepManeuverNavigation
import com.mapbox.navigation.base.route.model.VoiceInstructionsNavigation
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
    distance = distance() ?: .0,
    duration = duration()?.toLong() ?: 0,
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

fun RouteLeg.mapToRouteLegNavigation() = RouteLegNavigation.Builder()
    .distance(distance())
    .duration(duration())
    .steps(steps()?.map { it.mapToLegStep() })
    .summary(summary())
    .build()

fun LegStep.mapToLegStepNavigation() = LegStepNavigation.Builder()
    .stepManeuver(maneuver().mapToStepManeuverNavigation())
    .distance(distance())
    .drivingSide(drivingSide())
    .duration(duration())
    .geometry(geometry())
    .build()

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

fun VoiceInstructions.mapToVoiceInstructionsNavigation() = VoiceInstructionsNavigation(
    distanceAlongGeometry = distanceAlongGeometry(),
    announcement = announcement(),
    ssmlAnnouncement = ssmlAnnouncement()
)

fun BannerInstructions.mapToBannerInstructionsNavigation() = BannerInstructionsNavigation(
    distanceAlongGeometry = distanceAlongGeometry(),
    primary = primary().mapToBannerTextNavigation(),
    secondary = secondary()?.mapToBannerTextNavigation(),
    sub = sub()?.mapToBannerTextNavigation()
)

fun BannerText.mapToBannerTextNavigation() = BannerTextNavigation(
    text = text(),
    components = components()?.map { it.mapToBannerComponentsNavigation() },
    type = type(),
    modifier = modifier(),
    degrees = degrees(),
    drivingSide = drivingSide()
)

fun BannerComponents.mapToBannerComponentsNavigation() = BannerComponentsNavigation(
    text = text(),
    type = type(),
    abbreviation = abbreviation(),
    abbreviationPriority = abbreviationPriority(),
    imageBaseUrl = imageBaseUrl(),
    directions = directions(),
    active = active()
)

fun StepManeuver.mapToStepManeuverNavigation() = StepManeuverNavigation.Builder()
    .modifier(modifier())
    .type(type())
    .build()

fun StepIntersection.mapToStepIntersectionNavigation() = StepIntersectionNavigation(
    location = location(),
    bearings = bearings(),
    classes = classes(),
    entry = entry(),
    into = `in`(),
    out = out(),
    lanes = lanes()?.map { it.mapToIntersectionLanesNavigation() }
)

fun IntersectionLanes.mapToIntersectionLanesNavigation() = IntersectionLanesNavigation(
    valid = valid(),
    indications = indications()
)

fun LegAnnotation.mapToLegAnnotationNavigation() = LegAnnotationNavigation(
    distance = distance(),
    duration = duration(),
    speed = speed(),
    maxspeed = maxspeed()?.map(MaxSpeed::mapToMaxSpeedNavigation),
    congestion = congestion()
)

fun MaxSpeed.mapToMaxSpeedNavigation() = MaxSpeedNavigation(
    speed = speed(),
    unit = unit(),
    unknown = unknown(),
    none = none()
)

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
