@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)

package com.mapbox.navigation.ui.maps.route.callout.api

import androidx.annotation.RestrictTo
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.sign
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.times
import kotlin.time.toDuration

/**
 * Rounding up duration value by abs.
 * e.g.
 * (1 m 22 sec).roundUpByAbs(DurationUnit.MINUTE) = 2 m
 * (-1 m 22 sec).roundUpByAbs(DurationUnit.MINUTE) = -2 m
 */
internal fun Duration.roundUpByAbs(unit: DurationUnit): Duration {
    val durationByUnit = this.toDouble(unit)
    return sign(durationByUnit) * ceil(abs(durationByUnit)).toDuration(unit)
}
