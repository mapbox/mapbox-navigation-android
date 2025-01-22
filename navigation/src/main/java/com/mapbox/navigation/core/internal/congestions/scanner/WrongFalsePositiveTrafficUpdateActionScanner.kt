package com.mapbox.navigation.core.internal.congestions.scanner

import com.mapbox.navigation.core.internal.congestions.model.SpeedAnalysisResult
import com.mapbox.navigation.core.internal.congestions.model.TrafficUpdateAction

internal class WrongFalsePositiveTrafficUpdateActionScanner : TrafficUpdateActionScanner {
    override fun scan(
        acc: TrafficUpdateAction,
        value: SpeedAnalysisResult,
    ): TrafficUpdateAction? = when (value) {
        is SpeedAnalysisResult.WrongFalsePositiveOverrideDetected ->
            TrafficUpdateAction.RestoreTraffic(
                route = value.route,
                congestionNumericOverride = value.congestionNumericOverride,
            )
        is SpeedAnalysisResult.FailedToAnalyze,
        is SpeedAnalysisResult.HighSpeedDetected,
        is SpeedAnalysisResult.LowSpeedDetected,
        is SpeedAnalysisResult.SkippedAnalysis,
        is SpeedAnalysisResult.SpeedIsOk,
        is SpeedAnalysisResult.SpeedMatchesCongestionLevel,
        -> null
    }
}
