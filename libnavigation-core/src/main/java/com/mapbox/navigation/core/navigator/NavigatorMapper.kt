package com.mapbox.navigation.core.navigator

import com.mapbox.navigation.base.speed.model.SpeedLimit
import com.mapbox.navigation.core.trip.session.MapMatcherResult
import com.mapbox.navigation.navigator.internal.TripStatus
import com.mapbox.navigation.utils.internal.ifNonNull
import com.mapbox.navigator.NavigationStatus
import com.mapbox.navigator.SpeedLimitSign
import com.mapbox.navigator.SpeedLimitUnit

internal fun TripStatus.getMapMatcherResult(): MapMatcherResult {
    return MapMatcherResult(
        enhancedLocation,
        keyPoints,
        navigationStatus.offRoadProba > 0.5,
        navigationStatus.offRoadProba,
        navigationStatus.map_matcher_output.isTeleport,
        navigationStatus.prepareSpeedLimit(),
        navigationStatus.map_matcher_output.matches.firstOrNull()?.proba ?: 0f
    )
}

internal fun NavigationStatus.prepareSpeedLimit(): SpeedLimit? {
    return ifNonNull(speedLimit) { limit ->
        val speedLimitUnit = when (limit.localeUnit) {
            SpeedLimitUnit.KILOMETRES_PER_HOUR ->
                com.mapbox.navigation.base.speed.model.SpeedLimitUnit.KILOMETRES_PER_HOUR
            else -> com.mapbox.navigation.base.speed.model.SpeedLimitUnit.MILES_PER_HOUR
        }
        val speedLimitSign = when (limit.localeSign) {
            SpeedLimitSign.MUTCD -> com.mapbox.navigation.base.speed.model.SpeedLimitSign.MUTCD
            else -> com.mapbox.navigation.base.speed.model.SpeedLimitSign.VIENNA
        }
        SpeedLimit(
            limit.speedKmph,
            speedLimitUnit,
            speedLimitSign
        )
    }
}
