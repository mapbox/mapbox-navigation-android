package com.mapbox.navigation.examples.performance

data class FrameMetricsReport(
    val options: FrameMetricOptions,
    var totalFrames: Int = 0,
    var warningFrames: Int = 0,
    var errorFrames: Int = 0,
    var maxDuration: Double = 0.0
)

data class FrameMetricsJank(
    val unknownDelayDuration: Double,
    val inputHandlingDuration: Double,
    val animationDuration: Double,
    val layoutMeasureDuration: Double,
    val drawDuration: Double,
    val syncDuration: Double,
    val commandIssueDuration: Double,
    val swapBuffersDuration: Double,
    val totalDuration: Double
)