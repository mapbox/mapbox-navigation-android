@file:JvmName("Mappers")

package com.mapbox.services.android.navigation.v5.utils.extensions

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
import com.mapbox.services.android.navigation.v5.navigation.WalkingOptionsNavigation

fun WalkingOptionsNavigation.mapToWalkingOptions(): WalkingOptions = WalkingOptions
    .builder()
    .walkingSpeed(walkingSpeed)
    .walkwayBias(walkwayBias)
    .alleyBias(alleyBias)
    .build()

fun Route.mapToDirectionsRoute(): DirectionsRoute {
    return DirectionsRoute.builder() // FIXME route index
        .distance(distance)
        .duration(duration.toDouble())
        .geometry(geometry)
        .weight(weight)
        .weightName(weightName)
        .voiceLanguage(voiceLanguage)
        .legs(legs?.map(RouteLegNavigation::mapToRouteLeg))
        .routeOptions(routeOptions?.mapToRouteOptions())
        .build()
}

fun RouteOptionsNavigation.mapToRouteOptions(): RouteOptions =
    RouteOptions.builder()
        .apply {
            walkingOptions?.let { walkingOptions(it.mapToWalkingOptions()) }
            accessToken?.let { accessToken(it) }
            exclude?.let { exclude(it) }
            requestUuid?.let { requestUuid(it) }
        }
        .profile(profile)
        .user(user)
        .baseUrl(baseUrl)
        .annotations(annotations)
        .approaches(approaches)!!
        .bannerInstructions(bannerInstructions)
        .bearings(bearings)
        .continueStraight(continueStraight)
        .coordinates(coordinates)
        .geometries(geometries)
        .language(language)
        .overview(overview)
        .radiuses(radiuses)
        .roundaboutExits(roundaboutExits)
        .steps(steps)
        .alternatives(alternatives)
        .voiceInstructions(voiceInstructions)
        .voiceUnits(voiceUnits)
        .waypointIndices(waypointIndices)!!
        .waypointNames(waypointNames)!!
        .waypointTargets(waypointTargets)!!
        .build()

fun RouteLegNavigation.mapToRouteLeg(): RouteLeg =
    RouteLeg.builder()
        .annotation(annotation?.mapToLegAnnotation())
        .distance(distance)
        .duration(duration)
        .steps(steps?.map(LegStepNavigation::mapToLegStep))
        .summary(summary)
        .build()

fun LegAnnotationNavigation.mapToLegAnnotation(): LegAnnotation =
    LegAnnotation.builder()
        .congestion(congestion)
        .distance(distance)
        .duration(duration)
        .maxspeed(maxspeed?.map(MaxSpeedNavigation::mapToMaxSpeed))
        .speed(speed)
        .build()

fun LegStepNavigation.mapToLegStep(): LegStep =
    LegStep.builder()
        .bannerInstructions(bannerInstructions?.map(BannerInstructionsNavigation::mapToBannerInstruction) ?: emptyList())
        .destinations(destinations)
        .distance(distance)
        .drivingSide(drivingSide)
        .duration(duration)
        .exits(exits)
        .geometry(geometry)
        .intersections(intersections?.map(StepIntersectionNavigation::mapToStepIntersection) ?: emptyList())
        .maneuver(maneuver.mapToStepManeuver())
        .mode(mode)
        .name(name)
        .pronunciation(pronunciation)
        .ref(ref)
        .rotaryName(rotaryName)
        .rotaryPronunciation(rotaryPronunciation)
        .voiceInstructions(voiceInstructions?.map(VoiceInstructionsNavigation::mapToVoiceInstructions) ?: emptyList())
        .weight(weight)
        .build()

fun MaxSpeedNavigation.mapToMaxSpeed(): MaxSpeed =
    MaxSpeed.builder()
        .speed(speed)
        .none(none)
        .unit(unit)
        .unknown(unknown)
        .build()

fun BannerInstructionsNavigation.mapToBannerInstruction(): BannerInstructions =
    BannerInstructions.builder()
        .distanceAlongGeometry(distanceAlongGeometry)
        .primary(primary?.mapToBannerText() ?: BannerText.builder().build())
        .secondary(secondary?.mapToBannerText())
        .sub(sub?.mapToBannerText())
        .build()

fun StepIntersectionNavigation.mapToStepIntersection(): StepIntersection =
    StepIntersection.builder()
        .bearings(bearings)
        .classes(classes)
        .entry(entry)
        .`in`(into)
        .out(out)
        .lanes(lanes?.map(IntersectionLanesNavigation::mapToIntersectionLanes))
        .rawLocation(rawLocation)
        .build()

fun StepManeuverNavigation.mapToStepManeuver(): StepManeuver =
    StepManeuver.builder()
        .bearingAfter(bearingAfter)
        .bearingBefore(bearingBefore)
        .exit(exit)
        .instruction(instruction)
        .modifier(modifier)
        .rawLocation(rawLocation)
        .type(type)
        .build()

fun VoiceInstructionsNavigation.mapToVoiceInstructions(): VoiceInstructions =
    VoiceInstructions.builder()
        .announcement(announcement)
        .distanceAlongGeometry(distanceAlongGeometry)
        .ssmlAnnouncement(ssmlAnnouncement)
        .build()

fun BannerTextNavigation.mapToBannerText(): BannerText =
    BannerText.builder()
        .text(text ?: "")!!
        .components(components?.map(BannerComponentsNavigation::mapToBannerComponents))!!
        .degrees(degrees)
        .drivingSide(drivingSide)
        .modifier(modifier)
        .type(type)
        .build()

fun IntersectionLanesNavigation.mapToIntersectionLanes(): IntersectionLanes =
    IntersectionLanes.builder()
        .indications(indications)
        .valid(valid)
        .build()

fun BannerComponentsNavigation.mapToBannerComponents(): BannerComponents =
    BannerComponents.builder()
        .abbreviation(abbreviation)
        .abbreviationPriority(abbreviationPriority)
        .active(active)
        .directions(directions)
        .imageBaseUrl(imageBaseUrl)
        .text(text)
        .type(type)
        .build()
