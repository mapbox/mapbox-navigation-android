package com.mapbox.navigation.examples.performance

data class FrameMetricsReport(
    val options: FrameMetricOptions,
    var totalFrames: Int = 0,
    var warningFrames: Int = 0,
    var errorFrames: Int = 0,
    var maxDuration: Double = 0.0
)
