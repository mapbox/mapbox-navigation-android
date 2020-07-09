package com.mapbox.navigation.examples.performance

import android.os.Build
import android.view.FrameMetrics
import android.view.Window
import androidx.annotation.RequiresApi
import kotlin.math.max
import timber.log.Timber

@RequiresApi(Build.VERSION_CODES.N)
class FrameMetricsListener(
    val options: FrameMetricOptions
) : Window.OnFrameMetricsAvailableListener {

    private var frameMetricsReport = FrameMetricsReport(options)

    fun report(): FrameMetricsReport {
        return frameMetricsReport.copy()
    }

    override fun onFrameMetricsAvailable(window: Window, frameMetrics: FrameMetrics, dropCountSinceLastInvocation: Int) {
        val frameMetricsCopy = FrameMetrics(frameMetrics)
        val totalDurationMs = frameMetricsCopy.nanosToMillis(FrameMetrics.TOTAL_DURATION)

        frameMetricsReport.totalFrames++
        frameMetricsReport.maxDuration = max(totalDurationMs, frameMetricsReport.maxDuration)
        Timber.i("frame metric available total duration: %.2fms dropCount: %d".format(totalDurationMs, dropCountSinceLastInvocation))

        if (totalDurationMs > options.warningLevelMs) {
            val jankMessage = "Jank detected total duration: %.2fms\n".format(totalDurationMs) +
                "${mapToFrameMetricsFull(frameMetricsCopy)}"
            if (totalDurationMs > options.errorLevelMs) {
                frameMetricsReport.errorFrames++
                Timber.e(jankMessage)
            } else {
                frameMetricsReport.warningFrames++
                Timber.w(jankMessage)
            }
        }
    }

    private fun mapToFrameMetricsFull(frameMetrics: FrameMetrics): FrameMetricsFull {
        return FrameMetricsFull(
            frameMetrics.nanosToMillis(FrameMetrics.UNKNOWN_DELAY_DURATION),
            frameMetrics.nanosToMillis(FrameMetrics.INPUT_HANDLING_DURATION),
            frameMetrics.nanosToMillis(FrameMetrics.ANIMATION_DURATION),
            frameMetrics.nanosToMillis(FrameMetrics.LAYOUT_MEASURE_DURATION),
            frameMetrics.nanosToMillis(FrameMetrics.DRAW_DURATION),
            frameMetrics.nanosToMillis(FrameMetrics.SYNC_DURATION),
            frameMetrics.nanosToMillis(FrameMetrics.COMMAND_ISSUE_DURATION),
            frameMetrics.nanosToMillis(FrameMetrics.SWAP_BUFFERS_DURATION),
            frameMetrics.nanosToMillis(FrameMetrics.TOTAL_DURATION)
        )
    }

    private fun FrameMetrics.nanosToMillis(metric: Int): Double = getMetric(metric) * 0.000001
}

private data class FrameMetricsFull(
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
