package com.mapbox.navigation.core.internal.congestions.speed

import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.internal.congestions.model.SpeedAnalysisResult
import com.mapbox.navigation.core.trip.session.LocationMatcherResult

internal fun interface SpeedAnalysisResultHandler {
    operator fun invoke(
        routeProgress: RouteProgress,
        location: LocationMatcherResult,
    ): SpeedAnalysisResult
}
