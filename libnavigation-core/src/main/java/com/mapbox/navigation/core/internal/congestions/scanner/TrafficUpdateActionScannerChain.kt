package com.mapbox.navigation.core.internal.congestions.scanner

import com.mapbox.navigation.core.internal.congestions.model.SpeedAnalysisResult
import com.mapbox.navigation.core.internal.congestions.model.TrafficUpdateAction

internal class TrafficUpdateActionScannerChain(
    private val fallbackValue: TrafficUpdateAction = TrafficUpdateAction.NoAction,
    private vararg val chain: TrafficUpdateActionScanner,
) : TrafficUpdateActionScanner {
    override fun scan(
        acc: TrafficUpdateAction,
        value: SpeedAnalysisResult,
    ): TrafficUpdateAction = chain.firstNotNullOfOrNull { scanner ->
        scanner.scan(acc, value)
    } ?: fallbackValue
}
