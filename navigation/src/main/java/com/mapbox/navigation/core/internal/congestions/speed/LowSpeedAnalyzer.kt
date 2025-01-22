package com.mapbox.navigation.core.internal.congestions.speed

import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.core.internal.congestions.model.MetersPerSecond
import com.mapbox.navigation.core.internal.congestions.model.SpeedAnalysisResult
import com.mapbox.navigation.utils.internal.Time

internal class LowSpeedAnalyzer : SpeedAnalyzer {
    /**
     * Checks if the congestion value on the current [RouteLegProgress.geometryIndex] matches
     * with [currentSpeed] (determined as low speed), and if not calculates which congestion value
     * aligns with this speed
     */
    override fun invoke(
        currentLegProgress: RouteLegProgress,
        navigationRoute: NavigationRoute,
        currentSpeed: MetersPerSecond,
        expectedSpeed: MetersPerSecond,
    ): SpeedAnalysisResult {
        val expectedMinimalCongestion = calculateExpectedCongestions(
            speed = currentSpeed.value.toDouble(),
            limit = expectedSpeed.value.toDouble(),
        )
        return run {
            val currentCongestion = getCurrentCongestion(currentLegProgress)
            val timeToUpdate = if (currentCongestion != null) {
                currentCongestion < expectedMinimalCongestion
            } else {
                true
            }
            if (timeToUpdate) {
                SpeedAnalysisResult.LowSpeedDetected(
                    currentSpeed = currentSpeed,
                    expectedSpeed = expectedSpeed,
                    currentCongestion = currentCongestion,
                    expectedCongestion = expectedMinimalCongestion,
                    legProgress = currentLegProgress,
                    route = navigationRoute,
                    resultElapsedMilliseconds = Time.SystemClockImpl.millis(),
                )
            } else {
                SpeedAnalysisResult.SpeedMatchesCongestionLevel(
                    currentSpeed = currentSpeed,
                    expectedCongestionForCurrentSpeed = expectedMinimalCongestion,
                    congestion = currentCongestion,
                )
            }
        }
    }

    // inspired by https://docs.google.com/spreadsheets/d/1kyf9QCYEDA7LTlelmJhMsUn6IK9YJesqDm0_FE3wC4w/edit#gid=0
    private fun calculateExpectedCongestions(speed: Double, limit: Double): Int {
        return try {
            if (speed < limit / 2) {
                (50 + (limit / 2 - speed) * 50 / (limit / 2)).toInt()
            } else {
                0
            }
        } catch (t: Throwable) {
            0
        }
    }

    private fun getCurrentCongestion(currentLegProgress: RouteLegProgress?): Int? {
        return currentLegProgress
            ?.routeLeg
            ?.annotation()
            ?.congestionNumeric()
            ?.run { get(currentLegProgress.geometryIndex.coerceAtMost(lastIndex)) }
    }
}
