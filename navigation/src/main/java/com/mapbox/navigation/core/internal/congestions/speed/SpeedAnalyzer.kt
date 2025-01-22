package com.mapbox.navigation.core.internal.congestions.speed

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.core.internal.congestions.model.MetersPerSecond
import com.mapbox.navigation.core.internal.congestions.model.SpeedAnalysisResult

internal fun interface SpeedAnalyzer {
    operator fun invoke(
        currentLegProgress: RouteLegProgress,
        navigationRoute: NavigationRoute,
        currentSpeed: MetersPerSecond,
        expectedSpeed: MetersPerSecond,
    ): SpeedAnalysisResult
}
