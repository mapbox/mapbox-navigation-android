package com.mapbox.navigation.core.internal.congestions.scanner

import com.mapbox.navigation.core.internal.congestions.model.SpeedAnalysisResult
import com.mapbox.navigation.core.internal.congestions.model.TrafficUpdateAction

internal class NoActionTrafficUpdateActionScanner : TrafficUpdateActionScanner {
    override fun scan(
        acc: TrafficUpdateAction,
        value: SpeedAnalysisResult,
    ): TrafficUpdateAction? = when (acc) {
        is TrafficUpdateAction.IncreaseTraffic,
        is TrafficUpdateAction.DecreaseTraffic,
        -> TrafficUpdateAction.NoAction
        else -> null
    }
}
