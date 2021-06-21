@file:JvmName("DirectionsRouteEx")

package com.mapbox.navigation.core.internal.utils

import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.LegStep
import com.mapbox.navigation.utils.internal.ifNonNull

fun DirectionsRoute.isSameUuid(compare: DirectionsRoute?): Boolean =
    this?.requestUuid() == compare?.requestUuid()

/**
 * Compare routes as geometries(if exist) or as a names of [LegStep] of the [DirectionsRoute]
 */
fun DirectionsRoute.isSameRoute(compare: DirectionsRoute?): Boolean {
    if (compare == null) return false

    ifNonNull(this.geometry(), compare.geometry()) { g1, g2 ->
        return g1 == g2
    }

    ifNonNull(this.stepsNamesAsString(), compare.stepsNamesAsString()) { s1, s2 ->
        return s1 == s2
    }

    return false
}

private fun DirectionsRoute.stepsNamesAsString(): String? =
    this.legs()
        ?.joinToString { leg ->
            leg.steps()?.joinToString { step -> step.name() ?: "" } ?: ""
        }
