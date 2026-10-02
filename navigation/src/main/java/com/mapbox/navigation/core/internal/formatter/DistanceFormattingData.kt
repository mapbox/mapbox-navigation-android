package com.mapbox.navigation.core.internal.formatter

import androidx.annotation.RestrictTo
import com.mapbox.navigation.base.formatter.UnitType
import com.mapbox.navigation.core.formatter.MapboxDistanceUtil
import com.mapbox.turf.TurfConstants
import java.util.Locale

/**
 * A distance rounded for display by [MapboxDistanceUtil], before the unit is resolved to a
 * localized suffix.
 *
 * @param distance the distance in [turfDistanceUnit]. Small units hold the rounded value, and
 * large units hold the exact value, which is shown with [fractionDigits] decimals
 * @param distanceAsString [distance] formatted for the locale with [fractionDigits] decimals
 * @param turfDistanceUnit the Turf unit of [distance], for example [TurfConstants.UNIT_YARDS]
 * @param unitType the unit system of [turfDistanceUnit]
 * @param fractionDigits the maximum number of decimals [distanceAsString] shows
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
data class DistanceFormattingData(
    val distance: Double,
    val distanceAsString: String,
    @TurfConstants.TurfUnitCriteria val turfDistanceUnit: String,
    val unitType: UnitType,
    val fractionDigits: Int,
)

/**
 * Rounds [distanceInMeters] with the same thresholds, units and decimals as
 * [MapboxDistanceUtil.formatDistance], without resolving the unit suffix from resources.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun MapboxDistanceUtil.formattingData(
    distanceInMeters: Double,
    roundingIncrement: Int,
    unitType: UnitType,
    locale: Locale,
): DistanceFormattingData = getFormattingData(distanceInMeters, roundingIncrement, unitType, locale)
