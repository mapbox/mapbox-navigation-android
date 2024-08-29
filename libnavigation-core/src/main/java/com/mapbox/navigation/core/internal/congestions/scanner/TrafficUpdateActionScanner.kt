package com.mapbox.navigation.core.internal.congestions.scanner

import com.mapbox.navigation.core.internal.congestions.model.SpeedAnalysisResult
import com.mapbox.navigation.core.internal.congestions.model.TrafficUpdateAction

internal fun interface TrafficUpdateActionScanner {
    fun scan(
        acc: TrafficUpdateAction,
        value: SpeedAnalysisResult,
    ): TrafficUpdateAction?
}
