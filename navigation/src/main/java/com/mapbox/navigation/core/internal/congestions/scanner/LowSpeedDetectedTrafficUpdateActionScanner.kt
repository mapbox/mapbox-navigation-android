package com.mapbox.navigation.core.internal.congestions.scanner

import com.mapbox.navigation.core.internal.congestions.model.SpeedAnalysisResult
import com.mapbox.navigation.core.internal.congestions.model.TrafficUpdateAction
import com.mapbox.navigation.utils.internal.Time
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Since the user may drops their speed temporary for some reason the scanner keeps tracking their
 * speed for 20 seconds before triggering [TrafficUpdateAction.IncreaseTraffic].
 */
internal class LowSpeedDetectedTrafficUpdateActionScanner : TrafficUpdateActionScanner {
    override fun scan(
        acc: TrafficUpdateAction,
        value: SpeedAnalysisResult,
    ): TrafficUpdateAction? = when (value) {
        is SpeedAnalysisResult.LowSpeedDetected -> {
            val firstLowSpeedWasDetectedAt = (acc as? TrafficUpdateAction.AccumulatingLowSpeed)
                ?.accumulationStart
                ?: value.resultElapsedMilliseconds.milliseconds
            val currentMillis = Time.SystemClockImpl.millis()

            val timeUntilTrafficUpdate = TO_WAIT_UNTIL_UPDATE -
                (currentMillis.milliseconds - firstLowSpeedWasDetectedAt)

            when {
                timeUntilTrafficUpdate < Duration.ZERO -> TrafficUpdateAction.IncreaseTraffic(
                    value.route,
                    value.legProgress,
                    value.expectedCongestion,
                )

                else -> TrafficUpdateAction.AccumulatingLowSpeed(
                    accumulationStart = firstLowSpeedWasDetectedAt,
                    timeUntilUpdate = timeUntilTrafficUpdate,
                    latestLowSpeedDetectedResult = value,
                )
            }
        }

        is SpeedAnalysisResult.FailedToAnalyze,
        is SpeedAnalysisResult.HighSpeedDetected,
        is SpeedAnalysisResult.SkippedAnalysis,
        is SpeedAnalysisResult.SpeedIsOk,
        is SpeedAnalysisResult.SpeedMatchesCongestionLevel,
        is SpeedAnalysisResult.WrongFalsePositiveOverrideDetected,
        -> null
    }

    companion object {
        private val TO_WAIT_UNTIL_UPDATE = 20.seconds
    }
}
