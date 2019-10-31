package com.mapbox.navigation.route.common.extension

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.api.directions.v5.models.StepManeuver
import com.mapbox.navigation.base.route.model.LegStepNavigation
import com.mapbox.navigation.base.route.model.PointNavigation
import com.mapbox.navigation.base.route.model.Route
import com.mapbox.navigation.base.route.model.RouteLegNavigation
import com.mapbox.navigation.base.route.model.StepManeuverNavigation

fun LegStep.mapToStep() = LegStepNavigation(
    distance = distance(),
    duration = duration(),
    geometry = geometry(),
    name = name(),
    ref = ref(),
    destinations = destinations(),
    mode = mode(),
    pronunciation = pronunciation(),
    rotaryName = rotaryName(),
    rotaryPronunciation = rotaryPronunciation(),
    maneuver = maneuver().mapToManeuverStep()
)

fun DirectionsRoute.mapToRoute() = Route(
    routeIndex = routeIndex(),
    distance = distance(),
    duration = duration()?.toLong(),
    geometry = geometry(),
    weight = weight(),
    weightName = weightName(),
    voiceLanguage = voiceLanguage(),
    legs = legs()?.map { it.mapToLeg() }
)

fun RouteLeg.mapToLeg() = RouteLegNavigation(
    distance = distance(),
    duration = duration(),
    summary = summary(),
    steps = steps()?.map { it.mapToStep() }
)

fun StepManeuver.mapToManeuverStep() = StepManeuverNavigation(
    location = PointNavigation(location().longitude(), location().latitude()),
    bearingBefore = bearingBefore(),
    bearingAfter = bearingAfter(),
    type = if (typesStepManeuverNavigation.contains(type())) type() else null,
    exit = exit()
)

private val typesStepManeuverNavigation = arrayOf(
    StepManeuverNavigation.TURN,
    StepManeuverNavigation.NEW_NAME,
    StepManeuverNavigation.DEPART,
    StepManeuverNavigation.ARRIVE,
    StepManeuverNavigation.MERGE,
    StepManeuverNavigation.ON_RAMP,
    StepManeuverNavigation.OFF_RAMP,
    StepManeuverNavigation.FORK,
    StepManeuverNavigation.END_OF_ROAD,
    StepManeuverNavigation.CONTINUE,
    StepManeuverNavigation.ROUNDABOUT,
    StepManeuverNavigation.ROTARY,
    StepManeuverNavigation.ROUNDABOUT_TURN,
    StepManeuverNavigation.NOTIFICATION,
    StepManeuverNavigation.EXIT_ROUNDABOUT,
    StepManeuverNavigation.EXIT_ROTARY
)
