package com.mapbox.navigation.core.internal.congestions.model

import com.mapbox.navigation.base.internal.CongestionNumericOverride
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteLegProgress

internal sealed class SpeedAnalysisResult {
    data class LowSpeedDetected(
        val currentSpeed: MetersPerSecond,
        val expectedSpeed: MetersPerSecond,
        val currentCongestion: Int?,
        val expectedCongestion: Int,
        val legProgress: RouteLegProgress,
        val route: NavigationRoute,
        val resultElapsedMilliseconds: Long,
    ) : SpeedAnalysisResult()

    data class WrongFalsePositiveOverrideDetected(
        val route: NavigationRoute,
        val congestionNumericOverride: CongestionNumericOverride,
    ) : SpeedAnalysisResult()

    data class SpeedIsOk(
        val speed: MetersPerSecond,
        val expectedSpeed: MetersPerSecond,
    ) : SpeedAnalysisResult()

    data class HighSpeedDetected(
        val currentSpeed: MetersPerSecond,
        val legProgress: RouteLegProgress,
        val route: NavigationRoute,
    ) : SpeedAnalysisResult()

    data class SpeedMatchesCongestionLevel(
        val currentSpeed: MetersPerSecond,
        val expectedCongestionForCurrentSpeed: Int,
        val congestion: Int?,
    ) : SpeedAnalysisResult()

    data class FailedToAnalyze(val message: String) : SpeedAnalysisResult()

    data class SkippedAnalysis(val message: String) : SpeedAnalysisResult()
}
