package com.mapbox.navigation.core.internal.congestions.model

import com.mapbox.navigation.base.internal.CongestionNumericOverride
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import kotlin.time.Duration

internal sealed class TrafficUpdateAction {
    data class IncreaseTraffic(
        val route: NavigationRoute,
        val legProgress: RouteLegProgress,
        val expectedCongestion: Int,
    ) : TrafficUpdateAction()

    data class RestoreTraffic(
        val route: NavigationRoute,
        val congestionNumericOverride: CongestionNumericOverride,
    ) : TrafficUpdateAction()

    data class DecreaseTraffic(
        val currentSpeed: MetersPerSecond,
        val legProgress: RouteLegProgress,
        val navigationRoute: NavigationRoute,
    ) : TrafficUpdateAction()

    data class AccumulatingLowSpeed(
        val accumulationStart: Duration,
        val timeUntilUpdate: Duration,
        val latestLowSpeedDetectedResult: SpeedAnalysisResult.LowSpeedDetected,
    ) : TrafficUpdateAction()

    object NoAction : TrafficUpdateAction()
}
