package com.mapbox.navigation.core.internal.congestions.scanner

import com.mapbox.navigation.core.internal.congestions.model.SpeedAnalysisResult
import com.mapbox.navigation.core.internal.congestions.model.TrafficUpdateAction

internal class HighSpeedDetectedTrafficUpdateActionScanner : TrafficUpdateActionScanner {
    override fun scan(
        acc: TrafficUpdateAction,
        value: SpeedAnalysisResult,
    ): TrafficUpdateAction? = when (value) {
        is SpeedAnalysisResult.HighSpeedDetected -> TrafficUpdateAction.DecreaseTraffic(
            value.currentSpeed,
            value.legProgress,
            value.route,
        )

        is SpeedAnalysisResult.FailedToAnalyze,
        is SpeedAnalysisResult.LowSpeedDetected,
        is SpeedAnalysisResult.SkippedAnalysis,
        is SpeedAnalysisResult.SpeedIsOk,
        is SpeedAnalysisResult.SpeedMatchesCongestionLevel,
        is SpeedAnalysisResult.WrongFalsePositiveOverrideDetected,
        -> null
    }
}
