package com.mapbox.navigation.core.navigator

import com.mapbox.navigation.core.trip.session.MapMatcherResult
import com.mapbox.navigation.navigator.internal.TripStatus

internal fun TripStatus.getMapMatcherResult(): MapMatcherResult {
    return MapMatcherResult(
        enhancedLocation,
        keyPoints,
        navigationStatus.offRoadProba > 0.5,
        navigationStatus.offRoadProba,
        navigationStatus.map_matcher_output.isTeleport,
        navigationStatus.map_matcher_output.matches.firstOrNull()?.proba ?: 0f
    )
}
