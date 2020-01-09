@file:JvmName("Mappers")

package com.mapbox.navigation.examples.utils.extensions

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.api.directions.v5.models.RouteLeg
import com.mapbox.navigation.base.route.model.LegStepNavigation
import com.mapbox.navigation.base.route.model.Route
import com.mapbox.navigation.base.route.model.RouteLegNavigation

fun LegStepNavigation.mapToLegStep(): LegStep {
    return LegStep.builder()
        .distance(distance())
        .duration(duration())
        .build()
}

fun RouteLegNavigation.mapToRouteLeg(): RouteLeg {
    val steps = steps()?.let { stepLegList ->
        stepLegList.map {
            it.mapToLegStep()
        }
    }
    return RouteLeg.builder()
        .distance(distance())
        .duration(duration())
        .summary(summary())
        .steps(steps)
        .build()
}

fun Route.mapToDirectionsRoute(): DirectionsRoute {
    val duration = duration.toDouble()
    val legs = legs?.let { legList ->
        legList.map {
            it.mapToRouteLeg()
        }
    }
    return DirectionsRoute.builder()
        .distance(distance)
        .duration(duration)
        .geometry(geometry)
        .weight(weight)
        .weightName(weightName)
        .voiceLanguage(voiceLanguage)
        .legs(legs)
        .build()
}
